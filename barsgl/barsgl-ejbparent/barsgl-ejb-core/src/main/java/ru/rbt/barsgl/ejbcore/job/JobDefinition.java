package ru.rbt.barsgl.ejbcore.job;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

/**
 * Created by Ivan Sevastyanov
 */
class JobDefinition {
    private final String name;
    private final long delay;
    private final long interval;
    private final ParamsAwareRunnable worker;
    private final String calendarString;
    private final boolean singleAction;
    private Properties properties =  new Properties();

    /**
     *
     * @param name
     * @param delay играет роль "intervalDuration"
     * @param worker
     */
    public JobDefinition(String name, long delay, long interval, ParamsAwareRunnable worker) {
        this(name, delay, interval, worker, null, false, (String)null);
    }

    /**
     *
     * @param name название задачи
     * @param delay задержка старта задачи в миллисекундах
     * @param calendarString строка в формате {@link javax.ejb.ScheduleExpression}, например "hour=1;minute=*"
     * @param worker реализация задачи
     */
    public JobDefinition(String name, long delay, String calendarString, ParamsAwareRunnable worker) {
        this(name, delay, 0, worker, calendarString, false, (String)null);
    }

    /**
     * @param name название задачи
     * @param worker запускаемая задача
     * @param delay играет роль "intervalDuration"
     * @param singleAction если <code>true</code>, будет выполняться единожды
     */
    public JobDefinition(String name, boolean singleAction, ParamsAwareRunnable worker, long delay) {
        this(name, delay, 0, worker, null, singleAction, (String) null);
    }

    public JobDefinition(String name, long delay, long interval, ParamsAwareRunnable worker
            , String calendarString, boolean singleAction, String properties) {
        if (name == null) {
            throw new IllegalArgumentException("Scheduled job name cannot be null");
        }
        if (worker == null) {
            throw new IllegalArgumentException("Scheduled job worker cannot be null");
        }
        this.name = name;
        this.delay = delay;
        this.interval = interval;
        this.worker = worker;
        this.calendarString = calendarString;
        this.singleAction = singleAction;
        parseProps(properties);
    }

    public JobDefinition(String name, long delay, long interval, ParamsAwareRunnable worker
            , String calendarString, boolean singleAction, Properties properties) {
        this(name, delay, interval, worker, calendarString, singleAction, (String)null);
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public long getDelay() {
        return delay;
    }

    public ParamsAwareRunnable getWorker() {
        return worker;
    }

    public String getCalendarString() {
        return calendarString;
    }

    public boolean isSingleAction() {
        return singleAction;
    }

    public long getInterval() {
        return interval;
    }

    private void parseProps(String properties) {
        if (StringUtils.isEmpty(properties)) return;

        try {
            this.properties.load(new StringReader(properties));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Properties getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "ru.rbt.barsgl.ejbcore.job.JobDefinition{" +
                "name='" + name + '\'' +
                ", delay=" + delay +
                ", interval=" + interval +
                ", worker=" + worker +
                ", calendarString='" + calendarString + '\'' +
                ", singleAction=" + singleAction +
                ", properties=" + properties +
                '}';
    }
}
