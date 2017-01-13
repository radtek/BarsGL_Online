package ru.rbt.barsgl.gwt.core.utils;

/**
 * Created by Ivan Sevastyanov
 */
public interface AppFunction<F,T> {
    T apply(F from);
}
