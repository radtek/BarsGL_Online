package ru.rbt.barsgl.gwt.server.rpc.account;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;

/**
 * Created by er18837 on 22.10.2018.
 */
public interface ManualAccountServiceAsync {

    void saveAccount(ManualAccountWrapper wrapper, AsyncCallback<RpcRes_Base<ManualAccountWrapper>> callback);
    void updateAccount(ManualAccountWrapper wrapper, AsyncCallback<RpcRes_Base<ManualAccountWrapper>> callback);
    void closeAccount(ManualAccountWrapper wrapper, AsyncCallback<RpcRes_Base<ManualAccountWrapper>> callback);
    void savePlAccount(ManualAccountWrapper wrapper, AsyncCallback<RpcRes_Base<ManualAccountWrapper>> callback);

    void saveTechAccount(ManualAccountWrapper wrapper, AsyncCallback<RpcRes_Base<ManualAccountWrapper>> callback);
    void updateTechAccount(ManualAccountWrapper wrapper, AsyncCallback<RpcRes_Base<ManualAccountWrapper>> callback);
    void closeTechAccount(ManualAccountWrapper wrapper, AsyncCallback<RpcRes_Base<ManualAccountWrapper>> callback);
    void findTechAccount(ManualAccountWrapper wrapper, AsyncCallback<RpcRes_Base<ManualAccountWrapper>> callback);

    void repWaitAcc(String begDate, String endDate, Boolean isAllAcc, AsyncCallback<RpcRes_Base<Boolean>> callback);
}
