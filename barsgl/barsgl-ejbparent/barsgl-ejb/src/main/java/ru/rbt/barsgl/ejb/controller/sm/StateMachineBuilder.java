package ru.rbt.barsgl.ejb.controller.sm;

import ru.rbt.barsgl.shared.Builder;

/**
 * Created by Ivan Sevastyanov on 22.10.2018.
 */
public class StateMachineBuilder<State extends Enum, Event extends Enum,  Entity extends StatefullObject>
        implements Builder<StateMachine<State, Event, Entity>> {

    private StateMachine<State, Event,  Entity> sm = new StateMachine<>();

    public StateMachineBuilder() {}

    @Override
    public StateMachine<State, Event, Entity> build() {
        return sm;
    }

    public StateMachineBuilder<State, Event,  Entity> makeTransition(
            State from, State to, Event event
            , Class<? extends StateAction> actionClass) {
        sm.addTransition(new Transition<>(from, to, event,actionClass));
        return this;
    }

}
