package ru.rbt.barsgl.gwt.server.rpc.cob;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.cob.CobWrapper;

/**
 * Created by ER18837 on 10.03.17.
 */
@RemoteServiceRelativePath("service/CobService")    // TODO куда прописать?
public interface CobMonitorService extends RemoteService {
        RpcRes_Base<CobWrapper> getInfo(Long idCob) throws Exception;
}
