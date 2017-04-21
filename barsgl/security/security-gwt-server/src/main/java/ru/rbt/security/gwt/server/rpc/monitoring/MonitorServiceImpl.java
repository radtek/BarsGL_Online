package ru.rbt.security.gwt.server.rpc.monitoring;

import ru.rbt.barsgl.gwt.core.server.rpc.AbstractGwtService;
import ru.rbt.barsgl.gwt.core.server.rpc.RpcResProcessor;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.monitoring.MonitoringWrapper;
import ru.rbt.gwt.security.ejb.monitoring.MonitoringController;

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
