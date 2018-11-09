package ru.rbt.barsgl.ejbtest.aq;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.od.OperdaySynchronizationController;
import ru.rbt.barsgl.ejb.controller.operday.task.ExecutePreCOBTaskNew;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.BackvalueJournal;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.repository.AqRepository;
import ru.rbt.barsgl.ejb.repository.BankCurrencyRepository;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.AbstractRemoteIT;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.shared.enums.BalanceMode;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;
import ru.rbt.ejbcore.datarec.DBParam;
import ru.rbt.ejbcore.datarec.DBParams;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.COB;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;
import static ru.rbt.barsgl.ejb.entity.gl.BackvalueJournal.BackvalueJournalState.NEW;
import static ru.rbt.barsgl.ejb.entity.gl.BackvalueJournal.BackvalueJournalState.SELECTED;
import static ru.rbt.barsgl.shared.enums.BalanceMode.GIBRID;
import static ru.rbt.barsgl.shared.enums.BalanceMode.ONLINE;
import static ru.rbt.ejbcore.util.DateUtils.dbDateParse;

public class CalcBalanceAsyncIT extends AbstractRemoteIT {

    private static final Logger log = Logger.getLogger(CalcBalanceAsyncIT.class.getName());

    private static final Long zero = new Long(0);

    private static final String pbrGibrid = "@@GL-K+";
    private static String pbrOnline;

    static {
        try {
            pbrOnline = baseEntityRepository.selectFirst("select * from GL_AQPBR").getString("PBRVALUE");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private enum QUEUE {
        BAL_QUEUE, AQ$_BAL_QUEUE_TAB_E
    }

    @Test public void testGibrid() throws SQLException {
        stopListeningQueue();
        purgeQueueTable();

        Operday operday = getOperday();

        // отключены все триггера, кроме AQ

        setGibridBalanceMode();

        List<DataRecord> triggers = baseEntityRepository.select("select * from user_triggers where table_name = ? ", "PST");
        Assert.assertTrue(triggers.stream().anyMatch(r -> "ENABLED".equals(r.getString("status"))));
        Assert.assertEquals(BalanceMode.GIBRID, BalanceMode.valueOf(baseEntityRepository.selectFirst("select GLAQ_PKG_UTL.GET_CURRENT_BAL_STATE st from dual").getString("st")));

        // удаление baltur по счету
        GLAccount account = findAccount("40702810%");
        log.info("Account " + account.getBsaAcid());
        baseEntityRepository.executeNativeUpdate("delete from baltur where bsaacid = ?", account.getBsaAcid());

        Long amnt = 100L; Long amntbc = amnt + 1;
        long id = baseEntityRepository.nextId("PD_SEQ");
        log.info("PST id= " + id);
        createPosting(id, id, account.getAcid(), account.getBsaAcid(), amnt, amntbc, pbrGibrid, operday.getCurrentDate(), operday.getCurrentDate(), BankCurrency.RUB.getCurrencyCode(), null);

        // проверка остатков - нет изменений - включен гибридный режим
        List<DataRecord> balturs = baseEntityRepository.select("select * from baltur where bsaacid = ?", account.getBsaAcid());
        Assert.assertEquals(0, balturs.size());

        checkMessageCount(QUEUE.BAL_QUEUE, 1);
        dequeueProcessOne();

        // есть изменения (проверка сумм!)
        balturs = baseEntityRepository.select("select * from baltur where bsaacid = ?", account.getBsaAcid());
        Assert.assertEquals(1, balturs.size());
        Assert.assertEquals(zero, balturs.get(0).getLong("obac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("obbc"));
        Assert.assertEquals(zero, balturs.get(0).getLong("dtac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("dtbc"));
        Assert.assertEquals(amnt, balturs.get(0).getLong("ctac"));
        Assert.assertEquals(amntbc, balturs.get(0).getLong("ctbc"));

        // invisible
        baseEntityRepository.executeNativeUpdate("update pst set invisible = '1' where id = ?", id);

        checkMessageCount(QUEUE.BAL_QUEUE, 1);
        dequeueProcessOne();

        balturs = baseEntityRepository.select("select * from baltur where bsaacid = ?", account.getBsaAcid());
        Assert.assertEquals(1, balturs.size());
        Assert.assertEquals(zero, balturs.get(0).getLong("obac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("obbc"));
        Assert.assertEquals(zero, balturs.get(0).getLong("dtac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("dtbc"));
        Assert.assertEquals(zero, balturs.get(0).getLong("ctac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("ctbc"));

        // insert already invisible
        long id2 = baseEntityRepository.nextId("PD_SEQ");
        createPosting(id2,id2, account.getAcid(), account.getBsaAcid(),amnt, amntbc, pbrGibrid, operday.getCurrentDate(), operday.getCurrentDate(), BankCurrency.RUB.getCurrencyCode(), "1");

        checkMessageCount(QUEUE.BAL_QUEUE, 0);

        // balance is zero
        balturs = baseEntityRepository.select("select * from baltur where bsaacid = ?", account.getBsaAcid());
        Assert.assertEquals(1, balturs.size());
        Assert.assertEquals(zero, balturs.get(0).getLong("obac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("obbc"));
        Assert.assertEquals(zero, balturs.get(0).getLong("dtac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("dtbc"));
        Assert.assertEquals(zero, balturs.get(0).getLong("ctac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("ctbc"));

    }

    @Test
    public void testOnline() throws SQLException {
        setOnlineBalanceMode();
        stopListeningQueue();
        purgeQueueTable();

        GLAccount account = findAccount("40702810%");
        log.info("Account " + account.getBsaAcid());
        baseEntityRepository.executeNativeUpdate("delete from baltur where bsaacid = ?", account.getBsaAcid());

        Long amnt = 100L;
        Long amntbc = 101L;

        // онлайн пересчет остатков, в очередь ничего не поступает, остатки считаются в триггерах

        Operday operday = getOperday();
        long id = baseEntityRepository.nextId("PD_SEQ");
        createPosting(id, id, account.getAcid(), account.getBsaAcid(), amnt, amntbc, pbrGibrid, operday.getCurrentDate(), operday.getCurrentDate(), BankCurrency.RUB.getCurrencyCode(), "1");


        checkMessageCount(QUEUE.BAL_QUEUE, 0);

        List<DataRecord> balturs = baseEntityRepository.select("select * from baltur where bsaacid = ?", account.getBsaAcid());
        Assert.assertEquals(0, balturs.size());

        baseEntityRepository.executeNativeUpdate("update pst set invisible = '0' where id = ?", id);
        balturs = baseEntityRepository.select("select * from baltur where bsaacid = ?", account.getBsaAcid());
        Assert.assertEquals(1, balturs.size());
        Assert.assertEquals(zero, balturs.get(0).getLong("obac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("obbc"));
        Assert.assertEquals(zero, balturs.get(0).getLong("dtac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("dtbc"));
        Assert.assertEquals(amnt, balturs.get(0).getLong("ctac"));
        Assert.assertEquals(amntbc, balturs.get(0).getLong("ctbc"));
    }

    @Test public void testOndemand() throws SQLException {
        setGibridBalanceMode();
        setOndemanBalanceMode();

        List<DataRecord> triggers = baseEntityRepository.select("select * from user_triggers where table_name = ? ", "PST");
        List<DataRecord> enabled = triggers.stream().filter(r -> "ENABLED".equals(r.getString("status"))).collect(Collectors.toList());
        Assert.assertEquals(0, enabled.size());
    }

    @Test public void testGibridOnSpecificPBR() throws SQLException {
        setGibridBalanceMode();
        stopListeningQueue();
        purgeQueueTable();

        GLAccount account = findAccount("40702810%");
        log.info("Account " + account.getBsaAcid());
        baseEntityRepository.executeNativeUpdate("delete from baltur where bsaacid = ?", account.getBsaAcid());

        Long amnt = 100L;
        Long amntbc = 101L;

        // онлайн пересчет остатков, в очередь ничего не поступает, остатки считаются в триггерах

        Operday operday = getOperday();
        long id = baseEntityRepository.nextId("PD_SEQ");
        createPosting(id, id, account.getAcid(), account.getBsaAcid(), amnt, amntbc, pbrOnline, operday.getCurrentDate(), operday.getCurrentDate(), BankCurrency.RUB.getCurrencyCode(), "0");

        checkMessageCount(QUEUE.BAL_QUEUE, 0);

        List<DataRecord> balturs = baseEntityRepository.select("select * from baltur where bsaacid = ?", account.getBsaAcid());
        Assert.assertEquals(1, balturs.size());

        balturs = baseEntityRepository.select("select * from baltur where bsaacid = ?", account.getBsaAcid());
        Assert.assertEquals(1, balturs.size());
        Assert.assertEquals(zero, balturs.get(0).getLong("obac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("obbc"));
        Assert.assertEquals(zero, balturs.get(0).getLong("dtac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("dtbc"));
        Assert.assertEquals(amnt, balturs.get(0).getLong("ctac"));
        Assert.assertEquals(amntbc, balturs.get(0).getLong("ctbc"));

    }

    @Test public void testBvjrnl() throws Exception {

        setGibridBalanceMode();
        baseEntityRepository.executeNativeUpdate("delete from gl_bvjrnl");
        baseEntityRepository.executeNativeUpdate("delete from gl_locacc");

        GLAccount account1 = findAccount("40702840%");
        Date pod0 = DateUtils.parseDate("2018-09-01", "yyyy-MM-dd");
        Date pod1 = DateUtils.addDays(pod0, 1);
        Date pod2 = DateUtils.addDays(pod0, 2);
        cleanBvjrnlRecord(account1);
        insertBvJrnl(account1, NEW, pod0);
        insertBvJrnl(account1, NEW, pod1);
        insertBvJrnl(account1, NEW, pod2);

        List<DataRecord> bvs = baseEntityRepository.select("select * from gl_bvjrnl where bsaacid = ?", account1.getBsaAcid());
        Assert.assertTrue(bvs.stream().anyMatch(r -> r.getLong("seq") != null));

        GLAccount account2 = findAccount("40701840%");
        cleanBvjrnlRecord(account2);
        insertBvJrnl(account2, NEW, pod0);

        GLAccount account3 = findAccount("40701810%"); // по этому счету локализации не будет - не пройдет фильтр
        cleanBvjrnlRecord(account3);
        insertBvJrnl(account3, NEW, pod0);

        DBParams params = DBParams.createParams(new DBParam(Types.INTEGER, DBParam.DBParamDirectionType.OUT)
                ,new DBParam(Types.INTEGER, DBParam.DBParamDirectionType.OUT));
        params = baseEntityRepository.executeCallable(
                "declare\n" +
                "    l_cnt number;\n" +
                "    l_tot number;\n" +
                "begin\n" +
                "    PKG_LOCAL.INS_TO_LOCAL(l_cnt, l_tot);\n" +
                "    \n" +
                "    ? := l_cnt;\n" +
                "    ? := l_tot;\n" +
                "end;", params);
        Assert.assertEquals(2, params.getParams().get(0).getValue());
        Assert.assertEquals(4, params.getParams().get(1).getValue());
        // проверка статусов обработки процессом локализации
        // проверяем
        DBParams params2 = DBParams.createParams(new DBParam(Types.INTEGER, DBParam.DBParamDirectionType.OUT));
        params2 = baseEntityRepository.executeCallable(
                "declare\n" +
                "    l_cnt number;\n" +
                "begin\n" +
                "    PKG_LOCAL.UPDATE_BVJRNL(A_COUNT=>l_cnt, A_CHECK_PROCESSED=>'1');\n" +
                "    ? := l_cnt;\n" +
                "end;", params2);
        // проверка статусов обработки процессом локализации
        // не проверяем
        Assert.assertEquals(0, params2.getParams().get(0).getValue());
        List<DataRecord> bvRecs =  baseEntityRepository.select("select * from gl_bvjrnl");


        // рублевый счет отвалился на фильтре
        Assert.assertEquals(1L,
                bvRecs.stream().filter(r -> SELECTED.name().equals(r.getString("STATE")) && account3.getBsaAcid().equals(r.getString("bsaacid"))).count());
        Assert.assertEquals(4L,
                bvRecs.stream().filter(r -> NEW.name().equals(r.getString("STATE")) && !account3.getBsaAcid().equals(r.getString("bsaacid"))).count());

        params2 = baseEntityRepository.executeCallable(
                "declare\n" +
                "    l_cnt number;\n" +
                "begin\n" +
                "    PKG_LOCAL.UPDATE_BVJRNL(l_cnt);\n" +
                "    ? := l_cnt;\n" +
                "end;", params2);
        Assert.assertEquals(4, params2.getParams().get(0).getValue());


    }

    @Test public void testRestoreTriggersState() throws SQLException {
        setGibridBalanceMode();
        checkCurrentBalanceMode(BalanceMode.GIBRID);

        setOndemanBalanceMode();
        checkCurrentBalanceMode(BalanceMode.ONDEMAND);

        restorePreviousTriggersState();
        checkCurrentBalanceMode(BalanceMode.GIBRID);
    }

    @Test public void testErrors() throws Exception {

        setGibridBalanceMode();
        // проверка - очередь запущена на прием
        DataRecord queue = baseEntityRepository.selectFirst("select * from user_queues where name = GLAQ_PKG_CONST.GET_BALANCE_QUEUE_NAME");
        Assert.assertEquals("YES", queue.getString("ENQUEUE_ENABLED").trim());
        Assert.assertEquals("YES", queue.getString("DEQUEUE_ENABLED").trim());

        stopListeningQueue();
        purgeQueueTable();

        // отключаем задержку переобработки сообщений
        baseEntityRepository.executeNativeUpdate("" +
                "declare pragma autonomous_transaction; " +
                "BEGIN DBMS_AQADM.ALTER_QUEUE('BAL_QUEUE', RETRY_DELAY => 0); END;");

        GLAccount account = findAccount("40702810%");
//        Operday operday = getOperday();

        // берем счет и делаем для него битый baltur
        baseEntityRepository.executeNativeUpdate("delete from baltur where bsaacid = ?", account.getBsaAcid());

        final Date dat1 = dbDateParse("2018-01-01");
        final Date dat2 = dbDateParse("2018-01-02");
        final Date incorrectDatto29 = dbDateParse("2029-01-29");
        final Date correctDatto29 = dbDateParse("2029-01-01");
        insertBaltur(account.getBsaAcid(), account.getAcid(), dat1, dat1);
        insertBaltur(account.getBsaAcid(), account.getAcid(), dat2, incorrectDatto29);

        // делаем проводку в режиме gibrid
        long id = baseEntityRepository.nextId("PD_SEQ");
        createPosting(id, id, account.getAcid(), account.getBsaAcid(), 1, 1, pbrGibrid, dat2
                , dat2, BankCurrency.RUB.getCurrencyCode(), "0");
        DataRecord queueProperties = baseEntityRepository.selectFirst("select * from USER_QUEUES where name = GLAQ_PKG_CONST.GET_BALANCE_QUEUE_NAME");
        Assert.assertNotNull(queueProperties);

        // делаем кол-во попыток равное MAX_RETRIES + 1
        for (int i = 0; i <= queueProperties.getLong("MAX_RETRIES"); i++) {
            try {
                dequeueProcessOne();
            } catch (Exception e) {
                log.log(Level.WARNING, "error on dequing", e);
            }
        }

        // проверяем что сообщение в очереди ошибок
        String exceptionQueueName = remoteAccess.invoke(AqRepository.class, "getExceptionQueueName");
        String queueTableName = remoteAccess.invoke(AqRepository.class, "getQueueTableName");

        List<DataRecord> errorMessages = baseEntityRepository.select("select Q_NAME from " + queueTableName +" t where t.user_data.id = ?", id);
        Assert.assertEquals(1, errorMessages.size());
        Assert.assertEquals(exceptionQueueName, errorMessages.get(0).getString("Q_NAME"));

        // баланс не изменен
        DataRecord balance = baseEntityRepository.selectFirst("select sum(OBAC+DTAC+CTAC) SM from baltur where bsaacid = ? and dat = ?", account.getBsaAcid(), dat2);
        Assert.assertTrue(0 == balance.getInteger("sm"));

        // останавливаем обработку (?)

        baseEntityRepository.executeNativeUpdate("BEGIN GLAQ_PKG_UTL.CHECK_ERROR_QUEUE; END;");

        // проверка - очередь остановлена на прием
        queue = baseEntityRepository.selectFirst("select * from user_queues where name = GLAQ_PKG_CONST.GET_BALANCE_QUEUE_NAME");
        Assert.assertEquals("NO", queue.getString("ENQUEUE_ENABLED").trim());
        Assert.assertEquals("YES", queue.getString("DEQUEUE_ENABLED").trim());


        // фиксим ошибки
        baseEntityRepository.executeNativeUpdate("update baltur set datto = ? where bsaacid = ? and dat = ?"
            , correctDatto29, account.getBsaAcid(), dat2);

        // обрабатываем сообщение из ошибок
        dequeueProcessExceptionQueue();
        errorMessages = baseEntityRepository.select("select Q_NAME from " + queueTableName +" t where t.user_data.id = ?", id);
        Assert.assertEquals(0, errorMessages.size());
        balance = baseEntityRepository.selectFirst("select sum(OBAC+DTAC+CTAC) SM from baltur where bsaacid = ? and dat = ?", account.getBsaAcid(), dat2);
        Assert.assertTrue(balance.getInteger("sm") + "", 1 == balance.getInteger("sm"));
    }

    /**
     * установка целевого режима пересчета остатков при окончании закрытии дня
     * в случае текущего режима обработки проводок DIRECT
     * @throws Exception
     */
    @Test
    public void testExecutePreCOBNew_balanceMode() throws Exception {

        final String jobName = ExecutePreCOBTaskNew.class.getSimpleName();

        // DIRECT MODE
        updateOperday(Operday.OperdayPhase.ONLINE, OPEN);

        setGibridBalanceMode();

        Operday operday = getOperday();
        Assert.assertEquals(Operday.OperdayPhase.ONLINE, operday.getPhase());
        Assert.assertEquals(GIBRID, operday.getBalanceMode());

        baseEntityRepository.executeNativeUpdate("update gl_od set prc = ?", ProcessingStatus.STOPPED.name());

        baseEntityRepository.executeNativeUpdate("delete from gl_sched_h where operday = ?", getOperday().getCurrentDate());

        JobHistory history1 = getLastHistRecordObject(jobName);

        SingleActionJob precobJob =SingleActionJobBuilder.create()
                .withClass(ExecutePreCOBTaskNew.class).withName(jobName)
                .withProps(ExecutePreCOBTaskNew.FINAL_BALANCE_MODE_KEY+ "=" + ONLINE.name()).build();

        baseEntityRepository.executeNativeUpdate("update gl_etlpkg set state = ? where state in ('WORKING', 'LOADED', 'INPROGRESS')"
                , EtlPackage.PackageState.ERROR.name());

        jobService.executeJob(precobJob);

        JobHistory history2 = getLastHistRecordObject(jobName);
        Assert.assertNotNull(history2);
        Assert.assertTrue(history1 == null || !Objects.equals(history1.getId(), history2.getId()));
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED, history2.getResult());

        operday = getOperday();
        Assert.assertEquals(COB, operday.getPhase());
        Assert.assertEquals(ONLINE, operday.getBalanceMode());
    }

    /**
     * в гибридном режиме пересчета остатков регистрация в журнале бэквалуе должна
     * происходить в при обработке оборотов из очереди
     */
    @Test public void testRegisterBvjrnl_gibrid() throws SQLException {
        setGibridBalanceMode();
        purgeQueueTable();
        updateOperday(Operday.OperdayPhase.ONLINE, OPEN, DIRECT);

        baseEntityRepository.executeNativeUpdate("delete from gl_bvjrnl");
        GLOperation operation = createOperation(getOperday().getLastWorkingDay());
        List<DataRecord> bvRecords = baseEntityRepository.select("select * from gl_bvjrnl where bsaacid in (?,?)"
                , operation.getAccountDebit(), operation.getAccountCredit());
        Assert.assertTrue(bvRecords.isEmpty());

        dequeueProcessAll();
        bvRecords = baseEntityRepository.select("select * from gl_bvjrnl where bsaacid in (?,?)"
                , operation.getAccountDebit(), operation.getAccountCredit());

        Assert.assertEquals(2, bvRecords.size());
    }

    private void insertBaltur(String bsaacid, String acid, Date dat, Date datto) {
        baseEntityRepository.executeNativeUpdate("insert into baltur (bsaacid, acid, dat, datto, incl) values (?,?,?,?, '0')"
                , bsaacid, acid, dat, datto);
    }

    private void createPosting (long id, long pcid, String acid, String bsaacid, long amount, long amountbc, String pbr, Date pod, Date vald, String ccy, String invisible) {
        String insert = "insert into pst (id,pcid,acid,bsaacid,amnt,amntbc,pbr,pod,vald,ccy, invisible) values (?,?,?,?,?,?,?,?,?,?,?)";
        baseEntityRepository.executeNativeUpdate(insert, id, pcid, acid, bsaacid, amount, amountbc, pbr, pod, vald, ccy, invisible);
    }


    private void checkMessageCount(QUEUE queue, long expect) throws SQLException {
        List<DataRecord> records = baseEntityRepository.select("select queue from AQ$BAL_QUEUE_TAB where queue = ?", queue.name());
        Assert.assertEquals(expect, records.size());
    }

    private void restorePreviousTriggersState() {
        remoteAccess.invoke(OperdaySynchronizationController.class, "restorePreviousTriggersState");
    }

    private void stopListeningQueue() {
        baseEntityRepository.executeNativeUpdate(
                "begin\n" +
                "    for nn in (select * from user_scheduler_running_jobs where job_name like '%'||GLAQ_PKG_CONST.GET_BALANCE_QUEUE_LISTNR_PRFX||'%') loop\n" +
                "        dbms_scheduler.stop_job(nn.job_name, true);\n" +
                "        dbms_scheduler.disable(nn.job_name, true);\n" +
                "    end loop;\n" +
                "end;");
    }

    private void insertBvJrnl(GLAccount account, BackvalueJournal.BackvalueJournalState state, Date pod) {
        baseEntityRepository.executeNativeUpdate("insert into gl_bvjrnl (bsaacid,acid,pod,state) values (?, ?, ?, ?)",
                account.getBsaAcid(), account.getAcid(), pod, state.name());
    }

    private void cleanBvjrnlRecord(GLAccount account) {
        baseEntityRepository.executeNativeUpdate("delete from gl_bvjrnl where bsaacid = ?", account.getBsaAcid());
    }

    private void dequeueProcessExceptionQueue() {
        baseEntityRepository.executeNativeUpdate("begin GLAQ_PKG.DEQUEUE_PROCESS_ONE(GLAQ_PKG_CONST.C_EXCEPTION_QUEUE_NAME); end;");
    }

    private GLOperation createOperation(Date vdate) throws SQLException {
        Long stamp = System.currentTimeMillis();
        EtlPackage pkg = newPackageNotSaved(stamp, "Тестовый пакет " + stamp);
        pkg.setDateLoad(getSystemDateTime());
        pkg.setPackageState(EtlPackage.PackageState.INPROGRESS);
        pkg.setAccountCnt(0);
        pkg.setPostingCnt(4);
        pkg = (EtlPackage) baseEntityRepository.save(pkg);

        List<DataRecord> bsaacids = baseEntityRepository.select("select * from gl_acc " +
                "where bsaacid like '40817810__001%' and trim(acid) is not null " +
                "and ? between dto and nvl(dtc, to_date('2029-01-01','yyyy-mm-dd')) and rownum <= 2", getOperday().getCurrentDate());
        Assert.assertEquals(2, bsaacids.size());
        final String accCredit = bsaacids.get(0).getString("bsaacid");
        final String accDebit = bsaacids.get(1).getString("bsaacid");

        EtlPosting pst1 = createSimple(stamp, pkg, accCredit, accDebit, vdate);
        GLOperation operation = (GLOperation) postingController.processMessage(pst1);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        return operation;
    }

    private EtlPosting createSimple(long stamp, EtlPackage pkg, String accCredit, String accDebit, Date vdate) throws SQLException {
        EtlPosting pst = newPosting(stamp + 1, pkg);
        pst.setAccountCredit(accCredit);
        pst.setAccountDebit(accDebit);
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(remoteAccess.invoke(BankCurrencyRepository.class, "getCurrency", getCurrencyCodeByDigital(accCredit.substring(5, 8))));
        pst.setCurrencyDebit(remoteAccess.invoke(BankCurrencyRepository.class, "getCurrency", getCurrencyCodeByDigital(accDebit.substring(5, 8))));
        pst.setValueDate(vdate);
        pst.setSourcePosting("K+TP");
        return (EtlPosting) baseEntityRepository.save(pst);
    }

    private String getCurrencyCodeByDigital(String cbccy) throws SQLException {
        return baseEntityRepository.selectOne("select glccy from currency where cbccy = ?", cbccy).getString("glccy");
    }

}
