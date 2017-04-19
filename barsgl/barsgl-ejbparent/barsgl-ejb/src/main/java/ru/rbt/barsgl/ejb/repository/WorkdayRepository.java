package ru.rbt.barsgl.ejb.repository;

import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.shared.enums.Repository;

import java.sql.SQLException;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov
 */
public class WorkdayRepository extends AbstractBaseEntityRepository {

    public Date getWorkday() {
        try {
            return selectOne("select w.workday from workday w").getDate("workday");
        } catch (SQLException e) {
            throw new DefaultApplicationException("Failed getting Midas WORKDAY", e);
        }
    }

    public int setGlWorkday(Date newDate){
        try {
            return setWorkday(newDate, Repository.BARSGL);
        } catch (Exception e) {
            throw new DefaultApplicationException("Failed setting Midas WORKDAY in BarsGl", e);
        }
    }

    public int setRepWorkday(Date newDate){
        try {
            return setWorkday(newDate, Repository.BARSREP);
        } catch (Exception e) {
            throw new DefaultApplicationException("Failed setting Midas WORKDAY in BarsRep", e);
        }
    }

    public int setWorkday(Date newDate, Repository repository) throws Exception {
        return executeNativeUpdate( getPersistence(repository), "update workday set workday = ?", newDate);
    }

}
