package ru.rbt.barsgl.gwt.server.rpc.operation;

import com.google.gwt.user.client.rpc.AsyncCallback;
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
public interface ManualOperationServiceAsync {

    void processOperationRq(ManualOperationWrapper wrapper, AsyncCallback<RpcRes_Base<ManualOperationWrapper>> callback);
    void processPackageRq(ManualOperationWrapper wrapper, AsyncCallback<RpcRes_Base<ManualOperationWrapper>> callback);
    void exchangeCurrency(CurExchangeWrapper wrapper, AsyncCallback<RpcRes_Base<CurExchangeWrapper>> callback);

    void processOperationBv(BackValueWrapper wrapper, AsyncCallback<RpcRes_Base<Integer>> callback);

    void updatePostings(ManualOperationWrapper wrapper, AsyncCallback<RpcRes_Base<ManualOperationWrapper>> callback);
    void suppressPostings(ManualOperationWrapper wrapper, AsyncCallback<RpcRes_Base<ManualOperationWrapper>> callback);

    //Операции по техническим счетам
    void updateTechPostings(ManualTechOperationWrapper wrapper, AsyncCallback<RpcRes_Base<ManualTechOperationWrapper>> callback);
    void suppressPdTh(ManualTechOperationWrapper wrapper,AsyncCallback<RpcRes_Base<ManualTechOperationWrapper>> callback);
    void processTechOperationRq(ManualTechOperationWrapper wrapper, AsyncCallback<RpcRes_Base<ManualTechOperationWrapper>> callback);
    void saveTechOperation(ManualTechOperationWrapper wrapper, AsyncCallback<RpcRes_Base<ManualTechOperationWrapper>> callback);
    void updateTechOperation(ManualTechOperationWrapper wrapper, AsyncCallback<RpcRes_Base<ManualTechOperationWrapper>> callback);

    void correctErrors (List<Long> errorIdList, String comment, String idPstCorr, ErrorCorrectType type, AsyncCallback<RpcRes_Base<Integer>> callback);

    void operExists(String date, String limit, AsyncCallback<RpcRes_Base<Boolean>> callback);
    void getCardReport(CardReportWrapper wrapper, AsyncCallback<RpcRes_Base<CardReportWrapper>> callback );

}
