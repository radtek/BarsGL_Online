package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.repository.BranchDictRepository;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.inject.Provider;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.LoadBranchDict;
import static ru.rbt.barsgl.ejb.controller.operday.task.LoadBranchDictTask.MODE.Auto;
import static ru.rbt.barsgl.ejb.controller.operday.task.LoadBranchDictTask.MODE.Manual;
import static ru.rbt.ejbcore.util.DateUtils.dbDateString;
import static ru.rbt.ejbcore.validation.ErrorCode.OPERDAY_TASK_ALREADY_EXC;

/**
 * Created by er22317 on 08.02.2018.
 */
public class LoadBranchDictTask extends AbstractJobHistoryAwareTask {
    public static final String STREAM_ID = "DWH_BRANCH_LOAD";
    public static final String PROP_OPERDAY = "operday";
    public static final String PROP_LOADDATE = "LOADDATE";
    public static final String PROP_MAXLOADDATE = "MAXLOADDATE";

    public enum MODE  {
        Manual("ручном"), Auto("автоматическом");
        private final String value;
        public String getValue(){return value;};
        MODE(String value) {
            this.value = value;
        }
    }

    private enum LoadState {
        N,P,E
    }

    @Inject
    private OperdayController operdayController;

    @Inject
    private LoadDictFil loadDictFil;

    @Inject
    private LoadDictBr loadDictBr;

    @EJB
    private AuditController auditController;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @EJB
    private BranchDictRepository branchDictRepository;

    @Override
    protected void initExec(String jobName, Properties properties) throws Exception{
        Date maxLoadDate = branchDictRepository.getMaxLoadDate();
        properties.put(PROP_MAXLOADDATE, maxLoadDate);
        properties.put(PROP_LOADDATE, getDateLoad(properties, maxLoadDate));
    }

    @Override
    protected boolean checkOk(String jobName, Properties properties) {
        if (getMode(properties).equals(MODE.Auto)){
            return super.checkOk(jobName, properties);
        }
        return true;
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        Date maxLoadDate = (Date)properties.get(PROP_MAXLOADDATE);
        Date dateLoad = (Date)properties.get(PROP_LOADDATE);

        if (getMode(properties).equals(Auto)){
            if (branchDictRepository.isTaskProcessed(operdayController.getOperday().getLastWorkingDay())) {
                auditController.info(LoadBranchDict, String.format("LoadBranchDictTask за LWDATE = %s уже успешно отработала", dbDateString(dateLoad)));
                return false;
            }
            if (branchDictRepository.isTaskProcessed(maxLoadDate)){
                auditController.info(LoadBranchDict, String.format("LoadBranchDictTask за MAX_LOAD_DATE = %s уже успешно отработала", dateUtils.onlyDateString(maxLoadDate)));
                return false;
            }
        }else if (getMode(properties).equals(Manual)){
            if (dateLoad.compareTo(maxLoadDate) > 0) {
                auditController.info(LoadBranchDict, String.format("LoadBranchDictTask MAX_LOAD_DATE(%s) меньше параметра(%s)", dbDateString(maxLoadDate), dbDateString(dateLoad)));
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        clearInfTables();
        long loadStatId = -1;
        try {
            Date dateLoad = (Date)properties.get(PROP_LOADDATE);
            auditController.info(LoadBranchDict, format("LoadBranchDictTask стартовала в '%s' режиме за дату '%s'", getMode(properties).getValue(), dateUtils.onlyDateString(dateLoad)));

            loadStatId = branchDictRepository.insGlLoadStat((Date)properties.get(PROP_MAXLOADDATE), dateLoad);

            processDic(loadDictFil, dateLoad, loadStatId);
            processDic(loadDictBr, dateLoad, loadStatId);

            branchDictRepository.updGlLoadStat(loadStatId, LoadState.P.name());
            auditController.info(LoadBranchDict, format("LoadBranchDictTask окончилась. Идентификатор загрузки '%s'", loadStatId));

            return true;
        } catch (Exception e) {
            auditController.error(LoadBranchDict, "Завершение с ошибкой", null, e);
            if (loadStatId > 0) {
                branchDictRepository.updGlLoadStat(loadStatId, LoadState.E.name());
            }
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private boolean processDic(LoadDict loadDict, Date dateLoad, long loadStatId) {
        try {
            beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
                loadDict.fillTargetTables(dateLoad, loadStatId);
                return null;
            }, 60 * 60);
            return true;
        } catch (Throwable e) {
            auditController.error(LoadBranchDict, String.format("LoadBranchDictTask(%s) завершилась с ошибкой", loadDict.getClass().getSimpleName()), "", String.valueOf(loadStatId), e);
            return false;
        }
    }

    private Date getDateLoad(Properties properties, Date maxDate) {
        try {
            return Optional.ofNullable(properties.getProperty(PROP_OPERDAY)).map(p -> {
                try {
                    return dateUtils.dbDateParse(p);
                } catch (ParseException e) {
                    throw new DefaultApplicationException(e);
                }
            }).orElse(maxDate);
        } catch (Exception e) {
            throw new DefaultApplicationException(e);
        }
    }

    private MODE getMode(Properties properties) {
        return Optional.ofNullable(properties.getProperty(PROP_OPERDAY)).map(p -> MODE.Manual).orElse(MODE.Auto);
    }

    private void clearInfTables() throws Exception {
        jobHistoryRepository.executeInNewTransaction(persistence -> {
            jobHistoryRepository.executeNativeUpdate("DELETE FROM DWH_IMBCBBRP_INF");
            jobHistoryRepository.executeNativeUpdate("DELETE FROM DWH_IMBCBCMP_INF");
            return null;
        });
    }

}
