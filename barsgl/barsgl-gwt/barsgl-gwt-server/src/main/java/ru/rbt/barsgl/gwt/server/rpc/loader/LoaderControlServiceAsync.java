package ru.rbt.barsgl.gwt.server.rpc.loader;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.Repository;
import ru.rbt.barsgl.shared.loader.LoadStepWrapper;

import java.util.List;

/**
 * Created by SotnikovAV on 24.10.2016.
 */
public interface LoaderControlServiceAsync {

    /**
     * Сохранить действие для шага загрузки
     *
     * @param cnw - шаг загрузки
     * @param action - действие формы
     * @param callback - обработчик завершения действия формы
     */
    void saveLoadStepAction(LoadStepWrapper cnw, FormAction action, AsyncCallback<RpcRes_Base<LoadStepWrapper>> callback);

    /**
     * Назначить действия на шаги загрузки
     * @param orderId - идентификатор набора действий
     * @param callback - обработчик завершения метода
     */
     void assignActions(Repository repository, Long orderId, AsyncCallback<List<LoadStepWrapper>> callback);

    /**
     * Согласовать действия, назначенные на шаги загрузки
     * @param orderId - идентификатор набора действий
     * @param callback - обработчик завершения метода
     */
    void approveActions(Repository repository, Long orderId, AsyncCallback<List<LoadStepWrapper>> callback);

    /**
     * Выполнить действия, назначенные на шаги загрузки
     * @param orderId - идентификатор набора действий
     * @param callback - обработчик завершения метода
     */
    void executeActions(Repository repository, Long orderId, AsyncCallback<List<LoadStepWrapper>> callback);

    /**
     * Отменить действия, назначенные на шаги загрузки
     * @param orderId - идентификатор набора действий
     * @param callback - обработчик завершения метода
     */
    void cancelActions(Repository repository, Long orderId, AsyncCallback<List<LoadStepWrapper>> callback);

    /**
     * Удалить действия, назначенные на шаги загрузки
     * @param orderId - идентификатор набора действий
     * @param callback - обработчик завершения метода
     */
    void deleteActions(Repository repository, Long orderId, AsyncCallback<List<LoadStepWrapper>> callback);

    /**
     * Удалить действия, назначенные на шаги загрузки
     * @param actionId - идентификатор набора действий
     * @param callback - обработчик завершения метода
     */
    void deleteAction(Repository repository, Long actionId, AsyncCallback<List<LoadStepWrapper>> callback);

}
