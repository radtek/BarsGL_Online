/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.gwt.server.rpc.dict;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.*;
import ru.rbt.barsgl.shared.enums.AccTypeParts;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

/**
 *
 * @author Andrew Samsonov
 */
public interface ManualDictionaryServiceAsync {
    void saveDepartment(CodeNameWrapper wrapper, FormAction action, AsyncCallback<RpcRes_Base<CodeNameWrapper>> callback) throws Exception;
    void saveAccountigType(CodeNameWrapper wrapper, FormAction action, AsyncCallback<RpcRes_Base<CodeNameWrapper>> callback) throws Exception;
    void saveSourcesDeals(ExtCodeNameWrapper wrapper, FormAction action, AsyncCallback<RpcRes_Base<ExtCodeNameWrapper>> callback) throws Exception;
    void saveBVSourceDeal(BVSourceDealWrapper wrapper, FormAction action, AsyncCallback<RpcRes_Base<BVSourceDealWrapper>> callback);
    void saveClosedReportPeriod(ClosedReportPeriodWrapper wrapper, FormAction action, AsyncCallback<RpcRes_Base<ClosedReportPeriodWrapper>> callback );
    void saveTypesOfTerms(CodeNameWrapper wrapper, FormAction action, AsyncCallback<RpcRes_Base<CodeNameWrapper>> callback) throws Exception;
    void savePropertyType(PropertyTypeWrapper cnw, FormAction action, AsyncCallback<RpcRes_Base<PropertyTypeWrapper>> asyncCallbackImpl) throws Exception;
    void saveStamtUnloadParam(StamtUnloadParamWrapper wrapper, FormAction action, AsyncCallback<RpcRes_Base<StamtUnloadParamWrapper>> asyncCallbackImpl) throws Exception;
    void saveAccDeals(AccDealsWrapper wrapper, FormAction action, AsyncCallback<RpcRes_Base<AccDealsWrapper>> asyncCallbackImpl) throws Exception;
    void saveOperationTemplate(ManualOperationWrapper wrapper, FormAction action, AsyncCallback<RpcRes_Base<ManualOperationWrapper>> callback) throws Exception;
    void saveProfitCenter(ProfitCenterWrapper wrapper, FormAction action, AsyncCallback<RpcRes_Base<ProfitCenterWrapper>> callback) throws Exception;
    void saveAccTypeSection(AccTypeSectionWrapper wrapper, FormAction action, AsyncCallback<RpcRes_Base<AccTypeSectionWrapper>> callback) throws Exception;
    void saveAccTypeProduct(AccTypeProductWrapper wrapper, FormAction action, AsyncCallback<RpcRes_Base<AccTypeProductWrapper>> callback) throws Exception;
    void saveAccTypeSubProduct(AccTypeSubProductWrapper wrapper, FormAction action, AsyncCallback<RpcRes_Base<AccTypeSubProductWrapper>> callback) throws Exception;
    void saveAccTypeModifier(AccTypeModifierWrapper wrapper, FormAction action, AsyncCallback<RpcRes_Base<AccTypeModifierWrapper>> callback) throws Exception;
    void saveAccType(AccTypeWrapper wrapper, FormAction action, AsyncCallback<RpcRes_Base<AccTypeWrapper>> callback) throws Exception;
    void saveActParm(ActParmWrapper wrapper, FormAction action, AsyncCallback<RpcRes_Base<ActParmWrapper>> callback) throws Exception;
    void setSrcLinkedToAccType(AccTypeSourceWrapper wrapper, AsyncCallback<RpcRes_Base<AccTypeSourceWrapper>> callback);
    void getSrcLinkedToAccType(String accType, AsyncCallback<RpcRes_Base<AccTypeSourceWrapper>> callback);
    void checkAccType(AccTypeWrapper wrapper, AsyncCallback<RpcRes_Base<AccTypeWrapper>> callback) throws Exception;
    void createAccTypeParts(AccTypeWrapper wrapper, AccTypeParts part, AsyncCallback<RpcRes_Base<AccTypeWrapper>> callback) throws Exception;
    void saveAcod(AcodWrapper wrapper, FormAction action, AsyncCallback<RpcRes_Base<AcodWrapper>> callback) throws Exception;
    void getFreeAcod(AsyncCallback<RpcRes_Base<String>> callback) throws Exception;
}
