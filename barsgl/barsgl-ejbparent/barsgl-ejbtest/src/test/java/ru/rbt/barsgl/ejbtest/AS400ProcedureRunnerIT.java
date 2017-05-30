package ru.rbt.barsgl.ejbtest;

import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.barsgl.ejb.etc.AS400ProcedureRunner;

import java.util.logging.Logger;

/**
 * Created by ER22228
 */
public class AS400ProcedureRunnerIT extends AbstractTimerJobIT {

    public static final Logger logger = Logger.getLogger(AS400ProcedureRunnerIT.class.getName());

    // Вызываем метод бина, который вызывает тестовый класс из тестового async.jar
    // Смотрим результат в файле /Root/GCP/example.txt на TMB07

    // Тест должен длиться более 10 секунд
    @Test
    @Ignore
    public void testProcedureCall() throws Exception {
        long start = System.currentTimeMillis();
        remoteAccess.invoke(AS400ProcedureRunner.class, "callSynhro", new Object[]{"/GCP","/GCP:/GCP/async.jar", "ru.rb.test.Example", new Object[]{"один", 2, 33.67}});
        System.out.println(System.currentTimeMillis() - start);
    }

/*    @Test
    @Ignore
    public void testProcedureCallAsyncThread() throws Exception {
        long start = System.currentTimeMillis();
        remoteAccess.invoke(AS400ProcedureRunner.class, "callAsyncThread", new Object[]{"/GCP","/GCP:/GCP/async.jar", "ru.rb.test.Example", new Object[]{"один", 2, 33.67}});
        System.out.println(System.currentTimeMillis() - start);
//        Thread.sleep(30000);
//        System.out.println("Wait finished");
    }*/

    // Тест должен длиться менее 10 секунд
    @Test
    @Ignore
    public void testProcedureCallAsync() throws Exception {
        long start = System.currentTimeMillis();
        remoteAccess.invoke(AS400ProcedureRunner.class, "callAsync", new Object[]{"/GCP/async.jar", "ru.rb.test.Example", new Object[]{"один", 2, 55.67}});
        System.out.println(System.currentTimeMillis() - start);
    }

    // Тест должен длиться менее 10 секунд
    /*@Test
    @Ignore
    public void testProcedureCallAsyncNoFolder() throws Exception {
        long start = System.currentTimeMillis();
        remoteAccess.invoke(AS400ProcedureRunner.class, "callAsync", new Object[]{"/GCP","/GCP:/GCP/async.jar", "ru.rb.test.Example", new Object[]{"один", 2, 99.67}});
        System.out.println(System.currentTimeMillis() - start);
    }*/

/*
    public void callSynhro(String folderName, String jarName, String className, Object[] params) {
            baseEntityRepository.executeNativeUpdate("CALL DWH.QCMDEXC2 (CRTJOBD JOBD(QGPL/BARSJOBD) TEXT('Job Description for BARS') INLLIBL(DWH QTEMP QGPL) AUT(*CHANGE)");
    }
*/

}

/*
// Исходный текст тестового async.jar

package ru.rb.test;
    import java.io.FileWriter;
    import java.io.IOException;
    import java.util.Arrays;
    import java.util.concurrent.TimeUnit;

public class Example {
    public static void main(String[] args) throws InterruptedException, IOException {
        TimeUnit.SECONDS.sleep(10);
        FileWriter fw = new FileWriter("example.txt");
        fw.write(Arrays.toString(args));
        fw.flush();
        fw.close();
    }
}
*/