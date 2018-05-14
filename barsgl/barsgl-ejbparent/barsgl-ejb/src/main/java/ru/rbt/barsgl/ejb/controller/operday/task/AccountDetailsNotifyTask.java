package ru.rbt.barsgl.ejb.controller.operday.task;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.OperdayRepository;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.AccountDetailsNotifyController;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejbcore.datarec.DataRecord;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.text.ParseException;
import java.util.Optional;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountDetailsNotify;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by er18837 on 03.05.2018.
 */
public class AccountDetailsNotifyTask implements ParamsAwareRunnable {

    private static final Logger log = Logger.getLogger(AccountDetailsNotifyTask.class);
    private static final String SCHEDULED_TASK_NAME = "AccountDetailsNotify";
    private static final int defaultQueueBachSize = 5;
    private static final String UU_DATE_KEY = "operday";
    private static final String UU_CHECK_RUN = "checkRun";

    @EJB
    private AccountDetailsNotifyController queueController;

    @EJB
    private AuditController auditController;

    @Inject
    private OperdayController operdayController;

    @Inject
    private OperdayRepository operdayRepository;

    @Inject
    private ru.rbt.ejbcore.util.DateUtils dateUtils;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        try {
            if (checkRun(operdayController.getOperday(), properties)) {
                queueController.process(properties);
            }
        } catch (Exception e) {
            log.error("AccountDetailsNotify #run ", e);
            auditController.error(AccountDetailsNotify, "Ошибка при выполнении задачи AccountDetailsNotifyTask", null, e);
        }
    }

    private java.util.Date getExecuteDate(Operday operday, Properties properties) throws ParseException {
        String propday = Optional.ofNullable(properties.getProperty(UU_DATE_KEY)).orElse("");
        java.util.Date execday;
        if (isEmpty(propday)) {
            execday = operday.getCurrentDate();
        } else {
            execday = DateUtils.parseDate(propday, "dd.MM.yyyy", "yyyy-MM-dd");
        }
//        auditController.info(AccountDetailsNotify, "operday: " + operday);
        return execday;
    }

    public boolean checkRun(Operday operday, Properties properties) throws Exception {
        if (Boolean.parseBoolean(Optional.ofNullable(properties.getProperty(UU_CHECK_RUN)).orElse("true"))) {
            java.util.Date executeDate = getExecuteDate(operday, properties);
            DataRecord rec = operdayRepository.selectFirst(
                    "select count (1) from GL_ETLDWHS where PARDESC = ? and OPERDAY = ?"
                    , SCHEDULED_TASK_NAME, executeDate);
            boolean already = 0 < rec.getInteger(0);
            if (already) {
                auditController.warning(AccountDetailsNotify, "Ошибка при открытии счетов из SRVACC", null
                        , format("Открытие счетов из SRVACC невозможна: уже запущена или выполнена в текущем ОД (%s) <%s>"
                                , dateUtils.onlyDateString(executeDate), true));
                return false;
            }
            return true;
        } else {
            return true;
        }
    }

}
