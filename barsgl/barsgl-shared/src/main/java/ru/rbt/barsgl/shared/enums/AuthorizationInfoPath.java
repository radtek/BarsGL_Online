package ru.rbt.barsgl.shared.enums;

/**
 * Created by Ivan Sevastyanov
 */
public enum AuthorizationInfoPath {
    USER_NAME("user_path")
    , USER_LOGIN_RESULT("user_login_result");

    private final String path;

    AuthorizationInfoPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
