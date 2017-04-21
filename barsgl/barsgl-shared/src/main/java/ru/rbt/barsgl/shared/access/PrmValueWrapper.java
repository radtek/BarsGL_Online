package ru.rbt.barsgl.shared.access;

import com.google.gwt.user.client.rpc.IsSerializable;
import ru.rbt.barsgl.shared.DatePartsContainer;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.shared.enums.PrmValueEnum;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by akichigi on 06.04.16.
 */
public class PrmValueWrapper implements Serializable,IsSerializable {

    private Long id;
    private Long userId;
    private PrmValueEnum prmCode;
    private String prmValue;
    private String dateBeginStr; //Date
    private String dateEndStr; //Date
    private String userAut;
    private String dateAutStr;  //Date
    private String operDayDateStr; //Date

    private FormAction action;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public PrmValueEnum getPrmCode() {
        return prmCode;
    }

    public void setPrmCode(PrmValueEnum prmCode) {
        this.prmCode = prmCode;
    }

    public String getPrmValue() {
        return prmValue;
    }

    public void setPrmValue(String prmValue) {
        this.prmValue = prmValue;
    }

    public FormAction getAction() {
        return action;
    }

    public void setAction(FormAction action) {
        this.action = action;
    }

    public String getUserAut() {
        return userAut;
    }

    public void setUserAut(String userAut) {
        this.userAut = userAut;
    }

    public String getDateBeginStr() {
        return dateBeginStr;
    }

    public void setDateBeginStr(String dateBeginStr) {
        this.dateBeginStr = dateBeginStr;
    }

    public String getDateEndStr() {
        return dateEndStr;
    }

    public void setDateEndStr(String dateEndStr) {
        this.dateEndStr = dateEndStr;
    }

    public String getDateAutStr() {
        return dateAutStr;
    }

    public void setDateAutStr(String dateAutStr) {
        this.dateAutStr = dateAutStr;
    }

    public String getOperDayDateStr() {
        return operDayDateStr;
    }

    public void setOperDayDateStr(String operDayDateStr) {
        this.operDayDateStr = operDayDateStr;
    }
}
