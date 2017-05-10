package ru.rbt.barsgl.ejb.integr.dict.AccType;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.dict.AccType.*;
import ru.rbt.barsgl.ejb.entity.dict.AccountingType;
import ru.rbt.barsgl.ejb.integr.dict.BaseDictionaryController;
import ru.rbt.barsgl.ejb.repository.dict.AccType.*;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.ejbcore.security.RequestContextBean;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.ctx.UserRequestHolder;
import ru.rbt.barsgl.shared.dict.AccTypeWrapper;
import ru.rbt.barsgl.shared.enums.AccLogTarget;
import ru.rbt.barsgl.shared.enums.BoolType;
import ru.rbt.barsgl.shared.enums.LogRowAction;

import javax.inject.Inject;

import java.util.Date;

import static java.lang.String.format;
import static ru.rbt.barsgl.shared.ExceptionUtils.getErrorMessage;

/**
 * Created by akichigi on 24.08.16.
 */
public class AccTypeController extends BaseDictionaryController<AccTypeWrapper, String, AccountingType, AccTypeRepository> {
    @Inject
    private AccTypeRepository accTypeRepository;

    @Inject
    private ActParmRepository actParmRepository;

    @Inject
    private ActLogRepository actLogRepository;

    @Inject
    private RequestContextBean contextBean;

    @Inject
    private OperdayController operdayController;

    @Inject
    private AccTypeSectionRepository sectionRepository;

    @Inject
    private AccTypeProductRepository productRepository;

    @Inject
    private AccTypeSubProductRepository subProductRepository;

    @Inject
    private AccTypeModifierRepository modifierRepository;

    @Override
    public RpcRes_Base<AccTypeWrapper> create(AccTypeWrapper wrapper) {
        String primaryKey = wrapper.getAcctype();
        if(accTypeRepository.isAccountingTypeCodeExists(primaryKey)){
            return new RpcRes_Base<>(wrapper, true, format("AccType c кодом '%s' уже существует!", primaryKey));
        }

        if(accTypeRepository.isAccountingTypeNameExists(wrapper.getAcctypeName())){
            return new RpcRes_Base<>(wrapper, true, format("AccType c наименованием '%s' уже существует!", wrapper.getAcctypeName()));
        }

        return create(wrapper, accTypeRepository, AccountingType.class, primaryKey,
                format("AccType '%s' уже существует!", primaryKey),
                format("Создан AccType: '%s'", primaryKey),
                format("Ошибка при создании AccType: '%s'", primaryKey),
                () -> new AccountingType(primaryKey, wrapper.getAcctypeName(),
                        wrapper.getPl_act() == BoolType.Y ? YesNo.Y : YesNo.N,
                        wrapper.getFl_ctrl() == BoolType.Y ? YesNo.Y : YesNo.N,
                        wrapper.getTech_act() == BoolType.Y ? YesNo.Y : YesNo.N));
    }

    @Override
    public RpcRes_Base<AccTypeWrapper> update(AccTypeWrapper wrapper) {
        String primaryKey = wrapper.getAcctype();

        if(accTypeRepository.isAccountingTypeExists(primaryKey, wrapper.getAcctypeName())){
            return new RpcRes_Base<>(wrapper, true, format("AccType c наименованием '%s' уже существует!", wrapper.getAcctypeName()));
        }

        return update(wrapper, accTypeRepository, AccountingType.class,
                primaryKey,
                format("AccType '%s' не найден!", primaryKey),
                format("Изменен AccType: '%s'", primaryKey),
                format("Ошибка при изменении AccType: '%s'", primaryKey),
                accType -> {
                    accType.setAccountName(wrapper.getAcctypeName());
                    accType.setBarsAllowed(wrapper.getPl_act() == BoolType.Y ? YesNo.Y : YesNo.N);
                    accType.setCheckedAccount(wrapper.getFl_ctrl() == BoolType.Y ? YesNo.Y : YesNo.N);
                    accType.setTechAct(wrapper.getTech_act() == BoolType.Y ? YesNo.Y : YesNo.N);
                });
    }

    @Override
    public RpcRes_Base<AccTypeWrapper> delete(AccTypeWrapper wrapper) {
        String primaryKey = wrapper.getAcctype();

        if (accTypeRepository.isAccTypeInAcc(primaryKey)){
            return new RpcRes_Base<>(wrapper, true, format("Ошибка удаления AccType. Существуют счета по AccType '%s'!", primaryKey));
        }

        if (actParmRepository.isAccTypeExists(primaryKey)){
            return new RpcRes_Base<>(wrapper, true, format("Ошибка удаления AccType. Существуют параметры счета по AccType '%s'!", primaryKey));
        }

        return delete(wrapper, accTypeRepository, AccountingType.class,
                primaryKey,
                format("AccType '%s' не найден!", primaryKey),
                format("Удален AccType: '%s'", primaryKey),
                format("Ошибка при удалении AccType: '%s'", primaryKey)
        );
    }

    private String getUserAut(){
        UserRequestHolder requestHolder = contextBean.getRequest().orElse(UserRequestHolder.empty());
        return requestHolder.getUser();
    }

    private Date getNow(){
        return operdayController.getSystemDateTime();
    }

   private void accTypeLog(AccountingType entity, LogRowAction action){
       ActLog log = new ActLog(entity.getId(), entity.getAccountName(), entity.getBarsAllowed(),
               entity.getCheckedAccount(), getUserAut(), action, AccLogTarget.ACTNAME, getNow());
       actLogRepository.save(log);
   }

    @Override
    public void beforeCreate(AccountingType entity){
        accTypeLog(entity, LogRowAction.I);
    }

    @Override
    public void beforeUpdate(AccountingType entity){
        accTypeLog(entity, LogRowAction.U);
    }

    @Override
    public void beforeDelete(AccountingType entity){
        accTypeLog(entity, LogRowAction.D);
    }


    public RpcRes_Base<AccTypeWrapper> checkAccType(AccTypeWrapper wrapper){
        try{
            String s, err = "";
            Section section = sectionRepository.getSection(s = wrapper.getSection());
            if (section == null) {
                wrapper.setSection(null);
                err += format("Раздел '%s' не существует!\n", s);
            } else{
                wrapper.setSectionName(section.getName());
            }


            Product product = productRepository.getProduct(wrapper.getSection(), s = wrapper.getProduct());
            if (product == null) {
                wrapper.setProduct(null);
                err += format("Продукт '%s' не существует!\n", s);
            } else{
                wrapper.setProductName(product.getName());
            }

            SubProduct subProduct = subProductRepository.getSubProduct(wrapper.getSection(), wrapper.getProduct(),
                   s = wrapper.getSubProduct());
            if (subProduct == null) {
                wrapper.setSubProduct(null);
                err += format("Подпродукт '%s' не существует!\n", s);
            } else {
                wrapper.setSubProductName(subProduct.getName());
            }

            Modifier modifier = modifierRepository.getModifier(wrapper.getSection(), wrapper.getProduct(),
                    wrapper.getSubProduct(), s = wrapper.getModifier());
            if (modifier == null) {
                wrapper.setModifier(null);
                err += format("Модификатор '%s' не существует!\n", s);
            } else{
                wrapper.setModifierName(modifier.getName());
            }

            if(accTypeRepository.isAccountingTypeCodeExists(s = wrapper.getAcctype())){
                wrapper.setAcctype(null);
                err += format("AccType c кодом '%s' уже существует!", s);
            }

            if(accTypeRepository.isAccountingTypeNameExists(s = wrapper.getAcctypeName())){
                wrapper.setAcctypeName(null);
                err += format("AccType c наименованием '%s' уже существует!", s);
            }
           if (!err.trim().isEmpty()) throw new Exception(err.trim());

            return new RpcRes_Base<AccTypeWrapper>(wrapper, false, "");
        }catch (Exception e) {
            String errMessage = getErrorMessage(e);
            return new RpcRes_Base<AccTypeWrapper>(wrapper, true, errMessage);
        }
    }
}
