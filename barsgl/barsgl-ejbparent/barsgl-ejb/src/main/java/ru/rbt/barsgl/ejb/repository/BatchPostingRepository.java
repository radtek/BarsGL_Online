package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLManualOperation;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.InvisibleType;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static ru.rbt.barsgl.ejbcore.util.StringUtils.substr;

/**
 * Created by ER18837 on 29.02.16.
 */
@Stateless
@LocalBean
public class BatchPostingRepository extends AbstractBaseEntityRepository<BatchPosting, Long> {

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
     * @param posting   - запрос на операцию
     * @param status     - новый статус
     * @return
     * @throws Exception
     */
    public BatchPosting updatePostingStatus(BatchPosting posting, BatchPostStatus status) {
        executeUpdate("update BatchPosting p set p.status = ?1 where p = ?2", status, posting);
        return findById(BatchPosting.class, posting.getId());
    }

    public int updatePostingStatusNew(BatchPosting posting, BatchPostStatus status) {
        return executeUpdate("update BatchPosting p set p.status = ?1 where p = ?2 and p.status <> ?3", status, posting, status);
    }

    public int  updatePostingStatusChanged(BatchPosting posting, Date timestamp, String userName, BatchPostStatus status) {
        return executeUpdate("update BatchPosting p set p.status = ?1, p.changeTimestamp = ?2, p.changeName = ?3  where p = ?4",
                status, timestamp, userName, posting);
    }

    public int updatePostingStatusDeny(Long postingId, Date timestamp, BatchPostStatus status, String reason) {
        return executeUpdate("update BatchPosting p set p.status = ?1, p.changeTimestamp = ?2, p.reasonOfDeny = ?3  where p.id = ?4",
                status, timestamp, reason, postingId);
    }

    public int signedPostingStatus(BatchPosting posting, Date timestamp, String userName, BatchPostStatus status) {
        return executeUpdate("update BatchPosting p set p.signerTamestamp = ?1, p.signerName = ?2, p.status = ?3 where p = ?4",
                timestamp, userName, status, posting);
    }

    public int confirmPostingStatus (BatchPosting posting, Date timestamp, String userName, BatchPostStatus status) {
        return executeUpdate("update BatchPosting p set p.confirmTimestamp = ?1, p.confirmName = ?2, p.status = ?3 where p = ?4",
                timestamp, userName, status, posting);
    }

    public int signedConfirmPostingStatus(BatchPosting posting, Date timestamp, String userName, BatchPostStatus status) {
        return executeUpdate("update BatchPosting p set p.signerTamestamp = ?1, p.signerName = ?2, " +
                "p.confirmTimestamp = ?3, p.confirmName = ?4, p.status = ?5 where p = ?6",
                timestamp, userName, timestamp, userName, status, posting);
    }

    public int refusePostingStatus (BatchPosting posting, String reason, Date timestamp, String userName, BatchPostStatus status) {
        return executeUpdate("update BatchPosting p set p.changeTimestamp = ?1, p.changeName = ?2, " +
                "p.reasonOfDeny = ?3, p.status = ?4 where p = ?5",
                timestamp, userName, reason, status, posting);
    }

    public int setPostingInvisible (BatchPosting posting, Date timestamp, String userName) {
        return executeUpdate("update BatchPosting p set p.changeTimestamp = ?1, p.changeName = ?2, p.invisible = ?3 where p = ?4",
                timestamp, userName, InvisibleType.U, posting);
    }

    public int setUnprocessedPostingsInvisible (Date timestamp, Date currentDate) {
        return executeUpdate("update BatchPosting p set p.changeTimestamp = ?1, p.invisible = ?2" +
            " where p.procDate = ?3 and p.status <> ?4 and p.invisible = ?5 ",
                timestamp, InvisibleType.S, currentDate, BatchPostStatus.COMPLETED, InvisibleType.N);
    }

    public int setPostingDate (BatchPosting posting, Date postDate) {
        return executeUpdate("update BatchPosting p set p.postDate = ?1 where p = ?2",
                postDate, posting);
    }

    /**
     * Удаляет запрос на операцию из таблицы
     * @param posting
     */
    public int deletePosting(BatchPosting posting) {
        return executeUpdate("delete from BatchPosting p where p = ?1", posting);
    }

    /**
     * Создает историческую запись для запроса на операцию
     * @return
     * @throws SQLException
     */
    public BatchPosting createPostingHistory(Long postingId, Date timestamp, String userName) throws SQLException {
        String fields = //"INVISIBLE, ID_PAR, " +
                "ID_PREV, ID_PKG, SRC_PST, DEAL_ID, PMT_REF, DEPT_ID, OTS, NRT, RNRTL, RNRTS, \n" +
                "AC_DR, CCY_DR, AMT_DR, AC_CR, CCY_CR, AMT_CR, AMTRU, \n" +
                "SUBDEALID, FCHNG, PRFCNTR, USER_NAME, NROW, ECODE, EMSG, VDATE, INP_METHOD, PROCDATE, \n" +
                "HEADBRANCH, USER_AU2, OTS_AU2, USER_AU3, OTS_AU3, USER_CHNG, OTS_CHNG, STATE, \n" +
                "GLOID_REF, DESCRDENY, POSTDATE, CBCC_DR, CBCC_CR, SRV_REF, OTS_SRV\n";
        executeNativeUpdate("INSERT INTO GL_BATPST (INVISIBLE, ID_PAR, " + fields + ")" +
                " SELECT 'H', ID, "  + fields + " FROM GL_BATPST WHERE ID = ?",
                postingId);
        Long idHist = selectFirst("SELECT IDENTITY_VAL_LOCAL() id FROM SYSIBM.SYSDUMMY1").getLong("id");
        executeNativeUpdate("UPDATE GL_BATPST SET OTS_CHNG = ?, USER_CHNG = ?, ID_PAR = ?, ID_PREV = ? WHERE ID = ?",
                timestamp, userName, postingId, idHist, postingId);
        return findById(BatchPosting.class, postingId);
    }

    public int setPostingsWaitDate (Date procDate) {
        String where = " where p.packageId is null and p.status = ?1 and p.procDate = ?2 and p.invisible = ?3";
        return executeUpdate("update BatchPosting p set p.postDate = p.procDate" + where,
                BatchPostStatus.WAITDATE, procDate, InvisibleType.N);
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

    public List<BatchPosting> getPostingsNotCompleted (Date postDate) {
        String where = "where p.status <> ?2 and p.procDate = ?1 and p.invisible = ?3";
        return select(BatchPosting.class, "FROM BatchPosting p " + where,
                BatchPostStatus.COMPLETED, postDate, InvisibleType.N);
    }

    public int updateMovementId(Long postingId, String movementId, Date timestamp, String userName) {
        return executeUpdate("update BatchPosting p set p.movementId = ?1 where p.id = ?2",
                movementId, postingId);
    }

    public int updateMovementTimestamp(Long postingId, String movementId, Date timestamp) {
        return executeUpdate("update BatchPosting p set p.movementTimestamp = ?1 where p.id = ?2 and p.movementId = ?3",
                timestamp, postingId, movementId);
    }


}
