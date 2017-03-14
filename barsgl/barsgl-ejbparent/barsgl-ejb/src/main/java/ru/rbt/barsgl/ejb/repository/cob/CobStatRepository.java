package ru.rbt.barsgl.ejb.repository.cob;

import ru.rbt.barsgl.ejb.bt.BalturRecalculator;
import ru.rbt.barsgl.ejb.entity.cob.CobStatId;
import ru.rbt.barsgl.ejb.entity.cob.CobStatistics;
import ru.rbt.barsgl.ejb.entity.dict.SourcesDeals;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.CobStep;
import ru.rbt.barsgl.shared.enums.CobStepStatus;
import ru.rbt.barsgl.shared.enums.OperState;

import java.math.BigDecimal;
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
/*      // так почему-то не работает - не может связать параметры в подзапросе
        int cnt = executeNativeUpdate("insert into GL_COB_STAT " +
                " (ID_COB, PHASE_NO, DAT, PHASE_NAME, COEF_A, COEF_B, STATUS)" +
                " select ?, PHASE_NO, ?, PHASE_NAME, COEF_A, COEF_B, ? from GL_COB_MOD",
                idCob, curdate, Step_NotStart.name());
*/
        int cnt = executeNativeUpdate("insert into GL_COB_STAT " +
                " (ID_COB, PHASE_NO, DAT, PHASE_NAME, COEF_A, COEF_B, STATUS)" +
                " select " + idCob + ", PHASE_NO, '" + DateUtils.dbDateString(curdate) + "', PHASE_NAME, COEF_A, COEF_B, '"
                + CobStepStatus.Step_NotStart.name() + "' from GL_COB_MOD", curdate);
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
                res = selectOne("select count(1) from GL_OPER where STATE = ? and VDATE in (?, ?) and STRN = ?",
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
                        " where o.PROCDATE = ? and o.SRC_PST = ? and o.STRN = ? and o.STATE = ?",
                        curdate, SourcesDeals.SRCPST.KTP.getValue(), YesNo.Y.name(), OperState.POST.name());
                return res.getLong(0) + res2.getLong(0);
            default:
                return null;
//                throw new DefaultApplicationException("Неверный шаг COB:" + step);
        }
    }

    public int setStepEstimate(Long idCob, Integer phaseNo, Long parameter) {
        return executeNativeUpdate("update GL_COB_STAT set PARAMETER = ?, ESTIMATED = COEF_A + COEF_B * ? where ID_COB = ? and PHASE_NO = ?",
                parameter, parameter, idCob, phaseNo);
    }

    public int increaseStepEstimate(Long idCob, Integer phaseNo, BigDecimal newEstimate, BigDecimal oldEstimate) {
        return executeNativeUpdate("update GL_COB_STAT set DURATION = ?" +
                " where ID_COB = ? and PHASE_NO = ? and STATUS = ? and DURATION = ?",
                newEstimate, idCob, phaseNo, Step_Running.name(), oldEstimate);
    }

    public int setStepStart(Long idCob, Integer phaseNo, Date timestamp) {
        return executeNativeUpdate("update GL_COB_STAT set STATUS = ?, OTS_START = ?, DURATION = ESTIMATED " +
                " where ID_COB = ? and PHASE_NO = ? and STATUS = ?",
                Step_Running.name(), timestamp, idCob, phaseNo, Step_NotStart.name());
    }

    public int setStepSuccess(Long idCob, Integer phaseNo, Date timestamp, String message) {
        int cnt = executeNativeUpdate("update GL_COB_STAT set STATUS = ?, OTS_END = ?, MESSAGE = ? " +
                        " where ID_COB = ? and PHASE_NO = ? and STATUS = ?",
                Step_Success.name(), timestamp, message, idCob, phaseNo, Step_Running.name());
        if (cnt == 1) {
            cnt = executeNativeUpdate("update GL_COB_STAT set DURATION = OTS_END - OTS_START " +
                        " where ID_COB = ? and PHASE_NO = ? and STATUS = ?",
                    idCob, phaseNo, Step_Success.name());
        }
        return cnt;
    }

    public int setStepError(Long idCob, Integer phaseNo, Date timestamp, String message, String errorMessage) {
        int cnt = executeNativeUpdate("update GL_COB_STAT set STATUS = ?, OTS_END = ?, MESSAGE = ?, ERRORMSG = ? " +
                        " where ID_COB = ? and PHASE_NO = ?",
                Step_Error.name(), timestamp, message, errorMessage, idCob, phaseNo);
        if (cnt == 1) {
            cnt = executeNativeUpdate("update GL_COB_STAT set DURATION = OTS_END - OTS_START " +
                            " where ID_COB = ? and PHASE_NO = ? and STATUS = ?",
                    idCob, phaseNo, Step_Error.name());
        }
        return cnt;
    }

    public int updateStepMessage(Long idCob, Integer phaseNo, String message) {
        return executeNativeUpdate("update GL_COB_STAT set MESSAGE = ? where ID_COB = ? and PHASE_NO = ?",
                message, idCob, phaseNo);
    }

    public List<CobStatistics> getCobSteps(Long idCob) {
        return select(CobStatistics.class, "from CobStatistics s where s.id.idCob = ?1 order by s.id.phaseNo", idCob);
    }

    public Long getMaxCobId() throws SQLException {
        DataRecord res = selectFirst("select max(ID_COB) from GL_COB_STAT");
        return null != res ? res.getLong(0) : null;
    }
}
