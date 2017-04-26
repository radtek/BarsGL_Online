package ru.rbt.barsgl.gwt.servlet;

import ru.rbt.barsgl.ejbcore.remote.ServerAccess;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

/**
 * сервлет обслуживает вызовы EJB по HTTP
 */
public final class ServerAccessServlet extends HttpServlet {

    private static final String SERVICE_EJB_JNDI_NAME = "java:global.barsgl.barsgl-ejbcore.ServerAccessBean!ru.rbt.barsgl.ejbcore.remote.ServerAccessEJBLocal";

    private ServerAccess service;

    @Override
    public void init() throws ServletException {
        super.init();
        service = getService();
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.sendRedirect("index.jsp");
            return;
        }

        response.setHeader("Cache-Control", "no-cache, no-store"); //HTTP 1.1
        response.setHeader("Pragma","no-cache"); //HTTP 1.0
        response.setDateHeader ("Expires", 0); //prevents caching at the proxy server

        try {
            // получаем данные запроса
            byte[] req_data = new byte[request.getContentLength()];
            InputStream input = request.getInputStream();
            int offset = 0;
            do {
                offset += input.read(req_data, offset, req_data.length - offset);
            } while (offset < req_data.length);

            // вызываем ejb
            byte[] resp_data = (byte[])invokeEJBMethod(service, "invoke", (new byte[0]).getClass(), req_data);

            // возвращаем результат
            response.getOutputStream().write(resp_data);

        } catch (Exception ex) {
            Throwable th = ex;
            while (th.getCause() != null) {
                th = th.getCause();
            }
            response.sendError(500, th.getClass().getName() + ": " + th.getMessage());
        }
    }

    private Object invokeEJBMethod(Object ejb, String name, Class<?> type, Object arg) throws Exception {
        Method method = ejb.getClass().getMethod(name, type);
        return method.invoke(ejb, arg);
    }

    private ServerAccess getService() {
        if (service == null) {
            InitialContext context = null;
            try {
                context = new InitialContext();
                service = (ServerAccess) context.lookup(SERVICE_EJB_JNDI_NAME);
            } catch (NamingException e) {
                throw new RuntimeException(e.getMessage(), e);
            } finally {
                try {
                    if (context != null) context.close();
                } catch (NamingException e) {
                    e.printStackTrace();
                }
            }
        }
        return service;
    }

}
