/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.ejb.integr.dict;

import ru.rbt.barsgl.ejb.integr.dict.AccType.*;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.*;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.Serializable;

/**
 *
 * @author Andrew Samsonov
 */
@Stateless
@LocalBean
public class ManualDictionaryService {

  @EJB
  private DepartmentController departmentController;

  @EJB
  private AccountigTypeController accountigTypeController;

  @EJB
  private SourcesDealsController sourcesDealsController;

  @EJB
  private TypesOfTermsController typesOfTermsController;

  @EJB
  private PropertyTypeController propertyTypeController;

  @Inject
  private StamtUnloadParamsController stamtUnloadParamsController;

  @Inject
  private OperationTemplateController operationTemplateController;

  @Inject
  private ProfitCenterController profitCenterController;

  @Inject
  private AccTypeSectionController accTypeSectionController;

  @Inject
  private AccTypeProductController accTypeProductController;

  @Inject
  private AccTypeSubProductController accTypeSubProductController;

  @Inject
  private AccTypeModifierController accTypeModifiertController;

  @Inject
  private AccTypeController accTypeController;

  @Inject
  private ActParmController actParmController;

  @Inject
  private AcodController acodController;

  public RpcRes_Base<CodeNameWrapper> saveDepartment(CodeNameWrapper wrapper, FormAction action) {
    return save(departmentController, wrapper, action);
  }

  public RpcRes_Base<CodeNameWrapper> saveAccountigType(CodeNameWrapper wrapper, FormAction action) {
    return save(accountigTypeController, wrapper, action);
  }

  public RpcRes_Base<ExtCodeNameWrapper> saveSourcesDeals(ExtCodeNameWrapper wrapper, FormAction action) {
    return save(sourcesDealsController, wrapper, action);

  }

  public RpcRes_Base<CodeNameWrapper> saveTypesOfTerms(CodeNameWrapper wrapper, FormAction action) {
    return save(typesOfTermsController, wrapper, action);
  }

  public RpcRes_Base<PropertyTypeWrapper> savePropertyType(PropertyTypeWrapper wrapper, FormAction action) {
    return save(propertyTypeController, wrapper, action);
  }
  
  public RpcRes_Base<StamtUnloadParamWrapper> saveStamtUnloadParam(StamtUnloadParamWrapper wrapper, FormAction action) {
    return save(stamtUnloadParamsController, wrapper, action);
  }

  public RpcRes_Base<ManualOperationWrapper> saveOperationTemplate(ManualOperationWrapper wrapper, FormAction action) {
    return save(operationTemplateController, wrapper, action);
  }

  public RpcRes_Base<ProfitCenterWrapper> saveProfitCenter(ProfitCenterWrapper wrapper, FormAction action) {
    return save(profitCenterController, wrapper, action);
  }

  public RpcRes_Base<AccTypeSectionWrapper> saveAccTypeSection(AccTypeSectionWrapper wrapper, FormAction action) {
    return save(accTypeSectionController, wrapper, action);
  }

  public RpcRes_Base<AccTypeProductWrapper> saveAccTypeProduct(AccTypeProductWrapper wrapper, FormAction action) {
    return save(accTypeProductController, wrapper, action);
  }

  public RpcRes_Base<AccTypeSubProductWrapper> saveAccTypeSubProduct(AccTypeSubProductWrapper wrapper, FormAction action) {
    return save(accTypeSubProductController, wrapper, action);
  }

  public RpcRes_Base<AccTypeModifierWrapper> saveAccTypeModifier(AccTypeModifierWrapper wrapper, FormAction action) {
    return save(accTypeModifiertController, wrapper, action);
  }

  public RpcRes_Base<AccTypeWrapper> saveAccType(AccTypeWrapper wrapper, FormAction action) {
    return save(accTypeController, wrapper, action);
  }

  public RpcRes_Base<ActParmWrapper> saveActParm(ActParmWrapper wrapper, FormAction action) {
    return save(actParmController, wrapper, action);
  }

  public RpcRes_Base<AcodWrapper> saveAcod(AcodWrapper wrapper, FormAction action) {
    return save(acodController, wrapper, action);
  }

  private <T extends Serializable> RpcRes_Base<T> save(DictionaryController controller, T wrapper, FormAction action) {
    switch (action) {
      case CREATE:
        return controller.create(wrapper);
      case UPDATE:
        return controller.update(wrapper);
      case DELETE:
        return controller.delete(wrapper);
      default:
        return new RpcRes_Base<>(wrapper, true, "Unknown action!");
    }
  }
}
