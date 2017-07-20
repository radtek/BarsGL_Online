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

    public void execute(Properties properties) throws Exception {
        final Date executeDate = getExecuteDate(properties);
        try {
            auditController.info(AcccountBalanceOndemandUnload
                , format("Начало выгрузки остатков по требованию за '%s'", dateUtils.onlyDateString(executeDate)));
            auditController.info(AcccountBalanceOndemandUnload, format("Удалено старых записей: '%s'", cleanOld()));
            auditController.info(AcccountBalanceOndemandUnload, format("Выгружено счетов с остатками по требованию за '%s': '%s'"
                , dateUtils.onlyDateString(executeDate), fillData(executeDate)));
        } catch (Throwable e) {
            auditController.error(AcccountBalanceOndemandUnload, format("Ошибка при выгрузке остатков по требованию за '%s'"
                , dateUtils.onlyDateString(executeDate)), null, e);
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

    private int fillData(Date executeDate) throws Exception {
        return (int) repository.executeInNewTransaction((persistence) -> {
            return repository.executeTransactionally(connection -> {
                int updateResult = 0;
                try (
                        PreparedStatement updateCurdate = connection.prepareStatement("INSERT INTO GL_TMP_CURDATE VALUES(?)");
                ) {
                    updateCurdate.setDate(1, new java.sql.Date(executeDate.getTime()));
                    updateCurdate.executeUpdate();
                    try (
                            PreparedStatement fillTableStmt = connection.prepareStatement(textResourceController
                                    .getContent("ru/rbt/barsgl/ejb/controller/operday/task/dem/uni_ins_sums.sql"));

                            PreparedStatement restStatement = connection.prepareStatement(textResourceController
                                    .getContent("ru/rbt/barsgl/ejb/controller/operday/task/dem/uni_ins_rest.sql"));

                    ) {
                        fillTableStmt.setDate(1, new java.sql.Date(executeDate.getTime()));
                        fillTableStmt.execute();
                        updateResult = restStatement.executeUpdate();
                    }
                }
                return updateResult;

            });
        });
    }

    private int cleanOld() throws Exception {
        return (int) repository.executeInNewTransaction(persistence -> {
            return repository.executeNativeUpdate("delete from gl_accrest");
        });

    }

}
