package ru.rbt.barsgl.gwt.server.rpc.operday;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.cob.CobWrapper;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;

/**
 * Created by akichigi on 23.03.15.
 */
@RemoteServiceRelativePath("service/OperDayService")
public interface OperDayService extends RemoteService {

        RpcRes_Base<OperDayWrapper> getOperDay() throws Exception;

        RpcRes_Base<Boolean> runCloseLastWorkdayBalanceTask() throws Exception;

        RpcRes_Base<Boolean> runExecutePreCOBTask() throws Exception;

        RpcRes_Base<Boolean> runOpenOperdayTask() throws Exception;

        RpcRes_Base<OperDayWrapper> swithPdMode() throws Exception;

        RpcRes_Base<ProcessingStatus> getProcessingStatus() throws Exception;

        RpcRes_Base<CobWrapper> getCobInfo(Long idCob) throws Exception;

        RpcRes_Base<CobWrapper> calculateCob() throws Exception;

        RpcRes_Base<Boolean> runExecuteFakeCOBTask() throws Exception;
}

