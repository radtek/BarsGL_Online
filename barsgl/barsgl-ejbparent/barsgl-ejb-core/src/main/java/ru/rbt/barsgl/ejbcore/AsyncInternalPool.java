package ru.rbt.barsgl.ejbcore;

import ru.rbt.ejbcore.JpaAccessCallback;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;
import ru.rbt.ejbcore.JpaAccessCallback;

/**
 * Created by Ivan Sevastyanov on 24.05.2016.
 */
public class AsyncInternalPool <T extends JpaAccessCallback<R>, R> {

    private BlockingQueue<T> queue;
    private int size;
    private CoreRepository asyncProcessor;
    private Future<R>[] lastFutures;
    private AtomicInteger counter = new AtomicInteger(0);
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss SSS z Z");

    public AsyncInternalPool(int size, CoreRepository asyncProcessor) {
        this.size = size;
        this.asyncProcessor = asyncProcessor;
        queue = new ArrayBlockingQueue<>(size);
        lastFutures = new Future[size];
    }

    public Future<R> offer(T task, long timeout, TimeUnit unit) throws Exception {
        queue.offer(task, timeout, unit);
        Future<R> future = asyncProcessor.invokeAsynchronous(task);
        int count = counter.getAndIncrement();
        if (count > size - 1) {
            synchronized (this) {
                count = counter.getAndIncrement();
                if (count > size - 1) {
                    counter.set(0);
                    count = 0;
                }
            }
        }
        lastFutures[count] = future;
        return future;
    }

    /**
     *
     * @param tillTo
     * @throws InterruptedException
     */
    public void awaitCompletion(long tillTo) throws InterruptedException, TimeoutException {
        while (queue.size() != 0) {
            TimeUnit.MILLISECONDS.sleep(10);
            checkTimeout(tillTo);
        }
    }

    public void releaseOne() {
        queue.remove();
    }

    public void cancelRemaining() {
        for (Future<R> future : lastFutures) {
            if (future.isDone()) {
                future.cancel(true);
            }
        }
    }

    private <T> void checkTimeout(long till) throws TimeoutException {
        if (System.currentTimeMillis() > till) {
            throw new TimeoutException(format("Async operation is timed out. Current time '%s' greater than '%s'"
                    , dateFormat.format(new Date()), dateFormat.format(new Date(till))));
        }
    }
}
