package ru.rbt.barsgl.shared.access;

import ru.rbt.barsgl.shared.DatePartsContainer;

import java.util.Date;

/**
 * Created by akichigi on 11.04.16.
 */
public class PrmValueHistoryWrapper extends PrmValueWrapper {
    private Long prmId;
    private String userSys;
    private String dateSysStr; //Date
    private String chngType;

    public Long getPrmId() {
        return prmId;
    }

    public void setPrmId(Long prmId) {
        this.prmId = prmId;
    }

    public String getUserSys() {
        return userSys;
    }

    public void setUserSys(String userSys) {
        this.userSys = userSys;
    }

    public String getChngType() {
        return chngType;
    }

    public void setChngType(String chngType) {
        this.chngType = chngType;
    }

    public String getDateSysStr() {
        return dateSysStr;
    }

    public void setDateSysStr(String dateSysStr) {
        this.dateSysStr = dateSysStr;
    }
}
