package ru.rbt.security.gwt.server.rpc.operday.info;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.operday.DatesWrapper;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;

/**
 * Created by akichigi on 23.03.15.
 */
public interface OperDayInfoServiceAsync {
     void getOperDay(AsyncCallback<RpcRes_Base<OperDayWrapper>> callback);
     void getRep47425Dates(AsyncCallback<RpcRes_Base<DatesWrapper>> callback);
}
