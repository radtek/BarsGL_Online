package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.entity.acc.AclirqJournal;
import ru.rbt.barsgl.ejb.repository.AclirqJournalRepository;
import ru.rbt.barsgl.ejbcore.AccountQueryRepository;
import ru.rbt.ejbcore.JpaAccessCallback;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.persistence.EntityManager;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountQuery;
import static ru.rbt.barsgl.ejb.props.PropertyName.PD_CONCURENCY;

/**
 * Created by er18837 on 14.12.2017.
 */

@SuppressWarnings("ALL")
@Stateless
@LocalBean
public class AccountQueryQueueController extends CommonQueueController {
    private static final Logger log = Logger.getLogger(CommonQueueProcessor4.class);
    //private static final String SCHEDULED_TASK_NAME = "AccountQuery";

    @EJB
    private AccountQueryProcessor queryProcessor;

    @EJB
    private AccountQueryBAProcessor queryProcessorBA;

    @EJB
    private MasterAccountProcessor queryProcessorMAPB;

    @EJB
    private AccountQueryRepository queryRepository;

    @EJB
    private AclirqJournalRepository journalRepository;

    private static final Map<String, String> CURRENCY_MAP = new HashMap<>();
    private static final Map<String, Integer> CURRENCY_NBDP_MAP = new HashMap<>();

    private final int defaultBatchSize = 50;

    @Override
    protected int getConcurencySize() {
        return propertiesRepository.getNumberDef(PD_CONCURENCY.getName(), 10L).intValue();
    }

    @Override
    protected void afterConnect() throws Exception {
        loadCurrency();
    }

    @Override
    protected String processQuery(String queueType, String textMessage, Long jId) throws Exception {
        try {
            switch (queueType) {
                case "LIRQ":
                    return queryProcessor.process(textMessage, CURRENCY_MAP, CURRENCY_NBDP_MAP, jId, "show".equals(queueProperties.unspents));
                case "BALIRQ":
                    return queryProcessorBA.process(textMessage, CURRENCY_MAP, CURRENCY_NBDP_MAP, jId);
                case "MAPBRQ":
                    return queryProcessorMAPB.process(textMessage, CURRENCY_MAP, CURRENCY_NBDP_MAP, jId);
            }
        } catch (Exception e) {
            log.error("Ошибка при подготовке ответа. ", e);
            auditController.warning(AccountQuery, "Ошибка при подготовке ответа / Таблица GL_ACLIRQ / id=" + jId, null, e);
            return journalRepository.executeInNewTransaction(persistence2 -> {
                AclirqJournal aclirqJournal = journalRepository.findById(AclirqJournal.class, jId);
                return getErrorMessage(aclirqJournal.getComment());
            });
        }
        return "";
    }

    private void loadCurrency() throws Exception {
        if (CURRENCY_MAP.isEmpty() || CURRENCY_NBDP_MAP.isEmpty()) {
            queryRepository.loadCurrency(CURRENCY_MAP, CURRENCY_NBDP_MAP);
        }
    }

    @Override
    protected String getJournalName() {
        return "GL_ACLIRQ";
    }

    @Override
    protected Long createJournalEntry(String queueType, String textMessage) throws Exception {
        return journalRepository.createJournalEntry(queueType, textMessage);
    }

    @Override
    protected void updateStatusSuccess(Long journalId, String comment, String outMessage) throws Exception {
        journalRepository.updateLogStatus(journalId, AclirqJournal.Status.PROCESSED, comment, outMessage);
    }

    @Override
    protected void updateStatusErrorProc(Long journalId, Throwable e) throws Exception {
        journalRepository.updateLogStatus(journalId, AclirqJournal.Status.ERROR, "Ошибка при обработке сообщения. " + e.getMessage());
    }

    @Override
    protected void updateStatusErrorOut(Long journalId, Throwable e) throws Exception {
        journalRepository.updateLogStatus(journalId, AclirqJournal.Status.ERROR, "Ошибка отправки ответа. " + e.getMessage());
    }

    public String getErrorMessage(String message) throws DatatypeConfigurationException {
        String answerBody;
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        DatatypeFactory df = DatatypeFactory.newInstance();
        XMLGregorianCalendar dateTime = df.newXMLGregorianCalendar(calendar);

        answerBody =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "\t\t<asbo:Error xmlns:asbo=\"urn:asbo:barsgl\">\n" +
                        "\t\t\t<asbo:Code>0100</asbo:Code>\n" +
                        "\t\t\t<asbo:Description>" + message + "</asbo:Description>\n" +
                        "\t\t\t<asbo:Source>BarsGL</asbo:Source>\n" +
                        "\t\t\t<asbo:Kind>SYSERR</asbo:Kind>\n" +
                        "\t\t\t<asbo:DateTime>" + dateTime.toString() + "</asbo:DateTime>\n" +
                        "\t\t</asbo:Error>\n";
        return answerBody;
    }

}
