package ru.rbt.barsgl.shared.monitoring;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.List;

/**
 * Created by akichigi on 06.12.16.
 */
public class MonitoringWrapper implements Serializable, IsSerializable {
    private BufferItem pd;
    private BufferItem baltur;
    private ReplItem replTotal;
    private List<ReplTableItem> replList;
    private List<OperTableItem> operList;

    public BufferItem getPd() {
        return pd;
    }

    public void setPd(BufferItem pd) {
        this.pd = pd;
    }

    public BufferItem getBaltur() {
        return baltur;
    }

    public void setBaltur(BufferItem baltur) {
        this.baltur = baltur;
    }

    public ReplItem getReplTotal() {
        return replTotal;
    }

    public void setReplTotal(ReplItem replTotal) {
        this.replTotal = replTotal;
    }

    public List<ReplTableItem> getReplList() {
        return replList;
    }

    public void setReplList(List<ReplTableItem> replList) {
        this.replList = replList;
    }

    public List<OperTableItem> getOperList() {
        return operList;
    }

    public void setOperList(List<OperTableItem> operList) {
        this.operList = operList;
    }
}
