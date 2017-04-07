package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.etl.BatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.barsgl.shared.enums.BatchPackageState;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.InvisibleType;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.rbt.barsgl.shared.enums.BatchPackageState.*;
import static ru.rbt.barsgl.shared.enums.BatchPostStatus.*;

/**
 * Created by ER18837 on 29.02.16.
 */
public class BatchPackageRepository extends AbstractBaseEntityRepository<BatchPackage, Long> {
    private final SimpleDateFormat onlyDate = new SimpleDateFormat("dd.MM.yyyy");
    private final SimpleDateFormat dateTime = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public BatchPackage refresh(BatchPackage pkg) {
        return (null == pkg) ? null : refresh(pkg, true);
    }

    public BatchPackage findById(Long primaryKey) {
        return refresh(super.findById(BatchPackage.class, primaryKey));
    }

    public int updatePackageState(Long packageId, BatchPackageState stateNew, BatchPackageState stateOld) {
        return executeUpdate("update BatchPackage p set p.packageState = ?1 where p.id = ?2 and p.packageState = ?3"
                , stateNew, packageId, stateOld);
    }

    public int updatePackageStateNew(Long packageId, BatchPackageState stateNew) {
        return executeUpdate("update BatchPackage p set p.packageState = ?1 where p.id = ?2 and p.packageState <> ?3 "
                , stateNew, packageId, stateNew);
    }

    public int updatePackageStateProcessed(Long packageId, BatchPackageState stateNew, Date processDate) {
        return executeUpdate("update BatchPackage p set p.packageState = ?1, p.processDate = ?3 where p.id = ?2"
                , stateNew, packageId, processDate);
    }

    public List<BatchPosting> getPostingsByPackage(Long packageId) {
        return select(BatchPosting.class, "FROM BatchPosting p WHERE p.packageId = ?1 and p.invisible = ?2 ORDER BY p.id",
                packageId, InvisibleType.N);
    }

    public List<BatchPosting> getPostingsByPackageWithStatus(Long packageId, BatchPostStatus status) {
        return select(BatchPosting.class, "FROM BatchPosting p WHERE p.packageId = ?1 and p.invisible = ?2 and p.status = ?3 ORDER BY p.id",
                packageId, InvisibleType.N, status);
    }

    public void updatePostingsStatus(Long packageId, BatchPostStatus statusNew, String statusIn ) {
        executeNativeUpdate("update GL_BATPST p set p.STATE = ?" +
                " where p.ID_PKG = ? and p.INVISIBLE = ? and p.STATE in (" + statusIn + ")",
                statusNew.name(), packageId, InvisibleType.N.name());
    }

    public void updatePostingsStatusChange(Long packageId, Date timestamp, String userName, BatchPostStatus statusNew, String statusIn ) {
        executeNativeUpdate("update GL_BATPST p set p.STATE = ?, p.OTS_CHNG = ?, p.USER_CHNG = ?" +
                " where p.ID_PKG = ? and p.INVISIBLE = ? and p.STATE in (" + statusIn + ")",
                statusNew.name(), timestamp, userName, packageId, InvisibleType.N.name());
    }

    public int signedPostingsStatus(Long packageId, BatchPackageState pkgStateOld , Date timestamp, String userName, BatchPostStatus statusNew, String statusIn) {
        executeNativeUpdate("update GL_BATPST p set p.STATE = ?, p.OTS_AU2 = ?, p.USER_AU2 = ?" +
                " where p.ID_PKG = ? and p.INVISIBLE = ? and p.STATE in (" + statusIn + ")",
                statusNew.name(), timestamp, userName, packageId, InvisibleType.N.name());
        return updatePackageState(packageId, BatchPackageState.getStateByPostingStatus(statusNew), pkgStateOld);
    }

    public int confirmPostingsStatus (Long packageId, BatchPackageState pkgStateOld , Date timestamp, String userName, BatchPostStatus statusNew, String statusIn) {
        executeNativeUpdate("update GL_BATPST p set p.STATE = ?, p.OTS_AU3 = ?, p.USER_AU3 = ?" +
                " where p.ID_PKG = ? and p.INVISIBLE = ? and p.STATE in (" + statusIn + ")",
                statusNew.name(), timestamp, userName, packageId, InvisibleType.N.name());
        return updatePackageState(packageId, BatchPackageState.getStateByPostingStatus(statusNew), pkgStateOld);
    }

    public int signedConfirmPostingsStatus(Long packageId, BatchPackageState pkgStateOld , Date timestamp, String userName, BatchPostStatus statusNew, String statusIn) {
        executeNativeUpdate("update GL_BATPST p set p.STATE = ?, p.OTS_AU2 = ?, p.USER_AU2 = ?, p.OTS_AU3 = ?, p.USER_AU3 = ?" +
                        " where p.ID_PKG = ? and p.INVISIBLE = ? and p.STATE in (" + statusIn + ")",
                statusNew.name(), timestamp, userName, timestamp, userName, packageId, InvisibleType.N.name());
        return updatePackageState(packageId, BatchPackageState.getStateByPostingStatus(statusNew), pkgStateOld);
    }

    public int refusePackageStatus (Long packageId, BatchPackageState pkgStateOld , Date timestamp, String userName, String reason, BatchPostStatus status) {
        executeUpdate("update BatchPosting p set p.reasonOfDeny = ?1, p.status = ?2, p.changeTimestamp = ?3, p.changeName = ?4 " +
                        " where p.packageId = ?5  and p.invisible = ?6",
                reason, status, timestamp, userName, packageId, InvisibleType.N);
        return updatePackageState(packageId, BatchPackageState.getStateByPostingStatus(status), pkgStateOld);
    }

    public void setPackageInvisible (Long packageId, Date timestamp, String userName) {
        executeUpdate("update BatchPosting p set p.invisible = ?1, p.changeTimestamp = ?2, p.changeName = ?3 " +
                " where p.packageId = ?4 and p.invisible = ?5",
                InvisibleType.U, timestamp, userName, packageId, InvisibleType.N);
        executeUpdate("update BatchPackage p set p.packageState = ?1 where p.id = ?2",
                BatchPackageState.DELETED, packageId);
    }

    /**
     * Удаляет пакет запросов на операцию из таблицы
     */
    public void deletePackage(Long packageId) {
        executeUpdate("delete from BatchPosting p where p.packageId = ?1", packageId);
        executeUpdate("delete from BatchPackage p where p.id = ?1", packageId);
    }

    public void setPackagePostingDate(Long packageId, Date postDate) {
        executeUpdate("update BatchPosting p set p.postDate = ?1 where p.packageId = ?2  and p.invisible = ?3",
                postDate, packageId, InvisibleType.N);
        executeUpdate("update BatchPackage p set p.postDate = ?1 where p.id = ?2",
                postDate, packageId);
    }

    /**
     * Создает историческую запись для пакета запросов на операцию
     * @return
     * @throws SQLException
     */
    public BatchPackage createPackageHistory(BatchPackage pkg) throws SQLException {
        String fields = BatchPostingRepository.histfields;
        executeNativeUpdate("UPDATE GL_BATPST SET ID_PAR = ID WHERE ID_PKG = ? and INVISIBLE = ?", pkg.getId(), InvisibleType.N.name());
        executeNativeUpdate("" +
                "INSERT INTO GL_BATPST (INVISIBLE, ID_PAR, " + fields + ")" +
                " SELECT 'H', ID_PAR, " + fields + " FROM GL_BATPST WHERE ID_PKG = ? and INVISIBLE = ?",
                pkg.getId(), InvisibleType.N.name());
        return findById(pkg.getId());
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

    public List<Long> getPostingsControllable(Long packegeId, int maxRows) throws SQLException {
        List<DataRecord> res = selectMaxRows("select ID from V_GL_BATPST_CTRL where ID_PKG = ? and (CTRL_DR = 'Y' or CTRL_CR = 'Y' )" +
                " and INVISIBLE = 'N' and STATE = 'SIGNEDVIEW'", maxRows, new Object[]{packegeId});
        return res.stream().map(r -> r.getLong(0)).collect(Collectors.toList());
    }

    public List<Long> getPackagesWaitdate(Date operday) throws SQLException {
        List<DataRecord> res = select("select distinct ID_PKG from GL_BATPST where not ID_PKG is null and PROCDATE = ?" +
                " and INVISIBLE = 'N' and STATE = ?", operday, WAITDATE.name());
        return res.stream().map(r -> r.getLong(0)).collect(Collectors.toList());
    }

    public List<Long> getPackagesSigned(Date operday) throws SQLException {
        List<DataRecord> res = select("select distinct ID_PKG from GL_BATPST where not ID_PKG is null and PROCDATE = ?" +
                " and INVISIBLE = 'N' and STATE in ('SIGNED', 'SIGNEDDATE')", operday);
        return res.stream().map(r -> r.getLong(0)).collect(Collectors.toList());
    }

    public List<Long> getPackagesReceiveSrv(Date operday) throws SQLException {
        List<DataRecord> res = select("select ID_PKG from GL_BATPKG g where PROCDATE = ? and STATE = ? and not exists" +
                "(select 1 from GL_BATPST p where g.ID_PKG = p.ID_PKG and INVISIBLE = 'N' and p.STATE = ? )",
                operday, ON_WAITSRV.name(), WAITSRV.name());
        return res.stream().map(r -> r.getLong(0)).collect(Collectors.toList());
    }

    public DataRecord getPackageStatistics(long packageId) throws SQLException {
        DataRecord res = selectOne("select * from V_GL_BATPKG_STAT where ID_PKG = ?", packageId);
        return res;
    }

    public int getMovementCount(Long packageId) throws SQLException {
        DataRecord res = selectFirst("select count(1) from GL_BATPST " +
                "where ID_PKG = ? and INVISIBLE = ? and ((not SRV_REF is null and not STATE in (?, ?)) or STATE = ?)",
                packageId, InvisibleType.N.name(), REFUSESRV.name(), ERRSRV.name(), TIMEOUTSRV.name());
        return (null == res ? 0 : res.getInteger(0));
    }

    /**
     * загружаем в "грязном" режиме чтоб не висеть если процесс загрузки еще не завершил свою работу
     * @return
     */
    public List<Long> getPackagesForProcessing(int packageCount, Date curdate) {
        try {
            List<DataRecord> res = selectMaxRows("SELECT * FROM GL_BATPKG WHERE STATE in (?, ?) AND PROCDATE = ?  " +
                            "and ID_PKG not in (select ID_PKG from GL_BATPST where not ID_PKG is null and POSTDATE > ?) " +
                            "ORDER BY ID_PKG"
                    , packageCount, new Object[]{ IS_SIGNED.name(), IS_SIGNEDDATE.name(), curdate, curdate});
            return res.stream().map(r -> r.getLong(0)).collect(Collectors.toList());
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public String getPackagesStatistics(Date curdate) {
        try {
            List<DataRecord> listRec = select("select ID_PKG, DT_LOAD, POSTDATE from GL_BATPKG " +
                        "where STATE in (?, ?) and PROCDATE = ? order by ID_PKG"
                    , IS_SIGNED.name(), IS_SIGNEDDATE.name(), curdate);
            StringBuilder builder = new StringBuilder();
            for (DataRecord rec : listRec) {
                builder.append(format("ID_PKG: '%s'; DT_LOAD: '%s'; POSTDATE: '%s'# \n",
                        rec.getString(0), dateTime.format(rec.getDate(1)), onlyDate.format(rec.getDate(2))));
            }
            return builder.toString();
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }


}
