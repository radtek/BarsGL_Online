package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.entity.acc.AclirqJournal;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * Created by er18837 on 13.12.2017.
 */
@Stateless
@LocalBean
public class CustomerNotifyQueueController extends CommonQueueController {
    private static final Logger log = Logger.getLogger(CustomerNotifyQueueController.class);


    @Override
    protected void afterConnect() throws Exception {
    }

    @Override
    protected String processQuery(String queueType, String textMessage, Long jId) throws Exception {
        return null;
    }

    @Override
    protected String getJournalName() {
        return "GL_CUDENO";
    }

    @Override
    protected Long createJournalEntry(String queueType, String textMessage) throws Exception {
        return null;
    }

    @Override
    protected AclirqJournal updateStatusSuccess(Long journalId, String comment, String outMessage) throws Exception {
        return null;
    }

    @Override
    protected AclirqJournal updateStatusErrorProc(Long journalId, Exception e) throws Exception {
        return null;
    }

    @Override
    protected AclirqJournal updateStatusErrorOut(Long journalId, Exception e) throws Exception {
        return null;
    }
}
