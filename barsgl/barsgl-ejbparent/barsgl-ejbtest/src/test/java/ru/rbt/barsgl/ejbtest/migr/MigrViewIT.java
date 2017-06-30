package ru.rbt.barsgl.ejbtest.migr;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejbtest.AbstractRemoteIT;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by er21006 on 30.06.2017.
 */
public class MigrViewIT extends AbstractRemoteIT {

    public static final Logger log = Logger.getLogger(MigrViewIT.class.getName());

    @Test public void test() throws SQLException {

        selectView("V_GL_ACC");
        selectView("V_GL_ACCRLN");
        selectView("V_GL_ACC_TH");
        selectView("V_GL_ACCUST_TH");
        selectView("V_GL_AU_ACT");
        selectView("V_GL_AU_USRBR");
        selectView("V_GL_AU_USRPR");
        selectView("V_GL_AU_USRRL");
        selectView("V_GL_BACKVALUE");
        selectView("V_GL_BATPST");
        selectView("V_GL_BATPST_TH");
        selectView("V_GL_CBCTP");
        selectView("V_GL_COB_STAT");
        selectView("V_GL_CUSTOMER");
        selectView("V_GL_ERRPROC");
    }

    private void selectView(String viewName) throws SQLException {
        boolean success = false;
        Throwable throwable = null;
        try {
            baseEntityRepository.selectFirst("select * from " + viewName);
            success = true;
        } catch (Throwable e) {
            throwable = e;
        }

        if (!success) {
            log.log(Level.WARNING, "error", throwable);
        }
        Assert.assertTrue(viewName, success);

    }
}
