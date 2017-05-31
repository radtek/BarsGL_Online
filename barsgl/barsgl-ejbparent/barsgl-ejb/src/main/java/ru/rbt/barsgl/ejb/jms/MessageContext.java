/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2016
 */
package ru.rbt.barsgl.ejb.jms;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import javax.jms.*;
import java.io.Serializable;

/**
 *
 * @author Andrew Samsonov
 */
public class MessageContext implements Serializable, AutoCloseable {

  private String host;
  private int port;
  private String queueManager;
  private String channel;

  private String user;
  private String password;

  private JMSContext jmsContext;

  public MessageContext(String host, int port, String queueManager, String channel, String user, String password) {
    this.host = host;
    this.port = port;
    this.queueManager = queueManager;
    this.channel = channel;
    this.user = user;
    this.password = password;
  }
      
  private QueueConnectionFactory getConnectionFactory() throws JMSException {
    MQQueueConnectionFactory cf = new MQQueueConnectionFactory();
    cf.setHostName(host);
    cf.setPort(port);
    cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
    cf.setQueueManager(queueManager);
    cf.setChannel(channel);
    return cf;
  }

  public boolean getTransacted() throws JMSException{
    return jmsContext.getTransacted();
  }
  
  public JMSContext createJMSContext() throws JMSException {
    if (jmsContext == null) {
        QueueConnectionFactory cf = getConnectionFactory();
        if(user != null && !user.isEmpty() && password != null)
            jmsContext = cf.createContext(user, password);
        else
            jmsContext = cf.createContext();
    }
    return jmsContext;
  }
  
  public JMSContext createJMSContext(boolean transacted, int acknowledgeMode) throws JMSException {
    if (jmsContext == null) {
        QueueConnectionFactory cf = getConnectionFactory();
        if(user != null && !user.isEmpty() && password != null)
            jmsContext = cf.createContext(user, password, acknowledgeMode);
        else
            jmsContext = cf.createContext(acknowledgeMode);
    }
    return jmsContext;
  }

  public Queue createQueue(String queueName) throws JMSException{
    return jmsContext.createQueue(queueName);
  }
  
  public JMSConsumer createConsumer(Queue queue) throws JMSException {
    JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
    return jmsConsumer;
  }

  public JMSProducer createProducer() throws JMSException{
    JMSProducer messageProducer = jmsContext.createProducer();
    return messageProducer;
  }
  
  public QueueBrowser createBrowser(Queue queue) throws JMSException{
    QueueBrowser browser = jmsContext.createBrowser(queue);
    return browser;
  }
  
  public BytesMessage createBytesMessage() throws JMSException{
    return jmsContext.createBytesMessage();
  }
  
  public TextMessage createTextMessage() throws JMSException{
    return jmsContext.createTextMessage();
  }

  public TextMessage createTextMessage(String message) throws JMSException {
    return jmsContext.createTextMessage(message);
  }

  public void setExceptionListener(ExceptionListener listener) throws JMSException{
    jmsContext.setExceptionListener(listener);
  }
  
  public void start() throws JMSException{
      jmsContext.start();
  }
  
  public void stop() throws JMSException{
    jmsContext.stop();
  }
  
  public void commit() throws JMSException{
    jmsContext.commit();
  }

  public void rollback() throws JMSException{
    jmsContext.rollback();    
  }
  
  @Override
  public void close() throws Exception {
    if (jmsContext != null) {
      jmsContext.close();
    }
  }
}
