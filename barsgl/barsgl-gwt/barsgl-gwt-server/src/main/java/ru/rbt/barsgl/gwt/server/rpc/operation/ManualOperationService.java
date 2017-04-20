package ru.rbt.barsgl.gwt.server.rpc.operation;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.barsgl.shared.enums.ErrorCorrectType;
import ru.rbt.barsgl.shared.operation.CurExchangeWrapper;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import java.util.List;

/**
 * Created by ER18837 on 19.08.15.
 */
@RemoteServiceRelativePath("service/ManualOperationService")
public interface ManualOperationService  extends RemoteService {
    RpcRes_Base<ManualOperationWrapper> processOperationRq(ManualOperationWrapper wrapper) throws Exception;
    RpcRes_Base<ManualOperationWrapper> processPackageRq(ManualOperationWrapper wrapper) throws Exception;

    RpcRes_Base<ManualOperationWrapper> updatePostings(ManualOperationWrapper wrapper) throws Exception;
    RpcRes_Base<ManualOperationWrapper> suppressPostings(ManualOperationWrapper wrapper) throws Exception;

    RpcRes_Base<ManualAccountWrapper> saveAccount(ManualAccountWrapper wrapper) throws Exception;
    RpcRes_Base<ManualAccountWrapper> updateAccount(ManualAccountWrapper wrapper) throws Exception;
    RpcRes_Base<ManualAccountWrapper> closeAccount(ManualAccountWrapper wrapper) throws Exception;
    RpcRes_Base<ManualAccountWrapper> saveOfrAccount(ManualAccountWrapper wrapper) throws Exception;
    RpcRes_Base<ManualAccountWrapper> getOfrAccountParameters(ManualAccountWrapper wrapper) throws Exception;
    RpcRes_Base<ManualAccountWrapper> savePlAccount(ManualAccountWrapper wrapper) throws Exception;

    RpcRes_Base<CurExchangeWrapper> exchangeCurrency(CurExchangeWrapper wrapper) throws Exception;

    RpcRes_Base<Integer> correctErrors (List<Long> errorIdList, String comment, String idPstCorr, ErrorCorrectType type) throws Exception;

    RpcRes_Base<Boolean>  operExists(String date) throws Exception;
}
