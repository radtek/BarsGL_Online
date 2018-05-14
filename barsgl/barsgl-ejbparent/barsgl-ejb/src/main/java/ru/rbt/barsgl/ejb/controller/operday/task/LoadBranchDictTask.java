package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.repository.BranchDictRepository;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.DefaultApplicationException;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.inject.Provider;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

import static ru.rbt.audit.entity.AuditRecord.LogCode.LoadBranchDict;
import static ru.rbt.barsgl.ejb.controller.operday.task.LoadBranchDictTask.MODE.Auto;
import static ru.rbt.barsgl.ejb.controller.operday.task.LoadBranchDictTask.MODE.Manual;

/**
 * Created by er22317 on 08.02.2018.
 */
public class LoadBranchDictTask implements ParamsAwareRunnable {
    public static final String streamId = "DWH_BRANCH_LOAD";
    public static final String propOperDay = "operday";
    public static final String propForceStart = "forcestart";
    long _loadStatId;
    SimpleDateFormat yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd");
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
//    @Inject
//    private Provider<BranchDictRepository> repositoryProvider;
    @EJB
    private BranchDictRepository branchDictRepository;
    @EJB
    private TaskUniqueController taskUniqueController;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
    try {
        Date dateLoad = new Date();
        MODE mode;

        boolean isForceStart = Optional.ofNullable(properties.getProperty(propForceStart)).orElse("N").equals("Y");
        String dl = properties.getProperty(propOperDay);

        if (dl == null){
            dateLoad.setTime(operdayController.getOperday().getLastWorkingDay().getTime());
            mode = Auto;
        }else{
            dateLoad.setTime(yyyyMMdd.parse(dl).getTime());
            mode = Manual;
        }
        auditController.info(LoadBranchDict, "LoadBranchDictTask стартовала в "+mode.getValue()+" режиме за дату "+yyyyMMdd.format(dateLoad));

        if (!taskUniqueController.Start(TaskUniqueController.TaskId.LoadBranchDictTask, isForceStart)){
            auditController.info(LoadBranchDict, "LoadBranchDictTask за "+yyyyMMdd.format(dateLoad)+" уже запущена");
        }else {
            try{
                if (checkRun(dateLoad, mode, isForceStart)) {
                    if (!executeWork(dateLoad)) {
                        throw new DefaultApplicationException("Ошибка задачи");
                    }
                }
            } finally {
                taskUniqueController.setFree(TaskUniqueController.TaskId.LoadBranchDictTask);
            }
        }
            auditController.info(LoadBranchDict, "LoadBranchDictTask окончилась", "", String.valueOf(_loadStatId));
        }catch (Throwable e){
            auditController.error(LoadBranchDict,"Завершение с ошибкой", null, e);
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private boolean executeWork(Date dateLoad) throws Exception {
        clearInfTables();
        ExecutorService pool = Executors.newFixedThreadPool(2);

        try{
            Future<Boolean> fuFils = pool.submit(task(loadDictFil, dateLoad, _loadStatId));
            Future<Boolean> fuBrchs = pool.submit(task(loadDictBr, dateLoad, _loadStatId));
            if (fuFils.get() & fuBrchs.get()){
                branchDictRepository.updGlLoadStat(_loadStatId, "P");
                return true;
            }else{
                branchDictRepository.updGlLoadStat(_loadStatId, "E");
                return false;
            }
        }finally {
            pool.shutdown();
        }
    }

    private Callable<Boolean> task(LoadDict loadDict, Date dateLoad, long _loadStatId){
        return ()->{
                    try {
                        beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
                            loadDict.fillTargetTables(dateLoad, _loadStatId);
                            return null;
                        }, 60 * 60);
                        return true;
                    }catch (Throwable e){
                        auditController.error(LoadBranchDict, "LoadBranchDictTask("+loadDict.getClass().getSimpleName()+") завершилась с ошибкой","", String.valueOf(_loadStatId), e);
                        return false;
                    }
                };
    }

    public boolean checkRun(Date dateLoad, MODE mode, boolean forceStart) throws Exception {
        return beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
            Date maxLoadDate = null;

            if (mode.equals(Auto)){
                //dateLoad = lwdate
                if (branchDictRepository.isTaskProcessed(dateLoad)){
                    auditController.info(LoadBranchDict, "LoadBranchDictTask за "+yyyyMMdd.format(dateLoad)+" уже успешно отработала");
                    return false;
                }
                maxLoadDate = branchDictRepository.getMaxLoadDate();
                if (branchDictRepository.isTaskProcessed(maxLoadDate)){
                    auditController.info(LoadBranchDict, "LoadBranchDictTask за MAX_LOAD_DATE = "+yyyyMMdd.format(maxLoadDate)+" уже успешно отработала");
                    return false;
                }
                _loadStatId = branchDictRepository.insGlLoadStat( maxLoadDate, maxLoadDate);
                dateLoad.setTime(maxLoadDate.getTime());
            }else{
                maxLoadDate = branchDictRepository.getMaxLoadDate();
                //dateLoad = параметер
                if (dateLoad.compareTo(maxLoadDate) > 0){
                    auditController.info(LoadBranchDict, "LoadBranchDictTask MAX_LOAD_DATE("+yyyyMMdd.format(maxLoadDate)+") меньше параметра(" +yyyyMMdd.format(dateLoad)+")");
                    return false;
                }
                _loadStatId = branchDictRepository.insGlLoadStat( maxLoadDate, dateLoad);
            }
            return true;
        }, 60 * 60);
    }

    private void clearInfTables() throws Exception {
        beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
            try (PreparedStatement query = connection.prepareStatement("DELETE FROM DWH_IMBCBBRP_INF");
                 PreparedStatement query2 = connection.prepareStatement("DELETE FROM DWH_IMBCBCMP_INF")) {
                query.execute();
                query2.execute();
            }
            return 1;
        }), 60 * 60);
    }

    public enum MODE  {

        Manual("ручном"), Auto("автоматическом");

        private final String value;
        public String getValue(){return value;};
        private MODE(String value) {
            this.value = value;
        }

    }
}
