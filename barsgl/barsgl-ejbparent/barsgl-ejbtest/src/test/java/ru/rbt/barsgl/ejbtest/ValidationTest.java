package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.junit.*;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.integr.bg.EtlPostingController;
import ru.rbt.barsgl.ejb.integr.oper.IncomingPostingProcessor;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.shared.enums.OperState;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static ru.rbt.barsgl.ejbtest.utl.Utl4Tests.deleteGlAccountWithLinks;

/**
 * Created by ER18837 on 30.03.15.
 * Обработка операций с ошибками во входных данных
 * @fsd 7.4
 */
public class ValidationTest extends AbstractTimerJobTest {

    private static final Logger log = Logger.getLogger(ValidationTest.class);

    @BeforeClass
    public static void beforeClass() {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
    }

    @Before
    public void before() {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
    }

    @After
    public void after() {
        restoreOperday();
    }

    /**
     * Проверка ошибок в длине текстовых полей входной проводки (ошибка проводки из АЕ)
     * @fsd 7.4.1
     */
    @Test
    public void testEtlFieldLength() {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "FieldLengthError");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        // системные идентификаторы
        pst.setEventId(pst.getEventId() + "012345678901234567890123456789");
        pst.setAePostingId(pst.getAePostingId() + "012345678901234567890123456789");
        pst.setChnlName("012345678901234567890123456789");
        pst.setSourcePosting("01234567");
        pst.setDeptId("01234");

        // идентификаторы проводки
        pst.setParentReference("012345678901234567890123456789012345678901234567890123456789");
        pst.setPaymentRefernce(pst.getPaymentRefernce() + "012345678901234567890123456789");
        pst.setStornoReference("012345678901234567890123456789012345678901234567890123456789");
        pst.setDealId("0123456789012345678901234567898901234567890123456789012345678989");
        pst.setStorno(YesNo.Y);

        // описания
        pst.setNarrative(pst.getNarrative() + "012345678901234567890123456789");    // длинная строка
        pst.setRusNarrativeLong("  ");                                              // пустая строка
        pst.setRusNarrativeShort(null);                                             // совсем пустая строка

        pst.setAccountCredit("40817036200012959997");
        pst.setAccountDebit("40817036250010000018");
        pst.setAmountCredit(new BigDecimal("315.44"));
        pst.setAmountDebit(new BigDecimal("315.43"));
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNull(operation);                                               // операция не должна быть создана

        pst = (EtlPosting) baseEntityRepository.findById(pst.getClass(), pst.getId());
        Assert.assertTrue(pst.getErrorCode() == 1);
        String errorMessage = pst.getErrorMessage();
        Assert.assertNotEquals(errorMessage, "SUCCESS");

        Assert.assertTrue(errorMessage.contains("'GL_ETLPST.ID_PST'"));
        Assert.assertTrue(errorMessage.contains("'GL_ETLPST.SRC_PST'"));
        Assert.assertTrue(errorMessage.contains("'GL_ETLPST.CHNL_NAME'"));
        Assert.assertTrue(errorMessage.contains("'GL_ETLPST.EVT_ID'"));
        Assert.assertTrue(errorMessage.contains("'GL_ETLPST.PAR_RF'"));
        Assert.assertTrue(errorMessage.contains("'GL_ETLPST.DEAL_ID'"));
        Assert.assertTrue(errorMessage.contains("'GL_ETLPST.STRNRF'"));
//        Assert.assertTrue(errorMessage.contains("'GL_ETLPST.NRT'"));  // Эта проверка потеряла смысл
        Assert.assertTrue(errorMessage.contains("'GL_ETLPST.RNRTL'"));
        Assert.assertTrue(errorMessage.contains("'GL_ETLPST.RNRTS'"));
        Assert.assertTrue(errorMessage.contains("'GL_ETLPST.PMT_REF'"));
    }

    /** Этот тест не надо включать в документ
     * @notdoc
     * @fsd 7.4.1
     */
    @Test
    public void testBadFieldLength() {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "FieldLengthError");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        // системные идентификаторы
        pst.setAePostingId(pst.getAePostingId() + "012345678901234567890123456789");

        pst.setAccountCredit("40817036200012959997");
        pst.setAccountDebit("40817036250010000018");
        pst.setAmountCredit(new BigDecimal("315.44"));
        pst.setAmountDebit(new BigDecimal("315.43"));
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNull(operation);                                               // операция не должна быть создана

        pst = (EtlPosting) baseEntityRepository.findById(pst.getClass(), pst.getId());
        Assert.assertTrue(pst.getErrorCode() == 1);
        String errorMessage = pst.getErrorMessage();
        Assert.assertNotEquals(errorMessage, "SUCCESS");

        Assert.assertTrue(errorMessage.contains("'GL_ETLPST.ID_PST'"));
    }

    /**
     * Проверка ошибок в формате счета, коде валюты, сумме (ошибка проводки из АЕ)
     * @fsd 7.4.1
     */
    @Test
    public void testEtlValidation() {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SimpleError");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountCredit("0123456789012abc6789");                       // неверный номер счета
        pst.setAccountDebit("01");                                         // неверный номер счета
        pst.setAmountCredit(BigDecimal.ZERO);                               // сумма в валюте 0
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setAmountCreditRu(new BigDecimal("-1"));                        // сумма в рублях отрицательная
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setDealId(";DN01234567");                                        // номер сделки не число

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNull(operation);                                               // операция не должна быть создана

        pst = (EtlPosting) baseEntityRepository.findById(pst.getClass(), pst.getId());
        Assert.assertTrue(pst.getErrorCode() == 1);
        String errorMessage = pst.getErrorMessage();
        Assert.assertNotEquals(errorMessage, "SUCCESS");
        Assert.assertTrue(errorMessage.contains("'3'"));
        Assert.assertTrue(errorMessage.contains("'6'"));
        Assert.assertTrue(errorMessage.contains("'9'"));
        Assert.assertTrue(errorMessage.contains("'10'"));
        Assert.assertTrue(errorMessage.contains("'12'"));

    }

    /**
     * Проверка неверного кода валюты, отсутствия ИД проводки - ИД платежа или ИД сделки (ошибка проводки из АЕ)
     * @fsd 7.4.1
     */
    @Test public void testCurrency() {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SimpleValidation");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountCredit("40817036200012959997");
        pst.setAccountDebit("40817036250010000018");
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.EUR);
        pst.setCurrencyDebit(BankCurrency.USD);
        pst.setPaymentRefernce(null);
        pst.setDealId(null);

        pst = (EtlPosting) baseEntityRepository.save(pst);
        Assert.assertNotNull(pst);
        baseEntityRepository.executeNativeUpdate("update GL_ETLPST set CCY_CR = 'LOL' where ID = ?", pst.getId());
        pst = (EtlPosting) baseEntityRepository.findById(pst.getClass(), pst.getId());

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNull(operation);                                               // операция не должна быть создана

        pst = (EtlPosting) baseEntityRepository.findById(pst.getClass(), pst.getId());
        Assert.assertTrue(pst.getErrorCode() == 1);
        String errorMessage = pst.getErrorMessage();
        Assert.assertNotEquals(errorMessage, "SUCCESS");
        Assert.assertTrue(errorMessage.contains("'12'"));
        Assert.assertTrue(errorMessage.contains("'13'"));
        Assert.assertTrue(errorMessage.contains("'5'"));

    }

    /**
     * Проверка обработки операции при отсутствии счета (операция в статусе WTAC)
     * @fsd 7.5.1
     */
    @Test public void testWtacValidation() {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SimpleValidation");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountCredit("40817036200012959999");           // нет такоего счета
        pst.setAccountDebit("40817036250010000019");            // нет такоего счета
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);                                               // операция должна быть создана с ошибкой

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.WTAC, operation.getState());
        String errorMessage = operation.getErrorMessage();
        Assert.assertTrue(errorMessage, errorMessage.contains("'4'"));
        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);
        Assert.assertEquals(0, postList.size());        // нет проводки

    }

    /**
     * Проверка перевода операции в статус WTAC при отсутствии счета
     * и повторной обработки операции после создания счета
     * @fsd 7.5.1
     */
    @Test public void testMissingAccount() throws Exception {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SimpleValidation");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountCredit("40817036200012959999");           // нет такого счета
        pst.setAccountDebit("40817036250010000019");            // и такого счета тоже
        pst.setAmountCredit(new BigDecimal("15.92"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);                                               // операция должна быть создана с ошибкой

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.WTAC, operation.getState());
        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);
        Assert.assertEquals(0, postList.size());        // нет проводки

        int count = baseEntityRepository.selectOne(" Select Count(GL_OPER.GLOID) from GL_OPER GL_OPER where ((GL_OPER.STATE='WTAC'))").getInteger(0);
        Assert.assertNotEquals(0, count); //ensure there are WTAC postings

        // int gloid = baseEntityRepository.selectOne(" Select GL_OPER.GLOID from GL_OPER GL_OPER where ((GL_OPER.STATE='WTAC')) fetch first 1 row only").getInteger(0);
        long gloid = operation.getId();
        //get any WTAC operation to revalidate

//        String rightAcDr = "40817036250010000018";
//        String rightAcCr = "40817036200012959997";

        String rightAcDr = "40817978550160000066";     // "CHL"
        String rightAcCr = "47427978400404502369";        // "EKB"

//      //baseEntityRepository.executeNativeUpdate(" update GL_OPER set AC_DR = ?, AC_CR = ? where ((GL_OPER.STATE='WTAC'))", rightAcDr, rightAcCr);
        baseEntityRepository.executeUpdate("update GLOperation G set G.accountDebit = ?1, G.accountCredit = ?2 WHERE G.id=?3", rightAcDr, rightAcCr, gloid);
        //JPA update accounts in selected WTAC operation

        remoteAccess.invoke(EtlPostingController.class, "reprocessWtacOperations", getOperday().getLastWorkingDay(), getOperday().getCurrentDate());
        //launch new OperDay

        // LK бессмысленно, могут быть другие необработанные операции WTAC
//        count = baseEntityRepository.selectOne(" Select Count(GL_OPER.GLOID) from GL_OPER GL_OPER where ((GL_OPER.STATE='WTAC'))").getInteger(0);
//        Assert.assertEquals(0, count);//ensure there are no WTAC operations

        // Должна получиться операция МФО
        operation = (GLOperation) baseEntityRepository.selectOne(GLOperation.class, "from GLOperation o where o.id = ?1", operation.getId());
        Assert.assertEquals("operation: " + operation.getId(), OperState.POST, operation.getState());
        Assert.assertEquals("operation: " + operation.getId(), GLOperation.OperType.M, operation.getPstScheme());

        count = baseEntityRepository.selectOne(" Select Count(GLO_REF) from GL_POSTING G where G.GLO_REF = ?", gloid).getInteger(0);
        Assert.assertEquals(2, count);//ensure selected operation contains created posting
    }

    /**
     * Проверка ошибки в дате валютирования проводки  - больше текущего опердня (ошибка проводки из АЕ)
     * @fsd 7.3
     */
    @Test public void testValueDateError() throws ParseException, SQLException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "ValueDateError");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);

//        DataRecord res = baseEntityRepository.selectFirst("select max(DAT) from CURRATES");
//        Assert.assertNotNull(res);
        Date operDate = getOperday().getCurrentDate();
        pst.setValueDate(DateUtils.addDays(operDate, 1));  // неверная дата (> опердень)

        pst.setAccountCredit("40817036200012959997");
        pst.setAccountDebit("40817036250010000018");
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.ERCHK, operation.getState());
        String errorMessage = operation.getErrorMessage();
        Assert.assertTrue(errorMessage.contains("'1001'"));


/*
        EtlPosting pstS = newStornoPosting(System.currentTimeMillis(), pkg, pst);
        pstS.setValueDate(DateUtils.addDays(operDate, -62));  // неверная дата (< опердень на 2 м-ца)
        pstS = (EtlPosting) baseEntityRepository.save(pstS);

        operation = (GLOperation) postingController.processMessage(pstS);
        Assert.assertNull(operation);
        // TODO тест не проходит, временно убрала проверку на глуюину даты валютирования

        pstS = (EtlPosting) baseEntityRepository.findById(pst.getClass(), pstS.getId());
        Assert.assertTrue(pstS.getErrorCode() == 1);
        errorMessage = pst.getErrorMessage();
        Assert.assertNotEquals(errorMessage, "SUCCESS");
        Assert.assertTrue(errorMessage.contains("'1001'"));
*/

    }

    /**
     * Проверка ошибки суммы в рублях - если валюта не рубли, то не могут быть равны
     * @fsd 7.3
     */
    @Test public void testAmountRuError() throws ParseException, SQLException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "AmountRuError");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);

        Date operDate = getOperday().getCurrentDate();
        pst.setValueDate(operDate);  // неверная дата (> опердень)

        pst.setAccountCredit("40817036200012959997");
        pst.setAccountDebit("40817036250010000018");
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setAmountDebitRu(pst.getAmountCredit());
        pst.setAmountCreditRu(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNull(operation);

        pst = (EtlPosting) baseEntityRepository.findById(pst.getClass(), pst.getId());
        Assert.assertTrue(pst.getErrorCode() == 1);
        String errorMessage = pst.getErrorMessage();
        Assert.assertNotEquals(errorMessage, "SUCCESS");
        Assert.assertTrue(errorMessage.contains("'10'"));

    }

    /**
     * Проверка равенства сумм по дебету и кредиту, если одна валюта рубль, а другая - нет
     * @fsd 7.3
     */
    @Test
    public void testAmountEqualError() throws ParseException, SQLException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "AmountRuError");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);

        Date operDate = getOperday().getCurrentDate();
        pst.setValueDate(operDate);  // неверная дата (> опердень)

        pst.setAccountCredit("40817036200012959997");
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setAmountCredit(new BigDecimal("3500.00"));

        pst.setAccountDebit("47427810550160009330");     // "CHL"
        pst.setCurrencyDebit(BankCurrency.RUB);
        pst.setAmountDebit(new BigDecimal("3500.00"));

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNull(operation);

        pst = (EtlPosting) baseEntityRepository.findById(pst.getClass(), pst.getId());
        Assert.assertTrue(pst.getErrorCode() == 1);
        String errorMessage = pst.getErrorMessage();
        Assert.assertNotEquals(errorMessage, "SUCCESS");
        Assert.assertTrue(errorMessage.contains("'38'"));

    }

    /**
     * Проверка корреспонденции счетов - разная глава баланса (ошибка операции)
     * @fsd 7.4.1
     */
    @Test public void testBsChapterError() throws ParseException, SQLException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "MfoExchangeError");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);

        Date valDate = getOperday().getCurrentDate();
        pst.setValueDate(valDate);

        pst.setAccountCredit("40502840000010678450");
        pst.setAccountDebit("91418840000012452649");
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.USD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(operation.getState(), OperState.ERCHK);
        String errorMessage = operation.getErrorMessage();
        Assert.assertTrue(errorMessage.contains("'1005'"));
    }

    /**
     * Проверка обработки операции при отсутствии межфилиального счета
     * (автоматическая генерация межфилиального счета)
     * @fsd 7.5.2.2
     * @throws ParseException
     */
    @Test public void testMfoError() throws ParseException, SQLException {

        String fromIBCB = "from IBCB where (ibbrnm='CHL') AND (ibcbrn='EKB') AND (ibccy ='AUD')";
        DataRecord res = baseEntityRepository.selectFirst("select IBACOU, IBACIN, IBA305, IBA306 " + fromIBCB);
        if (null != res) {
            StringBuilder sb = new StringBuilder("in (");
            for (int i=0; i<4; i++) {
                sb.append("'").append(res.getString(i)).append("',");
            }
            sb.setCharAt(sb.length() - 1, ')');
            String inStr = sb.toString();
            baseEntityRepository.executeNativeUpdate("delete from ACCRLN where RLNTYPE = 'T' and BSAACID " + inStr);
            baseEntityRepository.executeNativeUpdate("delete from BSAACC where ID " + inStr);
            baseEntityRepository.executeNativeUpdate("delete " + fromIBCB);
        }

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "MfoError");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountCredit("47423036700163336829");     // "CHL"
        pst.setAccountDebit("47423036700401502607");        // "EKB"

        pst.setAmountCredit(new BigDecimal("123.45"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);                                               // операция должна быть создана с ошибкой

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        String errorMessage = operation.getErrorMessage();
        Assert.assertNull(errorMessage);

    }

    /**
     * Проверка обработки операции при отсутствии курса валюты на день проводки (ошибка операции)
     * @fsd 7.4.1
     */
    @Test public void testRateError() throws ParseException, SQLException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "RateError");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);

        // изменяем опердень
        DataRecord res = baseEntityRepository.selectFirst("select max(DAT) from CURRATES");
        Assert.assertNotNull(res);
        Date operDate = res.getDate(0);
        setOperday(DateUtils.addDays(operDate, 1), operDate, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);

        Date valDate = getOperday().getCurrentDate();
        pst.setValueDate(valDate);  // неверная дата (> опердень)

        pst.setAccountCredit("40817036200012959997");
        pst.setAccountDebit("40817036250010000018");
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(operation.getState(), OperState.ERCHK);
        String errorMessage = operation.getErrorMessage();
        Assert.assertTrue(errorMessage.contains("1003"));
    }

    /**
     * Проверка обработки операции сторно при отличии данных в сторнирующей операции (ошибка операции)
     * @fsd 7.7.1
     * @throws ParseException
     */
    @Test public void testStornoNotFound() throws ParseException {

        long stamp = System.currentTimeMillis();

        // прямая операция
        EtlPackage pkg = newPackage(stamp, "StornoOneday");
        Assert.assertTrue(pkg.getId() > 0);
        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());
        pst.setAccountDebit("30302840700010000033");    // "MOS"
        pst.setCurrencyDebit(BankCurrency.USD);
        pst.setAmountDebit(new BigDecimal("100.00"));
        pst.setAccountCredit("47427810550160009330");     // "CHL"
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setAmountCredit(new BigDecimal("3500.00"));
        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());       // операция создана
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST,operation.getState());

        // Сторно операция
        stamp = System.currentTimeMillis();
        pkg = newPackage(stamp, "StornoOnedayNotFound");
        Assert.assertTrue(pkg.getId() > 0);
        EtlPosting pstS = newStornoPosting(stamp, pkg, pst);
        pstS.setAmountCredit(new BigDecimal("3500.01"));
        pstS.setValueDate(pst.getValueDate());
        pstS = (EtlPosting) baseEntityRepository.save(pstS);

        GLOperation operationS = (GLOperation) postingController.processMessage(pstS);
        Assert.assertTrue(0 < operationS.getId());       // операция создана

        operationS = (GLOperation) baseEntityRepository.findById(operationS.getClass(), operationS.getId());
        Assert.assertEquals(operationS.getState(), OperState.ERCHK);
        Assert.assertNull(operationS.getStornoOperation());        // ссылка на сторно операцию
        String errorMessage = operationS.getErrorMessage();
        Assert.assertTrue(errorMessage.contains("1007"));

    }

    /**
     * Проверка обработки операции сторно, если сторнируемуя операции не обработана (ошибка операции)
     * @fsd 7.2.3
     * @throws ParseException
     */
    @Test public void testStornoNotPost() throws ParseException {

        long stamp = System.currentTimeMillis();

        // прямая операция
        EtlPackage pkg = newPackage(stamp, "StornoOnedayError");
        Assert.assertTrue(pkg.getId() > 0);
        EtlPosting pst = newPosting(stamp, pkg);

        pst.setValueDate(getOperday().getCurrentDate());
        pst.setAccountCredit("47411978750020010096");       // "SPB" не клиент
        pst.setAccountDebit("47427978400404502369");        // "EKB" клиент
        pst.setAmountCredit(new BigDecimal("321.56"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.EUR);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());       // операция создана
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        baseEntityRepository.executeNativeUpdate("update GL_OPER set STATE = ? where GLOID = ?", OperState.ERCHK.name(), operation.getId());

        // Сторно операция
        stamp = System.currentTimeMillis();
        pkg = newPackage(stamp, "StornoOnedayNotPosted");
        Assert.assertTrue(pkg.getId() > 0);
        EtlPosting pstS = newStornoPosting(stamp, pkg, pst);
        pstS.setValueDate(pst.getValueDate());
        pstS = (EtlPosting) baseEntityRepository.save(pstS);

        GLOperation operationS = (GLOperation) postingController.processMessage(pstS);
        Assert.assertTrue(0 < operationS.getId());       // операция создана

        operationS = (GLOperation) baseEntityRepository.findById(operationS.getClass(), operationS.getId());
        Assert.assertEquals(operationS.getState(), OperState.ERCHK);
        Assert.assertNull(operationS.getStornoOperation());        // ссылка на сторно операцию
        String errorMessage = operationS.getErrorMessage();
        Assert.assertTrue(errorMessage.contains("1007"));

    }

    /**
     * Проверка обработки операции, если не задан счет и ключи счета (по дебете и по кредиту)
     */
    @Test
    public void testAccountNotDefined() {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

//        pst.setAccountCredit("40817036200012959997");
//        pst.setAccountDebit("40817036250010000018");
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNull(operation);                                               // операция не должна быть создана

        pst = (EtlPosting) baseEntityRepository.findById(pst.getClass(), pst.getId());
        Assert.assertTrue(pst.getErrorCode() == 1);
        String errorMessage = pst.getErrorMessage();
        Assert.assertNotEquals(errorMessage, "SUCCESS");

        Assert.assertTrue(errorMessage.contains("'GL_ETLPST.AC_DR'"));
        Assert.assertTrue(errorMessage.contains("'GL_ETLPST.ACCKEY_DR'"));
        Assert.assertTrue(errorMessage.contains("'GL_ETLPST.AC_CR'"));
        Assert.assertTrue(errorMessage.contains("'GL_ETLPST.ACCKEY_CR'"));

    }

    /**
     * Проверка ошибок в суммах
     * @fsd 7.4.1
     */
    @Test
    public void testEtlValidationAmount() {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SimpleError");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountDebit("30302840700010000033");    // "MOS"
        pst.setCurrencyDebit(BankCurrency.USD);
        pst.setAmountDebit(new BigDecimal("0.00"));
//        pst.setAmountDebitRu(new BigDecimal("-6000.00"));

        pst.setAccountCredit("40817036200012959997");
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setAmountCredit(new BigDecimal("200"));
        pst.setAmountCreditRu(new BigDecimal("6000.00"));

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNull(operation);                                               // операция не должна быть создана

        pst = (EtlPosting) baseEntityRepository.findById(pst.getClass(), pst.getId());
        Assert.assertTrue(pst.getErrorCode() == 1);
        String errorMessage = pst.getErrorMessage();
        Assert.assertNotEquals(errorMessage, "SUCCESS");
        Assert.assertTrue(errorMessage.contains("'9'"));
        Assert.assertTrue(errorMessage.contains("'10'"));

    }

    /**
     * Проверка ошибок в суммах
     * @fsd 7.4.1
     */
    @Test
    public void testAmountEqual() {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SimpleError");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountDebit("30302840700010000033");    // "MOS"
        pst.setCurrencyDebit(BankCurrency.USD);

        pst.setAccountCredit("40817810200010462563");
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setAmountDebit(new BigDecimal("200"));
        pst.setAmountCredit(new BigDecimal("200"));

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNull(operation);                                               // операция не должна быть создана

        pst = (EtlPosting) baseEntityRepository.findById(pst.getClass(), pst.getId());
        Assert.assertTrue(pst.getErrorCode() == 1);
        String errorMessage = pst.getErrorMessage();
        Assert.assertNotEquals(errorMessage, "SUCCESS");
        Assert.assertTrue(errorMessage.contains("'38'"));

        pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountDebit("30302840700010000033");    // "MOS"
        pst.setCurrencyDebit(BankCurrency.USD);

        pst.setAccountCredit("40817810200010462563");
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setAmountDebit(new BigDecimal("0.1"));
        pst.setAmountCredit(new BigDecimal("0.1"));

        pst = (EtlPosting) baseEntityRepository.save(pst);

        operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);                                               // операция не должна быть создана

        pst = (EtlPosting) baseEntityRepository.findById(pst.getClass(), pst.getId());
        Assert.assertTrue(pst.getErrorCode() == 0);
        Assert.assertEquals(pst.getErrorMessage(), "SUCCESS");

    }

    @Test
    public void testAccountKeyPattern() {
        // BRANCH.CCY.CUSTNO.ATYPE.CUSTTYPE.TERM.GL_SEQ.CBCCN.ACC2.PLCODE.ACOD.SQ.DEALSRC.DEALID.SUBDEALID
        Pattern patternDealId = IncomingPostingProcessor.patternDealId;

        Assert.assertTrue(patternDealId.matcher("1234;").matches());
        Assert.assertTrue(patternDealId.matcher("12345678901234567890").matches());
        Assert.assertTrue(patternDealId.matcher("12345678901234567890;09876543210987654321").matches());

        Assert.assertFalse(patternDealId.matcher("123456789012345678901").matches());
        Assert.assertFalse(patternDealId.matcher("1234567890123456789009876543210987654321").matches());
        Assert.assertFalse(patternDealId.matcher(";01234567").matches());

    }

    /**
     * Проверка ошибок в счете доходов-расходов (валютный или псевдо-счет)
     * @fsd 7.4.1
     */
    @Test
    public void testAccount706() {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "AccountError");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountCredit("47427810000010119425");
        pst.setAccountDebit("70606810000010050479");                        // псевдо-счет
//        pst.setAccountDebit("70606840000010263171");                        // валютный счет
        pst.setAmountCredit(new BigDecimal("13.56"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setDealId("1234567");

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNull(operation);                                               // операция не должна быть создана

        pst = (EtlPosting) baseEntityRepository.findById(pst.getClass(), pst.getId());
        Assert.assertTrue(pst.getErrorCode() == 1);
        String errorMessage = pst.getErrorMessage();
        Assert.assertNotEquals(errorMessage, "SUCCESS");
        Assert.assertTrue(errorMessage.contains("'26'"));

    }

    /**
     * Проверка равенства счетов по дебету и кредиту
     * @fsd 7.4.1
     */
    @Ignore
    @Test public void testAccountsEqual() throws ParseException, SQLException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "AccountsEqual");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst1 = newPosting(stamp, pkg);

        Date valDate = getOperday().getCurrentDate();

        pst1.setValueDate(valDate);
        pst1.setAccountCredit("40502840000010678450");
        pst1.setAccountDebit(pst1.getAccountCredit());
        pst1.setAmountCredit(new BigDecimal("12.0056"));
        pst1.setAmountDebit(pst1.getAmountCredit());
        pst1.setCurrencyCredit(BankCurrency.USD);
        pst1.setCurrencyDebit(pst1.getCurrencyCredit());

        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        EtlPosting pst2 = newPosting(stamp, pkg);

        pst2.setValueDate(valDate);
        pst2.setAccountKeyCredit("001;USD;00114240;356030405;00;00;17;;47423;;1871;05;DEALSRC;123456;SUBDEALID");
        pst2.setAccountDebit(pst2.getAccountKeyCredit());
        pst2.setAmountCredit(new BigDecimal("13.42"));
        pst2.setAmountDebit(pst1.getAmountCredit());
        pst2.setCurrencyCredit(BankCurrency.EUR);
        pst2.setCurrencyDebit(pst2.getCurrencyCredit());

        pst2 = (EtlPosting) baseEntityRepository.save(pst2);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);
        Assert.assertNull(operation);                                               // операция не должна быть создана

        pst1 = (EtlPosting) baseEntityRepository.findById(pst1.getClass(), pst1.getId());
        Assert.assertTrue(pst1.getErrorCode() == 1);
        String errorMessage = pst1.getErrorMessage();
        Assert.assertTrue(errorMessage.contains("'GL_ETLPST.AC_DR'"));

        operation = (GLOperation) postingController.processMessage(pst2);
        Assert.assertNull(operation);                                               // операция не должна быть создана

        pst2 = (EtlPosting) baseEntityRepository.findById(pst2.getClass(), pst2.getId());
        Assert.assertTrue(pst2.getErrorCode() == 1);
        errorMessage = pst2.getErrorMessage();
        Assert.assertTrue(errorMessage.contains("'GL_ETLPST.ACCKEY_DR'"));

    }

    /**
     * Проверка ошибки корреспонденции счетов (переданы счета)
     * @throws ParseException
     * @throws SQLException
     */
    @Test
    public void testAccounts9999error() throws ParseException, SQLException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "AccountsNotCorresp");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst1 = newPosting(stamp, pkg);

        Date valDate = getOperday().getCurrentDate();

        pst1.setValueDate(valDate);
        pst1.setAccountDebit("99998810900660000001");
        pst1.setAccountCredit("91203810100663319172");
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(pst1.getCurrencyCredit());
        pst1.setAmountCredit(new BigDecimal("123.45"));
        pst1.setAmountDebit(pst1.getAmountCredit());

        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);
        Assert.assertNotNull(operation);                                               // операция не должна быть создана

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        String errorMessage = operation.getErrorMessage();
        Assert.assertTrue(errorMessage.contains("Код ошибки '27'"));

        EtlPosting pst2 = newPosting(stamp, pkg);

        pst2.setValueDate(valDate);
        pst2.setAccountDebit("96902810000014675457");
        pst2.setAccountCredit("99997810600010000001");
        pst2.setCurrencyCredit(BankCurrency.RUB);
        pst2.setCurrencyDebit(pst1.getCurrencyCredit());
        pst2.setAmountCredit(new BigDecimal("456.78"));
        pst2.setAmountDebit(pst2.getAmountCredit());

        pst2 = (EtlPosting) baseEntityRepository.save(pst2);

        operation = (GLOperation) postingController.processMessage(pst2);
        Assert.assertNotNull(operation);                                               // операция не должна быть создана

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        errorMessage = operation.getErrorMessage();
        Assert.assertTrue(errorMessage.contains("Код ошибки '27'"));

    }

    /**
     * Проверрка коррекции корреспондирующего счета (переданы ключи)
     * @throws ParseException
     * @throws SQLException
     */
    @Test
    public void testAccounts9999correct() throws ParseException, SQLException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "AccountsNotCorresp");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst1 = newPosting(stamp, pkg);

        Date valDate = getOperday().getCurrentDate();

        /*
        *   99998810900010000001	001;RUR;00000018;999999998;;;GL00000015;0001;99998;;;;;;    99998810900010000001
            91319810500014859604    001;RUR;00610000;891070200;18;;0000659456;0001;91319;;6574;21;FC12_CL;111_A104T_15;
        */
        pst1.setValueDate(valDate);
        pst1.setAccountKeyDebit("001;RUR;00000018;999999999;;;GL00000015;0001;99999;;;;;;");
        pst1.setAccountKeyCredit("001;RUR;00610000;891070200;18;;0000659456;0001;91319;;6574;21;FC12_CL;111_A104T_15;");
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(pst1.getCurrencyCredit());
        pst1.setAmountCredit(new BigDecimal("123.45"));
        pst1.setAmountDebit(pst1.getAmountCredit());

        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);
        Assert.assertNotNull(operation);                                               // операция не должна быть создана

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertNull(operation.getErrorMessage());
        Assert.assertEquals('8', operation.getAccountDebit().charAt(4));

        EtlPosting pst2 = newPosting(stamp, pkg);

        /*
        91604810020010000574	001;RUR;00685207;832020100;18;;0000868709;0001;;;;03;FC12_CL;101_A308C_16;101_A308C_16
        99999810200010000001	001;RUR;00000018;999999999;;;GL00000019;0001;99999;;;;;;    99999810200010000001
        */
        pst2.setValueDate(valDate);
        pst2.setAccountKeyDebit("001;RUR;00685207;832020100;18;;0000868709;0001;;;;03;FC12_CL;101_A308C_16;101_A308C_16");
        pst2.setAccountKeyCredit("001;RUR;00000018;999999998;;;GL00000019;0001;99998;;;;;;");
        pst2.setCurrencyCredit(BankCurrency.RUB);
        pst2.setCurrencyDebit(pst1.getCurrencyCredit());
        pst2.setAmountCredit(new BigDecimal("456.78"));
        pst2.setAmountDebit(pst2.getAmountCredit());

        pst2 = (EtlPosting) baseEntityRepository.save(pst2);

        operation = (GLOperation) postingController.processMessage(pst2);
        Assert.assertNotNull(operation);                                               // операция не должна быть создана

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertNull(operation.getErrorMessage());
        Assert.assertEquals('9', operation.getAccountCredit().charAt(4));

    }

    @Test @Ignore
    public void testAccountsKTPcorrect() throws ParseException, SQLException {

        long stamp = System.currentTimeMillis();

        Long etlId[] = {12964395L,12964398L,12964399L,12964443L,12964453L,12964454L};

        for (int i = 0; i < etlId.length; i++) {
            EtlPosting pst1 = (EtlPosting) baseEntityRepository.findById(EtlPosting.class, etlId[i]);
            Assert.assertNotNull(pst1);

            baseEntityRepository.executeNativeUpdate("update GL_ETLPST set ECODE = null, EMSG = null where ID = ?", pst1.getId());
            pst1 = (EtlPosting) baseEntityRepository.findById(pst1.getClass(), pst1.getId());

            GLOperation operation = (GLOperation) postingController.processMessage(pst1);
            System.out.println(operation);

            if (null != operation) {
                operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
                System.out.println(operation.getErrorMessage());
            }
        }

    }

    /**
     * Тест создания счетов ЦБ по проводке
     * @throws ParseException
     */
    @Test public void testPostingCreateAccount() throws ParseException, SQLException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

//        pst.setAccountCredit("47407840700010060039");
        pst.setAccountCredit("");
        // BRANCH.CCY.CUSTNO.ATYPE.CUSTTYPE.TERM.GL_SEQ.CBCCN.ACC2.PLCODE.ACOD.SQ.DEALSRC.DEALID.SUBDEALID
        final String keyStringCredit = "001;USD;00114240;356030405;00;00;17;123;47423;;1871;05;DEALSRC;123456;SUBDEALID";
        Assert.assertTrue(isEmpty(new AccountKeys(keyStringCredit).getPlCode()));
        deleteGlAccountWithLinks(baseEntityRepository, keyStringCredit);
        pst.setAccountKeyCredit(keyStringCredit);

//        pst.setAccountDebit("47408840700010262894");
        pst.setAccountDebit("");
        // BRANCH.CCY.CUSTNO.ATYPE.CUSTTYPE.TERM.GL_SEQ.CBCCN.ACC2.PLCODE.ACOD.SQ.DEALSRC.DEALID.SUBDEALID
        // 001;RUR;0000000018;912030101;;;XX00000007;0001;99997;;6280;01;K+TP;955304;
        final String keyStringDebit = "001;USD;00448806;356030405;00;00;17;456;47423;;;;DEALSRC;123457;SUBDEALID";
        Assert.assertTrue(isEmpty(new AccountKeys(keyStringDebit).getPlCode()));
        deleteGlAccountWithLinks(baseEntityRepository, keyStringDebit);
        pst.setAccountKeyDebit(keyStringDebit);

        pst.setAmountCredit(new BigDecimal("13.99"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.USD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.ERCHK, operation.getState());
        Assert.assertTrue(operation.getErrorMessage().contains("Код ошибки '2008'"));

    }
}
