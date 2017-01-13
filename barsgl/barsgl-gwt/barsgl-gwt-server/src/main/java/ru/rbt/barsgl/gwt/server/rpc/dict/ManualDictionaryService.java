/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.gwt.server.rpc.dict;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.*;
import ru.rbt.barsgl.shared.enums.AccTypeParts;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

/**
 *
 * @author Andrew Samsonov
 */
@RemoteServiceRelativePath("service/ManualDictionaryService")
public interface ManualDictionaryService extends RemoteService {
    RpcRes_Base<CodeNameWrapper> saveDepartment(CodeNameWrapper wrapper, FormAction action) throws Exception;
    RpcRes_Base<CodeNameWrapper> saveAccountigType(CodeNameWrapper wrapper, FormAction action) throws Exception;
    RpcRes_Base<ExtCodeNameWrapper> saveSourcesDeals(ExtCodeNameWrapper wrapper, FormAction action) throws Exception;
    RpcRes_Base<CodeNameWrapper> saveTypesOfTerms(CodeNameWrapper wrapper, FormAction action) throws Exception;
    RpcRes_Base<PropertyTypeWrapper> savePropertyType(PropertyTypeWrapper cnw, FormAction action) throws Exception;
    RpcRes_Base<StamtUnloadParamWrapper> saveStamtUnloadParam(StamtUnloadParamWrapper wrapper, FormAction action) throws Exception;
    RpcRes_Base<ManualOperationWrapper> saveOperationTemplate(ManualOperationWrapper wrapper, FormAction action) throws Exception;
    RpcRes_Base<ProfitCenterWrapper>  saveProfitCenter(ProfitCenterWrapper wrapper, FormAction action) throws Exception;
    RpcRes_Base<AccTypeSectionWrapper> saveAccTypeSection(AccTypeSectionWrapper wrapper, FormAction action) throws Exception;
    RpcRes_Base<AccTypeProductWrapper> saveAccTypeProduct(AccTypeProductWrapper wrapper, FormAction action) throws Exception;
    RpcRes_Base<AccTypeSubProductWrapper> saveAccTypeSubProduct(AccTypeSubProductWrapper wrapper, FormAction action) throws Exception;
    RpcRes_Base<AccTypeModifierWrapper> saveAccTypeModifier(AccTypeModifierWrapper wrapper, FormAction action) throws Exception;
    RpcRes_Base<AccTypeWrapper> saveAccType(AccTypeWrapper wrapper, FormAction action) throws Exception;
    RpcRes_Base<ActParmWrapper> saveActParm(ActParmWrapper wrapper, FormAction action) throws Exception;
    RpcRes_Base<AccTypeSourceWrapper> setSrcLinkedToAccType(AccTypeSourceWrapper wrapper) throws Exception;
    RpcRes_Base<AccTypeSourceWrapper> getSrcLinkedToAccType(String accType) throws Exception;
    RpcRes_Base<AccTypeWrapper> checkAccType(AccTypeWrapper wrapper) throws Exception;
    RpcRes_Base<AccTypeWrapper> createAccTypeParts(AccTypeWrapper wrapper, AccTypeParts part) throws Exception;
    RpcRes_Base<AcodWrapper> saveAcod(AcodWrapper wrapper, FormAction action) throws Exception;
    RpcRes_Base<String> getFreeAcod() throws Exception;
}
