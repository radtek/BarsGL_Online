package ru.rbt.barsgl.ejbtest.utl;

import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.barsgl.ejbcore.util.StringUtils;
import ru.rbt.barsgl.shared.Builder;
import ru.rbt.barsgl.shared.enums.JobStartupType;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

/**
 * Created by Ivan Sevastyanov on 17.02.2016.
 */
public class SingleActionJobBuilder implements Builder<SingleActionJob> {

    private final SingleActionJob job = new SingleActionJob();

    private SingleActionJobBuilder() {
        final String ts = System.currentTimeMillis() + "";
        job.setName(StringUtils.rsubstr(ts, 5));
        job.setDescription("desc_" + job.getName());
        job.setStartupType(JobStartupType.MANUAL);
        job.setState(TimerJob.JobState.STOPPED);
    }

    public static SingleActionJobBuilder create() {
        return new SingleActionJobBuilder();
    }

    public SingleActionJobBuilder withClass(Class<? extends ParamsAwareRunnable> clazz) {
        job.setRunnableClass(clazz.getName());
        return this;
    }

    public SingleActionJobBuilder withProps(String props) {
        job.setProperties(props);
        return this;
    }

    public SingleActionJobBuilder withProps(Properties props) {
        try {
            StringWriter propsWriter = new StringWriter();
            props.store(propsWriter, "tests");
            job.setProperties(propsWriter.toString());
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SingleActionJobBuilder withName(String name) {
        job.setName(name);
        return this;
    }

    @Override
    public SingleActionJob build() {
        return job;
    }
}
