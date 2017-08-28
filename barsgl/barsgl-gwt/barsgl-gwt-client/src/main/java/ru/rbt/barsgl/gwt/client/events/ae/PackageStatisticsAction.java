package ru.rbt.barsgl.gwt.client.events.ae;

import com.google.gwt.user.client.ui.Image;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.BatchPostAction;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.shared.user.AppUserWrapper;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by ER18837 on 17.01.17.
 */
public class PackageStatisticsAction extends GridAction {
    public PackageStatisticsAction(GridWidget grid) {
        super(grid, null, "Статистика по пакету", new Image(ImageConstants.INSTANCE.statistics()), 10, true);
    }

    @Override
    public void execute() {
        final Row row = grid.getCurrentRow();
        if (row == null) return;

        Long idPkg = (Long)grid.getCurrentRow().getField(grid.getTable().getColumns().getColumnIndexByName("ID_PKG")).getValue();
        if (null == idPkg || 0 == idPkg)
            return;

        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

        ManualOperationWrapper wrapper = new ManualOperationWrapper();
        wrapper.setPkgId(idPkg);
        wrapper.setAction(BatchPostAction.STATISTICS);

        AppUserWrapper appUserWrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        wrapper.setUserId(appUserWrapper.getId());

        BarsGLEntryPoint.operationService.processPackageRq(wrapper, new AuthCheckAsyncCallback<RpcRes_Base<ManualOperationWrapper>>() {
            @Override
            public void onSuccess(RpcRes_Base<ManualOperationWrapper> wrapper) {
                if (wrapper.isError()) {
                    showInfo("Ошибка", wrapper.getMessage());
                } else {
                    showInfo("Информация", wrapper.getMessage());
                }
                WaitingManager.hide();
                grid.refresh();
            }
        });
    }
}
