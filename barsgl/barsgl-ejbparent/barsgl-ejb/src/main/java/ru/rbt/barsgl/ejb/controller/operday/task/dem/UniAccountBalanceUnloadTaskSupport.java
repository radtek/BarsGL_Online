package ru.rbt.barsgl.ejb.controller.operday.task.dem;

import org.apache.commons.lang3.time.DateUtils;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.operday.task.stamt.StamtUnloadController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.CoreRepository;

import javax.ejb.AccessTimeout;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.inject.Inject;
import java.sql.PreparedStatement;
import java.text.ParseException;
import java.util.Date;
import java.util.Properties;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MINUTES;
import static ru.rbt.audit.entity.AuditRecord.LogCode.AcccountBalanceOndemandUnload;

/**
 * Created by Ivan Sevastyanov on 25.01.2016.
 */
@Singleton
@AccessTimeout(value = 15, unit = MINUTES)
public class UniAccountBalanceUnloadTaskSupport {

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @EJB
    private AuditController auditController;

    @EJB
    private OperdayController operdayController;

    @EJB
    private CoreRepository repository;

    @Inject
    private ru.rbt.ejbcore.util.DateUtils dateUtils;

    @Inject
    private StamtUnloadController unloadController;

    public void execute(Properties properties) throws Exception {
        final Date executeDate = getExecuteDate(properties);
        try {
            auditController.info(AcccountBalanceOndemandUnload
                , format("Начало выгрузки остатков по требованию за '%s'", dateUtils.onlyDateString(executeDate)));
            auditController.info(AcccountBalanceOndemandUnload, format("Удалено старых записей: '%s'", cleanOld()));
            auditController.info(AcccountBalanceOndemandUnload, format("Выгружено счетов с остатками по требованию за '%s': '%s'"
                , dateUtils.onlyDateString(executeDate), fillData(executeDate)));
        } catch (Throwable e) {
            auditController.error(AcccountBalanceOndemandUnload, format("Ошибка при выгрузке остатков по требованию за '%s'"
                , dateUtils.onlyDateString(executeDate)), null, e);
        }
    }

    private Date getExecuteDate(Properties properties) throws ParseException {
        String propday = properties.getProperty("operday");
        if ((propday == null) || propday.isEmpty()) {
            return operdayController.getOperday().getCurrentDate();
        } else {
            return DateUtils.parseDate(propday, "dd.MM.yyyy", "yyyy-MM-dd");
        }
    }

    private int fillData(Date executeDate) throws Exception {
//        beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
//            try (PreparedStatement createTableStmt = connection.prepareStatement(
//                "DECLARE GLOBAL TEMPORARY TABLE SESSION.GL_BALTUR_SUMS (" +
//                    "  BSAACID CHAR(20) NOT NULL, " +
//                    "  sumamnt DECIMAL(19, 0), " +
//                    "  sumamntbc DECIMAL(19, 0)) WITH REPLACE NOT LOGGED")) {
//
//                createTableStmt.execute();
//            }
//            return null;
//        }, 60 * 60);

        return beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
            unloadController.createTemporaryTableWithDate("GL_TMP_CURDATE", "curdate", executeDate);
            int updateResult = 0;
            try (
                PreparedStatement createTableStmt = connection.prepareStatement(
                            "DECLARE GLOBAL TEMPORARY TABLE SESSION.GL_BALTUR_SUMS (" +
                                    "  BSAACID CHAR(20) NOT NULL, " +
                                    "  sumamnt DECIMAL(19, 0), " +
                                    "  sumamntbc DECIMAL(19, 0)) WITH REPLACE NOT LOGGED");
            ) {
                createTableStmt.execute();
                try (
                        PreparedStatement fillTableStmt = connection.prepareStatement(
                                "INSERT INTO SESSION.GL_BALTUR_SUMS (BSAACID, SUMAMNT, SUMAMNTBC) " +
                                        "  SELECT " +
                                        "    BSAACID, " +
                                        "    SUM(DTAC + CTAC), " +
                                        "    SUM(DTBC + CTBC) " +
                                        "  FROM GL_BALTUR " +
                                        "  WHERE DAT <= ? " +
                                        "  GROUP BY BSAACID");

                        PreparedStatement statement = connection.prepareStatement(
                                "INSERT INTO gl_accrest (dat,acid,acc2,bsaacid,ccy,cbcc,outbal,outbalrur,outbmid,outbrmid,psav,acctype,cnum,deal_id,subdealid)" +
                                        "SELECT od.curdate, b.acid, a.acc2, b.bsaacid\n" +
                                        "       , a.ccy, a.cbcc\n" +
                                        "       , cast((decimal(b.obac + b.dtac + b.ctac+value(s.sumamnt,0))/integer(power(10,c.nbdp))) AS DECIMAL(19,2)) outbal \n" +
                                        "       , (b.obbc + b.dtbc + b.ctbc+value(s.sumamntbc,0))*0.01 outbalrur \n" +
                                        "       , b.obac + b.dtac + b.ctac+value(s.sumamnt,0) outbmid\n" +
                                        "       , b.obbc + b.dtbc + b.ctbc+value(s.sumamntbc,0) outbrmid\n" +
                                        "       , a.psav, a.acctype, a.custno, a.dealid, a.subdealid \n " +
                                        "  FROM baltur b LEFT OUTER JOIN SESSION.GL_BALTUR_SUMS s ON b.bsaacid=s.bsaacid, " +
                                        "          gl_acc a, currency c, (SELECT curdate FROM SESSION.GL_TMP_CURDATE) od\n" +
                                        " WHERE b.acid = a.acid\n" +
                                        "   AND b.bsaacid = a.bsaacid\n" +
                                        "   AND a.dto <= od.curdate\n" +
                                        "   AND value(a.dtc, od.curdate) >= od.curdate\n" +
                                        "   AND b.dat <= od.curdate\n" +
                                        "   AND b.datto >= od.curdate\n" +
                                        "   AND a.ccy = c.glccy");

                        PreparedStatement dropTableStmt = connection.prepareStatement("DROP TABLE SESSION.GL_BALTUR_SUMS")
                ) {
                    fillTableStmt.setDate(1, new java.sql.Date(executeDate.getTime()));
                    fillTableStmt.execute();
                    updateResult = statement.executeUpdate();
                    dropTableStmt.execute();
                }
            }
            return updateResult;

        }, 60 * 60);
    }

    private int cleanOld() throws Exception {
        return (int) repository.executeInNewTransaction(persistence -> {
            return repository.executeNativeUpdate("delete from gl_accrest");
        });

    }

}
