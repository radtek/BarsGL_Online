package ru.rbt.barsgl.ejb.integr.dict.AccType;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.dict.AccType.ActLog;
import ru.rbt.barsgl.ejb.entity.dict.AccType.ActParm;
import ru.rbt.barsgl.ejb.entity.dict.AccType.ActParmId;
import ru.rbt.barsgl.ejb.entity.dict.AccountingType;
import ru.rbt.barsgl.ejb.entity.dict.Acod;
import ru.rbt.barsgl.ejb.integr.dict.BaseDictionaryController;
import ru.rbt.barsgl.ejb.repository.dict.AccType.AccTypeRepository;
import ru.rbt.barsgl.ejb.repository.dict.AccType.ActLogRepository;
import ru.rbt.barsgl.ejb.repository.dict.AccType.ActParmRepository;
import ru.rbt.barsgl.ejb.repository.dict.AcodRepository;
import ru.rbt.barsgl.ejb.repository.dict.PropertyTypeRepository;
import ru.rbt.barsgl.ejb.repository.dict.TypesOfTermsRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.ActParmWrapper;
import ru.rbt.barsgl.shared.enums.AccLogTarget;
import ru.rbt.barsgl.shared.enums.LogRowAction;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.shared.ctx.UserRequestHolder;
import ru.rbt.shared.security.RequestContext;

import javax.inject.Inject;
import java.text.ParseException;
import java.util.Date;

import static java.lang.String.format;

/**
 * Created by akichigi on 25.08.16.
 */
public class ActParmController extends BaseDictionaryController<ActParmWrapper, ActParmId, ActParm, ActParmRepository> {
    @Inject
    private AccTypeRepository accTypeRepository;

    @Inject
    private PropertyTypeRepository propertyTypeRepository;

    @Inject
    private TypesOfTermsRepository typesOfTermsRepository;

    @Inject
    private ActParmRepository actParmRepository;

    @Inject
    private DateUtils dateUtils;

    @Inject
    private ActLogRepository actLogRepository;

    @Inject
    private RequestContext contextBean;

    @Inject
    private OperdayController operdayController;

    @Inject
    private AcodRepository acodRepository;

    @Override
    public RpcRes_Base<ActParmWrapper> create(ActParmWrapper wrapper) {
        if (!accTypeRepository.isAccountingTypeCodeExists(wrapper.getAccType())) {
            return new RpcRes_Base<>(wrapper, true, format("AccType c кодом '%s' не существует!", wrapper.getAccType()));
        }

        if (!("00".equals(wrapper.getCusType()) || propertyTypeRepository.isPropertyTypeExists(Short.parseShort(wrapper.getCusType())))) {
            return new RpcRes_Base<>(wrapper, true, format("Тип собственности c кодом '%s' не существует!", wrapper.getCusType()));
        }

        if (!typesOfTermsRepository.isTermExists(wrapper.getTerm())) {
            return new RpcRes_Base<>(wrapper, true, format("Код срока '%s' не существует!", wrapper.getTerm()));
        }

        if (!accTypeRepository.isAccTypeTechAct(wrapper.getAccType())) {
            if (!actParmRepository.isAcc2Exists(wrapper.getAcc2())) {
                return new RpcRes_Base<>(wrapper, true, format("Б/счет 2-го порядка '%s' не существует!", wrapper.getAcc2()));
            }
        }

        if (accTypeRepository.isAccTypePL_ACT_yes(wrapper.getAccType()) &&
           (!wrapper.getAcc2().startsWith("706") && !wrapper.getAcc2().startsWith("707"))){
            return new RpcRes_Base<>(wrapper, true, "Для AccType установлен признак доходов/расходов!");
        }

        if ((wrapper.getPlcode() != null) && (!wrapper.getPlcode().isEmpty()) && (!actParmRepository.isPlCodeExists(wrapper))) {
            return new RpcRes_Base<>(wrapper, true, format("Символ доходов/расходов '%s' не существует или не действует на период действия параметра!", wrapper.getPlcode()));
        }

        if ((wrapper.getAcod() != null) && (!wrapper.getAcod().isEmpty()) && (!actParmRepository.isAcodeExists(wrapper.getAcod()))) {
            return new RpcRes_Base<>(wrapper, true, format("ACOD '%s' не существует!", wrapper.getAcod()));
        }

        if ((wrapper.getAcod() != null) && (!wrapper.getAcod().isEmpty()) && (!actParmRepository.allowUseAcodMT1000(wrapper.getAcod()))) {
            return new RpcRes_Base<>(wrapper, true, format("ACOD '%s' типа 'not used'. Для его использования добавьте описание в справочнике!", wrapper.getAcod()));
        }

            Date data_end;
            try {
                data_end = dateUtils.onlyDateParse(wrapper.getDte());
            } catch (ParseException e) {
                return new RpcRes_Base<>(wrapper, true, format("Ошибка при создании параметров счета. Ошибка преобразования формата даты конца '%s'", wrapper.getDte()));
            }

            Date data_begin;
            try {
                data_begin = dateUtils.onlyDateParse(wrapper.getDtb());

                if (actParmRepository.isActParmExists(wrapper)) {
                    return new RpcRes_Base<>(wrapper, true, format("Параметры счета по AccType '%s' уже существуют!", wrapper.getAccType()));
                }

                if (!actParmRepository.isParmDateClosed(wrapper)) {
                    return new RpcRes_Base<>(wrapper, true, format("Найден такой же действующий на '%s' набор параметров по AccType '%s' ! \n"  +
                            "Установите сначала дату окончания действия существующего набора или \n" +
                            "измените дату начала действия вводимых параметров", wrapper.getDtb(), wrapper.getAccType()));
                }
            } catch (ParseException e) {
                return new RpcRes_Base<>(wrapper, true, format("Ошибка при создании параметров счета. Ошибка преобразования формата даты начала '%s'", wrapper.getDtb()));
            }

            ActParmId primaryKey = new ActParmId(wrapper.getAccType(), wrapper.getCusType(), wrapper.getTerm(),
                    wrapper.getAcc2(), data_begin);

            return create(wrapper, actParmRepository, ActParm.class, primaryKey,
                    format("Параметры счета по AccType '%s' уже существует!", primaryKey.getAccType()),
                    format("Созданы параметры счета по AccType: '%s'", primaryKey.getAccType()),
                    format("Ошибка при создании параметров счета по AccType: '%s'", primaryKey.getAccType()),
                    () -> new ActParm(primaryKey, wrapper.getPlcode(), wrapper.getAcod(), wrapper.getAc_sq(), data_end));
    }

    @Override
    public RpcRes_Base<ActParmWrapper> update(ActParmWrapper wrapper) {
        if ((wrapper.getPlcode() != null) && (!wrapper.getPlcode().isEmpty()) && (!actParmRepository.isPlCodeExists(wrapper))){
            return new RpcRes_Base<>(wrapper, true, format("Символ доходов/расходов '%s' не существует или не действует на период действия параметра!", wrapper.getPlcode()));
        }

        if ((wrapper.getAcod() != null) && (!wrapper.getAcod().isEmpty()) && (!actParmRepository.isAcodeExists(wrapper.getAcod()))){
            return new RpcRes_Base<>(wrapper, true, format("ACOD '%s' не существует!", wrapper.getAcod()));
        }

        if ((wrapper.getAcod() != null) && (!wrapper.getAcod().isEmpty()) && (!actParmRepository.allowUseAcodMT1000(wrapper.getAcod()))) {
            return new RpcRes_Base<>(wrapper, true, format("ACOD '%s' типа 'not used'. Для его использования добавьте описание в справочнике!", wrapper.getAcod()));
        }

        Date data_begin;
        try {
            data_begin = dateUtils.onlyDateParse(wrapper.getDtb());
        } catch (ParseException e) {
            return new RpcRes_Base<>(wrapper, true, format("Ошибка изменения параметров счета. Ошибка преобразования формата даты начала '%s'", wrapper.getDtb()));
        }

        Date data_end;
        try{
            data_end = dateUtils.onlyDateParse(wrapper.getDte());
        } catch (ParseException e) {
            return new RpcRes_Base<>(wrapper, true, format("Ошибка изменения параметров счета. Ошибка преобразования формата даты конца '%s'", wrapper.getDte()));
        }

        ActParmId primaryKey = new ActParmId(wrapper.getAccType(), wrapper.getCusType(), wrapper.getTerm(),
                wrapper.getAcc2(), data_begin);

        return update(wrapper, actParmRepository, ActParm.class,
                primaryKey,
                format("Параметры счета по AccType '%s' не найден!", primaryKey.getAccType()),
                format("Изменены параметры счета по AccType: '%s'", primaryKey.getAccType()),
                format("Ошибка при изменении параметров счета по AccType: '%s'", primaryKey.getAccType()),
                actParm -> {
                    actParm.setPlcode(wrapper.getPlcode());
                    actParm.setAcod(wrapper.getAcod());
                    actParm.setAc_sq(wrapper.getAc_sq());
                    actParm.setDte(data_end);
                });
    }

    @Override
    public RpcRes_Base<ActParmWrapper> delete(ActParmWrapper wrapper) {
        ActParmId primaryKey;
        try {
            primaryKey = new ActParmId(wrapper.getAccType(), wrapper.getCusType(), wrapper.getTerm(),
                    wrapper.getAcc2(), dateUtils.onlyDateParse(wrapper.getDtb()));
        } catch (ParseException e) {
            return new RpcRes_Base<>(wrapper, true, format("Ошибка удаления. Ошибка преобразования формата даты начала '%s'", wrapper.getDtb()));
        }

        if (actParmRepository.isActParmInAcc(wrapper)){
            return new RpcRes_Base<>(wrapper, true, format("Ошибка удаления. Существуют счета по данным параметрам AccType '%s'!", wrapper.getAccType()));
        }
        return delete(wrapper, actParmRepository, ActParm.class,
                primaryKey,
                format("Параметры счета по AccType '%s' не найден!", primaryKey.getAccType()),
                format("Удалены параметры счета по AccType: '%s'", primaryKey.getAccType()),
                format("Ошибка при удалении параметров счета по AccType: '%s'", primaryKey.getAccType())
        );
    }

    private String getUserAut(){
        UserRequestHolder requestHolder = contextBean.getRequest().orElse(UserRequestHolder.empty());
        return requestHolder.getUser();
    }

    private Date getNow(){
        return operdayController.getSystemDateTime();
    }

    private void accTypeLog(ActParm entity, LogRowAction action){
        ActLog log = new ActLog(entity.getId().getAccType(), entity.getId().getCusType(),
                                entity.getId().getTerm(), entity.getId().getAcc2(),
                                entity.getPlcode(), entity.getAcod(), entity.getAc_sq(),
                                entity.getId().getDtb(), entity.getDte(), getUserAut(),
                                action, AccLogTarget.ACTPARM, getNow());
        actLogRepository.save(log);
    }

    private void changeAcodParams(ActParm entity){
        Acod res = acodRepository.getNotUsedAcod(entity.getAcod());
        if ((res == null) || (Long.parseLong(entity.getAcod()) > 999)) return;

        res.setAcc2dscr(entity.getId().getAcc2());
        res.setSqdscr(entity.getAc_sq());
        res.setEname(entity.getId().getAccType());
        res.setType(actParmRepository.getPsav(entity.getId().getAcc2()));
        AccountingType at = accTypeRepository.getAccType(entity.getId().getAccType());
        res.setRname(at == null ? null : at.getAccountName());

        acodRepository.save(res, true);
    }


    @Override
    public void beforeCreate(ActParm entity){
        accTypeLog(entity, LogRowAction.I);
    }

    @Override
    public void afterCreate(ActParm entity){
        changeAcodParams(entity);
    }

    @Override
    public void beforeUpdate(ActParm entity){
        accTypeLog(entity, LogRowAction.U);
    }

    @Override
    public void afterUpdate(ActParm entity){
        changeAcodParams(entity);
    }

    @Override
    public void beforeDelete(ActParm entity){
        accTypeLog(entity, LogRowAction.D);
    }
}
