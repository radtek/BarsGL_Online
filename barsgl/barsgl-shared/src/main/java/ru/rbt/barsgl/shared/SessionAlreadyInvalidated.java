package ru.rbt.barsgl.shared;

/**
 * Created by Ivan Sevastyanov on 15.11.2017.
 */
public class SessionAlreadyInvalidated extends Exception {

    public static String SESSION_ALREADY_INVALIDATED = "Session already invalidated";

    public SessionAlreadyInvalidated() {
        super(SESSION_ALREADY_INVALIDATED);
    }
}
