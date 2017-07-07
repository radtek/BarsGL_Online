package ru.rbt.barsgl.gwt.client.quickFilter;

import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.shared.filter.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;

import java.util.ArrayList;
import java.util.Date;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;

/**
 * Created by ER18837 on 24.05.16.
 */
public abstract class AccountQuickFilterParams implements IQuickFilterParams {
    private Column colFilial;
    private Column colCurrency;
    private Column colAccount;
    private Column colAcc2;
    private Column colCustomer;
    private Column colDealSource;
    private Column colDealId;
    private Column colDateOpen;
    private Column colDateClose;

    private String filial;
    private String currency;
    private String account;
    private String customerNumber;
    private String dealSource;
    private String dealId;
    private Date dateFrom;
    private Date dateTo;
    private Date dateClose;
    private String filialN;
    private String currencyN;

    public AccountQuickFilterParams(Column colFilial, Column colCurrency, Column colAccount, Column colAcc2,
                                    Column colCustomer, Column colDealSource, Column colDealId, Column colDateOpen, Column colDateClose) {
        this.colFilial = colFilial;
        this.colCurrency = colCurrency;
        this.colAccount = colAccount;
        this.colAcc2 = colAcc2;
        this.colCustomer = colCustomer;
        this.colDealSource = colDealSource;
        this.colDealId = colDealId;
        this.colDateOpen = colDateOpen;
        this.colDateClose = colDateClose;
    }

    @Override
    public void setInitialFilterParams(Object[] params) {
        if (null == params)
            return;
        this.currency = (String)params[0];
        this.currencyN = (String)params[1];
        this.filial = (String)params[2];
        this.filialN = (String)params[3];
        this.account = (String)params[4];
        this.dateTo = (Date)params[5];
        this.dateClose = (Date)params[5];
    }

    protected abstract boolean isNumberCodeFilial();

    protected abstract boolean isNumberCodeCurrency();

    @Override
    public ArrayList<FilterItem> getFilter() {
        ArrayList<FilterItem> list = new ArrayList<>();
        String filialFltr = isNumberCodeFilial() ? filialN : filial;
        if (!isEmpty(filialFltr))
            list.add(new FilterItem(colFilial, FilterCriteria.EQ, filialFltr));
        String currencyFltr = isNumberCodeCurrency() ? currencyN : currency;
        if (!isEmpty(currencyFltr))
            list.add(new FilterItem(colCurrency, FilterCriteria.EQ, currencyFltr));
        if (!isEmpty(account)) {
            if (account.length() >= 5) {
                String acc2 = account.substring(0, 5);
                if (!(acc2.contains("%") || acc2.contains("_")))
                    list.add(new FilterItem(colAcc2, FilterCriteria.EQ, acc2));
            }
            boolean isPattern = account.contains("%") || account.contains("_");
            if ((account.length() != 5) || isPattern) {
                FilterCriteria accCriteria = (isPattern) ? FilterCriteria.LIKE : FilterCriteria.START_WITH;
                list.add(new FilterItem(colAccount, accCriteria, account));
            }
        }
        if (!isEmpty(dealSource))
            list.add(new FilterItem(colDealSource, FilterCriteria.EQ, dealSource));
        if (!isEmpty(dealId)) {
            list.add(new FilterItem(colDealId, FilterCriteria.START_WITH, dealId));
        }
        if (!isEmpty(customerNumber)) {
            FilterCriteria custCriteria = (customerNumber.contains("%") || customerNumber.contains("_"))
                    ? FilterCriteria.LIKE : FilterCriteria.START_WITH;
            list.add(new FilterItem(colCustomer, custCriteria, customerNumber));
        }
        if (null != dateFrom)
            list.add(new FilterItem(colDateOpen, FilterCriteria.GE, dateFrom));
        if (null != dateTo)
            list.add(new FilterItem(colDateOpen, FilterCriteria.LE, dateTo));
        if (null != dateClose)
            list.add(new FilterItem(colDateClose, FilterCriteria.GE, dateTo));
        return list;
    }

    public String getFilial() {
        return filial;
    }

    public void setFilial(String filial) {
        this.filial = filial;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public String getDealSource() {
        return dealSource;
    }

    public void setDealSource(String dealSource) {
        this.dealSource = dealSource;
    }

    public String getDealId() {
        return dealId;
    }

    public void setDealId(String dealId) {
        this.dealId = dealId;
    }

    public Date getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(Date dateFrom) {
        this.dateFrom = dateFrom;
    }

    public Date getDateTo() {
        return dateTo;
    }

    public void setDateTo(Date dateTo) {
        this.dateTo = dateTo;
    }

    public String getFilialN() {
        return filialN;
    }

    public void setFilialN(String filialN) {
        this.filialN = filialN;
    }

    public String getCurrencyN() {
        return currencyN;
    }

    public void setCurrencyN(String currencyN) {
        this.currencyN = currencyN;
    }
}
