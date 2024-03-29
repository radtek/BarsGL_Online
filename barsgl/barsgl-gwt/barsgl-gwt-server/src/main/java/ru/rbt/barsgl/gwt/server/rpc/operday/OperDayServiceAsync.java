package ru.rbt.barsgl.gwt.server.rpc.operday;

import com.google.gwt.user.client.rpc.AsyncCallback;
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
public interface OperDayServiceAsync {
     void setLwdBalanceCut(LwdBalanceCutWrapper wrapper, AsyncCallback<RpcRes_Base<LwdBalanceCutWrapper>> callback);
     void getLwdBalanceCut(AsyncCallback<RpcRes_Base<LwdBalanceCutWrapper>> callback);

     void getCOB_OK(AsyncCallback<RpcRes_Base<COB_OKWrapper>> callback);
     void runCloseLastWorkdayBalanceTask(AsyncCallback<RpcRes_Base<Boolean>> callback);
     void runOpenOperdayTask(AsyncCallback<RpcRes_Base<Boolean>> callback);
     void swithPdMode(AsyncCallback<RpcRes_Base<OperDayWrapper>> callback);
     void getProcessingStatus(AsyncCallback<RpcRes_Base<ProcessingStatus>> callback);
     void setProcessingStatus(ProcessingStatus processingStatus, AsyncCallback<RpcRes_Base<String>> callback);

     void getRefreshRestStatus(AsyncCallback<RpcRes_Base<BalanceMode>> callback);
     void setRefreshRestStatus(BalanceMode balanceMode, AsyncCallback<RpcRes_Base<String>> callback);

     void getCobInfo(Long idCob, AsyncCallback<RpcRes_Base<CobWrapper>> callback);
     void calculateCob(AsyncCallback<RpcRes_Base<CobWrapper>> callback);
     void runExecuteFakeCOBTask(AsyncCallback<RpcRes_Base<TimerJobHistoryWrapper>> callback);
     void runExecutePreCOBTask(AsyncCallback<RpcRes_Base<TimerJobHistoryWrapper>> callback);
     void switchAccessMode(OperDayWrapper wrapper, AsyncCallback<RpcRes_Base<Boolean>> callback);
}
