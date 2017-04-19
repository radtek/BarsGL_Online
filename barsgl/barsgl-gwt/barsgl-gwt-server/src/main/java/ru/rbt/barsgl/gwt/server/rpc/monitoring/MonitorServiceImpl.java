package ru.rbt.barsgl.gwt.server.rpc.monitoring;

import ru.rbt.barsgl.ejb.monitoring.MonitoringController;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.gwt.server.rpc.AbstractGwtService;
import ru.rbt.barsgl.gwt.server.rpc.RpcResProcessor;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.monitoring.MonitoringWrapper;
import ru.rbt.barsgl.shared.monitoring.ReplTableItem2;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.google.web.bindery.requestfactory.server.RequestFactoryServlet.getThreadLocalRequest;
import static ru.rbt.barsgl.ejbcore.util.DateUtils.dateTimeString;

/**
 * Created by akichigi on 06.12.16.
 */
public class MonitorServiceImpl extends AbstractGwtService implements MonitorService {
//    @Override
//    public RpcRes_Base<MonitoringWrapper> getInfo() throws Exception {
//        return new RpcResProcessor<MonitoringWrapper>() {
//            @Override
//            protected RpcRes_Base<MonitoringWrapper> buildResponse() throws Throwable {
//                RpcRes_Base<MonitoringWrapper> res = localInvoker.invoke(MonitoringController.class, "getInfo");
//                if (res == null) throw new Throwable("Не удалось получить данные для мониторинга!");
//                return res;
//            }
//        }.process();
//    }
    @Override
    public RpcRes_Base<HashMap> getBuff() throws Exception {

        return new RpcResProcessor<HashMap>() {
            @Override
            protected RpcRes_Base<HashMap> buildResponse() throws Throwable {
//                long intervalMillisec = 0;
                Date oldDate = (Date)getThreadLocalRequest().getSession().getAttribute("moniOldBuffDate");
//                Date newDate = new Date();
//                if (oldDate == null) oldDate = newDate;
//                if (oldDate.compareTo( newDate ) != 0){
//                    intervalMillisec = newDate.getTime() - oldDate.getTime();
//                }

                Integer oldPdMoved = (Integer)getThreadLocalRequest().getSession().getAttribute("oldPdMoved");
                Integer oldBltMoved = (Integer)getThreadLocalRequest().getSession().getAttribute("oldBltMoved");

                RpcRes_Base<HashMap> res = localInvoker.invoke(MonitoringController.class, "getBuff", oldDate, oldPdMoved, oldBltMoved);
                if (res == null) throw new Throwable("Не удалось получить данные для буффера!");

                getThreadLocalRequest().getSession().setAttribute("moniOldBuffDate", res.getResult().get("moniOldBuffDate"));
                getThreadLocalRequest().getSession().setAttribute("oldPdMoved",  Integer.valueOf(res.getResult().get("pd_Moved").toString()));
                getThreadLocalRequest().getSession().setAttribute("oldBltMoved", Integer.valueOf(res.getResult().get("blt_Moved").toString()));
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<ArrayList> getOper() throws Exception {
        return new RpcResProcessor<ArrayList>() {
            @Override
            protected RpcRes_Base<ArrayList> buildResponse() throws Throwable {
                RpcRes_Base<ArrayList> res = localInvoker.invoke(MonitoringController.class, "getOper");
                if (res == null) throw new Throwable("Не удалось получить данные по операциям!");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<HashMap> getRepl() throws Exception {
        return new RpcResProcessor<HashMap>() {
            @Override
            protected RpcRes_Base<HashMap> buildResponse() throws Throwable {
//                long intervalMillisec = 0;
                Date oldDate = (Date)getThreadLocalRequest().getSession().getAttribute("moniOldReplDate");
//                Date newDate = new Date();
//                if (oldDate == null) oldDate = newDate;
//                if (oldDate.compareTo( newDate ) != 0){
//                    intervalMillisec = newDate.getTime() - oldDate.getTime();
//                }
                List<ReplTableItem2> oldRepl = (List<ReplTableItem2>)getThreadLocalRequest().getSession().getAttribute("oldRepl");

                RpcRes_Base<HashMap> res = localInvoker.invoke(MonitoringController.class, "getRepl", oldDate, oldRepl);
                if (res == null) throw new Throwable("Не удалось получить данные по репликациям!");

                getThreadLocalRequest().getSession().setAttribute("oldRepl", res.getResult().get("repl"));
//                res.getResult().put("strNewDate", dateTimeString(newDate));
                getThreadLocalRequest().getSession().setAttribute("moniOldReplDate", res.getResult().get("moniOldReplDate"));
                return res;
            }
        }.process();
    }

//    private long saveDate(String attr){
//        long intervalMillisec = 0;
//        Date oldDate = (Date)getThreadLocalRequest().getSession().getAttribute(attr);
//        Date newDate = new Date();
//        if (oldDate == null) oldDate = newDate;
//        if (oldDate.compareTo( newDate ) != 0){
//            intervalMillisec = newDate.getTime() - oldDate.getTime();
//        }else{
//            intervalMillisec = 0;
//        }
//        getThreadLocalRequest().getSession().setAttribute(attr, newDate);
//        return intervalMillisec;
//    }
}
