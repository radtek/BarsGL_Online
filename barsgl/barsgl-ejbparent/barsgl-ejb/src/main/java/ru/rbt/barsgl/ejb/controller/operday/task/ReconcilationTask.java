package ru.rbt.barsgl.ejb.controller.operday.task;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.datarec.DefaultJdbcAdapter;
import ru.rbt.ejbcore.datarec.JdbcAdapter;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejbcore.mapping.YesNo;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;

/**
 * Created by Ivan Sevastyanov
 */
public class ReconcilationTask implements ParamsAwareRunnable {


    private static final Logger log = Logger.getLogger(ReconcilationTask.class);

    public static final String RECONCILATION_TASK_OPERDAY_KEY = "operday";

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @EJB
    private OperdayController operdayController;

    @EJB
    private GLOperationRepository operationRepository;

    @Inject
    private ru.rbt.ejbcore.util.DateUtils dateUtils;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        final String operdayString = properties.getProperty(RECONCILATION_TASK_OPERDAY_KEY);
        log.warn(format("Не установлено свойство задания '%s', будет использован по умолчанию последний рабочий день '%s'''"
                ,RECONCILATION_TASK_OPERDAY_KEY, dateUtils.onlyDateString(operdayController.getOperday().getLastWorkingDay())));
        Date operday;
        if (null != operdayString) {
            operday = DateUtils.parseDate(operdayString, "dd.MM.yyyy", "yyyy-MM-dd");
        } else {
            operday = operdayController.getOperday().getLastWorkingDay();
        }
        execute(operday);
    }

    public void execute(Date operday) throws Exception {
        int deleted = operationRepository.executeInNewTransaction(persistence -> cleanOld(operday));
        log.info(format("Удалено '%s' старых записаей за операционный день '%s'"
                , deleted, dateUtils.onlyDateString(operday)));

        final JdbcAdapter adapter = new DefaultJdbcAdapter();
        beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
            int count = fillMd(connection, operday, adapter);
            log.info(format("Загружено проводок Майдас '%s'", count));

            count = fillAe(connection, operday, adapter);
            log.info(format("Загружено проводок АЕ '%s'", count));

            log.info("Выверка обычных проводок");
            reconcilationSimple(connection, operday, adapter);
            log.info("Выверка веерных проводок");
            reconcilationFan(connection, operday, adapter);
            log.info("Выверка МФО проводок");
            reconcilationMfo(connection, operday, adapter);

            return null;
        }, 60 * 60);
        log.info("Процедура реконсиляции выполнена");
    }

    private int fillMd(Connection connection, Date operday, JdbcAdapter adapter) throws SQLException {
        int count = 0;
        try (PreparedStatement statement = connection.prepareStatement(
                "select p.*," +
                "       case when p.amnt > 0 then 'C' else 'D' end  fin" +
                " from pst p where pod = ? and pbr = 'PH' and invisible = '1'")) {
            statement.setDate(1, new java.sql.Date(operday.getTime()));
            try (ResultSet rs = statement.executeQuery();
                 PreparedStatement insert = connection.prepareStatement(
                         "insert into gl_recpdmd (" +
                                 "pdid    " +
                                 ",vald       " +
                                 ",acid       " +
                                 ",bsaacid    " +
                                 ",amnt       " +
                                 ",pref       " +
                                 ",pnar       " +
                                 ",reg_date   " +
                                 ",fin   " +
                                 ") values (?,?,?,?,?,?,?,?,?)")){
                while (rs.next()) {
                    DataRecord record = new DataRecord(rs, adapter);
                    insert.setLong(1, record.getLong("ID"));
                    insert.setDate(2, new java.sql.Date(record.getDate("VALD").getTime()));
                    insert.setString(3, record.getString("ACID"));
                    insert.setString(4, record.getString("BSAACID"));
                    insert.setLong(5, record.getLong("AMNT"));
                    insert.setString(6, record.getString("PREF"));
                    insert.setString(7, record.getString("PNAR"));
                    insert.setDate(8, new java.sql.Date(record.getDate("POD").getTime()));
                    insert.setString(9, record.getString("FIN"));
                    insert.addBatch();
                    count++;
                    if (count % 100 == 0) {
                        insert.executeBatch();
                    }
                }
                insert.executeBatch();
            }
            return count;
        }
    }

    private int fillAe(Connection connection, Date operday, JdbcAdapter adapter) throws SQLException {
        int count = 0;
        try (PreparedStatement select = connection.prepareStatement(
                "select p.id pdid, p.vald, p.acid, p.bsaacid, p.amnt, e.pref, p. pnar, o.curdate, p.invisible," +
                        "case when ps.post_type = '5' then 'Y' else 'N' end fan_yn, " +
                        "case when ps.post_type in ('3', '4') and (p.bsaacid like '30301%' or p.bsaacid like '30302%' or p.bsaacid like '30305%' or p.bsaacid like '30306%') then 'Y' else 'N' end  mfo_yn," +
                        "case when p.amnt > 0 then 'C' else 'D' end  fin" +
                " from gl_oper o, gl_posting ps, pd p, pdext e " +
                "where o.gloid = ps.glo_ref\n" +
                "  and ps.pcid = p.pcid " +
                "  and p.id = e.id " +
                "  and o.curdate =  ? " +
                "  and p.pbr = '@@GL-PH'")) {
            select.setDate(1, new java.sql.Date(operday.getTime()));
            try (ResultSet rs = select.executeQuery();
                 PreparedStatement insert = connection.prepareStatement(
                         "insert into gl_recpdae (" +
                                 "pdid    " +
                                 ",vald       " +
                                 ",acid       " +
                                 ",bsaacid    " +
                                 ",amnt       " +
                                 ",pref       " +
                                 ",pnar       " +
                                 ",reg_date   " +
                                 ",invisible   " +
                                 ",fan_yn   " +
                                 ",mfo_yn   " +
                                 ",fin   " +
                                 ") values (?,?,?,?,?,?,?,?,?,?,?,?)")){
                count++;
                while (rs.next()) {
                    DataRecord record = new DataRecord(rs, adapter);
                    insert.setLong(1, record.getLong("PDID"));
                    insert.setDate(2, new java.sql.Date(record.getDate("VALD").getTime()));
                    insert.setString(3, record.getString("ACID"));
                    insert.setString(4, record.getString("BSAACID"));
                    insert.setLong(5, record.getLong("AMNT"));
                    insert.setString(6, record.getString("PREF"));
                    insert.setString(7, record.getString("PNAR"));
                    insert.setDate(8, new java.sql.Date(record.getDate("CURDATE").getTime()));
                    insert.setString(9, record.getString("INVISIBLE"));
                    insert.setString(10, record.getString("FAN_YN"));
                    insert.setString(11, record.getString("MFO_YN"));
                    insert.setString(12, record.getString("FIN"));
                    insert.addBatch();
                    count++;
                    if (count % 100 == 0) {
                        insert.executeBatch();
                    }
                }
                insert.executeBatch();
            }
        }
        return count;
    }

    private int cleanOld(Date operday) {
        int count = operationRepository.executeNativeUpdate("delete from gl_recpdmd where reg_date = ?", operday);
        count += operationRepository.executeNativeUpdate("delete from gl_recpdae where reg_date = ?", operday);
        return count;
    }

    private void reconcilationSimple(Connection connection, Date operday, JdbcAdapter adapter) throws SQLException {
        final AtomicBoolean success = new AtomicBoolean(true);
        try (PreparedStatement selectAe = connection.prepareStatement("select * from gl_recpdae where reg_date = ? and invisible = '0' and fan_yn = 'N' and mfo_yn = 'N'")) {
            selectAe.setDate(1, new java.sql.Date(operday.getTime()));
            try (ResultSet resultSetAe = selectAe.executeQuery()){
                while (resultSetAe.next()) {
                    DataRecord currentAe = new DataRecord(resultSetAe, adapter);
                    try (PreparedStatement updateAe = connection.prepareStatement("update gl_recpdae set rec_yn = ? where pdid = ?");
                         PreparedStatement updateMd = connection.prepareStatement("update gl_recpdmd set rec_pdid = ? where vald = ? and bsaacid = ? and amnt = ? and pref = ? and pnar = ?")
                    ){
                        updateMd.setLong(1, currentAe.getLong("PDID"));
                        updateMd.setDate(2, currentAe.getSqlDate("VALD"));
                        updateMd.setString(3, currentAe.getString("BSAACID"));
                        updateMd.setLong(4, currentAe.getLong("AMNT"));
                        updateMd.setString(5, currentAe.getString("PREF"));
                        updateMd.setString(6, currentAe.getString("PNAR"));
                        int cnt = updateMd.executeUpdate();
                        if (0 == cnt) {
                            updateAe.setString(1, "N");
                            updateAe.setLong(2, currentAe.getLong("PDID"));
                            success.compareAndSet(true, false);
                        } else {
                            updateAe.setString(1, "Y");
                            updateAe.setLong(2, currentAe.getLong("PDID"));
                        }
                        updateAe.executeUpdate();
                    }
                }
            }
        }
        if (!success.get()) {
            log.warn(format("Найдены расхождения при выверке обычных проводок за '%s'", dateUtils.onlyDateString(operday)));
        } else {
            log.info(format("Не найдено расхождений при выверке обычных проводок за '%s'", dateUtils.onlyDateString(operday)));
        }
    }

    private void reconcilationFan(Connection connection, Date operday, JdbcAdapter adapter) throws SQLException {
        final AtomicBoolean success = new AtomicBoolean(true);
        try (PreparedStatement selectAe = connection.prepareStatement(
                "select * from GL_RECPDAE where REG_DATE = ? and INVISIBLE = '0' and FAN_YN = 'Y' and MFO_YN = 'N'")){
            selectAe.setDate(1, new java.sql.Date(operday.getTime()));
            try (ResultSet selectAeResultSet = selectAe.executeQuery()) {
                while (selectAeResultSet.next()) {
                    DataRecord recordAe = new DataRecord(selectAeResultSet, adapter);
                    List<DataRecord> mdRecords = operationRepository
                            .select("select * from gl_recpdmd where rec_pdid is null and fin = ? and vald = ? and bsaacid = ? and pref = ?"
                                    ,recordAe.getString("FIN"), recordAe.getSqlDate("VALD"),
                                    recordAe.getString("BSAACID"), recordAe.getString("PREF"));
                    if (0 == mdRecords.size()) {
                        success.compareAndSet(true, false);
                        setAeYN(recordAe.getLong("PDID"), YesNo.N);
                    } else if(1 == mdRecords.size()) {
                        setAeYN(recordAe.getLong("PDID"), YesNo.Y);
                        setMdRecId(mdRecords.get(0).getLong("PDID"), recordAe.getLong("PDID"));
                    } else {
                        long total = 0;
                        for (DataRecord mdrecord : mdRecords) {
                            total += mdrecord.getLong("AMNT");
                        }
                        if (total == recordAe.getLong("AMNT")) {
                            setAeYN(recordAe.getLong("PDID"), YesNo.Y);
                            for (DataRecord mdrecord : mdRecords) {
                                setMdRecId(mdrecord.getLong("PDID"), recordAe.getLong("PDID"));
                            }
                        } else {
                            success.compareAndSet(true, false);
                            setAeYN(recordAe.getLong("PDID"), YesNo.N);
                        }
                    }
                }
            }
        }
        if (!success.get()) {
            log.warn(format("Найдены расхождения при выверке веерных проводок за '%s'", dateUtils.onlyDateString(operday)));
        } else {
            log.info(format("Не найдено расхождений при выверке веерных проводок за '%s'", dateUtils.onlyDateString(operday)));
        }
    }

    private void reconcilationMfo(Connection connection, Date operday, JdbcAdapter adapter) throws SQLException {
        final AtomicBoolean success = new AtomicBoolean(true);
        try (PreparedStatement selectAe = connection.prepareStatement(
                "select * from GL_RECPDAE where REG_DATE = ? and INVISIBLE = '0' and MFO_YN = 'Y'")){
            selectAe.setDate(1, new java.sql.Date(operday.getTime()));
            try (ResultSet selectAeResultSet = selectAe.executeQuery()) {
                while (selectAeResultSet.next()) {
                    final DataRecord recordAe = new DataRecord(selectAeResultSet, adapter);
                    final DataRecord recordMd = operationRepository
                            .selectFirst("select * from gl_recpdmd where rec_pdid is null and vald = ? and pnar = ? and pref = ? and amnt = ?"
                                    , recordAe.getSqlDate("VALD"), recordAe.getString("PNAR")
                                    , recordAe.getString("PREF"), recordAe.getLong("AMNT"));
                    if (null != recordMd
                            && isSameMfoAccount(recordAe.getString("BSAACID"), recordMd.getString("BSAACID"))) {
                        setAeYN(recordAe.getLong("PDID"), YesNo.Y);
                        setMdRecId(recordMd.getLong("PDID"), recordAe.getLong("PDID"));
                    } else {
                        success.compareAndSet(true, false);
                        setAeYN(recordAe.getLong("PDID"), YesNo.N);
                    }
                }
            }
        }
        if (!success.get()) {
            log.warn(format("Найдены расхождения при выверке МФО проводок за '%s'", dateUtils.onlyDateString(operday)));
        } else {
            log.info(format("Не найдено расхождений при выверке МФО проводок за '%s'", dateUtils.onlyDateString(operday)));
        }
    }

    private void setAeYN(long pdid, YesNo yn) {
        operationRepository.executeNativeUpdate("update gl_recpdae set rec_yn = ? where pdid = ?", yn.name(), pdid);
    }

    private void setMdRecId(long mdpdId, long aepdId) {
        operationRepository.executeNativeUpdate("update gl_recpdmd set rec_pdid = ? where pdid = ?", aepdId, mdpdId);
    }

    private boolean isSameMfoAccount(String bsaacidAe, String bsaacidMd) throws SQLException {
        if ((bsaacidMd.matches("^30301.*") && bsaacidAe.matches("(^30301.*)|(^30305.*)"))
                || (bsaacidMd.matches("^30302.*") && bsaacidAe.matches("(^30302.*)|(^30306.*)"))) {
            final String aeFilial = operationRepository.getFilialByAccount(bsaacidAe);
            final String mdFilial = operationRepository.getFilialByAccount(bsaacidMd);
            return null != aeFilial && null != mdFilial && aeFilial.equals(mdFilial);
        } else {
            return false;
        }
    }
}
