package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import ru.rbt.barsgl.ejb.entity.acc.AcDNJournal;
import ru.rbt.barsgl.ejb.repository.AcDNJournalRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.ExceptionUtils;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.PersistenceException;

import java.sql.DataTruncation;
import java.sql.SQLException;

import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.*;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.ERROR;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.PROCESSED;
import static ru.rbt.ejbcore.util.StringUtils.substr;

/**
 * Created by er18837 on 03.05.2018.
 */
@Stateless
@LocalBean
public class AccountDetailsNotifyController  extends CommonQueueController  {

    @Inject
    private AccountDetailsNotifyProcessor messageProcessor;

    @Inject
    AcDNJournalRepository journalRepository;

    @Override
    protected void afterConnect() throws Exception {

    }

    @Override
    protected QueueProcessResult processQuery(String queueType, String textMessage, Long jId) throws Exception {
        messageProcessor.process(Sources.valueOf(queueType), textMessage, jId);
        return null;
    }

    @Override
    protected String getJournalName() {
        return AccountDetailsNotifyProcessor.journalName;
    }

    @Override
    protected Long createJournalEntryInternal(String queueType, String textMessage) throws Exception {
        return journalRepository.createJournalEntry(Sources.valueOf(queueType), textMessage);
    }

    @Override
    protected void updateStatusSuccessOut(Long journalId, String comment, QueueProcessResult result) throws Exception {
        if (!result.isError())
            journalRepository.updateLogStatus(journalId, PROCESSED);
        else
            journalRepository.updateLogStatus(journalId, ERROR);
    }

    @Override
    protected void updateStatusErrorProc(Long journalId, Throwable e) throws Exception {
        journalRepository.updateLogStatus(journalId, ERROR, getErrorMessage(e));
    }

    @Override
    protected void updateStatusErrorOut(Long journalId, Throwable e) throws Exception {
        journalRepository.updateLogStatus(journalId, ERROR, getErrorMessage(e));
    }

    private String getErrorMessage(Throwable t) {
        String msg = ExceptionUtils.getErrorMessage(t,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, ArithmeticException.class,
                IllegalArgumentException.class, PersistenceException.class, DefaultApplicationException.class);
        return substr(msg, 255);
    }
}
