package ru.rbt.barsgl.ejb.controller.sm;

/**
 * Created by Ivan Sevastyanov on 24.10.2018.
 */
public class StateMachineException extends Exception {

    public StateMachineException(String message) {
        super(message);
    }

    public StateMachineException(String message, Throwable cause) {
        super(message, cause);
    }

    public StateMachineException(Throwable cause) {
        super(cause);
    }
}
