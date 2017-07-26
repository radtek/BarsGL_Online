package ru.rbt.barsgl.ejb.controller.operday.task.balsep;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.operday.task.AccountBalanceUnloadTask;
import ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadParams;
import ru.rbt.barsgl.ejb.controller.operday.task.TaskUtils;
import ru.rbt.barsgl.ejb.repository.WorkprocRepository;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.controller.etc.TextResourceController;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountOvervaluedBalanceUnload;
import static ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus.ERROR;
import static ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus.SUCCEDED;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.COB;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.controller.operday.task.balsep.AccountBalanceRegisteredUnloadTask.CHECK_RUN_KEY;
import static ru.rbt.ejbcore.validation.ErrorCode.ALREADY_UNLOADED;

/**
 * Created by Ivan Sevastyanov
 * выгрузка остатков/оборотов по GL счетам и соавместным в текущем ОД после переоценки Майдас (шаг P9)
 */
public class AccountBalanceUnloadThree implements ParamsAwareRunnable {

    public static String STEP_CHECK_NAME_KEY = "stepCheck";
    public static String STEP_CHECK_NAME_DEFAULT = "P9";

    @EJB
    private CoreRepository repository;

    @EJB
    private AuditController auditController;

    @EJB
    private OperdayController operdayController;

    @Inject
    private DateUtils dateUtils;

    @Inject
    private AccountBalanceUnloadTask legecyTask;

    @Inject
    private TextResourceController textResourceController;

    @Inject
    private WorkprocRepository workprocRepository;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        final Date operday =
                COB == operdayController.getOperday().getPhase()
                    ? operdayController.getOperday().getCurrentDate()
                : (ONLINE == operdayController.getOperday().getPhase()
                            ? operdayController.getOperday().getLastWorkingDay()
                            : throwIllegalOperdayState().get());

        if (checkRun(properties, operday)) {
            long headerId = 0;
            try {
                headerId = legecyTask.createHeaders(DwhUnloadParams.UnloadBalanceThree, operday);
                auditController.info(AccountOvervaluedBalanceUnload,
                        format("Начало выгрузки переоцененных остатков/оборотов по совместно используемым счетам за дату '%s'"
                                , dateUtils.onlyDateString(operday)));
                auditController.info(AccountOvervaluedBalanceUnload, format("Перенесено в историю '%s'", moveToHistory()));
                auditController.info(AccountOvervaluedBalanceUnload, format("Удалено старых записей '%s'", cleanOld()));
                auditController.info(AccountOvervaluedBalanceUnload, format("Выгружено счетов GL с переоценкой '%s'", fillRegisteredOverloaded()));
                auditController.info(AccountOvervaluedBalanceUnload, format("Выгружено совместных счетов (0,2) с переоценкой '%s'", fillShared_0_2()));
                auditController.info(AccountOvervaluedBalanceUnload, format("Выгружено совместных счетов (2N) с переоценкой '%s'", fillOvervaluedAcoounts2N()));
                legecyTask.setResultStatus(headerId, SUCCEDED);
                auditController.info(AccountOvervaluedBalanceUnload,
                        format("Успешное окончание выгрузки ПЕРЕОЦЕНЕННЫХ остатков/оборотов по совместно используемым счетам за дату '%s'"
                                , dateUtils.onlyDateString(operday)));
            } catch (Throwable e) {
                auditController.error(AccountOvervaluedBalanceUnload
                        ,  format("Ошибка выгрузки остатков/оборотов после переоценки по счетам за дату '%s'"
                        , dateUtils.onlyDateString(operday)), null, e);
                if (0 < headerId) {
                    legecyTask.setResultStatus(headerId, ERROR);
                }
            }
        }
    }

    private Supplier<Date> throwIllegalOperdayState() {
        return () -> {
            throw new DefaultApplicationException(format("Недопустимый статус ОД: %s"
                , operdayController.getOperday().getPhase()));
        };
    }

    /**
     * текущее состояние после переоценки по зарегистрированным у нас счетам
     * @return кол-во вставленных
     * @throws Exception
     */
    private int fillRegisteredOverloaded() throws Exception {
        return (int) repository.executeInNewTransaction(persistence ->
                repository.executeTransactionally(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(textResourceController
                    .getContent("ru/rbt/barsgl/ejb/controller/operday/task/balsep/fill_three_reg.sql"))){
                return statement.executeUpdate();
            }
        }));
    }

    private int fillShared_0_2() throws Exception {
        return (int) repository.executeInNewTransaction(persistence -> {
            return repository.executeTransactionally(connection -> {
                int count = 0;
                try (PreparedStatement statementQuery = connection.prepareStatement(
                        textResourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/balsep/fill_three_0_2.sql"))
                     ; ResultSet resultSet = statementQuery.executeQuery()) {
                    while (resultSet.next()) {
                        DataRecord cntRecord = repository.selectOne("select count(1) cnt from glvd_bal3 where bsaacid = ? and acid = ? and dat = ?"
                                , resultSet.getString("bsaacid"), resultSet.getString("acid"), resultSet.getDate("dat"));
                        if (cntRecord.getLong("cnt") == 0) {
                            repository.executeNativeUpdate(
                                    "insert into glvd_bal3 (dat, acid, bsaacid, glacid, obal, dtrn, ctrn, unload_dat, obalrur, dtrnrur, ctrnrur)" +
                                            " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                                    , resultSet.getDate("dat"), resultSet.getString("acid"), resultSet.getString("bsaacid")
                                    , resultSet.getLong("glacid"), resultSet.getLong("obal"), resultSet.getLong("dtrn")
                                    , resultSet.getLong("ctrn"), resultSet.getDate("unload_dat"), resultSet.getLong("obalrur")
                                    , resultSet.getLong("dtrnrur"), resultSet.getLong("ctrnrur")
                            );
                            count++;
                        } else {
                            auditController.warning(AccountOvervaluedBalanceUnload
                                    , format("Пересечение зарегистрированных и совместных счетов " +
                                            "при выгрузке переоцененных счетов: bsaacid '%s', acid '%s', дата остатка '%s', дата выгрузки '%s'"
                                        , resultSet.getString("acid"), resultSet.getString("bsaacid"), dateUtils.onlyDateString(resultSet.getDate("dat"))
                                        , dateUtils.onlyDateString(resultSet.getDate("unload_dat"))), null, ""
                            );
                        }
                    }
                }
                return count;
            });
        });
    }

    /**
     * состояние остатков после переоценки по совместным без счета ЦБ
     * почти полный копи-паст из ru.rbt.barsgl.ejb.controller.operday.task.balsep.AccountBalanceSharedUnloadTask#fillShared2N()
     * @return
     * @throws Exception
     */
    private int fillOvervaluedAcoounts2N() throws Exception {
        return (int) repository.executeInNewTransaction(persistence -> {
            return repository.executeTransactionally(connection -> {
                int count = 0;
                try (PreparedStatement query = connection.prepareStatement(
                        "select b.acid, min(b.dat) min_pod, max(b.dat) max_pod \n" +
                        "  from glvd_bal2 b\n" +
                        " where nvl(b.bsaacid, '-') = '-'\n" +
                        " group by b.acid"); ResultSet rsRoot = query.executeQuery()) {
                    while (rsRoot.next()) {
                        try (PreparedStatement queryBaltur = connection.prepareStatement(
                                textResourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/balsep/fill_three_2N.sql"))) {
                            queryBaltur.setString(1, rsRoot.getString("ACID"));
                            queryBaltur.setDate(2, rsRoot.getDate("MIN_POD"));
                            queryBaltur.setDate(3, rsRoot.getDate("MAX_POD"));

                            try (ResultSet rsBaltur = queryBaltur.executeQuery()) {
                                while (rsBaltur.next()) {
                                    repository.executeNativeUpdate("insert into GLVD_BAL3 (unload_dat, dat, obal, obalrur, dtrn, dtrnrur, ctrn, ctrnrur, acid)" +
                                                    " values (?,?,?,?,?,?,?,?,?)"
                                            , rsRoot.getDate("MAX_POD")
                                            , rsBaltur.getDate("DAT")
                                            , rsBaltur.getLong("OBAL")
                                            , rsBaltur.getLong("OBALRUR")
                                            , rsBaltur.getLong("DTRN")
                                            , rsBaltur.getLong("DTRNRUR")
                                            , rsBaltur.getLong("CTRN")
                                            , rsBaltur.getLong("CTRNRUR")
                                            , rsBaltur.getString("ACID")
                                    );
                                    count++;
                                }
                            }
                        }
                    }
                }
                return count;
            });
        });
    }

    private boolean checkRun(Properties properties, Date executeDate) throws Exception {
        try {
            if (Boolean.parseBoolean(Optional.ofNullable(properties.getProperty(CHECK_RUN_KEY)).orElse("true"))) {
                Assert.isTrue(0 == TaskUtils.getDwhAlreadyHeaderCount(executeDate, DwhUnloadParams.UnloadBalanceThree, repository)
                        , () -> new ValidationError(ALREADY_UNLOADED
                                , format("Выгрузка уже запущена или выполнена в текущем ОД (%s)"
                                , dateUtils.onlyDateString(executeDate))));

                String stepName = getCheckStepName(properties);
                final boolean isStepOk = workprocRepository.isStepOK(stepName, executeDate);
                Assert.isTrue(isStepOk, () -> new ValidationError(ErrorCode.OPERDAY_LDR_STEP_ERR
                        , stepName, dateUtils.onlyDateString(executeDate), stepName));

                Assert.isTrue(!isRegisteredNotExecuted(executeDate)
                        , () -> new ValidationError(ErrorCode.PRE_TASK_NOT_COMPLETED, "'GL счета'", dateUtils.onlyDateString(executeDate)));

                Assert.isTrue(!isSharedNotExecuted(executeDate)
                        , () -> new ValidationError(ErrorCode.PRE_TASK_NOT_COMPLETED, "'совместные счета'", dateUtils.onlyDateString(executeDate)));
            }
            return true;
        } catch (ValidationError e) {
            auditController.warning(AccountOvervaluedBalanceUnload
                    , "Невозможно выгрузить остатки по счетам GL и совместным после переоценки", null, e);
            return false;
        }
    }

    private String getCheckStepName(Properties properties) {
        return Optional.ofNullable(properties.getProperty(STEP_CHECK_NAME_KEY)).orElse(STEP_CHECK_NAME_DEFAULT);
    }

    private int moveToHistory() throws Exception {
        return (int) repository.executeInNewTransaction(persistence ->
                repository.executeNativeUpdate("insert into GLVD_BAL3_H (DAT,ACID,BSAACID,GLACID,OBAL,OBALRUR,DTRN,DTRNRUR,CTRN,CTRNRUR,DTRNMID,DTRNMIDRUR,CTRNMID,CTRNMIDRUR,UNLOAD_DAT)" +
                        " (select DAT,ACID,BSAACID,GLACID,OBAL,OBALRUR,DTRN,DTRNRUR,CTRN,CTRNRUR,DTRNMID,DTRNMIDRUR,CTRNMID,CTRNMIDRUR,UNLOAD_DAT from GLVD_BAL3)"));
    }

    private int cleanOld() throws Exception {
        return (int) repository.executeInNewTransaction(persistence -> {
            return repository.executeNativeUpdate("delete from GLVD_BAL3");
        });
    }

    /**
     * была ли выгрузка зарегистрированных счетов в текущим ОД
     * @return
     */
    private boolean isRegisteredNotExecuted(Date operday) throws SQLException {
        return 0 == countUnloadsOnCurrentOperday(DwhUnloadParams.UnloadBalanceRegistered, operday);
    }

    /**
     * была ли выгрузка совместных счетов в текщем ОД
     * @return
     */
    private boolean isSharedNotExecuted(Date operday) throws SQLException {
        return 0 == countUnloadsOnCurrentOperday(DwhUnloadParams.UnloadBalanceShared, operday);
    }

    /**
     * кол-во выгрузок с типом в текущем ОД
     * @param param
     * @return
     * @throws SQLException
     */
    private int countUnloadsOnCurrentOperday(DwhUnloadParams param, Date operday) throws SQLException {
        return repository.selectFirst(
                "select count(1) cnt from gl_etldwhs " +
                        " where parname = ? and parvalue = ? " +
                        "   and pardesc = ? and operday = ?"
                , param.getParamName(), SUCCEDED.getFlag(), param.getParamDesc(), operday).getInteger("cnt");
    }


}
