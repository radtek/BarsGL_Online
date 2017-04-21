package ru.rbt.gwt.security.ejb.monitoring;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.monitoring.*;

import static ru.rbt.audit.entity.AuditRecord.LogCode.Monitoring;

import javax.ejb.EJB;
import java.util.ArrayList;
import java.util.List;

import static ru.rbt.shared.ExceptionUtils.getErrorMessage;

/**
 * Created by akichigi on 06.12.16.
 */
public class MonitoringController {
    @EJB
    private CoreRepository repository;
    @EJB
    private AuditController auditController;

    private final String pd_sql =
            "select total_moved, total_wait " +
            "from V_GL_MON_PD";

    private final String baltur_sql =
            "select total_moved, total_wait " +
            "from V_GL_MON_BALT";

    private final String repl_sql =
            "select total_error, total_wait, total_done, total_unknown " +
            "from V_GL_MON_REPL";

    private final String repl_table_sql =
            "select table_name, type, status, cnt " +
            "from V_GL_MON_REPLT " +
            "order by table_name, type, status";

    private final String oper_sql =
            "select state, fan, cnt " +
            "from V_GL_MON_OPER " +
            "order by fan, state";

    public RpcRes_Base<MonitoringWrapper> getInfo(){
        try{
            MonitoringWrapper wrapper = new MonitoringWrapper();

            DataRecord record = repository.selectOne(pd_sql);
            BufferItem item = new BufferItem(record.getInteger("total_wait"), record.getInteger("total_moved"));
            wrapper.setPd(item);

            record = repository.selectOne(baltur_sql);
            item = new BufferItem(record.getInteger("total_wait"), record.getInteger("total_moved"));
            wrapper.setBaltur(item);

            record = repository.selectOne(repl_sql);
            ReplItem replItem = new ReplItem(record.getInteger("total_wait"), record.getInteger("total_error"),
                    record.getInteger("total_done"), record.getInteger("total_unknown"));
            wrapper.setReplTotal(replItem);

            List<DataRecord> records = repository.select(repl_table_sql);
            if (records != null){
                List<ReplTableItem> list = new ArrayList<>();
                records.stream().forEach(r -> list.add(new ReplTableItem(r.getString("table_name"),
                        r.getString("type"), r.getString("status"), r.getInteger("cnt"))));

                wrapper.setReplList(list);
            }

            records = repository.select(oper_sql);
            if (records != null){
                List<OperTableItem> list = new ArrayList<>();
                records.stream().forEach(r -> list.add(new OperTableItem(r.getString("state"),
                        r.getString("fan"), r.getInteger("cnt"))));
                wrapper.setOperList(list);
            }

            return new RpcRes_Base<>(wrapper, false, "");
        }
        catch (Exception e){
            String errMessage = getErrorMessage(e);
            auditController.error(Monitoring, "Ошибка получения данных для мониторинга", null, e);
            return new RpcRes_Base<>(null, true, errMessage);
        }
    }
}
