package ru.rbt.barsgl.ejbtesting;

import ru.rbt.barsgl.ejb.repository.EtlPostingRepository;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;

import javax.ejb.EJB;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Ivan Sevastyanov
 */
public class AsyncTestService {

    private static final Logger logger = Logger.getLogger(AsyncTestService.class.getName());

    @EJB
    private EtlPostingRepository postingRepository;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @EJB
    private AsyncProcessor asyncProcessor;

    public void process() throws Exception {
        beanManagedProcessor.executeInNewTxWithTimeout(((persistence1, connection) -> {
            List<Future> futures = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                final int finalI = i;
                Future f = postingRepository.invokeAsynchronous(persistence -> {
                    try {
                        logger.info("");
//                        Thread.sleep(500000);
                        logger.info("EXECUTING SELECT " + finalI);
                        long cnt = postingRepository.selectFirst("select count(1) from pd p1, pd p2 where p1.pbr = '1'").getLong("cnt");
                        logger.info("COMPLETED SELECT " + finalI + " " + cnt);
                        return null;
                    } catch (Throwable e) {
                        logger.log(Level.SEVERE, "Error on executing: " + finalI, e);
                        return null;
                    }
                });
                logger.info("After posting async job " + finalI);
                futures.add(f);
            }
            /*for (Future f : futures) {
                f.get();
            }*/
            return null;
        }), 60);

    }

    public void process2(Integer count) throws Exception {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        try {
            long cnt = postingRepository.selectFirst("select count(1) cnt from tmp_test_async").getLong(0);
        } catch (SQLException e) {
            postingRepository.executeInNewTransaction(persistence ->
                    postingRepository.executeNativeUpdate("create table tmp_test_async (id bigint not null)"));
        }
        postingRepository.executeInNewTransaction(persistence1 ->
                postingRepository.executeNativeUpdate("delete from tmp_test_async"));
        List<Future<Integer>> futures = new ArrayList<>();
        for (int i=0; i< count; i++) {
            final int finalInteger = i;
            final long sleep = random.nextLong(1000);
            futures.add(asyncProcessor.invokeAsync(persistence -> {
                TimeUnit.MILLISECONDS.sleep(sleep);
                postingRepository.executeNativeUpdate("insert into tmp_test_async values(?)", new Object[]{finalInteger});
                return finalInteger;
            }));
        }

//        for (Future<Integer> future : futures) {
//            System.out.println("--------------------- ready: " + future.get());
//        }

    }
}
