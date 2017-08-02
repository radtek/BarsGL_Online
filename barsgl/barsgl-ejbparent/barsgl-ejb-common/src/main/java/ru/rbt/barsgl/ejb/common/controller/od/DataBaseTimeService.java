package ru.rbt.barsgl.ejb.common.controller.od;

import ru.rbt.barsgl.ejb.common.repository.od.OperdayRepository;
import ru.rbt.ejbcore.DefaultApplicationException;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov
 */
public class DataBaseTimeService implements SystemTimeService {

    @Inject
    private OperdayRepository repository;

    @Override
    public Date getCurrentTime() {
        try {
            return repository.selectFirst("SELECT TO_TIMESTAMP(TO_CHAR(SYSTIMESTAMP, 'YYYY-MM-DD HH24:MI:SS.FF6')) TS FROM DUAL").getDate("TS");
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }
}
