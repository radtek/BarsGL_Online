package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.MidasSubAccountsUpdateTask;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;

import java.io.StringReader;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by ER19371 on 05.11.15.
 * Выгрузка счетов доходов-расходов из системы BARS GL в систему Midas
 * @fsd ?
 */
public class MidasSubAccountsUpdateTest extends AbstractTimerJobTest {

    public static final Logger logger = Logger.getLogger(MidasSubAccountsUpdateTest.class.getName());

    /**
     * Выгрузка счетов доходов-расходов из системы BARS GL в систему Midas
     * Тестирование простого вызова
     * @fsd ?
     * @throws Exception
     */
    @Test
    public void testSimpleRun() throws Exception {

        DataRecord rec0 = baseEntityRepository.selectFirst("select max(LOGID) from dwh.NR_PROCESSLOG");
        String max_logid = rec0.getString(0);

        Properties properties = new Properties();
        properties.load(new StringReader(
                "operday=CURRENT\n" +
                "db_user=gcpuser\n" +
                "db_pwd=gcpuser1\n" +
                "db_url=jdbc:as400://tmb01:60000/dwh;naming=sql;errors=full;thread used=false"));
        properties.setProperty(MidasSubAccountsUpdateTask.DWH_UNLOAD_FULL_DATE_KEY, "01.01.1940");
        remoteAccess.invoke(MidasSubAccountsUpdateTask.class, "run", null, properties); //null, null);

        List<DataRecord> recs = baseEntityRepository.select("select PARAM3, PARAM4 from DWH.NR_PROCESSLOG where PROCNAME = ? and logid > ? order by LOGID desc FETCH FIRST 1 ROWS ONLY"
                , MidasSubAccountsUpdateTask.UNLOAD_PST_PARAM_DESC
                , max_logid);

        Assert.assertTrue("Не удалось запустить FILL_IFXBSHCP", 1 == recs.size());

        //Ожидаем, например
        // "FINISHED; Выгрузка счетов доходов-расходов из BARS GL в Midas 2015-11-05 завершена. Добавлено записей: 192730     "
        Assert.assertTrue(recs.get(0).getString(1), "FINISHED".equals(recs.get(0).getString(0)));

    }

    /**
     * Выгрузка счетов доходов-расходов из системы BARS GL в систему Midas
     * Тестирование последовательного вызова одного и того же метода для проверки
     * корректной работы
     * @fsd ?
     * @throws Exception
     */
//   Работает нестабильно - зависит от таймаутов запуска параллельных потоков (выключаем)
//    @Test
//    public void testConcurentRun() throws Exception {
//
//        DataRecord rec0 = baseEntityRepository.selectFirst("select max(LOGID) from dwh.NR_PROCESSLOG");
//        String max_logid = rec0.getString(0);
//
//        SingleActionJob midasSubAccountsUpdateJob1 = new SingleActionJob();
//        midasSubAccountsUpdateJob1.setDelay(100L);
//        midasSubAccountsUpdateJob1.setDescription("Выгрузка счетов доходов-расходов из BARS GL в Midas 1");
//        midasSubAccountsUpdateJob1.setName(MidasSubAccountsUpdateTask.NAME + "Test1");
//        midasSubAccountsUpdateJob1.setRunnableClass(MidasSubAccountsUpdateTask.class.getName());
//        midasSubAccountsUpdateJob1.setStartupType(JobStartupType.MANUAL);
//        midasSubAccountsUpdateJob1.setState(TimerJob.JobState.STOPPED);
//
//        //Тестовый запуск (без импорта в MIDAS) возможен для даты '1940-01-01', минимальная дата, полученная экспериментальным путем для DB2 v5
//        midasSubAccountsUpdateJob1.setProperties(MidasSubAccountsUpdateTask.DWH_UNLOAD_FULL_DATE_KEY + "=01.01.1940");
//        midasSubAccountsUpdateJob1 = (SingleActionJob) baseEntityRepository.save(midasSubAccountsUpdateJob1);
//        registerJob(midasSubAccountsUpdateJob1);
//
//        SingleActionJob midasSubAccountsUpdateJob2 = new SingleActionJob();
//        midasSubAccountsUpdateJob2.setDelay(0L);
//        midasSubAccountsUpdateJob2.setDescription("Выгрузка счетов доходов-расходов из BARS GL в Midas 2");
//        midasSubAccountsUpdateJob2.setName(MidasSubAccountsUpdateTask.NAME + "Test2");
//        midasSubAccountsUpdateJob2.setRunnableClass(MidasSubAccountsUpdateTask.class.getName());
//        midasSubAccountsUpdateJob2.setStartupType(JobStartupType.MANUAL);
//        midasSubAccountsUpdateJob2.setState(TimerJob.JobState.STOPPED);
//
//        //Тестовый запуск (без импорта в MIDAS) возможен для даты '1940-01-01', минимальная дата, полученная экспериментальным путем для DB2 v5
//        midasSubAccountsUpdateJob2.setProperties(MidasSubAccountsUpdateTask.DWH_UNLOAD_FULL_DATE_KEY + "=01.01.2005");
//        midasSubAccountsUpdateJob2 = (SingleActionJob) baseEntityRepository.save(midasSubAccountsUpdateJob2);
//        registerJob(midasSubAccountsUpdateJob2);
//
//        //Запускаем 2-экземпляра
//        startupAndWait(midasSubAccountsUpdateJob1, 0);
//        startupAndWait(midasSubAccountsUpdateJob2, 5000);
//
//        List<DataRecord> recs = baseEntityRepository.select("select PARAM3, PARAM4 from DWH.NR_PROCESSLOG where PROCNAME = ? and logid > ? and PARAM3='ERROR' order by LOGID desc FETCH FIRST 1 ROWS ONLY"
//                , MidasSubAccountsUpdateTask.UNLOAD_PST_PARAM_DESC
//                , max_logid);
//
//        Assert.assertTrue("Не удалось запустить FILL_IFXBSHCP", 1 == recs.size());
//
//        //Ожидаем, например
//        // "ERROR; Экземпляр этой процедуры уже запущен на сервере. Дождитесь ее завершения. (Время работы 25 сек)"
//        Assert.assertTrue(recs.get(0).getString(0), "ERROR".equals(recs.get(0).getString(0)));
//        Assert.assertTrue(recs.get(0).getString(1), recs.get(0).getString(1).startsWith("Экземпляр этой процедуры уже запущен"));
//    }


//    private void startup(SingleActionJob dwhUnloadFullJob) throws InterruptedException {
//        jobService.startupJob(dwhUnloadFullJob);
//    }
//
//    private void startupAndWait(SingleActionJob dwhUnloadFullJob, long timeout) throws InterruptedException {
//        startup(dwhUnloadFullJob);
//        Thread.sleep(timeout);
//    }

}
