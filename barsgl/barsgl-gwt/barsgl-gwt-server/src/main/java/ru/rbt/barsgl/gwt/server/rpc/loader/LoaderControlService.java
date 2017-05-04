package ru.rbt.barsgl.gwt.server.rpc.loader;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.shared.enums.Repository;
import ru.rbt.barsgl.shared.loader.LoadStepWrapper;

import java.util.List;

/**
 * Created by SotnikovAV on 24.10.2016.
 */
@RemoteServiceRelativePath("service/LoaderControlService")
public interface LoaderControlService extends RemoteService{

    /**
     * Сохранить действие для шага загрузки
     * @param cnw - шаг загрузки
     * @param action - действие формы
     * @return результат выполнения
     * @throws Exception
     */
    RpcRes_Base<LoadStepWrapper> saveLoadStepAction(LoadStepWrapper cnw, FormAction action) throws Exception;

    /**
     * Назначить действия на шаги загрузки
     * @param orderId - идентификатор набора действий
     * @return список измененных записей
     * @throws Exception
     */
    List<LoadStepWrapper> assignActions(Repository repository, Long orderId) throws Exception;

    /**
     * Согласовать действия, назначенные на шаги загрузки
     * @param orderId - идентификатор набора действий
     * @return список измененных записей
     * @throws Exception
     */
    List<LoadStepWrapper> approveActions(Repository repository, Long orderId) throws Exception;

    /**
     * Выполнить действия, назначенные на шаги загрузки
     * @param orderId - идентификатор набора действий
     * @return список измененных записей
     * @throws Exception
     */
    List<LoadStepWrapper> executeActions(Repository repository, Long orderId) throws Exception;

    /**
     * Отменить действия, назначенные на шаги загрузки
     * @param orderId - идентификатор набора действий
     * @return список измененных записей
     * @throws Exception
     */
    List<LoadStepWrapper> cancelActions(Repository repository, Long orderId) throws Exception;

    /**
     * Удалить действия, назначенные на шаги загрузки
     * @param orderId - идентификатор набора действий
     * @throws Exception
     */
    List<LoadStepWrapper> deleteActions(Repository repository, Long orderId) throws Exception;

    /**
     * Удалить действия, назначенные на шаги загрузки
     * @param actionId - идентификатор набора действий
     * @throws Exception
     */
    List<LoadStepWrapper> deleteAction(Repository repository, Long actionId) throws Exception;

}
