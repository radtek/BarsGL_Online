package ru.rbt.ejbcore.mapping;

/**
 * Created by Ivan Sevastyanov
 */
public enum YesNo {
    Y, N;

    static public YesNo getValue(boolean yes) {
        return yes ? Y : N;
    }
}
