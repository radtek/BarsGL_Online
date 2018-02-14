package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.dict.dwh.Filials;
import ru.rbt.barsgl.ejb.entity.dict.dwh.FilialsInf;
import ru.rbt.barsgl.ejb.repository.BranchDictRepository;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.inject.Provider;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static ru.rbt.audit.entity.AuditRecord.LogCode.LoadBranchDict;
import static ru.rbt.barsgl.ejb.controller.operday.task.LoadBranchDictTask.MODE.Auto;
import static ru.rbt.barsgl.ejb.controller.operday.task.LoadBranchDictTask.MODE.Manual;

/**
 * Created by er22317 on 08.02.2018.
 */
public class LoadBranchDictTask implements ParamsAwareRunnable {
    public static final String streamId = "DWH_BRANCH_LOAD";
    public static final String propOperDay = "operday";
    long _loadStatId;
    SimpleDateFormat yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd");
    @Inject
    private OperdayController operdayController;
    @EJB
    private AuditController auditController;
    @EJB
    private BeanManagedProcessor beanManagedProcessor;
    @EJB
    private PropertiesRepository propertiesRepository;
//    @Inject
//    private Provider<BranchDictRepository> repositoryProvider;
    @EJB
    private BranchDictRepository branchDictRepository;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
    try {
        Date dateLoad = new Date();
        MODE mode;

        String dl = properties.getProperty(propOperDay);
        if (dl == null){
            dateLoad.setTime(operdayController.getOperday().getLastWorkingDay().getTime());
            mode = Auto;
        }else{
            dateLoad.setTime(yyyyMMdd.parse(dl).getTime());
            mode = Manual;
        }
        auditController.info(LoadBranchDict, "LoadBranchDictTask стартовала в "+mode.getValue()+" режиме за дату "+yyyyMMdd.format(dateLoad));
        beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
            if (checkRun(dateLoad, mode)) {
                executeWork(dateLoad);
                branchDictRepository.updGlLoadStat(_loadStatId, "P");
                auditController.info(LoadBranchDict, "LoadBranchDictTask окончилась", "", String.valueOf(_loadStatId));
            }else {
                auditController.info(LoadBranchDict, "LoadBranchDictTask отложена");
            }
            return null;
        }, 60 * 60);
        }catch (Throwable e){
            if (_loadStatId > 0){
                branchDictRepository.updGlLoadStat(_loadStatId, "E");
            }
            auditController.error(LoadBranchDict, "LoadBranchDictTask завершилась с ошибкой","", String.valueOf(_loadStatId), e);
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private void executeWork(Date dateLoad) throws Exception {
            clearInfTables();
            auditController.info(LoadBranchDict, "LoadBranchDictTask тавлицы очищены", "", String.valueOf(_loadStatId));
            fillTargetTables(dateLoad);
            auditController.info(LoadBranchDict, "LoadBranchDictTask целевые тавлицы обновлены", "", String.valueOf(_loadStatId));
    }

    public boolean checkRun(Date dateLoad, MODE mode) throws Exception {
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
    }

    private void fillTargetTables(Date dateLoad) throws Exception {
        StringBuilder s = new StringBuilder("000").append(String.valueOf(getFixBranchCode()));
        String fixBranchCode = s.substring(s.length() - 3);
        StringBuilder insFils = new StringBuilder();
        StringBuilder updFils = new StringBuilder();

        List<FilialsInf> filialsInf = branchDictRepository.tableToList(FilialsInf.class, "select CCPCD, CCPNE, CCPNR, CCPRI, CCBBR, ALT_CODE, VALID_FROM from V_GL_DWH_IMBCBCMP");
        branchDictRepository.listToTable(filialsInf);
        auditController.info(LoadBranchDict, "LoadBranchDictTask промежуточные тавлицы заполнены", "", String.valueOf(_loadStatId));

        List<FilialsInf> filialsInfFix = new ArrayList<FilialsInf>();
        filialsInf.stream().filter(item -> item.getAltCode().compareTo(fixBranchCode) > 0 && item.getValidFrom().compareTo(dateLoad) >= 0 )
                           .forEach(item -> filialsInfFix.add(item) );

        List<Filials> filials = branchDictRepository.getAll(Filials.class);
        Collections.sort(filials);
//        int i = 0;
        for(FilialsInf item: filialsInfFix){
            Optional<Filials> f = filials.stream().filter(x->item.getId().equals(x.getId())).findFirst();
            if (!f.isPresent()){
                //добавить в целевые
//            if ( (i = filials.indexOf(item)) < 0) {
                branchDictRepository.saveEntityNoFlash(new Filials(item.getId(), item.getCcpne(), item.getCcpnr(), item.getCcpri(), item.getCcbbr()));
                branchDictRepository.nativeUpdate("insert into DH_BR_MAP(FCC_BRANCH, MIDAS_BRANCH, CBR_BRANCH) values (?,?,?)",
                                                        new Object[]{item.getId(), item.getAltCode(), item.getCcbbr()});
                insFils.append(item.getId()).append(" ");
            }
            //обновить целевые
            else {
                if (!item.getCcpne().equals(f.get().getCcpne()) ||
                    !item.getCcpnr().equals(f.get().getCcpnr()) ||
                    !item.getCcpri().equals(f.get().getCcpri()) ||
                    !item.getCcbbr().equals(f.get().getCcbbr())){
//                    if (!item.getCcpne().equals(filials.get(i).getCcpne()) ||
//                            !item.getCcpnr().equals(filials.get(i).getCcpnr()) ||
//                            !item.getCcpri().equals(filials.get(i).getCcpri()) ||
//                            !item.getCcbbr().equals(filials.get(i).getCcbbr())){
                    Filials filialsUpd = (Filials)branchDictRepository.findById(Filials.class, item.getId());
                    filialsUpd.setCcpne(item.getCcpne());
                    filialsUpd.setCcpnr(item.getCcpnr());
                    filialsUpd.setCcpri(item.getCcpri());
                    filialsUpd.setCcbbr(item.getCcbbr());
                    branchDictRepository.jpaUpdateNoFlash(filialsUpd);
                    branchDictRepository.nativeUpdate("update DH_BR_MAP set MIDAS_BRANCH=?, CBR_BRANCH=? where FCC_BRANCH =?",
                                                            new Object[]{item.getAltCode(), item.getCcbbr(), item.getId()});
                    updFils.append(item.getId()).append(" ");
                }
            }
        }
        branchDictRepository.flush();
        auditController.info(LoadBranchDict, "LoadBranchDictTask "+(insFils.length()>0?"добавлены филиалы "+insFils:"")+ (updFils.length()>0?"обновлены филиалы "+updFils:""), "", String.valueOf(_loadStatId));
    }
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

    private int getFixBranchCode() {
        try {
            return propertiesRepository.getNumber("dwh.fix.branch.code").intValue();
        } catch (ExecutionException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
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
