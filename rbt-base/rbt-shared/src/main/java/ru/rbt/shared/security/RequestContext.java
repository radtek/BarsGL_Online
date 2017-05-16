/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2017
 */
package ru.rbt.shared.security;

import java.util.Optional;
import ru.rbt.shared.ctx.UserRequestHolder;

/**
 *
 * @author Andrew Samsonov
 */
public interface RequestContext {
    void setRequest(UserRequestHolder requestHolder);

    Optional<UserRequestHolder> getRequest();
}
