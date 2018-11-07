package ru.rbt.barsgl.ejb.controller.sm;

import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.shared.Assert;

import javax.naming.InitialContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Created by Ivan Sevastyanov on 22.10.2018.
 */
public class StateMachine<State extends Enum, Event extends Enum, Entity extends StatefullObject> {

    private static final Logger logger = Logger.getLogger(StateMachine.class.getName());

    private List<Transition< State, Event>> transitions = new ArrayList<>();
    private Map<State, Class<? extends StateTrigger>> triggers = new HashMap<>();
    private Map<State, Class<? extends StateTrigger>> leaveStatetTiggers = new HashMap<>();

    StateMachine() {
    }

    void addTransition(Transition<State, Event> transition) {
        transitions.add(transition);
    }

    void addOnStateEnterTrigger(State state, Class<? extends StateTrigger> trigger) {
        triggers.putIfAbsent(state, trigger);
    }

    void addLeaveStateTrigger(State state, Class<? extends StateTrigger> trigger) {
        leaveStatetTiggers.putIfAbsent(state, trigger);
    }

    public void acceptEvent(Entity entity, Event event) throws Exception {

        StateMachineSupport supportBean = findStateMachineSupport();

        Entity entity0 = supportBean.refreshStatefullObject(entity);

        List<Transition> filteredTransitions = transitions.stream()
                .filter(t -> t.getEvent() == event && t.getFrom() == entity0.getState()).collect(Collectors.toList());
        if (1 == filteredTransitions.size()) {

            Transition transition = filteredTransitions.get(0);

            Class<? extends StateTrigger> triggerClass = findLeaveStateTrigger((State) transition.getFrom());
            if (null != triggerClass) {
                supportBean.fireTrigger(entity, triggerClass);
            }

            Event newEvent = supportBean.executeAction(entity, transition);
            if (null != newEvent) {
                acceptEvent(entity0, newEvent);
            }
            if (checkCurrentState(supportBean, transition, entity)) {
                triggerClass = findStateTrigger((State) transition.getTo());
                if (null != triggerClass) {
                    supportBean.fireTrigger(entity, triggerClass);
                }
                supportBean.updateToTargetState(entity, transition);
            } else {
                logger.log(Level.WARNING
                        , format("Не прошла проверка на обновление объекта %s в целевой статус: %s. Статус объекта не изменен."
                        , entity, transition.getTo()));
            }
        } else if (filteredTransitions.isEmpty()) {
            logger.log(Level.WARNING, format("Не найдено допустимого перехода для события '%s' на объекте '%s' в статуса '%s'", event, entity, entity.getState()));
        } else {
            throw new StateMachineException(format("Найдено более одного перехода для события '%s' на объекте '%s'", event, entity));
        }
    }

    private StateMachineSupport findStateMachineSupport() {
        try {
            return (StateMachineSupport) new InitialContext().lookup("java:app/barsgl-ejb/StateMachineSupportBean");
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private boolean checkCurrentState(StateMachineSupport supportBean, Transition transition, Entity entity) throws StateMachineException {
        try {
            Entity entity1 = supportBean.refreshStatefullObject(entity);
            Assert.isTrue(transition.getFrom() == entity1.getState()
                    , () -> new StateMachineException(format("Текущий статус '%s' объекта '%s' отличается от ожидаемого '%s' для обновления в целевой статус '%s'"
                            , entity1.getState(), entity1, transition.getFrom(), transition.getTo())));
            return true;
        } catch (StateMachineException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return false;
        }
    }

    private Class<? extends StateTrigger> findStateTrigger(State state) {
        return triggers.get(state);
    }

    private Class<? extends StateTrigger> findLeaveStateTrigger(State state) {
        return leaveStatetTiggers.get(state);
    }

}
