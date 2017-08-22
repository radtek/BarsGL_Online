package ru.rbt.barsgl.shared.dict;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by er22317 on 17.07.2017.
 */
public class AccDealsWrapper  implements Serializable,IsSerializable {
    private String acc2;
    private String name;
    private boolean flagOff;
//    private String dtr;
//    private String usrr;
//    private String dtm;
//    private String usrm;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFlagOff() {
        return flagOff;
    }

    public void setFlagOff(boolean flagOff) {
        this.flagOff = flagOff;
    }
    public String getFlagOff() {
        return this.flagOff? "N": "Y";
    }

//    public String getDtr() {
//        return dtr;
//    }
//
//    public void setDtr(String dtr) {
//        this.dtr = dtr;
//    }
//
//    public String getUsrr() {
//        return usrr;
//    }
//
//    public void setUsrr(String usrr) {
//        this.usrr = usrr;
//    }
//
//    public String getDtm() {
//        return dtm;
//    }
//
//    public void setDtm(String dtm) {
//        this.dtm = dtm;
//    }
//
//    public String getUsrm() {
//        return usrm;
//    }
//
//    public void setUsrm(String usrm) {
//        this.usrm = usrm;
//    }

    public String getAcc2() {
        return acc2;
    }

    public void setAcc2(String acc2) {
        this.acc2 = acc2;
    }
}
