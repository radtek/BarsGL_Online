/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.gwt.server.rpc.dict;

import ru.rbt.barsgl.ejb.integr.dict.AccType.*;
import ru.rbt.barsgl.ejb.integr.dict.AcodController;
import ru.rbt.barsgl.gwt.core.server.rpc.AbstractGwtService;
import ru.rbt.barsgl.gwt.core.server.rpc.RpcResProcessor;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.*;
import ru.rbt.barsgl.shared.enums.AccTypeParts;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import java.io.Serializable;
import java.util.logging.Logger;

/**
 *
 * @author Andrew Samsonov
 */
public class ManualDictionaryServiceImpl extends AbstractGwtService implements ManualDictionaryService {

  public static final Logger logger = Logger.getLogger(ManualDictionaryServiceImpl.class.getName());

  private <T extends Serializable> RpcRes_Base<T> save(final String methodName, final T wrapper, final FormAction action) throws Exception {
    return new RpcResProcessor<T>() {
      @Override
      public RpcRes_Base<T> buildResponse() throws Throwable {
        RpcRes_Base<T> res = localInvoker.invoke(ru.rbt.barsgl.ejb.integr.dict.ManualDictionaryService.class, methodName, wrapper, action);
        if (res == null) throw new Throwable("Не удалось сохранить операцию");
        return res;
      }
    }.process();
/*
    try {
      RpcRes_Base<T> res = localInvoker.invoke(ru.rbt.barsgl.ejb.integr.dict.ManualDictionaryService.class, methodName, wrapper, action);
      
      if (res == null) {
        throw new Throwable("Ошибка сохранения");
      }else if (res.isError()) {
        throw new Throwable(res.getMessage());
      }
      
      return res;
    } catch (Throwable throwable) {
      logger.log(Level.SEVERE, "Ошибка сохранения", throwable);
      return new RpcRes_Base<>(null, true, getErrorMessage(throwable, SQLException.class, DefaultApplicationException.class));
    }
*/
  }

  @Override
  public RpcRes_Base<CodeNameWrapper> saveDepartment(CodeNameWrapper wrapper, FormAction action) throws Exception {
    return save("saveDepartment", wrapper, action);
  }

  @Override
  public RpcRes_Base<CodeNameWrapper> saveAccountigType(CodeNameWrapper wrapper, FormAction action) throws Exception {
    return save("saveAccountigType", wrapper, action);
  }

  @Override
  public RpcRes_Base<ExtCodeNameWrapper> saveSourcesDeals(ExtCodeNameWrapper wrapper, FormAction action) throws Exception {
    return save("saveSourcesDeals", wrapper, action);
  }

  @Override
  public RpcRes_Base<CodeNameWrapper> saveTypesOfTerms(CodeNameWrapper wrapper, FormAction action) throws Exception {
    return save("saveTypesOfTerms", wrapper, action);
  }

  @Override
  public RpcRes_Base<PropertyTypeWrapper> savePropertyType(PropertyTypeWrapper wrapper, FormAction action) throws Exception {
    return save("savePropertyType", wrapper, action);
  }

  @Override
  public RpcRes_Base<StamtUnloadParamWrapper> saveStamtUnloadParam(StamtUnloadParamWrapper wrapper, FormAction action) throws Exception {
    return save("saveStamtUnloadParam", wrapper, action);
  }

  @Override
  public RpcRes_Base<ManualOperationWrapper> saveOperationTemplate(ManualOperationWrapper wrapper, FormAction action) throws Exception {
    return save("saveOperationTemplate", wrapper, action);
  }

  @Override
  public RpcRes_Base<ProfitCenterWrapper> saveProfitCenter(ProfitCenterWrapper wrapper, FormAction action) throws Exception {
    return save("saveProfitCenter", wrapper, action);
  }

  @Override
  public RpcRes_Base<AccTypeSectionWrapper> saveAccTypeSection(AccTypeSectionWrapper wrapper, FormAction action) throws Exception {
    return save("saveAccTypeSection", wrapper, action);
  }

  @Override
  public RpcRes_Base<AccTypeProductWrapper> saveAccTypeProduct(AccTypeProductWrapper wrapper, FormAction action) throws Exception {
    return save("saveAccTypeProduct", wrapper, action);
  }

  @Override
  public RpcRes_Base<AccTypeSubProductWrapper> saveAccTypeSubProduct(AccTypeSubProductWrapper wrapper, FormAction action) throws Exception {
    return save("saveAccTypeSubProduct", wrapper, action);
  }

  @Override
  public RpcRes_Base<AccTypeModifierWrapper> saveAccTypeModifier(AccTypeModifierWrapper wrapper, FormAction action) throws Exception {
    return save("saveAccTypeModifier", wrapper, action);
  }

  @Override
  public RpcRes_Base<AccTypeWrapper> saveAccType(AccTypeWrapper wrapper, FormAction action) throws Exception {
    return save("saveAccType", wrapper, action);
  }

  @Override
  public RpcRes_Base<ActParmWrapper> saveActParm(ActParmWrapper wrapper, FormAction action) throws Exception {
    return save("saveActParm", wrapper, action);
  }

  @Override
  public RpcRes_Base<AccTypeSourceWrapper> setSrcLinkedToAccType(final AccTypeSourceWrapper wrapper) throws Exception {
    return new RpcResProcessor<AccTypeSourceWrapper>() {
      @Override
      protected RpcRes_Base<AccTypeSourceWrapper> buildResponse() throws Throwable {
        RpcRes_Base<AccTypeSourceWrapper> res = localInvoker.invoke(ActSrcController.class, "setSrcLinkedToAccType", wrapper);
        return res;
      }
    }.process();
  }

  @Override
  public RpcRes_Base<AccTypeSourceWrapper> getSrcLinkedToAccType(final String accType) throws Exception {
    return new RpcResProcessor<AccTypeSourceWrapper>() {
      @Override
      protected RpcRes_Base<AccTypeSourceWrapper> buildResponse() throws Throwable {
        RpcRes_Base<AccTypeSourceWrapper> res = localInvoker.invoke(ActSrcController.class, "getSrcLinkedToAccType", accType);
        return res;
      }
    }.process();
  }

  @Override
  public RpcRes_Base<AccTypeWrapper> checkAccType(final AccTypeWrapper wrapper) throws Exception {
    return new RpcResProcessor<AccTypeWrapper>(){
       @Override
       protected RpcRes_Base<AccTypeWrapper> buildResponse() throws Throwable {
         RpcRes_Base<AccTypeWrapper> res = localInvoker.invoke(AccTypeController.class, "checkAccType", wrapper);
         return res;
       }
    }.process();
  }

    @Override
    public RpcRes_Base<AccTypeWrapper> createAccTypeParts(final AccTypeWrapper wrapper, final AccTypeParts part) throws Exception {
        return new RpcResProcessor<AccTypeWrapper>(){
            @Override
            protected RpcRes_Base<AccTypeWrapper> buildResponse() throws Throwable {
                RpcRes_Base<AccTypeWrapper> res = null;
               switch (part){
                   case section:
                       res = localInvoker.invoke(AccTypeSectionController.class, "create", (AccTypeSectionWrapper) wrapper);
                       break;
                   case product:
                       res = localInvoker.invoke(AccTypeProductController.class, "create", (AccTypeProductWrapper) wrapper);
                       break;
                   case subproduct:
                       res = localInvoker.invoke(AccTypeSubProductController.class, "create", (AccTypeSubProductWrapper) wrapper);
                       break;
                   case modifier:
                       res = localInvoker.invoke(AccTypeModifierController.class, "create", (AccTypeModifierWrapper) wrapper);
                       break;
               }
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<AcodWrapper> saveAcod(AcodWrapper wrapper, FormAction action) throws Exception {
        return save("saveAcod", wrapper, action);
    }

    @Override
    public RpcRes_Base<String> getFreeAcod() throws Exception {
        return new RpcResProcessor<String>() {
            @Override
            protected RpcRes_Base<String> buildResponse() throws Throwable {
                return localInvoker.invoke(AcodController.class, "getFreeAcod");
            }
        }.process();
    }

}
