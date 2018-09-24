package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.BankCalendarDay;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.controller.excel.BatchMessageProcessorBean;
import ru.rbt.barsgl.ejb.entity.etl.BatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.integr.bg.BatchPackageController;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.*;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.ejbcore.util.DateUtils;

import java.io.File;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import ru.rbt.barsgl.gwt.server.upload.ExcelParser;

/**
 * Created by ER18837 on 29.02.16.
 * Пакетная обработка проводок из файла Excel
 */
public class BatchMessageIT extends AbstractTimerJobIT {

    public static final String exampleBatchName = "example_batch.xlsx";
    public static final String exampleBatchMcName = "example_batch_mc.xlsx";
    public static final String exampleBatchDateStr = "2018-07-19"; // "2015-02-26";
//    private static final String ETL_SINGLE_ACTION_MONITOR = "ETL_SINGLE_ACTION_MONITOR";
    private final Long USER_ID = 1L;

    @BeforeClass
//    public static void beforeClass() {
//        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
//    }
    public static void beforeClass() {
        try {
            Date od = DateUtils.dbDateParse(exampleBatchDateStr);
            Date lw = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", od, 1, false);
            setOperday(od, lw, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
//            setOperday(DateUtils.dbDateParse("2015-02-26"), DateUtils.dbDateParse("2015-02-25"),
//                    Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Загрузка сообщений из файла Excel
     * @throws Exception
     */
    @Test
    public void testLoadPackage() throws Exception {
        PackageParam param = loadPackage(USER_ID);
        System.out.println(param);
    }

    /**
     * Загрузка сообщений из файла Excel и передача на подпись
     * @throws Exception
     */
    @Test
    public void testForSignPackage() throws Exception {
		// создать пакет
		PackageParam param = loadPackage(USER_ID);

		// передать на подпись
        ManualOperationWrapper wrapper = new ManualOperationWrapper();
        wrapper.setPkgId(param.getId());
		wrapper.setAction(BatchPostAction.SIGN);
        wrapper.setUserId(USER_ID);

        RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(BatchPackageController.class, "forSignPackageRq", wrapper);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        wrapper = res.getResult();
        BatchPackage pkg = (BatchPackage) baseEntityRepository.findById(BatchPackage.class, wrapper.getPkgId());
        Assert.assertEquals(BatchPackageState.ON_CONTROL, pkg.getPackageState());
        Assert.assertNotNull(pkg.getDateLoad());
    }

	/**
     * Удаление загруженного пакета - своего и чужого
     * @throws Exception
     */
    @Test
    public void testDeletePackage() throws Exception {
        Long userId = USER_ID;
		// создать пакет
		PackageParam param = loadPackage(userId);

		// удалить сразу
        ManualOperationWrapper wrapper = new ManualOperationWrapper();
        wrapper.setPkgId(param.getId());
		wrapper.setAction(BatchPostAction.DELETE);
        wrapper.setUserId(userId);
        BatchPackage pkg0 = (BatchPackage) baseEntityRepository.findById(BatchPackage.class, wrapper.getPkgId());
        Assert.assertNotNull(pkg0);

        RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(BatchPackageController.class, "deletePackageRq", wrapper);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        BatchPackage pkg = (BatchPackage) baseEntityRepository.findById(BatchPackage.class, wrapper.getPkgId());
        Assert.assertNull(pkg);
        BatchPosting posting = (BatchPosting) baseEntityRepository.selectFirst(BatchPosting.class, "from BatchPosting p where p.packageId = ?1",
                pkg0.getId());
        Assert.assertNull(posting);

		// снова создать пакет
		param = loadPackage(userId);

		// передать на подпись
        ManualOperationWrapper wrapper1 = new ManualOperationWrapper();
        wrapper1.setPkgId(param.getId());
		wrapper1.setAction(BatchPostAction.SIGN);
        wrapper1.setUserId(userId);

        res = remoteAccess.invoke(BatchPackageController.class, "forSignPackageRq", wrapper1);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
		
		// удалить (сделать невидимым)
        ManualOperationWrapper wrapper2 = new ManualOperationWrapper();
        wrapper2.setPkgId(param.getId());
		wrapper2.setAction(BatchPostAction.DELETE);
        wrapper2.setUserId(userId);

        // изменить пользователя
        baseEntityRepository.executeNativeUpdate("update GL_BATPST set USER_NAME = 'USER1' where ID_PKG = ?", wrapper2.getPkgId());

        res = remoteAccess.invoke(BatchPackageController.class, "deletePackageRq", wrapper2);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        pkg = (BatchPackage) baseEntityRepository.findById(BatchPackage.class, wrapper2.getPkgId());
        Assert.assertNotNull(pkg);
        Assert.assertEquals(BatchPackageState.DELETED, pkg.getPackageState());
        BatchPosting posting0 = (BatchPosting) baseEntityRepository.selectFirst(BatchPosting.class, "from BatchPosting p where p.packageId = ?1",
                pkg.getId());
        Assert.assertNotNull(posting0);
        Assert.assertEquals(InvisibleType.U, posting0.getInvisible());
    }

    /**
     * Авторизация пакета
     * @throws Exception
     */
    @Test
    public void testAuthorizePackage() throws Exception {
        // создать пакет
        PackageParam param = loadPackage(USER_ID);

        // передать на подпись
        ManualOperationWrapper wrapper1 = new ManualOperationWrapper();
        wrapper1.setPkgId(param.getId());
        wrapper1.setAction(BatchPostAction.SIGN);
        wrapper1.setUserId(USER_ID);

        RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(BatchPackageController.class, "forSignPackageRq", wrapper1);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());

        // авторизовать
        ManualOperationWrapper wrapper2 = new ManualOperationWrapper();
        wrapper2.setPkgId(param.getId());
        wrapper2.setAction(BatchPostAction.SIGN);

        // изменить пользователя
        baseEntityRepository.executeNativeUpdate("update GL_BATPST set USER_NAME = 'zz' where ID_PKG = ?", wrapper2.getPkgId());

        wrapper2.setUserId(USER_ID);
        res = remoteAccess.invoke(BatchPackageController.class, "authorizePackageRq", wrapper2, BatchPostStep.HAND2);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        BatchPackage pkg = (BatchPackage) baseEntityRepository.findById(BatchPackage.class, wrapper2.getPkgId());
        Assert.assertNotNull(pkg);
        List<BatchPosting> postings = (List<BatchPosting>) baseEntityRepository.select(BatchPosting.class, "from BatchPosting p where p.packageId = ?1 and p.invisible = ?2",
                pkg.getId(), InvisibleType.N);
        Assert.assertNotNull(postings);
        for (BatchPosting posting: postings) {
            Assert.assertEquals(BatchPostStatus.SIGNED, posting.getStatus());
        }
    }

    /**
     * Авторизация пакета с движением в сервис
     * @throws Exception
     */
    @Test
    public void testAuthorizePackageMC() throws Exception {
        // создать пакет
        PackageParam param = loadPackage(USER_ID, exampleBatchMcName);

        // передать на подпись
        ManualOperationWrapper wrapper1 = new ManualOperationWrapper();
        wrapper1.setPkgId(param.getId());
        wrapper1.setAction(BatchPostAction.SIGN);
        wrapper1.setUserId(USER_ID);

        RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(BatchPackageController.class, "forSignPackageRq", wrapper1);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());

        // авторизовать
        ManualOperationWrapper wrapper2 = new ManualOperationWrapper();
        wrapper2.setPkgId(param.getId());
        wrapper2.setAction(BatchPostAction.SIGN);

        // изменить пользователя
        baseEntityRepository.executeNativeUpdate("update GL_BATPST set USER_NAME = 'zz' where ID_PKG = ?", wrapper2.getPkgId());

        wrapper2.setUserId(USER_ID);
        res = remoteAccess.invoke(BatchPackageController.class, "authorizePackageRq", wrapper2, BatchPostStep.HAND2);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        BatchPackage pkg = (BatchPackage) baseEntityRepository.findById(BatchPackage.class, wrapper2.getPkgId());
        Assert.assertNotNull(pkg);
        List<BatchPosting> postings = (List<BatchPosting>) baseEntityRepository.select(BatchPosting.class, "from BatchPosting p where p.packageId = ?1 and p.invisible = ?2 order by p.id",
                pkg.getId(), InvisibleType.N);
        Assert.assertNotNull(postings);
        for (BatchPosting posting: postings) {
            Assert.assertTrue(BatchPostStatus.SIGNEDVIEW == posting.getStatus() || BatchPostStatus.WAITSRV == posting.getStatus());
        }
    }

    public static PackageParam loadPackage(Long userId) {
        return loadPackage(userId, exampleBatchName);
    }

    public static PackageParam loadPackage(Long userId, String pkgName) {
        String msg = "";
        File file = new File(BatchMessageIT.class.getClassLoader().getResource(pkgName).getFile());

        Map<String, String> params = new HashMap<>();
        params.put("filename", file.getAbsolutePath());
        params.put("userid", userId.toString());
        params.put("movement_off", "false");
        params.put("source", "K+TP");
        params.put("department", "AAC");

        msg = remoteAccess.invoke(BatchMessageProcessorBean.class, "processMessage", file, params);
	
        PackageParam param = getPackageParam(msg);
        Assert.assertNotNull(param.getId());
        Assert.assertEquals(0, param.getErrorCount());
		return param;
	}
		
    private static PackageParam getPackageParam(String result) {
        // "#ID пакета: (\\d+)#Загружено строк всего: (\\d+)#Загружено с ошибкой: (\\d+)#"
        Pattern pattern = Pattern.compile("#ID пакета: (\\d+)#Загружено строк всего: (\\d+)#Загружено с ошибкой: (\\d+)#");
        Matcher matcher = pattern.matcher(result);
        matcher.find();
        PackageParam param = new PackageParam(null, 0, 0);
        if (matcher.groupCount() == 3) {
            param.setId(Long.parseLong(matcher.group(1)));
            param.setPostingCount(Integer.parseInt(matcher.group(2)));
            param.setErrorCount(Integer.parseInt(matcher.group(3)));
        }
        return param;
    }

    public static class PackageParam {
        private Long id;
        private int postingCount;
        private int errorCount;

        public PackageParam(Long id, int postingCount, int errorCount) {
            this.id = id;
            this.postingCount = postingCount;
            this.errorCount = errorCount;
        }

        public Long getId() {
            return id;
        }

        public int getPostingCount() {
            return postingCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public void setPostingCount(int postingCount) {
            this.postingCount = postingCount;
        }

        public void setErrorCount(int errorCount) {
            this.errorCount = errorCount;
        }

        @Override
        public String toString() {
            return String.format("ID: %d Count: %d Errors: %d", id, postingCount, errorCount);
        }
    }
}
