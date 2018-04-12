package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.datarec.DefaultJdbcAdapter;
import ru.rbt.ejbcore.datarec.JdbcAdapter;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.shared.Assert;
import ru.rbt.shared.ExceptionUtils;

import javax.ejb.EJB;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

import static java.lang.String.format;
import static ru.rbt.ejbcore.util.StringUtils.ifEmpty;

/**
 * Created by Ivan Sevastyanov
 */
@Deprecated //("задача не выполняется на проде")
public class AccoutPaintTask implements ParamsAwareRunnable {

    private enum PaintLogLevel {
        INFO, ERROR, WARN
    }

    private static final JdbcAdapter jdbcAdapter = new DefaultJdbcAdapter();

    private static final Logger logger = Logger.getLogger(AccoutPaintTask.class.getName());

    @EJB
    private CoreRepository repository;

    @EJB
    private OperdayController operdayController;

    @EJB
    private AuditController auditController;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        execWork();
    }

    public void execWork() throws Exception {
        auditController.info(AuditRecord.LogCode.Task, "Старт процедуры раскраски счетов");
        try {
            beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection1) ->
                    repository.executeTransactionally(connection -> {
                try (PreparedStatement st = connection.prepareStatement("select * from GL_ACCPNT");
                     ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        DataRecord currentRecord = currentRecord(rs);
                        insertGlAccount(currentRecord);
                        insertSqValue(currentRecord);
                    }
                }
                return null;
            }), 60 * 60);
        } catch (Exception e) {
            auditController.error(AuditRecord.LogCode.Task, "Ошибка при выполнении раскраски счетов", null, e);
        }
        auditController.info(AuditRecord.LogCode.Task, "Окончание работы процедуры раскраски счетов");
    }

    private void logMessage(PaintLogLevel level, String bsaacid, String message) throws Exception {
        repository.executeInNewTransaction(persistence -> {
            return repository.executeInNewTransaction(persistence1 -> {
                return repository.executeNativeUpdate("insert into GL_ACCPNT_LOG (BSAACID, LOG_TYPE, LOG_MSG )\n" +
                        "        values (?,?,?)", bsaacid, level.name(), message);
            });
        });
    }

    private DataRecord currentRecord(ResultSet rs) {
        return new DataRecord(rs, jdbcAdapter);
    }

    private DataRecord accRlnRecord(String bsaacid) throws SQLException {
        DataRecord accRlnRecord = repository.selectFirst("select acid, psav, drlno from accrln where bsaacid = ?", bsaacid);
        Assert.notNull(accRlnRecord, format("По счету '%s' не найдена запись в ACCRLN", bsaacid));
        return accRlnRecord;
    }

    private void insertGlAccount(DataRecord currentRecord) throws Exception {
        DataRecord cnt = repository.selectFirst("select count(1) cnt from GL_ACC where BSAACID = ?", currentRecord.getString("BSAACID"));
        if (cnt.getLong("cnt") == 0) {
            logger.info(format("По счету '%s' НЕ найдено записей в GL_ACC. Добавляем...", currentRecord.getString("bsaacid")));
            repository.executeInNewTransaction(persistence -> {
                return repository.executeTransactionally(connection -> {
                    try (PreparedStatement statementInsert = connection.prepareStatement("insert into gl_acc (" +
                            "                                BSAACID     \n" +          // -- 1
                            "                                , CBCC      \n" +          // -- 2
                            "                                , CBCCN     \n" +          // -- 3
                            "                                , BRANCH    \n" +          // -- 4
                            "                                , CCY       \n" +          // -- 5
                            "                                , CUSTNO        -- 6\n" +
                            "                                , ACCTYPE       -- 7\n" +
                            "                                , CBCUSTTYPE        -- 8\n" +
                            "                                , TERM      -- 9\n" +
                            "                                , GL_SEQ        -- 10\n" +
                            "                                , ACC2      -- 11\n" +
                            "                                , PLCODE        -- 12\n" +
                            "                                , ACOD      -- 13\n" +
                            "                                , SQ        -- 14\n" +
                            "                                , ACID      -- 15\n" +
                            "                                , PSAV      -- 16\n" +
                            "                                , DEALSRS       -- 17\n" +
                            "                                , DEALID        -- 18\n" +
                            "                                , SUBDEALID     -- 19\n" +
                            "                                , DESCRIPTION   -- 20\n" +
                            "                                , DTO       -- 21\n" +
                            "                                , DTC       -- 22\n" +
                            "                                , DTR       -- 23\n" +
                            "                                , DTM       -- 24\n" +
                            "                                , OPENTYPE      -- 25\n" +
                            "                                , GLOID     -- 26\n" +
                            "                                , GLO_DC    -- 27\n" +
                            "                                )\n" +
                            "            VALUES (?        \n" +         // -- 1
                            "                    , (SELECT DISTINCT a8cmcd FROM imbcbbrp WHERE a8brcd = ?)     \n" + //-- 2
                            "                    , ?    \n" +          // -- 3
                            "                    , ?    \n" +          // -- 4
                            "                    , ?    \n" +          // -- 5
                            "                    , ?    \n" +          // -- 6
                            "                    , ?    \n" +          // -- 7
                            "                    , ?    \n" +          // -- 8
                            "                    , ?    \n" +          // -- 9
                            "                    , ?    \n" +          // -- 10
                            "                    , ?    \n" +          // -- 11
                            "                    , ?   \n" +           // -- 12
                            "                    , ?    -- 13\n" +
                            "                    , ?   -- 14\n" +
                            "                    , ? -- 15\n" +
                            "                    , ? -- 16\n" +
                            "                    , ? -- 17\n" +
                            "                    , ? -- 18\n" +
                            "                    , ? -- 19\n" +
                            "                    , ?  -- 20\n" +
                            "                    , ?  -- 21\n" +
                            "                    , ?  -- 22\n" +
                            "                    , ?   -- 23\n" +
                            "                    , ?   -- 24\n" +
                            "                    , null      -- 25\n" +
                            "                    , null      -- 26\n" +
                            "                    , null      -- 27\n" +
                            "                    )")) {
                        final DataRecord accRln = accRlnRecord(currentRecord.getString("bsaacid"));
                        java.sql.Date currSqlDate = new java.sql.Date(operdayController.getOperday().getCurrentDate().getTime());
                        statementInsert.setString(1, currentRecord.getString("BSAACID"));
                        statementInsert.setString(2, ifEmpty(currentRecord.getString("BRANCH"), "").trim());
                        statementInsert.setString(3, ifEmpty(currentRecord.getString("CBCCN"), "").trim());
                        statementInsert.setString(4, currentRecord.getString("BRANCH"));
                        statementInsert.setString(5, currentRecord.getString("CCY"));
                        statementInsert.setString(6, currentRecord.getString("CUSTNO"));
                        statementInsert.setString(7, currentRecord.getString("ACCTYPE"));
                        statementInsert.setString(8, currentRecord.getString("CBCUSTTYPE"));
                        statementInsert.setString(9, currentRecord.getString("TERM"));
                        statementInsert.setString(10, currentRecord.getString("GLSEQ"));
                        statementInsert.setString(11, currentRecord.getString("BSAACID").substring(0, 5));
                        statementInsert.setString(12, null);
                        statementInsert.setString(13, currentRecord.getString("ACODE"));
                        statementInsert.setString(14, currentRecord.getString("SQ"));
                        statementInsert.setString(15, accRln.getString("ACID"));
                        statementInsert.setString(16, accRln.getString("PSAV"));
                        statementInsert.setString(17, null);
                        statementInsert.setString(18, currentRecord.getString("DEALID"));
                        statementInsert.setString(19, currentRecord.getString("SUBDEALID"));
                        statementInsert.setString(20, "PAINTED");
                        statementInsert.setDate(21, accRln.getSqlDate("drlno"));
                        statementInsert.setString(22, null);
                        statementInsert.setDate(23, currSqlDate);
                        statementInsert.setDate(24, currSqlDate);
                        statementInsert.executeUpdate();
                    } catch (Throwable e) {
                        logMessage(PaintLogLevel.ERROR, currentRecord.getString("BSAACID")
                                , format("Ошибка при вставке записи по счету '%s'. ", currentRecord.getString("bsaacid")) + e.getMessage()
                                + "\n" + ExceptionUtils.getStacktrace(e).substring(0, 2000));
                    }
                    return null;
                });
            });
        } else {
            logger.warning(format("По счету '%s' найдено '%s' записей в GL_ACC. Пропускаем...", currentRecord.getString("bsaacid"), cnt.getLong("cnt")));
        }
    }

    private void insertSqValue(DataRecord currentRecord) throws Exception {
        repository.executeInNewTransaction(persistence -> {
            return repository.executeTransactionally(connection -> {
                if (!StringUtils.isEmpty(currentRecord.getString("dealid"))) {
                    DataRecord cnt = repository.selectFirst("select count(1) cnt from gl_sqvalue where custno = ? and deal_id = ? and ccy = ?"
                            , currentRecord.getString("custno"), currentRecord.getString("dealid"), currentRecord.getString("ccy"));
                    if (cnt.getLong("cnt") == 0) {
                        try (PreparedStatement insertStatement = connection.prepareStatement("insert into gl_sqvalue (custno, deal_id, ccy, acsq) values (?,?,?,?)")) {
                            insertStatement.setString(1, currentRecord.getString("custno"));
                            insertStatement.setString(2, currentRecord.getString("dealid"));
                            insertStatement.setString(3, currentRecord.getString("ccy"));
                            insertStatement.setString(4, org.apache.commons.lang3.StringUtils.leftPad(currentRecord.getString("sq"), 2, "0"));
                            insertStatement.executeUpdate();
                        }
                    } else {
                        logger.warning(format("Уже есть в GL_SQVALUE данные по счету ('%s'): custno='%s', dealid='%s', ccy='%s'"
                                , currentRecord.getString("bsaacid"), currentRecord.getString("custno"), currentRecord.getString("dealid"), currentRecord.getString("ccy")));
                    }
                }
                return null;
            });

        });
    }
}
