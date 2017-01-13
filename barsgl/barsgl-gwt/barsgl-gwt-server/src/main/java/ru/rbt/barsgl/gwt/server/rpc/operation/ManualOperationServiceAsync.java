package ru.rbt.barsgl.gwt.server.rpc.operation;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

/**
 * Created by ER18837 on 19.08.15.
 */
public interface ManualOperationServiceAsync {
    void processOperationRq(ManualOperationWrapper wrapper, AsyncCallback<RpcRes_Base<ManualOperationWrapper>> callback);
    void processPackageRq(ManualOperationWrapper wrapper, AsyncCallback<RpcRes_Base<ManualOperationWrapper>> callback);

    void updatePostings(ManualOperationWrapper wrapper, AsyncCallback<RpcRes_Base<ManualOperationWrapper>> callback);
    void suppressPostings(ManualOperationWrapper wrapper, AsyncCallback<RpcRes_Base<ManualOperationWrapper>> callback);

    void saveAccount(ManualAccountWrapper wrapper, AsyncCallback<RpcRes_Base<ManualAccountWrapper>> callback);
    void updateAccount(ManualAccountWrapper wrapper, AsyncCallback<RpcRes_Base<ManualAccountWrapper>> callback);
    void closeAccount(ManualAccountWrapper wrapper, AsyncCallback<RpcRes_Base<ManualAccountWrapper>> callback);
    void saveOfrAccount(ManualAccountWrapper wrapper, AsyncCallback<RpcRes_Base<ManualAccountWrapper>> callback);
    void getOfrAccountParameters(ManualAccountWrapper wrapper, AsyncCallback<RpcRes_Base<ManualAccountWrapper>> callback);
    void savePlAccount(ManualAccountWrapper wrapper, AsyncCallback<RpcRes_Base<ManualAccountWrapper>> callback);
}
