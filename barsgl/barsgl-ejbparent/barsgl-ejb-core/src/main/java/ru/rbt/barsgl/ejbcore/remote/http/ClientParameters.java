package ru.rbt.barsgl.ejbcore.remote.http;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * Created by Ivan Sevastyanov
 */
public final class ClientParameters implements Serializable {

    private String _host_name;
    private String _host_ip;
    private String _os_name;
    private String _os_username;

    public ClientParameters() {
    }

    protected ClientParameters(String hostName, String hostIp, String osName, String osUsername) {
        this._host_name = hostName;
        this._host_ip = hostIp;
        this._os_name = osName;
        this._os_username = osUsername;
    }

    public String getHostName() {
        return _host_name;
    }

    public String getHostIp() {
        return _host_ip;
    }

    public String getOsName() {
        return _os_name;
    }

    public String getOsUsername() {
        return _os_username;
    }

    public static ClientParameters getLocalParameters() {
        String hostName = null;
        String ip = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
            InetAddress addrs[] = InetAddress.getAllByName(hostName);
            for (InetAddress addr : addrs) {
                if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress()) {
                    ip = addr.getHostAddress();
                    break;
                }
            }
        } catch (Throwable ignore) {}
        return new ClientParameters(
                hostName,
                ip,
                System.getProperty("os.name"),
                System.getProperty("user.name")
        );
    }

}
