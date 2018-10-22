package ru.rbt.barsgl.gwt.server.rpc.account;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.ejb.integr.acc.AccountBatchController;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountService;
import ru.rbt.barsgl.ejb.rep.WaitCloseAccountsRep;
import ru.rbt.barsgl.gwt.core.server.rpc.AbstractGwtService;
import ru.rbt.barsgl.gwt.core.server.rpc.RpcResProcessor;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.barsgl.shared.operation.AccountBatchWrapper;

/**
 * Created by er18837 on 22.10.2018.
 */
public class ManualAccountServiceImpl extends AbstractGwtService implements ManualAccountService{

    @Override
    public RpcRes_Base<ManualAccountWrapper> saveAccount(final ManualAccountWrapper wrapper) throws Exception {
        return new RpcResProcessor<ManualAccountWrapper>() {
            @Override
            public RpcRes_Base<ManualAccountWrapper> buildResponse() throws Throwable {
                RpcRes_Base<ManualAccountWrapper> res = localInvoker.invoke(GLAccountService.class, "createManualAccount", wrapper);
                if (res == null) throw new Throwable("Не удалось сохранить счет");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<ManualAccountWrapper> savePlAccount(final ManualAccountWrapper wrapper) throws Exception {
        return new RpcResProcessor<ManualAccountWrapper>() {
            @Override
            public RpcRes_Base<ManualAccountWrapper> buildResponse() throws Throwable {
                RpcRes_Base<ManualAccountWrapper> res = localInvoker.invoke(GLAccountService.class, "createManualPlAccount", wrapper);
                if (res == null) throw new Throwable("Не удалось сохранить счет");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<ManualAccountWrapper> updateAccount(final ManualAccountWrapper wrapper) throws Exception {
        return new RpcResProcessor<ManualAccountWrapper>() {
            @Override
            public RpcRes_Base<ManualAccountWrapper> buildResponse() throws Throwable {
                RpcRes_Base<ManualAccountWrapper> res = localInvoker.invoke(GLAccountService.class, "updateManualAccount", wrapper);
                if (res == null) throw new Throwable("Не удалось изменить счет");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<ManualAccountWrapper> closeAccount(final ManualAccountWrapper wrapper) throws Exception {
        return new RpcResProcessor<ManualAccountWrapper>() {
            @Override
            public RpcRes_Base<ManualAccountWrapper> buildResponse() throws Throwable {
                RpcRes_Base<ManualAccountWrapper> res = localInvoker.invoke(GLAccountService.class, "closeManualAccount", wrapper);
                if (res == null) throw new Throwable("Не удалось изменить дату закрытия счета");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<ManualAccountWrapper> saveTechAccount(ManualAccountWrapper wrapper) throws Exception {
        return new RpcResProcessor<ManualAccountWrapper>() {
            @Override
            public RpcRes_Base<ManualAccountWrapper> buildResponse() throws Throwable {
                RpcRes_Base<ManualAccountWrapper> res = localInvoker.invoke(GLAccountService.class, "createManualAccountTech", wrapper);
                if (res == null) throw new Throwable("Не удалось сохранить счет");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<ManualAccountWrapper> updateTechAccount(ManualAccountWrapper wrapper) throws Exception {
        return new RpcResProcessor<ManualAccountWrapper>() {
            @Override
            public RpcRes_Base<ManualAccountWrapper> buildResponse() throws Throwable {
                RpcRes_Base<ManualAccountWrapper> res = localInvoker.invoke(GLAccountService.class, "updateManualAccountTech", wrapper);
                if (res == null) throw new Throwable("Не удалось изменить технический счёт");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<ManualAccountWrapper> closeTechAccount(ManualAccountWrapper wrapper) throws Exception {
        return new RpcResProcessor<ManualAccountWrapper>() {
            @Override
            public RpcRes_Base<ManualAccountWrapper> buildResponse() throws Throwable {
                RpcRes_Base<ManualAccountWrapper> res = localInvoker.invoke(GLAccountService.class, "closeManualAccountTech", wrapper);
                if (res == null) throw new Throwable("Не удалось изменить технический счёт");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<ManualAccountWrapper> findTechAccount(ManualAccountWrapper wrapper) throws Exception {
        return new RpcResProcessor<ManualAccountWrapper>() {
            @Override
            public RpcRes_Base<ManualAccountWrapper> buildResponse() throws Throwable {
                RpcRes_Base<ManualAccountWrapper> res = localInvoker.invoke(GLAccountService.class, "findManualAccountTech", wrapper);
                if (res == null) throw new Throwable("Не удалось изменить счет");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<Boolean> repWaitAcc(String begDate, String endDate, Boolean isAllAcc) throws Exception {
        return new RpcResProcessor<Boolean>(){

            @Override
            protected RpcRes_Base<Boolean> buildResponse() throws Throwable {
                RpcRes_Base<Boolean> res = localInvoker.invoke(WaitCloseAccountsRep.class, "repWaitAcc", begDate, endDate, isAllAcc);
                if (res == null) throw new Throwable("Не удалось проверить наличие данных для отчета");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<AccountBatchWrapper> processAccountBatchRq(AccountBatchWrapper wrapper) throws Exception {
        return new RpcResProcessor<AccountBatchWrapper>() {
            @Override
            public RpcRes_Base<AccountBatchWrapper> buildResponse() throws Throwable {
                RpcRes_Base<AccountBatchWrapper> res = localInvoker.invoke(AccountBatchController.class, "processAccountBatchRq", wrapper);
                if (res == null) throw new Throwable("Не удалось обработать пакет счетов");
                return res;
            }
        }.process();
    }
}
