package ru.rbt.barsgl.ejb.controller.operday.task.balsep;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.AccountBalanceUnloadTask;
import ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadParams;
import ru.rbt.barsgl.ejb.controller.operday.task.TaskUtils;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.ejbcore.controller.etc.TextResourceController;
import ru.rbt.barsgl.ejb.repository.BankCurrencyRepository;
import ru.rbt.barsgl.ejb.repository.RateRepository;
import ru.rbt.barsgl.ejb.repository.WorkprocRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.barsgl.shared.enums.EnumUtils;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.COB;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus.ERROR;
import static ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus.SUCCEDED;
import static ru.rbt.barsgl.ejb.controller.operday.task.balsep.AccountBalanceRegisteredUnloadTask.CHECK_RUN_KEY;
import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountSepBalanceUnload;
import static ru.rbt.ejbcore.validation.ErrorCode.ALREADY_UNLOADED;
import static ru.rbt.ejbcore.validation.ErrorCode.OPERDAY_STATE_INVALID;

/**
 * Created by Ivan Sevastyanov
 */
public class AccountBalanceSharedUnloadTask implements ParamsAwareRunnable {


    private static final Logger logger = Logger.getLogger(AccountBalanceSharedUnloadTask.class.getName());

    @EJB
    private CoreRepository repository;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @EJB
    private AuditController auditController;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private OperdayController operdayController;

    @Inject
    private RateRepository rateRepository;

    @Inject
    private BankCurrencyRepository currencyRepository;

    @Inject
    private AccountBalanceUnloadTask legecyTask;

    @Inject
    private TextResourceController textResourceController;

    @Inject
    private WorkprocRepository workprocRepository;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        java.util.Date executeDate = TaskUtils.getDateFromGLOD(properties, repository, operdayController.getOperday());
        if (checkRun(executeDate, properties)) {
            long headerId = 0;
            try {
                headerId = legecyTask.createHeaders(DwhUnloadParams.UnloadBalanceShared, executeDate);
                auditController.info(AccountSepBalanceUnload,
                        format("Начало выгрузки остатков/оборотов по совместно используемым счетам за дату '%s'"
                                , dateUtils.onlyDateString(executeDate)));
                auditController.info(AccountSepBalanceUnload, format("Перенесено в историю '%s'", moveToHistory()));
                auditController.info(AccountSepBalanceUnload, format("Удалено старых записей '%s'", cleanOld()));
                auditController.info(AccountSepBalanceUnload, format("Выгружено по первой фазе '%s'", fillSharedSimple(executeDate)));
                auditController.info(AccountSepBalanceUnload, format("Выгружено по 2N фазе '%s'", fillShared2N(executeDate)));
                auditController.info(AccountSepBalanceUnload, format("Выгружено по счетам с оборотами в Майдас в текущем ОД '%s'", fillSharedMidas(executeDate)));
                legecyTask.setResultStatus(headerId, SUCCEDED);
                auditController.info(AccountSepBalanceUnload,
                        format("Выгрузка остатков/оборотов по совместно используемым счетам за дату '%s' успешно завершена"
                                , dateUtils.onlyDateString(operdayController.getOperday().getCurrentDate())));
            } catch (Throwable e) {
                auditController.error(AccountSepBalanceUnload
                        ,  format("Ошибка выгрузки остатков/оборотов по совместно используемым счетам за дату '%s'"
                        , dateUtils.onlyDateString(operdayController.getOperday().getCurrentDate())), null, e);
                if (0 < headerId) {
                    legecyTask.setResultStatus(headerId, ERROR);
                }
            }
        }
    }

    /*don't used GL_SHACOD*/
    /**
     * Остатки обороты по счетам по настроечной таблице GL_SHACOD.BSTYPE 0 и 2
     * @return кол-во вставленных строк
     */    
    //*
    private int fillSharedSimple(java.util.Date executeDate) throws Exception {
        return beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
            int count = 0;
            try (PreparedStatement query = connection.prepareStatement(
                    textResourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/balsep/fill_shared02_0.sql"))
                 ; ){
                query.setDate(1, new java.sql.Date(executeDate.getTime()));
                try (ResultSet rs = query.executeQuery()){
                    while (rs.next()) {
                        List<DataRecord> acidDates = repository.select(textResourceController
                                        .getContent("ru/rbt/barsgl/ejb/controller/operday/task/balsep/fill_shared2n_bdat.sql")
                                , rs.getDate("min_pod"), rs.getDate("max_pod"));
                        for (DataRecord acidDate : acidDates) {
                            final String balanceDate = dateUtils.dbDateString(acidDate.getDate("dat"));
                            final String fillShared2nAccSql = textResourceController
                                    .getContent("ru/rbt/barsgl/ejb/controller/operday/task/balsep/fill_shared2n_acc.sql")
                                    .replace("$1", balanceDate);
                            try (PreparedStatement queryBaltur = connection.prepareStatement(fillShared2nAccSql)) {
                                queryBaltur.setString(1, rs.getString("ACID"));
                                logger.info(format("Type 0,2. Получаем остатки/обороты по счету Мидас '%s' за дату '%s'"
                                        , rs.getString("acid"), balanceDate));
                                try (ResultSet rsBaltur = queryBaltur.executeQuery()) {
                                    while (rsBaltur.next()) {
                                        repository.executeNativeUpdate(
                                                "insert into GLVD_BAL2 (unload_dat, dat, obal, obalrur, dtrn, dtrnrur, ctrn, ctrnrur, acid)" +
                                                        " values (?,?,?,?,?,?,?,?,?)"
                                                , rs.getDate("MAX_POD")
                                                , acidDate.getDate("DAT")
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
                        count++;
                    }
                }
            }
            return count;
        }), 60*60);
    }
    //*/
    
    private int moveToHistory() throws Exception {
        return (int) repository.executeInNewTransaction(persistence ->
                repository.executeNativeUpdate("insert into GLVD_BAL2_H (DAT,ACID,BSAACID,GLACID,OBAL,OBALRUR,DTRN,DTRNRUR,CTRN,CTRNRUR,DTRNMID,DTRNMIDRUR,CTRNMID,CTRNMIDRUR,UNLOAD_DAT)" +
                " (select DAT,ACID,BSAACID,GLACID,OBAL,OBALRUR,DTRN,DTRNRUR,CTRN,CTRNRUR,DTRNMID,DTRNMIDRUR,CTRNMID,CTRNMIDRUR,UNLOAD_DAT from GLVD_BAL2)"));
    }

    private int cleanOld() throws Exception {
        return (int) repository.executeInNewTransaction(persistence -> {
            return repository.executeNativeUpdate("delete from GLVD_BAL2");
        });
    }

    /*don't used GL_SHACOD*/
    /**
     * Остатки обороты по счетам по настроечной таблице GL_SHACOD.BSTYPE 2N
     * @return кол-во вставленных строк
     * @throws Exception
     */
    //*
    private int fillShared2N (java.util.Date executeDate) throws Exception {
        return beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
            int count = 0;
            try (PreparedStatement query = connection.prepareStatement(
                    textResourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/balsep/fill_shared2n_dt.sql"))) {
                query.setDate(1, new Date(executeDate.getTime()));
                try (ResultSet rsRoot = query.executeQuery()){
                    while (rsRoot.next()) {
                        List<DataRecord> acidDates = repository.select(textResourceController
                                        .getContent("ru/rbt/barsgl/ejb/controller/operday/task/balsep/fill_shared2n_bdat.sql")
                                , rsRoot.getDate("min_pod"), rsRoot.getDate("max_pod"));
                        for (DataRecord acidDate : acidDates) {
                            final String balanceDate = dateUtils.dbDateString(acidDate.getDate("dat"));
                            final String fillShared2nAccSql = textResourceController
                                    .getContent("ru/rbt/barsgl/ejb/controller/operday/task/balsep/fill_shared2n_acc.sql")
                                    .replace("$1", balanceDate);
                            try (PreparedStatement queryBaltur = connection.prepareStatement(fillShared2nAccSql)) {
                                queryBaltur.setString(1, rsRoot.getString("ACID"));
                                logger.info(format("Получаем остатки/обороты по счету Мидас '%s' за дату '%s'"
                                        , rsRoot.getString("acid"), balanceDate));
                                try (ResultSet rsBaltur = queryBaltur.executeQuery()) {
                                    while (rsBaltur.next()) {
                                        repository.executeNativeUpdate(
                                                "insert into GLVD_BAL2 (unload_dat, dat, obal, obalrur, dtrn, dtrnrur, ctrn, ctrnrur, acid)" +
                                                        " values (?,?,?,?,?,?,?,?,?)"
                                                , rsRoot.getDate("MAX_POD")
                                                , acidDate.getDate("DAT")
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
                }
            }
            return count;
        }), 60*60);
    }
    //*/

    /*don't used GL_SHACOD*/
    /**
     * совместно используемые счета Майдас, по которым существует проводка в Майдасе
     * @return кол-во вставленных/измененных строк
     */
    //*
    private int fillSharedMidas(java.util.Date executeDate) throws Exception {
        logger.info(format("Получаем список проводок по буферной таблице Майдас в текущем закрытом ОД: '%s'"
                , dateUtils.onlyDateString(operdayController.getOperday().getCurrentDate())));
        return beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
            int count = 0;
            try (PreparedStatement rootStatement = connection.prepareStatement(textResourceController
                    .getContent("ru/rbt/barsgl/ejb/controller/operday/task/balsep/fill_shared_mid.sql"))
                 ){
                rootStatement.setDate(1, new Date(executeDate.getTime()));
                try (ResultSet resultSetRoot = rootStatement.executeQuery()) {
                    while (resultSetRoot.next()) {
                        logger.info(format("Получаем обороты по совместному счету Майдас '%s' с ACCRLN.RLNTYPE '%s'"
                                , resultSetRoot.getString("ACID"), resultSetRoot.getString("RLNTYPE")));
                        try (PreparedStatement statementQuery = connection.prepareStatement(
                                textResourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/balsep/fill_shared_mid2.sql"))) {
                            statementQuery.setDate(1, new Date(executeDate.getTime()));
                            statementQuery.setString(2, resultSetRoot.getString("ACID"));
                            statementQuery.setString(3, resultSetRoot.getString("RLNTYPE"));
                            try (ResultSet resultSetTotal = statementQuery.executeQuery()) {
                                while (resultSetTotal.next()) {
                                    int updated = repository.executeNativeUpdate(
                                            "update glvd_bal2 set " +
                                                    " dtrnmid = ?" +
                                                    ", dtrnmidrur = ?" +
                                                    ", ctrnmid = ?" +
                                                    ", ctrnmidrur = ? " +
                                                    "where dat = ? and value(bsaacid,'') = '' and acid = ? "
                                            , resultSetRoot.getLong("trnv_drmid")
                                            , getMinorRur(resultSetRoot.getLong("trnv_drmid"), new BankCurrency(resultSetTotal.getString("ccy")))
                                            , resultSetRoot.getLong("trnv_crmid")
                                            , getMinorRur(resultSetRoot.getLong("trnv_crmid"), new BankCurrency(resultSetTotal.getString("ccy")))
                                            , operdayController.getOperday().getCurrentDate()
                                            , resultSetRoot.getString("ACID")
                                    );
                                    if (0 == updated) {
                                        final Object[] params = new Object[]{
                                                resultSetTotal.getLong("OBAL")
                                                , resultSetTotal.getLong("OBALRUR")
                                                , resultSetTotal.getLong("dtrn")
                                                , resultSetTotal.getLong("dtrnrur")
                                                , resultSetTotal.getLong("ctrn")
                                                , resultSetTotal.getLong("ctrnrur")
                                                , resultSetRoot.getLong("trnv_drmid")
                                                , getMinorRur(resultSetRoot.getLong("trnv_drmid"), new BankCurrency(resultSetTotal.getString("ccy")))
                                                , resultSetRoot.getLong("trnv_crmid")
                                                , getMinorRur(resultSetRoot.getLong("trnv_crmid"), new BankCurrency(resultSetTotal.getString("ccy")))
                                                , operdayController.getOperday().getCurrentDate()
                                                , resultSetRoot.getString("ACID")
                                                , operdayController.getOperday().getCurrentDate()
                                        };
                                        repository.executeNativeUpdate(
                                                "insert into glvd_bal2 (obal , obalrur , dtrn , dtrnrur , ctrn , ctrnrur , dtrnmid , dtrnmidrur , ctrnmid , ctrnmidrur, dat, acid, unload_dat)" +
                                                        " values (?,?,?,?,?,?,?,?,?,?,?,?,?)", params);
                                    }
                                    count++;
                                }
                            }
                        }
                    }
                }
            }
            return count;
        }), 2 * 60 * 60);
    }
    //*/

    private long getMinorRur(long minor, BankCurrency currency) {
        currency = currencyRepository.refreshCurrency(currency);
        BigDecimal majorAmount = currencyRepository.getMajorAmount(currency, minor);
        BigDecimal semi1 = majorAmount
                .multiply(rateRepository.getRate(currency, operdayController.getOperday().getCurrentDate()))
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        return currencyRepository.getMinorAmount(currency, semi1);
    }

    private boolean checkRun(java.util.Date executeDate, Properties properties) throws Exception {
        try {
            if (Boolean.parseBoolean(Optional.ofNullable(properties.getProperty(CHECK_RUN_KEY)).orElse("true"))) {

                String stepName = TaskUtils.getStringValue(properties, "stepName", "A1GL");
                final boolean isStepOk = workprocRepository.isStepOK(stepName, executeDate);
                Assert.isTrue(isStepOk, () -> new ValidationError(ErrorCode.OPERDAY_LDR_STEP_ERR
                        , stepName, dateUtils.onlyDateString(executeDate), stepName));

                Assert.isTrue(0 == TaskUtils.getDwhAlreadyHeaderCount(executeDate, DwhUnloadParams.UnloadBalanceShared, repository)
                        , () -> new ValidationError(ALREADY_UNLOADED
                                , format("Выгрузка уже запущена или выполнена в текущем ОД (%s)"
                                , dateUtils.onlyDateString(executeDate))));

                Operday.OperdayPhase[] phases = new Operday.OperdayPhase[] {COB, ONLINE};
                Assert.isTrue(EnumUtils.contains(phases, operdayController.getOperday().getPhase())
                        , () -> new ValidationError(OPERDAY_STATE_INVALID, operdayController.getOperday().getPhase().name(), Arrays.toString(phases)));
            }
            return true;
        } catch (ValidationError e) {
            auditController.warning(AccountSepBalanceUnload, "Невозможно выгрузить остатки по совместно используемым счетам", null, e);
            return false;
        }
    }


}
