package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.entity.acc.GLAccParam;
import ru.rbt.barsgl.ejb.entity.gl.Memorder;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejb.integr.pst.MemorderController;
import ru.rbt.barsgl.ejb.repository.Reg47422JournalRepository;
import ru.rbt.barsgl.ejb.repository.dict.ClosedPeriodCashedRepository;
import ru.rbt.barsgl.shared.HasLabel;
import ru.rbt.barsgl.shared.enums.Reg47422State;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.BackgroundLocalization;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Exclude47422;
import static ru.rbt.barsgl.ejb.common.controller.od.Rep47422Controller.REG47422_DEF_DEPTH;
import static ru.rbt.barsgl.ejb.common.controller.od.Rep47422Controller.REG47422_DEPTH;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;
import static ru.rbt.barsgl.ejb.controller.operday.task.Exclude47422Task.GlueMode.Glue;
import static ru.rbt.barsgl.ejb.controller.operday.task.Exclude47422Task.GlueMode.Load;
import static ru.rbt.barsgl.ejb.controller.operday.task.Exclude47422Task.GlueMode.Standard;
import static ru.rbt.barsgl.ejb.controller.operday.task.Exclude47422Task.PstSide.C;
import static ru.rbt.barsgl.ejb.controller.operday.task.Exclude47422Task.PstSide.D;
import static ru.rbt.barsgl.ejb.controller.operday.task.Exclude47422Task.PstSide.N;
import static ru.rbt.barsgl.ejb.entity.gl.Memorder.DocType.BANK_ORDER;
import static ru.rbt.barsgl.shared.enums.Reg47422State.CHANGE;
import static ru.rbt.barsgl.shared.enums.Reg47422State.LOAD;
import static ru.rbt.barsgl.shared.enums.Reg47422State.WT47416;
import static ru.rbt.ejbcore.validation.ErrorCode.EXCLUDE_47422_ERROR;

/**
 * Created by er18837 on 20.08.2018.
 */
public class Exclude47422Task extends AbstractJobHistoryAwareTask {

    private final String myTaskName = this.getClass().getSimpleName();

    public static final String REG47422_DEPTH_KEY = "depth";
    public static final String REG47422_CRPRD_KEY = "withClosedPeriod";
    public static final String REG47422_MODE_KEY = "mode";


    public enum PstSide {D, C, N};
    public enum GlueMode implements HasLabel{
        Standard("Загрузка и обработка"),
        Load("Загрузка новых/измененных"),
        Glue("Обработка с заданной даты"),
        Full("Загрузка и обработка (даже без изменений)")            ;

        private String label;

        GlueMode(String label) {
            this.label = label;
        }

        @Override
        public String getLabel() {
            return label;
        }

    };

    @EJB
    private Reg47422JournalRepository journalRepository;

    @EJB
    private PropertiesRepository propertiesRepository;

    @EJB
    private ClosedPeriodCashedRepository closedPeriodRepository;

    @EJB
    private MemorderController memorderController;

    @Inject
    private BankCalendarDayRepository calendarDayRepository;

    @Override
    protected void initExec(String jobName, Properties properties) throws Exception {

    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        Operday operday = operdayController.getOperday();
        if (ONLINE != operday.getPhase()) {
            auditController.warning(AuditRecord.LogCode.Operday, format("Нельзя запустить задачу '%s': опердень в статусе '%s', требуется '%s'"
                    , jobName, operday.getPhase().name(), ONLINE.name() ));
            return false;
        }
        if (DIRECT != operday.getPdMode()) {
            auditController.warning(AuditRecord.LogCode.Operday, format("Нельзя запустить задачу '%s': режим обработки '%s', требуется '%s'"
                    , jobName, operday.getPdMode().name(), DIRECT.name() ));
            return false;
        }

        return true;
    }

    @Override
    protected boolean checkJobStatus(String jobName, Properties properties) {
        try {
            return checkAlreadyRunning(jobName, properties);
        } catch (ValidationError e) {
            auditController.warning(Exclude47422, format("Задача %s не выполнена", jobName), null, e);
            return false;
        }
    }

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        // определить дату начала dateFrom (POD >= dateFrom && POD <= lwdate, PROCDATE is null or PROCDATE <= lwdate)
        Operday operday = operdayController.getOperday();
        Date dateFrom = getDateFrom(operday.getCurrentDate(), properties);
        try {
            GlueMode mode = GlueMode.valueOf(Optional.ofNullable(properties.getProperty(REG47422_MODE_KEY)).orElse(Standard.name()));
            auditController.info(Exclude47422, String.format("Запуск задачи %s в режиме %s(%s) с даты '%s' по дату '%s'",
                    myTaskName, mode.name(), mode.getLabel(), dateUtils.onlyDateString(dateFrom),
                    dateUtils.onlyDateString(operday.getLastWorkingDay())));
            // обновить GL_REG47422
            int cntLoad = 0;
            if (mode != Glue) {
                cntLoad = loadNewData(dateFrom);
            }
            if (mode == Load) {     //  || (mode == Standard && cntLoad == 0)
                auditController.info(Exclude47422, String.format("Окончание выполнения задачи %s (загрузка данных)", myTaskName));
                return true;
            }

            // склеить проводки с одной датой: одиночные, веера
            processEqualDates(true, dateFrom);
            processEqualDates(false, dateFrom);

            // обработать проводки с разной датой: одиночные, веера
            processDiffDates(true, dateFrom);
            processDiffDates(false, dateFrom);

            auditController.info(Exclude47422, String.format("Окончание выполнения задачи %s", myTaskName));
            return true;
        } catch (IllegalArgumentException e) {
            auditController.warning(Exclude47422, String.format("Ошибка при выполнении задачи %s", myTaskName), null, e);
            return false;
        }
    }

    public boolean testExec(JobHistory jobHistory, Properties properties) throws Exception {
        return execWork(jobHistory, properties);
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
                ValidationError error = new ValidationError(EXCLUDE_47422_ERROR, String.format("Неверное значение %s в свойствах задачи: %s %s", REG47422_DEPTH_KEY, depthProp, e.getMessage()));
                auditController.error(Exclude47422, "Ошибка при выполнении задачи " + myTaskName, null, error);
            }
        }
        else {
            depth = getDepthDef();
        }
        Date dateFrom = calendarDayRepository.getWorkDateBefore(curdate, depth, false);
        boolean withClosedPeriod = Boolean.valueOf(properties.getProperty(REG47422_CRPRD_KEY));
        if (!withClosedPeriod) {
            Date ldate = closedPeriodRepository.getPeriod().getLastDate();
            dateFrom = dateFrom.after(ldate) ? dateFrom : calendarDayRepository.getWorkDateAfter(ldate, false);
        }
        return dateFrom;
    }

    private int getDepthDef() {
        int depth = (int)(long)propertiesRepository.getNumberDef(REG47422_DEPTH, (long)REG47422_DEF_DEPTH);
        return depth > 0 ? depth : 1;
    }

    private String joinLists(String list1, String list2) {
        return list1 + "," + list2;
    }
    /**
     * Заполняет журнал новыми и измененными данными
     * @param dateFrom
     * @return
     * @throws Exception
     */
    private int loadNewData(Date dateFrom) throws Exception {
        try {
            int[] res = journalRepository.executeInNewTransaction(persistence -> {
                // найти проводки с отличием, проапдейтить VALID = 'U'
                int changed = journalRepository.markChangedPst();
                // вставить измененные записи (STATE = 'CHANGE') и новын записи (STATE = 'LOAD')
                int inserted = journalRepository.insertNewAndChangedPst(dateFrom);
                if (changed > 0) {
                    // статус измененных = 'N'
                    Assert.isTrue(changed == journalRepository.updateChangedPstOld(), String.format("Ошибка при изменении статуса, ожидается записей: %d", changed));
                }
                return new int[] {inserted - changed, changed};
            });
            auditController.info(Exclude47422, String.format("Вставлено записей в GL_REG47422: новых %d, измененных %d", res[0], res[1]));
            return res[0] + res[1];
        } catch (Exception e) {
            auditController.error(Exclude47422, "Ошибка при поиске проводок по счетам 47422", null, e);
            return 0;
        }
    }

    /**
     * обработка проводок с одинаковой датой
     * @param withSum = true - одиночные проводки, иначе веер
     * @param dateFrom - начиная с даты
     * @return
     */
    private int processEqualDates(boolean withSum, Date dateFrom) {
        try {
            int cnt = 0;
            List<DataRecord> glueList = journalRepository.getGroupedList(dateFrom, withSum, true, LOAD, CHANGE);    // наборы для склейки
            for (DataRecord glued: glueList) {
                // сторона PH
                PstSide phSide = getPaymentHubSide(new String[]{glued.getString("pbr_dr"), glued.getString("pbr_cr")});
                if (N == phSide) {  // совпадение PBR
                    journalRepository.executeInNewTransaction(persistence -> {
                        journalRepository.updateState(glued.getString("id_reg"), Reg47422State.SKIP_SRC);
                        return null;
                    });
                    continue;
                }
                // сторона ручки веера
                PstSide stickSide = glued.getInteger("cnt_dr") > 1 ? C : glued.getInteger("cnt_cr") > 1 ? D : N;
                boolean ok = processGlue(glued, phSide, stickSide);
                if(ok) cnt++;
            }
            auditController.info(Exclude47422, String.format("Выбрано записей для обработки в одной дате (%s): %d, успешно обработано: %d",
                    withSum ? "1:1" : "веера", glueList.size(), cnt));
            return cnt;
        } catch (Exception e) {
            auditController.error(Exclude47422, "Ошибка при обработке проводок по счетам 47422", null, e);
            return 0;
        }
    }

    private boolean processGlue(DataRecord glued, PstSide phSide, PstSide stickSide) throws Exception {
        try {
            // сторона PCID
            PstSide pcidSide = stickSide != N ? stickSide : phSide;
            Reg47422params params = fillGlueParams(phSide, stickSide, pcidSide,
                    new String[]{glued.getString("pcid_dr"), glued.getString("pcid_cr")},
                    new String[]{glued.getString("pdid_dr"), glued.getString("pdid_cr")},
                    new String[]{glued.getString("pbr_dr"), glued.getString("pbr_cr")},
                    new String[]{glued.getString("pmt_dr"), glued.getString("pmt_cr")}
            );
            return journalRepository.executeInNewTransaction(persistence -> {
                journalRepository.gluePostings(params.idInvisible, params.idVisible, params.pcidNew, params.pbr);
                updateOperations(stickSide, new String[] {glued.getString("glo_dr"), glued.getString("glo_cr")}, params);
                if (phSide != pcidSide) {
                    // проверить текущее значение поля BO_IND в таблице PCID_MO в случае, если BO_IND = 0, на соответствие значению
                    reviseDocType(params);
                }
                journalRepository.updateProcGL(glued.getString("id_reg"), params.pcidNew);
                return true;
            });
        } catch (Exception e) {
            auditController.error(Exclude47422, "Ошибка при обработке проводок по счетам 47422", null, e);
            journalRepository.executeInNewTransaction(persistence -> {
                journalRepository.updateErrProc(glued.getString("id_reg"));
                return null;
            });
            return false;
        }
    }

    /**
     * обработка проводок с одинаковой датой
     * @param withSum = true - одиночные проводки, иначе веер
     * @param dateFrom - начиная с даты
     * @return
     */
    private int processDiffDates(boolean withSum, Date dateFrom) {
        try {
            int cnt = 0;
            List<DataRecord> glueList = journalRepository.getGroupedList(dateFrom, withSum, false, LOAD, CHANGE, WT47416);    // наборы для склейки
            for (DataRecord glued: glueList) {
                // сторона PH
                PstSide phSide = getPaymentHubSide(new String[]{glued.getString("pbr_dr"), glued.getString("pbr_cr")});
                if (N == phSide) {  // совпадение PBR
                    journalRepository.executeInNewTransaction(persistence -> {
                        journalRepository.updateState(glued.getString("id_reg"), Reg47422State.SKIP_SRC);
                        return null;
                    });
                    continue;
                }
                // сторона ручки веера
                PstSide stickSide = glued.getInteger("cnt_dr") > 1 ? C : glued.getInteger("cnt_cr") > 1 ? D : N;
                boolean ok = processChangeAcc(glued, phSide, stickSide);
                if(ok) cnt++;
            }
            auditController.info(Exclude47422, String.format("Выбрано записей для обработки в разных датах (%s): %d, успешно обработано: %d",
                    withSum ? "1:1" : "веера", glueList.size(), cnt));
            return glueList.size();
        } catch (Exception e) {
            auditController.error(Exclude47422, "Ошибка при обработке проводок по счетам 47422", null, e);
            return 0;
        }
    }

    private boolean processChangeAcc(DataRecord glued, PstSide phSide, PstSide stickSide) throws Exception {
        // определить счет на замену
        String acid = glued.getString("acid");
        GLAccParam ac47416 = journalRepository.getAccount47416(acid);
        if (null == ac47416) {    // нет счета на подмену
            auditController.error(Exclude47422, "Ошибка при обработке проводок по счетам 47422", null,
                    new ValidationError(EXCLUDE_47422_ERROR, String.format("Не найден счет с ACC2='47416' для замены счета c ACC2='47422' , ACID = '%s'", acid)));
            journalRepository.executeInNewTransaction(persistence -> {
                journalRepository.updateState(glued.getString("id_reg"), WT47416);
                return null;
            });
            return false;
        }
        return journalRepository.executeInNewTransaction(persistence -> {
            String pcidList = joinLists(glued.getString("pcid_dr"), glued.getString("pcid_cr"));
            PstSide pcidSide = stickSide != N ? stickSide : C;
            journalRepository.replace47416(pcidList, joinLists(glued.getString("pdid_dr"), glued.getString("pdid_cr")), ac47416);
            journalRepository.updateProcAcc(glued.getString("id_reg"), pcidList, glued.getString(pcidSide == D ? "pcid_dr" : "pcid_cr"));
            return true;
        });
    }

    /**
     * определить сторону PH
     * ошибка при совпадении источников
     * @param pbrList
     * @return
     */
    private PstSide getPaymentHubSide(String[] pbrList) {
        int indOne = pbrList[0].length() < pbrList[1].length() ? 0 : 1; // индекс стороны, где одна проводка
        if (pbrList[indOne^1].indexOf(pbrList[indOne]) >= 0)            // PBR по дб и кр совпадают - ошибка
            return N;
        return pbrList[0].indexOf("@@GL-PH")>=0 || pbrList[0].indexOf("@@IF") >=0 ? D : C;  // сторона PH или FCC6
    }

    /**
     * заполняет параметры для склейки проводок
     * @param phSide
     * @param pcidList
     * @param idList
     * @return
     * @throws SQLException
     */
    private Reg47422params fillGlueParams(PstSide phSide, PstSide stickSide, PstSide pcidSide,
                                          String[] pcidList, String[] idList, String[] pbrList, String[] pmtList) throws SQLException {
        Reg47422params reg = new Reg47422params();
        reg.pcidNew = pcidList[pcidSide.ordinal()];
        reg.pcidOld = joinLists(pcidList[0], pcidList[1]);
        reg.idInvisible = joinLists(idList[0], idList[1]);

        // парные полупроводки - останутся видимыми
        List<DataRecord> idData = journalRepository.select("select id from pst where pcid in (" + reg.pcidOld + ") and id not in (" + reg.idInvisible + ") order by pcid");
        reg.idVisible = idData.stream().map(data -> data.getString(0)).collect(Collectors.joining(",")); // StringUtils.arrayToString();

        int indFl = phSide.ordinal()^1;
        List<String> pbrStr = Arrays.asList(pbrList[indFl].split(","));
        reg.pbr = pbrStr.stream().filter(p -> p.equals("@@GLFCL")).findFirst().orElse(pbrStr.get(0));
/*      // пока убрали заполнение дополнительных полей
        int indPh = phSide.ordinal();
        // параметры PH
        DataRecord recPh = journalRepository.selectFirst("select PREF from PST where id in (" + idList[indPh] + ") order by id");
        reg.pref = recPh.getString(0);
        // параметры Flex
        DataRecord recFcc = journalRepository.selectFirst("select PBR, DEAL_ID, PNAR, RNARLNG from PST where id in (" + idList[indPh^1] + ") order by abs(amnt) desc");
        reg.pbr = recFcc.getString(0);
        reg.dealId = recFcc.getString(1);
        reg.pnar = recFcc.getString(2);
        reg.rnarlng = recFcc.getString(3);
*/

        if (stickSide != N) {
            // связка веера
            reg.parRf = pmtList[stickSide.ordinal()];
        }
        return reg;
    }

    /**
     *  превратить операцию в веер
     * @param stickSide
     * @param gloList
     * @param params
     */
    private void updateOperations(PstSide stickSide, String[] gloList, Reg47422params params) {
        String gloAll = gloList[0] + "," + gloList[1];
        // INP_METHOD = 'AE_GL' - убрала, может испортиться склейка GL_OPER с GL_ETLPST и GL_BATPST
//        journalRepository.executeNativeUpdate("update GL_OPER set INP_METHOD = 'AE_GL' where GLOID in (" + gloAll + ")");
        if (stickSide == N)
            return;
        // только для веера
        String gloPar = gloList[stickSide.ordinal()];
        if (StringUtils.isEmpty(gloPar)) {
            auditController.warning(Exclude47422, "Ошибка при исключении проводок по техническим счетам 47422", "PST", params.pcidNew,
                    String.format("Не найдена родительская операция при склейке проводок в веер PCID = '%s'", params.pcidNew));
            return;
        }
        journalRepository.updateOperations(gloPar, gloAll, stickSide.name(), PstSide.values()[stickSide.ordinal()^1].name(), params.pcidNew);
    }

    /**
     * проверить / изменить DocType
     * @param params
     * @throws SQLException
     */
    private void reviseDocType(Reg47422params params) throws SQLException {
        Memorder memorder = journalRepository.selectFirst(Memorder.class, "from Memorder m where m.id = ?1", Long.valueOf(params.pcidNew));
        if (memorder.getDocType() == BANK_ORDER)
            return;
        List<Pd> pdDebit = journalRepository.select(Pd.class, "from Pd p where p.id in (" + params.idVisible + ") and p.amount < 0");
        List<Pd> pdCredit = journalRepository.select(Pd.class, "from Pd p where p.id in (" + params.idVisible + ") and p.amount > 0");
        Memorder.DocType docType = memorderController.getDocType(false, pdDebit, pdCredit, memorder.getPostDate());
        if (docType == BANK_ORDER) {
            journalRepository.executeUpdate("update Memorder m set m.docType = ?1 where m.id = ?2", BANK_ORDER, Long.valueOf(params.pcidNew));
        }
    }

    private class Reg47422params {
        private String pcidNew;
        private String pcidOld;
        private String idInvisible;
        private String idVisible;
        private String pbr;
//        private String pref;
//        private String dealId;
//        private String pnar;
//        private String rnarlng;
        private String parRf;
    }

}
