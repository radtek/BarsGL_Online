package ru.rbt.barsgl.ejb.controller.operday.task;

import org.apache.commons.lang3.time.DateUtils;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.OperdayRepository;
import ru.rbt.barsgl.ejb.controller.operday.task.stamt.StamtUnloadController;
import ru.rbt.barsgl.ejb.controller.operday.task.stamt.UnloadStamtParams;
import ru.rbt.barsgl.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.barsgl.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.Assert;
import ru.rbt.barsgl.shared.enums.EnumUtils;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.COB;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.audit.entity.AuditRecord.LogCode.DwhUnloadPosting;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.ALREADY_UNLOADED;
import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.OPERDAY_STATE_INVALID;

/**
 * Created by ER18837 on 18.06.15.
 */
public class DwhUnloadFullTask implements ParamsAwareRunnable {

    public static final String DWH_UNLOAD_FULL_DATE_KEY = "operday";
    public static final String DWH_UNLOAD_FULL_CHECK_RUN = "checkRun";

    @Inject
    private OperdayController operdayController;

    @Inject
    private OperdayRepository operdayRepository;

    @EJB
    private AuditController auditController;

    @Inject
    private ru.rbt.barsgl.ejbcore.util.DateUtils dateUtils;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @Inject
    private StamtUnloadController unloadController;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        Date executeDate = getExecuteDate(properties);
        beanManagedProcessor.executeInNewTxWithDefaultTimeout(((persistence, connection) -> {
            if (checkRun(executeDate, properties)) {
                auditController.info(DwhUnloadPosting, format("Запуск задачи полной выгрузки проводок в DWH: ОД '%s'"
                        , dateUtils.onlyDateString(executeDate)));
                try {
                    operdayRepository.executeNativeUpdate("call GL_DWHLD_F(?)", executeDate);
                    auditController.info(DwhUnloadPosting, format("ПОЛНАЯ выгрузка проводок в DWH прошла успешно: ОД '%s'"
                            , dateUtils.onlyDateString(executeDate)));
                } catch (Throwable e) {
                    auditController.error(DwhUnloadPosting, format("Ошибка при ПОЛНОЙ выгрузка проводок в DWH: ОД '%s'"
                            , dateUtils.onlyDateString(executeDate)), null, e);
                }
            }
            return null;
        }));
    }

    public boolean checkRun(Date executeDate, Properties properties) throws Exception {
        try {
            if (Boolean.parseBoolean(Optional.ofNullable(
                    properties.getProperty(DWH_UNLOAD_FULL_CHECK_RUN)).orElse("true"))) {

                Assert.isTrue(TaskUtils.getDwhAlreadyHeaderCount(executeDate, DwhUnloadParams.UnloadFullPostings, operdayRepository)  == 0
                        , () -> new ValidationError(ALREADY_UNLOADED
                                , format("Выгрузка проводок невозможна: выгрузка уже запущена или выполнена в текущем ОД (%s)", dateUtils.onlyDateString(executeDate))));

                Operday.OperdayPhase[] phases = new Operday.OperdayPhase[] {COB, ONLINE};
                Assert.isTrue(EnumUtils.contains(phases, operdayController.getOperday().getPhase())
                        , () -> new ValidationError(OPERDAY_STATE_INVALID, operdayController.getOperday().getPhase().name(), Arrays.toString(phases)));
            }
            return true;
        } catch (ValidationError e) {
            auditController.warning(DwhUnloadPosting, format("Полная выгрузка проводок для DWH в ОД '%s' невозможна"
                    , dateUtils.onlyDateString(executeDate)), null, e);
            return false;
        }
    }

    private Date getExecuteDate(Properties properties) throws ParseException, SQLException {
        return TaskUtils.getDateFromGLOD(properties, operdayRepository, operdayController.getOperday());
    }

}
