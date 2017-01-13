package ru.rbt.barsgl.ejb.integr.dict;

import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.dict.OperationTemplate;
import ru.rbt.barsgl.ejb.repository.BankCurrencyRepository;
import ru.rbt.barsgl.ejb.repository.dict.OperationTemplateRepository;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.ejbcore.security.RequestContextBean;
import ru.rbt.barsgl.ejbcore.util.StringUtils;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.ctx.UserRequestHolder;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import javax.inject.Inject;
import java.util.ArrayList;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.substr;

/**
 * Created by ER18837 on 17.03.16.
 */
public class OperationTemplateController extends BaseDictionaryController<ManualOperationWrapper, Long, OperationTemplate, OperationTemplateRepository> {

    @Inject
    private OperationTemplateRepository repository;

    @Inject
    private BankCurrencyRepository bankCurrencyRepository;

    @Inject
    private RequestContextBean contextBean;

    @Override
    public RpcRes_Base<ManualOperationWrapper> create(ManualOperationWrapper wrapper) {
        String errorMessage = validate(wrapper);
        if(!isEmpty(errorMessage)){
            return new RpcRes_Base<>(wrapper, true, errorMessage);
        }

        return create(wrapper, repository, OperationTemplate.class, wrapper.getId(),
                format("Шаблон операции '%d' уже существует", wrapper.getId()),
                format("Создан шаблон операции: '%s'", wrapper.getTemplateName()),
                format("Ошибка при создании шаблон операции: '%s'", wrapper.getTemplateName()),
                () ->  createTemplate(wrapper));
    }

    @Override
    public RpcRes_Base<ManualOperationWrapper> update(ManualOperationWrapper wrapper) {
        String errorMessage = validate(wrapper);
        if(!isEmpty(errorMessage)){
            return new RpcRes_Base<>(wrapper, true, errorMessage);
        }

        return update(wrapper, repository, OperationTemplate.class, wrapper.getId(),
                format("Шаблон операции '%d' не найден", wrapper.getId()),
                format("Изменен шаблон операции: '%s'", wrapper.getTemplateName()),
                format("Ошибка при удалении шаблон операции: '%s'", wrapper.getTemplateName()),
                operationTemplate ->  updateTemplate(operationTemplate, wrapper));
    }

    @Override
    public RpcRes_Base<ManualOperationWrapper> delete(ManualOperationWrapper wrapper) {
        return delete(wrapper, repository, OperationTemplate.class, wrapper.getId(),
                format("Шаблон операции '%d' не найден", wrapper.getId()),
                format("Удален шаблон операции: '%s'", wrapper.getTemplateName()),
                format("Ошибка при изменении шаблон операции: '%s'", wrapper.getTemplateName()));
    }

    OperationTemplate createTemplate(ManualOperationWrapper wrapper) {
        OperationTemplate template = new OperationTemplate();
        fillTemplate(template, wrapper);
        return template;
    }

    OperationTemplate updateTemplate(OperationTemplate template, ManualOperationWrapper wrapper) {
        fillTemplate(template, wrapper);
        return template;
    }

    void fillTemplate(OperationTemplate template, ManualOperationWrapper wrapper) {
        // название, тип, источник
        template.setTemplateName(wrapper.getTemplateName());
        template.setTemplateType(wrapper.isExtended() ? OperationTemplate.TemplateType.E : OperationTemplate.TemplateType.S);
        template.setIsSystem(wrapper.isSystem() ? YesNo.Y : YesNo.N);
        template.setSourcePosting(wrapper.getDealSrc());

        // Дебет
        template.setAccountDebit(wrapper.getAccountDebit());
        BankCurrency ccyDebit = bankCurrencyRepository.getCurrency(wrapper.getCurrencyDebit());
        template.setCurrencyDebit(ccyDebit);
        template.setAmountDebit(wrapper.getAmountDebit());
        template.setFilialDebit(wrapper.getFilialDebit());

        // Кредит
        template.setAccountCredit(wrapper.getAccountCredit());
        BankCurrency ccyCredit = bankCurrencyRepository.getCurrency(wrapper.getCurrencyCredit());
        template.setCurrencyCredit(ccyCredit);
        template.setAmountCredit(wrapper.getAmountCredit());
        template.setFilialCredit(wrapper.getFilialCredit());

        // Описание
        template.setNarrative(wrapper.getNarrative());
        template.setRusNarrativeLong(wrapper.getRusNarrativeLong());
        template.setProfitCenter(wrapper.getProfitCenter());
        template.setDeptId(StringUtils.trim(wrapper.getDeptId()));

        // USER
        final UserRequestHolder requestHolder = contextBean.getRequest().orElse(UserRequestHolder.empty());
        String userName = requestHolder.getUser();
        template.setUserName(userName);
    }

    String validate(ManualOperationWrapper wrapper) {
        ArrayList<String> errorList = new ArrayList<String>();
        try {
            String name = wrapper.getTemplateName();
            if (isEmpty(name))
                errorList.add("Не задано наименование шаблона");
            if (isEmpty(wrapper.getDealSrc()))
                errorList.add("Не задан источник сделки");

            if (repository.checkTemplateNameExists(name, wrapper.getId()))
                errorList.add("Шаблон с таким именем уже существует!");

            String accountDebit = wrapper.getAccountDebit();
            String accountCredit = wrapper.getAccountCredit();
            if (isEmpty(accountDebit) && isEmpty(accountCredit))
                errorList.add("Должен быть задан хотя бы один счет");

            checkAccount(accountDebit, wrapper.getCurrencyDebit(), wrapper.getFilialDebit(), "по дебету", errorList);
            checkAccount(accountCredit, wrapper.getCurrencyCredit(), wrapper.getFilialCredit(), "по кредиту", errorList);
        } catch (Exception e) {
            errorList.add(e.getMessage());
        }
        return StringUtils.listToString(errorList, "\n");
    }

    private void checkAccount(String account, String ccy, String filial, String side, ArrayList<String> errorList) {
        if (isEmpty(account))
            return;

        String acc2 = substr(account, 5);
        int pos = acc2.indexOf("%");
        if (pos >= 0)
            acc2 = substr(acc2, pos);
        if (acc2.length() < 5)
            acc2 = acc2 + "%";
        if (!repository.checkAccount2Exists(acc2)) {
            errorList.add(format("Балансовый счет второго порядка по маске '%s' не существует", acc2));
            return;
        }

        if (account.length() != 20 || account.contains("%") || account.contains("_"))
            return;

        DataRecord res = repository.getAccountParams(account);
        if (null == res) {
            errorList.add(format("Не найден счет %s: '%s'", account, side));
            return;
        }

        String accFilial = res.getString("CBCC");
        String accCcy = res.getString("CCY");
        if (!accFilial.equals(filial) || !accCcy.equals(ccy)) {
            errorList.add(format("Заданы неверные атрибуты счета %s '%s': нужна валюта '%s', филиал '%s'",
                    account, side, accCcy, accFilial));
        }

    }
}
