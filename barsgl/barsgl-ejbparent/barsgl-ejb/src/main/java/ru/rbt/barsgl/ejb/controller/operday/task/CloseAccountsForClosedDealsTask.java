package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejbcore.DefaultApplicationException;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.Properties;

import static ru.rbt.audit.entity.AuditRecord.LogCode.CloseAccountsForClosedDeals;
import static ru.rbt.ejbcore.util.DateUtils.dbDateString;

/**
 * Created by er22317 on 19.03.2018.
 */
public class CloseAccountsForClosedDealsTask implements ParamsAwareRunnable {
    @EJB
    private AuditController auditController;
    @EJB
    private BeanManagedProcessor beanManagedProcessor;
    @Inject
    private OperdayController operdayController;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        Date dateLoad = operdayController.getOperday().getCurrentDate();
        try {
            auditController.info(CloseAccountsForClosedDeals, this.getClass().getSimpleName() + " стартовала в режиме за дату " + dbDateString(dateLoad));
            executeWork(dateLoad);
        }catch (Throwable e){
            auditController.error(CloseAccountsForClosedDeals,"Завершение с ошибкой", null, e);
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private void executeWork(Date dateLoad) throws Exception {
        //delete processed deals
        beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
            try (PreparedStatement query = connection.prepareStatement("DELETE FROM GL_DEALCLOSE d where exists(select 1 from GL_DEALCLOSE_H h where d.cnum=h.cnum and d.source=h.source and d.dealid=h.dealid and d.subdealid=h.subdealid)");) {
                query.execute();
            }
            return 1;
        }), 60 * 60);

        beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
            try (PreparedStatement query = connection.prepareStatement("select a.bsaacid from GL_DEALCLOSE d left join gl_acc a on a.dealid=d.dealid and a.subdealid=d.subdealid and a.custno=d.cnum and a.dtc is null order by d.dealid");
                 ResultSet rec = query.executeQuery()) {
                String bsaacid = rec.getString("bsaacid");
                if (bsaacid == null){
                    moveToHistory();
                }
            }
            return 1;
        }), 60 * 60);

    }

    void moveToHistory(){

    }
}