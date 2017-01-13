package ru.rbt.barsgl.ejb.controller.operday.task.stamt;

import org.apache.commons.lang3.time.DateUtils;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.etc.TextResourceController;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

/**
 * Created by Ivan Sevastyanov on 28.01.2016.
 */
public class StamtUnloadController {

    public static final String STAMT_UNLOAD_FULL_DATE_KEY = "operday";

    @EJB
    private CoreRepository repository;

    @EJB
    private OperdayController operdayController;

    @Inject
    private TextResourceController textResourceController;

    public void setHeaderStatus(long headerId, DwhUnloadStatus status) throws Exception {
        repository.executeInNewTransaction(persistence ->
                repository.executeNativeUpdate("UPDATE GL_ETLSTMS SET PARVALUE = ?, END_LOAD = CURRENT TIMESTAMP " +
                        "    WHERE ID = ?", status.getFlag(), headerId));
    }

    public long createHeader(Date operday, UnloadStamtParams params) throws Exception {
        return (Long)repository.executeInNewTransaction(persistence -> {
            repository.executeNativeUpdate(
                    "INSERT INTO GL_ETLSTMS (PARNAME, PARVALUE, PARDESC, OPERDAY, START_LOAD)\n" +
                            "    VALUES (?, ?, ?, ?, CURRENT TIMESTAMP)"
                    , params.getParamName(), DwhUnloadStatus.STARTED.getFlag(), params.getParamDesc(), operday);
            return repository.selectFirst("SELECT IDENTITY_VAL_LOCAL() id FROM SYSIBM.SYSDUMMY1").getLong("id");
        });
    }

    public long getAlreadyHeaderCount(Date executeDate, UnloadStamtParams params) throws SQLException {
        return repository.selectFirst(
                "select count(1) cnt from GL_ETLSTMS " +
                        "where PARNAME = ? and PARVALUE in (0, 1) and PARDESC = ? and OPERDAY = ?"
                , params.getParamName(), params.getParamDesc(), executeDate).getLong(0);
    }

    /**
     * кол-во заголовков со статусом
     * @param executeDate дата ОД
     * @param params параметры выгрузки
     * @param status ожидаемый статус
     * @return кол-во заголовков
     * @throws SQLException
     */
    public long getAlreadyHeaderCount(Date executeDate, UnloadStamtParams params, DwhUnloadStatus status) throws SQLException {
        return repository.selectFirst(
                "select count(1) cnt from GL_ETLSTMS " +
                        "where PARNAME = ? and PARVALUE = ? and PARDESC = ? and OPERDAY = ?"
                , params.getParamName(), status.getFlag(), params.getParamDesc(), executeDate).getLong(0);
    }

    public Date getExecuteDate(Properties properties) throws ParseException {
        String propday = properties.getProperty(STAMT_UNLOAD_FULL_DATE_KEY);
        if ((propday == null) || propday.isEmpty()) {
            return operdayController.getOperday().getCurrentDate();
        } else {
            return DateUtils.parseDate(propday, "dd.MM.yyyy", "yyyy-MM-dd");
        }
    }

    /**
     * создаем временную таблицу <code>GL_TMP_CURDATE</code> и заполняем датой <code>CURDATE</code>
     */
    public void createTemporaryTableWithDate(String tableName, String columnName, Date executeDate) throws Exception {
        repository.executeTransactionally(connection -> {
            try (PreparedStatement statementDeclare = connection.prepareStatement(
                    "declare global temporary table " + tableName + " (\n" +
                            "   "+columnName+" date not null\n" +
                            ") with replace on commit preserve rows");) {
                statementDeclare.executeUpdate();
            }
            try (PreparedStatement statementInsert = connection.prepareStatement(
                    "insert into session."+tableName+" values (?)")) {
                statementInsert.setDate(1, new java.sql.Date(executeDate.getTime()));
                statementInsert.executeUpdate();
            }
            return null;
        });

    }

    /**
     * перенос ранее выгруженных проводок в GL_ETLSTMA и удаление из GL_ETLSTMD
     * @return
     * @throws Exception
     */
    public int moveIntrodayHistory() throws Exception {
        return (int) repository.executeInNewTransaction(persistence -> {
            Integer maxcnt = Optional.ofNullable(repository.selectOne("select max(trycnt) mx from gl_etlstma"))
                    .orElse(new DataRecord().addColumn("mx", 1)).getInteger("mx");
            return repository.executeNativeUpdate(textResourceController
                    .getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/move_introday_backvalue2hist.sql"), maxcnt);
        });
    }
}
