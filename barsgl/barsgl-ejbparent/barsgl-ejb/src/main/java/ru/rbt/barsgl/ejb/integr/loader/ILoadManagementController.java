package ru.rbt.barsgl.ejb.integr.loader;

import ru.rbt.barsgl.ejb.integr.dict.DictionaryController;
import ru.rbt.barsgl.shared.enums.LoadManagementStatus;
import ru.rbt.shared.enums.Repository;
import ru.rbt.barsgl.shared.loader.LoadStepWrapper;

import java.io.Serializable;
import java.util.List;

/**
 * Created by SotnikovAV on 31.10.2016.
 */
public interface ILoadManagementController<T extends Serializable> extends DictionaryController<T> {

    /**
     * Обновить статус
     * @param orderId - идентификатор набора действий над шагами загрузки
     * @param status - статус
     * @return список измененных записей
     */
    List<LoadStepWrapper> updateStatus(Repository repository, Long orderId, LoadManagementStatus status) throws Exception;

}
