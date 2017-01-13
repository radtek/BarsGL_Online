package ru.rbt.barsgl.ejb.integr.loader;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.entity.loader.LoadManagement;
import ru.rbt.barsgl.ejb.integr.dict.BaseDictionaryController;
import ru.rbt.barsgl.ejb.repository.loader.LoadManagementRepository;
import ru.rbt.barsgl.ejb.security.UserContext;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.LoadManagementAction;
import ru.rbt.barsgl.shared.enums.LoadManagementStatus;
import ru.rbt.barsgl.shared.enums.Repository;
import ru.rbt.barsgl.shared.loader.LoadStepWrapper;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.lang.String.format;
import static javax.ejb.LockType.READ;

/**
 * Created by SotnikovAV on 28.10.2016.
 */
@Stateless
@LocalBean
public class LoadManagementController extends BaseDictionaryController<LoadStepWrapper, Long, LoadManagement, LoadManagementRepository>
        implements ILoadManagementController<LoadStepWrapper> {

    private static final Logger log = Logger.getLogger(LoadManagementController.class);

    @Inject
    private LoadManagementRepository loadManagementRepository;

    @Inject
    private UserContext userContext;

    @Override
    public RpcRes_Base<LoadStepWrapper> create(LoadStepWrapper wrapper) {
        try {
            DataSource dataSource = loadManagementRepository.getDataSource(wrapper.getRepository());
            if(isStepActionExists(dataSource, wrapper.getDat(), wrapper.getCode())) {
                throw new Exception("Действие для шага " + wrapper.getCode()
                        + " за дату " + wrapper.getDat() + " уже существует.");
            }
            Long ordid = getCurrentOrderId(dataSource);
            wrapper.setOrdid(ordid);

            Long primaryKey = wrapper.getId();
            return create(wrapper, loadManagementRepository, LoadManagement.class, primaryKey,
                    format("Действие '%d' уже существует!", primaryKey),
                    format("Создано действие: '%d'", primaryKey),
                    format("Ошибка при создании действия: '%d'", primaryKey),
                    () -> {
                        return new LoadManagement(
                                wrapper.getId(),
                                wrapper.getOrdid(),
                                wrapper.getDat(),
                                wrapper.getCode(),
                                LoadManagementAction.values()[wrapper.getAction()],
                                wrapper.getOrder(),
                                LoadManagementStatus.values()[wrapper.getStatus()],
                                wrapper.getExpert(),
                                wrapper.getExpertModified(),
                                wrapper.getManager(),
                                wrapper.getManagerModified(),
                                wrapper.getOperator(),
                                wrapper.getOperatorModified(),
                                wrapper.getActionStartTime(),
                                wrapper.getActionFinishTime()
                        );
                    });
        } catch(Exception ex) {
            log.error("Ошибка создания записи управления шагом загрузки", ex);
            return new RpcRes_Base<LoadStepWrapper>(wrapper, true, ex.toString());
        }
    }

    @Override
    public RpcRes_Base<LoadStepWrapper> update(LoadStepWrapper wrapper) {
        try {
        Long primaryKey = wrapper.getId();
        return update(wrapper, loadManagementRepository, LoadManagement.class,
                primaryKey,
                format("Действие '%d' не найден!", primaryKey),
                format("Изменено Действие: '%d'", primaryKey),
                format("Ошибка при изменении Действия: '%d'", primaryKey),
                loadManagement -> {
                    loadManagement.setAction(LoadManagementAction.values()[wrapper.getAction()]);
                    loadManagement.setOrder(wrapper.getOrder());
                    loadManagement.setStatus(LoadManagementStatus.values()[wrapper.getStatus()]);
                    if(null != wrapper.getExpert()) {
                        loadManagement.setExpert(wrapper.getExpert());
                        loadManagement.setExpertModified(wrapper.getManagerModified());
                    }
                    if(null != wrapper.getOperator()) {
                        loadManagement.setManager(wrapper.getManager());
                        loadManagement.setManagerModified(wrapper.getManagerModified());
                    }
                    if(null != wrapper.getOperator()) {
                        loadManagement.setOperator(wrapper.getOperator());
                        loadManagement.setOperatorModified(wrapper.getOperatorModified());
                    }
                });
        } catch(Exception ex) {
            log.error("Ошибка обновления записи управления шагом загрузки", ex);
            return new RpcRes_Base<LoadStepWrapper>(wrapper, true, ex.toString());
        }
    }

    @Override
    public RpcRes_Base<LoadStepWrapper> delete(LoadStepWrapper wrapper) {
        Long primaryKey = wrapper.getId();
        return delete(wrapper, loadManagementRepository, LoadManagement.class, primaryKey,
                format("Действие '%d' не найден!", primaryKey),
                format("Удалено Действие: '%d'", primaryKey),
                format("Ошибка при удалении Действия: '%d'", primaryKey)
        );
    }

    @Override
    public List<LoadStepWrapper> updateStatus(Repository repository, Long orderId, LoadManagementStatus status) throws Exception {
        DataSource dataSource = loadManagementRepository.getDataSource(repository);
        DataRecord rec = loadManagementRepository.selectFirst(
                dataSource,
                "select status from load_management where ordid=?",
                orderId
        );
        Integer currentStatus = rec.getInteger("status");
        String sql = null;
        switch(status) {
            case Assigned:
                if(null != currentStatus && LoadManagementStatus.None.ordinal() != currentStatus) {
                    throw new Exception("Невозможно назначить действие!");
                }
                sql = "update load_management set status=?, expert=?, expert_modified=? where ordid=?";
                break;
            case Approved:
                if(LoadManagementStatus.Assigned.ordinal() != currentStatus) {
                    throw new Exception("Невозможно согласовать действие!");
                }
                sql = "update load_management set status=?, manager=?, manager_modified=? where ordid=?";
                break;
            case Executed:
                if(LoadManagementStatus.Approved.ordinal() != currentStatus) {
                    throw new Exception("Невозможно выполнить действие!");
                }
                sql = "update load_management set status=?, operator=?, operator_modified=? where ordid=?";
                break;
            case None:
                if(LoadManagementStatus.Executed.ordinal() == currentStatus) {
                    throw new Exception("Невозможно отменить действие!");
                }
                sql = "update load_management set status=?, operator=?, operator_modified=? where ordid=?";
                break;
        }
        EntityManager persistence = loadManagementRepository.getPersistence(repository);
        int cnt = loadManagementRepository.executeNativeUpdate(
                persistence,
                sql,
                status.ordinal(),
                userContext.getUserName(),
                userContext.getTimestamp(),
                orderId
        );
        List<LoadStepWrapper> res = new ArrayList<>(cnt);
        List<LoadManagement> rows = loadManagementRepository.findNative(
                persistence,
                LoadManagement.class,
                "select * from load_management where ordid=? and status=?",
                1000,
                orderId,
                status.ordinal()
        );
        for(LoadManagement row:rows) {
            LoadStepWrapper wrapper = new LoadStepWrapper();
            wrapper.setId(row.getId());
            wrapper.setOrdid(row.getOrdid());
            wrapper.setDat(row.getDat());
            wrapper.setCode(row.getCode());
            wrapper.setAction(row.getAction().ordinal());
            wrapper.setOrder(row.getOrder());
            wrapper.setStatus(row.getStatus().ordinal());
            wrapper.setExpert(row.getExpert());
            wrapper.setExpertModified(row.getExpertModified());
            wrapper.setManager(row.getManager());
            wrapper.setManagerModified(row.getManagerModified());
            wrapper.setOperator(row.getOperator());
            wrapper.setOperatorModified(row.getOperatorModified());
            wrapper.setActionStartTime(row.getStartTime());
            wrapper.setActionFinishTime(row.getFinishTime());
            res.add(wrapper);
        }
        return res;
    }

    @Lock(READ)
    public boolean isStepActionExists(DataSource dataSource, Date dat, String stepCode) throws Exception {
        DataRecord rec = loadManagementRepository.selectFirst(
                dataSource,
                "select count(1) as cnt from load_management where dat=? and code=? and status=0",
                dat,
                stepCode
        );
        Long cnt = rec.getLong("cnt");
        return 0L < cnt;
    }

    @Lock(READ)
    public Long getCurrentOrderId(DataSource dataSource) throws Exception {
        DataRecord rec = loadManagementRepository.selectFirst(
                dataSource,
                "select max(ordid) as ordid from load_management where status=0");
        Long ordid = rec.getLong("ordid");
        if(null == ordid) {
            return getNextOrderId(dataSource);
        }
        return ordid;
    }

    @Lock(READ)
    public Long getNextOrderId(DataSource dataSource) throws Exception {
        DataRecord rec = loadManagementRepository.selectFirst(
                dataSource,
                "select max(ordid) as ordid from load_management"
        );
        Long ordid = rec.getLong("ordid");
        if(null == ordid) {
            return new Long(1);
        } else {
            ordid++;
            return ordid;
        }
    }

    @Override
    public RpcRes_Base<LoadStepWrapper> create(LoadStepWrapper wrapper,
                                               LoadManagementRepository repository,
                                               Class<LoadManagement> clazz, Long primaryKey,
                                               String checkMessage, String infoMessage, String auditErrorMessage,
                                               Supplier<LoadManagement> supplier) {
        try {
            EntityManager persistence = repository.getPersistence(wrapper.getRepository());

            LoadManagement entity;
            // проверка уникальности имени в таблице
            if (null != primaryKey) {
                entity = repository.findById(persistence, clazz, primaryKey);
                if (entity != null) {
                    return new RpcRes_Base<>(wrapper, true, checkMessage);
                }
            }


            entity = supplier.get();
            repository.save(persistence, entity, true);
            wrapper.setId(entity.getId());
            return auditInfo(wrapper, infoMessage);
        } catch (Exception ex) {
            return auditError(wrapper, auditErrorMessage, ex);
        }
    }

    @Override
    protected RpcRes_Base<LoadStepWrapper> update(LoadStepWrapper wrapper, LoadManagementRepository repository, Class<LoadManagement> clazz, Long primaryKey, String checkMessage, String infoMessage, String auditErrorMessage, Consumer<LoadManagement> consumer) {
        try {
            EntityManager persistence = repository.getPersistence(wrapper.getRepository());
            LoadManagement entity = repository.findById(persistence, clazz, primaryKey);
            if (entity == null) {
                return new RpcRes_Base<>(wrapper, true, checkMessage);
            }


            consumer.accept(entity);

            repository.update(persistence, entity);

            return auditInfo(wrapper, infoMessage);
        } catch (Exception ex) {
            return auditError(wrapper, auditErrorMessage, ex);
        }
    }

    @Override
    public RpcRes_Base<LoadStepWrapper> delete(LoadStepWrapper wrapper, LoadManagementRepository repository, Class<LoadManagement> clazz, Long primaryKey, String checkMessage, String infoMessage, String auditErrorMessage) {
        try {
            EntityManager persistence = repository.getPersistence(wrapper.getRepository());
            LoadManagement entity = repository.findById(persistence, clazz, primaryKey);
            if (entity == null) {
                return new RpcRes_Base<>(wrapper, true, checkMessage);
            }

            repository.remove(persistence, entity);

            return auditInfo(wrapper, infoMessage);
        } catch (Exception ex) {
            return auditError(wrapper, auditErrorMessage, ex);
        }
    }

    public List<LoadStepWrapper> deleteActions(Repository repository, Long orderId) throws Exception {
        EntityManager persistence = loadManagementRepository.getPersistence(repository);
        List<LoadManagement> result = loadManagementRepository.findNative(persistence, LoadManagement.class, "select * from load_management where ordid=?", 1000, orderId);
        for(LoadManagement lm:result) {
            loadManagementRepository.remove(persistence, lm);
        }
        return new ArrayList<>(1);
    }

    public List<LoadStepWrapper> deleteAction(Repository repository, Long actionId) throws Exception {
        EntityManager persistence = loadManagementRepository.getPersistence(repository);
        LoadManagement result = loadManagementRepository.findNativeFirst(persistence, LoadManagement.class, "select * from load_management where id=?", 1000, actionId);
        loadManagementRepository.remove(persistence, result);
        return new ArrayList<>(1);
    }
}
