package ru.rbt.barsgl.ejb.controller.operday.task;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.OperdayRepository;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountService;
import ru.rbt.barsgl.ejb.repository.GLAccountRequestRepository;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.UnloadPDandUnspents;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.isEmpty;


/**
 * Created by ER22228 on 14.03.2016.
 */
public class UnloadUnspentsToDWHServiceTask implements ParamsAwareRunnable {

    private static final Logger log = Logger.getLogger(UnloadUnspentsToDWHServiceTask.class);

    private static final String unloadSelectSql =
            "SELECT" +
                    "      PDID," +
                    "      T.BSAACID," +
                    "      CASE" +
                    "      WHEN W.ACOD IS NULL" +
                    "      THEN CASE WHEN VALUE(A.RLNTYPE,'0') <> '2' THEN A.ID ELSE NULL END" +
                    "      ELSE NULL" +
                    "      END                                             GLACID," +
                    "      SUBSTRING(T.ACID, 1, 8)                         CNUM," +
                    "      T.CCY," +
                    "      CAST(SUBSTRING(T.ACID, 12, 4) AS NUMERIC(4, 0)) ACOD," +
                    "      CAST(SUBSTRING(T.ACID, 16, 2) AS NUMERIC(2, 0)) ACSQ," +
                    "      CAST(PSTA AS NUMERIC(13, 0))                    PSTA," +
                    "      PSTARUR," +
                    "      CAST(DRCR AS NUMERIC(1, 0))                     DRCR," +
                    "      CAST(SUBSTRING(T.ACID, 18, 3) AS CHARACTER(3))  BRCA," +
                    "      CAST(PREF AS VARCHAR(20))                       PREF," +
                    "      CAST(DLREF AS VARCHAR(20))                      DLREF," +
                    "      CAST(OTRF AS VARCHAR(20))                       OTRF," +
                    "      PSTD                                            PSTB," +
                    "      PROCDATE                                        VALB," +
                    "      EVTID" +
                    "    FROM (" +
                    "           SELECT" +
                    "             D.ID          PDID," +
                    "             D.ACID        ACID," +
                    "             D.BSAACID     BSAACID," +
                    "             D.CCY         CCY," +
                    "             D.POD         PSTD," +
                    "             D.VALD        VALD," +
                    "             ABS(D.AMNT)   PSTA," +
                    "             COALESCE(" +
                    "                 CASE" +
                    "                 WHEN D.AMNT > 0" +
                    "                   THEN 1" +
                    "                 WHEN D.AMNT < 0" +
                    "                   THEN 0" +
                    "                 END," +
                    "                 CASE" +
                    "                 WHEN D.AMNTBC > 0" +
                    "                   THEN 1" +
                    "                 WHEN D.AMNTBC < 0" +
                    "                   THEN 0" +
                    "                 END" +
                    "             )             DRCR," +
                    "             E.DPMT        DPMT," +
                    "             O.PMT_REF     PREF," +
                    "             O.SUBDEALID   DLREF," +
                    "             E.PREF        OTRF," +
                    "             O.PROCDATE    PROCDATE," +
                    "             ABS(D.AMNTBC) PSTARUR," +
                    "             O.EVT_ID EVTID" +
                    "           FROM GL_POSTING P" +
                    "             JOIN GL_OPER O ON P.GLO_REF = O.GLOID" +
                    "             JOIN PD D ON P.PCID = D.PCID" +
                    "             JOIN PDEXT E ON D.ID = E.ID" +
                    "           WHERE VALUE(ACID, '') <> '' AND D.INVISIBLE <> '1' AND O.STATE='POST' " +
                    "                 AND O.PROCDATE BETWEEN ? AND ? " +
                    "         ) T" +
                    "      LEFT JOIN GL_ACC A ON T.BSAACID = A.BSAACID " +
                    "      LEFT JOIN GL_DWHPARM W ON W.ACOD = A.ACOD AND W.SQ = A.SQ ";

    private static String unloadInserSql =
            "INSERT INTO GLVD_PST_V(PDID, BSAACID, GLACID, CNUM, CCY, ACOD, ACSQ, PSTA, PSTARUR, DRCR, BRCA, PREF, DLREF, OTRF, PSTB, VALB, EVTID)" +
                    " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String unloadUnspentsSelectSql2N =
            "SELECT DISTINCT " +
                    "  B.ACID, " +
                    "  B.BSAACID, " +
                    "  B.DAT, " +
                    "  B.DATTO, " +
                    "  B.OBAC + B.DTAC + B.CTAC OBAL, " +
                    "  B.OBBC + B.DTBC + B.CTBC OBALRUR " +
                    "FROM DWH.BALTUR B, (SELECT " +
                    "                      A.ID ACID " +
                    "                    FROM DWH.GL_OPER O, DWH.GL_OD OD, DWH.GL_POSTING PS, DWH.PD D, DWH.GL_SHACOD S, DWH.ACC A " +
                    "                    WHERE " +
                    "                      PS.PCID = D.PCID AND PS.GLO_REF = O.GLOID AND O.POSTDATE BETWEEN ? AND ? AND " +
                    "                      S.ACOD = A.ACOD AND " +
                    "                      S.ACSQ = RIGHT('0' || A.ACSQ, 2) AND D.ACID = A.ID AND S.BSTYPE = '2N' " +
                    "                      AND S.DATTO >= ? AND S.DAT <= ? " +
                    "                      AND D.INVISIBLE <> '1' " +
                    "                    GROUP BY OD.CURDATE, A.ID, D.BSAACID, S.BSTYPE) V, DWH.ACCRLN AC " +
                    "WHERE " +
//            "      V.ACID in ('00000331RUR898601033','00000331RUR898201033') AND " +
                    "      B.ACID = V.ACID " +
                    "      AND B.ACID = AC.ACID AND B.BSAACID = AC.BSAACID " +
                    "      AND B.DATTO >= ? AND B.DAT <= ? " +
                    "      AND AC.RLNTYPE IN ('0','1','2') AND AC.DRLNC = DATE('2029-01-01') " +
                    "ORDER BY B.ACID, B.BSAACID, B.DAT";

    private static final String unloadUnspentsShared2NInsertSql =
            "INSERT INTO DWH.GLVD_BAL_V(DAT,UNLOAD_DAT,ACID,OBAL,OBALRUR,MID_FLAG) " +
                    "VALUES(?,?,?,?,?,?)";

    private static final String unloadUnspentsSelectSql02 =
            "SELECT DISTINCT B.ACID, B.BSAACID, B.DAT, B.DATTO, B.OBAC+B.DTAC+B.CTAC OBAL, B.OBBC+B.DTBC+B.CTBC OBALRUR, A.DRLNO " +
                    "FROM DWH.BALTUR B,  ( " +
                    "    SELECT A.ID ACID, D.BSAACID " +
                    "    FROM DWH.GL_OPER O, DWH.GL_OD OD, DWH.GL_POSTING PS, DWH.PD D, DWH.GL_SHACOD S, DWH.ACC A " +
                    "    WHERE PS.PCID = D.PCID " +
                    "          AND PS.GLO_REF = O.GLOID " +
                    "          AND O.POSTDATE BETWEEN ? AND ? " +
                    "          AND S.ACOD = A.ACOD " +
                    "          AND S.ACSQ = RIGHT('0' || A.ACSQ, 2) " +
                    "          AND D.ACID = A.ID " +
                    "          AND S.BSTYPE IN ('0', '2') " +
                    "          AND S.DATTO >= ? AND S.DAT <= ?  " +
                    "          AND D.INVISIBLE <> '1' " +
                    "    GROUP BY OD.CURDATE, A.ID, D.BSAACID, S.BSTYPE " +
                    "  ) V, DWH.ACCRLN A " +
                    "WHERE B.ACID = A.ACID AND B.BSAACID = A.BSAACID AND B.ACID = V.ACID AND B.BSAACID = V.BSAACID " +
                    "AND (B.DAT BETWEEN ? AND ? OR B.DATTO BETWEEN ? AND ?) " +
                    "ORDER BY B.ACID, B.BSAACID, B.DAT";

    private static final String unloadUnspentsShared02InsertSql =
            "INSERT INTO DWH.GLVD_BAL_V(DAT,UNLOAD_DAT,ACID,BSAACID,OBAL,OBALRUR,MID_FLAG) " +
                    "VALUES(?,?,?,?,?,?,?)";

    private static final String unloadUnspentsSelectSql =
            "SELECT DISTINCT" +
                    "  B.ACID, " +
                    "  B.BSAACID, " +
                    "  B.DAT, " +
                    "  B.DATTO, " +
                    "  B.OBAC + B.DTAC + B.CTAC AS SUM1, " +
                    "  B.OBBC + B.DTBC + B.CTBC AS SUM2, " +
                    "  A.ID                     AS GLACID, " +
                    "  A.DTC, " +
                    "  A.DTO " +
                    "FROM DWH.BALTUR B, DWH.GL_ACC A " +
                    "WHERE " +
                    "  A.ACID = B.ACID AND A.BSAACID = B.BSAACID AND " +
                    "  B.DATTO >= ? AND B.DAT <= ? " +
                    "  AND (B.ACID, B.BSAACID) IN ( " +
                    "    SELECT " +
                    "      D.ACID, " +
                    "      D.BSAACID " +
                    "    FROM DWH.GL_POSTING P " +
                    "      JOIN DWH.GL_OPER O ON P.GLO_REF = O.GLOID " +
                    "      JOIN DWH.PD D ON P.PCID = D.PCID " +
                    "    WHERE " +
                    "      O.POSTDATE BETWEEN ? AND ? AND VALUE(D.ACID, '') <> '' AND D.INVISIBLE <> '1' AND " +
                    "      O.STATE = 'POST' " +
                    "  ) " +
                    "  AND (A.ACOD, A.SQ) NOT IN (SELECT ACOD, SQ FROM DWH.GL_DWHPARM) " +
                    "  AND VALUE(A.RLNTYPE, '0') <> '2' " +
                    "ORDER BY B.BSAACID, B.DAT";

    private static final String unloadUnspentsInsertSql =
            "INSERT INTO DWH.GLVD_BAL_V(DAT,UNLOAD_DAT,ACID,BSAACID,GLACID,OBAL,OBALRUR,MID_FLAG,DTC) " +
                    "VALUES(?,?,?,?,?,?,?,?,?)";

    private static final String etldwhsInsertSql =
            "INSERT INTO DWH.GL_ETLDWHS(PARNAME,PARVALUE,PARDESC,OPERDAY,START_LOAD) " +
                    "VALUES('BARS_GL_DWH',1,?,?,?)";

    private static final String unloadDatSelectSql = "SELECT CURDATE FROM DWH.GL_OD";
    private static final String SCHEDULED_TASK_NAME = "GLVD_PST_V LOAD";
    private static final int batchInsertSize = 1000;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @EJB
    private GLAccountRequestRepository accountRequestRepository;

    @Inject
    private GLAccountService glAccountService;

    @Inject
    private OperdayRepository operdayRepository;

    @EJB
    private AuditController auditController;

    @EJB
    private CoreRepository coreRepository;

    @Inject
    private OperdayController operdayController;

    @Inject
    private ru.rbt.barsgl.ejbcore.util.DateUtils dateUtils;

    private static final String UU_DATE_KEY = "operday";
    private static final String UU_CHECK_RUN = "checkRun";

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        System.out.println("UnloadPDandUnspents started");
        auditController.info(UnloadPDandUnspents, "UnloadPDandUnspents стартовала");
        if (checkRun(operdayController.getOperday(), properties)) {
            executeWork(properties);
            auditController.info(UnloadPDandUnspents, "UnloadUnspentsToDWHServiceTask закончила");
        } else {
            auditController.info(UnloadPDandUnspents, "UnloadUnspentsToDWHServiceTask отложена");
        }
    }

    private java.util.Date getExecuteDate(Properties properties) throws ParseException {
        String propday = Optional.ofNullable(properties.getProperty(UU_DATE_KEY)).orElse("");
        java.util.Date operday;
        if (isEmpty(propday)) {
            operday = operdayController.getOperday().getCurrentDate();
        } else {
            operday = DateUtils.parseDate(propday, "dd.MM.yyyy", "yyyy-MM-dd");
        }
        auditController.info(UnloadPDandUnspents, "operday: " + operday);
        return operday;
    }

    public boolean checkRun(Operday operday, Properties properties) throws Exception {
        if (Boolean.parseBoolean(Optional.ofNullable(properties.getProperty(UU_CHECK_RUN)).orElse("true"))) {
            java.util.Date executeDate = getExecuteDate(properties);
            DataRecord rec = operdayRepository.selectFirst(
                    "select count (1) from GL_ETLDWHS where PARDESC = ? and OPERDAY = ?"
                    , SCHEDULED_TASK_NAME, executeDate);
            boolean already = 0 < rec.getInteger(0);
            if (already) {
                auditController.warning(UnloadPDandUnspents, "Ошибка при выгрузке проводок и остатков", null
                        , format("Выгрузка проводок и остатков невозможна: выгрузка уже запущена или выполнена в текущем ОД (%s) <%s>"
                                , dateUtils.onlyDateString(executeDate), true));
                return false;
            }
            return true;
        } else {
            return true;
        }
    }

    private void executeWork(Properties properties) throws Exception {
        LocalDate curdate = getCurDate();

        // За текущий operday уже выгружали?
        if (checkStep(curdate, "GLVD_PST_V")) {
            return;
        }

        String propCheckRun = Optional.ofNullable(properties.getProperty("checkRun")).orElse("");
        // Если checkRun = false, то не проверяем условия запуска - запускаем
        if (!"false".equals(propCheckRun)) {
            if (!checkStep(curdate, "GLVD_BAL LOAD 3")) {
                return;
            }
        }


        BigDecimal idInsertedEtldwhs = insertIntoEtldwhs("GLVD_PST_V", curdate);
        log.info(" id=" + idInsertedEtldwhs);

        // Очистка таблиц
        auditController.info(UnloadPDandUnspents, "UnloadUnspentsToDWHServiceTask очистка таблиц запущена");
        clearWorkTables();
        auditController.info(UnloadPDandUnspents, "UnloadUnspentsToDWHServiceTask очистка таблиц закончена");

        // Определение периода выгрузки
        LocalDate startPeriod, finishPeriod;

        String propMinDay = Optional.ofNullable(properties.getProperty("minDay")).orElse("");
        String propMaxDay = Optional.ofNullable(properties.getProperty("maxDay")).orElse("");


        if (!isEmpty(propMaxDay) && !isEmpty(propMinDay)) {
            startPeriod = LocalDate.parse(propMinDay);
            finishPeriod = LocalDate.parse(propMaxDay);
        } else {
            log.info(" curdate=" + curdate);
            startPeriod = curdate.minusDays(31);
            finishPeriod = curdate;//.minusDays(1);
        }

        // Выгрузка проводок
        auditController.info(UnloadPDandUnspents, "UnloadUnspentsToDWHServiceTask заполнение GLVD_PST_V стартовало");
        unloadPD(startPeriod, finishPeriod);
        auditController.info(UnloadPDandUnspents, "UnloadUnspentsToDWHServiceTask заполнение GLVD_PST_V закончено");

        updateEtldwhs(idInsertedEtldwhs);

        // Выгрузка остатков
        idInsertedEtldwhs = insertIntoEtldwhs("GLVD_BAL_V", curdate);
        log.info(" id=" + idInsertedEtldwhs);

        auditController.info(UnloadPDandUnspents, "UnloadUnspentsToDWHServiceTask заполнение GLVD_BAL_V стартовало");
        unloadUnspents2N(startPeriod, finishPeriod);
        auditController.info(UnloadPDandUnspents, "UnloadUnspentsToDWHServiceTask заполнение GLVD_BAL_V шаг Shared 2N закончено");
        unloadUnspents(startPeriod, finishPeriod);
        auditController.info(UnloadPDandUnspents, "UnloadUnspentsToDWHServiceTask заполнение GLVD_BAL_V шаг Midas закончено");
        unloadUnspents02(startPeriod, finishPeriod);
        auditController.info(UnloadPDandUnspents, "UnloadUnspentsToDWHServiceTask заполнение GLVD_BAL_V шаг Shared 0&2 закончено");
        auditController.info(UnloadPDandUnspents, "UnloadUnspentsToDWHServiceTask заполнение GLVD_BAL_V закончено");

        // Отмечаем завершение работы в GL_ETLDWHS
        updateEtldwhs(idInsertedEtldwhs);

    }

    private boolean checkStep(LocalDate operday, String taskName) throws Exception {
        return beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT * FROM DWH.GL_ETLDWHS WHERE OPERDAY=? AND PARDESC=? AND PARVALUE=1")) {
                query.setDate(1, Date.valueOf(operday));
                query.setString(2, taskName);
                ResultSet rs = query.executeQuery();
                if (rs.next()) {
                    return true;
                }
            }
            return false;
        }), 60 * 60);
    }

    private void clearWorkTables() throws Exception {
        beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
            try (PreparedStatement query = connection.prepareStatement("DELETE FROM DWH.GLVD_PST_V");
                 PreparedStatement query2 = connection.prepareStatement("DELETE FROM DWH.GLVD_BAL_V")) {
                query.execute();
                query2.execute();
            }
            return 1;
        }), 60 * 60);
    }

    private static final String etldwhsUpdateSql = "UPDATE DWH.GL_ETLDWHS SET END_LOAD=? WHERE ID=?";

    private void updateEtldwhs(BigDecimal idInsertedEtldwhs) {
        try {
            beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
                int i;
                try (PreparedStatement query = connection.prepareStatement(etldwhsUpdateSql)) {
                    query.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    query.setBigDecimal(2, idInsertedEtldwhs);
                    i = query.executeUpdate();
                }
                return i;
            }), 60 * 60);
        } catch (Exception e) {
            log.error("unloadUnspents", e);
        }
    }

    private BigDecimal insertIntoEtldwhs(String message, LocalDate curdate) {
        // Отмечаем начало работы в GL_ETLDWHS
        try {
            return beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
                try (PreparedStatement query = connection.prepareStatement(etldwhsInsertSql, Statement.RETURN_GENERATED_KEYS)) {
                    query.setString(1, message);
                    query.setDate(2, Date.valueOf(curdate));
                    query.setTimestamp(3, new Timestamp(System.currentTimeMillis()));

                    query.executeUpdate();

                    ResultSet rs = query.getGeneratedKeys();
                    if (rs.next()) {
                        return rs.getBigDecimal(1);
                    }
                }
                return BigDecimal.ZERO;
            }), 60 * 60);
        } catch (Exception e) {
            log.error("unloadUnspents", e);
        }
        return BigDecimal.ZERO;
    }

    private void unloadUnspents2N(LocalDate startPeriod, LocalDate finishPeriod) {
        try {
            int num = beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
                int count = 0;
                Date unload_dat = getUnloadDat(connection);
                // Заполняем данными из baltur
                Map<String, Map<String, List<Object[]>>> balturMap = fillFromBalturShared2N(startPeriod, finishPeriod, connection);
                if (balturMap.size() > 0) {
                    count = saveBalVShared2N(startPeriod, finishPeriod, connection, balturMap, unload_dat);
                }
                return count;
            }), 60 * 60);
            log.info(" num=" + num);
            auditController.info(UnloadPDandUnspents, "Вставка в GLVD_BAL_V (Shared BSTYPE='2N'): " + num);
        } catch (Exception e) {
            log.error("unloadUnspents", e);
            auditController.error(UnloadPDandUnspents, e.getMessage(), null, e);
        }
    }

    private Map<String, Map<String, List<Object[]>>> fillFromBalturShared2N(LocalDate startPeriod, LocalDate finishPeriod, Connection connection) throws SQLException {
        Map<String, Map<String, List<Object[]>>> balturMap = new HashMap<>();
        try (PreparedStatement query = connection.prepareStatement(unloadUnspentsSelectSql2N)) {
            query.setDate(1, Date.valueOf(startPeriod));
            query.setDate(2, Date.valueOf(finishPeriod));
            query.setDate(3, Date.valueOf(startPeriod));
            query.setDate(4, Date.valueOf(finishPeriod));
            query.setDate(5, Date.valueOf(startPeriod));
            query.setDate(6, Date.valueOf(finishPeriod));
//            query.setDate(7, Date.valueOf(startPeriod));
//            query.setDate(8, Date.valueOf(finishPeriod));

            try (ResultSet rs = query.executeQuery()) {
                String bsaacid = "";
                String acid = "";
                List<Object[]> oneBsaacid = new ArrayList<Object[]>();
                Map<String, List<Object[]>> oneAcid = new HashMap<String, List<Object[]>>();
                while (rs.next()) {
                    if (!rs.getString("BSAACID").equals(bsaacid)) {
                        // Записывается накопленное
                        if (bsaacid.length() > 0) {
                            oneAcid.put(bsaacid, oneBsaacid);
                            oneBsaacid = new ArrayList<Object[]>();
                        }

                        // Переинициализация для следующего bsaacid
                        bsaacid = rs.getString("BSAACID");
                    }

                    if (!rs.getString("ACID").equals(acid)) {
                        // Записывается накопленное
                        if (acid.length() > 0) {
                            balturMap.put(acid, oneAcid);
                            oneAcid = new HashMap<String, List<Object[]>>();
                        }

                        // Переинициализация для следующего acid
                        acid = rs.getString("ACID");
                    }

                    oneBsaacid.add(new Object[]{null, // пустышка
                            rs.getString(1), rs.getString(2),
                            rs.getDate(3), rs.getDate(4),
                            rs.getBigDecimal(5), rs.getBigDecimal(6)
                    });
                }
                // дописываем последний "пакет"
                oneAcid.put(bsaacid, oneBsaacid);
                balturMap.put(acid, oneAcid);
            }
        }
        balturMap.remove("");
        return balturMap;
    }

    private int saveBalVShared2N(LocalDate startPeriod, LocalDate finishPeriod, Connection connection, Map<String, Map<String, List<Object[]>>> balturMap, Date unload_dat) {
        int count = 0;
        try (PreparedStatement insert = connection.prepareStatement(unloadUnspentsShared2NInsertSql)) {
            LocalDate stopDate = finishPeriod.plusDays(1); // Чтобы <= упростить
            for (LocalDate pDate = startPeriod; pDate.isBefore(stopDate); pDate = pDate.plusDays(1)) {
                log.info("pDate=" + pDate);
                Date curDate = Date.valueOf(pDate);
                //for (Map<String, List<Object[]>> oneAcid : balturMap.values()) {
                for (String key : balturMap.keySet()) {
                    String sumstring = "";
                    Map<String, List<Object[]>> oneAcid = balturMap.get(key);
                    BigDecimal obal = new BigDecimal(0L);
                    BigDecimal obalrur = new BigDecimal(0L);
                    for (List<Object[]> oneBsaacid : oneAcid.values()) {
                        for (Object[] oneBaltur : oneBsaacid) {
                            if (((Date) oneBaltur[3]).compareTo(curDate) > 0 || curDate.compareTo((Date) oneBaltur[4]) > 0)
                                continue;
                            obal = obal.add((BigDecimal) oneBaltur[5]);
                            obalrur = obalrur.add((BigDecimal) oneBaltur[6]);
                            sumstring += "+(" + oneBaltur[5] + ")";
                            break;
                        }
                    }
                    //DAT,UNLOAD_DAT,ACID, OBAL, OBALRUR, MID_FLAG
                    insert.setDate(1, curDate);
                    insert.setDate(2, unload_dat);
                    insert.setString(3, key);
                    insert.setBigDecimal(4, obal);
                    insert.setBigDecimal(5, obalrur);
                    insert.setInt(6, 1);

//                    log.info("step 2N # " + pDate.toString() + " # " + key + " # " + obal + " # " + (sumstring.length() > 0 ? sumstring.substring(1) : "empty"));
//                    System.out.println("step 2N # " + pDate.toString() + " # " + key + " # " + obal + " # " + (sumstring.length() > 0 ? sumstring.substring(1) : "empty"));

                    insert.addBatch();

                    count++;
                    if (count % batchInsertSize == 0) {
                        insert.executeBatch();
                    }
                }
            }
            insert.executeBatch();

        } catch (SQLException e) {
            log.error("Ошибка присвоения. count=" + count, e);
            auditController.error(UnloadPDandUnspents, e.getMessage(), null, e);
        }
        return count;
    }


    /*

     */

    private void unloadUnspents02(LocalDate startPeriod, LocalDate finishPeriod) {
        try {
            int num = beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
                int count = 0;
                Date unload_dat = getUnloadDat(connection);
                // Заполняем данными из baltur
                Map<String, List<Object[]>> balturMap = fillFromBalturShared02(startPeriod, finishPeriod, connection);
                if (balturMap.size() > 0) {
                    count = saveBalVShared02(startPeriod, finishPeriod, connection, balturMap, unload_dat);
                }
                return count;
            }), 60 * 60);
            log.info(" num=" + num);
            auditController.info(UnloadPDandUnspents, "Вставка в GLVD_BAL_V (Shared with BSTYPE in ('0','2')): " + num);
        } catch (Exception e) {
            log.error("unloadUnspents", e);
            auditController.error(UnloadPDandUnspents, e.getMessage(), null, e);
        }
    }

    private Map<String, List<Object[]>> fillFromBalturShared02(LocalDate startPeriod, LocalDate finishPeriod, Connection connection) throws SQLException {
        Map<String, List<Object[]>> balturMap = new HashMap<>();
        try (PreparedStatement query = connection.prepareStatement(unloadUnspentsSelectSql02)) {
            query.setDate(1, Date.valueOf(startPeriod));
            query.setDate(2, Date.valueOf(finishPeriod));
            query.setDate(3, Date.valueOf(startPeriod));
            query.setDate(4, Date.valueOf(finishPeriod));
            query.setDate(5, Date.valueOf(startPeriod));
            query.setDate(6, Date.valueOf(finishPeriod));
            query.setDate(7, Date.valueOf(startPeriod));
            query.setDate(8, Date.valueOf(finishPeriod));

            try (ResultSet rs = query.executeQuery()) {
                String bsaacid = "";
                List<Object[]> oneBsaacid = new ArrayList<Object[]>();
                while (rs.next()) {
                    if (!rs.getString("BSAACID").equals(bsaacid)) {
                        // Записывается накопленное
                        if (bsaacid.length() > 0) {
                            balturMap.put(bsaacid, oneBsaacid);
                            oneBsaacid = new ArrayList<Object[]>();
                        }

                        // Переинициализация для следующего bsaacid
                        bsaacid = rs.getString("BSAACID");
                    }

                    //B.ACID, B.BSAACID, B.DAT, B.DATTO, B.OBAC OBAL, B.OBBC OBALRUR
                    oneBsaacid.add(new Object[]{null, // пустышка
                            rs.getString(1), rs.getString(2),
                            rs.getDate(3), rs.getDate(4),
                            rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getDate(7)
                    });
                }
                // дописываем последний "пакет"
                balturMap.put(bsaacid, oneBsaacid);
            }
        }
        balturMap.remove("");
        return balturMap;
    }

    private int saveBalVShared02(LocalDate startPeriod, LocalDate finishPeriod, Connection connection, Map<String, List<Object[]>> balturMap, Date unload_dat) throws Exception {
        int count = 0;
        try (PreparedStatement insert = connection.prepareStatement(unloadUnspentsShared02InsertSql)) {
            LocalDate stopDate = finishPeriod.plusDays(1); // Чтобы <= упростить
            for (List<Object[]> oneBsaacid : balturMap.values()) {

                // Дата открытия счёта может быть позднее даты начала выгружаемого периода
                if (oneBsaacid == null || oneBsaacid.size() == 0 || oneBsaacid.get(0) == null || oneBsaacid.get(0).length < 8 || oneBsaacid.get(0)[7] == null) {
                    throw new Exception("No opening date for accountCB");
                }
                LocalDate pDate = startPeriod.isBefore(((Date) oneBsaacid.get(0)[7]).toLocalDate()) ?
                        ((Date) oneBsaacid.get(0)[7]).toLocalDate() : startPeriod;

                for (Object[] oneBaltur : oneBsaacid) {
                    LocalDate nearestDatto = ((Date) oneBaltur[4]).toLocalDate().plusDays(1);
                    for (; pDate.isBefore(stopDate) && pDate.isBefore(nearestDatto); pDate = pDate.plusDays(1)) {

                        Date curDate = Date.valueOf(pDate);

                        insert.setDate(1, curDate);
                        //B.ACID, B.BSAACID, B.DAT, B.DATTO, B.OBAC OBAL, B.OBBC OBALRUR
                        //DAT,UNLOAD_DAT,ACID, OBAL, OBALRUR, MID_FLAG
                        insert.setDate(1, curDate);
                        insert.setDate(2, unload_dat);
                        insert.setString(3, (String) oneBaltur[1]);
                        insert.setString(4, (String) oneBaltur[2]);
                        insert.setBigDecimal(5, (BigDecimal) oneBaltur[5]);
                        insert.setBigDecimal(6, (BigDecimal) oneBaltur[6]);
                        insert.setInt(7, 1);

                        insert.addBatch();

                        count++;
                        if (count % batchInsertSize == 0) {
                            insert.executeBatch();
                        }
                    }
                }
            }
            insert.executeBatch();

        } catch (SQLException e) {
            log.error("Ошибка присвоения. count=" + count, e);
            auditController.error(UnloadPDandUnspents, e.getMessage(), null, e);
        }
        return count;
    }


    private void unloadUnspents(LocalDate startPeriod, LocalDate finishPeriod) {
        try {
            int num = beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
                int count = 0;
                Date unload_dat = getUnloadDat(connection);
                // Заполняем данными из baltur
                Map<String, List<Object[]>> balturMap = fillFromBalturGl(startPeriod, finishPeriod, connection);
                // Записываем данные
                if (balturMap.size() > 0) {
                    count = saveBalVGl(startPeriod, finishPeriod, connection, balturMap, unload_dat);
                }

                return count;
            }), 60 * 60);
            log.info(" num=" + num);
            auditController.info(UnloadPDandUnspents, "Вставка в GLVD_PST_V: " + num);
        } catch (Exception e) {
            log.error("unloadUnspents", e);
            auditController.error(UnloadPDandUnspents, e.getMessage(), null, e);
        }
    }

    private Date getUnloadDat(Connection connection) throws SQLException {
        try (PreparedStatement queryUDat = connection.prepareStatement(unloadDatSelectSql);
             ResultSet rs = queryUDat.executeQuery()) {
            if (rs.next()) {
                return rs.getDate(1);
            }
        }
        return null;
    }

    private int saveBalVGl(LocalDate startPeriod, LocalDate finishPeriod, Connection connection, Map<String, List<Object[]>> balturMap, Date unload_dat) throws Exception {
        int count = 0;
        try (PreparedStatement insert = connection.prepareStatement(unloadUnspentsInsertSql)) {
            LocalDate stopDate = finishPeriod.plusDays(1); // Чтобы <= упростить

            for (List<Object[]> oneBsaacid : balturMap.values()) {
                // Дата открытия счёта может быть позднее даты начала выгружаемого периода
                if (oneBsaacid.get(0) == null || oneBsaacid.get(0).length < 10 || oneBsaacid.get(0)[9] == null) {
                    throw new Exception("No opening date for accountCB");
                }
                LocalDate pDate = startPeriod.isBefore(((Date) oneBsaacid.get(0)[9]).toLocalDate()) ?
                        ((Date) oneBsaacid.get(0)[9]).toLocalDate() : startPeriod;

                for (Object[] oneBaltur : oneBsaacid) {
                    LocalDate nearestDatto = ((Date) oneBaltur[4]).toLocalDate().plusDays(1);
                    for (; pDate.isBefore(stopDate) && pDate.isBefore(nearestDatto); pDate = pDate.plusDays(1)) {

                        Date curDate = Date.valueOf(pDate);

                        insert.setDate(1, curDate);
                        insert.setDate(2, unload_dat);
                        insert.setString(3, (String) oneBaltur[1]);
                        insert.setString(4, (String) oneBaltur[2]);
                        insert.setBigDecimal(5, (BigDecimal) oneBaltur[7]);
                        insert.setBigDecimal(6, (BigDecimal) oneBaltur[5]);
                        insert.setBigDecimal(7, (BigDecimal) oneBaltur[6]);
                        insert.setInt(8, 0);
                        insert.setDate(9, (Date) oneBaltur[8]);

                        insert.addBatch();

                        count++;
                        if (count % batchInsertSize == 0) {
                            insert.executeBatch();
                        }
                    }
                }
                insert.executeBatch();
            }
        } catch (SQLException e) {
            log.error("Ошибка присвоения. count=" + count, e);
            auditController.error(UnloadPDandUnspents, e.getMessage(), null, e);
        }
        return count;
    }

    private Map<String, List<Object[]>> fillFromBalturGl(LocalDate startPeriod, LocalDate finishPeriod, Connection connection) throws SQLException {
        Map<String, List<Object[]>> balturMap = new HashMap<>();
        try (PreparedStatement query = connection.prepareStatement(unloadUnspentsSelectSql)) {
            query.setDate(1, Date.valueOf(startPeriod));
            query.setDate(2, Date.valueOf(finishPeriod));
            query.setDate(3, Date.valueOf(startPeriod));
            query.setDate(4, Date.valueOf(finishPeriod));

            try (ResultSet rs = query.executeQuery()) {
                String bsaacid = "";
                List<Object[]> oneBsaacid = new ArrayList<Object[]>();
                while (rs.next()) {
                    if (!rs.getString("BSAACID").equals(bsaacid)) {
                        // Записывается накопленное
                        if (bsaacid.length() > 0) {
                            balturMap.put(bsaacid, oneBsaacid);
                            oneBsaacid = new ArrayList<Object[]>();
                        }

                        // Переинициализация для следующего bsaacid
                        bsaacid = rs.getString("BSAACID");
                    }

                    oneBsaacid.add(new Object[]{null, // пустышка
                            rs.getString(1), rs.getString(2),
                            rs.getDate(3), rs.getDate(4),
                            rs.getBigDecimal(5), rs.getBigDecimal(6),
                            rs.getBigDecimal(7), rs.getDate(8), rs.getDate(9)
                    });
                }
                // дописываем последний "пакет"
                balturMap.put(bsaacid, oneBsaacid);
            }
        }
        return balturMap;
    }

    private void unloadPD(LocalDate startPeriod, LocalDate finishPeriod) throws Exception {
        // Выгрузка проводок
        int num = beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
            int count = 0;
            try (PreparedStatement query = connection.prepareStatement(unloadSelectSql)) {
                query.setDate(1, Date.valueOf(startPeriod));
                query.setDate(2, Date.valueOf(finishPeriod));
                try (ResultSet rs = query.executeQuery();
                     PreparedStatement insert = connection.prepareStatement(unloadInserSql)) {
                    while (rs.next()) {
                        int n = 1;
                        insert.setLong(n, rs.getLong(n));
                        n++;
                        insert.setString(n, rs.getString(n));
                        n++;
                        insert.setLong(n, rs.getLong(n));
                        n++;
                        insert.setString(n, rs.getString(n));
                        n++;
                        insert.setString(n, rs.getString(n));
                        n++;
                        insert.setInt(n, rs.getInt(n));
                        n++;
                        insert.setInt(n, rs.getInt(n));
                        n++;
                        insert.setBigDecimal(n, rs.getBigDecimal(n));
                        n++;
                        insert.setBigDecimal(n, rs.getBigDecimal(n));
                        n++;
                        insert.setInt(n, rs.getInt(n));
                        n++;
                        insert.setString(n, rs.getString(n));
                        n++;
                        insert.setString(n, rs.getString(n));
                        n++;
                        insert.setString(n, rs.getString(n));
                        n++;
                        insert.setString(n, rs.getString(n));
                        n++;
                        insert.setDate(n, rs.getDate(n));
                        n++;
                        insert.setDate(n, rs.getDate(n));
                        n++;
                        insert.setString(n, rs.getString(n));

                        insert.addBatch();

                        count++;
                        if (count % batchInsertSize == 0) {
                            insert.executeBatch();
                        }
                    }
                    insert.executeBatch();
                } catch (SQLException e) {
                    log.error("Ошибка присвоения. count=" + count, e);
                    auditController.error(UnloadPDandUnspents, e.getMessage(), null, e);
                }
            }
            return count;
        }), 60 * 60);

        log.info(" num=" + num);
    }

    private LocalDate getCurDate() {
        List<DataRecord> records = null;
        try {
            records = coreRepository.select("select * from dwh.gl_od", null);
        } catch (SQLException e) {
            log.error("Ошибка при работе с таблицей DWH.GL_OD", e);
            auditController.error(UnloadPDandUnspents, e.getMessage(), null, e);
            e.printStackTrace();
        }
        if (records == null || records.size() == 0) {
            log.error("Таблица DWH.GL_OD либо отсутствует, либо не содержит данных");
            throw new RuntimeException("Таблица DWH.GL_OD либо отсутствует, либо не содержит данных");
        }

        java.sql.Date date;
        if ("ONLINE".equals(records.get(0).getString("PHASE"))) {
            date = records.get(0).getSqlDate("LWDATE");
        } else {
            date = records.get(0).getSqlDate("CURDATE");
        }
        auditController.info(UnloadPDandUnspents, "Таблица DWH.GL_OD. ANSWER=" + date + " / PHASE=" + records.get(0).getString("PHASE") + " LWDATE=" + records.get(0).getSqlDate("LWDATE") + " CURDATE=" + records.get(0).getSqlDate("CURDATE"));

        return date.toLocalDate();
    }
}
