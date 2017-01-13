package ru.rbt.barsgl.ejb.integr.dict;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.dict.ProfitCenter;
import ru.rbt.barsgl.ejb.repository.dict.ProfitCenterRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.ProfitCenterWrapper;
import ru.rbt.barsgl.shared.enums.BoolType;

import javax.inject.Inject;
import java.util.Date;

import static java.lang.String.format;

/**
 * Created by akichigi on 04.08.16.
 */
public class ProfitCenterController extends BaseDictionaryController<ProfitCenterWrapper, String, ProfitCenter, ProfitCenterRepository> {
    @Inject
    private OperdayController operdayController;
    @Inject
    private ProfitCenterRepository repository;

    @Override
    public RpcRes_Base<ProfitCenterWrapper> create(ProfitCenterWrapper wrapper) {
        if(repository.isProfitCenterExists(wrapper.getCode())){
            return new RpcRes_Base<>(wrapper, true, format("Профит центр c кодом '%s' уже существует!", wrapper.getCode()));
        }

        Date sysDate = operdayController.getSystemDateTime();

        String primaryKey = wrapper.getCode();
        return create(wrapper, repository, ProfitCenter.class, primaryKey,
                format("Профит центр '%s' уже существует!", primaryKey),
                format("Создан профит центр: '%s'", primaryKey),
                format("Ошибка при создании профит центра: '%s'", primaryKey),
                () -> new ProfitCenter(wrapper.getCode(), wrapper.getName(), sysDate,
                        wrapper.getClosed() == BoolType.N ? null : BoolType.Y));
    }

    @Override
    public RpcRes_Base<ProfitCenterWrapper> update(ProfitCenterWrapper wrapper) {
        if(repository.isProfitCenterExists(wrapper.getCode(), wrapper.getName())){
            return new RpcRes_Base<>(wrapper, true, format("Профит центр c наименованием '%s' уже существует!", wrapper.getName()));
        }

        Date sysDate = operdayController.getSystemDateTime();
        String primaryKey = wrapper.getCode();

        return update(wrapper, repository, ProfitCenter.class,
                primaryKey,
                format("Профит центр '%s' не найден!", primaryKey),
                format("Изменен профит центр: '%s'", primaryKey),
                format("Ошибка при изменении профит центра: '%s'", primaryKey),
                profitCenter -> {
                    profitCenter.setName(wrapper.getName());
                    profitCenter.setDate(sysDate);
                    profitCenter.setClosed(wrapper.getClosed() == BoolType.N ? null : BoolType.Y);
                });
    }

    @Override
    public RpcRes_Base<ProfitCenterWrapper> delete(ProfitCenterWrapper wrapper) {
        String primaryKey = wrapper.getCode();

        return delete(wrapper, repository, ProfitCenter.class, primaryKey,
                format("Профит центр '%s' не найден!", primaryKey),
                format("Удален профит центр: '%s'", primaryKey),
                format("Ошибка при удалении профит центра: '%s'", primaryKey)
        );
    }
}
