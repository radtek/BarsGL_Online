package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import org.apache.log4j.Logger;
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

import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Sources.KTP_CLOSE;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.ERROR;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.PROCESSED;
import static ru.rbt.ejbcore.util.StringUtils.substr;

/**
 * Created by er18837 on 16.03.2018.
 */
@Stateless
@LocalBean
public class AccDealCloseQueueController extends CommonQueueController {
    private static final Logger log = Logger.getLogger(AccDealCloseQueueController.class);

    @EJB
    private AcDNJournalRepository journalRepository;

    @Inject
    private AccDealCloseProcessor messageProcessor;

    @Override
    protected void afterConnect() throws Exception {
    }

    @Override
    protected int getConcurencySize() {
        return 10;
    }

    @Override
    protected String processQuery(String queueType, String textMessage, Long jId) throws Exception {
        return messageProcessor.process(textMessage, jId);
    }

    @Override
    protected String getJournalName() {
        return AccDealCloseProcessor.journalName;
    }

    @Override
    protected Long createJournalEntryInternal(String queueType, String textMessage) throws Exception {
        return journalRepository.createJournalEntry(AcDNJournal.Sources.KTP_CLOSE , textMessage);
    }

    @Override
    protected void updateStatusSuccess(Long journalId, String comment, String outMessage) throws Exception {
        journalRepository.updateLogStatus(journalId, PROCESSED);
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
