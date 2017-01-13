package ru.rbt.barsgl.ejb.bt;

import ru.rbt.barsgl.ejb.controller.od.OperdaySynchronizationController;
import ru.rbt.barsgl.ejb.etc.TextResourceController;
import ru.rbt.barsgl.ejb.repository.PdRepository;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.datarec.DefaultJdbcAdapter;
import ru.rbt.barsgl.ejbcore.datarec.JdbcAdapter;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.shared.Assert;

import javax.ejb.*;
import javax.inject.Inject;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.BalturRecalc;

/**
 * Created by Ivan Sevastyanov on 11.08.2016.
 */
@Singleton
@Lock(LockType.READ)
@AccessTimeout(unit = TimeUnit.MINUTES, value = 15)
public class BalturRecalculator {

    public static final Logger log = Logger.getLogger(BalturRecalculator.class.getName());

    @EJB
    private PdRepository pdRepository;

    @Inject
    private TextResourceController textController;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private AuditController auditController;

    @Inject
    private TextResourceController resourceController;

    @EJB
    private OperdaySynchronizationController synchronizationController;

    public enum BalturRecalcState {
        NEW("0"),PROCESSED("1"),ERROR("2");

        private String value;

        BalturRecalcState(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * пересчет остатков по GL_BSARC
     * @return Кол-во пересчитанных счетов
     */
    @Lock(LockType.WRITE)
    public int recalculateBaltur() throws Exception {
        return pdRepository.executeInNewTransaction(persistence ->
                pdRepository.executeTransactionally(connection -> {
            int cnt = 0;
            try (PreparedStatement statement = connection.prepareStatement(resourceController.getContent("ru/rbt/barsgl/ejb/bt/balrec_torecalc.sql"))) {
                statement.setString(1, BalturRecalcState.NEW.getValue());
                try (ResultSet rs = statement.executeQuery()){
                    JdbcAdapter adp = new DefaultJdbcAdapter();
                    while (rs.next()) {
                        cnt++;
                        DataRecord record = new DataRecord(rs, adp);
                        try {
                            pdRepository.executeInNewTransaction(p -> recalculateBsaacid(
                                    record.getString("bsaacid"), record.getString("acid"), record.getDate("mdat")));
                            log.info(format("Помечено как пересчитанных записей '%s' по счету '%s' с даты '%s'"
                                    , updateStatusSuccess(record.getString("bsaacid"), record.getString("acid"), record.getDate("mdat"))
                                    , record.getString("bsaacid"), record.getDate("mdat")));
                        } catch (Exception e) {
                            auditController.error(BalturRecalc, format("Ошибка при пересчете остатков по счету '%s' начиная с '%s'"
                                    , record.getString("bsaacid"), dateUtils.onlyDateString(record.getDate("mdat"))), null, e);
                            updateStatusError(record.getString("bsaacid"), record.getString("acid"), record.getDate("mdat"));
                        }
                    }
                }
            }
            return cnt;
        }));
    }

    public void registerChangeMarker(String bsaacid, String acid, Date dat) {
        try {
            pdRepository.executeInNewTransaction(p -> registerChangeMarkerInternal(bsaacid, acid, dat));
        } catch (Exception e) {
            registerChangeMarkerInternal(bsaacid, acid, dat);
        }
    }

    private int recalculateBsaacid(String bsaacid, String acid, Date from) throws Exception {
        DataRecord startBal = pdRepository.selectFirst("select DAT, OBAC, OBBC from BALTUR bal where " +
                "bal.DAT = (select min(DAT) from baltur b where b.dat >= ? and b.bsaacid = ? and b.acid = ?) and bal.bsaacid = ? and bal.acid = ?"
                , from, bsaacid, acid, bsaacid, acid);      // стартовая дата в Baltur
        if (null != startBal) {                             // очистить Baltur с даты
            Date startDat = startBal.getDate("DAT");
            updateBalturAfter(bsaacid, acid, startDat, startBal.getLong("OBAC"), startBal.getLong("OBBC"));
            clearBalturTurnover(bsaacid, acid, startDat);
        }
        List<DataRecord> trnvs = pdRepository.select(textController
                .getContent("ru/rbt/barsgl/ejb/bt/balrec_tovers.sql"), from, bsaacid, acid);
        for (DataRecord trn : trnvs) {
            synchronizationController.syncOneBaltur(trn.getDate("pod"), acid, bsaacid,
                    trn.getLong("dtac"), trn.getLong("dtbc"), trn.getLong("ctac"), trn.getLong("ctbc") );
        }
        return trnvs.size();
    }

    private int recalculateBsaacid_old(String bsaacid, String acid, Date from) throws Exception {
        DataRecord startBal = pdRepository.selectFirst("select * from baltur b where b.dat = ? and b.bsaacid = ? and b.acid = ?"
                , from, bsaacid, acid);
        Assert.isTrue(null != startBal, () -> new DefaultApplicationException(
                format("По счету '%s' не найден оборот за '%s'", bsaacid, dateUtils.onlyDateString(from))));
        // open balance
        Long obac = startBal.getLong("obac");
        Long obbc = startBal.getLong("obbc");
        List<DataRecord> trnvs = pdRepository.select(textController
                .getContent("ru/rbt/barsgl/ejb/bt/balrec_tovers.sql"), from, bsaacid, acid);
        for (DataRecord trn : trnvs) {
            int updCount = updateBaltur(bsaacid, acid, trn.getDate("pod"), obac, obbc
                    , trn.getLong("dtac"), trn.getLong("dtbc"), trn.getLong("ctac"), trn.getLong("ctbc"));
            Assert.isTrue(1 == updCount
                    , () -> new DefaultApplicationException(format("По счету '%s' не найден (кол: %s) оборот за '%s'"
                            , bsaacid, updCount, dateUtils.onlyDateString(from))));
            // close balance
            obac += trn.getLong("dtac") + trn.getLong("ctac");
            obbc += trn.getLong("dtbc") + trn.getLong("ctbc");
        }
        return trnvs.size();
    }

    private int updateBalturAfter(String bsaacid, String acid, Date date, Long obac, Long obbc) {
        return pdRepository.executeNativeUpdate(
                "update baltur set obac = ?, obbc = ? where dat > ? and acid = ? and bsaacid = ?"
                , obac, obbc, date, acid, bsaacid);
    }

    private int clearBalturTurnover(String bsaacid, String acid, Date date) {
        return pdRepository.executeNativeUpdate(
                "update baltur set dtac = 0, dtbc = 0, ctac = 0, ctbc = 0  where dat >= ? and acid = ? and bsaacid = ?"
                , date, acid, bsaacid);
    }

    private int updateBaltur(String bsaacid, String acid, Date date, Long obac, Long obbc, Long dtac, Long dtbc, Long ctac, Long ctbc) {
        return pdRepository.executeNativeUpdate(
                "update baltur set obac = ?, obbc = ?, dtac = ?, dtbc = ?, ctac = ?, ctbc = ?  where dat =? and acid = ? and bsaacid = ?"
            , obac, obbc, dtac, dtbc, ctac, ctbc, date, acid, bsaacid);
    }

    private int updateStatusJournal(String bsaacid, String acid, Date dat, BalturRecalcState state) {
        return pdRepository.executeNativeUpdate("update GL_BSARC set RECTED = ? where bsaacid = ? and acid = ? and dat >= ? and RECTED = ?"
                , state.getValue(), bsaacid, acid, dat, BalturRecalcState.NEW.getValue());
    }

    private int updateStatusError(String bsaacid, String acid, Date dat) {
        return updateStatusJournal(bsaacid, acid, dat, BalturRecalcState.ERROR);
    }

    private int updateStatusSuccess(String bsaacid, String acid, Date dat) {
        return updateStatusJournal(bsaacid, acid, dat, BalturRecalcState.PROCESSED);
    }

    private int registerChangeMarkerInternal(String bsaacid, String acid, Date dat) {
        if (1 > pdRepository.executeNativeUpdate("update GL_BSARC set RECTED = ? where bsaacid = ? and acid = ? and dat = ?"
                , BalturRecalcState.NEW.getValue(), bsaacid, acid, dat)) {
            return pdRepository.executeNativeUpdate("insert into GL_BSARC (RECTED,bsaacid,acid,dat) values (?,?,?,?)"
                    , BalturRecalcState.NEW.getValue(), bsaacid, acid, dat);
        }
        return 1;
    }
}
