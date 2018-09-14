package ru.rbt.security.gwt.server.rpc.operday.info;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.controller.od.Rep47422Controller;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.gwt.core.server.rpc.AbstractGwtService;
import ru.rbt.barsgl.gwt.core.server.rpc.RpcResProcessor;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.OperDayButtons;
import ru.rbt.barsgl.shared.operday.DatesWrapper;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by akichigi on 23.03.15.
 */
public class OperDayInfoServiceImpl extends AbstractGwtService implements OperDayInfoService {

    @Override
    public RpcRes_Base<OperDayWrapper> getOperDay() throws Exception {
        return new RpcResProcessor<OperDayWrapper>() {
            @Override
            protected RpcRes_Base<OperDayWrapper> buildResponse() throws Throwable {
                // обновляем на тот случай, если изменили через БД
                localInvoker.invoke(OperdayController.class, "refresh");
                // получаем
                Operday od = localInvoker.invoke(OperdayController.class, "getOperday");
                if (od == null) throw new Throwable("Не найдена информация по операционному дню'.");

                Operday.OperdayPhase phase = od.getPhase();
                Operday.LastWorkdayStatus status = od.getLastWorkdayStatus();

                OperDayWrapper wrapper = new OperDayWrapper();

                wrapper.setCurrentODDate(od.getCurrentDate());
                wrapper.setPreviosODDate(od.getLastWorkingDay());
                wrapper.setCurrentOD(new SimpleDateFormat("dd.MM.yyyy").format(od.getCurrentDate()));
                wrapper.setPhaseCurrentOD(phase.getLabel());
                wrapper.setPreviousOD(new SimpleDateFormat("dd.MM.yyyy").format(od.getLastWorkingDay()));
                wrapper.setPreviousODBalanceStatus(status.getLabel());
                wrapper.setPdMode(od.getPdMode().name() + " (" + od.getPdMode().getLabel() + ")");
                wrapper.setAccessMode(od.getAccessMode());

                OperDayButtons buttonStatus = OperDayButtons.NONE;


                if (phase == Operday.OperdayPhase.ONLINE)
                    buttonStatus = OperDayButtons.CHANGE_PHASE_TO_PRE_COB;
                else
                    buttonStatus = OperDayButtons.OPEN_OD;
/*
                switch (status){
                    case OPEN:
                        if (phase == Operday.OperdayPhase.ONLINE)  buttonStatus = OperDayButtons.CLOSE_BALANCE_PREVIOUS_OD;
                        break;
                    case CLOSED:
                        switch (phase){
                            case COB:
                                buttonStatus = OperDayButtons.OPEN_OD;
                                break;
                            case ONLINE:
                                buttonStatus = OperDayButtons.CHANGE_PHASE_TO_PRE_COB;
                                break;
                            default: buttonStatus = OperDayButtons.NONE;
                        }
                }
*/
                wrapper.setEnabledButton(buttonStatus);

                additionalAction(wrapper);

                return new RpcRes_Base<>(wrapper, false, "");
            }
        }.process();
    }

    @Override
    public RpcRes_Base<DatesWrapper> getRep47425Dates() throws Exception {
        return new RpcResProcessor<DatesWrapper>() {
            @Override
            protected RpcRes_Base<DatesWrapper> buildResponse() throws Throwable {
                // получаем
                DatesWrapper wrapper = localInvoker.invoke(Rep47422Controller.class, "getDatesReport");

                return new RpcRes_Base<>(wrapper, false, "");
            }
        }.process();
    }

    protected void additionalAction(OperDayWrapper wrapper) throws Exception {
    }
}
