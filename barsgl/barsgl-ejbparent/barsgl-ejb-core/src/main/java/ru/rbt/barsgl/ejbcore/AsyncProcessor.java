package ru.rbt.barsgl.ejbcore;

import ru.rbt.ejbcore.JpaAccessCallback;
import ru.rbt.ejbcore.util.DateUtils;

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
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedThreadFactory;
import ru.rbt.ejb.repository.properties.PropertiesRepository;

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

    private static int DEFAULT_MAX_POOL_SIZE = 5000;
    
    @EJB
    private CoreRepository repository;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private PropertiesRepository propertiesRepository;

    private BlockingQueue<InternalAsyncTask> internalQueue;

    
    @Resource
    private ManagedThreadFactory managedThreadFactory;    
    
    private ThreadPoolExecutor defaultThreadPoolExecutor;
    
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
        asyncProcessPooledByExecutor(callbacks, maxConcurrency, timeout, unit);
   }

    /**
     * Обрабатываем асинхронно с таймаутом на обработку всех заданий
     * @param callbacks задания
     * @param maxConcurrency одновременно
     * @param timeout таймаут
     * @param unit единицы измерения
     * @param <T> параметр
     * @throws Exception
     */
    public <T> void asyncProcessPooledOld(List<JpaAccessCallback<T>> callbacks, int maxConcurrency
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
     * Обрабатываем асинхронно с таймаутом на обработку всех заданий
     * Increase performance up to 20%
     * @param callbacks задания
     * @param maxConcurrency одновременно
     * @param timeout таймаут
     * @param unit единицы измерения
     * @param <T> параметр
     * @throws Exception
     */
    public <T> void asyncProcessPooledByExecutor(List<JpaAccessCallback<T>> callbacks, int maxConcurrency
            , long timeout, TimeUnit unit) throws Exception {      
        if(!callbacks.isEmpty()){
            logger.info(format("Starting async processing(ManagedThreadFactory) callbacks: '%s'", callbacks.size()));
            //int maxPoolSize = callbacks.size() < maxConcurrency ? maxConcurrency + 1 : callbacks.size() + 1;// + 1 -- for managed
            final long tillTo = System.currentTimeMillis() + unit.toMillis(timeout);

            //create with fixed thread pool size
            ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                  maxConcurrency,
                  maxConcurrency,
                  0L,// A time value of zero will cause excess threads to terminate immediately after executing tasks(see doc) OFFER_DEFAULT_TIMEOUT_MS, 
                  MILLISECONDS,
                  new ArrayBlockingQueue<>(callbacks.size()), 
                  managedThreadFactory);
            
            callbacks.stream().forEach(callback -> {
                threadPoolExecutor.submit(() -> {
                  return repository.invoke((persistence) -> {
                    return callback.call(persistence);
                  }).get();
                });
            });

            awaitTermination(threadPoolExecutor, timeout, unit, tillTo);
        }
    }
    
    public void awaitTermination(ThreadPoolExecutor threadPoolExecutor, long timeout, TimeUnit unit, long tillTo) throws Exception {
        try {
            threadPoolExecutor.shutdown();
            if (threadPoolExecutor.awaitTermination(timeout, unit)) {
                logger.log(Level.INFO, "All threads are terminated");
            } else {
                throw new TimeoutException(format("Async operation is timed out. Current time '%s' greater than '%s'",
                         dateUtils.fullDateString(new Date()), dateUtils.fullDateString(new Date(tillTo))));
            }
        } catch (InterruptedException ex) {
            throw new Exception("Execution tasks is interrupted", ex);
        }
    }
    
    public <T> void submitToDefaultExecutor(JpaAccessCallback<T> callback, int maxConcurrency) {
        ExecutorService executorService = getDefaultThreadPoolExecutor(maxConcurrency);

        executorService.submit(() -> {
            return repository.invoke((persistence) -> {
                return callback.call(persistence);
            }).get();
        });
    }
    
    public ExecutorService getDefaultThreadPoolExecutor(int corePoolSize){
      //create with fixed thread pool size
      if(defaultThreadPoolExecutor == null){
        defaultThreadPoolExecutor = new ThreadPoolExecutor(
              corePoolSize,
              corePoolSize,
              0L,// A time value of zero will cause excess threads to terminate immediately after executing tasks(see doc) OFFER_DEFAULT_TIMEOUT_MS, 
              MILLISECONDS,
              new LinkedBlockingQueue<>(), 
              managedThreadFactory);
        
      }
      defaultThreadPoolExecutor.setCorePoolSize(corePoolSize);
      defaultThreadPoolExecutor.setMaximumPoolSize(corePoolSize);
      return defaultThreadPoolExecutor;
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
