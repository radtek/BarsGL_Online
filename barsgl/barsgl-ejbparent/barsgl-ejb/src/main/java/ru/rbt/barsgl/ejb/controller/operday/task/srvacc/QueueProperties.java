package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.security.AuditController;

import javax.ejb.EJB;
import java.util.Optional;
import java.util.Properties;

import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.AccountQuery;

/**
 * Created by ER22228 on 12.07.2016.
 */
public class QueueProperties {
    private static final Logger log = Logger.getLogger(QueueProperties.class);

    @EJB
    private AuditController auditController;

    public String mqHost;
    public int mqPort;
    public String mqQueueManager;
    public String mqChannel;
    public int mqBatchSize;
    public String mqTopics;

    public String mqUser;
    public String mqPassword;
    public String unspents;

    @Override
    public String toString() {
        return "QueueProperties{" +
                   "auditController=" + auditController +
                   ", mqHost='" + mqHost + '\'' +
                   ", mqPort=" + mqPort +
//                   ", mqQueueManager='" + mqQueueManager + '\'' +
                   ", mqChannel='" + mqChannel + '\'' +
                   ", mqBatchSize=" + mqBatchSize +
                   ", mqTopics='" + mqTopics + '\'' +
                   ", unspents='" + unspents + '\'' +
                   '}';
    }

    private QueueProperties() {
    }

    public QueueProperties(Properties properties) throws Exception {
        this.mqHost = Optional.ofNullable(properties.getProperty("mq.host")).orElse("###");
        this.mqPort = Integer.parseInt(Optional.ofNullable(properties.getProperty("mq.port")).orElse("1414"));
        this.mqQueueManager = Optional.ofNullable(properties.getProperty("mq.queueManager")).orElse("QM_MBROKER");
        this.mqChannel = Optional.ofNullable(properties.getProperty("mq.channel")).orElse("###");
        this.mqBatchSize = Integer.parseInt(Optional.ofNullable(properties.getProperty("mq.batchSize ")).orElse("50"));
        this.mqTopics = Optional.ofNullable(properties.getProperty("mq.topics")).orElse("###");
        this.unspents = Optional.ofNullable(properties.getProperty("unspents")).orElse("hide");
        this.mqUser = Optional.ofNullable(properties.getProperty("mq.user")).orElse("###");
        this.mqPassword = Optional.ofNullable(properties.getProperty("mq.password")).orElse("###");
        if ((this.toString()).contains("###")) {
            log.error("Ошибка в параметрах подключения к серверу очередей (без user и password). " + this.toString());
            auditController.error(AccountQuery, "Ошибка в параметрах подключения к серверу очередей (без user и password). " + this.toString(), null, "");
            throw new Exception("Ошибка в параметрах подключения к серверу очередей (без user и password). " + this.toString());
        }
    }
}
