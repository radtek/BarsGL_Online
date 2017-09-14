package ru.rbt.barsgl.ejb.integr.dict.AccType;

import ru.rbt.barsgl.ejb.entity.dict.AccType.GlAccDeals;
import ru.rbt.barsgl.ejb.entity.dict.StamtUnloadParam;
import ru.rbt.barsgl.ejb.integr.dict.BaseDictionaryController;
import ru.rbt.barsgl.ejb.repository.dict.AccType.AccDealsRepository;
import ru.rbt.barsgl.ejb.repository.dict.AccType.AccTypeRepository;
import ru.rbt.barsgl.ejb.repository.dict.AccType.ActParmRepository;
import ru.rbt.barsgl.ejb.security.UserContext;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.AccDealsWrapper;

import javax.inject.Inject;

import java.util.Date;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Acc2ForDeals;

/**
 * Created by er22317 on 17.07.2017.
 */
public class AccDealsController extends BaseDictionaryController<AccDealsWrapper, String, GlAccDeals, AccDealsRepository> {
    @Inject
    private AccDealsRepository repository;
    @Inject
    UserContext userContext;
    @Inject
    private ActParmRepository actParmRepository;


    @Override
    public RpcRes_Base<AccDealsWrapper> create(AccDealsWrapper wrapper) {
            if (!actParmRepository.isAcc2Exists(wrapper.getAcc2())) {
                return new RpcRes_Base<>(wrapper, true, format("Б/счет 2-го порядка '%s' не существует!", wrapper.getAcc2()));
            }
        RpcRes_Base<AccDealsWrapper> ret = create(wrapper, repository, GlAccDeals.class, wrapper.getAcc2(),
                format("Настройка для '%s' уже существует", wrapper.getAcc2()),
                "",
//                format("Создана настройка для: '%s'", wrapper.getAcc2()),
                format("Ошибка при создании настройки: '%s'", wrapper.getAcc2()),
                () ->  new GlAccDeals(wrapper.getAcc2(), wrapper.getFlagOff()));
        writeLog(wrapper, "Создание в", wrapper.getAcc2());
        return ret;
    }

    @Override
    public RpcRes_Base<AccDealsWrapper> update(AccDealsWrapper wrapper) {
        String primaryKey = wrapper.getAcc2();
        RpcRes_Base<AccDealsWrapper> ret = update(wrapper,repository, GlAccDeals.class,
                primaryKey,
                format("Настройка '%s' не найдена", primaryKey),
                "",
//                format("Изменена настройка: '%s'", primaryKey),
                format("Ошибка при изменении настройки: '%s'", primaryKey),
                param -> {
                    param.setFlag_off(wrapper.getFlagOff());
                    param.setDtm(new Date());
                    param.setUsrm(userContext.getUserName());
                });
        writeLog(wrapper, "Изменение", primaryKey);
        return ret;
    }

    @Override
    public RpcRes_Base<AccDealsWrapper> delete(AccDealsWrapper wrapper) {
        String primaryKey = wrapper.getAcc2();
        RpcRes_Base<AccDealsWrapper> ret = delete(wrapper, repository, GlAccDeals.class, primaryKey,
                format("Настройка '%s' не найдена", primaryKey),
                "",
//                format("Удалена настройка: '%s'", primaryKey),
                format("Ошибка при удалении настройки: '%s'", primaryKey)
        );
        writeLog(wrapper, "Удаление из", primaryKey);
        return ret;
    }

    private void writeLog(AccDealsWrapper wrapper, String title, String primaryKey){
        auditController.info(Acc2ForDeals, String.format("%s GL_ACCDEALS, acc2 = %s, FlagOff = %s", title, wrapper.getAcc2(), wrapper.getFlagOff()), "GL_ACCDEALS", primaryKey);
    }
}
