package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadFullTask;
import ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadParams;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.CLOSED;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.*;
import static ru.rbt.barsgl.ejbtest.utl.Utl4Tests.cleanHeader;
import static ru.rbt.barsgl.shared.enums.DealSource.KondorPlus;

/**
 * Created by ER18837 on 18.06.15.
 * Выгрузка данных о проводках в DWH
 * @fsd 8.2
 */
@Ignore("задача не выполняется на проде")
public class DwhUnloadIT extends AbstractTimerJobIT {

    public static final Logger logger = Logger.getLogger(DwhUnloadIT.class.getName());

    @BeforeClass
    public static void beforeClass() throws SQLException {
        DataRecord record = baseEntityRepository.selectFirst("select * from DWH_CLOSED_DEALS_STATUS");
    }


    @Test
    public void testCloseDeals() {

    }

}
