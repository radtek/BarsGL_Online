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
        if (checkRun(dateLoad, mode, isForceStart)) {
            try {
                if (!executeWork(dateLoad)) {
                    throw new DefaultApplicationException("Ошибка задачи");
                }
            } finally {
                taskUniqueController.setFree(TaskUniqueController.TaskId.LoadBranchDictTask);
            }
//                branchDictRepository.updGlLoadStat(_loadStatId, "P");
            auditController.info(LoadBranchDict, "LoadBranchDictTask окончилась", "", String.valueOf(_loadStatId));
//            }else {
//                auditController.info(LoadBranchDict, "LoadBranchDictTask отложена");
        }
        }catch (Throwable e){
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private boolean executeWork(Date dateLoad) throws Exception {
        clearInfTables();
//        auditController.info(LoadBranchDict, "LoadBranchDictTask тавлицы очищены", "", String.valueOf(_loadStatId));
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

            if (!taskUniqueController.Start(TaskUniqueController.TaskId.LoadBranchDictTask, forceStart)){
                auditController.info(LoadBranchDict, "LoadBranchDictTask за "+yyyyMMdd.format(dateLoad)+" уже запущена");
                return false;
            }

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

//    private void fillTargetTables(Date dateLoad) throws Exception {
//        StringBuilder s = new StringBuilder("000").append(String.valueOf(getFixBranchCode()));
//        String fixBranchCode = s.substring(s.length() - 3);
//        StringBuilder insFils = new StringBuilder();
//        StringBuilder updFils = new StringBuilder();
//
//        List<FilialsInf> filialsInf = branchDictRepository.tableToList(FilialsInf.class, "select CCPCD, CCPNE, CCPNR, CCPRI, CCBBR, ALT_CODE, VALID_FROM from V_GL_DWH_IMBCBCMP");
//        branchDictRepository.listToTable(filialsInf);
//        auditController.info(LoadBranchDict, "LoadBranchDictTask промежуточные тавлицы заполнены", "", String.valueOf(_loadStatId));
//
//        List<FilialsInf> filialsInfFix = new ArrayList<FilialsInf>();
//        filialsInf.stream().filter(item -> item.getAltCode().compareTo(fixBranchCode) > 0 && item.getValidFrom().compareTo(dateLoad) >= 0 )
//                           .forEach(item -> filialsInfFix.add(item) );
//
//        List<Filials> filials = branchDictRepository.getAll(Filials.class);
//        Collections.sort(filials);
//        for(FilialsInf item: filialsInfFix){
//            Optional<Filials> f = filials.stream().filter(x->item.getId().equals(x.getId())).findFirst();
//            if (!f.isPresent()){
//                //добавить в целевые
//                branchDictRepository.saveEntityNoFlash(new Filials(item.getId(), item.getCcpne(), item.getCcpnr(), item.getCcpri(), item.getCcbbr()));
//                branchDictRepository.nativeUpdate("insert into DH_BR_MAP(FCC_BRANCH, MIDAS_BRANCH, CBR_BRANCH) values (?,?,?)",
//                                                        new Object[]{item.getId(), item.getAltCode(), item.getCcbbr()});
//                insFils.append(item.getId()).append(" ");
//            }
//            //обновить целевые
//            else {
//                if (!item.getCcpne().equals(f.get().getCcpne()) ||
//                    !item.getCcpnr().equals(f.get().getCcpnr()) ||
//                    !item.getCcpri().equals(f.get().getCcpri()) ||
//                    !item.getCcbbr().equals(f.get().getCcbbr())){
//                    Filials filialsUpd = (Filials)branchDictRepository.findById(Filials.class, item.getId());
//                    filialsUpd.setCcpne(item.getCcpne());
//                    filialsUpd.setCcpnr(item.getCcpnr());
//                    filialsUpd.setCcpri(item.getCcpri());
//                    filialsUpd.setCcbbr(item.getCcbbr());
//                    branchDictRepository.jpaUpdateNoFlash(filialsUpd);
//                    branchDictRepository.nativeUpdate("update DH_BR_MAP set MIDAS_BRANCH=?, CBR_BRANCH=? where FCC_BRANCH =?",
//                                                            new Object[]{item.getAltCode(), item.getCcbbr(), item.getId()});
//                    updFils.append(item.getId()).append(" ");
//                }
//            }
//        }
//        branchDictRepository.flush();
//        auditController.info(LoadBranchDict, "LoadBranchDictTask "+(insFils.length()>0?"добавлены филиалы "+insFils:"")+ (updFils.length()>0?"обновлены филиалы "+updFils:""), "", String.valueOf(_loadStatId));
//    }

//    private void fillInfTables() throws Exception {
//        beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
//            String ablok = "DECLARE PRAGMA AUTONOMOUS_TRANSACTION;"+
//                           " BEGIN"+
//                           " INSERT INTO DWH_IMBCBCMP_INF (CCPCD, CCPNE, CCPNR, CCPRI, CCBBR, ALT_CODE, VALID_FROM) select CCPCD, CCPNE, CCPNR, CCPRI, CCBBR, ALT_CODE, VALID_FROM from V_GL_DWH_IMBCBCMP; "+
//                           " COMMIT;"+
//                           " END;";
//
//            try(CallableStatement cs = connection.prepareCall(ablok)){
//                cs.execute();
//            }
//            try (PreparedStatement query = connection.prepareStatement("INSERT INTO DWH_IMBCBBRP_INF (A8BRCD, A8LCCD, A8BICN, A8BRNM, BBRRI, BCORI, BCBBR, BR_HEAD, BR_OPER, FCC_CODE, VALID_FROM) select A8BRCD, A8LCCD, A8BICN, A8BRNM, BBRRI, BCORI, BCBBR, BR_HEAD, BR_OPER, FCC_CODE, VALID_FROM from V_GL_DWH_IMBCBBRP");
//                 PreparedStatement query2 = connection.prepareStatement("INSERT INTO DWH_IMBCBCMP_INF (CCPCD, CCPNE, CCPNR, CCPRI, CCBBR, ALT_CODE, VALID_FROM) select CCPCD, CCPNE, CCPNR, CCPRI, CCBBR, ALT_CODE, VALID_FROM from V_GL_DWH_IMBCBCMP")) {
//                query.execute();
//                query2.execute();
//            }
//            return 1;
//        }), 60 * 60);
//    }

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
