package ru.rbt.gwt.security.ejb.monitoring;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.monitoring.OperTableItem;
import ru.rbt.barsgl.shared.monitoring.ReplTableItem2;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;

import javax.ejb.EJB;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static ru.rbt.audit.entity.AuditRecord.LogCode.Monitoring;
import static ru.rbt.ejbcore.util.DateUtils.dateTimeString;
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

//    private final String repl_sql =
//            "select total_error, total_wait, total_done, total_unknown " +
//            "from V_GL_MON_REPL";
//
//    private final String repl_table_sql =
//            "select table_name, type, status, cnt " +
//            "from V_GL_MON_REPLT " +
//            "order by table_name, type, status";

    private final String repl_sql = "SELECT TABLE_NAME, "+
                                "count(case when IS_PROCESSED=0 then 1 else null end) is_wait, "+
                                "count(case when IS_PROCESSED=1 then 1 else null end) is_proc, "+
                                "count(case when IS_PROCESSED=-1 then 1 else null end) is_err "+
                                "FROM BARS_JRN GROUP BY TABLE_NAME order by TABLE_NAME ";

    private final String oper_sql =
            "select state, fan, cnt " +
            "from V_GL_MON_OPER " +
            "order by fan, state";

    public RpcRes_Base<HashMap> getBuff(Date oldDate, Integer oldPdMoved, Integer oldBltMoved){
       HashMap ret = new HashMap();
       try {
           long intervalMillisec = 0;
           DataRecord record = repository.selectOne(pd_sql);
           Integer newPdMoved = record.getInteger("total_moved");
           Integer newPdWait = record.getInteger("total_wait");
           ret.put("pd_Total", String.valueOf(newPdWait + newPdMoved));
           ret.put("pd_Wait", String.valueOf(newPdWait));
           ret.put("pd_Moved", String.valueOf(newPdMoved));
           record = repository.selectOne(baltur_sql);
           Integer newBltMoved = record.getInteger("total_moved");
           Integer newBltWait = record.getInteger("total_wait");
           ret.put("blt_Total", String.valueOf(newBltWait + newBltMoved));
           ret.put("blt_Wait", String.valueOf(newBltWait));
           ret.put("blt_Moved", String.valueOf(newBltMoved));

           Date newDate = new Date();
           if (oldDate == null) oldDate = newDate;
           if (oldDate.compareTo( newDate ) != 0){
              intervalMillisec = newDate.getTime() - oldDate.getTime();
           }

           ret.put("strInterval", DateUtils.formatElapsedTimeOver24h(intervalMillisec));
           if (oldPdMoved == null) {
              oldPdMoved = newPdMoved;
              oldBltMoved = newBltMoved;
           }

           ret.put("moniOldBuffDate", newDate);
           ret.put("strNewDate", dateTimeString(newDate));
           ret.put("strPdSpeed", intervalMillisec == 0 ? "?" :String.format("%1$,.0f", (double)(Math.abs(oldPdMoved - newPdMoved) / intervalMillisec * 1000)  ));
           ret.put("strBltSpeed", intervalMillisec == 0 ? "?" :String.format("%1$,.0f", (double) (Math.abs(oldBltMoved - newBltMoved) / intervalMillisec * 1000)));
           ret.put("strPdRest", intervalMillisec == 0 ? "?" : DateUtils.formatElapsedTimeOver24h( (long) (newPdWait * Math.abs(oldPdMoved - newPdMoved) * 1000/ intervalMillisec)) );
           ret.put("strBltRest", intervalMillisec == 0 ? "?" : DateUtils.formatElapsedTimeOver24h( (long) (newBltWait * Math.abs(oldBltMoved - newBltMoved) * 1000/ intervalMillisec)) );
//           if (!oldDate.equals( newDate )){
//               intervalMillisec = newDate.getTime() - oldDate.getTime();
//               ret.put("strInterval", DateUtils.formatElapsedTimeOver24h(intervalMillisec));
//               ret.put("interval", new Long(intervalMillisec));
//           }
           return new RpcRes_Base<>(ret, false, "");
       }catch (Exception e){
            String errMessage = getErrorMessage(e);
            auditController.error(Monitoring, "Ошибка получения данных для буффера", null, e);
            return new RpcRes_Base<>(null, true, errMessage);
       }
    }

    public RpcRes_Base<ArrayList> getOper(){
        ArrayList<OperTableItem> ret = new ArrayList<OperTableItem>();
        try {
            List<DataRecord> records = repository.select(oper_sql);
            if (records != null) {
                records.stream().forEach(r -> ret.add(new OperTableItem(r.getString("state"),
                        r.getString("fan"), r.getInteger("cnt"))));
            }
            return new RpcRes_Base<>(ret, false, "");
        }catch (Exception e){
            String errMessage = getErrorMessage(e);
            auditController.error(Monitoring, "Ошибка получения данных для операций", null, e);
            return new RpcRes_Base<>(null, true, errMessage);
        }
    }

    public RpcRes_Base<HashMap> getRepl(Date oldDate, ArrayList<ReplTableItem2> oldRepl){
        HashMap ret = new HashMap();
        ArrayList<ReplTableItem2> repl = new ArrayList<ReplTableItem2>();
        try {
            List<DataRecord> records = repository.select(repl_sql);
            long intervalMillisec = 0;
            Date newDate = new Date();
            if (oldDate == null) oldDate = newDate;
            if (oldDate.compareTo( newDate ) != 0){
                intervalMillisec = newDate.getTime() - oldDate.getTime();
            }

            if (records != null) {
                records.stream().forEach(r -> repl.add(new ReplTableItem2(r.getString("table_name"),
                        r.getInteger("is_proc"), r.getInteger("is_wait"), r.getInteger("is_err"))));
            }
            for(int i = 0; i < repl.size(); i++){
                ReplTableItem2 oldItem = getItem( repl.get(i).getName(), oldRepl);
                if (oldItem == null) {
                    repl.get(i).setStrSpeed("0");
                    repl.get(i).setStrRestTime("0");
                }else{
                    double speed = intervalMillisec == 0 ? -1 :Math.abs(oldItem.getCntProc() - repl.get(i).getCntProc()) * 1000/ intervalMillisec;
//                    repl.get(i).setStrSpeed(speed == -1 ? "?" : String.format("%1$,.2f",speed));
                    repl.get(i).setStrSpeed(speed == -1 ? "?" : String.valueOf(speed));
                    repl.get(i).setStrRestTime(speed == -1 || speed == 0 ? "?": DateUtils.formatElapsedTimeOver24h( (long) (repl.get(i).getCntWait() / speed * 1000) ));
                }
            }
            ret.put("repl", repl);
            ret.put("strNewDate", dateTimeString(newDate));
            ret.put("moniOldReplDate", newDate);
            ret.put("strInterval", DateUtils.formatElapsedTimeOver24h(intervalMillisec));
            return new RpcRes_Base<>(ret, false, "");
        }catch (Exception e){
            String errMessage = getErrorMessage(e);
            auditController.error(Monitoring, "Ошибка получения данных для репликаций", null, e);
            return new RpcRes_Base<>(null, true, errMessage);
        }
    }

    ReplTableItem2 getItem(String tableName, List<ReplTableItem2> l){
        if (l == null) return null;
		for(int i = 0; i < l.size(); i++){
		   if (l.get(i).getName().equals(tableName)) return l.get(i);
		}
		return null;
	}

//    public RpcRes_Base<MonitoringWrapper> getInfo(){
//        try{
//            MonitoringWrapper wrapper = new MonitoringWrapper();
//
//            DataRecord record = repository.selectOne(pd_sql);
//            BufferItem item = new BufferItem(record.getInteger("total_wait"), record.getInteger("total_moved"));
//            wrapper.setPd(item);
//
//            record = repository.selectOne(baltur_sql);
//            item = new BufferItem(record.getInteger("total_wait"), record.getInteger("total_moved"));
//            wrapper.setBaltur(item);
//
//            record = repository.selectOne(repl_sql);
//            ReplItem replItem = new ReplItem(record.getInteger("total_wait"), record.getInteger("total_error"),
//                    record.getInteger("total_done"), record.getInteger("total_unknown"));
//            wrapper.setReplTotal(replItem);
//
//            List<DataRecord> records = repository.select(repl_table_sql);
//            if (records != null){
//                List<ReplTableItem> list = new ArrayList<>();
//                records.stream().forEach(r -> list.add(new ReplTableItem(r.getString("table_name"),
//                        r.getString("type"), r.getString("status"), r.getInteger("cnt"))));
//
//                wrapper.setReplList(list);
//            }
//
//            records = repository.select(oper_sql);
//            if (records != null){
//                List<OperTableItem> list = new ArrayList<>();
//                records.stream().forEach(r -> list.add(new OperTableItem(r.getString("state"),
//                        r.getString("fan"), r.getInteger("cnt"))));
//                wrapper.setOperList(list);
//            }
//
//            return new RpcRes_Base<>(wrapper, false, "");
//        }
//        catch (Exception e){
//            String errMessage = getErrorMessage(e);
//            auditController.error(Monitoring, "Ошибка получения данных для мониторинга", null, e);
//            return new RpcRes_Base<>(null, true, errMessage);
//        }
//    }
}
