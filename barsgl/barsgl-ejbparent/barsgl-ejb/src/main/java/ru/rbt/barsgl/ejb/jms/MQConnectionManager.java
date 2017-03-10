/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2016
 */
package ru.rbt.barsgl.ejb.jms;

import javax.ejb.Singleton;
import javax.jms.*;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Andrew Samsonov
 */
@Singleton
public class MQConnectionManager {

  private final static Logger logger = Logger.getLogger("ru.rbt.barsgl.ejb.jms.MQConnectionManager");

  private MessageContext messageContext;
  private MessageConsumer messageConsumer;

  public void start(Properties properties, MessageListener messageListener, ExceptionListener listener) throws Exception {
    String mqHost = Optional.ofNullable(properties.getProperty(MQPropertiesConst.MQ_HOST)).orElse("###");
    String mqPortStr = Optional.ofNullable(properties.getProperty(MQPropertiesConst.MQ_PORT)).orElse("###");
    String mqQueueManager = Optional.ofNullable(properties.getProperty(MQPropertiesConst.MQ_QUEUE_MANAGER)).orElse("###");
    String mqChannel = Optional.ofNullable(properties.getProperty(MQPropertiesConst.MQ_CHANNEL)).orElse("###");
    String mqQueue = Optional.ofNullable(properties.getProperty(MQPropertiesConst.MQ_QUEUE_INC)).orElse("###");
    String mqUser = Optional.ofNullable(properties.getProperty("mq.user")).orElse("###");
    String mqPassword = Optional.ofNullable(properties.getProperty("mq.password")).orElse("###");

    String queuePropertiesStr = "QueueProperties{"
            + "mqHost='" + mqHost + '\''
            + ", mqPortStr='" + mqPortStr + '\''
            // + ", mqQueueManager='" + mqQueueManager + '\'' +
            + ", mqChannel='" + mqChannel + '\''
            + ", mqQueueInc='" + mqQueue + '\''
            //+ ", mqQueueOut='" + mqQueueOut + '\'' +
            + '}';

    if (queuePropertiesStr.contains("###")) {
      logger.log(Level.SEVERE, "Ошибка в параметрах подключения к серверу очередей (без user и password). {0}", queuePropertiesStr);
      return;
    }

    if (messageContext == null) {
      messageContext = new MessageContext(mqHost, Integer.valueOf(mqPortStr), mqQueueManager, mqChannel, mqUser, mqPassword);
      messageContext.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
      if (listener != null) {
        messageContext.setExceptionListener(listener);
      }
      Queue queue = messageContext.createQueue("queue:///" + mqQueue);
      messageConsumer = messageContext.createConsumer(queue);
      if (messageListener != null) {
        messageConsumer.setMessageListener(messageListener);
      }

      messageContext.start();
      logger.log(Level.INFO, "Start MQ");
    }
  }

  public void stop() {
    logger.log(Level.INFO, "Stop MQ");
    try {
      if (messageConsumer != null) {
        messageConsumer.close();
      }
    } catch (JMSException ex) {
      logger.log(Level.SEVERE, null, ex);
    }
    messageConsumer = null;
    try {
      if (messageContext != null) {
        messageContext.stop();
        messageContext.close();
      }
    } catch (Exception ex) {
      logger.log(Level.SEVERE, null, ex);
    }
    messageContext = null;
  }

}
