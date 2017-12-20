package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.entity.cust.CustDNJournal;
import ru.rbt.barsgl.ejb.repository.customer.CustDNJournalRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.ExceptionUtils;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.sql.DataTruncation;
import java.sql.SQLException;

import static ru.rbt.ejbcore.util.StringUtils.substr;

/**
 * Created by er18837 on 13.12.2017.
 */
@Stateless
@LocalBean
public class CustomerNotifyQueueController extends CommonQueueController {
    private static final Logger log = Logger.getLogger(CustomerNotifyQueueController.class);

    @Inject
    private CustDNJournalRepository journalRepository;

    @Inject
    private CustomerNotifyProcessor messageProcessor;

    @Override
    protected void afterConnect() throws Exception {
    }

    @Override
    protected String processQuery(String queueType, String textMessage, Long jId) throws Exception {
        messageProcessor.process(textMessage, jId);
        return null;
    }

    @Override
    protected String getJournalName() {
        return "GL_CUDENO1";
    }

    @Override
    protected Long createJournalEntry(String queueType, String textMessage) throws Exception {
        return journalRepository.createJournalEntry(textMessage);
    }

    @Override
    protected void updateStatusSuccess(Long journalId, String comment, String outMessage) throws Exception {
        journalRepository.updateLogStatus(journalId, CustDNJournal.Status.PROCESSED, comment);
    }

    @Override
    protected void updateStatusErrorProc(Long journalId, Throwable e) throws Exception {
        journalRepository.updateLogStatus(journalId, CustDNJournal.Status.ERR_PROC, getErrorMessage(e));
    }

    @Override
    protected void updateStatusErrorOut(Long journalId, Throwable e) throws Exception {
        journalRepository.updateLogStatus(journalId, CustDNJournal.Status.ERR_PROC, getErrorMessage(e));
    }

    private String getErrorMessage(Throwable t) {
        String msg = ExceptionUtils.getErrorMessage(t,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, ArithmeticException.class,
                IllegalArgumentException.class, PersistenceException.class, DefaultApplicationException.class);
        return substr(msg, 255);
    }


}
