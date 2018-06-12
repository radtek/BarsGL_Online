package ru.rbt.barsgl.gwt.server.rpc.operation;

import ru.rbt.barsgl.ejb.cur_exchng.CurrencyExchangeSupport;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountService;
import ru.rbt.barsgl.ejb.integr.bg.*;
import ru.rbt.barsgl.ejb.integr.bg.BatchPackageController;
import ru.rbt.barsgl.ejb.integr.bg.EditPostingController;
import ru.rbt.barsgl.ejb.integr.bg.ManualPostingController;
import ru.rbt.barsgl.ejb.integr.bg.ReprocessPostingService;
import ru.rbt.barsgl.ejb.rep.PostingBackValueRep;
import ru.rbt.barsgl.ejb.rep.WaitCloseAccountsRep;
import ru.rbt.barsgl.gwt.core.server.rpc.AbstractGwtService;
import ru.rbt.barsgl.gwt.core.server.rpc.RpcResProcessor;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.barsgl.shared.enums.ErrorCorrectType;
import ru.rbt.barsgl.shared.operation.BackValueWrapper;
import ru.rbt.barsgl.shared.operation.CardReportWrapper;
import ru.rbt.barsgl.shared.operation.CurExchangeWrapper;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.barsgl.shared.operation.ManualTechOperationWrapper;

import java.util.ArrayList;
import java.util.List;

import static ru.rbt.barsgl.gwt.core.utils.WhereClauseBuilder.filterCriteriaAdapter;

/**
 * Created by ER18837 on 19.08.15.
 */
public class ManualOperationServiceImpl extends AbstractGwtService implements ManualOperationService {

    @Override
    public RpcRes_Base<CardReportWrapper> getCardReport(CardReportWrapper wrapper) throws Exception {
        return new RpcResProcessor<CardReportWrapper>() {
            @Override
            public RpcRes_Base<CardReportWrapper> buildResponse() throws Throwable {
                RpcRes_Base<CardReportWrapper> res = localInvoker.invoke(CardReportController.class, "getCardReport", wrapper);
                if (res == null) throw new Throwable("Не удалось создать отчет по картотеке");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<Integer> processOperationBv(final BackValueWrapper wrapper) throws Exception {
        return new RpcResProcessor<Integer>() {
            @Override
            public RpcRes_Base<Integer> buildResponse() throws Throwable {
                RpcRes_Base<Integer> res = localInvoker.invoke(BackValuePostingController.class, "processOperationBv", wrapper,
                        filterCriteriaAdapter(wrapper.getFilters()));
                if (res == null) throw new Throwable("Не удалось обработать запрос на операцию");
                return res;
            }
        }.process();
    }

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
    public RpcRes_Base<ManualTechOperationWrapper> processTechOperationRq(final ManualTechOperationWrapper wrapper) throws Exception {
        return new RpcResProcessor<ManualTechOperationWrapper>() {
            @Override
            public RpcRes_Base<ManualTechOperationWrapper> buildResponse() throws Throwable {
                RpcRes_Base<ManualTechOperationWrapper> res = localInvoker.invoke(ManualTechOperationController.class, "processOperationRq", wrapper);
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
    public RpcRes_Base<ManualTechOperationWrapper> saveTechOperation(ManualTechOperationWrapper wrapper) throws Exception {
        return null;
    }

    @Override
    public RpcRes_Base<ManualTechOperationWrapper> updateTechOperation(ManualTechOperationWrapper wrapper) throws Exception {
        return new RpcResProcessor<ManualTechOperationWrapper>() {
            @Override
            public RpcRes_Base<ManualTechOperationWrapper> buildResponse() throws Throwable {
                RpcRes_Base<ManualTechOperationWrapper> res = localInvoker.invoke(ManualTechOperationController.class, "updateTechOperation", wrapper);
                if (res == null) throw new Throwable("Не удалось обработать пакет");
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
    public RpcRes_Base<ManualTechOperationWrapper> suppressPdTh(final ManualTechOperationWrapper wrapper) throws Exception {
        return new RpcResProcessor<ManualTechOperationWrapper>() {
            @Override
            public RpcRes_Base<ManualTechOperationWrapper> buildResponse() throws Throwable {
                RpcRes_Base<ManualTechOperationWrapper> res = localInvoker.invoke(EditPdThController.class, "suppressPostingsWrapper", wrapper);
                if (res == null) throw new Throwable("Не удалось изменить операцию");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<ManualAccountWrapper> findAccount(ManualAccountWrapper wrapper) throws Exception {
        return new RpcResProcessor<ManualAccountWrapper>() {
            @Override
            public RpcRes_Base<ManualAccountWrapper> buildResponse() throws Throwable {
                RpcRes_Base<ManualAccountWrapper> res = localInvoker.invoke(GLAccountService.class, "findManualAccount", wrapper);
                if (res == null) throw new Throwable("Не удалось изменить счет");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<ManualTechOperationWrapper> updateTechPostings(final ManualTechOperationWrapper wrapper) throws Exception {
        return new RpcResProcessor<ManualTechOperationWrapper>() {
            @Override
            public RpcRes_Base<ManualTechOperationWrapper> buildResponse() throws Throwable {
                RpcRes_Base<ManualTechOperationWrapper> res = localInvoker.invoke(EditPdThController.class, "updatePdThWrapper", wrapper);
                if (res == null) throw new Throwable("Не удалось изменить операцию");
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

    @Override
    public RpcRes_Base<Boolean> operExists(String date, String limit) throws Exception {
        return new RpcResProcessor<Boolean>(){

            @Override
            protected RpcRes_Base<Boolean> buildResponse() throws Throwable {
                RpcRes_Base<Boolean> res = localInvoker.invoke(PostingBackValueRep.class, "operExists", date, limit);
                if (res == null) throw new Throwable("Не удалось проверить наличие данных для отчета");
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

}
