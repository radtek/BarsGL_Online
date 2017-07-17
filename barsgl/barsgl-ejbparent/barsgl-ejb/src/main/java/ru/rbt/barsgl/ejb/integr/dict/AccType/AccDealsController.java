package ru.rbt.barsgl.ejb.integr.dict.AccType;

import ru.rbt.barsgl.ejb.entity.dict.AccType.GlAccDeals;
import ru.rbt.barsgl.ejb.entity.dict.StamtUnloadParam;
import ru.rbt.barsgl.ejb.integr.dict.BaseDictionaryController;
import ru.rbt.barsgl.ejb.repository.dict.AccType.AccDealsRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.AccDealsWrapper;

import javax.inject.Inject;

import java.util.Date;

import static java.lang.String.format;

/**
 * Created by er22317 on 17.07.2017.
 */
public class AccDealsController extends BaseDictionaryController<AccDealsWrapper, String, GlAccDeals, AccDealsRepository> {
    @Inject
    private AccDealsRepository repository;

    @Override
    public RpcRes_Base<AccDealsWrapper> create(AccDealsWrapper wrapper) {
        return create(wrapper, repository, GlAccDeals.class, wrapper.getAcc2(),
                format("Настройка для '%s' уже существует", wrapper.getAcc2()),
                format("Создана настройка для: '%s'", wrapper.getAcc2()),
                format("Ошибка при создании настройки: '%s'", wrapper.getAcc2()),
                () ->  new GlAccDeals(wrapper.getAcc2(), wrapper.getFlagOff(), wrapper.getUsrr()));
    }

    @Override
    public RpcRes_Base<AccDealsWrapper> update(AccDealsWrapper wrapper) {
        String primaryKey = wrapper.getAcc2();
        return update(wrapper,repository, GlAccDeals.class,
                primaryKey,
                format("Настройка '%s' не найдена", primaryKey),
                format("Изменена настройка: '%s'", primaryKey),
                format("Ошибка при изменении настройки: '%s'", primaryKey),
                param -> {
                    param.setFlag_off(wrapper.getFlagOff());
                    param.setDtm(new Date());
                    param.setUsrm(wrapper.getUsrm());
                });
    }

    @Override
    public RpcRes_Base<AccDealsWrapper> delete(AccDealsWrapper wrapper) {
        String primaryKey = wrapper.getAcc2();
        return delete(wrapper, repository, GlAccDeals.class, primaryKey,
                format("Настройка '%s' не найдена", primaryKey),
                format("Удалена настройка: '%s'", primaryKey),
                format("Ошибка при удалении настройки: '%s'", primaryKey)
        );
    }
}
