package ru.rbt.barsgl.ejb.security;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.shared.security.RequestContext;
import ru.rbt.shared.ctx.UserRequestHolder;
import ru.rbt.shared.user.AppUserWrapper;

import javax.inject.Inject;
import java.util.Date;

/**
 * Created by ER18837 on 23.06.16.
 */
public class UserContext {

    @Inject
    RequestContext contextBean;

    @Inject
    OperdayController operdayController;

    public UserRequestHolder getRequestHolder() {
        return contextBean.getRequest().orElse(UserRequestHolder.empty());
    }

    public String getUserName() {
        return getRequestHolder().getUser();
    }

    public AppUserWrapper getUserWrapper() {
        return getRequestHolder().getUserWrapper();
    }

    public Long getUserId() {
        return getUserWrapper().getId();
    }

    public Date getTimestamp() {
        return operdayController.getSystemDateTime();
    }

    public Date getCurrentDate() {
        return operdayController.getOperday().getCurrentDate();
    }

}
