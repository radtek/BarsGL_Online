package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.repository.od.OperdayRepository;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.controller.operday.task.EtlStructureMonitorTask;
import ru.rbt.barsgl.ejb.controller.operday.task.ReprocessWtacOparationsTask;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.tasks.ejb.entity.task.JobHistory;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.shared.enums.OperState;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Logger;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.BUFFER;
import static ru.rbt.barsgl.ejb.controller.operday.task.ReprocessWtacOparationsTask.DEFAULT_STEP_NAME;
import static ru.rbt.barsgl.ejb.entity.etl.EtlPackage.PackageState.LOADED;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

/**
 * Created by Ivan Sevastyanov on 18.02.2016.
 * проверка работы задачи переобработки операций со статусом WTAC
 */
public class ReprocessWtacOparationsTaskTest extends AbstractTimerJobTest {

    public static final Logger logger = Logger.getLogger(ReprocessWtacOparationsTaskTest.class.getName());

    /**
     * корректность запуска задачи
     */
    @Test public void test() throws Exception {
        checkCreateStep(DEFAULT_STEP_NAME, getOperday().getLastWorkingDay(), "O");

        logger.info("deleted: " + baseEntityRepository.executeUpdate("delete from JobHistory h"));

        updateOperday(ONLINE, OPEN, BUFFER);

        SingleActionJob job = SingleActionJobBuilder.create()
                .withClass(ReprocessWtacOparationsTask.class).withName(ReprocessWtacOparationsTask.JOB_NAME).build();
        jobService.executeJob(job);

        JobHistory history = (JobHistory) baseEntityRepository
                .selectFirst(JobHistory.class, "from JobHistory h where h.jobName = ?1 and h.operday = ?2"
            , ReprocessWtacOparationsTask.JOB_NAME, getOperday().getCurrentDate());
        Assert.assertNotNull(history);
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED, history.getResult());

        jobService.executeJob(job);

        JobHistory history2 = (JobHistory) baseEntityRepository
                .selectFirst(JobHistory.class, "from JobHistory h where h.jobName = ?1 and h.operday = ?2 order by h.id desc"
                        , ReprocessWtacOparationsTask.JOB_NAME, getOperday().getCurrentDate());
        Assert.assertEquals(history, history2);
    }

    @Test
    public void testAll() throws Exception {
        updateOperday(ONLINE,OPEN);

        baseEntityRepository.executeNativeUpdate("delete from GL_SCHED_H");

        Long stamp = System.currentTimeMillis();
        EtlPackage pkg = newPackageNotSaved(stamp, "Тестовый пакет " + stamp);
        pkg.setDateLoad(getSystemDateTime());
        pkg.setPackageState(LOADED);
        pkg = (EtlPackage) baseEntityRepository.save(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAccountCredit("00000036000000000000");
        pst1.setAccountDebit("40817036250010000018");
        pst1.setAmountCredit(new BigDecimal("12.0056"));
        pst1.setAmountDebit(pst1.getAmountCredit());
        pst1.setCurrencyCredit(BankCurrency.AUD);
        pst1.setCurrencyDebit(pst1.getCurrencyCredit());
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        jobService.executeJob(SingleActionJobBuilder.create().withClass(EtlStructureMonitorTask.class).build());

        GLOperation oper1 = getOperation(pst1.getId());
        Assert.assertNotNull(oper1);
        Assert.assertEquals(OperState.WTAC, oper1.getState());
        Assert.assertNull(oper1.getPstScheme());

        oper1.setAccountCredit("40817036200012959997");
        oper1.setValueDate(getOperday().getLastWorkingDay());
        baseEntityRepository.update(oper1);

        // обработка WTAC
        jobService.executeJob(SingleActionJobBuilder.create().withClass(ReprocessWtacOparationsTask.class).build());

        oper1 = getOperation(pst1.getId());
        Assert.assertNotNull(oper1);
        Assert.assertEquals(OperState.POST, oper1.getState());
        Assert.assertEquals(GLOperation.OperType.S, oper1.getPstScheme());
    }

    private GLOperation getOperation(Long idpst) {
        return (GLOperation) baseEntityRepository.selectFirst(GLOperation.class, "from GLOperation o where o.etlPostingRef = ?1", idpst);
    }
}

