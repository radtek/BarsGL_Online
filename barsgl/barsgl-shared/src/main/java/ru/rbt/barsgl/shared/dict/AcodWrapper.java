package ru.rbt.barsgl.shared.dict;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by akichigi on 21.10.16.
 */
public class AcodWrapper implements Serializable,IsSerializable {
    private Long id;
    private String acod;
    private String acc2dscr;
    private String type;
    private String sqdscr;
    private String ename;
    private String rname;

    public String getAcod() {
        return acod;
    }

    public void setAcod(String acod) {
        this.acod = acod;
    }

    public String getAcc2dscr() {
        return acc2dscr;
    }

    public void setAcc2dscr(String acc2dscr) {
        this.acc2dscr = acc2dscr;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSqdscr() {
        return sqdscr;
    }

    public void setSqdscr(String sqdscr) {
        this.sqdscr = sqdscr;
    }

    public String getEname() {
        return ename;
    }

    public void setEname(String ename) {
        this.ename = ename;
    }

    public String getRname() {
        return rname;
    }

    public void setRname(String rname) {
        this.rname = rname;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
