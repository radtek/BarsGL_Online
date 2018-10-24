package ru.rbt.barsgl.ejb.controller.sm;

import javax.ejb.Local;

/**
 * Created by Ivan Sevastyanov on 24.10.2018.
 */
@Local
public interface StateMachineSupport {

    <Event extends Enum, Entity extends StatefullObject> Event executeAction(Entity entity, Transition transition) throws StateMachineException;

    <Entity extends StatefullObject> void updateToTargetState(final Entity entity, Transition transition);

    <O extends StatefullObject> O refreshStatefullObject(O statefullObject);
}
