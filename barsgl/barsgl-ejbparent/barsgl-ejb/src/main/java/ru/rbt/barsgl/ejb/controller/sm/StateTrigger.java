package ru.rbt.barsgl.ejb.controller.sm;

/**
 * Created by Ivan Sevastyanov on 29.10.2018.
 */
public interface StateTrigger<Entity extends StatefullObject> {

    void onStateEnter(Entity statefullObject);
}
