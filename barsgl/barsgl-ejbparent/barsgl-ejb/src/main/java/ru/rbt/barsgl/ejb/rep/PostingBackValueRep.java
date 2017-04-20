package ru.rbt.barsgl.ejb.rep;

import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.shared.RpcRes_Base;

import static ru.rbt.barsgl.shared.ExceptionUtils.getErrorMessage;
import javax.ejb.EJB;
import java.util.List;

/**
 * Created by akichigi on 14.04.17.
 */
public class PostingBackValueRep {
    @EJB
    private CoreRepository repository;

    private final String sql = "select procdate from gl_oper where procdate = '%s' fetch first 1 row only";

    public RpcRes_Base<Boolean> operExists(String date){
        try{
            String query = String.format(sql, date);
            List<DataRecord> records =  repository.select(query);
            if (records == null || records.size() == 0) {
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
