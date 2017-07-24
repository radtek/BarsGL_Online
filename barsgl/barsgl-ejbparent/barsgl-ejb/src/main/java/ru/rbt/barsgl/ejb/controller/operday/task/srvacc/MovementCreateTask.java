package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.integr.oper.MovementCreateProcessor;
import ru.rbt.barsgl.ejb.integr.struct.MovementCreateData;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * ER22228
 * Для теста MovementCreateProcessor
 */
public class MovementCreateTask implements ParamsAwareRunnable {

    @Inject
    private MovementCreateProcessor processor;

    @EJB
    private CoreRepository coreRepository;

    @EJB
    private AuditController auditController;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        MovementCreateData data = processor.fillTestData();
        List<MovementCreateData> datas = new ArrayList<>();
        datas.add(data);

        datas.get(0).setAccountCBD("40807810000010496202");
        processor.processOld(datas);
        data = datas.get(0);

        // Debug if local -- resend for xml validation
        processor.sendRequests(datas);
        
        //processor.sendRequests(datas);
        //processor.putTestAnswer();
//        processor.receiveResponses(datas);
        auditController.info(AuditRecord.LogCode.MovementCreate,"Status cb account: "+data.getState());
        /*List<DataRecord> records = coreRepository.select("SELECT bsaacid " +
                                                             "FROM dwh.baltur " +
                                                             "WHERE bsaacid IN (SELECT bsaacid " +
                                                             "                  FROM dwh.accrln " +
                                                             "                  WHERE bsaacid LIKE '40817%') " +
                                                             "      AND dat >= '2016-02-17' " +
                                                             "      AND obac > 0");*/
//        auditController.info(AuditRecord.LogCode.MovementCreate,"Records: "+records.size());
        /*for (DataRecord item:records) {
            datas.get(0).setAccountCBD(item.getString("bsaacid"));
            auditController.info(AuditRecord.LogCode.MovementCreate,"Current cbAccount: "+item.getString("bsaacid"));
            processor.process(datas);
            if (MovementCreateData.StateEnum.SUCCESS.equals(data.getState())) {
                auditController.info(AuditRecord.LogCode.MovementCreate,"Success cb account: "+data.getAccountCBD());
                break;
            }
        }*/
//        auditController.info(AuditRecord.LogCode.MovementCreate,"Finished");
    }
}
