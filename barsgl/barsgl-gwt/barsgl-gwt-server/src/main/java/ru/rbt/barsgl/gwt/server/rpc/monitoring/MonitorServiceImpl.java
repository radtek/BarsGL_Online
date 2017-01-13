package ru.rbt.barsgl.gwt.server.rpc.monitoring;

import ru.rbt.barsgl.ejb.monitoring.MonitoringController;
import ru.rbt.barsgl.gwt.server.rpc.AbstractGwtService;
import ru.rbt.barsgl.gwt.server.rpc.RpcResProcessor;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.monitoring.MonitoringWrapper;

/**
 * Created by akichigi on 06.12.16.
 */
public class MonitorServiceImpl extends AbstractGwtService implements MonitorService {
    @Override
    public RpcRes_Base<MonitoringWrapper> getInfo() throws Exception {
        return new RpcResProcessor<MonitoringWrapper>() {
            @Override
            protected RpcRes_Base<MonitoringWrapper> buildResponse() throws Throwable {
                RpcRes_Base<MonitoringWrapper> res = localInvoker.invoke(MonitoringController.class, "getInfo");
                if (res == null) throw new Throwable("Не удалось получить данные для мониторинга!");
                return res;
            }
        }.process();
    }
}
