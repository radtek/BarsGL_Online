package ru.rbt.barsgl.ejb.controller.sm;

import java.util.Objects;

/**
 * Created by Ivan Sevastyanov on 22.10.2018.
 */
public class Transition <State extends Enum, Event extends Enum> {

    private State from;
    private State to;
    private Event event;
    private Class<? extends StateAction> actionClass;


    public Transition(State from, State to, Event event,  Class<? extends StateAction> actionClass) {
        this.from = from;
        this.to = to;
        this.event = event;
        this.actionClass = actionClass;
    }

    public State getFrom() {
        return from;
    }

    public State getTo() {
        return to;
    }

    public Event getEvent() {
        return event;
    }

    public Class<? extends StateAction> getActionClass() {
        return actionClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transition that = (Transition) o;
        return from == that.from &&
                to == that.to &&
                event == that.event;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, event);
    }

    @Override
    public String toString() {
        return "Transition{" +
                "from=" + from +
                ", to=" + to +
                ", event=" + event +
                ", actionClass=" + actionClass +
                '}';
    }
}
