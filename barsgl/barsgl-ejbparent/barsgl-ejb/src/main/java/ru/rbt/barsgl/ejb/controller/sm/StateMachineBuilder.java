package ru.rbt.barsgl.ejb.controller.sm;

import ru.rbt.barsgl.shared.Builder;

/**
 * Created by Ivan Sevastyanov on 22.10.2018.
 */
public class StateMachineBuilder<Event extends Enum, State extends Enum,  Entity extends StatefullObject>
        implements Builder<StateMachine<State, Event, Entity>> {

    private StateMachine sm = new StateMachine<Event, State, Entity>();

    public StateMachineBuilder() {}

    @Override
    public StateMachine build() {
        return sm;
    }

    public StateMachineBuilder<Event, State, Entity> makeTransition(
            State from, State to, Event event
            , Class<? extends StateAction> actionClass) {
        sm.addTransition(new Transition<State, Event,Entity>(from, to, event,actionClass));
        return this;
    }

}
