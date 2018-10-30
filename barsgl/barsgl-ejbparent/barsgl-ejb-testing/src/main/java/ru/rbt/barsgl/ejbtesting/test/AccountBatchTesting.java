package ru.rbt.barsgl.ejbtesting.test;

import ru.rbt.barsgl.ejb.controller.excel.AccountBatchProcessorBean;
import ru.rbt.barsgl.ejb.integr.acc.AccountBatchController;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.operation.AccountBatchWrapper;

import javax.inject.Inject;
import java.io.File;
import java.util.Map;

/**
 * Created by er18837 on 30.10.2018.
 */
public class AccountBatchTesting extends ManualAuthTesting {

    @Inject
    AccountBatchProcessorBean batchProcessor;

    @Inject
    AccountBatchController batchController;

    public String loadPackage(File file, Map<String, String> params) throws Exception {
        try {
            String userIdStr = params.get("userid");
            Long userId = userIdStr == null ? null : Long.valueOf(userIdStr);
            logon(userId);
            String res = batchProcessor.processMessage(file, params);
            return res;
        } finally {
            logoff();
        }
    }

    public RpcRes_Base<AccountBatchWrapper> processAccountBatchRq(AccountBatchWrapper wrapper) throws Exception {
        try {
            logon(wrapper.getUserId());
            RpcRes_Base<AccountBatchWrapper> res = batchController.processAccountBatchRq(wrapper);
            return res;
        } finally {
            logoff();
        }
    }

    public RpcRes_Base<AccountBatchWrapper> authorizePackage(AccountBatchWrapper wrapper) throws Exception {
        try {
            logon(wrapper.getUserId());
            RpcRes_Base<AccountBatchWrapper> res = batchController.authorizePackage(wrapper);
            return res;
        } finally {
            logoff();
        }
    }

    public RpcRes_Base<AccountBatchWrapper> deletePackage(AccountBatchWrapper wrapper) throws Exception {
        try {
            logon(wrapper.getUserId());
            RpcRes_Base<AccountBatchWrapper> res = batchController.deletePackage(wrapper);
            return res;
        } finally {
            logoff();
        }
    }
}
