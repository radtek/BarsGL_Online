/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2017
 */
package ru.rbt.barsgl.ejb.integr.struct;

import java.io.Serializable;

/**
 *
 * @author Andrew Samsonov
 */
public class PaymentDetails implements Serializable{

    private String currency; //P14/P014 Валюта операции
    private boolean controlable;//P15/P015 Признак контролируемости (Y or N)
    private String narrativeCustomers; //P16/P016 Наименование плательщика 
    private String taxCustomers;//P17/P017 ИНН плательщика

    public PaymentDetails() {
    }

    public PaymentDetails(String currency, boolean controlable, String narrativeCustomers, String taxCustomers) {
        this.currency = currency;
        this.controlable = controlable;
        this.narrativeCustomers = narrativeCustomers;
        this.taxCustomers = taxCustomers;
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
    
}
