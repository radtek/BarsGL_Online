package ru.rbt.barsgl.ejb.common.controller.od;

import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.shared.operday.COB_OKWrapper;

import javax.ejb.EJB;
import java.sql.SQLException;

/**
 * Created by akichigi on 28.03.17.
 */
public class COB_OK_Controller {
    @EJB
    private CoreRepository repository;

    private final String sql =
            "select state, reason " +
                    "from V_GLA_COB_OK";

    public COB_OKWrapper getData() throws SQLException {
        COB_OKWrapper wrapper = new COB_OKWrapper();
        DataRecord data = repository.selectFirst(sql);
        wrapper.setState(data == null ? null : data.getInteger("state"));
        wrapper.setReason(data == null ? null : data.getString("reason"));
        return wrapper;
    }
}
