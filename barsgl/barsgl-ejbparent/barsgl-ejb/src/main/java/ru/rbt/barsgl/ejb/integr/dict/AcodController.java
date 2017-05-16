package ru.rbt.barsgl.ejb.integr.dict;

import ru.rbt.barsgl.ejb.entity.dict.Acod;
import ru.rbt.barsgl.ejb.repository.dict.AcodRepository;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.AcodWrapper;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.FreeAcod;
import static ru.rbt.shared.ExceptionUtils.getErrorMessage;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by akichigi on 24.10.16.
 */
public class AcodController extends BaseDictionaryController<AcodWrapper, Long, Acod, AcodRepository> {
    @Inject
    private AcodRepository acodRepository;

    @Override
    public RpcRes_Base<AcodWrapper> create(AcodWrapper wrapper) {
        if(null != acodRepository.getNotUsedAcod(wrapper.getAcod())){
            return new RpcRes_Base<>(wrapper, true, format("Существует свободный Acod c кодом '%s'!", wrapper.getAcod()));
        }

        Long primaryKey = -1L;
        return create(wrapper, acodRepository, Acod.class, primaryKey,
                format("Acod '%s' уже существует!", primaryKey),
                format("Создан Acod: '%s'", wrapper.getAcod()),
                format("Ошибка при создании Acod: '%s'", wrapper.getAcod()),
                () -> new Acod(wrapper.getAcod(),
                        wrapper.getAcc2dscr(),
                        wrapper.getType(),
                        wrapper.getSqdscr(),
                        wrapper.getEname(),
                        wrapper.getRname()));
    }

    @Override
    public RpcRes_Base<AcodWrapper> update(AcodWrapper wrapper) {
        Long primaryKey = wrapper.getId();

        return update(wrapper, acodRepository, Acod.class,
                primaryKey,
                format("Acod '%s' не найден!", primaryKey),
                format("Изменен Acod: '%s'", wrapper.getAcod()),
                format("Ошибка при изменении Acod: '%s'", wrapper.getAcod()),
                acod -> {
                    acod.setAcod(wrapper.getAcod());
                    acod.setAcc2dscr(wrapper.getAcc2dscr());
                    acod.setType(wrapper.getType());
                    acod.setSqdscr(wrapper.getSqdscr());
                    acod.setEname(wrapper.getEname());
                    acod.setRname(wrapper.getRname());
                });
    }

    @Override
    public RpcRes_Base<AcodWrapper> delete(AcodWrapper wrapper) {
        Long primaryKey = wrapper.getId();

        return delete(wrapper, acodRepository, Acod.class, primaryKey,
                format("Acod '%s' не найден!", primaryKey),
                format("Удален Acod: '%s'", wrapper.getAcod()),
                format("Ошибка при удалении Acod: '%s'", wrapper.getAcod())
        );
    }

    public RpcRes_Base<String> getFreeAcod(){
        try {
               DataRecord rec = acodRepository.selectFirst("select min(acod) as FreeAcod from GL_ACOD where acod < '1000' and upper(ename) = 'NOT USED'");
               if (rec == null) {
                  throw new Exception("Нет свободных Acod");
               }
            String acod = rec.getString("FreeAcod");
            auditController.error(FreeAcod, "Попытка получения свободного Acod", null, format("Выделен Acod: '%s'", acod));
            return new RpcRes_Base<String>(rec.getString("FreeAcod"), false, "");
        } catch (Exception e) {
            String errMessage = getErrorMessage(e);
            auditController.error(FreeAcod, "Попытка получения свободного Acod", null, e);
            return new RpcRes_Base<String>("", true, errMessage);
        }
    }
}
