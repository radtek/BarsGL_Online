package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.etl.BatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.InvisibleType;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ER18837 on 29.02.16.
 */
public class BatchPackageRepository extends AbstractBaseEntityRepository<BatchPackage, Long> {

    public int updatePackageStateProcessed(BatchPackage batchPackage, BatchPackage.PackageState state, Date processDate) {
        return executeUpdate("update BatchPackage p set p.packageState = ?1, p.processDate = ?3 where p.id = ?2"
                , state, batchPackage.getId(), processDate);
    }

    public int updatePackageStateNew(BatchPackage batchPackage, BatchPackage.PackageState state) {
        return executeUpdate("update BatchPackage p set p.packageState = ?1 where p.id = ?2 and p.packageState <> ?3 "
                , state, batchPackage.getId(), state);
    }

    public List<BatchPosting> getPostingsByPackage(Long idPackage) {
        return select(BatchPosting.class, "FROM BatchPosting p WHERE p.packageId = ?1" +
                " and p.invisible = ?2 ORDER BY p.id",
                idPackage, InvisibleType.N);
    }

    public List<BatchPosting> getPostingsByPackage(Long idPackage, BatchPostStatus status) {
        return select(BatchPosting.class, "FROM BatchPosting p WHERE p.packageId = ?1" +
                " and p.invisible = ?2 and p.status = ?3 ORDER BY p.id",
                idPackage, InvisibleType.N, status);
    }

    public BatchPosting getOnePostingByPackage(Long idPackage) {
        String sql = "FROM BatchPosting p WHERE p.packageId = ?1 and p.invisible = ?2 ";
            return selectFirst(BatchPosting.class, sql, idPackage, InvisibleType.N);
    }

    public BatchPosting getOnePostingByPackage(Long idPackage, BatchPostStatus enabledStatus) {
        String sql = "FROM BatchPosting p WHERE p.packageId = ?1 and p.invisible = ?2 ";
            return selectFirst(BatchPosting.class, sql + " and p.status = ?3",
                    idPackage, InvisibleType.N, enabledStatus);
    }

    public BatchPosting getOnePostingSigned(Long idPackage) {
        String sql = "FROM BatchPosting p WHERE p.packageId = ?1 and p.invisible = ?2 ";
        return selectFirst(BatchPosting.class, sql + " and p.status in (?3, ?4) ",
                idPackage, InvisibleType.N, BatchPostStatus.SIGNED, BatchPostStatus.SIGNEDDATE);
    }

    public int updatePostingsStatus(BatchPackage batchPackage, BatchPostStatus status) {
        return executeUpdate("update BatchPosting p set p.status = ?1 where p.packageId = ?2 and p.invisible = ?3",
                status, batchPackage.getId(), InvisibleType.N);
    }

    public int updatePostingsStatus(BatchPackage batchPackage, BatchPostStatus statusOld, BatchPostStatus statusNew) {
        return executeUpdate("update BatchPosting p set p.status = ?1 where p.packageId = ?2 and p.invisible = ?3 and p.status = ?4",
                statusNew, batchPackage.getId(), InvisibleType.N, statusOld);
    }

    public void updatePostingsStatusChange(BatchPackage batchPackage, Date timestamp, String userName, BatchPostStatus status) {
        executeUpdate("update BatchPosting p set p.status = ?1, p.changeTimestamp = ?2, p.changeName = ?3" +
                " where p.packageId = ?4 and p.invisible = ?5",
                status, timestamp, userName, batchPackage.getId(), InvisibleType.N);
    }

    public int signedPostingsStatus(BatchPackage batchPackage, Date timestamp, String userName, BatchPostStatus statusOld, BatchPostStatus statusNew) {
        return executeUpdate("update BatchPosting p set p.signerTamestamp = ?1, p.signerName = ?2, p.status = ?3" +
                        " where p.packageId = ?4 and p.invisible = ?5 and p.status = ?6",
                timestamp, userName, statusNew, batchPackage.getId(), InvisibleType.N, statusOld);
    }

    public int confirmPostingsStatus (BatchPackage batchPackage, Date timestamp, String userName, BatchPostStatus statusOld, BatchPostStatus statusNew) {
        return executeUpdate("update BatchPosting p set p.confirmTimestamp = ?1, p.confirmName = ?2, p.status = ?3" +
                " where p.packageId = ?4 and p.invisible = ?5 and p.status = ?6",
                timestamp, userName, statusNew, batchPackage.getId(), InvisibleType.N, statusOld);
    }

    public int signedConfirmPostingsStatus(BatchPackage batchPackage, Date timestamp, String userName, BatchPostStatus statusOld, BatchPostStatus statusNew) {
        return executeUpdate("update BatchPosting p set p.signerTamestamp = ?1, p.signerName = ?2, " +
                        "p.confirmTimestamp = ?3, p.confirmName = ?4, p.status = ?5" +
                        " where p.packageId = ?6 and p.invisible = ?7 and p.status = ?8",
                timestamp, userName, timestamp, userName, statusNew, batchPackage.getId(), InvisibleType.N, statusOld);
    }

    public void setPackageInvisible (BatchPackage pkg, Date timestamp, String userName) {
        executeUpdate("update BatchPosting p set p.invisible = ?1, p.changeTimestamp = ?2, p.changeName = ?3 " +
                " where p.packageId = ?4 and p.invisible = ?5",
                InvisibleType.U, timestamp, userName, pkg.getId(), InvisibleType.N);
        executeUpdate("update BatchPackage p set p.packageState = ?1 where p.id = ?2",
                BatchPackage.PackageState.DELETED, pkg.getId());
    }

    /**
     * Удаляет пакет запросов на операцию из таблицы
     */
    public void deletePackage(BatchPackage pkg) {
        executeUpdate("delete from BatchPosting p where p.packageId = ?1", pkg.getId());
        executeUpdate("delete from BatchPackage p where p.id = ?1", pkg.getId());
    }

    public void refusePackageStatus (BatchPackage pkg, Date timestamp, String userName, String reason, BatchPostStatus status) {
        executeUpdate("update BatchPosting p set p.reasonOfDeny = ?1, p.status = ?2, p.changeTimestamp = ?3, p.changeName = ?4 " +
                " where p.packageId = ?5  and p.invisible = ?6",
                reason, status, timestamp, userName, pkg.getId(), InvisibleType.N);
    }

    public void setPackagePostingDate(Long packageId, Date postDate) {
        executeUpdate("update BatchPosting p set p.postDate = ?1 where p.packageId = ?2  and p.invisible = ?3",
                        postDate, packageId, InvisibleType.N);
    }

    /**
     * Создает историческую запись для пакета запросов на операцию
     * @return
     * @throws SQLException
     */
    public BatchPackage createPackageHistory(BatchPackage pkg) throws SQLException {
        //	ID_PKG , SRC_PST, DEAL_ID, PMT_REF, DEPT_ID, OTS, NRT, RNRTL, RNRTS, AC_DR, CCY_DR, AMT_DR, AC_CR, CCY_CR, AMT_CR, AMTRU, SUBDEALID, FCHNG, PRFCNTR, USER_NAME, NROW, ECODE, EMSG, VDATE, INP_METHOD, PROCDATE, INVISIBLE, HEADBRANCH, USER_AU2, OTS_AU2, USER_AU3, OTS_AU3, USER_CHNG, OTS_CHNG, STATE, GLOID_REF, ID_PAR, ID_PREV, DESCRDENY, POSTDATE, CBCC_DR, CBCC_CR
        executeNativeUpdate("UPDATE GL_BATPST SET ID_PAR = ID WHERE ID_PKG = ? and INVISIBLE = ?", pkg.getId(), InvisibleType.N.name());
        executeNativeUpdate(
                "INSERT INTO GL_BATPST (ID_PKG, SRC_PST, DEAL_ID, PMT_REF, DEPT_ID, OTS, NRT, RNRTL, RNRTS, AC_DR, CCY_DR, AMT_DR, AC_CR, CCY_CR, AMT_CR, AMTRU, SUBDEALID, FCHNG, PRFCNTR, USER_NAME, NROW, ECODE, EMSG, VDATE, INP_METHOD, PROCDATE, INVISIBLE, HEADBRANCH, USER_AU2, OTS_AU2, USER_AU3, OTS_AU3, USER_CHNG, OTS_CHNG, STATE, GLOID_REF, ID_PAR, ID_PREV, DESCRDENY, POSTDATE, CBCC_DR, CBCC_CR)" +
                " SELECT ID_PKG, SRC_PST, DEAL_ID, PMT_REF, DEPT_ID, OTS, NRT, RNRTL, RNRTS, AC_DR, CCY_DR, AMT_DR, AC_CR, CCY_CR, AMT_CR, AMTRU, SUBDEALID, FCHNG, PRFCNTR, USER_NAME, NROW, ECODE, EMSG, VDATE, INP_METHOD, PROCDATE, '" +
                InvisibleType.H.name() + "', HEADBRANCH, USER_AU2, OTS_AU2, USER_AU3, OTS_AU3, USER_CHNG, OTS_CHNG, STATE, GLOID_REF, ID_PAR, ID_PREV, DESCRDENY, POSTDATE, CBCC_DR, CBCC_CR" +
                " FROM GL_BATPST WHERE ID_PKG = ? and INVISIBLE = ?",
                pkg.getId(), InvisibleType.N.name());
        return pkg;
    }

    public boolean checkFilialPermission(Long packageId, Long userId) throws Exception {
        DataRecord data = selectFirst("select 1 from GL_AU_PRMVAL where ID_USER = ? and PRM_CODE = ? and PRMVAL = ?",
                userId, "HeadBranch", "*");
        if (null != data)       // разрешены все филиалы
            return true;

        data = selectFirst("with PRM as (select PRMVAL from GL_AU_PRMVAL where ID_USER = ? and PRM_CODE = ?)\n" +
                        " select * from GL_BATPST where ID_PKG = ?\n" +
                        " and (CBCC_DR not in (select * from prm)" +
                        " and CBCC_CR not in (select * from prm))",
                userId, "HeadBranch", packageId);

        return (null == data);
    }

    public List<Long> getPackagesWaitdate(Date operday) throws SQLException {
        List<DataRecord> res = select("select distinct ID_PKG from GL_BATPST where not ID_PKG is null and PROCDATE = ?" +
                " and INVISIBLE = 'N' and STATE = 'WAITDATE'", operday);
        return res.stream().map(r -> r.getLong(0)).collect(Collectors.toList());
    }

    public List<Long> getPackagesSigned(Date operday) throws SQLException {
        List<DataRecord> res = select("select distinct ID_PKG from GL_BATPST where not ID_PKG is null and PROCDATE = ?" +
                " and INVISIBLE = 'N' and STATE in ('SIGNED', 'SIGNEDDATE')", operday);
        return res.stream().map(r -> r.getLong(0)).collect(Collectors.toList());
    }

    public List<Integer> getPackageStatistics(long idPackage) throws SQLException {
        List<DataRecord> res = select(
            "with GL_BATP as (select ID_PKG, SRV_REF, STATE from GL_BATPST where ID_PKG = ? and INVISIBLE = ?) \n" +
                "select count(1) from GL_BATP \n" +
                "union all    select count(1) from GL_BATP where STATE = 'COMPLETED' \n" +
                "union all    select count(1) from GL_BATP where STATE not in ('COMPLETED', 'CONTROL', 'WAITDATE', 'SIGNED', 'SIGNEDDATE') \n" +
                "union all    select count(1) from GL_BATP where not SRV_REF is null \n" +
                "union all    select count(1) from GL_BATP where not SRV_REF is null and STATE in ('ERRSRV', 'REFUSESRV')"
            , idPackage, InvisibleType.N.name());
        return res.stream().map(r -> r.getInteger(0)).collect(Collectors.toList());
    }

    public int getMovementCount(Long packageId) throws SQLException {
        DataRecord res = selectFirst("select count(1) from GL_BATPST where ID_PKG = ? and INVISIBLE = ? and not SRV_REF is null and not STATE in ('ERRSRV', 'REFUSESRV')",
                packageId, InvisibleType.N.name());
        return (null == res ? 0 : res.getInteger(0));
    }

}
