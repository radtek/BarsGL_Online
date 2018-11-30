package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.LoadCurratesTask;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.dict.CurrencyRate;
import ru.rbt.barsgl.ejb.entity.dict.CurrencyRateId;
import ru.rbt.barsgl.ejb.entity.etl.EtlCurrencyRate;
import ru.rbt.barsgl.ejb.entity.etl.EtlCurrencyRateId;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

import static java.math.BigDecimal.ROUND_HALF_UP;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.BGN;

/**
 * Created by Ivan Sevastyanov
 * Загрузка курсов валют
 * @fsd 9.1
 */
public class EtlRateIT extends AbstractTimerJobIT {

    private static final Logger logger = Logger.getLogger(EtlRateIT.class.getName());

    private static final String badCurrency = "LOL";
    private static final BankCurrency BDT = new BankCurrency("BDT");
    private static final EtlCurrencyRateId BGN_RATE_ID;
    private static final EtlCurrencyRateId BDT_RATE_ID;
    private static final EtlCurrencyRateId BAD_RATE_ID;

    public static final BankCurrency BYR = new BankCurrency("BYR");
    public static final BankCurrency BYB = new BankCurrency("BYB");

    static {
        BGN_RATE_ID = new EtlCurrencyRateId(BGN.getCurrencyCode(), getWorkdayAfter(getOperday().getCurrentDate()));
        BDT_RATE_ID = new EtlCurrencyRateId(BDT.getCurrencyCode(), getWorkdayAfter(getOperday().getCurrentDate()));
        BAD_RATE_ID = new EtlCurrencyRateId(badCurrency, getWorkdayAfter(getOperday().getCurrentDate()));
    }

    @Before
    public void beforeClass() {
        baseEntityRepository.executeUpdate("delete from EtlCurrencyRate r where r.id = ?1", BGN_RATE_ID);
        baseEntityRepository.executeUpdate("delete from EtlCurrencyRate r where r.id = ?1", BDT_RATE_ID);
        baseEntityRepository.executeUpdate("delete from EtlCurrencyRate r where r.id = ?1", BAD_RATE_ID);
    }

    @Before
    public void before() {
        restoreOperday();
        baseEntityRepository.executeNativeUpdate("delete from gl_sched_h where sched_name = ?", LoadCurratesTask.class.getSimpleName());
    }

    /**
     * Проверка загрузка курсов валют
     * @fsd 9.1
     */
    @Test public void test() throws Exception {

        baseEntityRepository.executeNativeUpdate("delete from gl_sched_h");

        final String jobName = LoadCurratesTask.class.getSimpleName() + StringUtils.rsubstr(System.currentTimeMillis() + "", 5);
        SingleActionJob rateJob = SingleActionJobBuilder.create().withClass(LoadCurratesTask.class)
                .withName(jobName).build();

        logger.info("deleted 1: " + baseEntityRepository.executeNativeUpdate(
                "delete from currates where dat = ?", BGN_RATE_ID.getRateDt()));

        CurrencyRate rate = findRate(BGN, BGN_RATE_ID.getRateDt());
        if (rate != null) {          
            baseEntityRepository.executeUpdate("delete from CurrencyRate c where c.id = ?1"
                    , new CurrencyRateId(BGN.getCurrencyCode(), BGN_RATE_ID.getRateDt()));
        }

        rate = findRate(BGN, BGN_RATE_ID.getRateDt());
        Assert.assertNull(rate);

        BigDecimal newRate = new BigDecimal("12.9023");
        EtlCurrencyRate etlRate = new EtlCurrencyRate();
        etlRate.setId(BGN_RATE_ID);
        etlRate.setCurrencyName("Болгария");
        etlRate.setDigitalCode("875");
        etlRate.setNominal(1);
        etlRate.setRate(newRate);
        etlRate = (EtlCurrencyRate) baseEntityRepository.save(etlRate);

//        remoteAccess.invoke(LoadCurratesTask.class, "run", LoadCurratesTask.class.getSimpleName(), new Properties());
        jobService.executeJob(rateJob);

        printAuditLog(10);
        JobHistory history1 = getLastHistRecordObject(jobName);
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED, history1.getResult());

        rate = findRate(BGN, BGN_RATE_ID.getRateDt());
        Assert.assertNotNull(rate);
        Assert.assertEquals(newRate, rate.getRate().setScale(4, ROUND_HALF_UP));

        // меняем курс в etlrate -> будет пропуск
        baseEntityRepository.executeUpdate("update EtlCurrencyRate r set r.rate = ?1 where r.id = ?2"
                , new BigDecimal("15.0001"), BGN_RATE_ID);

//        remoteAccess.invoke(LoadCurratesTask.class, "run", LoadCurratesTask.class.getSimpleName(), new Properties());
        jobService.executeJob(rateJob);

        JobHistory history2 = getLastHistRecordObject(jobName);
        Assert.assertEquals(history1, history2);

        rate = findRate(BGN, BGN_RATE_ID.getRateDt());
        Assert.assertEquals(newRate, rate.getRate().setScale(4, ROUND_HALF_UP));
    }

    /**
     * в таблице currates уже могут быть какие-то курсы, которые мы должны перетереть,
     * если там нет курсов для 5-ти основных валют: USD, EUR, GBP, CHF, JPY
     */
    @Test
    public void testAlreadyPartialyLoaded() throws SQLException {

        logger.info("deleted 1: " + baseEntityRepository.executeNativeUpdate(
                "delete from currates where dat = ?", BGN_RATE_ID.getRateDt()));

        CurrencyRate rate = findRate(BGN, BGN_RATE_ID.getRateDt());
        if (rate != null) {
            baseEntityRepository.executeUpdate("delete from CurrencyRate c where c.id = ?1"
                    , new CurrencyRateId(BGN.getCurrencyCode(), BGN_RATE_ID.getRateDt()));
        }

        rate = findRate(BGN, BGN_RATE_ID.getRateDt());
        Assert.assertNull(rate);

        EtlCurrencyRate newRate = createEtlRate(new BankCurrency(BGN_RATE_ID.getBankCurrency()), BGN_RATE_ID.getRateDt(), new BigDecimal("12.9023"), 1);

        createRate(new BankCurrency("USD"), BGN_RATE_ID.getRateDt(), new BigDecimal("1.00"), new BigDecimal("1"));

        remoteAccess.invoke(LoadCurratesTask.class, "run", LoadCurratesTask.class.getSimpleName(), new Properties());

        printAuditLog(10);
        rate = findRate(BGN, BGN_RATE_ID.getRateDt());
        Assert.assertNotNull(rate);
        Assert.assertEquals(newRate.getRate(), rate.getRate().setScale(4, ROUND_HALF_UP));

        // меняем курс в etlrate -> будет пропуск - курсы уже загружены
        baseEntityRepository.executeUpdate("update EtlCurrencyRate r set r.rate = ?1 where r.id = ?2"
                , new BigDecimal("15.0001"), BGN_RATE_ID);

        remoteAccess.invoke(LoadCurratesTask.class, "run", LoadCurratesTask.class.getSimpleName(), new Properties());
        rate = findRate(BGN, BGN_RATE_ID.getRateDt());
        Assert.assertEquals(newRate.getRate(), rate.getRate().setScale(4, ROUND_HALF_UP));
    }

    private static EtlCurrencyRate createEtlRate(BankCurrency currency, Date ondate, BigDecimal rate, int nominal) {
        currency = (BankCurrency) baseEntityRepository.selectOne(BankCurrency.class, "from BankCurrency c where c.id = ?1", currency.getId());
        Assert.assertNotNull(currency);
        EtlCurrencyRate etlRate = new EtlCurrencyRate();
        etlRate.setId(new EtlCurrencyRateId(currency.getCurrencyCode(), ondate));
        etlRate.setCurrencyName(currency.getCurrencyCode());
        etlRate.setDigitalCode(currency.getDigitalCode());
        etlRate.setNominal(nominal);
        etlRate.setRate(rate);
        return (EtlCurrencyRate) baseEntityRepository.save(etlRate);
    }

    private static CurrencyRate createRate(BankCurrency currency, Date ondate,  BigDecimal rate, BigDecimal amount) {
        CurrencyRate currencyRate = new CurrencyRate(currency, ondate, rate, amount, new BigDecimal("0"));
        return (CurrencyRate) baseEntityRepository.save(currencyRate);
    }

    /**
     * Проверка загрузки при не нашей валюте
     */
    @Test public void testErr() throws SQLException {

        logger.info("deleted 1: " + baseEntityRepository.executeNativeUpdate(
                "delete from currates where dat = ?", BGN_RATE_ID.getRateDt()));

        // BDT	050	TAKA          	Така Бангладеш
        CurrencyRate rate = findRate(new BankCurrency("BDT"), BDT_RATE_ID.getRateDt());
        if (rate != null) {          
            baseEntityRepository.executeUpdate("delete from CurrencyRate c where c.id = ?1"
                    , new CurrencyRateId(BDT.getCurrencyCode(), BDT_RATE_ID.getRateDt()));
        }

        baseEntityRepository.executeNativeUpdate("delete from GL_ETLRATE where CCY_ALPHA_CODE = ?", badCurrency);

        rate = findRate(BDT, BDT_RATE_ID.getRateDt());
        Assert.assertNull(rate);

        BigDecimal newRate = new BigDecimal("12.9023");
        EtlCurrencyRate etlRate = new EtlCurrencyRate();
        etlRate.setId(BDT_RATE_ID);
        etlRate.setCurrencyName("Болгария");
        etlRate.setDigitalCode("875");
        etlRate.setNominal(1);
        etlRate.setRate(newRate);
        etlRate = (EtlCurrencyRate) baseEntityRepository.save(etlRate);

        EtlCurrencyRate badRate = new EtlCurrencyRate();
        badRate.setId(BAD_RATE_ID);
        badRate.setCurrencyName("Ошибка");
        badRate.setDigitalCode("123");
        badRate.setNominal(1);
        badRate.setRate(new BigDecimal("100"));
        badRate = (EtlCurrencyRate) baseEntityRepository.save(badRate);

        remoteAccess.invoke(LoadCurratesTask.class, "run", LoadCurratesTask.class.getSimpleName(), new Properties());

        printAuditLog(10);

        rate = findRate(BDT, BDT_RATE_ID.getRateDt());
        Assert.assertNotNull(rate);
        Assert.assertEquals(newRate, rate.getRate().setScale(4, ROUND_HALF_UP));

        // меняем курс в etlrate -> будет пропуск
        baseEntityRepository.executeUpdate("update EtlCurrencyRate r set r.rate = ?1 where r.id = ?2"
                , new BigDecimal("15.0001"), BDT_RATE_ID);

        baseEntityRepository.executeNativeUpdate("delete from GL_ETLRATE where CCY_ALPHA_CODE = ?", badCurrency);

        remoteAccess.invoke(LoadCurratesTask.class, "run", LoadCurratesTask.class.getSimpleName(), new Properties());
        rate = findRate(BDT, BDT_RATE_ID.getRateDt());
        Assert.assertEquals(newRate, rate.getRate().setScale(4, ROUND_HALF_UP));
    }

    /**
     * - при загрузке курсов грузим "мягкие" валюты из предыдущего ОД
     * - на следующий планируемый к открытию ОД курсы в ETL структуру генерируются в следующий выходной после закрытого ОД,
     * т.е. для следующего после выходных ОД нужно брать курсы из ETL с датой после предыдущего закрытого ОД
     * - белорусский рубль заполняем для BYB BYR
     */
    @Test public void testSoftCurrencyAndHolidays() throws Exception {

        final String HONG_USD = "HKD";

        final Date currentOperdayCOB = DateUtils.parseDate("13.03.2015", "dd.MM.yyyy");
        final Date previousDay = DateUtils.addDays(currentOperdayCOB, -1);
        Assert.assertTrue(1 <= baseEntityRepository.selectFirst(
                "select count(1) from currates where dat = ?", previousDay).getLong(0));

        Date nextHoliday = DateUtils.parseDate("14.03.2015", "dd.MM.yyyy");
        Date nextHoliday2 = DateUtils.parseDate("15.03.2015", "dd.MM.yyyy");
        Date nextOperday = DateUtils.parseDate("16.03.2015", "dd.MM.yyyy");

        logger.info("deleted 0: " + baseEntityRepository.executeNativeUpdate("delete from GL_ETLRATE"));

        // должны быть курсы валют за 13.11
        logger.info("deleted 1: " + baseEntityRepository.executeNativeUpdate(
                "delete from currates where dat = ?", currentOperdayCOB));

        // 'HKD' мягкая валюта
        baseEntityRepository.executeNativeUpdate(
                "insert into currates (" +
                        "select date '" + Utl4Tests.toString(currentOperdayCOB, "yyyy-MM-dd") + "',c.CCY,c.RATE,c.AMNT,c.RATE0\n" +
                "  from currates c\n" +
                " where c.dat = (select max(c0.dat) from currates c0 where c0.ccy = ?))", HONG_USD);

        // вычищаем курсы за след ОД
        logger.info("deleted 2: " + baseEntityRepository.executeNativeUpdate(
                "delete from currates where dat = ?", nextOperday));

        // вычищаем курсы за все выходные
        logger.info("deleted holidays: " + baseEntityRepository.executeNativeUpdate(
                "delete from currates where dat in (?,?)", nextHoliday, nextHoliday2));

        // устан ОД 13.03.2015 COB
        setOperday(currentOperdayCOB, previousDay
                , Operday.OperdayPhase.COB, Operday.LastWorkdayStatus.CLOSED);

        // создаем курс в витрине на 14.03.2015
        BigDecimal newRateSoft = new BigDecimal("4.787878780");
        EtlCurrencyRate etlRateSoft = new EtlCurrencyRate();
        etlRateSoft.setId(new EtlCurrencyRateId(HONG_USD, nextHoliday));
        etlRateSoft.setCurrencyName("Гонконгский Доллар");
        etlRateSoft.setDigitalCode("344");
        etlRateSoft.setNominal(1);
        etlRateSoft.setRate(newRateSoft);
        etlRateSoft = (EtlCurrencyRate) baseEntityRepository.save(etlRateSoft);

        BigDecimal newRateHard = new BigDecimal("50.505050505");
        EtlCurrencyRate etlRateHard = new EtlCurrencyRate();
        etlRateHard.setId(new EtlCurrencyRateId(BankCurrency.USD.getCurrencyCode(), nextHoliday));
        etlRateHard.setCurrencyName("Доллар США");
        etlRateHard.setDigitalCode("840");
        etlRateHard.setNominal(1);
        etlRateHard.setRate(newRateHard);
        etlRateHard = (EtlCurrencyRate) baseEntityRepository.save(etlRateHard);

        BigDecimal newByr = new BigDecimal("37.311300000000");
        EtlCurrencyRate etlRateBul = new EtlCurrencyRate();
        etlRateBul.setId(new EtlCurrencyRateId(BYR.getCurrencyCode(), nextHoliday));
        etlRateBul.setCurrencyName("Белорусский рубль");
        etlRateBul.setDigitalCode("974");
        etlRateBul.setNominal(10000);
        etlRateBul.setRate(newByr);
        etlRateBul = (EtlCurrencyRate) baseEntityRepository.save(etlRateBul);

        // запускаем задачу загрузки курсов
        remoteAccess.invoke(LoadCurratesTask.class, "run", LoadCurratesTask.class.getSimpleName(), new Properties());

        // наличие мягких валют в курсах на 16.11
        CurrencyRate rateIEP = findRate("IEP", nextOperday);
        Assert.assertNotNull(rateIEP);

        CurrencyRate rateHKD = findRate(HONG_USD, nextOperday);
        Assert.assertNotNull(rateHKD);
        Assert.assertEquals(newRateSoft, rateHKD.getRate().setScale(9, ROUND_HALF_UP));

        // курс для валюты равен присутствующей в витрине
        CurrencyRate rateUSD = findRate(BankCurrency.USD.getId(), nextOperday);
        CurrencyRate ratePrevUSD = findRate(BankCurrency.USD.getId(), currentOperdayCOB);
        Assert.assertNotNull(rateUSD);
        Assert.assertEquals(newRateHard, rateUSD.getRate());
        Assert.assertEquals(ratePrevUSD.getRate(), rateUSD.getRatePrev());

        CurrencyRate rateBur = findRate(BYR, nextOperday);
        Assert.assertNotNull(rateBur);
        Assert.assertEquals(newByr.divide(new BigDecimal(10000)).setScale(9, ROUND_HALF_UP), rateBur.getRate().setScale(9, ROUND_HALF_UP));

        CurrencyRate rateBuB = findRate(BYB, nextOperday);
        Assert.assertNotNull(rateBuB);
        Assert.assertEquals(newByr.divide(new BigDecimal(10000)).setScale(9, ROUND_HALF_UP), rateBuB.getRate().setScale(9, ROUND_HALF_UP));

        // повторная загрузка курсов на след раб день не производится
        etlRateHard = (EtlCurrencyRate) baseEntityRepository.findById(EtlCurrencyRate.class, etlRateHard.getId());
        etlRateHard.setRate(new BigDecimal("50.101010101"));
        etlRateHard = (EtlCurrencyRate) baseEntityRepository.update(etlRateHard);

        remoteAccess.invoke(LoadCurratesTask.class, "run", LoadCurratesTask.class.getSimpleName(), new Properties());
        rateUSD = findRate(BankCurrency.USD.getId(), nextOperday);
        Assert.assertNotNull(rateUSD);
        Assert.assertEquals(newRateHard, rateUSD.getRate());

        // загружены курсы в т.ч. за выходные дни 14-го и 15-го
        // за 14-е предыд курс за 13-е
        rateUSD = findRate(BankCurrency.USD.getId(), nextHoliday);
        ratePrevUSD = findRate(BankCurrency.USD.getId(), currentOperdayCOB);
        Assert.assertNotNull(rateUSD);
        Assert.assertEquals(newRateHard, rateUSD.getRate());
        Assert.assertEquals(ratePrevUSD.getRate(), rateUSD.getRatePrev());

        // за 15-е предыд курс ТОЖЕ за 13-е
        rateUSD = findRate(BankCurrency.USD.getId(), nextHoliday2);
        Assert.assertNotNull(rateUSD);
        Assert.assertEquals(newRateHard, rateUSD.getRate());
        Assert.assertEquals(ratePrevUSD.getRate(), rateUSD.getRatePrev());

    }

}
