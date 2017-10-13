package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.entity.gl.*;
import ru.rbt.barsgl.ejb.integr.bg.ManualOperationController;
import ru.rbt.barsgl.ejb.integr.bg.ManualPostingController;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.barsgl.shared.enums.BatchPostAction;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.ejbcore.util.StringUtils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

import static ru.rbt.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by ER18837 on 17.08.15.
 */
public class ManualOperationIT extends AbstractTimerJobIT {

    private final Long USER_ID = 1L;

    @Before
    public void beforeClass() {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);

        // все права пользователю 1
        Utl4Tests.grantAllPerission(USER_ID, baseEntityRepository);
    }
    /**
     * Создание и обработка операции по ручному вводу
     */
    @Test
    public void test() throws SQLException {

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20202810_0001%1");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810_0001%3");
        ManualOperationWrapper wrapper = newOperationWrapper("А",
                "MOS", bsaDt, "RUR", new BigDecimal("12.056"),
                "MOS", bsaCt, "RUR", new BigDecimal("12.056")
        );

        final String src = "FC12_CL";
        final String dealId = "DD_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 4);
        final String subDealId = "SD_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 4);
        wrapper.setDealSrc(src);
        wrapper.setSubdealId(subDealId);
        wrapper.setDealId(dealId);
        wrapper.setInputMethod(InputMethod.M);

        BatchPosting posting = createAuthorizedPosting(wrapper, USER_ID);
        GLManualOperation res = remoteAccess.invoke(ManualOperationController.class, "processPosting", posting, false);
        Assert.assertTrue(0 < res.getId());

        GLManualOperation operation = (GLManualOperation) baseEntityRepository.findById(GLManualOperation.class, res.getId());
        GLOperation opera = (GLOperation) baseEntityRepository.findById(GLOperation.class, res.getId());
        Assert.assertEquals(OperState.POST, opera.getState());

        Assert.assertEquals(src, opera.getSourcePosting());
        Assert.assertEquals(dealId, opera.getDealId());
        Assert.assertEquals(subDealId, opera.getSubdealId());
    }

    /**
     * Создание и обработка операции по ручному вводу из PaymentHub
     */
    @Test
    public void testPH() throws SQLException {

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20202810_0001%2");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810_0001%4");
        ManualOperationWrapper wrapper = newOperationWrapper("А",
                "MOS", bsaDt, "RUR", new BigDecimal("12.056"),
                "MOS", bsaCt, "RUR", new BigDecimal("12.056")
        );

        final String src = "PH";
        final String dealId = "DD_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 4);
        final String subDealId = "SD_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 4);
        wrapper.setDealSrc(src);
        wrapper.setSubdealId(subDealId);
        wrapper.setDealId(dealId);
        wrapper.setPaymentRefernce(dealId);
        wrapper.setInputMethod(InputMethod.M);

        BatchPosting posting = createAuthorizedPosting(wrapper, USER_ID);

        GLManualOperation res = remoteAccess.invoke(ManualOperationController.class, "processPosting", posting, false);
        Assert.assertTrue(0 < res.getId());

        GLManualOperation operation = (GLManualOperation) baseEntityRepository.findById(GLManualOperation.class, res.getId());
        Assert.assertEquals(OperState.POST, operation.getState());

        Assert.assertEquals(src, operation.getSourcePosting());
        Assert.assertEquals(dealId, operation.getDealId());
        Assert.assertEquals(subDealId, operation.getSubdealId());
        Assert.assertEquals(dealId, operation.getPaymentRefernce());
    }

    /**
     * Создание и обработка операции по ручному вводу в режиме BUFFER
     */
    @Test
    public void testBuffer() throws SQLException {

        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN, Operday.PdMode.BUFFER);

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20202810_0001%5");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810_0001%7");
        ManualOperationWrapper wrapper = newOperationWrapper("А",
                "MOS", bsaDt, "RUR", new BigDecimal("12.056"),
                "MOS", bsaCt, "RUR", new BigDecimal("12.056")
        );

        final String src = "K+TP";
        final String dealId = "DD_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 4);
        final String subDealId = "SD_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 4);
        wrapper.setDealSrc(src);
        wrapper.setSubdealId(subDealId);
        wrapper.setDealId(dealId);
        wrapper.setInputMethod(InputMethod.M);

        BatchPosting posting = createAuthorizedPosting(wrapper, USER_ID);
        GLManualOperation res = remoteAccess.invoke(ManualOperationController.class, "processPosting", posting, false);
//        Assert.assertFalse(res.isError());
//        wrapper = res.getResult();
        Assert.assertTrue(0 < res.getId());

        GLManualOperation operation = (GLManualOperation) baseEntityRepository.findById(GLManualOperation.class, res.getId());
        Assert.assertEquals(OperState.POST, operation.getState());

        List<GLPosting> postList = getPostings(operation);
        Assert.assertTrue(postList.isEmpty());                 //  нет проводок, только буфер

        List<GLPd> pdList = getGLPostingPd(operation);
        GLPd pdDr = Utl4Tests.findDebitPd(pdList);
        GLPd pdCr = Utl4Tests.findCreditPd(pdList);
        Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит
        Assert.assertTrue(pdCr.getCcy().equals(pdDr.getCcy()));                 // валюта одинаковый

        Assert.assertTrue(pdCr.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr.getAmount() == -pdDr.getAmount());       // сумма в валюте дебет - кредит

    }

    /**
     * Создание и обработка операции по ручному вводу BACKVALUE
     */
    @Test
    public void testManualBackvalue() throws SQLException {

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20202810_0001%6");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810_0001%8");
        cleanBackvaluejournal(bsaDt, bsaCt);
        ManualOperationWrapper wrapper = newOperationWrapper("А",
                "MOS", bsaDt, "RUR", new BigDecimal("12.056"),
                "MOS", bsaCt, "RUR", new BigDecimal("12.056")
        );

        final String src = "K+TP";
        final String dealId = "DD_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 4);
        final String subDealId = "SD_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 4);
        wrapper.setDealSrc(src);
        wrapper.setSubdealId(subDealId);
        wrapper.setDealId(dealId);
        wrapper.setPostDateStr(Utl4Tests.toString(getOperday().getLastWorkingDay(), wrapper.dateFormat));
        wrapper.setValueDateStr(wrapper.getPostDateStr());
        wrapper.setInputMethod(InputMethod.M);

        BatchPosting posting = createAuthorizedPosting(wrapper, USER_ID);
        GLManualOperation res = remoteAccess.invoke(ManualOperationController.class, "processPosting", posting, false);
//        Assert.assertFalse(res.isError());
//        wrapper = res.getResult();
        Assert.assertTrue(0 < res.getId());

        GLManualOperation operation = (GLManualOperation) baseEntityRepository.findById(GLManualOperation.class, res.getId());
        Assert.assertEquals(OperState.POST, operation.getState());

        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);                 // 1 проводка
        Assert.assertEquals(postList.size(), 1);

        List<Pd> pdList = getPostingPd(postList.get(0));
        Pd pdDr = pdList.get(0);
        Pd pdCr = pdList.get(1);
        Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит
        Assert.assertTrue(pdCr.getCcy().equals(pdDr.getCcy()));                 // валюта одинаковый

        Assert.assertTrue(pdCr.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr.getAmount() == -pdDr.getAmount());       // сумма в валюте дебет - кредит

        BackvalueJournal journalDt = (BackvalueJournal) baseEntityRepository.findById(BackvalueJournal.class,
                new BackvalueJournalId(pdDr.getAcid(), pdDr.getBsaAcid(), pdDr.getPod()));
        Assert.assertNotNull(journalDt);
        // может быть ошибка при запуске локализации из-за запуска 5-й java на as400 v5
//        Assert.assertTrue(EnumUtils.contains(new BackvalueJournal.BackvalueJournalState[]{PROCESSED, ERROR_LC}, journalDt.getState()));

        BackvalueJournal journalCt = (BackvalueJournal) baseEntityRepository.findById(BackvalueJournal.class,
                new BackvalueJournalId(pdCr.getAcid(), pdCr.getBsaAcid(), pdCr.getPod()));
        Assert.assertNotNull(journalCt);
//        Assert.assertTrue(EnumUtils.contains(new BackvalueJournal.BackvalueJournalState[]{PROCESSED, ERROR_LC}, journalCt.getState()));
    }

    private void cleanBackvaluejournal(String bsaAcidDt, String bsaAcidCt) {
        baseEntityRepository.executeUpdate("delete from BackvalueJournal j where j.id.bsaAcid = ?1", bsaAcidCt);
        baseEntityRepository.executeUpdate("delete from BackvalueJournal j where j.id.bsaAcid = ?1", bsaAcidDt);

        // удалить бэквалуе журнал
        BackvalueJournal journalDt = (BackvalueJournal) baseEntityRepository.selectFirst(BackvalueJournal.class,
                "from BackvalueJournal j where j.id.bsaAcid = ?1", bsaAcidDt);
        Assert.assertNull(journalDt);

        BackvalueJournal journalCt = (BackvalueJournal) baseEntityRepository.selectFirst(BackvalueJournal.class,
                "from BackvalueJournal j where j.id.bsaAcid = ?1", bsaAcidCt);
        Assert.assertNull(journalCt);
    }

    /**
     * Проверка ошибки ввода валюты при создании операции по ручному вводу
     */
    @Test
    public void testErrorCurrency() {
        ManualOperationWrapper wrapper = newOperationWrapper(
                "40817036050010000056", "40817036050010000111",
                "LOL", new BigDecimal("12.056"));
        wrapper.setAction(BatchPostAction.SAVE);
        wrapper.setUserId(USER_ID);

        RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(ManualPostingController.class, "processOperationRq", wrapper);
        Assert.assertTrue(res.isError());
        Assert.assertTrue(!isEmpty(res.getMessage()));
        System.out.println(res.getMessage());
        Assert.assertNull(res.getResult().getId());

    }

    /**
     * Проверка ошибки отсутствия номера сделки при создании операции по ручному вводу
     */
    @Test
    public void testErrorDealId() {
        ManualOperationWrapper wrapper = newOperationWrapper(
                "40817036050010000056", "40817036050010000111",
                "AUD", new BigDecimal("12.056"));
        wrapper.setDealId(null);
        wrapper.setAction(BatchPostAction.SAVE);
        wrapper.setUserId(USER_ID);

        RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(ManualPostingController.class, "processOperationRq", wrapper);
        Assert.assertTrue(res.isError());
        Assert.assertTrue(!isEmpty(res.getMessage()));
        System.out.println(res.getMessage());
        Assert.assertNull(res.getResult().getId());

    }

    /**
     * Проверка ошибки отсутствия даты валютирования при создании операции по ручному вводу
     */
    @Test
    public void testErrorDate() {
        ManualOperationWrapper wrapper = newOperationWrapper(
                "40817036050010000056", "40817036050010000111",
                "AUD", new BigDecimal("12.056"));
        wrapper.setValueDateStr(null);
        wrapper.setInputMethod(InputMethod.M);
        wrapper.setAction(BatchPostAction.SAVE);
        wrapper.setUserId(USER_ID);

        RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(ManualPostingController.class, "processOperationRq", wrapper);
        Assert.assertTrue(res.isError());
        Assert.assertTrue(!isEmpty(res.getMessage()));
        System.out.println(res.getMessage());
        Assert.assertNull(res.getResult().getId());

    }

    /**
     * Проверка ошибок валидации при создании операции по ручному вводу
     */
    @Test
    public void testErrorValidation() {
        ManualOperationWrapper wrapper = newOperationWrapper("А",
                "MOS", "40817036050010000056", "USD", new BigDecimal("12.056"),
                "MOS", "40817036050010000112", "AUR", new BigDecimal("12")
                );

        wrapper.setAction(BatchPostAction.SAVE);
        wrapper.setUserId(USER_ID);

        RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(ManualPostingController.class, "processOperationRq", wrapper);
        Assert.assertTrue(res.isError());
        Assert.assertTrue(!isEmpty(res.getMessage()));
        System.out.println(res.getMessage());
        Assert.assertNull(res.getResult().getId());

    }

    /**
     * Проверка ошибкк главы баланса при создании межфилиальной операции по ручному вводу
     */
    @Test
    public void testErrorChapter() {
        ManualOperationWrapper wrapper = newOperationWrapper("А",
                "CHL", "91414810000161395277", "RUR", new BigDecimal("13.056"),
                "EKB", "99999810400400000001", "RUR", new BigDecimal("13.056")
        );

        wrapper.setAction(BatchPostAction.SAVE);
        wrapper.setUserId(USER_ID);

        RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(ManualPostingController.class, "processOperationRq", wrapper);
        Assert.assertTrue(res.isError());
        Assert.assertTrue(!isEmpty(res.getMessage()));
        System.out.println(res.getMessage());
        Assert.assertNull(res.getResult().getId());
    }

    /**
     * Проверка создания операции в дату раньше, чем дата открытия счета
     */
    @Test
    public void testDateAccountError() {
        String ccy = "USD";
        ManualAccountWrapper accountWrapperDb = ManualAccountIT.createManualAccount(
                "001", ccy, "00100081", 370010202, getOperday().getCurrentDate(), "", "00");
        GLAccount accountDb = (GLAccount) baseEntityRepository.findById(GLAccount.class, accountWrapperDb.getId());
        Assert.assertNotNull(accountDb);

        ManualAccountWrapper accountWrapperCr = ManualAccountIT.createManualAccount(
                "001", ccy, "00100347", 181030102, getOperday().getCurrentDate(), "", "00");
        GLAccount accountCr = (GLAccount) baseEntityRepository.findById(GLAccount.class, accountWrapperCr.getId());
        Assert.assertNotNull(accountCr);

        ManualOperationWrapper wrapper = newOperationWrapper("А",
                "MOS", accountDb.getBsaAcid(), ccy, new BigDecimal("4321.88"),
                "MOS", accountCr.getBsaAcid(), ccy, new BigDecimal("4321.88")
        );
        wrapper.setValueDateStr(new SimpleDateFormat(wrapper.dateFormat).format(getOperday().getLastWorkingDay()));
        wrapper.setAction(BatchPostAction.SAVE);
        wrapper.setUserId(USER_ID);

        RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(ManualPostingController.class, "processOperationRq", wrapper);
        Assert.assertTrue(res.isError());
        Assert.assertTrue(!isEmpty(res.getMessage()));
        System.out.println(res.getMessage());
        Assert.assertNull(res.getResult().getId());

    }


    public static BatchPosting createAuthorizedPosting(ManualOperationWrapper wrapper, Long userId) throws SQLException {
        return createAuthorizedPosting(wrapper, userId, BatchPostStatus.SIGNED);
    }

    public static BatchPosting createAuthorizedPosting(ManualOperationWrapper wrapper, Long userId, BatchPostStatus status) throws SQLException {
        wrapper.setAction(BatchPostAction.SAVE);
        // создать запрос
        BatchPostStatus inputStatus = BatchPostStatus.CONTROL;
        RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(ManualPostingController.class, "saveOperationRqInternal", wrapper, inputStatus);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        wrapper = res.getResult();
        Assert.assertTrue(0 < wrapper.getId());

        if (status != inputStatus) {
            baseEntityRepository.executeNativeUpdate("update GL_BATPST set STATE = ?, " +
                    "USER_NAME = (select USER_NAME from GL_USER where ID_USER = ?) where ID = ?", status.name(), userId, wrapper.getId());
        }
        BatchPosting posting = (BatchPosting)baseEntityRepository.findById(BatchPosting.class, wrapper.getId());
        baseEntityRepository.refresh(posting, true);
        return posting;

    }
}
