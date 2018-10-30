package ru.rbt.barsgl.ejbtesting.test;

import ru.rbt.barsgl.ejb.entity.monitor.AppHttpSession;
import ru.rbt.barsgl.ejb.integr.bg.BatchPackageController;
import ru.rbt.barsgl.ejb.integr.bg.ManualPostingController;
import ru.rbt.barsgl.ejb.monitoring.SessionSupportBean;
import ru.rbt.barsgl.ejb.repository.monitor.AppHttpSessionRepository;
import ru.rbt.barsgl.shared.LoginParams;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.access.HttpSessionWrapper;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.BatchPostStep;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.security.ejb.repository.AppUserRepository;
import ru.rbt.security.entity.AppUser;
import ru.rbt.shared.Assert;
import ru.rbt.shared.ctx.UserRequestHolder;
import ru.rbt.shared.security.RequestContext;

import javax.inject.Inject;

import java.util.Date;

import static ru.rbt.barsgl.shared.enums.AuthorizationInfoPath.USER_LOGIN_RESULT;

/**
 * Created by er18837 on 02.10.2018.
 */
public class ManualOperationAuthTesting extends ManualAuthTesting{

    @Inject
    private ManualPostingController postingController;

    @Inject
    BatchPackageController packageController;

    public RpcRes_Base<ManualOperationWrapper> authorizeOperationRq(ManualOperationWrapper wrapper) throws Exception {
        try {
            logon(wrapper.getUserId());
            RpcRes_Base<ManualOperationWrapper> res = postingController.authorizeOperationRq(wrapper);
            return res;
        } finally {
            logoff();
        }
    }

    public RpcRes_Base<ManualOperationWrapper> deleteOperationRq(ManualOperationWrapper wrapper) throws Exception {
         try {
            logon(wrapper.getUserId());
            RpcRes_Base<ManualOperationWrapper> res = postingController.deleteOperationRq(wrapper);
            return res;
        } finally {
            logoff();
        }
    }

    public RpcRes_Base<ManualOperationWrapper> refuseOperationRq(ManualOperationWrapper wrapper, BatchPostStatus postStatus) throws Exception {
        try {
            logon(wrapper.getUserId());
            RpcRes_Base<ManualOperationWrapper> res = postingController.refuseOperationRq(wrapper, postStatus);
            return res;
        } finally {
            logoff();
        }
    }

    public RpcRes_Base<ManualOperationWrapper> authorizePackageRq(ManualOperationWrapper wrapper, BatchPostStep postStep) throws Exception {
        try {
            logon(wrapper.getUserId());
            RpcRes_Base<ManualOperationWrapper> res = packageController.authorizePackageRq(wrapper, postStep);
            return res;
        } finally {
            logoff();
        }
    }

    public RpcRes_Base<ManualOperationWrapper> deletePackageRq(ManualOperationWrapper wrapper) throws Exception {
        try {
            logon(wrapper.getUserId());
            RpcRes_Base<ManualOperationWrapper> res = packageController.deletePackageRq(wrapper);
            return res;
        } finally {
            logoff();
        }
    }


}
