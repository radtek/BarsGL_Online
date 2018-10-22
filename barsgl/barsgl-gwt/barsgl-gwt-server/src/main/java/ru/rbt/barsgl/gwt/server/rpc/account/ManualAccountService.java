package ru.rbt.barsgl.gwt.server.rpc.account;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.barsgl.shared.operation.AccountBatchWrapper;

/**
 * Created by er18837 on 22.10.2018.
 */
@RemoteServiceRelativePath("service/ManualAccountService")
public interface ManualAccountService extends RemoteService {

    // счета ЦБ
    RpcRes_Base<ManualAccountWrapper> saveAccount(ManualAccountWrapper wrapper) throws Exception;
    RpcRes_Base<ManualAccountWrapper> updateAccount(ManualAccountWrapper wrapper) throws Exception;
    RpcRes_Base<ManualAccountWrapper> closeAccount(ManualAccountWrapper wrapper) throws Exception;
    RpcRes_Base<ManualAccountWrapper> savePlAccount(ManualAccountWrapper wrapper) throws Exception;

    // технические счета
    RpcRes_Base<ManualAccountWrapper> saveTechAccount(ManualAccountWrapper wrapper) throws Exception;
    RpcRes_Base<ManualAccountWrapper> updateTechAccount(ManualAccountWrapper wrapper) throws Exception;
    RpcRes_Base<ManualAccountWrapper> closeTechAccount(ManualAccountWrapper wrapper) throws Exception;
    RpcRes_Base<ManualAccountWrapper> findTechAccount(ManualAccountWrapper wrapper) throws Exception;

    // отчет по закрытым счетам
    RpcRes_Base<Boolean> repWaitAcc(String begDate, String endDate, Boolean isAllAcc) throws Exception;

    // пакеты счетов
    RpcRes_Base<AccountBatchWrapper> processAccountBatchRq(AccountBatchWrapper wrapper) throws Exception;
}
