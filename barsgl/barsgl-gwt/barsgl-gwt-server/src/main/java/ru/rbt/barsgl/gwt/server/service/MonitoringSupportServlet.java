package ru.rbt.barsgl.gwt.server.service;

import ru.rbt.barsgl.ejb.monitoring.MonitoringSupportService;
import ru.rbt.barsgl.ejbcore.remote.ServerAccess;
import ru.rbt.barsgl.gwt.serverutil.GwtServerUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

/**
 * Created by Ivan Sevastyanov
 */
public class MonitoringSupportServlet extends HttpServlet {

    private static Logger log = Logger.getLogger(MonitoringSupportServlet.class.getName());

    protected ServerAccess localInvoker;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getRequestURI().matches(".*/checkdbpool$")) {
            checkDbPoolAlive(resp);
        } else
        if (req.getRequestURI().matches(".*/checketl$")) {
            checkEtlMonitorAlive(resp);
        } else {
            resp.sendError(SC_NOT_FOUND);
        }
    }

    private void checkDbPoolAlive(HttpServletResponse resp) throws IOException {
        try {
            log.info("Checking db or connection pool on aliving...");
            localInvoker.invoke(MonitoringSupportService.class, "checkDbConnectionPool", new Object[]{});
            log.info("Checking db or connection pool on aliving has SUCCESSED");
            resp.sendRedirect("../../monitorresult/dbpoolsuccess.jsp");
        } catch (Throwable e) {
            log.log(Level.SEVERE, "Error on checking db aliving", e);
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
    }

    private void checkEtlMonitorAlive(HttpServletResponse resp) throws IOException {
        try {
            log.info("Checking etl is aliving...");
            Date nextDate = localInvoker.invoke(MonitoringSupportService.class, "checkEtlMonitorAlive", new Object[]{});
            log.info("Checking etl is aliving has SUCCESSED");
            resp.getWriter().write("Next time exipiration " + nextDate);
        } catch (Throwable e) {
            log.log(Level.SEVERE, "Error on checking etl monitor aliving", e);
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        localInvoker = GwtServerUtils.findServerAccessEJBNoAuth();
    }

}
