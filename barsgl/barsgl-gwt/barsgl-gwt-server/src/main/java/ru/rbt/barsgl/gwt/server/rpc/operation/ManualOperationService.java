package ru.rbt.barsgl.gwt.server.rpc.operation;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.barsgl.shared.enums.ErrorCorrectType;
import ru.rbt.barsgl.shared.operation.BackValueWrapper;
import ru.rbt.barsgl.shared.operation.CardReportWrapper;
import ru.rbt.barsgl.shared.operation.CurExchangeWrapper;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.barsgl.shared.operation.ManualTechOperationWrapper;

import java.util.List;

/**
 * Created by ER18837 on 19.08.15.
 */
@RemoteServiceRelativePath("service/ManualOperationService")
public interface ManualOperationService  extends RemoteService {

    // авторизация ручных операций и пакетов
    RpcRes_Base<ManualOperationWrapper> processOperationRq(ManualOperationWrapper wrapper) throws Exception;
    RpcRes_Base<ManualOperationWrapper> processPackageRq(ManualOperationWrapper wrapper) throws Exception;
    RpcRes_Base<CurExchangeWrapper> exchangeCurrency(CurExchangeWrapper wrapper) throws Exception;

    // авторизация операций backvalue
    RpcRes_Base<Integer> processOperationBv(BackValueWrapper wrapper) throws Exception;

    // редактирование проводок
    RpcRes_Base<ManualOperationWrapper> updatePostings(ManualOperationWrapper wrapper) throws Exception;
    RpcRes_Base<ManualOperationWrapper> suppressPostings(ManualOperationWrapper wrapper) throws Exception;

    // Операции по техническим счетам
    RpcRes_Base<ManualTechOperationWrapper> updateTechPostings(ManualTechOperationWrapper wrapper) throws Exception;
    RpcRes_Base<ManualTechOperationWrapper> suppressPdTh(ManualTechOperationWrapper wrapper) throws Exception;
    RpcRes_Base<ManualTechOperationWrapper> processTechOperationRq(ManualTechOperationWrapper wrapper) throws Exception;
    RpcRes_Base<ManualTechOperationWrapper> saveTechOperation(ManualTechOperationWrapper wrapper) throws Exception;
    RpcRes_Base<ManualTechOperationWrapper> updateTechOperation(ManualTechOperationWrapper wrapper) throws Exception;

    // переобработка ошибок
    RpcRes_Base<Integer> correctErrors (List<Long> errorIdList, String comment, String idPstCorr, ErrorCorrectType type) throws Exception;

    // отчет по backvalue
    RpcRes_Base<Boolean>  operExists(String date, String limit) throws Exception;

    // отчет по картотеки
    RpcRes_Base<CardReportWrapper> getCardReport(CardReportWrapper wrapper) throws Exception;

}
