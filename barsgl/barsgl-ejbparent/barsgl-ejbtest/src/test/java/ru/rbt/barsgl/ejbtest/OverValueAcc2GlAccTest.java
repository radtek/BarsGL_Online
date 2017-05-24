package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadParams;
import ru.rbt.barsgl.ejb.controller.operday.task.OverValueAcc2GlAccTask;
import ru.rbt.barsgl.ejb.controller.operday.task.TaskUtils;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.io.StringReader;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

/**
 * Created by ER22317 on 02.11.2016.
 */
public class OverValueAcc2GlAccTest extends AbstractTimerJobTest {
    final private static String stepName = "P9";

    @Test
    public void test() throws Exception {
        Properties properties = new Properties();
        properties.load(new StringReader(TaskUtils.CHECK_RUN_KEY + "=true\nstepName=" + stepName));
        Date executeDate = TaskUtils.getDateFromGLOD(properties, baseEntityRepository, getOperday());

        String sql = "delete from gl_etldwhs where pardesc = '"+DwhUnloadParams.UnloadOverValueAcc.getParamDesc()+"' and operday=?";    //+new SimpleDateFormat("yyyy-MM-dd").format(operDay)+"'";
        baseEntityRepository.executeNativeUpdate(sql, executeDate);

        emulateWorkprocStep(executeDate, stepName);

        SingleActionJobBuilder builder = SingleActionJobBuilder.create().withClass(OverValueAcc2GlAccTask.class).withProps(properties);
        jobService.executeJob(builder.build());
        checkHeadersSuccess(executeDate);
    }

    private static void emulateP9(Date operDay) throws Exception {
        int cnt = baseEntityRepository.selectFirst("select count(1) cnt from workproc where dat = ? and trim(id) = 'P9'"
                , operDay).getInteger(0);
        if (cnt == 1) {
            baseEntityRepository.executeNativeUpdate("update workproc set result = 'O' where dat = ? and trim(id) = 'P9'"
                    , operDay);
        } else {
            baseEntityRepository.executeNativeUpdate("insert into workproc  values (?, 'P9', systimestamp, systimestamp, 'O', 1, 'P9')"
                    , operDay);
        }

    }

    private static void checkHeadersSuccess(Date operDay) throws SQLException {
        DataRecord rec = baseEntityRepository.selectFirst("select count(1) cnt from GL_ETLDWHS where PARDESC = '"+ DwhUnloadParams.UnloadOverValueAcc.getParamDesc()
                +"' and parvalue = '1' and operday=?", operDay);

        Assert.assertEquals(1, (int)rec.getInteger("cnt"));
    }

}
