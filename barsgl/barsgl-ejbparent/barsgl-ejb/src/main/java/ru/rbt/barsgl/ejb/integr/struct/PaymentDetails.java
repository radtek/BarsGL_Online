/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2017
 */
package ru.rbt.barsgl.ejb.integr.struct;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 *
 * @author Andrew Samsonov
 */
public class PaymentDetails implements Serializable{

    private String currency; //P14/P014 Валюта операции
    private boolean controlable;//P15/P015 Признак контролируемости (Y or N)
    private String narrativeCustomers; //P16/P016 Наименование плательщика 
    private String taxCustomers;//P17/P017 ИНН плательщика

    private String account;       //AC_?R;    //	P2	Счет ЦБ по ? операции
    private BigDecimal operAmount;  //AMT_?R;  //  P3	сумма по ? операции    
    
    public PaymentDetails(String currency, boolean controlable, String narrativeCustomers, String taxCustomers, String account, BigDecimal operAmount) {
        this.currency = currency;
        this.controlable = controlable;
        this.narrativeCustomers = narrativeCustomers;
        this.taxCustomers = taxCustomers;
        this.account = account;
        this.operAmount = operAmount;
    }
    
    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getNarrativeCustomers() {
        return narrativeCustomers;
    }

    public void setNarrativeCustomers(String narrativeCustomers) {
        this.narrativeCustomers = narrativeCustomers;
    }

    public String getTaxCustomers() {
        return taxCustomers;
    }

    public void setTaxCustomers(String taxCustomers) {
        this.taxCustomers = taxCustomers;
    }

    public boolean isControlable() {
        return controlable;
    }

    public void setControlable(boolean controlable) {
        this.controlable = controlable;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public BigDecimal getOperAmount() {
        return operAmount;
    }

    public void setOperAmount(BigDecimal operAmount) {
        this.operAmount = operAmount;
    }
    
}
