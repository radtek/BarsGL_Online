package ru.rbt.barsgl.gwt.server.rpc.operday;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.cob.CobWrapper;
import ru.rbt.barsgl.shared.enums.BalanceMode;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;
import ru.rbt.barsgl.shared.jobs.TimerJobHistoryWrapper;
import ru.rbt.barsgl.shared.operday.COB_OKWrapper;
import ru.rbt.barsgl.shared.operday.LwdBalanceCutWrapper;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;

/**
 * Created by akichigi on 23.03.15.
 */
@RemoteServiceRelativePath("service/OperDayService")
public interface OperDayService extends RemoteService {

//        RpcRes_Base<OperDayWrapper> getOperDay() throws Exception;

        RpcRes_Base<LwdBalanceCutWrapper> setLwdBalanceCut(LwdBalanceCutWrapper wrapper) throws Exception;

        RpcRes_Base<LwdBalanceCutWrapper> getLwdBalanceCut() throws Exception;

        RpcRes_Base<COB_OKWrapper> getCOB_OK() throws Exception;

        RpcRes_Base<Boolean> runCloseLastWorkdayBalanceTask() throws Exception;

        RpcRes_Base<Boolean> runOpenOperdayTask() throws Exception;

        RpcRes_Base<OperDayWrapper> swithPdMode() throws Exception;

        RpcRes_Base<ProcessingStatus> getProcessingStatus() throws Exception;

        RpcRes_Base<String> setProcessingStatus(ProcessingStatus processingStatus) throws Exception;

        RpcRes_Base<BalanceMode> getRefreshRestStatus() throws Exception;

        RpcRes_Base<String> setRefreshRestStatus(BalanceMode balanceMode) throws Exception;

        RpcRes_Base<CobWrapper> getCobInfo(Long idCob) throws Exception;

        RpcRes_Base<CobWrapper> calculateCob() throws Exception;

        RpcRes_Base<TimerJobHistoryWrapper> runExecuteFakeCOBTask() throws Exception;

        RpcRes_Base<TimerJobHistoryWrapper> runExecutePreCOBTask() throws Exception;

        RpcRes_Base<Boolean> switchAccessMode(OperDayWrapper wrapper) throws Exception;
}

