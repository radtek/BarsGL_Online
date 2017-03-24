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

  private QueueConnection connection;
  private QueueSession session;

  public MessageContext(String host, int port, String queueManager, String channel, String user, String password) {
    this.host = host;
    this.port = port;
    this.queueManager = queueManager;
    this.channel = channel;
    this.user = user;
    this.password = password;
  }
      
  private QueueConnection getConnection() throws JMSException {
    if (connection == null) {
      QueueConnectionFactory cf = getConnectionFactory();
      if(user != null && !user.isEmpty() && password != null)
        connection = cf.createQueueConnection(user, password);
      else
        connection = cf.createQueueConnection();
    }
    return connection;
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
    return session.getTransacted();
  }

  public QueueSession createQueueSession(boolean transacted, int acknowledgeMode) throws JMSException {
    if (session == null) {
      session = getConnection().createQueueSession(transacted, acknowledgeMode);//Session.AUTO_ACKNOWLEDGE);    
    }
    return session;
  }

  public Queue createQueue(String queueName) throws JMSException{
    return session.createQueue(queueName);
  }
  
  public MessageProducer createProducer(Queue queue) throws JMSException{
    MessageProducer messageProducer = session.createProducer(queue);
    return messageProducer;
  }

  private QueueReceiver createReceiver(Queue queue) throws JMSException {
    QueueReceiver receiver = session.createReceiver(queue);
    return receiver;
  }

  public MessageConsumer createConsumer(Queue queue) throws JMSException {
    MessageConsumer messageConsumer = session.createConsumer(queue);
    return messageConsumer;
  }

  public QueueBrowser createBrowser(Queue queue) throws JMSException{
    QueueBrowser browser = session.createBrowser(queue);
    return browser;
  }
  
  public BytesMessage createBytesMessage() throws JMSException{
    return session.createBytesMessage();
  }
  
  public TextMessage createTextMessage() throws JMSException{
    return session.createTextMessage();
  }

  public TextMessage createTextMessage(String message) throws JMSException {
    return session.createTextMessage(message);
  }

  public void setExceptionListener(ExceptionListener listener) throws JMSException{
    connection.setExceptionListener(listener);
  }
  
  public void start() throws JMSException{
    getConnection().start();
  }
  
  public void stop() throws JMSException{
    getConnection().stop();
  }
  
  public void commit() throws JMSException{
    session.commit();
  }

  public void rollback() throws JMSException{
    session.rollback();    
  }
  
  @Override
  public void close() throws Exception {
    if (session != null) {
      session.close();
    }
    if (connection != null) {
      connection.close();
    }
  }

}
