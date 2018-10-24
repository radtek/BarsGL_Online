package ru.rbt.barsgl.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import ru.rbt.barsgl.gwt.server.rpc.account.ManualAccountService;
import ru.rbt.barsgl.gwt.server.rpc.account.ManualAccountServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.replication.ReplService;
import ru.rbt.barsgl.gwt.server.rpc.replication.ReplServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.access.AccessService;
import ru.rbt.barsgl.gwt.server.rpc.access.AccessServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.dict.ManualDictionaryService;
import ru.rbt.barsgl.gwt.server.rpc.dict.ManualDictionaryServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.loader.LoaderControlService;
import ru.rbt.barsgl.gwt.server.rpc.loader.LoaderControlServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.operation.ManualOperationService;
import ru.rbt.barsgl.gwt.server.rpc.operation.ManualOperationServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.operday.OperDayService;
import ru.rbt.barsgl.gwt.server.rpc.operday.OperDayServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.properties.PropertiesService;
import ru.rbt.barsgl.gwt.server.rpc.properties.PropertiesServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.sync.PdSyncService;
import ru.rbt.barsgl.gwt.server.rpc.sync.PdSyncServiceAsync;
import ru.rbt.barsgl.gwt.client.formmanager.MenuBuilder;

import ru.rbt.security.gwt.client.security.SecurityEntryPoint;

/**
 * Created by ER21006 on 13.01.2015.
 */
public class BarsGLEntryPoint implements EntryPoint {

    public static OperDayServiceAsync operDayService;
    public static ManualOperationServiceAsync operationService;
    public static ManualAccountServiceAsync accountService;
    public static ManualDictionaryServiceAsync dictionaryService;
    public static AccessServiceAsync accessService;
    public static PdSyncServiceAsync pdSyncService;
    public static PropertiesServiceAsync propertiesService;
    public static LoaderControlServiceAsync loaderService;
    public static ReplServiceAsync replService;

    public static MenuBuilder menuBuilder;

    @Override
    public void onModuleLoad() {
        operDayService = GWT.create(OperDayService.class);
        operationService = GWT.create(ManualOperationService.class);
        accountService = GWT.create(ManualAccountService.class);
        dictionaryService = GWT.create(ManualDictionaryService.class);
        accessService = GWT.create(AccessService.class);
        pdSyncService = GWT.create(PdSyncService.class);
        propertiesService = GWT.create(PropertiesService.class);
        loaderService = GWT.create(LoaderControlService.class);
        replService = GWT.create(ReplService.class);

        SecurityEntryPoint.init(menuBuilder = new MenuBuilder());
        SecurityEntryPoint.checkSession();
        SecurityEntryPoint.setDatabaseVersion();
    }
}
