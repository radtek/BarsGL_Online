package ru.rbt.barsgl.ejb.etc;

import ru.rbt.barsgl.shared.Builder;

import java.io.OutputStream;

/**
 * Created by Ivan Sevastyanov on 07.09.2018.
 */
public class SshCommandBuilder implements Builder<SshCommand> {
    
    private SshCommand cmd = new SshCommand();
    
    public static SshCommandBuilder create() {
        return new SshCommandBuilder();
    }
    
    @Override
    public SshCommand build() {
        return cmd;
    }

    public SshCommandBuilder withUser(String user) {
        cmd.setUser(user);
        return this;
    }

    public SshCommandBuilder withEncryptedPassword(String encryptedPassword) {
        cmd.setEncryptedPassword(encryptedPassword);
        return this;
    }

    public SshCommandBuilder withHost(String host) {
        cmd.setHost(host);
        return this;
    }

    public SshCommandBuilder withPort(int port) {
        cmd.setPort(port);
        return this;
    }

    public SshCommandBuilder withCommand(String command) {
        cmd.setCommand(command);
        return this;
    }

    public SshCommandBuilder withOutputStream(OutputStream outputStream) {
        cmd.setOutputStream(outputStream);
        return this;
    }

}
