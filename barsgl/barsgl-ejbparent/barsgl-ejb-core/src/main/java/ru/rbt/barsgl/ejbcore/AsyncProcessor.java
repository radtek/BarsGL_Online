package ru.rbt.barsgl.ejbcore;

import ru.rbt.barsgl.ejbcore.repository.PropertiesRepository;
import ru.rbt.barsgl.ejbcore.util.DateUtils;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Created by Ivan Sevastyanov
 */
@Singleton
@Lock(LockType.READ)
@AccessTimeout(value = 15, unit = MINUTES)
public class AsyncProcessor {

    public static final Logger logger = Logger.getLogger(AsyncProcessor.class.getName());

    private static long OFFER_DEFAULT_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(15);
    private static long INIT_DEFAULT_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(15);

    @EJB
    private CoreRepository repository;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private PropertiesRepository propertiesRepository;

    private BlockingQueue<InternalAsyncTask> internalQueue;

    /**
     * Обрабатываем асинхронно с таймаутом на обработку всех заданий
     * @param callbacks задания
     * @param maxConcurrency одновременно
     * @param timeout таймаут
     * @param unit единицы измерения
     * @param <T> параметр
     * @throws Exception
     */
    public <T> void asyncProcessPooled(List<JpaAccessCallback<T>> callbacks, int maxConcurrency
            , long timeout, TimeUnit unit) throws Exception {
        logger.info(format("Starting async processing callbacks: '%s'", callbacks.size()));
        final long tillTo = System.currentTimeMillis() + unit.toMillis(timeout);
        BlockingQueue<InternalAsyncTask> pool = new ArrayBlockingQueue<>(maxConcurrency);
        try {
            for (JpaAccessCallback<T> callback : callbacks) {
                checkTimeout(tillTo);
                InternalAsyncTask<T> intCallback = new InternalAsyncTask<>(callback, pool);
                if (pool.offer(intCallback, OFFER_DEFAULT_TIMEOUT_MS, MILLISECONDS)) {
                    repository.invokeAsynchronous(intCallback);
                } else {
                    throw new RuntimeException(format("Timeout is exceeded in waiting pool free space, size: %s", maxConcurrency));
                }
                logger.log(Level.FINE, format("Current pool size: %s after submit callback", pool.size()));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error on offering task", e);
        }
        awaitCompletion(pool, tillTo);
    }

    /**
     * !!!Не дает конкуренции. Нужно профилировать
     * @param callback
     * @param pool
     * @param offertimeout
     * @param unit
     * @param <T>
     * @return
     * @throws Exception
     */
    @Lock(LockType.READ)
    public <T> Future<T> asyncProcessPooled(JpaAccessCallback<T> callback, BlockingQueue pool
            , long offertimeout, TimeUnit unit) throws Exception {
        InternalAsyncTask<T> internalAsyncTask = new InternalAsyncTask<>(callback, pool);
        if (pool.offer(internalAsyncTask, offertimeout, unit)) {
            logger.log(Level.INFO, format("Current pool size: %s before processed callback", pool.size()));
            return repository.invokeAsynchronous(internalAsyncTask);
        } else {
            throw new RuntimeException(format("Timeout is exceeded in waiting pool free space, size: %s, remaining: %s"
                    , pool.size(), pool.remainingCapacity()));
        }
    }

    /**
     * асинхронная обработка с
     * @param callback Обработчик
     * @param <T> параметризованный тип
     * @return результат выполнения
     * @throws Exception
     */
    public <T> Future<T> invokeAsync(JpaAccessCallback<T> callback) throws Exception {
        InternalAsyncTask<T> intCallback = new InternalAsyncTask<>(callback, internalQueue);
        if (internalQueue.offer(intCallback, OFFER_DEFAULT_TIMEOUT_MS, MILLISECONDS)) {
            return repository.invokeAsynchronous(intCallback);
        } else {
            throw new RuntimeException(format("Timeout is exceeded in waiting pool free space, size: %s"
                    , propertiesRepository.getNumber("pd.cuncurency").intValue()));
        }
    }

    private <T> void checkTimeout(long till) throws TimeoutException {
        if (System.currentTimeMillis() > till) {
            throw new TimeoutException(format("Async operation is timed out. Current time '%s' greater than '%s'"
                    , dateUtils.fullDateString(new Date()), dateUtils.fullDateString(new Date(till))));
        }
    }

    private class InternalAsyncTask <T> implements JpaAccessCallback {

        private final JpaAccessCallback<T> callback;
        private BlockingQueue<InternalAsyncTask> pool;

        public InternalAsyncTask(JpaAccessCallback<T> callback, BlockingQueue<InternalAsyncTask> pool) {
            this.callback = callback;
            this.pool = pool;
        }

        @Override
        public Object call(EntityManager persistence) throws Exception {
            try {
                return callback.call(persistence);
            } finally {
                pool.remove();
                logger.log(Level.FINE,format("Current pool size after processing callback: %s", pool.size()));
            }
        }
    }

    /**
     * ждем обработки всех задач
     * @param unit timeunit
     * @param timeout timeunit quantity
     * @throws InterruptedException when sleep interrupeted
     * @throws TimeoutException when wait timeout exceeded
     */
    @Lock(LockType.WRITE)
    public void awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        awaitCompletion(internalQueue, timeout, unit);
    }

    /**
     * ждем обработки всех задач
     * @param queue проверяемая очередь
     * @param unit timeunit
     * @param timeout timeunit quantity
     * @throws InterruptedException when sleep interrupeted
     * @throws TimeoutException when wait timeout exceeded
     */
    public void awaitCompletion(BlockingQueue queue, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        long tillTo = System.currentTimeMillis() + unit.toMillis(timeout);
        awaitCompletion(queue, tillTo);
    }

    private void awaitCompletion(BlockingQueue queue, long tillTo) throws InterruptedException, TimeoutException {
        while (queue.size() != 0){
            MILLISECONDS.sleep(10);
            checkTimeout(tillTo);
        }
    }

    @Lock(LockType.WRITE)
    public void initConcurrency() {
        try {
            if (null != internalQueue) {
                awaitCompletion(internalQueue
                        , INIT_DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            }
            internalQueue = new ArrayBlockingQueue<>(propertiesRepository.getNumber("pd.cuncurency").intValue());
        } catch (Throwable e) {
            throw new RuntimeException("Error on initialize async processor: " + e.getMessage(), e);
        }
    }

    @PostConstruct
    public void postConstruct() {
        initConcurrency();
    }

}
