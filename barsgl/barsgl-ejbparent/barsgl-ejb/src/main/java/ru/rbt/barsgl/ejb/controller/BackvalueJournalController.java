package ru.rbt.barsgl.ejb.controller;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.gl.BackvalueJournal;
import ru.rbt.barsgl.ejb.entity.gl.BackvalueJournal.BackvalueJournalState;
import ru.rbt.barsgl.ejb.repository.BackvalueJournalRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.ejbcore.DataAccessCallback;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;

import javax.ejb.AccessTimeout;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.inject.Inject;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MINUTES;
import static ru.rbt.barsgl.ejb.entity.gl.BackvalueJournal.BackvalueJournalState.*;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Task;

/**
 * Created by Ivan Sevastyanov
 * доступ к пересчету/локализации должен быть синхронизирован
 */
@Singleton
@AccessTimeout(value = 15, unit = MINUTES)
public class BackvalueJournalController {

    public static final Logger logger = Logger.getLogger(BackvalueJournalController.class.getName());

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @Inject
    private ru.rbt.ejbcore.util.DateUtils dateUtils;

    @EJB
    private BackvalueJournalRepository journalRepository;

    @EJB
    private AuditController auditController;

    @EJB
    private OperdayController operdayController;

    /**
     * полный пересчет/локализация и пересчет остатков по БС2
     * @throws Exception
     */
    public void recalculateBackvalueJournal() throws Exception {
        if (0 < journalRepository.executeInNewTransaction(persistence -> recalculateLocal())) {
            journalRepository.executeInNewTransaction(persistence -> recalculateBS2());
        }
    }

    /**
     * пересчет/локализация
     * @return кол-во пересчитанных счетов
     */
    public int recalculateLocal() throws Exception {
        return processRecalcException(connection -> {
            final int[] count = {0};
            List<BackvalueJournal> journal = journalRepository.select(BackvalueJournal.class,
                    "  from BackvalueJournal j " +
                            " where j.state = ?1 " +
                            " order by j.id.postingDate desc, j.id.bsaAcid, j.id.acid", BackvalueJournalState.NEW);
            if (!journal.isEmpty()) {
                auditController.info(Task, format("Начало пересчета/локализации. Записей в журнале BACKVALUE: '%s'", journal.size()));
            } else {
                auditController.info(Task, format("Пересчет/локализация не требуется. Записей в журнале BACKVALUE: '%s'", journal.size()));
                return 0;
            }
            // пересчет производится по таблице GL_LOCACC
            journalRepository.executeInNewTransaction(persistence -> {
                logger.info(format("deleted from GL_LOCACC: %s", journalRepository.executeNativeUpdate("delete from GL_LOCACC")));
                journal.stream().forEach(rec ->
                        journalRepository.executeNativeUpdate("insert into GL_LOCACC (bsaacid,acid,pod) values (?,?,?)"
                            , rec.getId().getBsaAcid(), rec.getId().getAcid(), rec.getId().getPostingDate()));
                count[0]++;
                return null;
            });
            try {
                journalRepository.executeNativeUpdate("call GL_CORRLOCAL");
            } catch (Exception e) {
                throw new DefaultApplicationException("Ошибка при пересчете/локализации", e);
            }
            setBackvalueJournalState(LOCAL, NEW);
            auditController.info(Task, "Окончание пересчета/локализации");
            return count[0];
        }, ERROR_LC, NEW, "пересчет/локализация");
    }

    public int recalculateLocalIncrementBuffer() throws Exception {
        auditController.info(Task, format("Начало пересчета/локализации по остаткам backvalue buffer"));
        // пересчет производится по таблице GL_LOCACC
        int accs = journalRepository.executeInNewTransaction(persistence -> {
            logger.info(format("deleted from GL_LOCACC: %s", journalRepository.executeNativeUpdate("delete from GL_LOCACC")));
            return journalRepository.executeTransactionally(connection -> {
                int count[] = {0};
                try (PreparedStatement statement = connection.prepareStatement("select bsaacid, acid, dat from gl_baltur where moved = 'Y'");
                     ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        journalRepository.executeNativeUpdate("insert into GL_LOCACC (bsaacid,acid,pod) values (?,?,?)"
                                , rs.getString(1), rs.getString(2), rs.getDate(3));
                        count[0]++;
                    }
                }
                return count[0];
            });
        });
        auditController.info(Task, format("Записей для локализации backvalue %s", accs));
        try {
            journalRepository.executeInNewTransaction(pers -> {
                journalRepository.executeNativeUpdate("call GL_CORRLOCAL");
                return null;
            });
        } catch (Exception e) {
            auditController.error(Task, "Ошибка при пересчете/локализации", null, e);
        }
        return accs;
    }

    /**
     * пересчет остатков по БС2
     * @return кол-во пересчитаных счетов
     */
    public int recalculateBS2() throws Exception {
        return processRecalcException(connection -> {
            int count[] = {0};
            List<DataRecord> groups = journalRepository.select("select distinct pod from GL_BVJRNL where state = ? order by 1"
                    , LOCAL.name());
            if (!groups.isEmpty()) {
                auditController.info(Task, format("Начало пересчета остатков по БС2. Кол-во дат журнале BACKVALUE: '%s'", groups.size()));
            }
            groups.stream().forEach(record -> {
                try {
                    journalRepository.executeNativeUpdate("call GL_RECALC_BS2(?)", record.getDate("POD"));
                    count[0]++;
                } catch (Exception e) {
                    throw new DefaultApplicationException(
                            format("Ошибка при пересчете остатков по БС за дату: '%s'", dateUtils.onlyDateString(record.getSqlDate("POD"))), e);
                }
            });
            setBackvalueJournalState(PROCESSED, LOCAL);
            auditController.info(Task, "Окончание пересчета остатков по БС2");
            return count[0];
        }, ERROR_BL, LOCAL, "пересчет остатков БС2");

    }

    private int setBackvalueJournalState(BackvalueJournalState targetState, BackvalueJournalState from) {
        return journalRepository.executeUpdate("update BackvalueJournal j set j.state = ?1 where j.state = ?2"
                , targetState, from);
    }

    private int processRecalcException(DataAccessCallback<Integer> callback, BackvalueJournalState errorState, BackvalueJournalState fromState, String actionName) throws Exception {
        try {
            return journalRepository.executeTransactionally(callback);
        } catch (Exception e) {
            auditController.error(Task, format("Ошибка при %s. " +
                    "Записи не прошедшие пересчет/локализацию в таблице GL_BVJRNL.STATE = '%s'", actionName, errorState.name()), null, e);
            journalRepository.executeInNewTransaction(persistence ->
                    setBackvalueJournalState(errorState, fromState));
            throw e;
        }
    }

}
