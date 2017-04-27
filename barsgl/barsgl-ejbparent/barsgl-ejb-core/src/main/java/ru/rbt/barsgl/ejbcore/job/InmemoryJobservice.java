package ru.rbt.barsgl.ejbcore.job;

import com.google.common.base.Joiner;
import org.apache.log4j.Logger;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.util.ServerUtils;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Map;

import static java.lang.String.format;

/**
 * Created by Ivan Sevastyanov
 */
@Stateless
@LocalBean
public class InmemoryJobservice {

    public static final Logger log = Logger.getLogger(InmemoryJobservice.class);

    @Resource
    private SessionContext context;

    @Inject
    private Instance<InmemoryTask> runnables;

    /**
     * Создаем задачу с контекстом выполнения
     * @param delay задержка старта
     * @param runnableClass класс пускающего бина
     * @param properties контекст выполнения
     */
    public void createSingleActionJob(int delay, Class<? extends InmemoryTask> runnableClass
            , final Map<String,Object> properties) {
        context.getTimerService().createSingleActionTimer(delay
                , new TimerConfig(new InmemoryTaskHolder(runnableClass, properties), false));
    }

    @Timeout
    public void timeout(Timer timer) {
        if (timer.getInfo() instanceof InmemoryTaskHolder) {
            final InmemoryTaskHolder holder = (InmemoryTaskHolder) timer.getInfo();
            InmemoryTask runnable = ServerUtils
                    .findAssignable(holder.getRunnableClass(), runnables);
            try {
                log.info(format("Execuiting inmemory job: '%s' with context '%s'"
                        , runnable.getClass(), mapToString(holder.getContext())));
                runnable.run(holder.getContext());
                log.info(format("Inmemory job: '%s' successfully completed with context '%s'"
                        , runnable.getClass(), mapToString(holder.getContext())));
            } catch (Exception e) {
                context.setRollbackOnly();
                log.error(format("Error on executing job: '%s' with context '%s'\n"
                        , runnable.getClass(), mapToString(holder.getContext())), e);
                throw new DefaultApplicationException(e.getMessage(), e);
            }
        } else {
            log.error(format("'%s' is not instance of '%s'", timer.getInfo(), InmemoryTaskHolder.class));
        }
    }

    private static String mapToString(Map map) {
        return null != map ? Joiner.on(";").withKeyValueSeparator("=").join(map) : "<empty>";
    }


}
