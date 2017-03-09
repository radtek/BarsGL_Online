package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.barsgl.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.datarec.DefaultJdbcAdapter;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.shared.Assert;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static ru.rbt.barsgl.audit.entity.AuditRecord.LogCode.AccountBalanceUnload;
import static ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus.*;

/**
 * Created by Ivan Sevastyanov<br/>
 * выгрузка остатков/оборотов по счетам GL и совместно используемым с Майдас
 * TODO: этот таск должен потом быть удален, после разделения на наши счета и отдельно Майдас
 * @deprecated Не используется. Теперь остатки выгружаются тремя задачами
 * - ru.rbt.barsgl.ejb.controller.operday.task.balsep.AccountBalanceRegisteredUnloadTask
 * - ru.rbt.barsgl.ejb.controller.operday.task.balsep.AccountBalanceSharedUnloadTask
 * - ru.rbt.barsgl.ejb.controller.operday.task.balsep.AccountBalanceUnloadThree
 */
public class AccountBalanceUnloadTask implements ParamsAwareRunnable {

    private static final Logger logger = Logger.getLogger(AccountBalanceUnloadTask.class.getName());

    public static final String CHECK_RUN_KEY = "checkRun";

    @Inject
    private OperdayController operdayController;

    @EJB
    private GLOperationRepository repository;

    @EJB
    private AuditController auditController;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        if (checkRun(properties)) {
            beanManagedProcessor.executeInNewTxWithTimeout((p1, p2) -> {
                auditController.info(AccountBalanceUnload
                        , "Начало выгрузки остатков/оборотов по счетам GL и совместно используемым с Майдас");
                Operday operday = operdayController.getOperday();
                auditController.info(AccountBalanceUnload, format("Выгрузка остатков в DWH: Текущий ОД '%1$td.%1$tm.%1$tY' в фазе '%2$s'", operday.getCurrentDate(), operday.getPhase()));

                long headerId = 0;
                try {
                    headerId = createHeaders(DwhUnloadParams.UnloadBalanceAll);
                    clearAll();
                    fillRegistered();
                    executePhaseTwo();
                    executePhaseThree();
                    executePhaseFour();
                    executePhaseFive();
                    setResultStatus(headerId, SUCCEDED);
                    auditController.info(AccountBalanceUnload
                            , "Успешное завершение выгрузка остатков/оборотов по счетам GL и совместно используемым с Майдас");
                } catch (Exception e) {
                    auditController.error(AccountBalanceUnload, "Ошибка выгрузки остатков оборотов в DWH: ", null, e);
                    if (0 < headerId) {
                        setResultStatus(headerId, ERROR);
                    }
                }
                return null;
            }, 2 * 60 * 60);
        }
    }

    public long createHeaders(DwhUnloadParams unloadParams) throws Exception {
        return createHeaders(unloadParams, operdayController.getOperday().getCurrentDate());
    }

    public long createHeaders(DwhUnloadParams unloadParams, Date operdate) throws Exception {
        return repository.executeInNewTransaction(persistence -> {
            // нужна запись о готовности выгрузки счетов, так как это вьюха - GLVD_ACC
            repository.executeNativeUpdate(
                    "insert into GL_ETLDWHS (PARNAME,PARVALUE,PARDESC,OPERDAY,START_LOAD,END_LOAD) values (?,?,?,?,?,?)"
                    , unloadParams.getParamName(), STARTED.getFlag(), unloadParams.getParamDesc()
                    , operdate, operdayController.getSystemDateTime(), null);
            return repository.selectFirst("SELECT IDENTITY_VAL_LOCAL() id FROM SYSIBM.SYSDUMMY1").getLong("id");
        });
    }

    /**
     * Остатки/обороты по зарегистрированным в BarsGL счетам
     * @throws Exception
     */
    public void clearAll() throws Exception {
        repository.executeInNewTransaction(persistence ->
                repository.executeNativeUpdate(" delete from GLVD_BAL "));
    }
    /**
     * Остатки/обороты по зарегистрированным в BarsGL счетам
     * @throws Exception
     */
    public void fillRegistered() throws Exception {
        int cnt = beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence,connection) ->
                repository.executeNativeUpdate("insert into GLVD_BAL (DAT, ACID, BSAACID, GLACID, OBAL, DTRN, CTRN, DTRNBD, CTRNBD, UNLOAD_DAT)" +
                        "(" +
                        "select am.curdate, am.acid, am.bsaacid, am.gl_acc_id,\n" +
                        "       case when b.dat = curdate then b.obac\n" +
                        "            when b.dat < curdate then b.obac + b.dtac + b.ctac\n" +
                        "       end obal,\n" +
                        "       abs(case when b.dat = curdate then b.dtac\n" +
                        "            when b.dat < curdate then 0\n" +
                        "       end) dtrn,\n" +
                        "       case when b.dat = curdate then b.ctac\n" +
                        "            when b.dat < curdate then 0\n" +
                        "       end ctrn, abs(am.trnv_drbd), am.trnv_crbd, am.curdate\n" +
                        "  from\n" +
                        "        (\n" +
                        "            select p.acid, p.bsaacid, od.curdate, acc.id gl_acc_id,\n" +
                        "                   sum (\n" +
                        "                        case when p.amnt < 0 and p.pod < od.curdate then p.amnt else 0 end\n" +
                        "                   ) trnv_drbd,\n" +
                        "                   sum (\n" +
                        "                        case when p.amnt > 0 and p.pod < od.curdate then p.amnt else 0 end\n" +
                        "                   ) trnv_crbd\n" +
                        "              from gl_acc acc, gl_oper o, gl_posting ps, pd p, gl_od od\n" +
                        "             where o.gloid = ps.glo_ref\n" +
                        "               and ps.pcid = p.pcid\n" +
                        "               and p.invisible <> '1'\n" +
                        "               and o.procdate = od.curdate\n" +
                        "               and acc.acid = p.acid and acc.bsaacid = p.bsaacid\n" +
                        "               and not exists (select 1 from gl_dwhparm w where acc.acod = w.acod and acc.sq = w.sq)\n" +
                        "              group by p.acid, p.bsaacid, od.curdate, acc.id\n" +
                        "        ) am, baltur b\n" +
                        "  where am.bsaacid = b.bsaacid and am.acid = b.acid\n" +
                        "    and am.curdate between b.dat and b.datto" +
                        ")"));
    }

    public void setResultStatus(long headerId, DwhUnloadStatus status) throws Exception {
        if (headerId > 0) {
            int count = repository.executeInNewTransaction(persistence ->
                    repository.executeNativeUpdate("update GL_ETLDWHS set PARVALUE = ?, END_LOAD = ? where ID = ?"
                            , status.getFlag(), operdayController.getSystemDateTime(), headerId));
            Assert.isTrue(1 == count, format("Нет данных для обновления <%d>", headerId));
        }
    }

    /**
     * Обороты BarsGL по совместно используемым счетам
     * @throws Exception
     */
    private void executePhaseTwo() throws Exception {
        beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence,connection) -> {
            repository.executeNativeUpdate("insert into glvd_bal (dat, dtrnbd, ctrnbd, acid, obal, unload_dat)\n" +
                    "(\n" +
                    "    select od.curdate,\n" +
                    "           abs(sum(\n" +
                    "            case when p.amnt < 0 and p.pod < od.curdate then p.amnt else 0 end\n" +
                    "           )) trnv_drbd,\n" +
                    "           sum(\n" +
                    "             case when p.amnt > 0 and p.pod < od.curdate then p.amnt else 0 end\n" +
                    "           ) trnv_crbd,\n" +
                    "           p.acid, -2, od.curdate\n" +
                    "      from pd p, gl_shacpar s, acc a, gl_oper o, gl_posting ps, gl_od od\n" +
                    "     where o.gloid = ps.glo_ref\n" +
                    "       and ps.pcid = p.pcid\n" +
                    "       and p.invisible <> '1'\n" +
                    "       and o.procdate = od.curdate\n" +
                    "       and a.id = p.acid\n" +
                    "       and s.acod = a.ACOD and s.BRANCH = a.brca and s.ACSQ = a.acsq\n" +
                    "     group by p.acid, od.curdate\n" +
                    ")");
            return null;
        });
    }

    /**
     * Загрузка оборотов по совместным счетам из Майдас
     * @throws Exception
     */
    public void executePhaseThree() throws Exception {
        beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence,connection1) -> repository.executeTransactionally(connection -> {
            try (final PreparedStatement selectStatement = connection.prepareStatement(
                            "select right('00000000'||e.cnum, 8)||e.ccy||e.acod||right('00'||e.acsq, 2)||e.brca acid,\n" +
                            "       sum(case when drcr = 0 then e.psta else 0 end) dt,\n" +
                            "       sum(case when drcr = 1 then e.psta else 0 end) ct\n" +
                            "  from m10mmdwh.eodpopd e\n" +
                            "  join gl_shacpar s on e.acod = s.acod\n" +
                            "   and e.acsq  = s.acsq\n" +
                            "   and e.brca = s.branch" +
                            "   and UPPER(e.spos) NOT LIKE 'TECH%'\n" +
                            "   and UPPER(e.spos) NOT LIKE 'FINR%'\n" +
                            " group by e.acod, e.acsq, e.brca, e.cnum, e.ccy");
                 final ResultSet rs = selectStatement.executeQuery()){
                while (rs.next()) {
                    DataRecord record = new DataRecord(rs, new DefaultJdbcAdapter());
                    try (PreparedStatement updateStatement =
                                 connection.prepareStatement(
                                         "update glvd_bal b set dtrnmid = ?, ctrnmid = ? \n" +
                                         " where acid = ? and dat = (select curdate from gl_od)")){
                        updateStatement.setLong(1, record.getLong("dt"));
                        updateStatement.setLong(2, record.getLong("ct"));
                        updateStatement.setString(3, record.getString("acid"));
                        if (0 == updateStatement.executeUpdate()) {
                            try (PreparedStatement insertStatement = connection.prepareStatement(
                                    "insert into glvd_bal (acid, dtrnmid, ctrnmid, dat, obal, unload_dat) " +
                                    "values (?, ?, ?, (select curdate from gl_od ), -3, (select curdate from gl_od ))")){
                                insertStatement.setString(1, record.getString("acid"));
                                insertStatement.setLong(2, record.getLong("dt"));
                                insertStatement.setLong(3, record.getLong("ct"));
                                insertStatement.executeUpdate();
                            }
                        }
                    }
                }
            }
            return null;
        }));
    }

    /**
     * устанавливаем счет ЦБ для записей где его нет
     */
    private void executePhaseFour() throws Exception {
        beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence,connection1) ->
                repository.executeTransactionally(connection -> {
            try (PreparedStatement selectStatement =
                         connection.prepareStatement(
                                 "select acid from glvd_bal b " +
                                 " where bsaacid is null and dat = (select curdate from gl_od)")){
                try (final ResultSet rs = selectStatement.executeQuery()){
                    while (rs.next()) {
                        final String acid = rs.getString(1);
                        final String bsaacid = findBsaacidInternal(acid, findBsaacid(acid)).orElseThrow(() -> new DefaultApplicationException(
                                format("Ошибка при выгрузке остатков в DWH: Не найден или более одного счета по счету Майдас '%s'", acid)));
                        try (PreparedStatement updateStatement = connection.prepareStatement(
                                "update glvd_bal b set bsaacid = ? \n" +
                                        " where dat = (select curdate from gl_od) and acid = ?")){
                            updateStatement.setString(1, bsaacid);
                            updateStatement.setString(2, acid);
                            updateStatement.executeUpdate();
                        }
                    }
                }
            }
            return null;
        }));
    }

    /**
     * заполнение баланса и оборотов по совместно используемым счетам
     */
    private void executePhaseFive() throws Exception {
        beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence,connection1) -> repository.executeTransactionally(connection -> {
            try (PreparedStatement selectStatement = connection.prepareStatement(
                    "select case when b.dat = od.curdate then b.obac\n" +
                    "            when b.dat < od.curdate then b.obac + b.dtac + b.ctac\n" +
                    "       end obal,\n" +
                    "       abs(case when b.dat = od.curdate then b.dtac\n" +
                    "            when b.dat < od.curdate then 0\n" +
                    "       end) dtrn,\n" +
                    "       case when b.dat = od.curdate then b.ctac\n" +
                    "            when b.dat < od.curdate then 0\n" +
                    "       end dtrn, g.acid, g.bsaacid\n" +
                    "  from baltur b, glvd_bal g, gl_od od\n" +
                    " where g.bsaacid = b.bsaacid and g.acid = b.acid\n" +
                    "   and od.curdate between b.dat and b.datto\n" +
                    "   and od.curdate = g.dat and g.obal < 0")){
                try (final ResultSet resultSet = selectStatement.executeQuery()) {
                    while (resultSet.next()) {
                        try (PreparedStatement updateStatement = connection
                                .prepareStatement(
                                        "update glvd_bal set obal = ?, dtrn = ?, ctrn = ? " +
                                                " where acid = ? and bsaacid = ? and dat = (select curdate from gl_od)")) {
                            updateStatement.setLong(1, resultSet.getLong(1));
                            updateStatement.setLong(2, resultSet.getLong(2));
                            updateStatement.setLong(3, resultSet.getLong(3));
                            updateStatement.setString(4, resultSet.getString(4));
                            updateStatement.setString(5, resultSet.getString(5));
                            updateStatement.executeUpdate();
                        }
                    }
                }
            }
          return null;
        }));
    }

    private Optional<String> findBsaacidInternal(String acid, List<DataRecord> rlns) throws SQLException {
        if (rlns.isEmpty()) {
            logger.log(Level.WARNING, format("Не найдено счета ЦБ по счету Майдас '%s'", acid));
            return Optional.empty();
        } else
        if (1 == rlns.size()) {
            return Optional.of(rlns.get(0).getString("bsaacid"));
        } else {
            logger.log(Level.WARNING, format("Найдено '%d' счетов ЦБ по счету Майдас '%s'", rlns.size(), acid));
            return Optional.empty();
        }
    }

    public List<DataRecord> findBsaacid(String acid) throws SQLException {
        return repository.select(
                "select *\n" +
                        "  from (\n" +
                        "    select case\n" +
                        "                when b.bstype = '0' and r.rlntype = '0' then 1 \n" +
                        "                when b.bstype = '2' and r.rlntype = '2' then 1 \n" +
                        "                when b.bstype not in ('2', '0') then 1 \n" + // TODO здесь должна быть обработка "2N"
                        "                else 0 \n" +
                        "           end fl,\n" +
                        "           r.bsaacid, r.rlntype \n" +
                        "      from ACCRLN r, GL_BSTYPE b, gl_od od\n" +
                        "     where substr(r.acid, 12, 4) = b.acod\n" +
                        "       and od.curdate between r.drlno and r.drlnc\n" +
                        "       and r.acid = ? \n" +
                        ") v where v.fl = 1", acid);
    }

    public boolean checkRun(Properties properties) throws Exception {
        if (Boolean.parseBoolean(Optional.ofNullable(properties.getProperty(CHECK_RUN_KEY)).orElse("true"))) {
            // в текущем ОД должна начаться загрузка Майдас
            final DataRecord countWorkproc = repository.selectFirst(
                    "select count(1) cnt\n" +
                            "  from workproc w, gl_od od \n" +
                            " where w.dat = od.curdate");
            final DataRecord countUnloadBalance = repository.selectFirst(
                    "select count(1) cnt from gl_etldwhs " +
                            " where parname = ? and parvalue in (?, ?) and pardesc = ? and operday = (select curdate from gl_od)"
                    , DwhUnloadParams.UnloadBalanceAll.getParamName(), SUCCEDED.getFlag(), STARTED.getFlag(), DwhUnloadParams.UnloadBalanceAll.getParamDesc());
            final boolean workproc = 0 == countWorkproc.getLong(0);
            final boolean already = 0 < countUnloadBalance.getLong(0);
            final boolean notCob = operdayController.getOperday().getPhase() != Operday.OperdayPhase.COB;

            if (workproc || already || notCob) {
                auditController.warning(AccountBalanceUnload
                        , "Ошибка при выгрузке остатков", null
                        , format("Выгрузка остатков невозможна: не началась ежедневная загрузка <'%s'>, " +
                        "выгрузка уже запущена или выполнена в текущем ОД (%s): <'%s'>, " +
                        "текущий ОД не в фазе COB: <'%s'> "
                        , workproc
                        , dateUtils.onlyDateString(operdayController.getOperday().getCurrentDate()), already, notCob));
                return false;
            }
            return true;
        } else {
            return true;
        }
    }


}
