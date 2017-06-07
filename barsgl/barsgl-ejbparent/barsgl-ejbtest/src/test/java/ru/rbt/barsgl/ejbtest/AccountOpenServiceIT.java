package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.AccountOpenServiceTask;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.acc.GLAccountRequest;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountController;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountService;
import ru.rbt.barsgl.ejbcore.AccountQueryRepository;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Logger;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.substring;
import static ru.rbt.barsgl.ejbtest.AccountOpenAePostingsIT.checkDefinedAccount;

/**
 * Created by ER18837 on 19.10.15.
 */
public class AccountOpenServiceIT extends AbstractRemoteIT {

        private static final Logger logger = Logger.getLogger(AccountOpenServiceIT.class.getName());

        @Before
        public void beforeClass() {
            updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        }

    /**
     * Создание нового счета
     * @throws SQLException
     */
    @Test
    public void testCreateServiceAccount() throws SQLException {
//        GLAccountRequest request = createAccountRequest("A01", "RUR", "00118067", "161020100", "6", "9");
        GLAccountRequest request = createAccountRequest("A01", "RUR", "00650143", "161020100", "6", "9");
        GLAccount account = createServiceAccount(request);
        account = (GLAccount) baseEntityRepository.findById(GLAccount.class, account.getId());
        Assert.assertNotNull(account);
        Date dateOpen = null != request.getDateOpen() ? request.getDateOpen() : getOperday().getCurrentDate();
        AccountKeys keys = remoteAccess.invoke(GLAccountController.class, "createRequestAccountKeys", request, dateOpen);
        request = (GLAccountRequest) baseEntityRepository.findById(GLAccountRequest.class, request.getId());
        Assert.assertEquals(GLAccountRequest.RequestStatus.OK, request.getStatus());
        checkGlAccount(account, keys.toString());
        String error = checkResponce(request, account.getBsaAcid(), "Y");
        Assert.assertNull(error);
        checkDefinedAccount(GLOperation.OperSide.N, account.getBsaAcid(), keys.toString());
    }

    /**
     * Создание счета и повторный запрос на создание такого счета (тип клиента и код срока: 00 == null)
     * @throws SQLException
     */
    @Test
    public void testFindServiceAccount() throws SQLException {
        GLAccountRequest request = createAccountRequest("A02", "RUR", "00650143", "912020201", "8", "6", null);
        GLAccount account = createServiceAccount(request);
        account = (GLAccount) baseEntityRepository.findById(GLAccount.class, account.getId());
        Assert.assertNotNull(account);
        Date dateOpen = null != request.getDateOpen() ? request.getDateOpen() : getOperday().getCurrentDate();
        AccountKeys keys = remoteAccess.invoke(GLAccountController.class, "createRequestAccountKeys", request, dateOpen);
        request = (GLAccountRequest) baseEntityRepository.findById(GLAccountRequest.class, request.getId());
        Assert.assertEquals(GLAccountRequest.RequestStatus.OK, request.getStatus());
        checkGlAccount(account, keys.toString());
        String error = checkResponce(request, account.getBsaAcid(), "Y");
        Assert.assertNull(error);
        checkDefinedAccount(GLOperation.OperSide.N, account.getBsaAcid(), keys.toString());

        GLAccountRequest request2 = createAccountRequest("A02", "RUR", "00650143", "912020201", "8", "6",
                request.getDealSource(), request.getDealId(), request.getSubDealId(), null);
        GLAccount account2 = createServiceAccount(request2);
        account2 = (GLAccount) baseEntityRepository.findById(GLAccount.class, account2.getId());
        Assert.assertNotNull(account2);
        Assert.assertEquals(account.getBsaAcid(), account2.getBsaAcid());
        checkResponce(request2, account2.getBsaAcid(), "N");
    }

    /**
     * Тест создания счета из ручного ввода и поиска его по ключам (тип клиента и код срока: 00 == null)
     * @throws SQLException
     */
    @Test
    public void testFindServiceAccountWith00() throws SQLException {

/*
        DataRecord data = baseEntityRepository.selectFirst("select BBCUST from SDCUSTPD" +
                " where BXCTYP < 3 and not coalesce(BBCNA1, ' ') = ' ' and not coalesce(BXRUNM, ' ') = ' '");
        String custNo = data.getString(0);
        data = baseEntityRepository.selectFirst("select p.ACCTYPE from GL_ACTPARM p join GL_ACTNAME n on n.ACCTYPE = p.ACCTYPE" +
                " where CUSTYPE = '00' and TERM = '00' and TECH_ACT <> 'Y' and DTB <= ? and DTE is null", getOperday().getCurrentDate());
        String accType = data.getString(0);
*/

        String custNo = getCustomerNumber();
        String accType = getAccountType("00", "00");
        String dealId = substring("" + System.currentTimeMillis(), 1, 10);
        String src = "AXAPTA";

        GLAccountRequest request = createAccountRequest("A02", "RUR", custNo, accType, null, "00", src, dealId, null, null);
        GLAccount account = createServiceAccount(request);

        account = (GLAccount) baseEntityRepository.findById(GLAccount.class, account.getId());
        Assert.assertNotNull(account);
        Assert.assertNotNull(account);
        Assert.assertNull(account.getSubDealId());
        Assert.assertTrue(0 == account.getTerm());
        Assert.assertNull(account.getCbCustomerType());

        Date dateOpen = null != request.getDateOpen() ? request.getDateOpen() : getOperday().getCurrentDate();
        AccountKeys keys = remoteAccess.invoke(GLAccountController.class, "createRequestAccountKeys", request, dateOpen);
        request = (GLAccountRequest) baseEntityRepository.findById(GLAccountRequest.class, request.getId());
        Assert.assertEquals(GLAccountRequest.RequestStatus.OK, request.getStatus());
        checkGlAccount(account, keys.toString());
        String error = checkResponce(request, account.getBsaAcid(), "Y");
        Assert.assertNull(error);
        checkDefinedAccount(GLOperation.OperSide.N, account.getBsaAcid(), keys.toString());

        GLAccountRequest request1 = createAccountRequest("A02", "RUR", custNo, accType, "00", null, src, dealId, null, null);
        GLAccount account1 = createServiceAccount(request1);
        Assert.assertEquals(account, account1);

        request1 = (GLAccountRequest) baseEntityRepository.findById(GLAccountRequest.class, request1.getId());
        Assert.assertEquals(GLAccountRequest.RequestStatus.OK, request1.getStatus());
        error = checkResponce(request1, account.getBsaAcid(), "N");
        Assert.assertNull(error);
    }

    /**
     * Создание нового контролируемого счета
     * @throws SQLException
     */
    @Test
    public void testCreateServiceAccountCtrl() throws SQLException {
        // P01	RUR	00642182	441010100	18	00	FCC12	00642182RURCOSA101	null
        GLAccountRequest request = createAccountRequest("P01", "RUR", "00642182", "441010100", "18", "00");
        GLAccount account = createServiceAccount(request);
        account = (GLAccount) baseEntityRepository.findById(GLAccount.class, account.getId());
        Assert.assertNotNull(account);
        Date dateOpen = null != request.getDateOpen() ? request.getDateOpen() : getOperday().getCurrentDate();
        AccountKeys keys = remoteAccess.invoke(GLAccountController.class, "createRequestAccountKeys", request, dateOpen);
        request = (GLAccountRequest) baseEntityRepository.findById(GLAccountRequest.class, request.getId());
        Assert.assertEquals(GLAccountRequest.RequestStatus.OK, request.getStatus());
        checkGlAccount(account, keys.toString());
        String error = checkResponce(request, account.getBsaAcid(), "Y");
        Assert.assertNull(error);
        checkDefinedAccount(GLOperation.OperSide.N, account.getBsaAcid(), keys.toString());
    }

    /**
     * Проверка ошибки "Неверный бранч FLEX"
     * @throws SQLException
     */
    @Test
    public void testCreateServiceBranchError() throws SQLException {
//        GLAccountRequest request = createAccountRequest("A00", "RUR", "00650143", "161010100", "9", "11");
        GLAccountRequest request = createAccountRequest("A00", "RUR", "00650143", "161010100", "8", "09");
        GLAccount account = createServiceAccount(request);
        Assert.assertNull(account);
        request = (GLAccountRequest) baseEntityRepository.findById(GLAccountRequest.class, request.getId());
        Assert.assertEquals(GLAccountRequest.RequestStatus.OK, request.getStatus());
        String error = checkResponce(request, null, "N");
        Assert.assertEquals("2022", error);
    }

    /**
     * Проверка ошибки "Неверный номер сделки" "Неверный номер субсделки"
     * @throws SQLException
     */
    @Test
    public void testCreateServiceDealError() throws SQLException {
        GLAccountRequest request = createAccountRequest("A01", "RUR", "00118067", "161020100", "6", "9");
        request.setDealId("1234567890123456789012");
        request.setSubDealId("1234567890123456789012");
        GLAccount account = createServiceAccount(request);
        Assert.assertNull(account);
        request = (GLAccountRequest) baseEntityRepository.findById(GLAccountRequest.class, request.getId());
        Assert.assertEquals(GLAccountRequest.RequestStatus.OK, request.getStatus());
        String error = checkResponce(request, null, "N");
        Assert.assertEquals("2", error);
    }

    /**
     * Проверка ошибки "Неверный номер клиента"
     * @throws SQLException
     */
    @Test
    public void testCreateServiceCustNoError() throws SQLException {
        GLAccountRequest request = createAccountRequest("A02", "RUR", "00000020", "161010100", "9", "11");
        GLAccount account = createServiceAccount(request);
        Assert.assertNull(account);
        request = (GLAccountRequest) baseEntityRepository.findById(GLAccountRequest.class, request.getId());
        Assert.assertEquals(GLAccountRequest.RequestStatus.OK, request.getStatus());
        String error = checkResponce(request, null, "N");
        Assert.assertEquals("2005", error);
    }

    /**
     * Проверка ошибки "Не найдены ключи счета"
     * @throws SQLException
     */
    @Test
    public void testCreateServiceAccountTypeError() throws SQLException {
        GLAccountRequest request = createAccountRequest("A02", "RUR", "00650143", "161010100", "9", "11");
        GLAccount account = createServiceAccount(request);
        Assert.assertNull(account);
        request = (GLAccountRequest) baseEntityRepository.findById(GLAccountRequest.class, request.getId());
        Assert.assertEquals(GLAccountRequest.RequestStatus.OK, request.getStatus());
        String error = checkResponce(request, null, "N");
        Assert.assertEquals("2011", error);
    }

    /**
     * Проверка ошибки "Неверная валюта"
     * @throws SQLException
     */
    @Test
    public void testCreateServiceAccountCurrencyError() throws SQLException {
        GLAccountRequest request = createAccountRequest("A02", "LOL", "00650143",  "161020100", "6", "9");
        GLAccount account = createServiceAccount(request);
        Assert.assertNull(account);
        request = (GLAccountRequest) baseEntityRepository.findById(GLAccountRequest.class, request.getId());
        Assert.assertEquals(GLAccountRequest.RequestStatus.OK, request.getStatus());
        String error = checkResponce(request, null, "N");
        Assert.assertEquals("1002", error);
    }

    @Test
    public void testAccountOpenServiceTask() throws SQLException {
        baseEntityRepository.executeNativeUpdate("delete from GL_ACOPENRQ where STATUS = 'NEW'");
        GLAccountRequest[] requests = new GLAccountRequest[12];
        requests[0] = createAccountRequest("A01", "RUR", "00118067", "161020100", "6", "9");
        requests[1] = createAccountRequest("A01", "USD", "00650143", "161020100", "6", "9");
        requests[2] = createAccountRequest("A02", "RUR", "00650143", "161010100", "9", "11");
        requests[3] = createAccountRequest("A02", "RUR", "00000020", "161010100", "9", "11");
        requests[4] = createAccountRequest("A02", "RUR", "00650143", "161010100", "9", "11");
        requests[5] = createAccountRequest("A00", "RUR", "00118067", "161020100", "6", "9");
        requests[6] = createAccountRequest("A01", "RUR", "00118067", "161020100", "6", "9",
                requests[0].getDealSource(), requests[0].getDealId(), requests[0].getSubDealId(), null);
        remoteAccess.invoke(AccountOpenServiceTask.class, "executeWork");
        for (GLAccountRequest request: requests) {
            if (null == request)
                break;
            request = (GLAccountRequest) baseEntityRepository.findById(GLAccountRequest.class, request.getId());
            Assert.assertEquals(GLAccountRequest.RequestStatus.OK, request.getStatus());
            checkResponce(request);
        }
    }

    private GLAccount createServiceAccount(GLAccountRequest request) {
        return remoteAccess.invoke(GLAccountService.class, "createRequestAccount", request);
    }

    private void checkGlAccount(GLAccount account, String keyString){
        AccountKeys keys = new AccountKeys(keyString);
        Assert.assertNotNull(account);
        Assert.assertEquals(account.getBranch(), keys.getBranch());
        Assert.assertEquals(account.getCurrency().getCurrencyCode(), keys.getCurrency());
        Assert.assertEquals(account.getCustomerNumber(), keys.getCustomerNumber());

        Assert.assertEquals(Long.toString(account.getAccountType()), keys.getAccountType());
        if (!isEmpty(keys.getCustomerType())) {
            Short custType = Short.parseShort(keys.getCustomerType());
            Assert.assertEquals(account.getCbCustomerType(), custType);
        }
        if (!isEmpty(keys.getTerm())) {
            Short term = Short.parseShort(keys.getTerm());
            Assert.assertEquals(account.getTerm(), term);
        }

        Assert.assertEquals(account.getDealSource(), keys.getDealSource());
        Assert.assertEquals(account.getDealId(), keys.getDealId());
        if (isEmpty(keys.getSubDealId()))
            Assert.assertNull(account.getSubDealId());
        else
            Assert.assertEquals(account.getSubDealId(), keys.getSubDealId());
    }

    private String checkResponce(GLAccountRequest request, String bsaAcid, String newAcc) throws SQLException {
        DataRecord responce = checkResponce(request);
        Assert.assertEquals(bsaAcid, responce.getString("CBACCOUNT_NO"));
        Assert.assertEquals(newAcc, responce.getString("NEWACC"));
        if (!isEmpty(bsaAcid)) {
            Date dateOpen = null != request.getDateOpen() ? request.getDateOpen() : getOperday().getCurrentDate();
            Assert.assertEquals(dateOpen, responce.getDate("OPEN_DATE"));
        }
        String errorMessage = responce.getString("ERRORDESCRIPTION");
        if (!isEmpty(errorMessage)) {
            System.out.println("Message: " + errorMessage);
        }
        return responce.getString("ERRORCODE");
    }

    private DataRecord checkResponce(GLAccountRequest request) throws SQLException {
        DataRecord event = baseEntityRepository.selectOne("select 1 from WBI_Events where OBJECT_KEY = ?", request.getId());

        DataRecord responce = baseEntityRepository.selectOne("select * from GL_ACOPENRS where REQUEST_ID = ?", request.getId());
        Assert.assertNotNull(responce);
        System.out.println(format("Request: '%s', BsaAcid: '%s', New: '%s', ErrorCode: '%s'",
                request.getId(), responce.getString("CBACCOUNT_NO"), responce.getString("NEWACC"), responce.getString("ERRORCODE")));
        return responce;
    }

    private GLAccountRequest createAccountRequest(String branch, String ccy, String customerNumber,
                                                  String accountType, String custType, String term ) {
        return createAccountRequest(branch, ccy, customerNumber, accountType, custType, term,
                getOperday().getCurrentDate());

    }

    private GLAccountRequest createAccountRequest(String branch, String ccy, String customerNumber,
                                                  String accountType, String custType, String term,
                                                  Date dateOpen) {
        long stamp = System.currentTimeMillis();
        return createAccountRequest(branch, ccy, customerNumber, accountType, custType, term,
                "K+TP", substring("" + stamp, 1, 10), substring("" + stamp, 6, 10),
                dateOpen);
    }

    private GLAccountRequest createAccountRequest(String branch, String ccy, String customerNumber,
                                                  String accountType, String custType, String term,
                                                  String dealSource, String dealId, String subdealId,
                                                  Date dateOpen) {
        GLAccountRequest request = new GLAccountRequest();
        long stamp = System.currentTimeMillis();

        request.setId("" + stamp);
        request.setBranchFlex(branch);
//        BankCurrency currency = remoteAccess.invoke(BankCurrencyRepository.class, "getCurrency", "RUR");
        request.setCurrency(ccy);
        request.setCustomerNumber(customerNumber);
        request.setAccountType(accountType);
        request.setCbCustomerType(custType);
        request.setTerm(term);
        request.setDealSource(dealSource);
        request.setDealId(dealId);
        request.setSubDealId(subdealId);
        request.setDateOpen(dateOpen);
        request.setStatus(GLAccountRequest.RequestStatus.NEW);
        request = (GLAccountRequest)baseEntityRepository.save(request);

//        baseEntityRepository.executeNativeUpdate("update GL_ACOPENRQ set CCY = ? where REQUEST_ID = ?", ccy, request.getId());
        return (GLAccountRequest) baseEntityRepository.findById(GLAccountRequest.class, request.getId());
    }

    /* Тест поиска бранча Флекс по Bsaacid, Acid
    * */
    @Test
    public void testtBranchByBsaacidorAcid() throws SQLException {
        // 00000661RUR100355066	20208810820660000002
        String acid = "00000661RUR100355066";
        String bsaacid = "20208810820660000002";
        String fccBranch = remoteAccess.invoke(AccountQueryRepository.class, "getBranchByBsaacidorAcid", bsaacid, acid,
                getOperday().getCurrentDate());
        System.out.printf("BsaAcid = '%s', Acid = '%s', fccBranch = '%s'\n", bsaacid, acid, fccBranch);
    }

}
