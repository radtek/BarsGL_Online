package ru.rbt.barsgl.ejb.repository.cob;

import ru.rbt.barsgl.ejb.entity.cob.CobStatId;
import ru.rbt.barsgl.ejb.entity.cob.CobStepStatistics;
import ru.rbt.barsgl.ejb.entity.dict.SourcesDeals;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.CobPhase;
import ru.rbt.barsgl.shared.enums.CobStepStatus;
import ru.rbt.barsgl.shared.enums.OperState;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static ru.rbt.barsgl.ejbcore.util.StringUtils.substr;
import static ru.rbt.barsgl.shared.enums.CobStepStatus.*;

/**
 * Created by ER18837 on 10.03.17.
 */
@Stateless
@LocalBean
public class CobStatRepository extends AbstractBaseEntityRepository<CobStepStatistics, CobStatId> {
    public static final String MSG_DELIMITER = "; \n";
    public static final int MSG_LEN = 1024;
    public static final int ERR_LEN = 4000;

    public Long createCobStepGroup(Date curdate) {
        Long idCob = nextId("SEQ_GL_COB");
/*      // так почему-то не работает - не может связать параметры в подзапросе
        int cnt = executeNativeUpdate("insert into GL_COB_STAT " +
                " (ID_COB, PHASE_NO, DAT, PHASE_NAME, COEF_A, COEF_B, STATUS)" +
                " select ?, PHASE_NO, ?, PHASE_NAME, COEF_A, COEF_B, ? from GL_COB_MOD",
                idCob, curdate, NotStart.name());
*/
        int cnt = executeNativeUpdate("insert into GL_COB_STAT " +
                " (ID_COB, PHASE_NO, DAT, PHASE_NAME, COEF_A, COEF_B, STATUS)" +
                " select " + idCob + ", PHASE_NO, '" + DateUtils.dbDateString(curdate) + "', PHASE_NAME, COEF_A, COEF_B, '"
                + CobStepStatus.NotStart.name() + "' from GL_COB_MOD", curdate);
        return idCob;
    }

    public List<CobStepStatistics> getCobSteps(Long idCob) {
        return select(CobStepStatistics.class, "from CobStepStatistics s where s.id.idCob = ?1 order by s.id.phaseNo", idCob);
    }

    /**
     * получает параметр - переменную для расчета длитеьлности шала COB
     * @param curdate
     * @param lwdate
     * @param phase
     * @return
     * @throws SQLException
     */
    public Long getStepParameter(CobPhase phase, Date curdate, Date lwdate) throws SQLException {
        DataRecord res;
        switch (phase) {
            case CobStopEtlProc:
                return 0L;
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
            case CobCloseBalance:
                return 0L;
            case CobFanProc:
                res = selectOne("select count(DISTINCT PAR_RF) from GL_OPER where FAN = ? and PROCDATE = ? and STATE = ?",
                        YesNo.Y.name(), curdate, OperState.LOAD.name());
                return res.getLong(0);
            case CobRecalcBaltur:
                res = selectOne("select sum(cnt) from (" +
                                "select count(1) cnt from (select BSAACID, ACID from GL_BSARC where RECTED = 0 group by BSAACID, ACID) T union all " +
                                "select count(1) cnt from GL_OPER o join GL_POSTING p on o.GLOID = p.GLO_REF " +
                                    "where o.PROCDATE = ? and o.SRC_PST = ? and o.STRN = 'Y' and o.STATE = 'POST' union all " +
                                "select count(1) cnt from GL_BVJRNL where STATE = 'NEW') T1",
                        curdate, SourcesDeals.SRCPST.KTP.getValue());
                return res.getLong(0);
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
                newEstimate, idCob, phaseNo, Running.name(), oldEstimate);
    }

/*
    private String getJointMessage(String message, boolean withDelim) {
        if (StringUtils.isEmpty(message))
            return "MESSAGE";
        else {
            String delim = withDelim ? MSG_DELIMITER : "'; '";
            return substr("MESSAGE || " + delim + " || '" + message.replace("'", "''") + "'", 1024);
        }
    }
*/

    private String getJointMessage(Long idCob, Integer phaseNo, String message, boolean withDelim) {
        String msg = "";
        String joined = withDelim ? MSG_DELIMITER : "; ";
        try {
            DataRecord res = selectFirst("select MESSAGE from GL_COB_STAT  where ID_COB = ? and PHASE_NO = ?", idCob, phaseNo);
            msg = res.getString(0);
            joined = msg + joined;
        } catch (SQLException e) {
            return substr(message, MSG_LEN);
        }
        if (joined.length() > MSG_LEN)
            return msg;
        return joined + substr(message, MSG_LEN - joined.length());
    }

    public int setStepStart(Long idCob, Integer phaseNo, Date timestamp, String message) {
        return executeNativeUpdate("update GL_COB_STAT set STATUS = ?, OTS_START = ?, DURATION = ESTIMATED, MESSAGE = ? " +
                " where ID_COB = ? and PHASE_NO = ? and STATUS = ?",
                Running.name(), timestamp, substr(message, 1024), idCob, phaseNo, NotStart.name());
    }

    public int setStepSkipped(Long idCob, Integer phaseNo, Date timestamp, String message, boolean withDelim) {
        return executeNativeUpdate("update GL_COB_STAT set STATUS = ?, OTS_END = ?, DURATION = 0, MESSAGE = ? " +
                " where ID_COB = ? and PHASE_NO = ? and STATUS = ?",
                Skipped.name(), timestamp, getJointMessage(idCob, phaseNo, message, withDelim), idCob, phaseNo, Running.name());
    }

    public int setStepSuccess(Long idCob, Integer phaseNo, Date timestamp, String message, boolean withDelim) {
        int cnt = executeNativeUpdate("update GL_COB_STAT set STATUS = ?, OTS_END = ?, MESSAGE = ? " +
                " where ID_COB = ? and PHASE_NO = ? and STATUS = ?",
                Success.name(), timestamp, getJointMessage(idCob, phaseNo, message, withDelim), idCob, phaseNo, Running.name());
        if (cnt == 1) {
            cnt = executeNativeUpdate("update GL_COB_STAT set DURATION = OTS_END - OTS_START " +
                    " where ID_COB = ? and PHASE_NO = ? and STATUS = ?",
                    idCob, phaseNo, Success.name());
        }
        return cnt;
    }

    public int setStepError(Long idCob, Integer phaseNo, Date timestamp, String message, String errorMessage, CobStepStatus status, boolean withDelim) {
        int cnt = executeNativeUpdate("update GL_COB_STAT set STATUS = ?, OTS_END = ?, ERRORMSG = ?, MESSAGE = ? " +
                        " where ID_COB = ? and PHASE_NO = ?",
                status.name(), timestamp, substr(errorMessage, ERR_LEN), getJointMessage(idCob, phaseNo, message, withDelim), idCob, phaseNo);
        if (cnt == 1) {
            cnt = executeNativeUpdate("update GL_COB_STAT set DURATION = OTS_END - OTS_START " +
                            " where ID_COB = ? and PHASE_NO = ? and STATUS = ?",
                    idCob, phaseNo, status.name());
        }
        return cnt;
    }

    public int updateStepMessage(Long idCob, Integer phaseNo, String message, boolean withDelim) {
        return executeNativeUpdate("update GL_COB_STAT set MESSAGE = ? " +
                        " where ID_COB = ? and PHASE_NO = ?",
                getJointMessage(idCob, phaseNo, message, withDelim), idCob, phaseNo);
    }

    public Long getMaxCobId() throws SQLException {
        DataRecord res = selectFirst("select max(ID_COB) from GL_COB_STAT");
        return null != res ? res.getLong(0) : null;
    }

    public Long getMaxRunCobId() throws SQLException {
        DataRecord res = selectFirst("select max(ID_COB) from GL_COB_STAT where PHASE_NO = 1 and STATUS != ?", NotStart.name());
        return null != res ? res.getLong(0) : null;
    }

    public Long getMaxRunCobId(Date curdate) throws SQLException {
        DataRecord res = selectFirst("select max(ID_COB) from GL_COB_STAT where DAT = ? and PHASE_NO = 1 and STATUS != ?", curdate, NotStart.name());
        return null != res ? res.getLong(0) : null;
    }

    public CobStepStatus getRunCobStatus(Date curdate) throws SQLException {
        Long idCob = getMaxRunCobId(curdate);
        if (null != idCob)
            return getCobStatus(getCobSteps(idCob));
        else
            return NotStart;
    }

    public CobStepStatus getCobStatus(List<CobStepStatistics> stepList) {
        if (null == stepList || stepList.isEmpty())
            return NotStart;
        CobStepStatus firstStatus = stepList.get(0).getStatus();
        CobStepStatus lastStatus = stepList.get(stepList.size() - 1).getStatus();
        if (firstStatus == NotStart) {
            return NotStart;
        } else if (stepList.stream().anyMatch(a -> a.getStatus() == Halt)){
            return Halt;
        } else if (lastStatus != NotStart) {
            return lastStatus;
        } else {
            return Running;
        }
    }

}
