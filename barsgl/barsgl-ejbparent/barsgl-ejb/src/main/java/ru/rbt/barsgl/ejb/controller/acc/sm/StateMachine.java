package ru.rbt.barsgl.ejb.controller.acc.sm;

import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.shared.Assert;

import javax.naming.InitialContext;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Ivan Sevastyanov on 22.10.2018.
 */
public class StateMachine<Event extends Enum, State extends Enum, Entity extends StatefullObject> {

    private List<Transition<Event, State, Entity>> transitions = new ArrayList<>();

    StateMachine() {
    }

    void addTransition(Transition<Event, State, Entity> transition) {
        transitions.add(transition);
    }

    public void acceptEvent(Entity entity, Event event) throws Exception {
        List<Transition> filteredTransitions = transitions.stream().filter(t -> t.getEvent() == event).collect(Collectors.toList());
        Assert.isTrue(1 == filteredTransitions.size());
        Transition targetTransition = filteredTransitions.get(0);
        StateMachineSupportBean supportBean = findStateMachineSupport();
        Event newEvent = supportBean.executeAction(entity, targetTransition);
        supportBean.updateToTargetState(entity, targetTransition);
    }

    private StateMachineSupportBean findStateMachineSupport() {
        try {
            return (StateMachineSupportBean) new InitialContext().lookup("java:app/barsgl-ejb/StateMachineSupportBean");
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

}
