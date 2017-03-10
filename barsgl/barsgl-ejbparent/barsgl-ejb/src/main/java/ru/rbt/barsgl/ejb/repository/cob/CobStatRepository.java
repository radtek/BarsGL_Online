package ru.rbt.barsgl.ejb.repository.cob;

import ru.rbt.barsgl.ejb.bt.BalturRecalculator;
import ru.rbt.barsgl.ejb.entity.cob.CobStatId;
import ru.rbt.barsgl.ejb.entity.cob.CobStatistics;
import ru.rbt.barsgl.ejb.entity.dict.SourcesDeals;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.CobStep;
import ru.rbt.barsgl.shared.enums.OperState;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static ru.rbt.barsgl.shared.enums.CobStepStatus.*;

/**
 * Created by ER18837 on 10.03.17.
 */
public class CobStatRepository extends AbstractBaseEntityRepository<CobStatistics, CobStatId> {

    public Long createCobStepGroup(Date curdate) {
        Long idCob = nextId("SEQ_GL_COB");
        int cnt = executeNativeUpdate("insert into GL_COB_STAT " +
                "(ID_COB, PHASE_NO, DAT, PHASE_NAME, COEF_A, COEF_B, STATUS)" +
                "select ?, PHASE_NO, ?, PHASE_NAME, COEF_A, COEF_B, ? from GL_COB_MOD",
                idCob, curdate, Step_NotStart.name());
        return idCob;
    }

    /**
     * получает параметр - переменную для расчета длитеьлности шала COB
     * @param curdate
     * @param lwdate
     * @param step
     * @return
     * @throws SQLException
     */
    public Long getStepParameter(CobStep step, Date curdate, Date lwdate) throws SQLException {
        DataRecord res;
        switch (step) {
            case CobStopEtlProc:
            case CobResetBuffer:
                res = selectOne("select count(1) from GL_PD where PD_ID is null");
                return res.getLong(0);
            case CobManualProc:
                res = selectOne("select count(1) from GL_BATPST where PROCDATE = ? and STATE <> ? and INVISIBLE = ?",
                        curdate, BatchPostStatus.COMPLETED.name(), YesNo.N.name());
                return res.getLong(0);
            case CobStornoProc:
                res = selectOne("select count(1) from GL_OPER where STATE = ? and VDATE in (?, ?) and STORNO = ?",
                        OperState.ERCHK.name(), curdate, lwdate, YesNo.Y.name());
                return res.getLong(0);
            case CobFanProc:
                res = selectOne("select count(DISTINCT PAR_RF) from GL_OPER where FAN = ? and PROCDATE = ? and STATE = ?",
                        YesNo.Y.name(), curdate, OperState.LOAD.name());
                return res.getLong(0);
            case CobRecalc:
                res = selectOne("select count(1) from (select BSAACID, ACID from GL_BSARC where RECTED = ? group by BSAACID,ACID) T",
                        BalturRecalculator.BalturRecalcState.NEW.getValue());
                DataRecord res2 = selectOne("select count(1) from GL_OPER o join GL_POSTING p on o.GLOID = p.GLO_REF" +
                        " where o.PROCDATE = ? and o.SRC_PST = ? and o.STRN = ? and o.STATE = ",
                        curdate, SourcesDeals.SRCPST.KTP.getValue(), YesNo.Y.name(), OperState.POST.name());
                return res.getLong(0) + res2.getLong(0);
            default:
                return null;
//                throw new DefaultApplicationException("Неверный шаг COB:" + step);
        }
    }

    public int setStepEstimation(Long idCob, CobStep step, Long parameter) {
        return executeNativeUpdate("update GL_COB_STAT set PARAMETER = ?, ESTIMATED = COEF_A + COEF_B * ? where ID_COB = ? and PHASE_NO = ?",
                parameter, parameter, idCob, step.getPhaseNo());
    }

    public int increaseStepEstimation(Long idCob, CobStep step, Double scale) {
        return executeNativeUpdate("update GL_COB_STAT set ESTIMATED = ESTIMATED * ? where ID_COB = ? and PHASE_NO = ? and STATUS = ?",
                scale, idCob, step.getPhaseNo(), Step_Running.name());
    }

    public int setStepStart(Long idCob, CobStep step, Date timestamp) {
        return executeNativeUpdate("update GL_COB_STAT set STATUS = ?, OTS_START = ?, DURATION = ESTIMATED " +
                " where ID_COB = ? and PHASE_NO = ? and STATUS = ?",
                Step_Running.name(), timestamp, idCob, step.getPhaseNo(), Step_NotStart.name());
    }

    public int setStepSuccess(Long idCob, CobStep step, Date timestamp) {
        return executeNativeUpdate("update GL_COB_STAT set STATUS = ?, OTS_END = ?, DURATION = ? - OTS_START " +
                        " where ID_COB = ? and PHASE_NO = ? and STATUS = ?",
                Step_Success.name(), timestamp, timestamp, idCob, step.getPhaseNo(), Step_Running.name());
    }

    public int setStepError(Long idCob, CobStep step, Date timestamp, String message, String errorMessage) {
        return executeNativeUpdate("update GL_COB_STAT set STATUS = ?, OTS_END = ?, DURATION = ? - OTS_START, MESSAGE = ?, ERRORMSG = ? " +
                        " where ID_COB = ? and PHASE_NO = ?",
                Step_Error.name(), timestamp, timestamp, message, errorMessage, idCob, step.getPhaseNo());
    }

    public int updateStepMessage(Long idCob, CobStep step, String message) {
        return executeNativeUpdate("update GL_COB_STAT set MESSAGE = ? where ID_COB = ? and PHASE_NO = ?",
                message, idCob, step.getPhaseNo());
    }

    public List<CobStatistics> getCobSteps(Long idCob) {
        return select(CobStatistics.class, "from CobStatistics s where s.id.idCob = ?1 order by s.phaseNo", idCob);
    }

    public Long getMaxCobId() throws SQLException {
        DataRecord res = selectFirst("select max(ID_COB) from GL_COB_STAT");
        return null != res ? res.getLong(0) : null;
    }
}
