package ru.rbt.barsgl.gwt.server.rpc.operation;

import ru.rbt.barsgl.ejb.cur_exchng.CurrencyExchangeSupport;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountService;
import ru.rbt.barsgl.ejb.integr.acc.OfrAccountService;
import ru.rbt.barsgl.ejb.integr.bg.BatchPackageController;
import ru.rbt.barsgl.ejb.integr.bg.EditPostingController;
import ru.rbt.barsgl.ejb.integr.bg.ManualPostingController;
import ru.rbt.barsgl.ejb.integr.bg.ReprocessPostingService;
import ru.rbt.barsgl.gwt.server.rpc.AbstractGwtService;
import ru.rbt.barsgl.gwt.server.rpc.RpcResProcessor;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.barsgl.shared.enums.ErrorCorrectType;
import ru.rbt.barsgl.shared.operation.CurExchangeWrapper;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import java.util.List;

/**
 * Created by ER18837 on 19.08.15.
 */
public class ManualOperationServiceImpl extends AbstractGwtService implements ManualOperationService {

    @Override
    public RpcRes_Base<ManualOperationWrapper> processOperationRq(final ManualOperationWrapper wrapper) throws Exception {
        return new RpcResProcessor<ManualOperationWrapper>() {
            @Override
            public RpcRes_Base<ManualOperationWrapper> buildResponse() throws Throwable {
                RpcRes_Base<ManualOperationWrapper> res = localInvoker.invoke(ManualPostingController.class, "processOperationRq", wrapper);
                if (res == null) throw new Throwable("Не удалось обработать запрос на операцию");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<ManualOperationWrapper> processPackageRq(final ManualOperationWrapper wrapper) throws Exception {
        return new RpcResProcessor<ManualOperationWrapper>() {
            @Override
            public RpcRes_Base<ManualOperationWrapper> buildResponse() throws Throwable {
                RpcRes_Base<ManualOperationWrapper> res = localInvoker.invoke(BatchPackageController.class, "processPackageRq", wrapper);
                if (res == null) throw new Throwable("Не удалось обработать пакет");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<ManualOperationWrapper> updatePostings(final ManualOperationWrapper wrapper) throws Exception {
        return new RpcResProcessor<ManualOperationWrapper>() {
            @Override
            public RpcRes_Base<ManualOperationWrapper> buildResponse() throws Throwable {
                RpcRes_Base<ManualOperationWrapper> res = localInvoker.invoke(EditPostingController.class, "updatePostingsWrapper", wrapper);
                if (res == null) throw new Throwable("Не удалось изменить операцию");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<ManualOperationWrapper> suppressPostings(final ManualOperationWrapper wrapper) throws Exception {
        return new RpcResProcessor<ManualOperationWrapper>() {
            @Override
            public RpcRes_Base<ManualOperationWrapper> buildResponse() throws Throwable {
                RpcRes_Base<ManualOperationWrapper> res = localInvoker.invoke(EditPostingController.class, "suppressPostingsWrapper", wrapper);
                if (res == null) throw new Throwable("Не удалось изменить операцию");
                return res;
            }
        }.process();
    }

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
    public RpcRes_Base<ManualAccountWrapper> saveOfrAccount(final ManualAccountWrapper wrapper) throws Exception {
        return new RpcResProcessor<ManualAccountWrapper>() {
            @Override
            public RpcRes_Base<ManualAccountWrapper> buildResponse() throws Throwable {
                RpcRes_Base<ManualAccountWrapper> res = localInvoker.invoke(OfrAccountService.class, "createOfrManualAccount", wrapper);
                if (res == null) throw new Throwable("Не удалось сохранить счет");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<ManualAccountWrapper> getOfrAccountParameters(final ManualAccountWrapper wrapper) throws Exception {
        return new RpcResProcessor<ManualAccountWrapper>() {
            @Override
            public RpcRes_Base<ManualAccountWrapper> buildResponse() throws Throwable {
                RpcRes_Base<ManualAccountWrapper> res = localInvoker.invoke(OfrAccountService.class, "getOfrAccountParameters", wrapper);
                if (res == null) throw new Throwable("Не удалось получить параметры счета");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<CurExchangeWrapper> exchangeCurrency(final CurExchangeWrapper wrapper) throws Exception {
        return new RpcResProcessor<CurExchangeWrapper>(){

            @Override
            protected RpcRes_Base<CurExchangeWrapper> buildResponse() throws Throwable {
                RpcRes_Base<CurExchangeWrapper> res = localInvoker.invoke(CurrencyExchangeSupport.class, "exchange", wrapper);
                if (res == null) throw new Throwable(Utils.Fmt("Не удалось выполнить пересчет суммы из {0} в {1}",
                        wrapper.getSourceCurrency(), wrapper.getTargetCurrency()));
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<Integer> correctErrors(List<Long> errorIdList, String comment, String idPstCorr, ErrorCorrectType type) throws Exception {
        return new RpcResProcessor<Integer>() {
            @Override
            public RpcRes_Base<Integer> buildResponse() throws Throwable {
                RpcRes_Base<Integer> res = localInvoker.invoke(ReprocessPostingService.class, "correctErrors",
                        errorIdList, comment, idPstCorr, type);
                if (res == null) throw new Throwable("Не удалось скорректировать ошибки");
                return res;
            }
        }.process();
    }
}
