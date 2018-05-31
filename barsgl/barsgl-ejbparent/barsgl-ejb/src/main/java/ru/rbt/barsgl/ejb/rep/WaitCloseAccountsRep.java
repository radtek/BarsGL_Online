package ru.rbt.barsgl.ejb.rep;

import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.ejbcore.datarec.DataRecord;

import javax.ejb.EJB;
import java.sql.SQLException;
import java.util.List;

import static ru.rbt.shared.ExceptionUtils.getErrorMessage;

/**
 * Created by er22317 on 22.03.2018.
 */
public class WaitCloseAccountsRep {
    @EJB
    private CoreRepository repository;

    public RpcRes_Base<Boolean> repWaitAcc(String begDate, String endDate, Boolean isAllAcc) {
        try{
            String query = String.format("select 1 from GL_ACWAITCLOSE %s", isAllAcc? "":"where OPERDAY BETWEEN TO_DATE('"+begDate+"', 'YYYY-MM-DD') AND TO_DATE('"+endDate+"', 'YYYY-MM-DD')");
            if ( null == repository.selectFirst(query, null)) {
                return new RpcRes_Base<>(false, true, "Нет данных для отчета");
            }
            return new RpcRes_Base<>(true, false, "");
        }
        catch (Exception e){
            String errMessage = getErrorMessage(e);
            return new RpcRes_Base<>(false, true, errMessage);
        }
    }

}
