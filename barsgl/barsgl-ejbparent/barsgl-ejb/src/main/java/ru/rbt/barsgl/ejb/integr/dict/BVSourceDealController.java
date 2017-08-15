package ru.rbt.barsgl.ejb.integr.dict;

import org.apache.commons.lang3.time.DateUtils;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.dict.*;
import ru.rbt.barsgl.ejb.repository.dict.BVSourceDealRepository;
import ru.rbt.barsgl.ejb.repository.dict.SourcesDealsRepository;
import ru.rbt.barsgl.ejb.security.UserContext;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.BVSourceDealWrapper;
import ru.rbt.ejbcore.util.StringUtils;

import javax.ejb.EJB;
import javax.inject.Inject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static java.lang.String.format;
import static ru.rbt.barsgl.shared.enums.DealSource.KondorPlus;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by er18837 on 14.08.2017.
 */
public class BVSourceDealController extends BaseDictionaryController<BVSourceDealWrapper, BVSourceDealId, BVSourceDeal, BVSourceDealRepository>{

    @Inject
    private BVSourceDealRepository repository;

    @Inject
    OperdayController operdayController;

    @EJB
    private SourcesDealsRepository sourcesDealsRepository;

    @Inject
    private UserContext userContext;

    @Inject
    ru.rbt.ejbcore.util.DateUtils dateUtils;

    @Override
    public RpcRes_Base<BVSourceDealWrapper> create(BVSourceDealWrapper wrapper) {
        String errorMessage = validate(wrapper);
        if(!isEmpty(errorMessage)){
            return new RpcRes_Base<>(wrapper, true, errorMessage);
        }

        BVSourceDealId id = new BVSourceDealId(wrapper.getSourceDeal(), wrapper.getStartDate());
        return create(wrapper, repository, BVSourceDeal.class, id,
                format("Настройка глубины BackValue для источника '%d' c даты '%s' уже существует", wrapper.getSourceDeal(), wrapper.getStartDateStr()),
                format("Создана настройка глубины BackValue для источника '%s' c даты '%s'", wrapper.getSourceDeal(), wrapper.getStartDateStr()),
                format("Ошибка при создании настройки глубины BackValue для источника '%s' c даты '%s'", wrapper.getSourceDeal(), wrapper.getStartDateStr()),
                () -> new BVSourceDeal(id, wrapper.getEndDate(), wrapper.getDepth(), userContext.getUserName()));
    }

    @Override
    public void beforeCreate(BVSourceDeal entity) {
        Date prevDate = DateUtils.addDays(entity.getId().getStartDate(), -1);
        repository.closePreviousRecord(entity, prevDate);
    }

    @Override
    public RpcRes_Base<BVSourceDealWrapper> update(BVSourceDealWrapper wrapper) {
        String errorMessage = validate(wrapper);
        if(!isEmpty(errorMessage)){
            return new RpcRes_Base<>(wrapper, true, errorMessage);
        }

        return update(wrapper, repository, BVSourceDeal.class, new BVSourceDealId(wrapper.getSourceDeal(), wrapper.getStartDate()),
                format("Настройка глубины BackValue для источника '%d' c даты '%s' не найдена", wrapper.getSourceDeal(), wrapper.getStartDateStr()),
                format("Изменена настройка глубины BackValue для источника '%s' c даты '%s'", wrapper.getSourceDeal(), wrapper.getStartDateStr()),
                format("Ошибка при изменении настройки глубины BackValue для источника '%s' c даты '%s'", wrapper.getSourceDeal(), wrapper.getStartDateStr()),
                (param) -> {
                    if (wrapper.getStartDate().after(operdayController.getOperday().getCurrentDate())) // глубину можно менять только у будущих настроек
                        param.setShift(wrapper.getDepth());
                    param.setEndDate(wrapper.getEndDate());
                });
    }

    @Override
    public RpcRes_Base<BVSourceDealWrapper> delete(BVSourceDealWrapper wrapper) {
        String parseError = parseDates(wrapper);
        if (!isEmpty(parseError))
            return new RpcRes_Base<>(wrapper, true, parseError);
        if (!wrapper.getStartDate().after(operdayController.getOperday().getCurrentDate()))
            return new RpcRes_Base<>(wrapper, true, "Нельзя удалить настройку глубины BackValue с датой начала <= текущему опердню");;

        return delete(wrapper, repository, BVSourceDeal.class, new BVSourceDealId(wrapper.getSourceDeal(), wrapper.getStartDate()),
                format("Настройка глубины BackValue для источника '%d' c даты '%s' не найдена", wrapper.getSourceDeal(), wrapper.getStartDateStr()),
                format("Удалена настройка глубины BackValue для источника '%s' c даты '%s'", wrapper.getSourceDeal(), wrapper.getStartDateStr()),
                format("Ошибка при удалении настройки глубины BackValue для источника '%s' c даты '%s'", wrapper.getSourceDeal(), wrapper.getStartDateStr())
                );
    }

    private String parseDates(BVSourceDealWrapper wrapper) {
        if (isEmpty(wrapper.getStartDateStr()))
            return "Не задана дата начала действия";

        SimpleDateFormat dateFormat = new SimpleDateFormat(wrapper.getDateFormat());
        try {
            wrapper.setStartDate(dateFormat.parse(wrapper.getStartDateStr()));
            wrapper.setEndDate(isEmpty(wrapper.getEndDateStr()) ? null : dateFormat.parse(wrapper.getEndDateStr()));
            return "";
        } catch (ParseException e) {
            return e.getMessage();
        }
    }

    private String validate(BVSourceDealWrapper wrapper) {
        ArrayList<String> errorList = new ArrayList<String>();
        try {
            String parseError = parseDates(wrapper);
            if (!isEmpty(parseError))
                return parseError;

            SourcesDeals src = sourcesDealsRepository.findCached(wrapper.getSourceDeal());
            if (null == src)
                errorList.add(format("Система-источник '%s' не существует", wrapper.getSourceDeal()));
            else if (KondorPlus.getLabel().equals(wrapper.getSourceDeal()))
                errorList.add(format("Нельзя задать глубину BackValue для системы-источника '%s'", wrapper.getSourceDeal()));

            if (wrapper.getDepth() <= 1)
                errorList.add("Глубина BackValue должна быть > 1");

            if (wrapper.getStartDate().before(operdayController.getOperday().getCurrentDate()))
                errorList.add(format("Дата начала действия настройки '%s' < даты текущего опердня", wrapper.getStartDateStr()));

            if (null != wrapper.getEndDate() && wrapper.getStartDate().before(wrapper.getEndDate()))
                errorList.add(format("Дата начала действия настройки '%s' > даты окончания '%s'", wrapper.getStartDateStr(), wrapper.getEndDateStr()));

            // проверка непересечения периодов - при редактировании
            BVSourceDeal intersected = repository.findIntersectedRecord(wrapper);
            if (null != intersected)
                errorList.add(format("Для источника '%s' найдена настройка с периодом действия с '%s' по '%s', перекрывающим заданный"
                        , wrapper.getSourceDeal(), dateUtils.onlyDateString(intersected.getId().getStartDate())
                        , (null == intersected.getEndDate() ? "не задано" : dateUtils.onlyDateString(intersected.getEndDate()))));
        } catch (Exception e) {
            errorList.add(e.getMessage());
        }
        return StringUtils.listToString(errorList, "\n");
    }

}
