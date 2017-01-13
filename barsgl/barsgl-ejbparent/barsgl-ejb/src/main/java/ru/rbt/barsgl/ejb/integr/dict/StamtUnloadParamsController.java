package ru.rbt.barsgl.ejb.integr.dict;

import ru.rbt.barsgl.ejb.entity.dict.StamtUnloadParam;
import ru.rbt.barsgl.ejb.repository.dict.StamtUnloadParamRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.StamtUnloadParamWrapper;

import javax.inject.Inject;

import static java.lang.String.format;

/**
 * Created by Ivan Sevastyanov on 19.01.2016.
 */
public class StamtUnloadParamsController extends BaseDictionaryController<StamtUnloadParamWrapper, String, StamtUnloadParam, StamtUnloadParamRepository>{

    @Inject
    private StamtUnloadParamRepository repository;

    @Override
    public RpcRes_Base<StamtUnloadParamWrapper> create(StamtUnloadParamWrapper wrapper) {
        return create(wrapper, repository, StamtUnloadParam.class, wrapper.getAccount(),
                format("Настройка для '%s' уже существует", wrapper.getAccount()),
                format("Создана настройка для: '%s'", wrapper.getAccount()),
                format("Ошибка при создании настройки: '%s'", wrapper.getAccount()),
                () ->  new StamtUnloadParam(wrapper.getAccount(), wrapper.getParamType(), wrapper.getParamTypeCheck(), wrapper.getParamTypeCheckBln()));
    }

    @Override
    public RpcRes_Base<StamtUnloadParamWrapper> update(StamtUnloadParamWrapper wrapper) {
        String primaryKey = wrapper.getAccount();
        return update(wrapper,repository, StamtUnloadParam.class,
                primaryKey,
                format("Настройка '%s' не найдена", primaryKey),
                format("Изменена настройка: '%s'", primaryKey),
                format("Ошибка при изменении настройки: '%s'", primaryKey),
                param -> {
                    param.setType(wrapper.getParamType());
                    param.setCheck(wrapper.getParamTypeCheck());
                    param.setCheckBln(wrapper.getParamTypeCheckBln());
                });
    }

    @Override
    public RpcRes_Base<StamtUnloadParamWrapper> delete(StamtUnloadParamWrapper wrapper) {
        String primaryKey = wrapper.getAccount();
        return delete(wrapper, repository, StamtUnloadParam.class, primaryKey,
                format("Настройка '%s' не найдена", primaryKey),
                format("Удалена настройка: '%s'", primaryKey),
                format("Ошибка при удалении настройки: '%s'", primaryKey)
        );
    }

}
