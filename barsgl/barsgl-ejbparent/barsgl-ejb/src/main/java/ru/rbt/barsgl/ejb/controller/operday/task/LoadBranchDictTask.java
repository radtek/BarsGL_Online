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

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.inject.Provider;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static ru.rbt.audit.entity.AuditRecord.LogCode.LoadBranchDict;

/**
 * Created by er22317 on 08.02.2018.
 */
public class LoadBranchDictTask implements ParamsAwareRunnable {
    @Inject
    private OperdayController operdayController;
    @EJB
    private AuditController auditController;
    @EJB
    private BeanManagedProcessor beanManagedProcessor;
    @EJB
    private PropertiesRepository propertiesRepository;
    @Inject
    private Provider<BranchDictRepository> repositoryProvider;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        Date lwdate = operdayController.getOperday().getLastWorkingDay();

        auditController.info(LoadBranchDict, "LoadBranchDictTask стартовала");
        if (checkRun(lwdate, properties)) {
            executeWork(lwdate, properties);
            auditController.info(LoadBranchDict, "LoadBranchDictTask закончила");
        }else {
            auditController.info(LoadBranchDict, "LoadBranchDictTask отложена");
        }
    }

    private void executeWork(Date lwdate, Properties properties) throws Exception {
        int fixBranchCode = getFixBranchCode();

        clearInfTables();
        auditController.info(LoadBranchDict, "LoadBranchDictTask тавлицы очищены");
        fillInfTables();
        auditController.info(LoadBranchDict, "LoadBranchDictTask промежуточные тавлицы заполнены");
        fillTargetTables();
        auditController.info(LoadBranchDict, "LoadBranchDictTask целевые тавлицы обновлены");
    }

    public boolean checkRun(Date lwdate, Properties properties) throws Exception {
        return true;
    }

    private void fillTargetTables() throws Exception {
        List<FilialsInf> filsInf = repositoryProvider.get().getAll(FilialsInf.class);
        List<Filials> fils = repositoryProvider.get().getAll(Filials.class);
    }


    private void fillInfTables() throws Exception {
        beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
            String ablok = "DECLARE PRAGMA AUTONOMOUS_TRANSACTION;"+
                           " BEGIN"+
                           " INSERT INTO DWH_IMBCBCMP_INF (CCPCD, CCPNE, CCPNR, CCPRI, CCBBR, ALT_CODE, VALID_FROM) select CCPCD, CCPNE, CCPNR, CCPRI, CCBBR, ALT_CODE, VALID_FROM from V_GL_DWH_IMBCBCMP; "+
                           " COMMIT;"+
                           " END;";

            try(CallableStatement cs = connection.prepareCall(ablok)){
                cs.execute();
            }
//            try (PreparedStatement query = connection.prepareStatement("INSERT INTO DWH_IMBCBBRP_INF (A8BRCD, A8LCCD, A8BICN, A8BRNM, BBRRI, BCORI, BCBBR, BR_HEAD, BR_OPER, FCC_CODE, VALID_FROM) select A8BRCD, A8LCCD, A8BICN, A8BRNM, BBRRI, BCORI, BCBBR, BR_HEAD, BR_OPER, FCC_CODE, VALID_FROM from V_GL_DWH_IMBCBBRP");
//                 PreparedStatement query2 = connection.prepareStatement("INSERT INTO DWH_IMBCBCMP_INF (CCPCD, CCPNE, CCPNR, CCPRI, CCBBR, ALT_CODE, VALID_FROM) select CCPCD, CCPNE, CCPNR, CCPRI, CCBBR, ALT_CODE, VALID_FROM from V_GL_DWH_IMBCBCMP")) {
//                query.execute();
//                query2.execute();
//            }
            return 1;
        }), 60 * 60);
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

    private int getFixBranchCode() {
        try {
            return propertiesRepository.getNumber("dwh.fix.branch.code").intValue();
        } catch (ExecutionException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }
}
