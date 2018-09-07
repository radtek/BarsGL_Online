package ru.rbt.barsgl.ejb.etc;

import ru.rb.cfg.CryptoUtil;

import java.io.OutputStream;

/**
 * Created by Ivan Sevastyanov on 07.09.2018.
 */
public class SshCommand {
    
    private String user;
    
    private String encryptedPassword;
    
    private String host;
    
    private int port;
    
    private String command;
    
    private OutputStream outputStream;

    public String getUser() {
        return user;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public String getDecryptedPassword() {
        return CryptoUtil.decrypt(encryptedPassword);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getCommand() {
        return command;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public String toString() {
        return "SshCommand{" +
                "user='" + user + '\'' +
                ", encryptedPassword='" + encryptedPassword + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", command='" + command + '\'' +
                ", outputStream=" + outputStream +
                '}';
    }
}
