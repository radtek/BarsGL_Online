package ru.rbt.barsgl.ejb.common.controller.od;

import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.shared.operday.COB_OKWrapper;

import javax.ejb.EJB;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by akichigi on 28.03.17.
 */
public class COB_OK_Controller {
    @EJB
    private CoreRepository repository;

    private final String sql =
            "select state, reason from V_GLA_COB_OK";

    private final String vip_sql =
            //"select  is_vip, cnt from  V_GLA2_ERRORS_Test fetch first 2 rows only"; //test
           "select is_vip, count(1) cnt from v_gla2_errors group by is_vip fetch first 2 rows only";

    public COB_OKWrapper getData() throws SQLException {
        COB_OKWrapper wrapper = new COB_OKWrapper();
        DataRecord data = repository.selectFirst(sql);
        wrapper.setState(data == null ? null : data.getInteger("state"));
        //wrapper.setState(0); //test
        wrapper.setReason(data == null ? null : data.getString("reason"));

        List<DataRecord> vip_data = repository.select(vip_sql);

        wrapper.setVipCount(null);
        wrapper.setNotVipCount(null);

        if (vip_data != null && vip_data.size() != 0){
            for (DataRecord rec: vip_data){
                if (rec.getInteger("is_vip") == 1){
                    wrapper.setVipCount(rec.getInteger("cnt"));
                } else {
                    wrapper.setNotVipCount(rec.getInteger("cnt"));
                }
            }
        }

        return wrapper;
    }
}
