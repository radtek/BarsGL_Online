package ru.rbt.barsgl.ejb.controller.sm;

import ru.rbt.barsgl.ejb.controller.acc.act.AccountBatchSendToValidate;
import ru.rbt.barsgl.ejbcore.BarsglPersistenceProvider;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.util.ServerUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * Created by Ivan Sevastyanov on 22.10.2018.
 */
@Stateless
public class StateMachineSupportBean implements StateMachineSupport {

    private static final Logger logger = Logger.getLogger(StateMachineSupportBean.class.getName());

    @Inject
    private Instance<StateAction<?,?>> actions;

    @EJB
    private CoreRepository repository;

    @Inject
    private AccountBatchSendToValidate start;

    @Inject
    private BarsglPersistenceProvider persistenceProvider;

    @SuppressWarnings("All")
    public <Event extends Enum, Entity extends StatefullObject> Event executeAction(Entity entity, Transition transition) throws StateMachineException {
        if (null != transition.getActionClass()) {
            try {
                StateAction action = ServerUtils.findAssignable(transition.getActionClass(), actions);
                return (Event) repository.executeInNewTransaction(persistence -> action.proceed(entity, transition));
            } catch (Exception e) {
                throw new StateMachineException(format("Ошибка при выпонении логики перехода:  объект '%s' переход '%s'", entity, transition), e);
            }
        } else {
            logger.warning(format("Не установлен класс логики для объекта '%s' переход '%s'", entity, transition));
        }
        return null;
    }

    public <Entity extends StatefullObject> void updateToTargetState(final Entity entity, Transition transition) {
        try {
            repository.executeInNewTransaction(persistence -> {
                Entity entity0 = (Entity) repository.findById(entity.getClass(), entity.getId());
                entity0.setState(transition.getTo());
                return repository.update(entity0);
            });
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public <O extends StatefullObject> O refreshStatefullObject(O statefullObject) {
        StatefullObject attached = (StatefullObject) repository.refresh(statefullObject, true);
        persistenceProvider.getDefaultPersistence().refresh(attached);
        return (O) attached;
    }
}
