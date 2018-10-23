package ru.rbt.barsgl.ejb.controller.acc.sm;

import ru.rbt.ejbcore.mapping.BaseEntity;

import java.io.Serializable;

/**
 * Created by Ivan Sevastyanov on 22.10.2018.
 */
public abstract class StatefullObject<State extends Enum, T extends Serializable> extends BaseEntity<T> {
    public abstract void setState(State state);
    public abstract State getState();
}
