package ru.rbt.barsgl.ejb.integr.bg.fan;

import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.fan.FanOperationProcessor;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by Ivan Sevastyanov on 15.09.2016.
 * TODO ВНИМАНИЕ копипаст c ru.rbt.barsgl.ejb.integr.bg.EtlPostingController!!!!!!!!!
 */
@Stateless
@LocalBean
public class FanProcessorStorage {

    @Inject
    private Instance<FanOperationProcessor> operationProcessors;

    private List<FanOperationProcessor> fanProcessorsCached;

    /**
     * поиск обработчика для операции
     * @param operation
     * @return
     */
    public FanOperationProcessor findOperationProcessor(GLOperation operation) throws Exception {
        return findSupported(fanProcessorsCached, p -> p.isSupported(operation)
                , () -> new DefaultApplicationException(String.format("Не найдено обработчика для веера '%s'", operation))
                , () -> new DefaultApplicationException(String.format("Найдено более одного обработчика для веера '%s'", operation)));
    }

    private <T> T findSupported(List<T> beans, Predicate<T> predicate
            , Supplier<? extends Exception> throwsNotFound
            , Supplier<? extends Exception> throwsTooMany) throws Exception {
        List<T> filtered = beans.stream().filter(predicate).collect(Collectors.toList());
        return 1 == filtered.size() ? filtered.get(0) : (filtered.isEmpty() ? throwsSupplied(throwsNotFound)
                : throwsSupplied(throwsTooMany));
    }

    private <T> T throwsSupplied(Supplier<? extends Exception> supplier) throws Exception {
        throw supplier.get();
    }


    @PostConstruct
    public void init() {
        fanProcessorsCached = new ArrayList<>();
        for (FanOperationProcessor processor : operationProcessors) {
            fanProcessorsCached.add(processor);
        }
    }

}
