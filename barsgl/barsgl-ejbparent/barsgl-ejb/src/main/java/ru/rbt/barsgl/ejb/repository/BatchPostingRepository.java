package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLManualOperation;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.InvisibleType;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.ejbcore.util.StringUtils;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static ru.rbt.barsgl.shared.enums.BatchPostStatus.SIGNED;
import static ru.rbt.barsgl.shared.enums.BatchPostStatus.SIGNEDDATE;
import static ru.rbt.ejbcore.util.StringUtils.substr;

/**
 * Created by ER18837 on 29.02.16.
 */
@Stateless
@LocalBean
public class BatchPostingRepository extends AbstractBaseEntityRepository<BatchPosting, Long> {
    public static final String histfields = //"INVISIBLE, ID_PAR, " +
            "ID_PREV, ID_PKG, SRC_PST, DEAL_ID, PMT_REF, DEPT_ID, OTS, NRT, RNRTL, RNRTS, \n" +
            "AC_DR, CCY_DR, AMT_DR, AC_CR, CCY_CR, AMT_CR, AMTRU, \n" +
            "SUBDEALID, FCHNG, PRFCNTR, USER_NAME, NROW, ECODE, EMSG, VDATE, INP_METHOD, PROCDATE, \n" +
            "HEADBRANCH, USER_AU2, OTS_AU2, USER_AU3, OTS_AU3, USER_CHNG, OTS_CHNG, STATE, \n" +
            "GLOID_REF, DESCRDENY, POSTDATE, CBCC_DR, CBCC_CR, SRV_REF, SEND_SRV, OTS_SRV, TECH_ACT \n";

    public BatchPosting refresh(BatchPosting posting) {
        return (null == posting) ? null : refresh(posting, true);
    }

    public BatchPosting findById(Long postingId) {
        BatchPosting posting = super.findById(BatchPosting.class, postingId);
        if (null != posting) {
            posting = refresh(posting, true);
            posting.setControllableDebit(isControlableAccount(posting.getAccountDebit()));
            posting.setControllableCredit(isControlableAccount(posting.getAccountCredit()));
            return posting;
        } else
            return null;
    }

    public int updatePostingStatusSuccess(Long postingId, GLManualOperation operation) {
        return executeUpdate("update BatchPosting p set p.errorCode = ?1, p.errorMessage = ?2, p.status = ?3, p.operation = ?4 where p.id = ?5",
                0, "", BatchPostStatus.COMPLETED, operation, postingId);
    }

    public int updatePostingStatusError(Long postingId, String message, BatchPostStatus status, int errorCode) {
        return executeUpdate("update BatchPosting p set p.errorCode = ?1, p.errorMessage = ?2, p.status = ?3 where p.id = ?4",
                errorCode, substr(message, 4000), status, postingId);
    }

    /**
     * Устанавливает статус запроса на операцию
     * @param statusNew     - новый статус
     * @return
     * @throws Exception
     */
    public int updatePostingStatus(Long postingId, BatchPostStatus statusNew, BatchPostStatus statusOld) {
        return executeUpdate("update BatchPosting p set p.status = ?1 where p.id = ?2  and p.status = ?3", statusNew, postingId, statusOld);
    }

    public int updatePostingStatusNew(Long postingId, BatchPostStatus statusNew) {
        return executeUpdate("update BatchPosting p set p.status = ?1 where p.id = ?2 and p.status <> ?3", statusNew, postingId, statusNew);
    }

    public int updatePostingStatusChanged(Long postingId, Date timestamp, String userName, BatchPostStatus statusNew, BatchPostStatus statusOld) {
        return executeUpdate("update BatchPosting p set p.status = ?1, p.changeTimestamp = ?2, p.changeName = ?3  where p.id = ?4 and p.status = ?5",
                statusNew, timestamp, userName, postingId, statusOld);
    }

    public int updatePostingStatusDeny(Long postingId, Date timestamp, BatchPostStatus statusNew, BatchPostStatus statusOld, String reason) {
        return executeUpdate("update BatchPosting p set p.status = ?1, p.changeTimestamp = ?2, p.reasonOfDeny = ?3  where p.id = ?4 and p.status = ?5",
                statusNew, timestamp, reason, postingId, statusOld);
    }

    public int signedPostingStatus(Long postingId, Date timestamp, String userName, BatchPostStatus statusNew, BatchPostStatus statusOld) {
        return executeUpdate("update BatchPosting p set p.signerTamestamp = ?1, p.signerName = ?2, p.status = ?3 where p.id = ?4 and p.status = ?5",
                timestamp, userName, statusNew, postingId, statusOld);
    }

    public int confirmPostingStatus (Long postingId, Date timestamp, String userName, BatchPostStatus statusNew, BatchPostStatus statusOld) {
        return executeUpdate("update BatchPosting p set p.confirmTimestamp = ?1, p.confirmName = ?2, p.status = ?3 where p.id = ?4 and p.status = ?5",
                timestamp, userName, statusNew, postingId, statusOld);
    }

    public int signedConfirmPostingStatus(Long postingId, Date timestamp, String userName, BatchPostStatus statusNew, BatchPostStatus statusOld) {
        return executeUpdate("update BatchPosting p set p.signerTamestamp = ?1, p.signerName = ?2, " +
                "p.confirmTimestamp = ?3, p.confirmName = ?4, p.status = ?5 where p.id = ?6 and p.status = ?7",
                timestamp, userName, timestamp, userName, statusNew, postingId, statusOld);
    }

    public int refusePostingStatus (Long postingId, String reason, Date timestamp, String userName, BatchPostStatus statusNew, BatchPostStatus statusOld) {
        return executeUpdate("update BatchPosting p set p.changeTimestamp = ?1, p.changeName = ?2, " +
                "p.reasonOfDeny = ?3, p.status = ?4 where p.id = ?5 and p.status = ?6",
                timestamp, userName, reason, statusNew, postingId, statusOld);
    }

    public int sendPostingStatus(Long postingId, String movementId, Date timestamp, BatchPostStatus statusNew, BatchPostStatus statusOld) {
        return executeUpdate("update BatchPosting p set p.movementId = ?1, p.sendTimestamp = ?2, p.receiveTimestamp = ?3, " +
                "p.status = ?4 where p.id = ?5 and p.status = ?6",
                movementId, timestamp, null, statusNew, postingId, statusOld);
    }

    public int receivePostingStatus(Long postingId, String movementId, Date timestamp, BatchPostStatus statusNew, BatchPostStatus statusOld,
                                    int errorCode, String errorMessage) {
        return executeUpdate("update BatchPosting p set p.receiveTimestamp = ?1, p.status = ?2, p.errorCode = ?3, p.errorMessage = ?4" +
                " where p.id = ?5 and p.movementId = ?6 and p.status = ?7",
                timestamp, statusNew, errorCode, errorMessage, postingId, movementId, statusOld);
    }

    public int setPostingInvisible (Long postingId, Date timestamp, String userName) {
        return executeUpdate("update BatchPosting p set p.changeTimestamp = ?1, p.changeName = ?2, p.invisible = ?3 where p.id = ?4",
                timestamp, userName, InvisibleType.U, postingId);
    }

    public int setUnprocessedPostingsInvisible (Date timestamp, Date currentDate) {
        return executeUpdate("update BatchPosting p set p.changeTimestamp = ?1, p.invisible = ?2" +
            " where p.procDate = ?3 and p.status <> ?4 and p.invisible = ?5 ",
                timestamp, InvisibleType.S, currentDate, BatchPostStatus.COMPLETED, InvisibleType.N);
    }

    public int setWaitsrvPostingsTimeout (Date timestamp, Date currentDate) {
        return executeUpdate("update BatchPosting p set p.changeTimestamp = ?1, p.invisible = ?2" +
                        " where p.procDate = ?3 and p.status <> ?4 and p.invisible = ?5 ",
                timestamp, InvisibleType.S, currentDate, BatchPostStatus.COMPLETED, InvisibleType.N);
    }

    public int setPostingDate (Long postingId, Date postDate) {
        return executeUpdate("update BatchPosting p set p.postDate = ?1 where p.id = ?2",
                postDate, postingId);
    }

    /**
     * Удаляет запрос на операцию из таблицы
     */
    public int deletePosting(Long postingId) {
        return executeUpdate("delete from BatchPosting p where p.id = ?1", postingId);
    }

    /**
     * Создает историческую запись для запроса на операцию
     * @return
     * @throws SQLException
     */
    public BatchPosting createPostingHistory(Long postingId, Date timestamp, String userName) throws SQLException {
        executeNativeUpdate(
                "INSERT INTO GL_BATPST (ID, INVISIBLE, ID_PAR, " + histfields + ")" +
                " SELECT GL_BATPST_SEQ.NEXTVAL, 'H', ID, "  + histfields + " FROM GL_BATPST WHERE ID = ?",
                postingId);
        Long idHist = selectFirst("SELECT GL_BATPST_SEQ.CURRVAL id FROM DUAL").getLong("id");
        executeNativeUpdate("UPDATE GL_BATPST SET OTS_CHNG = ?, USER_CHNG = ?, ID_PAR = ?, ID_PREV = ? WHERE ID = ?",
                timestamp, userName, postingId, idHist, postingId);
        return findById(postingId);
    }

    public int setPostingsWaitDate (Date procDate) {
        String where = " where p.packageId is null and p.status = ?1 and p.procDate = ?2 and p.invisible = ?3";
        return executeUpdate("update BatchPosting p set p.postDate = p.procDate" + where,
                BatchPostStatus.WAITDATE, procDate, InvisibleType.N);
    }

    public List<BatchPosting> getPostingsById (List<Long> listId) {
        return select(BatchPosting.class, "FROM BatchPosting p where p.id in (" + StringUtils.listToString(listId, ",") + ") order by p.id");
    }

    public List<BatchPosting> getPostingsWaitDate (Date procDate) {
        String where = " where p.packageId is null and p.status = ?1 and p.procDate = ?2 and p.invisible = ?3";
        return select(BatchPosting.class, "FROM BatchPosting p " + where,
                BatchPostStatus.WAITDATE, procDate, InvisibleType.N);
    }

    public List<BatchPosting> getPostingsSigned (Date procDate) {
        String where = " where p.packageId is null and p.status in (?1, ?2) and p.procDate = ?3 and p.invisible = ?4";
        return select(BatchPosting.class, "FROM BatchPosting p " + where,
                BatchPostStatus.SIGNED, BatchPostStatus.SIGNEDDATE, procDate, InvisibleType.N);
    }

    public List<Long> getPostingsForProcessing (int postingCount, Date curdate) {
        try {
            List<DataRecord> res = selectMaxRows("select * from GL_BATPST where ID_PKG is NULL and STATE in (?, ?) and PROCDATE = ? and INVISIBLE = ? " +
                            "ORDER BY ID"
                    , postingCount, new Object[]{SIGNED.name(), SIGNEDDATE.name(), curdate, InvisibleType.N.name()});
            return res.stream().map(r -> r.getLong(0)).collect(Collectors.toList());
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public List<BatchPosting> getPostingsNotCompleted (Date postdate) {
        String where = "where p.status <> ?2 and p.procDate = ?1 and p.invisible = ?3";
        return select(BatchPosting.class, "FROM BatchPosting p " + where,
                BatchPostStatus.COMPLETED, postdate, InvisibleType.N);
    }

    public List<BatchPosting> findPostingsByTimeout(Date sendTime, Date postDate) {
        return select(BatchPosting.class, "FROM BatchPosting p where p.status = ?1 and p.sendTimestamp < ?2 and p.receiveTimestamp is null " +
                "and p.procDate = ?3 and p.invisible = ?4",
                BatchPostStatus.WAITSRV, sendTime, postDate, InvisibleType.N);
    }

    public BatchPosting findPostingByMovementId(String movementId, Date postDate) {
        return refresh(selectFirst(BatchPosting.class, "FROM BatchPosting p where p.movementId = ?1 and p.procDate = ?2 and p.invisible <> ?3",
                movementId, postDate, InvisibleType.H));
    }

    public BatchPosting getOnePostingByPackage(Long packageId) {
        String sql = "FROM BatchPosting p WHERE p.packageId = ?1 and p.invisible = ?2 ";
        return refresh(selectFirst(BatchPosting.class, sql, packageId, InvisibleType.N));
    }

    public BatchPosting getOnePostingByPackageWithStatus(Long packageId, BatchPostStatus enabledStatus) {
        String sql = "FROM BatchPosting p WHERE p.packageId = ?1 and p.invisible = ?2 ";
        return refresh(selectFirst(BatchPosting.class, sql + " and p.status = ?3",
                packageId, InvisibleType.N, enabledStatus));
    }

    public BatchPosting getOnePostingByPackageWithoutStatus(Long packageId, BatchPostStatus enabledStatus) {
        String sql = "FROM BatchPosting p WHERE p.packageId = ?1 and p.invisible = ?2 ";
        return refresh(selectFirst(BatchPosting.class, sql + " and p.status <> ?3",
                packageId, InvisibleType.N, enabledStatus));
    }

    public BatchPosting getOnePostingByPackageForSign(Long packageId) {
        String sql = "FROM BatchPosting p WHERE p.packageId = ?1 and p.invisible = ?2 ";
        return refresh(selectFirst(BatchPosting.class, sql + " and p.status in (?3, ?4) ",
                packageId, InvisibleType.N, BatchPostStatus.SIGNEDVIEW, BatchPostStatus.OKSRV));
    }

    public BatchPosting getOnePostingByPackageSigned(Long packageId) {
        String sql = "FROM BatchPosting p WHERE p.packageId = ?1 and p.invisible = ?2 ";
        return refresh(selectFirst(BatchPosting.class, sql + " and p.status in (?3, ?4) ",
                packageId, InvisibleType.N, BatchPostStatus.SIGNED, BatchPostStatus.SIGNEDDATE));
    }

    /**
     * true - клиентсий счет, контролируемый АБС
     * @param bsaacid
     * @return
     */
    public boolean isControlableAccount(String bsaacid) {
        try {
            DataRecord res = selectFirst("select IS_CTRL from V_ACC_CTRL where BSAACID = ? and IS_CTRL = ?", bsaacid, YesNo.Y.name());
            return (null != res);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

}
