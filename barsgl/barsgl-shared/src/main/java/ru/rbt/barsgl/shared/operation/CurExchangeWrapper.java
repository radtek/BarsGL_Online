package ru.rbt.barsgl.shared.operation;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Created by akichigi on 20.01.17.
 */
public class CurExchangeWrapper implements Serializable, IsSerializable {
    private String date;
    private BigDecimal sourceSum;
    private BigDecimal targetSum;
    private String sourceCurrency;
    private String targetCurrency;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public BigDecimal getSourceSum() {
        return sourceSum;
    }

    public void setSourceSum(BigDecimal sourceSum) {
        this.sourceSum = sourceSum;
    }

    public BigDecimal getTargetSum() {
        return targetSum;
    }

    public void setTargetSum(BigDecimal targetSum) {
        this.targetSum = targetSum;
    }

    public String getSourceCurrency() {
        return sourceCurrency;
    }

    public void setSourceCurrency(String sourceCurrency) {
        this.sourceCurrency = sourceCurrency;
    }

    public String getTargetCurrency() {
        return targetCurrency;
    }

    public void setTargetCurrency(String targetCurrency) {
        this.targetCurrency = targetCurrency;
    }
}
