package ru.rbt.barsgl.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
//import com.google.gwt.user.client.Window;
//import com.google.gwt.user.client.rpc.AsyncCallback;
//import com.google.gwt.user.client.ui.RootPanel;
//import ru.rbt.barsgl.gwt.common.cli.formmanager.FormManagerUI;
//import ru.rbt.security.gwt.client.LoginForm;
//import ru.rbt.security.gwt.client.LoginFormHandler;
//import ru.rbt.barsgl.gwt.core.LocalDataStorage;
//import ru.rbt.barsgl.gwt.core.SecurityChecker;
//import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.server.rpc.access.AccessService;
import ru.rbt.barsgl.gwt.server.rpc.access.AccessServiceAsync;
//import ru.rbt.security.gwt.server.rpc.asyncGrid.AsyncGridService;
//import ru.rbt.security.gwt.server.rpc.asyncGrid.AsyncGridServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.dict.ManualDictionaryService;
import ru.rbt.barsgl.gwt.server.rpc.dict.ManualDictionaryServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.job.TimerJobService;
import ru.rbt.barsgl.gwt.server.rpc.job.TimerJobServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.loader.LoaderControlService;
import ru.rbt.barsgl.gwt.server.rpc.loader.LoaderControlServiceAsync;
//import ru.rbt.monitoring.MonitorService;
//import ru.rbt.monitoring.MonitorServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.operation.ManualOperationService;
import ru.rbt.barsgl.gwt.server.rpc.operation.ManualOperationServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.operday.OperDayService;
import ru.rbt.barsgl.gwt.server.rpc.operday.OperDayServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.properties.PropertiesService;
import ru.rbt.barsgl.gwt.server.rpc.properties.PropertiesServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.sync.PdSyncService;
import ru.rbt.barsgl.gwt.server.rpc.sync.PdSyncServiceAsync;
//import ru.rbt.barsgl.shared.LoginResult;
//import ru.rbt.barsgl.shared.NotAuthorizedUserException;
//import ru.rbt.barsgl.shared.RpcRes_Base;
//import ru.rbt.barsgl.shared.Utils;
//import ru.rbt.barsgl.shared.user.AppUserWrapper;

import java.util.Date;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.shared.NotAuthorizedUserException.NOT_AUTHORIZED_MESSAGE;

/**
 * Created by ER21006 on 13.01.2015.
 */
public class BarsGLEntryPoint implements EntryPoint {

    public static TimerJobServiceAsync timerJobService;
    public static OperDayServiceAsync operDayService;
//    public static AsyncGridServiceAsync asyncGridService;
    public static ManualOperationServiceAsync operationService;
    public static ManualDictionaryServiceAsync dictionaryService;
    public static AccessServiceAsync accessService;
    public static PdSyncServiceAsync pdSyncService;
    public static PropertiesServiceAsync propertiesService;
    public static LoaderControlServiceAsync loaderService;
//    public static MonitorServiceAsync monitorService;
//    public static Date CURRENT_WORKDAY;
    
    @Override
    public void onModuleLoad() {
        timerJobService = GWT.create(TimerJobService.class);
        operDayService = GWT.create(OperDayService.class);
//        asyncGridService = GWT.create(AsyncGridService.class);
        operationService = GWT.create(ManualOperationService.class);
        dictionaryService = GWT.create(ManualDictionaryService.class);
        accessService = GWT.create(AccessService.class);
        pdSyncService = GWT.create(PdSyncService.class);
        propertiesService = GWT.create(PropertiesService.class);
        loaderService = GWT.create(LoaderControlService.class);
//        monitorService = GWT.create(MonitorService.class);
    }
}
