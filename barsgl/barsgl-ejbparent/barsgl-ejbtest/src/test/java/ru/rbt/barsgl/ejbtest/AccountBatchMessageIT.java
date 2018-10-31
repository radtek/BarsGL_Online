package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.excel.AccountBatchProcessorBean;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.ejb.integr.acc.AccountBatchController;
import ru.rbt.barsgl.ejbtesting.test.AccountBatchTesting;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.BatchPostStep;
import ru.rbt.barsgl.shared.operation.AccountBatchWrapper;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.rbt.barsgl.ejbtest.BackValueAuthIT.addUserRole;
import static ru.rbt.barsgl.ejbtest.BackValueAuthIT.subUserRole;
import static ru.rbt.barsgl.shared.enums.AccountBatchPackageState.IS_LOAD;
import static ru.rbt.barsgl.shared.enums.AccountBatchPackageState.ON_VALID;

/**
 * Created by er18837 on 30.10.2018.
 */
public class AccountBatchMessageIT  extends AbstractTimerJobIT {

    public static final String exampleAccBatchName = "example_acc_10.xlsx";

    private static final Long USER_ID = 1L;
    private static final Long USER_ID2 = 2L;
    private static final Long USER_ID3 = 3L;
    private static final Long ROLE_ADM = 1L;
    private static final Long ROLE_AB = 27L;

    @BeforeClass
    public static void beforeAll() throws SQLException {
        addUserRole(USER_ID, ROLE_ADM);
        addUserRole(USER_ID2, ROLE_AB);
        subUserRole(USER_ID3, ROLE_AB);
    }

        /**
         * Загрузка сообщений из файла Excel
         * @throws Exception
         */
    @Test
    public void testLoadPackage() throws Exception {
        BatchMessageIT.PackageParam param = loadPackage(USER_ID);
        System.out.println(param);
        AccountBatchPackage batchPackage = getPackage(param.getId());
        Assert.assertNotNull(batchPackage);
        Assert.assertEquals(IS_LOAD, batchPackage.getState());
        Assert.assertNotNull(batchPackage.getOperday());
        Assert.assertNotNull(batchPackage.getLoadUser());
        Assert.assertNotNull(batchPackage.getCntRequests());
    }

    @Test
    public void testDeletePackageOwn() {
        BatchMessageIT.PackageParam param = loadPackage(USER_ID);
        System.out.println(param);

        // авторизовать
        AccountBatchWrapper wrapper = new AccountBatchWrapper();
        wrapper.setPackageId(param.getId());
        wrapper.setAction(AccountBatchWrapper.AccountBatchAction.DELETE);

        RpcRes_Base<AccountBatchWrapper> res = null;
        wrapper.setUserId(USER_ID3);
        res = remoteAccess.invoke(AccountBatchTesting.class, "processAccountBatchRq", wrapper);
        System.out.println(res.getMessage());
        Assert.assertTrue(res.isError());

        wrapper.setUserId(USER_ID);
        res = remoteAccess.invoke(AccountBatchTesting.class, "processAccountBatchRq", wrapper);
        System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());

        AccountBatchPackage batchPackage = getPackage(param.getId());
        Assert.assertNull(batchPackage);
    }

    @Test
    public void testDeletePackageOther() {
        BatchMessageIT.PackageParam param = loadPackage(USER_ID2);
        System.out.println(param);

        // авторизовать
        AccountBatchWrapper wrapper = new AccountBatchWrapper();
        wrapper.setPackageId(param.getId());
        wrapper.setAction(AccountBatchWrapper.AccountBatchAction.DELETE);

        wrapper.setUserId(USER_ID);
        RpcRes_Base<AccountBatchWrapper> res = remoteAccess.invoke(AccountBatchTesting.class, "processAccountBatchRq", wrapper);
        System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());

        AccountBatchPackage batchPackage = getPackage(param.getId());
        Assert.assertNotNull(batchPackage);
        Assert.assertEquals("Y", batchPackage.getInvisible().name());
        Assert.assertNull(batchPackage.getValidateStartDate());
        Assert.assertNotNull(batchPackage.getProcUser());
    }

    @Test
    public void testOnvalidPackage() {
        BatchMessageIT.PackageParam param = loadPackage(USER_ID);
        System.out.println(param);

        // авторизовать
        AccountBatchWrapper wrapper = new AccountBatchWrapper();
        wrapper.setPackageId(param.getId());
        wrapper.setAction(AccountBatchWrapper.AccountBatchAction.OPEN);

        wrapper.setUserId(USER_ID3);
        RpcRes_Base<AccountBatchWrapper> res = remoteAccess.invoke(AccountBatchTesting.class, "processAccountBatchRq", wrapper);
        System.out.println(res.getMessage());
        Assert.assertTrue(res.isError());

        wrapper.setUserId(USER_ID);
        res = remoteAccess.invoke(AccountBatchTesting.class, "processAccountBatchRq", wrapper);
        System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());

        AccountBatchPackage batchPackage = getPackage(param.getId());
        Assert.assertNotNull(batchPackage);
        Assert.assertEquals(ON_VALID, batchPackage.getState());
        Assert.assertNotNull(batchPackage.getValidateStartDate());
        Assert.assertNotNull(batchPackage.getProcUser());
    }

    private BatchMessageIT.PackageParam getPackageParam(String result) {
        // "...#ID пакета: (\\d+)#Загружено строк всего: (\\d+)#..."
        Pattern pattern = Pattern.compile(".*#ID пакета: (\\d+)#Загружено строк всего: (\\d+)#.*");

        Matcher matcher = pattern.matcher(result);
        matcher.find();
        BatchMessageIT.PackageParam param = new BatchMessageIT.PackageParam(null, 0, 0);
        if (matcher.groupCount() == 2) {
            param.setId(Long.parseLong(matcher.group(1)));
            param.setRecordCount(Integer.parseInt(matcher.group(2)));
//            param.setErrorCount(Integer.parseInt(matcher.group(3)));
        }
        return param;
    }

    public BatchMessageIT.PackageParam loadPackage(Long userId) {
        return loadPackage(userId, exampleAccBatchName);
    }

    public BatchMessageIT.PackageParam loadPackage(Long userId, String pkgName) {
        String msg = "";
        File file = new File(BatchMessageIT.class.getClassLoader().getResource(pkgName).getFile());

        Map<String, String> params = new HashMap<>();
        params.put("filename", file.getAbsolutePath());
        params.put("userid", userId.toString());

        msg = remoteAccess.invoke(AccountBatchTesting.class, "loadPackage", file, params);

        BatchMessageIT.PackageParam param = getPackageParam(msg);
        Assert.assertNotNull(param.getId());
        Assert.assertEquals(10, param.getRecordCount());
        return param;
    }

    private AccountBatchPackage getPackage(Long idPackage) {
         return (AccountBatchPackage) baseEntityRepository.selectFirst(AccountBatchPackage.class, "from AccountBatchPackage p where p.id = ?1", idPackage);
    }
}
