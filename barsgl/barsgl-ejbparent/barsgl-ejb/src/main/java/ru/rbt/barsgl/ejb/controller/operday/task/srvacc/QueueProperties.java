package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import org.apache.log4j.Logger;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ValidationError;

import javax.ejb.EJB;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Properties;

import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountQuery;
import static ru.rbt.ejbcore.util.StringUtils.ifEmpty;
import static ru.rbt.ejbcore.validation.ErrorCode.QUEUE_ERROR;
import static ru.rbt.ejbcore.validation.ErrorCode.STAMT_UNLOAD_DELETED;

/**
 * Created by ER22228 on 12.07.2016.
 */
public class QueueProperties implements Serializable{
    private static final Logger log = Logger.getLogger(QueueProperties.class);
    private static final String noParam = "###";

    public String mqHost;
    public int mqPort;
    public String mqQueueManager;
    public String mqChannel;
    public int mqBatchSize;
    public String mqTopics;

    public String mqUser;
    public String mqPassword;
    public String unspents;
    public String writeOut;
    public String writeSleepThreadTime;
    public String remoteQueueOut;

    @Override
    public String toString() {
/*
        return "QueueProperties{" +
                   ", mqHost='" + mqHost + '\'' +
                   ", mqPort=" + mqPort +
                   ", mqQueueManager='" + mqQueueManager + '\'' +
                   ", mqChannel='" + mqChannel + '\'' +
                   ", mqBatchSize=" + mqBatchSize +
                   ", mqTopics='" + mqTopics + '\'' +
                   ", unspents='" + unspents + '\'' +
                   ", writeOut='" + writeOut + '\'' +
                   ", writeSleepThreadTime='" + writeSleepThreadTime + '\'' +
                   ", remoteQueueOut='" + remoteQueueOut + '\'' +
                   '}';
*/

        return  "mq.type = queue\n"
                + "mq.host = " + mqHost + "\n"
                + "mq.port = " + mqPort + "\n"
                + "mq.queueManager = " + mqQueueManager + "\n"
                + "mq.channel = " + mqChannel + "\n"
                + "mq.batchSize = " + mqBatchSize + "\n"
                + "mq.topics = " + mqTopics + "\n"
                + "unspents=" + unspents + "\n"
                + "writeOut=" + writeOut +"\n"
                + "writeSleepThreadTime=" + writeSleepThreadTime + "\n"
                + "remoteQueueOut=" + remoteQueueOut +"\n"

                ;
    }

    private QueueProperties() {
    }

    public QueueProperties(Properties properties) throws Exception {
        try {
            this.mqHost = ifEmpty(properties.getProperty("mq.host", noParam).trim(), noParam);
            this.mqPort = Integer.parseInt(properties.getProperty("mq.port","1414").trim());
            this.mqQueueManager = ifEmpty(properties.getProperty("mq.queueManager", "QM_MBROKER").trim(), noParam);
            this.mqChannel = ifEmpty(properties.getProperty("mq.channel", noParam).trim(), noParam);;
            this.mqBatchSize = Integer.parseInt(properties.getProperty("mq.batchSize","50").trim());
            this.mqTopics = ifEmpty(properties.getProperty("mq.topics", noParam).trim(), noParam);;
            this.unspents = properties.getProperty("unspents", "hide").trim();
            this.writeOut = properties.getProperty("writeOut", "false").trim();
            this.remoteQueueOut = properties.getProperty("remoteQueueOut", "false").trim();
            this.writeSleepThreadTime = properties.getProperty("writeSleepThreadTime", "false").trim();
            this.mqUser = ifEmpty(properties.getProperty("mq.user", noParam).trim(), noParam);;
            this.mqPassword = ifEmpty(properties.getProperty("mq.password", noParam).trim(), noParam);
            if ((this.toString()).contains(noParam)) {
                logError();
            }
        } catch (NumberFormatException ex) {
            logError();
        }
    }

    public QueueProperties(String mqHost, int mqPort, String mqQueueManager, String mqChannel, int mqBatchSize, String mqTopics,
                           String mqUser, String mqPassword, String unspents, boolean writeOut, boolean writeSleepThreadTime, boolean remoteQueueOut) {
        this.mqHost = mqHost;
        this.mqPort = mqPort;
        this.mqQueueManager = mqQueueManager;
        this.mqChannel = mqChannel;
        this.mqBatchSize = mqBatchSize;
        this.mqTopics = mqTopics;
        this.mqUser = mqUser;
        this.mqPassword = mqPassword;
        this.unspents = unspents;
        this.writeOut = Boolean.toString(writeOut);
        this.writeSleepThreadTime = Boolean.toString(writeSleepThreadTime);
        this.remoteQueueOut = Boolean.toString(remoteQueueOut);
    }

    private void logError() {
        String errorMsg = "Ошибка в параметрах подключения к серверу очередей (без user и password). " + this.toString();
        log.error(errorMsg);
        throw new ValidationError(QUEUE_ERROR, errorMsg);
    }
}
