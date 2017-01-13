package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.excel.BatchMessageProcessor;
import ru.rbt.barsgl.ejb.entity.etl.BatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.integr.bg.BatchPackageController;
//import ru.rbt.barsgl.gwt.server.upload.ExcelParser;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.BatchPostAction;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.BatchPostStep;
import ru.rbt.barsgl.shared.enums.InvisibleType;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ER18837 on 29.02.16.
 * Пакетная обработка проводок из файла Excel
 */
public class BatchMessageTest extends AbstractTimerJobTest {

//    private static final String ETL_SINGLE_ACTION_MONITOR = "ETL_SINGLE_ACTION_MONITOR";

    @BeforeClass
    public static void beforeClass() {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
    }

    /**
     * Загрузка сообщений из файла Excel
     * @throws Exception
     */
    @Test
    public void testLoadPackage() throws Exception {
        // TODO поиск юзера с нужными правами
        Long userId = 2L;
        try (InputStream is = BatchMessageTest.class.getClassLoader().getResourceAsStream("example_batch.xlsx")){
//            ExcelParser parser = new ExcelParser(is);
//            List<List<Object>> rows = parser.parse(1, "K+TP", "AAC");

//            rows = Lists.newArrayList(Iterables.transform(rows, (List<Object> objects) -> {
//                objects.set(0, System.currentTimeMillis() + "");
//                objects.set(7, getOperday().getCurrentDate());
//                return objects;
//            }));

//            Assert.assertTrue(Iterables.all(rows, row ->
//                    null != row.get(17) && null != row.get(18) && null != row.get(19) && null != row.get(20)));

//            String msg = remoteAccess.invoke(BatchMessageProcessor.class, "processMessage", rows, "test.xlsx", userId, false);
//            System.out.println(msg);
//            PackageParam param = getPackageParam(msg);
//            Assert.assertNotNull(param.getId());
//            Assert.assertEquals(0, param.getErrorCount());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.assertFalse(true);
        }
    }

    /**
     * Загрузка сообщений из файла Excel и передача на подпись
     * @throws Exception
     */
    @Test
    public void testForSignPackage() throws Exception {
        Long userId = 2L;
		// создать пакет
		PackageParam param = loadPackage(userId);

		// передать на подпись
        ManualOperationWrapper wrapper = new ManualOperationWrapper();
        wrapper.setPkgId(param.getId());
		wrapper.setAction(BatchPostAction.SIGN);
        wrapper.setUserId(userId);

        RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(BatchPackageController.class, "forSignPackageRq", wrapper);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        wrapper = res.getResult();
        BatchPackage pkg = (BatchPackage) baseEntityRepository.findById(BatchPackage.class, wrapper.getPkgId());
        Assert.assertEquals(EtlPackage.PackageState.PROCESSED, pkg.getPackageState());
        Assert.assertNotNull(pkg.getDateLoad());
    }

	/**
     * Удаление загруженного пакета - своего и чужого
     * @throws Exception
     */
    @Test
    public void testDeletePackage() throws Exception {
        Long userId = 2L;
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
        Assert.assertEquals(BatchPackage.PackageState.DELETED, pkg.getPackageState());
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
        Long userId = 2L;
        // создать пакет
        PackageParam param = loadPackage(userId);

        // передать на подпись
        ManualOperationWrapper wrapper1 = new ManualOperationWrapper();
        wrapper1.setPkgId(param.getId());
        wrapper1.setAction(BatchPostAction.SIGN);
        wrapper1.setUserId(userId);

        RpcRes_Base<ManualOperationWrapper> res = res = remoteAccess.invoke(BatchPackageController.class, "forSignPackageRq", wrapper1);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());

        // авторизовать
        ManualOperationWrapper wrapper2 = new ManualOperationWrapper();
        wrapper2.setPkgId(param.getId());
        wrapper2.setAction(BatchPostAction.SIGN);

        // изменить пользователя
        baseEntityRepository.executeNativeUpdate("update GL_BATPST set USER_NAME = 'zz' where ID_PKG = ?", wrapper2.getPkgId());

        wrapper2.setUserId(userId);
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
            Assert.assertEquals(BatchPostStatus.COMPLETED, posting.getStatus());
        }
    }

    public static PackageParam loadPackage(Long userId) {
        String msg = "";
        try (InputStream is = BatchMessageTest.class.getClassLoader().getResourceAsStream("example_batch.xlsx")){
//            ExcelParser parser = new ExcelParser(is);
//            List<List<Object>> rows = parser.parse(1, "K+TP", "AAC");
//            msg = remoteAccess.invoke(BatchMessageProcessor.class, "processMessage", rows, "test.xlsx", userId, false);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.assertFalse(true);
        }
	
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
    }
}
