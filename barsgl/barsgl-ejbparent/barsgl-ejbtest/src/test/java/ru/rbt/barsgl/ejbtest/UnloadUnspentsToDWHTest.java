package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadFullTask;
import ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadParams;
import ru.rbt.barsgl.ejb.controller.operday.task.RecalcBS2Task;
import ru.rbt.barsgl.ejb.controller.operday.task.UnloadUnspentsToDWHServiceTask;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbcore.util.StringUtils;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.enums.OperState;

import javax.ejb.EJB;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.CLOSED;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.COB;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejbtest.utl.Utl4Tests.cleanHeader;

/**
 * Created by ER22228
 * Выгрузка данных о проводках и остатках в DWH
 * @fsd 8.2
 */
public class UnloadUnspentsToDWHTest extends AbstractTimerJobTest {

    public static final Logger logger = Logger.getLogger(UnloadUnspentsToDWHTest.class.getName());

    @Test
    public void testFull() throws Exception {
        SingleActionJob job = SingleActionJobBuilder.create()
                .withClass(UnloadUnspentsToDWHServiceTask.class)
                                  .withProps("#minDay=2016-02-18\n#maxDay=2016-03-18\ncheckRun=false")
                                  .build();
        jobService.executeJob(job);
    }
}
