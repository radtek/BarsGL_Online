package ru.rbt.barsgl.ejb.integr.loader;

import ru.rbt.barsgl.ejb.entity.loader.LoadManagement;
import ru.rbt.barsgl.ejb.repository.WorkdayRepository;
import ru.rbt.barsgl.ejb.repository.WorkprocRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.LoadManagementAction;
import ru.rbt.barsgl.shared.enums.LoadManagementStatus;
import ru.rbt.shared.enums.Repository;
import ru.rbt.barsgl.shared.loader.LoadStepWrapper;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by SotnikovAV on 28.10.2016.
 */
@Stateless
@LocalBean
public class LoadManagementService {

    @EJB
    private LoadManagementController loadManagementController;

    /**
     * Сохранить действие для шага загрузки
     * @param wrapper - строка управления шагом загрузки
     * @param action - действие формы
     * @return результат
     */
    public RpcRes_Base<LoadStepWrapper> saveAction(LoadStepWrapper wrapper, FormAction action) throws Exception {
        long id = wrapper.getId();
        if(0L == id) {
            return loadManagementController.create(wrapper);
        } else {
            return loadManagementController.update(wrapper);
        }
    }

    /**
     * Назначить действия на шаги загрузки
     * @param orderId - идентификатор набора действий
     * @return список измененных записей
     * @throws Exception
     */
    public List<LoadStepWrapper> assignActions(Repository repository, Long orderId) throws Exception {
        return loadManagementController.updateStatus(repository, orderId, LoadManagementStatus.Assigned);
    }

    /**
     * Согласовать действия, назначенные на шаги загрузки
     * @param orderId - идентификатор набора действий
     * @return список измененных записей
     * @throws Exception
     */
    public List<LoadStepWrapper> approveActions(Repository repository, Long orderId) throws Exception {
        return loadManagementController.updateStatus(repository, orderId, LoadManagementStatus.Approved);
    }

    /**
     * Выполнить действия, назначенные на шаги загрузки
     * @param orderId - идентификатор набора действий
     * @return список измененных записей
     * @throws Exception
     */
    public List<LoadStepWrapper> executeActions(Repository repository, Long orderId) throws Exception {
        return loadManagementController.updateStatus(repository, orderId, LoadManagementStatus.Executed);
    }

    /**
     * Отменить действия, назначенные на шаги загрузки
     * @param orderId - идентификатор набора действий
     * @return список измененных записей
     * @throws Exception
     */
    public List<LoadStepWrapper> cancelActions(Repository repository, Long orderId) throws Exception {
        return loadManagementController.updateStatus(repository, orderId, LoadManagementStatus.None);
    }

    public List<LoadStepWrapper> deleteActions(Repository repository, Long orderId) throws Exception {
        return loadManagementController.deleteActions(repository, orderId);
    }

    public List<LoadStepWrapper> deleteAction(Repository repository, Long actionId) throws Exception {
        return loadManagementController.deleteAction(repository, actionId);
    }
}
