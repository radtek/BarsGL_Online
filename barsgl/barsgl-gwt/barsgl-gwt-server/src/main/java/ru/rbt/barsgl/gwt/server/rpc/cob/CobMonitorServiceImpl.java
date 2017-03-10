package ru.rbt.barsgl.gwt.server.rpc.cob;

import ru.rbt.barsgl.ejb.controller.cob.CobStatService;
import ru.rbt.barsgl.gwt.server.rpc.AbstractGwtService;
import ru.rbt.barsgl.gwt.server.rpc.RpcResProcessor;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.cob.CobWrapper;

/**
 * Created by ER18837 on 10.03.17.
 */
public class CobMonitorServiceImpl extends AbstractGwtService implements CobMonitorService {

    @Override
    public RpcRes_Base<CobWrapper> getInfo(Long idCob) throws Exception {
        return new RpcResProcessor<CobWrapper>() {
            @Override
            protected RpcRes_Base<CobWrapper> buildResponse() throws Throwable {
                RpcRes_Base<CobWrapper> res = localInvoker.invoke(CobStatService.class, "getInfo", idCob);
                if (res == null) throw new Throwable("Не удалось получить данные для мониторинга COB!");
                return res;
            }
        }.process();
    }
}
