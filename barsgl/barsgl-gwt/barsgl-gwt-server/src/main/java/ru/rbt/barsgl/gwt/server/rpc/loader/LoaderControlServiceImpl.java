package ru.rbt.barsgl.gwt.server.rpc.loader;

import ru.rbt.barsgl.gwt.core.server.rpc.AbstractGwtService;
import ru.rbt.barsgl.gwt.core.server.rpc.RpcResProcessor;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.Repository;
import ru.rbt.barsgl.shared.loader.LoadStepWrapper;

import java.util.List;

/**
 * Created by SotnikovAV on 24.10.2016.
 */
public class LoaderControlServiceImpl extends AbstractGwtService implements LoaderControlService {

    @Override
    public RpcRes_Base<LoadStepWrapper> saveLoadStepAction(LoadStepWrapper wrapper, FormAction action) throws Exception {
        return new RpcResProcessor<LoadStepWrapper>() {
            @Override
            public RpcRes_Base<LoadStepWrapper> buildResponse() throws Throwable {
                RpcRes_Base<LoadStepWrapper> res = localInvoker.invoke(ru.rbt.barsgl.ejb.integr.loader.LoadManagementService.class, "saveAction", wrapper, action);
                if (res == null) {
                    throw new Exception("Не удалось сохранить запись управления шагом загрузки");
                }
                return res;
            }
        }.process();
    }

    @Override
    public List<LoadStepWrapper> assignActions(Repository repository, Long orderId) throws Exception {
        List<LoadStepWrapper> res = localInvoker.invoke(ru.rbt.barsgl.ejb.integr.loader.LoadManagementService.class, "assignActions", repository, orderId);
        if (res == null) {
            throw new Exception("Не удалось назначить действия на шаги загрузки");
        }
        return res;
    }

    @Override
    public List<LoadStepWrapper> approveActions(Repository repository, Long orderId) throws Exception {
        List<LoadStepWrapper> res = localInvoker.invoke(ru.rbt.barsgl.ejb.integr.loader.LoadManagementService.class, "approveActions", repository, orderId);
        if (res == null) {
            throw new Exception("Не удалось согласовать действия на шаги загрузки");
        }
        return res;
    }

    @Override
    public List<LoadStepWrapper> executeActions(Repository repository, Long orderId) throws Exception {
        List<LoadStepWrapper> res = localInvoker.invoke(ru.rbt.barsgl.ejb.integr.loader.LoadManagementService.class, "executeActions", repository, orderId);
        if (res == null) {
            throw new Exception("Не удалось выполнить действия на шаги загрузки");
        }
        return res;
    }

    @Override
    public List<LoadStepWrapper> cancelActions(Repository repository, Long orderId) throws Exception {
        List<LoadStepWrapper> res = localInvoker.invoke(ru.rbt.barsgl.ejb.integr.loader.LoadManagementService.class, "cancelActions", repository, orderId);
        if (res == null) {
            throw new Exception("Не удалось отменить действия на шаги загрузки");
        }
        return res;
    }

    @Override
    public List<LoadStepWrapper> deleteActions(Repository repository, Long orderId) throws Exception {
        return localInvoker.invoke(ru.rbt.barsgl.ejb.integr.loader.LoadManagementService.class, "deleteActions", repository, orderId);
    }

    @Override
    public List<LoadStepWrapper> deleteAction(Repository repository, Long actionId) throws Exception {
        return localInvoker.invoke(ru.rbt.barsgl.ejb.integr.loader.LoadManagementService.class, "deleteAction", repository, actionId);
    }
}
