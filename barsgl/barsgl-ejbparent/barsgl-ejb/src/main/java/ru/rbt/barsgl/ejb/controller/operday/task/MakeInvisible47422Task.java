package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.repository.Reg47422JournalRepository;
import ru.rbt.barsgl.ejb.repository.dict.ClosedPeriodCashedRepository;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.MakeInvisible47422;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.controller.operday.task.MakeInvisible47422Task.PstSide.C;
import static ru.rbt.barsgl.ejb.controller.operday.task.MakeInvisible47422Task.PstSide.D;
import static ru.rbt.barsgl.ejb.controller.operday.task.MakeInvisible47422Task.PstSide.N;
import static ru.rbt.barsgl.ejb.props.PropertyName.REG47422_DEPTH;
import static ru.rbt.ejbcore.validation.ErrorCode.REG47422_ERROR;

/**
 * Created by er18837 on 20.08.2018.
 */
public class MakeInvisible47422Task extends AbstractJobHistoryAwareTask {

    private static final String myTaskName = CloseLwdBalanceCutTask.class.getSimpleName();

    public static final String REG47422_DEPTH_KEY = "depth";
    public static final String REG47422_CRPRD_KEY = "withClosedPeriod";

    private final int REG47422_DEF_DEPTH = 4;

    enum PstSide {D, C, N};

    @EJB
    private Reg47422JournalRepository journalRepository;

    @EJB
    private PropertiesRepository propertiesRepository;

    @EJB
    private AuditController auditController;

    @EJB
    private ClosedPeriodCashedRepository closedPeriodRepository;

    @Inject
    private BankCalendarDayRepository calendarDayRepository;

    @Override
    protected void initExec(String jobName, Properties properties) throws Exception {

    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        Operday operday = operdayController.getOperday();
        if (ONLINE != operday.getPhase()) {
            auditController.warning(AuditRecord.LogCode.Operday, format("Нельзя запустить задачу '%s': опердень в статусе '%s'"
                    , myTaskName, operday.getPhase().name() ));
            return false;
        }

//        final Date currentDateTime = operdayController.getSystemDateTime();
//        final Date currentDate = DateUtils.onlyDate(currentDateTime);

        checkAlreadyRunning(myTaskName, properties);
        return true;
    }

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        // определить дату начала dateFrom (POD >= dateFrom && POD <= lwdate, PROCDATE is null or PROCDATE <= lwdate)
        Operday operday = operdayController.getOperday();
        Date dateFrom = getDateFrom(operday.getCurrentDate(), properties);
        // обновить GL_REG47422
        int cntLoad = loadNewData(dateFrom);
        if (cntLoad == 0)
            return true;
        /* TODO
        склеить проводки с одной датой
            одиночные
            веера
        обработать проводки с разной датой
            одиночные
            веера
        */

        return false;
    }

    /**
     * Определяет дату начала обработки (из таблицы настроек и свойст задачи)
     * @param curdate
     * @param properties
     * @return
     * @throws SQLException
     */
    private Date getDateFrom(Date curdate, Properties properties) throws SQLException {
        int depth = REG47422_DEF_DEPTH;
        String depthProp = properties.getProperty(REG47422_DEPTH_KEY);
        if (null != depthProp) {
            try {
                depth = Integer.parseInt(depthProp);
                if (depth < 1 || depth > 30)
                    throw new NumberFormatException("Значение должно быть от 1 до 30");
            } catch (NumberFormatException e) {
                ValidationError error = new ValidationError(REG47422_ERROR, String.format("Неверное значение %s в свойствах задачи: %s %s", REG47422_DEPTH_KEY, depthProp, e.getMessage()));
                auditController.error(MakeInvisible47422, "Ошибка при выполнении задачи " + myTaskName, null, error);
            }
        }
        else {
            depth = (int)(long)propertiesRepository.getNumberDef(REG47422_DEPTH.name(), (long)REG47422_DEF_DEPTH);
        }
        Date dateFrom = calendarDayRepository.getWorkDateBefore(curdate, depth, false);
        boolean withClosedPeriod = Boolean.valueOf(properties.getProperty(REG47422_CRPRD_KEY));
        if (!withClosedPeriod) {
            Date ldate = closedPeriodRepository.getPeriod().getLastDate();
            dateFrom = dateFrom.after(ldate) ? dateFrom : calendarDayRepository.getWorkDateAfter(ldate, false);
        }
        return dateFrom;
    }

    /**
     * Заполняет журнал новыми и измененными данными
     * @param dateFrom
     * @return
     * @throws Exception
     */
    private int loadNewData(Date dateFrom) throws Exception {
        int res = journalRepository.executeInNewTransaction(persistence -> {
            // найти проводки с отличием, проапдейтить VALID = 'U'
            int changed = journalRepository.findChangedPst();
            // вставить измененные записи (STATE = 'CHANGE') и новын записи (STATE = 'LOAD')
            int inserted = journalRepository.insertNewAndChangedPst(dateFrom);
            if (changed > 0) {
                // статус измененных = 'N'
                Assert.isTrue(changed == journalRepository.updateChangedPstOld(), String.format("Ошибка при изменении статуса, ожидается записей: %в", changed));
            }
            return inserted;
        });
        auditController.info(MakeInvisible47422, "Вставлено новых / измененных записей в GL_REG47422: " + res);
        return res;
    }

    private String getGroupSql(boolean withSum, boolean withPod) {
        String strPod = withPod ? ", pod" : "";
        String strSum = withSum ? ", abs(amnt)" : "";
        return "with glued as\n" +
                "  (select \n" +
                "        ndog, acid, sum(amnt) flag,\n" +
                "        sum(case when amnt<0 then amnt else 0 end) sum_dr,\n" +
                "        sum(case when amnt>0 then amnt else 0 end) sum_cr,\n" +
                "        sum(case when amnt<0 then 1 else 0 end) cnt_dr,\n" +
                "        sum(case when amnt>0 then 1 else 0 end) cnt_cr,\n" +
                "        listagg (case when amnt<0 then pbr else null end,',') within group (order by pd_id) pbr_dr,\n" +
                "        listagg (case when amnt>0 then pbr else null end,',') within group (order by pd_id) pbr_cr,\n" +
                "        listagg (case when amnt<0 then pcid else null end,',') within group (order by pd_id) pcid_dr,\n" +
                "        listagg (case when amnt>0 then pcid else null end,',') within group (order by pd_id) pcid_cr,\n" +
                "        listagg (case when amnt<0 then pd_id else null end,',') within group (order by pd_id) id_dr,\n" +
                "        listagg (case when amnt>0 then pd_id else null end,',') within group (order by pd_id) id_cr,\n" +
                "        listagg (case when amnt<0 then pod else null end,',') within group (order by pd_id) pod_dr,\n" +
                "        listagg (case when amnt>0 then pod else null end,',') within group (order by pd_id) pod_cr\n" +
                "        " + strPod + "\n" +
                "  from GL_REG47422 \n" +
                "    where  valid = 'Y' \n" +
                "        and state not like 'PROC%'\n" +
                "        and pod > ?\n" +
                "  group by acid, ndog" + strPod + strSum + "\n" +
                "  )\n" +
                "select * from glued\n" +
                "where flag=0 and (cnt_cr=1 or cnt_dr=1)\n" +
                "order by cnt_cr, cnt_dr" + strPod + ", ndog";
    }

    private int glueEqualDate(boolean withSum, Date dateFrom) {
        String sql = getGroupSql(withSum, false);
        try {
            List<DataRecord> glueList = journalRepository.select(sql, dateFrom);    // наборы для склейки
            for (DataRecord glued: glueList) {
                // сторона PH
                PstSide phSide = getPaymentHubSide(new String[]{glued.getString("pbr_dr"), glued.getString("pbr_cr")});
                if (N == phSide) {
                    // совпадение PBR, записать статус ERRSRC
                    continue;
                }
                String pcid =  getPcid(phSide,
                        new int[]{glued.getInteger("cnt_dr"), glued.getInteger("cnt_cr")},
                        new String[]{glued.getString("pcid_dr"), glued.getString("pcid_cr")});
                Reg47422params params = fillCommonFields(phSide, new String[]{glued.getString("id_dr"), glued.getString("id_cr")});


            }
            return glueList.size();
        } catch (SQLException e) {
            auditController.error(MakeInvisible47422, "Ошибка при обработке проводок по счетам 47422", null, e);
            return 0;
        }
    }

    private PstSide getPaymentHubSide(String[] pbrList) {
        int indOne = pbrList[0].length() < pbrList[1].length() ? 0 : 1; // индекс стороны, где одна проводка
        if (pbrList[indOne^1].indexOf(pbrList[indOne]) >= 0)            // PBR по дб и кр совпадают - ошибка
            return N;
        return pbrList[0].indexOf("@@GL-PH")>=0 || pbrList[0].indexOf("@@IF") >=0 ? D : C;  // сторона PH или FCC6
    }

    private String getPcid(PstSide phSide, int[] cnt, String[] pcidList) {
        PstSide pcidSide = cnt[0] > 1 ? C : cnt[1] > 1 ? D : phSide;
        return pcidList[pcidSide.ordinal()];
    }

    private Reg47422params fillCommonFields(PstSide phSide, String[] idList) throws SQLException {
        int indPh = D == phSide ? 0 : 1;
        Reg47422params reg = new Reg47422params();
        // параметры PH
        DataRecord recPh = journalRepository.selectFirst("select PREF, PMT_REF from PST where id in (" + idList[indPh] + ") order by id");
        reg.pref = recPh.getString(0);
        reg.pmtref = recPh.getString(1);
        // параметры Flex
        DataRecord recFcc = journalRepository.selectFirst("select PBR, DEAL_ID, PNAR, RNARLNG from PST where id in (" + idList[indPh^1] + ") order by abs(amnt) desc");
        reg.pbr = recFcc.getString(0);
        reg.dealId = recFcc.getString(1);
        reg.pnar = recFcc.getString(2);
        reg.rnarlng = recFcc.getString(3);
        return reg;
    }

    private class Reg47422params {
        private String pref;
        private String pmtref;
        private String pbr;
        private String dealId;
        private String pnar;
        private String rnarlng;
    }
}
