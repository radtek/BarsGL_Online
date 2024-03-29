package ru.rbt.security.gwt.client.operday;

import com.google.gwt.user.client.Window;
import ru.rbt.barsgl.shared.operday.DatesWrapper;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.security.gwt.client.CommonEntryPoint;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.shared.RpcRes_Base;
//import ru.rbt.barsgl.shared.enums.ProcessingStatus;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by ER18837 on 28.10.15.
 */
public class OperDayGetter {
    public static final String dateFormat = "dd.MM.yyyy";

    public static void getOperday(final IDataConsumer<OperDayWrapper> consumer) {
 //       WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

        CommonEntryPoint.operDayService.getOperDay(new AuthCheckAsyncCallback<RpcRes_Base<OperDayWrapper>>() {
            @Override
            public void onFailureOthers(Throwable throwable) {
//                WaitingManager.hide();

                Window.alert("Операция не удалась.\nОшибка: " + throwable.getLocalizedMessage());
            }

            @Override
            public void onSuccess(RpcRes_Base<OperDayWrapper> res) {
                if (res.isError()){
                    DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + res.getMessage());
                } else {
//                	final String od = res.getResult().getCurrentOD();
                    consumer.accept(res.getResult());
                }
//                WaitingManager.hide();
            }
        });

    }

    public static void getRep47422Dates(final IDataConsumer<DatesWrapper> consumer) {
        CommonEntryPoint.operDayService.getRep47425Dates(new AuthCheckAsyncCallback<RpcRes_Base<DatesWrapper>>() {
            @Override
            public void onFailureOthers(Throwable throwable) {
                Window.alert("Операция не удалась.\nОшибка: " + throwable.getLocalizedMessage());
            }

            @Override
            public void onSuccess(RpcRes_Base<DatesWrapper> res) {
                if (res.isError()){
                    DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + res.getMessage());
                } else {
                    consumer.accept(res.getResult());
                }
            }
        });
    }

    //@@@
    /*
    public static void getProcessingStatus(final IDataConsumer<ProcessingStatus> consumer) {
        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

        CommonEntryPoint.operDayService.getProcessingStatus(new AuthCheckAsyncCallback<RpcRes_Base<ProcessingStatus>>() {
            @Override
            public void onFailureOthers(Throwable throwable) {
                WaitingManager.hide();

                Window.alert("Операция не удалась.\nОшибка: " + throwable.getLocalizedMessage());
            }

            @Override
            public void onSuccess(RpcRes_Base<ProcessingStatus> res) {
                if (res.isError()){
                    DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + res.getMessage());
                } else {
//                	final String od = res.getResult().getCurrentOD();
                    consumer.accept(res.getResult());
                }
                WaitingManager.hide();
            }
        });

    }
    */
}
