package ru.rbt.barsgl.ejb.controller.sm;

/**
 * Created by Ivan Sevastyanov on 22.10.2018.
 */
public interface StateAction<Entity extends StatefullObject, Event extends Enum> {

    Event proceed(Entity stateObject, Transition transition);
}
