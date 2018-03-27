package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountController;
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

import static ru.rbt.audit.entity.AuditRecord.LogCode.AccDealCloseTask;
import static ru.rbt.ejbcore.util.DateUtils.dbDateString;
import static ru.rbt.ejbcore.util.DateUtils.getFinalDate;

/**
 * Created by er22317 on 19.03.2018.
 */
public class CloseAccountsForClosedDealsTask extends CloseAccountsForClosedDealsIterate implements ParamsAwareRunnable {
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
    @EJB
    private GLAccountController glAccountController;

    Date dateLoad;
    int cntProcessedDeals, cntClosedAcc, cntWaitAcc;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        dateLoad = operdayController.getOperday().getCurrentDate();
        try {
            auditController.info(AccDealCloseTask, this.getClass().getSimpleName() + " стартовала за дату " + dbDateString(dateLoad));
            beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {closeAccountsRepository.delOldDeals();
                return 1;
            }), 60 * 60);
            if (checkRun()) {
                executeWork();
                auditController.info(AccDealCloseTask, "Обработано закрытых сделок в количестве "+cntProcessedDeals+
                                                                ", закрыто счетов "+cntClosedAcc+", в листе ожидания счетов " + cntWaitAcc);
            }
        }catch (Throwable e){
            auditController.error(AccDealCloseTask,"Завершение с ошибкой", null, e);
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public boolean checkRun() throws Exception {
        if (!closeAccountsRepository.isExistsDeals()) {
            auditController.info(AccDealCloseTask, "Нет сделок для закрытия счетов (таблица GL_DEALCLOSE пустая)");
            return false;
        }
        return true;
    }

    private void executeWork() throws Exception {
        beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
            try{
                init(connection);
                while(next()){
                    if (!getAccounts().isEmpty()) {
                        for(GLAccount item: getAccounts()) {
                            closeAccount(item);
                        }
                    }
                    closeAccountsRepository.moveToHistory( getCnum(), getDealid(), getSubdealid(), getSource());
                    cntProcessedDeals++;
                }
            }finally {
                close();
            }
            return 1;
        }), 60 * 60);
    }

    void closeAccount(GLAccount glAccount) throws Exception {
        if (glAccountRepository.isAccountBalanceZero(glAccount.getBsaAcid(), glAccount.getAcid(), getFinalDate())){
            glAccountController.closeGLAccountDeals(glAccount,
                                                    dateLoad.compareTo(glAccount.getDateRegister())==0?glAccount.getDateOpen():dateLoad,
                                                    GLAccount.CloseType.Normal);
            cntClosedAcc++;
        }else{
            closeAccountsRepository.moveToWaitClose( glAccount, dateLoad, GLAccount.CloseType.Normal);
            cntWaitAcc++;
        }
    }
}