package ru.rbt.security.gwt.server.rpc.operday.info;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.operday.DatesWrapper;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;

/**
 * Created by akichigi on 23.03.15.
 */
@RemoteServiceRelativePath("service/OperDayInfoService")
public interface OperDayInfoService extends RemoteService {

        RpcRes_Base<OperDayWrapper> getOperDay() throws Exception;
        RpcRes_Base<DatesWrapper> getRep47425Dates() throws Exception;
}

