package ru.rbt.barsgl.ejb.etc;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by ER19391 on 25.08.2017.
 */
public class SshProcedureRunner {

    private static Logger log = Logger.getLogger(SshProcedureRunner.class);

    public void executeSshCommand(String host, String user, String pswd, int port, String command, OutputStream out) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host, port);
        if(pswd != null) {
            UserInfo ui = new UserInfo() {

                public String getPassphrase() {
                    return null;
                }

                public String getPassword() {
                    return pswd;
                }

                public boolean promptPassphrase(String arg0) {
                    return true;
                }

                public boolean promptPassword(String arg0) {
                    return true;
                }

                public boolean promptYesNo(String arg0) {
                    return true;
                }

                public void showMessage(String arg0) {
                }

            };
            session.setUserInfo(ui);
        }
        session.connect();

        ChannelExec channel = (ChannelExec)session.openChannel("exec");
        channel.setCommand(command);

        channel.setInputStream(null);

        ((ChannelExec) channel).setErrStream(System.err);

        InputStream in = channel.getInputStream();

        channel.connect();

        if(out != null) {
            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0)
                        break;
                    out.write(new String(tmp, 0, i).getBytes());
                }
                if (channel.isClosed()) {
                    if (in.available() > 0)
                        continue;
                    out.write(new String("exit-status: " + channel.getExitStatus()).getBytes());
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            }
        }
        channel.disconnect();
        session.disconnect();
    }

}
