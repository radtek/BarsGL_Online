package ru.rbt.barsgl.ejb.controller.sm;

import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.shared.Assert;

import javax.naming.InitialContext;
import java.util.ArrayList;
import java.util.List;
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

    StateMachine() {
    }

    void addTransition(Transition<State, Event> transition) {
        transitions.add(transition);
    }

    public void acceptEvent(Entity entity, Event event) throws Exception {

        StateMachineSupport supportBean = findStateMachineSupport();

        Entity entity0 = supportBean.refreshStatefullObject(entity);

        List<Transition> filteredTransitions = transitions.stream()
                .filter(t -> t.getEvent() == event && t.getFrom() == entity0.getState()).collect(Collectors.toList());
        if (1 == filteredTransitions.size()) {
            Transition transition = filteredTransitions.get(0);
            Event newEvent = supportBean.executeAction(entity, transition);

            checkCurrentState(supportBean, transition, entity);

            supportBean.updateToTargetState(entity, transition);

            if (null != newEvent) {
                acceptEvent(entity0, newEvent);
            }
        } else if (filteredTransitions.isEmpty()) {
            logger.log(Level.WARNING, format("Не найдено допустимого перехода для события '%s' на объекте '%s'", entity, entity));
        } else {
            throw new StateMachineException(format("Найдено более одного перехода для события '%s' на объекте '%s'", entity, entity));
        }
    }

    private StateMachineSupport findStateMachineSupport() {
        try {
            return (StateMachineSupport) new InitialContext().lookup("java:app/barsgl-ejb/StateMachineSupportBean");
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private void checkCurrentState(StateMachineSupport supportBean, Transition transition, Entity entity) throws StateMachineException {
        Entity entity1 = supportBean.refreshStatefullObject(entity);
        Assert.isTrue(transition.getFrom() == entity1.getState()
                , () -> new StateMachineException(format("Текущий статус '%s' объекта '%s' отличается от ожидаемого '%s' для обновления в целевой статус '%s'"
                        , entity1.getState(), entity1, transition.getFrom(), transition.getTo())));
    }

}
