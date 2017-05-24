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
  private MessageContext messageContextOut;
  //private MessageConsumer messageConsumer;
  private JMSConsumer messageConsumer;

  public void start(Properties properties, MessageListener messageListener, ExceptionListener exceptionListener) throws Exception {
    String mqHost = Optional.ofNullable(properties.getProperty(MQPropertiesConst.MQ_HOST)).orElse("###");
    String mqPortStr = Optional.ofNullable(properties.getProperty(MQPropertiesConst.MQ_PORT)).orElse("###");
    String mqQueueManager = Optional.ofNullable(properties.getProperty(MQPropertiesConst.MQ_QUEUE_MANAGER)).orElse("###");
    String mqChannel = Optional.ofNullable(properties.getProperty(MQPropertiesConst.MQ_CHANNEL)).orElse("###");
    String mqQueue = Optional.ofNullable(properties.getProperty(MQPropertiesConst.MQ_QUEUE_INC)).orElse("###");
    String mqUser = Optional.ofNullable(properties.getProperty("mq.user")).orElse("###");
    String mqPassword = Optional.ofNullable(properties.getProperty("mq.password")).orElse("###");
    String mqTopics = Optional.ofNullable(properties.getProperty(MQPropertiesConst.MQ_TOPICS)).orElse(null);

    if(mqTopics != null){
        String[] params = mqTopics.split(":");
        mqQueue = params[1];      
    }
//    if (queuePropertiesStr.contains("###")) {
//      logger.log(Level.SEVERE, "Ошибка в параметрах подключения к серверу очередей (без user и password). {0}", queuePropertiesStr);
//      return;
//    }

    if (messageContext == null) {
      messageContext = new MessageContext(mqHost, Integer.valueOf(mqPortStr), mqQueueManager, mqChannel, mqUser, mqPassword);
      JMSContext jmsContext = messageContext.createJMSContext();
      if (exceptionListener != null) {
        messageContext.setExceptionListener(exceptionListener);
      }
      messageConsumer = messageContext.createConsumer(messageContext.createQueue("queue:///" + mqQueue));
      if(messageListener != null)
        setMessageListener(messageListener);
    }
    
    if(messageContextOut == null){
      messageContextOut = new MessageContext(mqHost, Integer.valueOf(mqPortStr), mqQueueManager, mqChannel, mqUser, mqPassword);      
    }
  }

  public void stop() {
    logger.log(Level.INFO, "Stop MQ");
    if (messageConsumer != null) {
      messageConsumer.close();
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
  
  public void setMessageListener(MessageListener messageListener) throws JMSException{
    if (messageListener != null) {
      messageConsumer.setMessageListener(messageListener);
      messageContext.start();    
      logger.log(Level.INFO, "Start MQ");
    }    
  }
  
  public JMSContext createOutgouingJMSContext() throws JMSException{
    return messageContextOut.createJMSContext();
  }
}
