package ru.rbt.barsgl.shared.account;

import com.google.gwt.user.client.rpc.IsSerializable;
import ru.rbt.barsgl.shared.ErrorList;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Created by er23851 on 24.07.2017.
 */
public class CheckAccountWrapper implements Serializable, IsSerializable {

    public static final String dateFormat = "dd.MM.yyyy";
    public static final String dateNull = "01.01.2029";

    private Long id;
    private String bsaAcid;
    private String dateOperStr;
    private BigDecimal amount;

    final private ErrorList errorList = new ErrorList();

    public static String getDateFormat() {
        return dateFormat;
    }

    public static String getDateNull() {
        return dateNull;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBsaAcid() {
        return bsaAcid;
    }

    public void setBsaAcid(String bsaAcid) {
        this.bsaAcid = bsaAcid;
    }

    public String getDateOperStr() {
        return dateOperStr;
    }

    public void setDateOperStr(String dateOperStr) {
        this.dateOperStr = dateOperStr;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public ErrorList getErrorList() {
        return errorList;
    }

    public String getErrorMessage() {
        return errorList.getErrorMessage();
    }
}
