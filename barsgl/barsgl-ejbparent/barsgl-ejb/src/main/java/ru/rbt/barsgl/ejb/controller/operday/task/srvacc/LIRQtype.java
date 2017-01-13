package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

/**
 * Created by ER22228 on 16.05.2016.
 */
public enum LIRQtype {

    BALIRQ, LIRQ;

    public String value() {
        return name();
    }

    public static LIRQtype fromValue(String v) {
        return valueOf(v);
    }

}
