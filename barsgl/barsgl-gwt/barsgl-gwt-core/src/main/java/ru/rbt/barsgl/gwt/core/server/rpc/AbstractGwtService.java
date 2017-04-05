package ru.rbt.barsgl.gwt.core.server.rpc;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import ru.rbt.barsgl.ejbcore.remote.ServerAccess;
import ru.rbt.barsgl.gwt.serverutil.GwtServerUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.rbt.barsgl.gwt.serverutil.GwtServerUtils.findServerAccess;

/**
 * Created by Ivan Sevastyanov
 */
public abstract class AbstractGwtService extends RemoteServiceServlet {

    public static final Logger logger = Logger.getLogger(AbstractGwtService.class.getName());

    protected ServerAccess localInvoker;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        localInvoker = findServerAccess();
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            logger.log(Level.FINE, "setting request: " + request);
            GwtServerUtils.setRequest(request);
            super.service(request, response);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

}