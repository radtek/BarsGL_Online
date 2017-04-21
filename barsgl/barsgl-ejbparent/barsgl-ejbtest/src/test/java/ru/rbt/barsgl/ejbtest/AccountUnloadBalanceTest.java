package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.OperdayRepository;
import ru.rbt.barsgl.ejb.controller.operday.task.AccountBalanceUnloadTask;
import ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadParams;
import ru.rbt.barsgl.ejb.controller.operday.task.balsep.AccountBalanceRegisteredUnloadTask;
import ru.rbt.barsgl.ejb.controller.operday.task.balsep.AccountBalanceSharedUnloadTask;
import ru.rbt.barsgl.ejb.controller.operday.task.balsep.AccountBalanceUnloadThree;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.enums.OperState;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import static ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus.SUCCEDED;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.CLOSED;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.COB;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadParams.*;
import static ru.rbt.barsgl.ejb.controller.operday.task.OpenOperdayTask.FLEX_FINAL_MSG_OK;
import static ru.rbt.barsgl.ejb.controller.operday.task.OpenOperdayTask.FLEX_FINAL_STEP_RESULT_OK;
import static ru.rbt.barsgl.ejb.controller.operday.task.balsep.AccountBalanceUnloadThree.STEP_CHECK_NAME_DEFAULT;
import static ru.rbt.barsgl.ejb.entity.acc.AccountKeysBuilder.create;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.RUB;
import static ru.rbt.barsgl.ejbtest.utl.Utl4Tests.*;

/**
 * Created by Ivan Sevastyanov
 */
public class AccountUnloadBalanceTest extends AbstractTimerJobTest {

    public static final Logger logger = Logger.getLogger(AccountUnloadBalanceTest.class.getName());

    @BeforeClass
    public static void beforeClass() {
        logger.info("deleted 1: " + baseEntityRepository.executeNativeUpdate("delete from GL_ETLDWHS"));
    }
    /**
     * Выгрузка данных об остатках по счетам открытим в BarsGL и совместно используемым
     * @fsd 0.0.0
     */
    @Test
    public void testBalance() throws Exception {

        prepareForBalanceUnloadTask();

        restoreOperday();

        Operday od = getOperday();
        logger.info("updated = " + baseEntityRepository.executeNativeUpdate("update gl_oper set procdate = ? where procdate = ?"
                , DateUtils.addDays(od.getCurrentDate(), -5), od.getCurrentDate()));

        setOperday(od.getCurrentDate(), od.getLastWorkingDay(), Operday.OperdayPhase.ONLINE
                , Operday.LastWorkdayStatus.OPEN);

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "MIDAS");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setCurrencyCredit(RUB);
        pst.setCurrencyDebit(RUB);

        pst.setAccountCredit("");
        pst.setAccountDebit(Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "47408810%40"));

        final AccountKeys acCt
                = create()
                .withBranch("001").withCurrency(pst.getCurrencyCredit().getCurrencyCode()).withCustomerNumber("01584414")
                .withAccountType("131060102").withCustomerType("00").withTerm("00")
                .withGlSequence("123457").withAcc2("30424").withAccountCode("1049").withAccSequence("02")
                .build();

        String accountKeyCt = acCt.toString();
        deleteGlAccountWithLinks(baseEntityRepository, accountKeyCt);

        GLAccount glAccCt = findGlAccount(baseEntityRepository, acCt);
        Assert.assertNull(glAccCt);

        pst.setAccountKeyCredit(accountKeyCt);

        pst.setAmountCredit(new BigDecimal("13.99"));
        pst.setAmountDebit(pst.getAmountCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertNotNull(operation.getId());
        Assert.assertTrue(0 < operation.getId());

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertTrue(!isEmpty(operation.getAccountKeyCredit()));

        Assert.assertNotNull(findGlAccount(baseEntityRepository, acCt));

        // выгрузка

        setOperday(od.getCurrentDate(), od.getLastWorkingDay(), COB, CLOSED);

        int cnt = baseEntityRepository.executeNativeUpdate("delete from GLVD_BAL");
        logger.info("deleted balance: " + cnt);
        cnt = baseEntityRepository.executeNativeUpdate("delete from GL_ETLDWHS where parname = ? and pardesc = ? and operday = ?"
                , UnloadBalanceAll.getParamName(), UnloadBalanceAll.getParamDesc(), getOperday().getCurrentDate());
        logger.info("deleted header: " + cnt);

        remoteAccess.invoke(AccountBalanceUnloadTask.class, "run", "unload", new Properties());

        List<DataRecord> headers = findDwhUnloadHeaders(baseEntityRepository
                , DwhUnloadParams.UnloadBalanceAll, getOperday());
        Assert.assertEquals(1, headers.size());
        // может быть ошибка но не системная - по Майдас
        Assert.assertTrue(headers.stream().allMatch(t -> null != t.getObject("start_load")
                        && null != t.getObject("end_load")
                        && Utl4Tests.in(parvalueSupplier(t.getString("parvalue"))
                            , DwhUnloadStatus.SUCCEDED, DwhUnloadStatus.ERROR)
        ));

        final List<DataRecord> balances = baseEntityRepository.select("select * from GLVD_BAL where DAT = ?"
                , getOperday().getCurrentDate());
        Assert.assertTrue(1 <= balances.size());
        Assert.assertTrue(balances.stream().anyMatch(((Predicate<DataRecord>) d -> d.getLong("obal") > 0)
                        .or(d -> d.getLong("ctrn") > 0)
                        .or(d -> d.getLong("dtrn") > 0)
        ));

        glAccCt = findGlAccount(baseEntityRepository, acCt);
        Assert.assertNotNull(glAccCt);

        final GLAccount finalGlAccCt = glAccCt;
        Assert.assertTrue(balances.stream().filter(d ->
                d.getString("acid").equals(finalGlAccCt.getAcid()) && d.getString("bsaacid").equals(finalGlAccCt.getBsaAcid())).findAny().isPresent());

        remoteAccess.invoke(AccountBalanceUnloadTask.class, "run", "unload", new Properties());

        Assert.assertEquals(headers.get(0).getLong("id")
                , findDwhUnloadHeaders(baseEntityRepository, DwhUnloadParams.UnloadBalanceAll, getOperday()
        ).get(0).getLong("id"));

        Assert.assertTrue(findDwhUnloadHeaders(baseEntityRepository
                , DwhUnloadParams.UnloadBalanceAll, getOperday()).stream()
                .allMatch(d -> Utl4Tests.in(parvalueSupplier(d.getString("parvalue"))
                        , DwhUnloadStatus.SUCCEDED, DwhUnloadStatus.ERROR)));
    }

    private static Supplier<DwhUnloadStatus> parvalueSupplier(String parvalue) {
        return () -> {
            for (DwhUnloadStatus st : DwhUnloadStatus.values()) {
                if (st.getFlag().equals(parvalue)) {
                    return st;
                }
            }
            throw new RuntimeException("Not found: " + parvalue);
        };
    }

    @Test
    public void testCheckrun() throws IOException {
        cleanHeader(baseEntityRepository, UnloadBalanceAll.getParamDesc());
        Operday od = getOperday();
        setOperday(od.getCurrentDate(), od.getLastWorkingDay(), Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.CLOSED);
        Properties properties = new Properties();
        properties.load(new StringReader(AccountBalanceUnloadTask.CHECK_RUN_KEY+"=false"));
        Assert.assertTrue(remoteAccess.invoke(AccountBalanceUnloadTask.class, "checkRun", properties));

        long id = remoteAccess.invoke(AccountBalanceUnloadTask.class, "createHeaders", DwhUnloadParams.UnloadBalanceAll);
        remoteAccess.invoke(AccountBalanceUnloadTask.class, "setResultStatus", id, DwhUnloadStatus.SUCCEDED);
        Assert.assertFalse(remoteAccess.invoke(AccountBalanceUnloadTask.class, "checkRun", new Properties()));

        // отключаем проверки
        remoteAccess.invoke(AccountBalanceUnloadTask.class, "setResultStatus", id, DwhUnloadStatus.SUCCEDED);
        properties = new Properties();
        properties.load(new StringReader(AccountBalanceUnloadTask.CHECK_RUN_KEY+"=false"));
        Assert.assertTrue(remoteAccess.invoke(AccountBalanceUnloadTask.class, "checkRun", properties));

    }

    @Test
    public void testUnloadBalanceStep1() throws SQLException {
        remoteAccess.invoke(AccountBalanceRegisteredUnloadTask.class, "run", "unload", new Properties());
    }
    /**
     * Разделение выгрузки остатков: выгрузка остатков по зарегистированным ("нашим") счетам
     * @throws SQLException
     */
    @Test
    public void testSeparatedUnloadBalance() throws SQLException {
        Operday od = getOperday();
        logger.info("updated = " + baseEntityRepository.executeNativeUpdate("update gl_oper set procdate = ? where procdate = ?"
                , DateUtils.addDays(od.getCurrentDate(), -5), od.getCurrentDate()));

        updateOperday(ONLINE, OPEN);

        final AccountKeys acCt
                = create()
                .withBranch("001").withCurrency(RUB.getCurrencyCode()).withCustomerNumber("01584414")
                .withAccountType("131060102").withCustomerType("00").withTerm("00")
                .withGlSequence("123457").withAcc2("30424").withAccountCode("1049").withAccSequence("02")
                .build();

        processOper(getOperday().getLastWorkingDay(), acCt, true);
        processOper(getOperday().getCurrentDate(), acCt, false);

        // выгрузка
        updateOperday(COB, CLOSED);

        int cnt = baseEntityRepository.executeNativeUpdate("delete from GLVD_BAL");
        logger.info("deleted balance: " + cnt);
        cnt = baseEntityRepository.executeNativeUpdate("delete from GL_ETLDWHS where parname = ? and pardesc = ? and operday = ?"
                , UnloadBalanceRegistered.getParamName(), UnloadBalanceRegistered.getParamDesc(), getOperday().getCurrentDate());
        logger.info("deleted header: " + cnt);

        remoteAccess.invoke(AccountBalanceRegisteredUnloadTask.class, "run", "unload", new Properties());

        List<DataRecord> headers = findDwhUnloadHeaders(baseEntityRepository
                , DwhUnloadParams.UnloadBalanceRegistered, getOperday());
        Assert.assertEquals(1, headers.size());
        Assert.assertTrue(headers.stream().allMatch(t -> null != t.getObject("start_load")
                        && null != t.getObject("end_load")
                        && SUCCEDED.getFlag().equals(t.getString("parvalue"))
        ));

        final List<DataRecord> balances = baseEntityRepository.select("select * from GLVD_BAL where UNLOAD_DAT = ?"
                , getOperday().getCurrentDate());
        Assert.assertTrue(1 <= balances.size());
        Assert.assertTrue(balances.stream().anyMatch(((Predicate<DataRecord>) d -> d.getLong("obal") > 0)
                        .or(d -> d.getLong("ctrn") > 0)
                        .or(d -> d.getLong("dtrn") > 0)
        ));

        GLAccount glAccCt = findGlAccount(baseEntityRepository, acCt);
        Assert.assertNotNull(glAccCt);

        final GLAccount finalGlAccCt = glAccCt;
        Assert.assertEquals(2, balances.stream().filter(d ->
                d.getString("acid").equals(finalGlAccCt.getAcid())
                        && d.getString("bsaacid").equals(finalGlAccCt.getBsaAcid())).count());

        remoteAccess.invoke(AccountBalanceRegisteredUnloadTask.class, "run", "unload", new Properties());

        headers = findDwhUnloadHeaders(baseEntityRepository
                , DwhUnloadParams.UnloadBalanceRegistered, getOperday());
        Assert.assertEquals(headers.get(0).getLong("id"), findDwhUnloadHeaders(baseEntityRepository
                , UnloadBalanceRegistered, getOperday()).get(0).getLong("id"));

        Assert.assertTrue(findDwhUnloadHeaders(baseEntityRepository
                , UnloadBalanceRegistered, getOperday()).stream()
                .allMatch(d -> SUCCEDED.getFlag().equals(d.getString("parvalue"))));
    }

    /**
     * Сохранение остатков с предыдущей выгрузки в GLVD_BAL_H
     * @throws SQLException
     */
    @Test
    public void historicalRegisteredTest() throws SQLException {
        logger.info("deleted 1: " + baseEntityRepository.executeNativeUpdate("delete from GL_ETLDWHS"));
        logger.info("deleted 2: " + baseEntityRepository.executeNativeUpdate("delete from GLVD_BAL"));
        // создаем за тек ОД
        final AccountKeys acCt
                = create()
                .withBranch("001").withCurrency(RUB.getCurrencyCode()).withCustomerNumber("01584414")
                .withAccountType("131060102").withCustomerType("00").withTerm("00")
                .withGlSequence("123457").withAcc2("30424").withAccountCode("1049").withAccSequence("02")
                .build();
        processOper(getOperday().getCurrentDate(), acCt, true);

        updateOperday(COB, CLOSED);
        // выгружаем
        remoteAccess.invoke(AccountBalanceRegisteredUnloadTask.class, "run", "unload", new Properties());
        List<DataRecord> headers = findDwhUnloadHeaders(baseEntityRepository
                , DwhUnloadParams.UnloadBalanceRegistered, getOperday());
        Assert.assertEquals(1, headers.size());
        Assert.assertTrue(headers.stream().allMatch(t -> null != t.getObject("start_load")
                        && null != t.getObject("end_load")
                        && SUCCEDED.getFlag().equals(t.getString("parvalue"))
        ));

        List<DataRecord> balances = baseEntityRepository.select("select * from GLVD_BAL where DAT = ?"
                , getOperday().getCurrentDate());
        Assert.assertTrue(1 <= balances.size());

        GLAccount glAccCt = findGlAccount(baseEntityRepository, acCt);
        Assert.assertNotNull(glAccCt);

        final GLAccount finalGlAccCt = glAccCt;
        Assert.assertTrue(balances.stream().filter(d ->
                d.getString("acid").equals(finalGlAccCt.getAcid()) && d.getString("bsaacid").equals(finalGlAccCt.getBsaAcid())).findAny().isPresent());

        Date oldOperday = getOperday().getCurrentDate();

        // переводим ОД
        setOperday(getWorkdayAfter(getOperday().getCurrentDate()), getOperday().getCurrentDate(), ONLINE, OPEN);
        processOper(getOperday().getCurrentDate(), acCt, true);

        updateOperday(COB, CLOSED);

        // выгружаем в след ОД
        remoteAccess.invoke(AccountBalanceRegisteredUnloadTask.class, "run", "unload", new Properties());

        // новые данные в одной таблице, счет другой потому что старый удален
        glAccCt = findGlAccount(baseEntityRepository, acCt);
        Assert.assertNotNull(glAccCt);
        final GLAccount finalGlAccCt2 = glAccCt;
        balances = baseEntityRepository.select("select * from GLVD_BAL where DAT = ?"
                , getOperday().getCurrentDate());
        Assert.assertTrue(balances.stream().filter(d ->
                d.getString("acid").equals(finalGlAccCt2.getAcid()) && d.getString("bsaacid").equals(finalGlAccCt2.getBsaAcid())).findAny().isPresent());

        // старые данные должны переместится в историческую таблицу
        List<DataRecord> hist = baseEntityRepository.select("select * from GLVD_BAL_H where unload_dat = ?", oldOperday);
        Assert.assertTrue(hist.stream().filter(d ->
                d.getString("acid").equals(finalGlAccCt.getAcid()) && d.getString("bsaacid").equals(finalGlAccCt.getBsaAcid())).findAny().isPresent());
    }

    /**
     * выгрузка остатков оборотов по совместно используемым счетам
     */
    @Test public void testSeparatedSharedAccounts () throws SQLException, ParseException {
        setOperday(DateUtils.parseDate("27.02.2015", "dd.MM.yyyy"), DateUtils.parseDate("25.02.2015", "dd.MM.yyyy"),COB, CLOSED);
        updateOperday(COB, CLOSED);

        int cnt = baseEntityRepository.executeNativeUpdate("delete from GL_ETLDWHS where parname = ? and pardesc = ? and operday = ?"
                , UnloadBalanceShared.getParamName(), UnloadBalanceShared.getParamDesc(), getOperday().getCurrentDate());
        logger.info("deleted header: " + cnt);

        remoteAccess.invoke(AccountBalanceSharedUnloadTask.class, "run", "task1", new Properties());

        List<DataRecord> records = baseEntityRepository.select("select * from GLVD_BAL2");
        Assert.assertTrue(0 < records.size());
        List<DataRecord> headers = findDwhUnloadHeaders(baseEntityRepository
                , UnloadBalanceShared, getOperday());
        Assert.assertTrue(!headers.isEmpty() && headers.stream()
                .allMatch(d -> SUCCEDED.getFlag().equals(d.getString("parvalue"))));

    }

    /**
     * выгрузка остатков оборотов после переоценки
     */
    @Test public void testOvervaluedAccounts () throws SQLException {
        restoreOperday();
        // одна операция в текущем ОД
        Operday od = getOperday();
        logger.info("updated = " + baseEntityRepository.executeNativeUpdate("update gl_oper set procdate = ? where procdate = ?"
                , DateUtils.addDays(od.getCurrentDate(), -5), od.getCurrentDate()));

        updateOperday(ONLINE, OPEN);

        final AccountKeys acCt
                = create()
                .withBranch("001").withCurrency(RUB.getCurrencyCode()).withCustomerNumber("01584414")
                .withAccountType("131060102").withCustomerType("00").withTerm("00")
                .withGlSequence("123457").withAcc2("30424").withAccountCode("1049").withAccSequence("02")
                .build();

        processOper(getOperday().getLastWorkingDay(), acCt, true);
        //~

        updateOperday(COB, CLOSED);

        int cnt = baseEntityRepository.executeNativeUpdate("delete from GL_ETLDWHS");
        logger.info("deleted header: " + cnt);

        checkCreateFinalP9step(getOperday().getCurrentDate());
        checkCreateFinalP9step(getOperday().getLastWorkingDay());
        checkCreateStep("A1GL", getOperday().getCurrentDate(), "O");
        checkCreateStep("A1GL", getOperday().getLastWorkingDay(), "O");

        // нужна выгрузка по зарег счетам в текущем ОД
        remoteAccess.invoke(AccountBalanceRegisteredUnloadTask.class, "run", "task1", new Properties());
        // нужна выгрузка по совместным счетам в текущем ОД
        remoteAccess.invoke(AccountBalanceSharedUnloadTask.class, "run", "task1", new Properties());

        remoteAccess.invoke(AccountBalanceUnloadThree.class, "run", "task1", new Properties());

        List<DataRecord> records = baseEntityRepository.select("select * from GLVD_BAL3");
        Assert.assertTrue(0 < records.size());
        List<DataRecord> headers = findDwhUnloadHeaders(baseEntityRepository
                , UnloadBalanceThree, getOperday());
        Assert.assertTrue(!headers.isEmpty());
        Assert.assertTrue(!headers.isEmpty() && headers.stream()
                .allMatch(d -> SUCCEDED.getFlag().equals(d.getString("parvalue"))));

        // если день открыт то пытаемся выгрузить за последний закрытый ОД
        // ничего не выгрузится т.к. в предыд дне не было выгрузки зарег и совместных счетов
        updateOperday(ONLINE,OPEN);

        remoteAccess.invoke(AccountBalanceUnloadThree.class, "run", "task1", new Properties());
        headers = findDwhUnloadHeaders(baseEntityRepository
                , UnloadBalanceThree, getOperday().getLastWorkingDay());
        Assert.assertTrue(headers.isEmpty());

        // создаем условия для выгрузки в прошлый ОД
        baseEntityRepository.executeNativeUpdate("delete from gl_etldwhs where pardesc = ?"
                , DwhUnloadParams.UnloadBalanceThree.getParamDesc());
        baseEntityRepository.executeNativeUpdate("update gl_etldwhs set operday = ?", getOperday().getLastWorkingDay());
        remoteAccess.invoke(AccountBalanceUnloadThree.class, "run", "task1", new Properties());
        headers = findDwhUnloadHeaders(baseEntityRepository
                , UnloadBalanceThree, getOperday().getLastWorkingDay());
        Assert.assertTrue(!headers.isEmpty() && headers.stream()
                .allMatch(d -> SUCCEDED.getFlag().equals(d.getString("parvalue"))));
    }

    private void processOper(Date valueDate, AccountKeys acCt, boolean deleteAccount) throws SQLException {
        updateOperday(ONLINE,OPEN);
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "MIDAS");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(valueDate);

        pst.setCurrencyCredit(new BankCurrency(acCt.getCurrency()));
        pst.setCurrencyDebit(RUB);

        pst.setAccountCredit("");
        pst.setAccountDebit(Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "47408810%40"));


        if (deleteAccount) {
            deleteGlAccountWithLinks(baseEntityRepository, acCt.toString());

            GLAccount glAccCt = findGlAccount(baseEntityRepository, acCt);
            Assert.assertNull(glAccCt);
        }

        pst.setAccountKeyCredit(acCt.toString());

        pst.setAmountCredit(new BigDecimal("13.99"));
        pst.setAmountDebit(pst.getAmountCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertNotNull(operation.getId());
        Assert.assertTrue(0 < operation.getId());

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertTrue(!isEmpty(operation.getAccountKeyCredit()));

        GLAccount result = findGlAccount(baseEntityRepository, acCt);
        Assert.assertNotNull(result);
    }


    /**
     * Фикс ошибки
     * Ошибка при выгрузке остатков в DWH: Не найден или более одного счета по счету Майдас '00000018RUR730101001'
     */
    public void prepareForBalanceUnloadTask() throws SQLException {
        cleanHeader(baseEntityRepository, UnloadBalanceAll.getParamDesc());
        baseEntityRepository.executeNativeUpdate("delete from glvd_bal");

        remoteAccess.invoke(AccountBalanceUnloadTask.class, "executePhaseThree");
        List<DataRecord> recordsBal = baseEntityRepository.select(
                "select acid from glvd_bal b " +
                        " where bsaacid is null and dat = (select curdate from gl_od)");
        for (DataRecord recBal : recordsBal) {
            List<DataRecord> rlnsFound = remoteAccess.invoke(AccountBalanceUnloadTask.class, "findBsaacid", recBal.getString("acid"));
            if (1 < rlnsFound.size()){
                List<DataRecord> rlnsReal = baseEntityRepository.select(
                        "select a.*, rrn(a) rn from accrln a where a.acid = ? and a.rlntype = ?"
                        , recBal.getString("acid"), rlnsFound.get(0).getString("rlntype"));
                if (1 < rlnsReal.size()) {
                    for (int i=1; i< rlnsReal.size(); i++) {
                        baseEntityRepository.executeNativeUpdate("delete from accrln a where rrn(a) = ?", rlnsReal.get(i).getLong("rn"));
                    }
                }
            }
        }
        baseEntityRepository.executeNativeUpdate("delete from glvd_bal");
    }

    private void checkCreateFinalP9step(Date ondate) {
        DataRecord record = remoteAccess.invoke(OperdayRepository.class, "findWorkprocStep"
                , STEP_CHECK_NAME_DEFAULT, ondate);

        if (null == record) {
            baseEntityRepository.executeNativeUpdate(
                    "insert into workproc (dat, id, starttime, endtime, result, count, msg) values (?,?,?,?,?,?,?)"
                    , ondate, STEP_CHECK_NAME_DEFAULT, null, null, FLEX_FINAL_STEP_RESULT_OK, 0, "");
        } else if (!FLEX_FINAL_STEP_RESULT_OK.equalsIgnoreCase(record.getString("result"))
                || !FLEX_FINAL_MSG_OK.equalsIgnoreCase(record.getString("msg"))) {
            baseEntityRepository.executeNativeUpdate("update workproc set result = ?, msg = ? where id = ? and dat = ?",
                    FLEX_FINAL_STEP_RESULT_OK, FLEX_FINAL_MSG_OK, STEP_CHECK_NAME_DEFAULT, ondate);
        }
    }

}
