package ru.rbt.barsgl.ejb.controller.operday.task.dem;

import org.apache.commons.lang3.time.DateUtils;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.operday.task.stamt.StamtUnloadController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.controller.etc.TextResourceController;

import javax.ejb.AccessTimeout;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.inject.Inject;
import java.sql.PreparedStatement;
import java.text.ParseException;
import java.util.Date;
import java.util.Properties;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MINUTES;
import static ru.rbt.audit.entity.AuditRecord.LogCode.AcccountBalanceOndemandUnload;

/**
 * Created by Ivan Sevastyanov on 25.01.2016.
 */
@Singleton
@AccessTimeout(value = 15, unit = MINUTES)
public class UniAccountBalanceUnloadTaskSupport {

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @EJB
    private AuditController auditController;

    @EJB
    private OperdayController operdayController;

    @EJB
    private CoreRepository repository;

    @Inject
    private ru.rbt.ejbcore.util.DateUtils dateUtils;

    @Inject
    private StamtUnloadController unloadController;

    @Inject
    private TextResourceController textResourceController;

    public boolean execute(Properties properties) throws Exception {
        final Date executeDate = getExecuteDate(properties);
        try {
            auditController.info(AcccountBalanceOndemandUnload
                , format("Начало выгрузки остатков по требованию за '%s'", dateUtils.onlyDateString(executeDate)));
            fillData(executeDate);
            return true;
        } catch (Throwable e) {
            auditController.error(AcccountBalanceOndemandUnload, format("Ошибка при выгрузке остатков по требованию за '%s'"
                , dateUtils.onlyDateString(executeDate)), null, e);
            return false;
        }
    }

    private Date getExecuteDate(Properties properties) throws ParseException {
        String propday = properties.getProperty("operday");
        if ((propday == null) || propday.isEmpty()) {
            return operdayController.getOperday().getCurrentDate();
        } else {
            return DateUtils.parseDate(propday, "dd.MM.yyyy", "yyyy-MM-dd");
        }
    }

    private void fillData(Date executeDate) throws Exception {
        repository.executeInNewTransaction((persistence) -> repository.executeTransactionally(connection -> {
            try (PreparedStatement updateCurdate = connection.prepareStatement("begin PKG_ACC_ONDEMAND.INS_ACCOUNTS(?); end;")) {
                updateCurdate.setDate(1, new java.sql.Date(executeDate.getTime()));
                updateCurdate.executeUpdate();
            }
            return null;
        }));
    }

}
