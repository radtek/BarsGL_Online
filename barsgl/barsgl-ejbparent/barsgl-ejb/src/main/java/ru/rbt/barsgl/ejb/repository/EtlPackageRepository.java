package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.entity.etl.EtlPackage.PackageState.WORKING;

/**
 * Created by Ivan Sevastyanov
 */
public class EtlPackageRepository extends AbstractBaseEntityRepository<EtlPackage, Long> {
    private final SimpleDateFormat onlyDate = new SimpleDateFormat("dd.MM.yyyy");
    private final SimpleDateFormat dateTime = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    /**
     * загружаем в "грязном" режиме чтоб не висеть если процесс загрузки еще не завершил свою работу
     * @return
     */
    public List<DataRecord> getPackagesForProcessing(int packageCount, Date from, Date to, Date curdate) {
        try {
            return selectMaxRows("SELECT p.* FROM GL_ETLPKG p WHERE STATE = ? AND DT_LOAD >= ? AND DT_LOAD < ? " +
                    "and not exists (select ID_PKG from GL_ETLPST e where e.id_pkg=p.id_pkg and VDATE > ?) " +
                    "ORDER BY p.ID_PKG"
                    , packageCount, new Object[]{EtlPackage.PackageState.LOADED.name(), from, to, curdate});
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public void updatePackageStateProcessed(long etlPackageId, EtlPackage.PackageState state, Date processDate) {
        executeUpdate("update EtlPackage p set p.packageState = ?1, p.processDate = ?3 where p.id = ?2"
                , state, etlPackageId, processDate);
    }

    public void updatePackageInprogress(long etlPackageId) {
        executeUpdate("update EtlPackage p set p.packageState = ?1 where p.id = ?2"
                , WORKING, etlPackageId);
    }

    public String getPackageStatistics(Date from, Date to) {
        String sql = "select G.ID_PKG, G.DT_LOAD, P.VD from GL_ETLPKG G " +
                "join (select ID_PKG, max(VDATE) VD from GL_ETLPST group by ID_PKG, VDATE order by ID_PKG desc) P on G.ID_PKG = P.ID_PKG " +
                "where G.STATE = 'LOADED' and DT_LOAD >= ? and DT_LOAD < ? AND ROWNUM <= 10 order by ID_PKG";
        try {
            List<DataRecord> listRec = select(sql, from, to);
            StringBuilder builder = new StringBuilder();
            for (DataRecord rec : listRec) {
                builder.append(format("ID_PKG: '%s'; DT_LOAD: '%s'; max(VDATE): '%s'# \n",
                        rec.getString(0), dateTime.format(rec.getDate(1)), onlyDate.format(rec.getDate(2))));
            }
            return builder.toString();
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public Long nextId() {
        return nextId("GL_SEQ_PKG");
    }

}
