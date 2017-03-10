package ru.rbt.barsgl.gwt.server.rpc.cob;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.cob.CobWrapper;

/**
 * Created by ER18837 on 10.03.17.
 */
public interface CobMonitorServiceAsync {
    void getInfo(AsyncCallback<RpcRes_Base<CobWrapper>> callback, Long idCob) throws Exception;
}
