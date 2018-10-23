package ru.rbt.barsgl.ejb.controller.sm;

import ru.rbt.barsgl.ejb.controller.acc.act.AccountBatchSendToValidate;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.util.ServerUtils;

import javax.ejb.*;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

/**
 * Created by Ivan Sevastyanov on 22.10.2018.
 */
@Stateless
@LocalBean
public class StateMachineSupportBean {

    @Inject
    private Instance<StateAction<?,?>> actions;

    @EJB
    private CoreRepository repository;

    @Inject
    private AccountBatchSendToValidate start;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public <Event extends Enum, Entity extends StatefullObject> Event executeAction(Entity entity, Transition transition) throws Exception {
        StateAction action = ServerUtils.findAssignable(transition.getActionClass(), actions);
        return (Event) repository.executeInNewTransaction(persistence -> action.proceed(entity, transition));
    }

    public <Entity extends StatefullObject> void updateToTargetState(final Entity entity, Transition transition) {
        try {
            repository.executeInNewTransaction(persistence -> {
                Entity entity0 = (Entity) repository.findById(entity.getClass(), entity.getId());
                entity0.setState(transition.getTo());
                return repository.update(entity0);
            });
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }
}
