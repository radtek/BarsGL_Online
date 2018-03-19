package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.repository.CloseAccountsRepository;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountCloseTask;
import static ru.rbt.ejbcore.util.DateUtils.dbDateString;

/**
 * Created by er22317 on 19.03.2018.
 */
public class CloseAccountsForClosedDealsTask implements ParamsAwareRunnable {
    @EJB
    private AuditController auditController;
    @EJB
    private BeanManagedProcessor beanManagedProcessor;
    @EJB
    private CloseAccountsRepository closeAccountsRepository;
    @Inject
    private OperdayController operdayController;
    @Inject
    private GLAccountRepository glAccountRepository;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        Date dateLoad = operdayController.getOperday().getCurrentDate();
        try {
            auditController.info(AccountCloseTask, this.getClass().getSimpleName() + " стартовала за дату " + dbDateString(dateLoad));
            if (checkRun()) executeWork(dateLoad);
        }catch (Throwable e){
            auditController.error(AccountCloseTask,"Завершение с ошибкой", null, e);
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public boolean checkRun() throws Exception {
        closeAccountsRepository.delOldDeals();
        long cnt = closeAccountsRepository.countDeals();
        if (cnt == 0) {
            auditController.info(AccountCloseTask, "Нет сделок для закрытия счетов (таблица GL_DEALCLOSE пустая)");
            return false;
        }else{
            auditController.info(AccountCloseTask, "Начало обработки закрытых сделок в количестве "+cnt);
        }
        return true;
    }

    private void executeWork(Date dateLoad) throws Exception {
        beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
            try (CloseAccountsForClosedDealsIterate rec = new CloseAccountsForClosedDealsIterate(connection)) {
                while(rec.next()){
                    if (!rec.getAccounts().isEmpty()) rec.getAccounts().forEach(item->closeAccounts(item));
                    closeAccountsRepository.moveToHistory( rec.getCnum(), rec.getDealid(), rec.getSubdealid(), rec.getSource());
                }
            }
            return 1;
        }), 60 * 60);
    }

    void closeAccounts(DataRecord accounts){
//        glAccountRepository.getAccountBalance(accounts.);
    }
}