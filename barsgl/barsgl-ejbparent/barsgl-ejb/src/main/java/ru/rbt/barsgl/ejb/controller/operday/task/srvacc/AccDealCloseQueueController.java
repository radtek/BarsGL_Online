package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.repository.AcDNJournalRepository;

import javax.ejb.EJB;

import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Sources.KTP_CLOSE;

/**
 * Created by er18837 on 16.03.2018.
 */
public class AccDealCloseQueueController extends CommonQueueController {
    private static final Logger log = Logger.getLogger(AccDealCloseQueueController.class);

    @EJB
    private AcDNJournalRepository journalRepository;

    @Override
    protected void afterConnect() throws Exception {

    }

    @Override
    protected String processQuery(String queueType, String textMessage, Long jId) throws Exception {
        return null;
    }

    @Override
    protected String getJournalName() {
        return "GL_ACDENO";
    }

    @Override
    protected Long createJournalEntryInternal(String queueType, String textMessage) throws Exception {
        return journalRepository.createJournalEntry(KTP_CLOSE, textMessage);
    }

    @Override
    protected void updateStatusSuccess(Long journalId, String comment, String outMessage) throws Exception {

    }

    @Override
    protected void updateStatusErrorProc(Long journalId, Throwable e) throws Exception {

    }

    @Override
    protected void updateStatusErrorOut(Long journalId, Throwable e) throws Exception {

    }
}
