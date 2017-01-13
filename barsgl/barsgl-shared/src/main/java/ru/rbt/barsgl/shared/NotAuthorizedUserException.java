package ru.rbt.barsgl.shared;

/**
 * Created by Ivan Sevastyanov
 */
public class NotAuthorizedUserException extends Exception {

    public static final String NOT_AUTHORIZED_MESSAGE = "User is not authorized";

    @Override
    public String getMessage() {
        return NOT_AUTHORIZED_MESSAGE;
    }
}
