package ru.rbt.barsgl.ejbtest;

import org.junit.*;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.integr.bg.ManualOperationController;
import ru.rbt.barsgl.ejb.integr.bg.ManualPostingController;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.ejbtesting.test.ManualOperationAuthTesting;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.BatchPostAction;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.enums.InvisibleType;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.StringUtils;

import java.math.BigDecimal;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static ru.rbt.ejbcore.util.StringUtils.trim;

/**
 * Created by ER18837 on 01.06.16.
 */
public class ManualOperationAuthIT extends AbstractTimerJobIT {
    private final SimpleDateFormat onlyDate = new SimpleDateFormat("dd.MM.yyyy");
    private static final Long USER_ID = 2L;

    private static final String mcDebugId = "mc.debug";
    private static boolean mcDebug = false;

    @BeforeClass
    public static void beforeClass() {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        Utl4Tests.createUser(USER_ID, baseEntityRepository);
        Utl4Tests.grantAllBranches(USER_ID, baseEntityRepository);
        mcDebug = setMcDebug(true);
    }

    @AfterClass
    public static void afterClass() {
        setMcDebug(mcDebug);
    }

    public static boolean setMcDebug(boolean debug)  {
        DataRecord res = null;
        try {
            res = baseEntityRepository.selectFirst("select STRING_VALUE from GL_PRPRP where ID_PRP = ?", mcDebugId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        boolean debugWas = !(res == null) && "Y".equals(res.getString(0));
        if (debug == debugWas)
            return debugWas;

        // Insert into gl_prprp (ID_PRP,ID_PRN,REQUIRED,PRPTP,DESCRP,DECIMAL_VALUE,STRING_VALUE,NUMBER_VALUE) values ('mc.debug','root','N','STRING_TYPE','режим отладки SCASAMovementCreate',null,'Y',null);
        if (debug)
            baseEntityRepository.executeNativeUpdate("merge into gl_prprp p\n" +
                    "using (select 'mc.debug' id_prp from dual) p2\n" +
                    "on (p.id_prp = p2.id_prp)\n" +
                    "when not matched then insert (ID_PRP,ID_PRN,REQUIRED,PRPTP,DESCRP,STRING_VALUE)\n" +
                    "values (?,'root','N','STRING_TYPE','режим отладки SCASAMovementCreate','Y')", mcDebugId);
        else
            baseEntityRepository.executeNativeUpdate("delete from GL_PRPRP where ID_PRP = ?", mcDebugId);
        return debugWas;
    }

    /**
     * Создание операции по ручному вводу и затем передача на подпись
     */
    @Test
    public void testSaveOperationRq() throws SQLException, ParseException {

        BigDecimal sum = new BigDecimal("99.056");
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20202810_0001%1");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40702810_0001%3");
        ManualOperationWrapper wrapper = newOperationWrapper("А",
                "MOS", bsaDt, "RUR", sum,
                "MOS", bsaCt, "RUR", sum
        );

        wrapper.setAction(BatchPostAction.SAVE);
        // создать запрос
        RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(ManualPostingController.class, "saveOperationRqInternal", wrapper, BatchPostStatus.INPUT);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        wrapper = res.getResult();
        Assert.assertTrue(0 < wrapper.getId());
        BatchPosting posting = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper.getId());
        Assert.assertEquals(BatchPostStatus.INPUT, posting.getStatus());
        checkOperationRq(wrapper, posting, true);

//        baseEntityRepository.executeNativeUpdate("update GL_BATPST set STATE = 'CONTROL' where ID = ?", wrapper.getId());
        // на авторизацию
        res = remoteAccess.invoke(ManualPostingController.class, "forSignOperationRqInternal", wrapper);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        wrapper = res.getResult();
        BatchPosting posting2 = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper.getId());
        Assert.assertEquals(BatchPostStatus.CONTROL, posting2.getStatus());
    }

    /**
     * Создание операции по ручному вводу и затем передача на подпись
     */
    @Test
    public void testSaveForSignOperationRq() throws SQLException, ParseException {
        BigDecimal sum = new BigDecimal("102.056");
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20202810_0001%1");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40702810_0001%3");
        ManualOperationWrapper wrapper = newOperationWrapper("А",
                "MOS", bsaDt, "RUR", sum,
                "MOS", bsaCt, "RUR", sum
        );

        wrapper.setAction(BatchPostAction.CONTROL);
        // создать запрос
        RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(ManualPostingController.class, "saveOperationRqInternal", wrapper, BatchPostStatus.CONTROL);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        wrapper = res.getResult();
        Assert.assertTrue(0 < wrapper.getId());
        BatchPosting posting = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper.getId());
        Assert.assertEquals(BatchPostStatus.CONTROL, posting.getStatus());
        checkOperationRq(wrapper, posting, true);
    }

    /**
     * Редактирование операции по ручному вводу на этапе ввода и авторизации (своего и чужого)
     */
    @Test
    public void testUpdateOperationRq() throws SQLException, ParseException {

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20202036_0001%2");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20202036_0001%4");
        ManualOperationWrapper wrapper = newOperationWrapper("А",
                "MOS", bsaDt, "AUD", new BigDecimal("102.056"),
                "MOS", bsaCt, "AUD", new BigDecimal("102.056")
        );

        wrapper.setAction(BatchPostAction.SAVE);
        // создать запрос
        RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(ManualPostingController.class, "saveOperationRqInternal", wrapper, BatchPostStatus.INPUT);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        ManualOperationWrapper wrapper1 = res.getResult();
        Assert.assertTrue(0 < wrapper1.getId());
        BatchPosting posting = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper1.getId());
        Assert.assertEquals(BatchPostStatus.INPUT, posting.getStatus());
        checkOperationRq(wrapper1, posting, true);

        // изменить параматры
        wrapper1.setDealSrc("FC12_CL");
        wrapper1.setDealId("DD_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 4));
        wrapper1.setSubdealId("SD_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 4));
        wrapper1.setProfitCenter("CNTR");
        wrapper1.setAmountDebit(new BigDecimal("132.056"));
        wrapper1.setAmountCredit(wrapper1.getAmountDebit());

        wrapper1.setAction(BatchPostAction.UPDATE);
        res = remoteAccess.invoke(ManualPostingController.class, "updateOperationRqInternal", wrapper1, BatchPostStatus.INPUT);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        posting = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper1.getId());
        Assert.assertEquals(wrapper1.getAmountDebit(), posting.getAmountDebit());
        Assert.assertEquals(wrapper1.getAmountCredit(), posting.getAmountCredit());

        ManualOperationWrapper wrapper2 = res.getResult();
        Assert.assertEquals(wrapper1.getId(), wrapper2.getId());
        posting = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper2.getId());
        Assert.assertEquals(BatchPostStatus.INPUT, posting.getStatus());
        checkOperationRq(wrapper2, posting, true);

        // на авторизацию
        wrapper.setAction(BatchPostAction.CONTROL);
        res = remoteAccess.invoke(ManualPostingController.class, "forSignOperationRqInternal", wrapper1);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        wrapper2 = res.getResult();
        posting = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper2.getId());
        Assert.assertEquals(BatchPostStatus.CONTROL, posting.getStatus());
        
        // изменить параматры снова
        wrapper2.setDealSrc("K+TP");
        wrapper2.setDealId("DD_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 4));
        wrapper2.setSubdealId("SD_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 4));
        wrapper2.setProfitCenter("PRFT");
        wrapper2.setAmountDebit(new BigDecimal("152.056"));
        wrapper2.setAmountCredit(wrapper2.getAmountDebit());

        // изменить пользователя
        baseEntityRepository.executeNativeUpdate("update GL_BATPST set USER_NAME = 'USER1' where ID = ?", wrapper2.getId());

        res = remoteAccess.invoke(ManualPostingController.class, "updateOperationRqInternal", wrapper2, BatchPostStatus.INPUT);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        ManualOperationWrapper wrapper3 = res.getResult();
        Assert.assertEquals(wrapper1.getId(), wrapper2.getId());
        posting = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper2.getId());
        Assert.assertEquals(BatchPostStatus.CONTROL, posting.getStatus());
        checkOperationRq(wrapper3, posting, false);
        Assert.assertEquals(wrapper2.getAmountDebit(), posting.getAmountDebit());
        Assert.assertEquals(wrapper2.getAmountCredit(), posting.getAmountCredit());
        Long idHist = posting.getHistoryPostingId();
        Assert.assertNotNull(idHist);

        BatchPosting postingHist = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, idHist);
        Assert.assertEquals(wrapper2.getId(), postingHist.getParentPostingId());
    }

    /**
     * Удаление запроса на операцию - своего и чужого
     * @throws SQLException
     */
    @Test
    public void testDeleteOperationRq() throws SQLException {

        BigDecimal sum = new BigDecimal("149.056");
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20202810_0001%4");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40702810_0001%6");
        ManualOperationWrapper wrapper = newOperationWrapper("А",
                "MOS", bsaDt, "RUR", sum,
                "MOS", bsaCt, "RUR", sum
        );

        wrapper.setAction(BatchPostAction.SAVE);
        // создать запрос
        RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(ManualPostingController.class, "saveOperationRqInternal", wrapper, BatchPostStatus.INPUT);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        wrapper = res.getResult();
        Assert.assertTrue(0 < wrapper.getId());

        // удалить свой запрос
        wrapper.setUserId(USER_ID);
        wrapper.setAction(BatchPostAction.DELETE);
        res = remoteAccess.invoke(ManualPostingController.class, "deleteOperationRq", wrapper);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        BatchPosting posting = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper.getId());
        Assert.assertNull(posting);

        // снова создать запрос
        wrapper.setAction(BatchPostAction.SAVE);
        res = remoteAccess.invoke(ManualPostingController.class, "saveOperationRqInternal", wrapper, BatchPostStatus.INPUT);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        wrapper = res.getResult();
        Assert.assertTrue(0 < wrapper.getId());

        // изменить пользователя
        baseEntityRepository.executeNativeUpdate("update GL_BATPST set USER_NAME = 'USER1' where ID = ?", wrapper.getId());
//        baseEntityRepository.executeNativeUpdate("update GL_BATPST set STATE = 'CONTROL' where ID = ?", wrapper.getId());

        // удалить чужой запрос
        wrapper.setAction(BatchPostAction.DELETE);
        res = remoteAccess.invoke(ManualOperationAuthTesting.class, "deleteOperationRq", wrapper);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        posting = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper.getId());
        Assert.assertNotNull(posting);  // история
        Assert.assertEquals(InvisibleType.U, posting.getInvisible());
        Assert.assertNotNull(posting.getChangeTimestamp());

    }

    /**
     * Авторизация и обработка запроса на операцию
     * @throws SQLException
     */
    @Test
    public void testAuthorizeOperationRq() throws SQLException {

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20202810_0001%4");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40702810_0001%6");
        ManualOperationWrapper wrapper = newOperationWrapper("А",
                "MOS", bsaDt, "RUR", new BigDecimal("152.057"),
                "MOS", bsaCt, "RUR", new BigDecimal("152.057")
        );

        wrapper.setUserId(USER_ID);
        wrapper.setAction(BatchPostAction.SAVE_CONTROL);
        // создать запрос
        RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(ManualPostingController.class, "saveOperationRqInternal", wrapper, BatchPostStatus.CONTROL);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        wrapper = res.getResult();
        Assert.assertTrue(0 < wrapper.getId());

        wrapper.setAction(BatchPostAction.SIGN);
//        wrapper.setAccountDebit("408170/36050010000015");
        // обработать свой запрос
        baseEntityRepository.executeNativeUpdate("update GL_BATPST set USER_NAME = 'TEST' where ID = ?", wrapper.getId());
        Exception ex = null;
        try {
            res = remoteAccess.invoke(ManualOperationController.class, "authorizeOperationRq", wrapper);
        } catch (Exception e) {
            ex = e;
            res = remoteAccess.invoke(ManualOperationAuthTesting.class, "authorizeOperationRq", wrapper);
        }
        Assert.assertNotNull(ex);

        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        BatchPosting posting = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper.getId());
        Assert.assertNotNull(posting);  // запрос
        Assert.assertEquals(BatchPostStatus.WAITSRV, posting.getStatus());
        Assert.assertNotNull(posting.getSignerTamestamp());
    }

    /**
     * Отказ в авторизации запроса на операцию
     * @throws SQLException
     */
    @Test
    public void testRefuseOperationRq() throws SQLException {

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20202810_0001%5");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40702810_0001%7");
        ManualOperationWrapper wrapper = newOperationWrapper("А",
                "MOS", bsaDt, "RUR", new BigDecimal("162.057"),
                "MOS", bsaCt, "RUR", new BigDecimal("162.057")
        );

        wrapper.setUserId(USER_ID);
        wrapper.setAction(BatchPostAction.SAVE);
        // создать запрос
        RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(ManualPostingController.class, "saveOperationRqInternal", wrapper, BatchPostStatus.CONTROL);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        wrapper = res.getResult();
        Assert.assertTrue(0 < wrapper.getId());

        wrapper.setAction(BatchPostAction.REFUSE);
        wrapper.setReasonOfDeny("Не подписывать!");
        // обработать свой запрос
        res = remoteAccess.invoke(ManualOperationAuthTesting.class, "refuseOperationRq", wrapper, BatchPostStatus.REFUSE);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        BatchPosting posting = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper.getId());
        Assert.assertNotNull(posting);  // запрос
        Assert.assertEquals(BatchPostStatus.REFUSE, posting.getStatus());
        Assert.assertNotNull(posting.getReasonOfDeny());
    }

    /**
     * Отказ в авторизации запроса на операцию
     * @throws SQLException
     */
    @Test
    public void testMovementProcessor() throws SQLException {

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20202810_0001%5");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40702810_0001%7");
        ManualOperationWrapper wrapper = newOperationWrapper("А",
                "MOS", bsaDt, "RUR", new BigDecimal("25015.57"),
                "MOS", bsaCt, "RUR", new BigDecimal("25015.57")
        );

        wrapper.setUserId(USER_ID);
        wrapper.setInputMethod(InputMethod.M);
        wrapper.setAction(BatchPostAction.SAVE);
        wrapper.setDealId("ERROR");

        // создать запрос
        RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(ManualPostingController.class, "saveOperationRqInternal", wrapper, BatchPostStatus.CONTROL);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        wrapper = res.getResult();
        Assert.assertTrue(0 < wrapper.getId());
        BatchPosting posting0 = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper.getId());

        wrapper.setAction(BatchPostAction.SIGN);

        // обработать свой запрос
        baseEntityRepository.executeNativeUpdate("update GL_BATPST set USER_NAME = 'TEST' where ID = ?", wrapper.getId());
        res = remoteAccess.invoke(ManualOperationAuthTesting.class, "authorizeOperationRq", wrapper);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        BatchPosting posting = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper.getId());
        Assert.assertNotNull(posting);  // запрос
        Assert.assertNotNull(posting.getMovementId());
        Assert.assertNotNull(posting.getSendTimestamp());

//        Assert.assertEquals(BatchPostStatus.ERRSRV, posting.getStatus());
//        Assert.assertNotNull(posting.getErrorMessage());
    }

    private void checkOperationRq(ManualOperationWrapper wrapper, BatchPosting posting, boolean all) throws ParseException {

        Assert.assertEquals(wrapper.getDealSrc(), posting.getSourcePosting());
        Assert.assertEquals(trim(wrapper.getDealId()), trim(posting.getDealId()));
        Assert.assertEquals(wrapper.getSubdealId(), posting.getSubDealId());
        Assert.assertEquals(trim(wrapper.getDeptId()), trim(posting.getDeptId()));
        Assert.assertEquals(trim(wrapper.getProfitCenter()), trim(posting.getProfitCenter()));
        Assert.assertEquals(wrapper.getInputMethod(), posting.getInputMethod());

        Assert.assertEquals(onlyDate.parse(wrapper.getValueDateStr()), posting.getValueDate());

        if (all) {
            Assert.assertEquals(onlyDate.parse(wrapper.getPostDateStr()), posting.getPostDate());

            Assert.assertEquals(wrapper.getAccountDebit(), posting.getAccountDebit());
            Assert.assertEquals(wrapper.getAmountDebit(), posting.getAmountDebit());
            Assert.assertEquals(wrapper.getCurrencyDebit(), posting.getCurrencyDebit().getCurrencyCode());
            Assert.assertEquals(wrapper.getAmountDebit(), posting.getAmountDebit());

            Assert.assertEquals(wrapper.getAccountCredit(), posting.getAccountCredit());
            Assert.assertEquals(wrapper.getAmountCredit(), posting.getAmountCredit());
            Assert.assertEquals(wrapper.getCurrencyCredit(), posting.getCurrencyCredit().getCurrencyCode());
            Assert.assertEquals(wrapper.getAmountCredit(), posting.getAmountCredit());
            Assert.assertEquals(wrapper.getAmountRu(), posting.getAmountRu());
        }

        // TODO все остальные поля 
    }
}
