package ru.rbt.barsgl.ejb.controller.od;

import org.apache.commons.lang3.time.DateUtils;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdaySupportBean;
import ru.rbt.barsgl.ejb.controller.operday.task.stamt.StamtUnloadController;
import ru.rbt.barsgl.ejb.entity.gl.*;
import ru.rbt.barsgl.ejb.integr.pst.MemorderController;
import ru.rbt.barsgl.ejb.props.PropertyName;
import ru.rbt.barsgl.ejb.repository.*;
import ru.rbt.barsgl.ejb.repository.props.ConfigProperty;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.barsgl.ejbcore.DbTryingExecutor;
import ru.rbt.barsgl.shared.enums.BalanceMode;
import ru.rbt.barsgl.shared.enums.EnumUtils;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.JpaAccessCallback;
import ru.rbt.ejbcore.controller.etc.TextResourceController;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.shared.Assert;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.*;
import ru.rbt.barsgl.shared.enums.BalanceMode.*;
import static ru.rbt.barsgl.ejb.repository.props.ConfigProperty.SyncIcrementMaxGLPdCount;
import static ru.rbt.barsgl.shared.enums.BalanceMode.*;

/**
 * Created by Ivan Sevastyanov on 12.02.2016.
 * Синхронизация проводок GL_PD и PD
 * Оборотов GL_BALTUR и BALTUR
 */
@Stateless
@LocalBean
public class OperdaySynchronizationController {

    private static final Logger log = Logger.getLogger(OperdaySynchronizationController.class.getName());

    @Inject
    private GLPdRepository glPdRepository;

    @EJB
    private PdRepository pdRepository;

    @EJB
    private GLOperationRepository glOperationRepository;

    @Inject
    private GLPostingRepository postingRepository;

    @EJB
    private MemorderRepository memorderRepository;

    @EJB
    private MemorderController memorderController;

    @EJB
    private AuditController auditController;
    
    @Inject
    private TextResourceController textResourceController;

    @EJB
    private OperdayController operdayController;

    @Inject
    private StamtUnloadController unloadController;

    @EJB
    private AsyncProcessor asyncProcessor;

    @EJB
    private GLBsaAccLockRepository bsaAccLockRepository;

    @Inject
    private ru.rbt.ejbcore.util.DateUtils dateUtils;
    
    @Inject
    private TextResourceController resourceController;

    @EJB
    private PropertiesRepository propertiesRepository;

    @EJB
    private DbTryingExecutor dbTryingExecutor;

    @EJB
    private OperdaySupportBean operdaySupport;

    /**
     * синхронизация проводок и оборотов с буфером
     * @param targetMode в какой режим обработки остатков переходить после сброса буфера,
     *                   или восстановить предыдущее состаяние
     * @return
     * @throws Exception
     */
    public String syncPostings(BalanceMode targetMode) throws Exception {
        final DataRecord bufferStat = glPdRepository.selectFirst(
                "select case when mx is null then 0 else mx end mx, cnt \n" +
                "  from ( \n" +
                "select max(id) mx, count(1) cnt from gl_pd where pd_id is null \n" +
                ") v");
        final Long maxGlPdId = bufferStat.getLong("mx");
        final Long cnt = bufferStat.getLong("cnt");

        if (maxGlPdId == 0) {
            auditController.info(BufferModeSync, "Буферная таблица (GL_PD) пустая, синхронизация полупроводок не требуется");
        }

        final DataRecord balanceStat = glOperationRepository.selectFirst("select count(1) cnt from gl_baltur where moved = 'N'");
        if (balanceStat.getLong("cnt") == 0) {
            auditController.info(BufferModeSync, "Таблица с остатками (GL_BALTUR) пустая, синхронизация оборотов не требуется");
        }

        try {
            if (maxGlPdId == 0 && balanceStat.getLong("cnt") == 0) {
                auditController.info(BufferModeSync, "Таблицы с остатками GL_BALTUR и GL_PD пустые, синхронизация не требуется");
                return "Таблицы с остатками GL_BALTUR и GL_PD пустые, синхронизация не требуется";
            }
            try {
                operdayController.switchBalanceMode(ONDEMAND);
            } catch (Throwable e) {
                String message = "Не удалось отключить триггера перед сбросом буфера";
                auditController.error(BufferModeSync, message, null, e);
                throw new DefaultApplicationException(message, e);
            }

            try {
                return pdRepository.executeInNewTransaction(persistence -> {
                    auditController.info(BufferModeSync, format("Кол-во проводок в буфере GL_PD: '%s'", cnt));

                    Long currentPdSeq = Long.max(pdRepository.getNextId(), getMaxPdId(0L));
                    Long currentGLPdSeq = glPdRepository.getNextId();

                    String msg1, msg2;
                    auditController.info(BufferModeSync, msg1 = format("Перенесено проводок: '%s'", copyGLPd(currentPdSeq, currentGLPdSeq)));
                    auditController.info(BufferModeSync, msg2 = format("Перенесено остатков по счетам в разрезе дат: '%s'", copyBalance()));

                    restartSequencePD(getMaxPdId(0L));
                    return format("%s; \n%s", msg1, msg2);
                });
            } catch (Throwable e) {
                String message = "Ошибка при сбросе буфера";
                auditController.error(BufferModeSync, message, null, e);
                throw new DefaultApplicationException(message, e);
            }
        } finally {
            switchToTargetMode(targetMode);
        }
    }

    /**
     * переносит в историю только синхронизированные данные
     * @param operday
     * @return
     * @throws Exception
     */
    public int moveGLPdsToHistory(Date operday) throws Exception {
        auditController.info(BufferModeSync, format("Перенесено в GL_PD_H: %s"
                , pdRepository.executeNativeUpdate(textResourceController
                        .getContent("ru/rbt/barsgl/ejb/controller/od/move_glpd.sql"), operday)));
        auditController.info(BufferModeSync, format("Удалено перенесенных оборотов: %s"
                , pdRepository.executeNativeUpdate("DELETE FROM GL_BALTUR WHERE MOVED = 'Y'")));
        int cntDeleted = pdRepository.executeNativeUpdate("DELETE FROM GL_PD WHERE PD_ID IS NOT NULL");
        auditController.info(BufferModeSync, format("Удалено перенесенных проводок: %s", cntDeleted));
        return cntDeleted;
    }

    private long copyGLPd(Long currentPdSeq, Long currentGLPdSeq) throws Exception {
        // кол-во секций для синхронизации
        final int partsCount = propertiesRepository.getNumber(ConfigProperty.SyncPieceCount.getValue()).intValue();
        final int partsConcurrency = propertiesRepository.getNumber(ConfigProperty.SyncPieceConcurrency.getValue()).intValue();
        DataRecord record = pdRepository.selectFirst("select count(1) cnt, min(id) mn, max(id) mx from gl_pd where pd_id is null");
        if (null != record && record.getLong("cnt") != 0) {
            long increment = (record.getLong("mx") - record.getLong("mn"))/partsCount;
            List<JpaAccessCallback<Long>> callbacks = new ArrayList<>();
            if (increment > 1) {
                long lmin = record.getLong("mn"), lmax;
                for (int i=0; i < partsCount; i++) {
                    lmax = lmin + increment;
                    if (i == partsCount - 1) {
                        callbacks.add(createPdCallback(lmin, record.getLong("mx"), currentPdSeq, currentGLPdSeq));
                    } else {
                        callbacks.add(createPdCallback(lmin, lmax, currentPdSeq, currentGLPdSeq));
                    }
                    lmin = lmax + 1;
                }
                processAsyncGLPd(callbacks, partsConcurrency);
            } else {
                callbacks.add(createPdCallback(record.getLong("mn"), record.getLong("mx"), currentPdSeq, currentGLPdSeq));
                processAsyncGLPd(callbacks, partsConcurrency);
            }
            return record.getLong("cnt");
        } else {
            auditController.warning(BufferModeSync, "Нет данных для синхронизации полупроводок", null, "");
            return 0;
        }
    }

    private void processAsyncGLPd(List<JpaAccessCallback<Long>> callbacks, int maxConcurrency) throws Exception {
        try {
            asyncProcessor.asyncProcessPooled(callbacks, maxConcurrency, 24, TimeUnit.HOURS);
        } catch (Throwable e) {
            auditController.error(BufferModeSync
                    , format("Ошибка синхронизации полупроводок.", e), null, e);
            throw e;
        }
    }

    private JpaAccessCallback<Long> createPdCallback(final Long minId, final Long maxId, final Long currentPdSeq, final Long currentGLPdSeq) {
        final int[] count = {0};
        log.info(format("Creating copy PD callback by interval: from '%s' to '%s'", minId, maxId));
        return new PdIntervalCallback(minId, maxId, currentPdSeq, currentGLPdSeq);
    }

    private Pd createPd(long sequenceDelta, GLPd glPd) {
        Pd pd = new Pd();
        pd.setId(glPd.getId() + sequenceDelta);
        pd.setPcId(glPd.getPcId() + sequenceDelta);
        pd.setCcy(glPd.getCcy());
        pd.setPref(glPd.getPref());
        pd.setDealId(glPd.getDealId());
        pd.setAmount(glPd.getAmount());
        pd.setGlOperationId(glPd.getGlOperationId());
        pd.setProcDate(glPd.getProcDate());
        pd.setRusNarrLong(glPd.getRusNarrLong());
        pd.setRusNarrShort(glPd.getRusNarrShort());
        pd.setSubdealId(glPd.getSubdealId());
        pd.setOperReference(glPd.getOperReference());
        pd.setEventType(glPd.getEventType());
        pd.setEventId(glPd.getEventId());
        pd.setPaymentRef(glPd.getPaymentRef());
        pd.setIsCorrection(glPd.getIsCorrection());
        pd.setProfitCenter(glPd.getProfitCenter());
        pd.setNarrative(glPd.getNarrative());
        pd.setVald(glPd.getVald());
        pd.setPod(glPd.getPod());
        pd.setPnar(glPd.getPnar());
        pd.setPbr(glPd.getPbr());
        pd.setAmountBC(glPd.getAmountBC());
        pd.setBsaAcid(glPd.getBsaAcid());
        pd.setAuthorizerDepartment(glPd.getAuthorizerDepartment());
        pd.setOperatorDepartment(glPd.getOperatorDepartment());
        pd.setAcid(glPd.getAcid());
        pd.setAuthorizer(glPd.getAuthorizer());
        pd.setOperator(glPd.getOperator());
        pd.setInvisible(glPd.getInvisible());
        pd.setDepartment(glPd.getDepartment());
        pd.setStornoRef(glPd.getStornoRef());
//        pd.setAsoc(glPd.getAsoc());
        pd.setCtype(glPd.getCtype());
        return pdRepository.save(pd, false);
    }

    /**
     * отключаем/включаем триггера на PD (за исключением журналирующих)
     * @return false если не было отключено ни одного триггера
     */
    /*private void swithTriggersPD(boolean off) throws Exception {
        Assert.isTrue(pdRepository.executeTransactionally(connection -> {
            int cnt = 0;
            try (PreparedStatement selectStatement = connection.prepareStatement(
                    " SELECT TRIGGER_NAME, STATUS\n" +
                            "FROM USER_TRIGGERS WHERE TABLE_NAME = 'PD' \n" +
                            " AND TRIGGER_NAME NOT IN ('PD_AI_JRN','PD_AD_JRN','PD_AU_JRN') AND TRIGGER_NAME NOT LIKE '%MIGR%'")
                 ; ResultSet selectResultSet = selectStatement.executeQuery()) {
                while (selectResultSet.next()) {
                    switchOneTrigger(selectResultSet.getString("TRIGGER_NAME"), off);
                    cnt++;
                }
                // проверка статуса триггеров
                try (ResultSet testRs = selectStatement.executeQuery()) {
                    while (testRs.next()) {
                        final String triggerName = testRs.getString("TRIGGER_NAME");
                        final String enabled = testRs.getString("STATUS");
                        Assert.isTrue(enabled.equals(off ? "DISABLED" : "ENABLED")
                                , () -> new DefaultApplicationException(format("Trigger '%s' is in not valid state '%s'", triggerName, enabled)));
                    }
                }
            }
            return cnt > 0;
        }), () -> new DefaultApplicationException(format("Не было переключено ни одного триггера в режим: '%s'", off ? "OFF" : "ON")));
    }

    private void lockTable(String tableName) throws Exception {
        pdRepository.executeNativeUpdate(format("LOCK TABLE %s IN EXCLUSIVE MODE", tableName));
    }*/

    private void restartSequence(String sequenceName, long startWith) throws Exception {
        glPdRepository.executeTransactionally(connection -> {
            final String restartSequence = resourceController.getContent("ru/rbt/barsgl/ejb/controller/od/restart_sequence.sql");
            try (PreparedStatement alterSequence = connection.prepareCall(restartSequence)) {
                alterSequence.setString(1, sequenceName);
                alterSequence.setString(2, Long.toString(startWith));
                alterSequence.executeUpdate();
            }
            return null;
        });
    }

    public void restartSequencePD(long startWith) throws Exception {
        final String sequence = "PD_SEQ";
        auditController.info(BufferModeSync
                , format("Trying to restart current '%s' sequence value to '%s'. Current value is '%s'", sequence, startWith, pdRepository.nextId(sequence)));
        restartSequence(sequence, startWith + 1000);
        auditController.info(BufferModeSync
                , format("Sequence '%s' is restarted. Next (used) value is '%s'", sequence, pdRepository.nextId(sequence)));
    }

    public void restartSequenceGLPD(long startWith) throws Exception {
        restartSequence("SEQ_GL_PD0", startWith);
    }

    public int syncBackvaluePostings(Date operday) {
        try {
            DataRecord countBaltur = pdRepository.selectOne("select count(1) cnt from gl_baltur where dat < ?", operday);
            DataRecord countPd = pdRepository.selectOne("select count(1) cnt from gl_pd where pod < ?", operday);
            auditController.info(BufferModeSyncBackvalue
                    , format("Начало синхронизации backvalue опердень '%s' кол-во счетов '%s' кол-во проводок '%s'"
                            , dateUtils.onlyDateString(operday), countBaltur.getLong(0), countPd.getLong(0)));
            // сразу метим gl_baltur как выгруженный по backvalue
            auditController.info(BufferModeSyncBackvalue, format("Помечено оборотов как выгруженных %s"
                    , pdRepository.executeNativeUpdate("update gl_baltur set moved = 'Y' where dat < ?", operday)));
            // сбрасываем сначала обороты
            int[] count = {0};
            return pdRepository.executeTransactionally(connection -> {
               auditController.info(BufferModeSyncBackvalue, format("Начало синхронизации п/проводок backvalue од '%s'"
                       , dateUtils.onlyDateString(operday)));
               count[0] = 0;
               try (PreparedStatement stGLPd = connection.prepareStatement(
                       "select id from gl_pd p, gl_baltur b where p.bsaacid = b.bsaacid " +
                       "   and p.pod = b.dat and b.moved = 'Y' and pod < ? and pd_id is null")) {
                   stGLPd.setDate(1, new java.sql.Date(operday.getTime()));
                   Long currentPdSeq = Long.max(pdRepository.getNextId(), getMaxPdId(1L));
                   Long currentGLPdSeq = glPdRepository.getNextId();
                   try (ResultSet rs = stGLPd.executeQuery()){
                       while (rs.next()) {
                           syncOneGLPd(rs.getLong(1), currentPdSeq, currentGLPdSeq);
                           count[0]++;
                           if (count[0]%10 == 0) {
                               log.info(format("Синхронизировано проводок backvalue %s", count[0]));
                           }
                       }
                   }
               }
               return count[0];
            });
        } catch (Throwable e) {
            auditController.error(BufferModeSyncBackvalue, format("Ошибка синхронизации backvalue в ОД '%s'", dateUtils.onlyDateString(operday)), null, e);
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public void syncOneBaltur(Date dat, String acid, String bsaacid, Long dtac, Long dtbc, Long ctac, Long ctbc) throws Exception {
        DataRecord recBaltur = pdRepository.selectFirst("select * from baltur b where ? between dat and datto and acid = ? and bsaacid = ?"
                , dat, acid, bsaacid);
        if (null == recBaltur) {
            DataRecord recordLater = pdRepository.selectFirst("select min(dat) mx_dat from baltur where dat > ? and acid = ? and bsaacid = ? group by bsaacid"
                    , dat, acid, bsaacid);
            if (null == recordLater) {
                insertBaltur(dat, DateUtils.parseDate("2029-01-01", "yyyy-MM-dd")
                        , acid,bsaacid, 0L, 0L
                        ,dtac,dtbc,ctac,ctbc);
            } else {
                insertBaltur(dat, DateUtils.addDays(recordLater.getDate("mx_dat"), -1)
                        , acid,bsaacid, 0L, 0L
                        ,dtac,dtbc,ctac,ctbc);
                // обновляем все записи с датами после вставленной оборотами вставленной записи
                updateLater(dat, acid, bsaacid
                        , dtac+ctac
                        , dtbc+ctbc);
            }
        } else {
            // добавляем обороты GL_BALTUR
            if (recBaltur.getDate("dat").equals(dat)) {
                pdRepository.executeNativeUpdate(
                        "update baltur set dtac = dtac + ?, dtbc = dtbc +?, ctac = ctac + ?, ctbc = ctbc + ?, datl = dat where dat =? and acid = ? and bsaacid = ?"
                        , dtac, dtbc,ctac, ctbc
                        , recBaltur.getDate("dat"), recBaltur.getString("acid"), recBaltur.getString("bsaacid"));
                // обновляем все записи с датами после вставленной оборотами вставленной записи
                updateLater(dat, recBaltur.getString("acid"), recBaltur.getString("bsaacid")
                        , dtac+ctac
                        , dtbc+ctbc);
            } else {
                // закрываем предыдущий интервал BALTUR
                pdRepository.executeNativeUpdate("update baltur set datto = ? where dat =? and acid = ? and bsaacid = ?"
                        , DateUtils.addDays(dat, -1), recBaltur.getDate("dat"), recBaltur.getString("acid"), recBaltur.getString("bsaacid"));
                // считаем исходящие остатки и пишем их как входящие в новую запись
                Long obac = recBaltur.getLong("obac") + recBaltur.getLong("dtac") + recBaltur.getLong("ctac");
                Long obbc = recBaltur.getLong("obbc") + recBaltur.getLong("dtbc") + recBaltur.getLong("ctbc");
                insertBaltur(dat, recBaltur.getDate("datto")
                        , acid,bsaacid, obac, obbc
                        , dtac,dtbc,ctac,ctbc);
                // обновляем все записи с датами после вставленной оборотами вставленной записи
                updateLater(dat, recBaltur.getString("acid"), recBaltur.getString("bsaacid")
                        , dtac+ctac
                        , dtbc+ctbc);
            }
        }
    }

    /**
     * сохранение одной полупроводки
     * @param glpdId
     * @param currentPdSeq
     * @param currentGLPdSeq
     */
    private void syncOneGLPd (Long glpdId, Long currentPdSeq, Long currentGLPdSeq) {
        GLPd glPd = glPdRepository.findById(GLPd.class, glpdId);
        Pd pd = createPd(currentPdSeq, glPd);
        if (java.util.Objects.equals(pd.getPcId(), pd.getId())) {
            GLOperation operation = glOperationRepository.findById(GLOperation.class, pd.getGlOperationId());
            // gl_posting
            GLPosting posting = new GLPosting(pd.getPcId(), operation, GLPosting.PostingType.parseType(glPd.getPostType()));
            // инкремент только если ссылка взята из GL_PD
            if (glPd.getStornoPcid() != null) {
                posting.setStornoPcid(glPd.getStornoPcid() < currentGLPdSeq ? glPd.getStornoPcid() + currentPdSeq : glPd.getStornoPcid());
            }
            posting = postingRepository.save(posting, false);
            // pcid_mo
            Memorder memorder = new Memorder();
            memorder.setId(pd.getPcId());
            memorder.setCancelFlag(glPd.getCancelFlag());
            memorder.setDocType(glPd.getDocType());
            memorder.setNumber(glPd.getMemorderNumber());
            memorder.setPostDate(glPd.getPod());
            memorder = memorderRepository.save(memorder, false);
        }
        glPd.setPdId(pd.getId());
        glPdRepository.executeUpdate("update GLPd d set d.pdId = ?1 where d.id = ?2", pd.getId(), glPd.getId());
        glPdRepository.update(glPd, false);
        glPdRepository.flush();
    }

    private int copyBalance() throws Exception {
        // пересчитываем остатки по исключаемым счетам GL_BALTUR
        int[] count = {0};
        pdRepository.executeInNewTransaction(persistence ->
                pdRepository.executeTransactionally(connection -> {
            try (PreparedStatement excludesStatement = connection
                    .prepareStatement("SELECT BSAACID FROM GL_EXBTACC WHERE ? BETWEEN DAT AND DATTO ORDER BY 1")) {
                excludesStatement.setDate(1, operdayController.getOperday().getCurrentSqlDate());
                try (ResultSet excludesResultSet = excludesStatement.executeQuery()){
                    while (excludesResultSet.next()) {
                        List<DataRecord> record = pdRepository
                                .select(textResourceController.getContent("ru/rbt/barsgl/ejb/controller/od/excl_bal.sql")
                                        , excludesResultSet.getString("BSAACID"));
                        record.stream().forEach(r -> {
                            count[0]++;
                            pdRepository.executeNativeUpdate(
                                    "INSERT INTO GL_BALTUR (DAT,ACID,BSAACID,DTAC,DTBC,CTAC,CTBC)\n" +
                                            "VALUES (?, ?, ?, ?, ?, ?, ?)"
                                    , r.getDate("POD"), r.getString("ACID"), r.getString("BSAACID")
                                    , r.getLong("DTAC"), r.getLong("DTBC"), r.getLong("CTAC"), r.getLong("CTBC"));
                        });
                    }
                }
            }
            auditController.info(BufferModeSync, format("Пересчитано остатков по исключенным счетам в разрезе дат: '%s'", count[0]));
            return count[0];
        }));
        // переносим остатки в BALTUR
        count[0] = 0;
        final int maxConcurrency = propertiesRepository.getNumber(ConfigProperty.SyncBalturConcurrency.getValue()).intValue();
        return pdRepository.executeTransactionally(connection -> {
            try (PreparedStatement statementGLBal = connection.prepareStatement("select * from gl_baltur where moved = 'N'");
                    ResultSet statementGLBalRs = statementGLBal.executeQuery()){
                List<JpaAccessCallback<Void>> balturCallbacks = new ArrayList<>();
                while (statementGLBalRs.next()) {
                    count[0]++;
                    balturCallbacks.add(createBalturCallback(maxConcurrency, statementGLBalRs.getDate("dat"), statementGLBalRs.getString("acid")
                            , statementGLBalRs.getString("bsaacid"), statementGLBalRs.getLong("dtac")
                            , statementGLBalRs.getLong("dtbc"), statementGLBalRs.getLong("ctac"), statementGLBalRs.getLong("ctbc")));
                    if (maxConcurrency == balturCallbacks.size()) {
                        processGLBalurAsync(balturCallbacks, maxConcurrency, count[0]);
                        balturCallbacks.clear();
                    }
                }
                if (!balturCallbacks.isEmpty()) {
                    processGLBalurAsync(balturCallbacks, maxConcurrency, count[0]);
                }
            }
            return count[0];
        });
    }

    private void processGLBalurAsync(List<JpaAccessCallback<Void>> balturCallbacks, int maxConcurrency, int currentCount) throws Exception {
        try {
            asyncProcessor.asyncProcessPooled(balturCallbacks, maxConcurrency, 24, TimeUnit.HOURS);
            if (currentCount % 50 == 0) {
                log.info(format("Пересчитано остатков по датам: '%s'", currentCount));
            }
        } catch (Exception e) {
            auditController.error(BufferModeSync
                    , format("Ошибка пересчета остатков. Текущее значение счетчика: %s", currentCount), null, e);
            throw e;
        }
    }

    private JpaAccessCallback<Void> createBalturCallback(final int maxConcurrency, Date dat
            , String acid, String bsaacid, Long dtac, Long dtbc, Long ctac, Long ctbc) {
        log.info(format("Creating baltur callback acid '%s' bsaacid '%s' dat '%s'", acid, bsaacid, dateUtils.onlyDateString(dat)));
        return new BalturCallback(acid, bsaacid, dat, maxConcurrency, dtac, dtbc, ctac, ctbc);
    }

    private void insertBaltur(Date dat, Date datto, String acid, String bsaacid, Long obac, Long obbc, Long dtac, Long dtbc, Long ctac, Long ctbc) {
        String incl = bsaacid.substring(0,1).equals("7") && !bsaacid.substring(5,8).equals("810") ? "1" : "0";
        pdRepository.executeNativeUpdate("INSERT INTO BALTUR (DAT,DATTO,DATL,ACID,BSAACID,OBAC,OBBC,DTAC,DTBC,CTAC,CTBC,INCL) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"
            ,dat,datto,dat,acid,bsaacid,obac,obbc,dtac,dtbc,ctac,ctbc,incl);
    }

    public Long getMaxPdId(Long plus) throws SQLException {
        return pdRepository.selectFirst("select max(id)+1 mx from pst").getLong("mx") + plus;
    }

    /**
     * обновляем входящие остатки в интервалах с датой начала больше  <code>dat</code>
     */
    private void updateLater(Date dat, String acid, String bsaacid, Long obacDelta, Long obbcDelta) {
        pdRepository.executeNativeUpdate("update baltur set obac = obac + ?, obbc = obbc + ? where dat > ? and acid = ? and bsaacid = ?"
                , obacDelta,obbcDelta,dat,acid,bsaacid);
    }

    private class PdIntervalCallback implements JpaAccessCallback<Long> {

        private final Long minId;
        private final Long maxId;
        private final Long currentPdSeq;
        private final Long currentGLPdSeq;

        public PdIntervalCallback(Long minId, Long maxId, Long currentPdSeq, final Long currentGLPdSeq) {
            this.minId = minId;
            this.maxId = maxId;
            this.currentPdSeq = currentPdSeq;
            this.currentGLPdSeq = currentGLPdSeq;
        }

        @Override
        public Long call(EntityManager persistence) throws Exception {
            final long[] count = {0};
            try {
                return pdRepository.executeTransactionally(connection -> {
                    log.info(format("Starting copy PD by interval: from '%s' to '%s'", minId, maxId));
                    auditController.warning(BufferModeSync, format("Скорректировано мемордеров '%s' в интервале с '%s' по '%s'", fixIntersectedMONO(minId, maxId), minId, maxId));
                    try (PreparedStatement statement = connection.prepareStatement("select id from gl_pd where id between ? and ? and pd_id is null")) {
                        statement.setLong(1, minId);
                        statement.setLong(2, maxId);
                        try (ResultSet resultSet = statement.executeQuery()) {
                            while (resultSet.next()) {
                                count[0]++;
                                syncOneGLPd(resultSet.getLong(1), currentPdSeq, currentGLPdSeq);
                                if (count[0] % 100 == 0) {
                                    log.info(format("Перенесено проводок '%s' в интервале с '%s' по '%s'", count[0], minId, maxId));
                                }
                            }
                        }
                    }
                    log.info(format("Перенесено проводок '%s' в интервале с '%s' по '%s'", count[0], minId, maxId));
                    return count[0];
                });
            } catch (Exception e) {
                auditController.error(BufferModeSync, format("Ошибка при синхронизации полупроводок. Интервал ID с '%s' по '%s'. Текущее знач счетчика %s"
                        , minId, maxId, count[0]), null, e);
                throw e;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PdIntervalCallback that = (PdIntervalCallback) o;

            if (!minId.equals(that.minId)) return false;
            return maxId.equals(that.maxId);

        }

        @Override
        public int hashCode() {
            int result = minId.hashCode();
            result = 31 * result + maxId.hashCode();
            return result;
        }

        private int fixIntersectedMONO(long fromId, long toId) throws Exception {
            return pdRepository.executeTransactionally(conn -> {
                try (PreparedStatement select = conn.prepareStatement(textResourceController.getContent("ru/rbt/barsgl/ejb/controller/od/sync_existence_mo_no.sql"))
                    ){
                    select.setLong(1, fromId);
                    select.setLong(2, toId);
                    try (ResultSet selectRs = select.executeQuery()){
                        int i = 0;
                        while (selectRs.next()) {
                            GLOperation operation = glOperationRepository.findById(GLOperation.class, selectRs.getLong("GLO_REF"));
                            Assert.notNull(operation, "GLOperation is null");
                            Assert.isTrue(1 == glPdRepository.executeNativeUpdate("update gl_pd p set p.mo_no = ? where p.id = ?"
                                    , memorderController.nextMemorderNumber(new Date(selectRs.getDate("POD").getTime()), selectRs.getString("BSAACID"), operation.isCorrection()), selectRs.getLong("ID"))
                                    , "gl_pd is not exists");
                            i++;
                        }
                        return i;
                    }
                }
            });
        }
    }

    private class BalturCallback implements JpaAccessCallback<Void> {

        private final String acid;
        private final String bsaacid;
        private final Date dat;
        private final int maxConcurrency;
        private final Long dtac;
        private final Long dtbc;
        private final Long ctac;
        private final Long ctbc;

        public BalturCallback(String acid, String bsaacid, Date dat, int maxConcurrency
            , Long dtac, Long dtbc, Long ctac, Long ctbc) {
            this.acid = acid;
            this.bsaacid = bsaacid;
            this.dat = dat;
            this.maxConcurrency = maxConcurrency;
            this.dtac = dtac;
            this.dtbc = dtbc;
            this.ctac = ctac;
            this.ctbc = ctbc;
        }

        @Override
        public Void call(EntityManager persistence) throws Exception {

            final int[] counter = {0};
            pdRepository.executeInNewTransaction(persistence1 -> {
                while (counter[0] <= maxConcurrency) {
                    counter[0]++;
                    try {
                        pdRepository.executeInNewTransaction(persistence2 -> {
                            log.info(format("Copying balance acid '%s' bsaacid '%s' dat '%s'" , acid, bsaacid, dat));
                            bsaAccLockRepository.createOrUpdateLock(new GLBsaAccLock(bsaacid, operdayController.getSystemDateTime()));
                            syncOneBaltur(dat, acid, bsaacid, dtac, dtbc, ctac, ctbc);
                            pdRepository.executeNativeUpdate("update gl_baltur set moved = 'Y' where bsaacid = ? and dat = ?", bsaacid, dat);
                            return null;
                        });
                        break;
                    } catch (Exception e) {
                        String error = format("Ошибка при пресчете остатков по счету bsaacid: '%s' acid: '%s' за дату: %s. Попытка: %s"
                                , bsaacid, acid, dat, counter[0]);
                        if (counter[0] < maxConcurrency) {
                            log.log(Level.SEVERE, error, e);
                            TimeUnit.MILLISECONDS.sleep(50);
                        } else {
                            auditController.error(BufferModeSync, error, null, e);
                            throw new DefaultApplicationException(error);
                        }
                    }
                }
                return null;
            });

            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BalturCallback that = (BalturCallback) o;

            if (!acid.equals(that.acid)) return false;
            if (!bsaacid.equals(that.bsaacid)) return false;
            return dat.equals(that.dat);

        }

        @Override
        public int hashCode() {
            int result = acid.hashCode();
            result = 31 * result + bsaacid.hashCode();
            result = 31 * result + dat.hashCode();
            return result;
        }
    }

    /**
     * Статистика данных в буфере
     * @return DataRecord с полями pd_cnt - кол-во проводок, bal_cnt - кол-во оборотов в буфере
     * @throws Exception
     */
    public DataRecord getBifferStatistic() throws Exception {
        return pdRepository.selectFirst(resourceController.getContent("ru/rbt/barsgl/ejb/controller/od/buffer_stats.sql"));
    }

    /**
     * Ожидаем окончания обработки
     * @return true если обработка остановлена
     * @throws Exception
     */
    public boolean waitStopProcessing() {
        try {
            return waitStopProcessingOnly();
        } catch (Throwable e) {
            auditController.error(Operday, "Ошибка ожидания остановки обработки", null, e);
            return false;
        }
    }

    public boolean waitStopProcessingOnly() throws Exception {
        if (operdayController.isProcessingAllowed()
                || ProcessingStatus.REQUIRED == operdayController.getProcessingStatus()) {
            auditController.warning(Operday, format("Обработка проводок в статусе '%s' требуется остановка"
                    , operdayController.getProcessingStatus()), null, "");
            if (ProcessingStatus.STARTED == operdayController.getProcessingStatus()) {
                pdRepository.executeInNewTransaction(p ->  {operdayController.setProcessingStatus(ProcessingStatus.REQUIRED); return null; });
            }
            int timeout = (int)(long)propertiesRepository.getNumberDef(PropertyName.STOP_PROC_TIMEOUT.getName(), 3L) * 6;   // интервалов по 10 секунд
            int tryCount = 0;
            while (tryCount < timeout) {
                tryCount++;
                TimeUnit.SECONDS.sleep(10);
                if (ProcessingStatus.STOPPED == operdayController.getProcessingStatus()) {
                    return true;
                }
            }
            return false;
        } else {
            return true;
        }
    }


    public void restorePreviousTriggersState() throws Exception {
        try {
            dbTryingExecutor.tryExecuteTransactionally((conn, att) -> {
                operdaySupport.removeLock(OperdaySupportBean.PST_TABLE_NAME);
                pdRepository.executeNativeUpdate("begin GLAQ_PKG_UTL.RESTORE_PST_TRIGGERS; end;");
                return null;
            }, 3, TimeUnit.SECONDS, 5);
        } catch (Throwable e) {
            auditController.error(BufferModeSync, "Не удалось восстановить предыдущее состояние триггеров на PST", null, e);
            throw e;
        }
    }

    private void switchToTargetMode(BalanceMode  targetMode) throws Exception {
        if (EnumUtils.contains(new BalanceMode[]{GIBRID,ONLINE,ONDEMAND}, targetMode)) {
            operdayController.switchBalanceMode(targetMode);
        }
        else if (NOCHANGE == targetMode) {
            restorePreviousTriggersState();
        }
    }

}


