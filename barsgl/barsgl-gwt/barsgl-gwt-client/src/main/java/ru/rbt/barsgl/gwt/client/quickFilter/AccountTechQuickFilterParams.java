package ru.rbt.barsgl.gwt.client.quickFilter;

import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.shared.filter.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;

import java.util.ArrayList;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;

/**
 * Created by ER18837 on 24.05.16.
 */
public abstract class AccountTechQuickFilterParams implements IQuickFilterParams {
    private Column colFilial;
    private Column colCurrency;
    private Column colAccType;

    private String filial;
    private String currency;
    private String acctype;
    private String filialN;
    private String currencyN;

    public AccountTechQuickFilterParams(Column colFilial, Column colCurrency, Column colAccType) {
        this.colFilial = colFilial;
        this.colCurrency = colCurrency;
        this.colAccType = colAccType;
    }

    @Override
    public void setInitialFilterParams(Object[] params) {
        if (null == params)
            return;
        this.filial = (String)params[0];
        this.currency = (String)params[1];
        this.acctype = (String)params[2];
    }

    protected abstract boolean isNumberCodeFilial();

    protected abstract boolean isNumberCodeCurrency();

    @Override
    public ArrayList<FilterItem> getFilter() {
        ArrayList<FilterItem> list = new ArrayList<>();
        if (!isEmpty(filial))
            list.add(new FilterItem(colFilial, FilterCriteria.EQ, filial));
        if (!isEmpty(currency))
            list.add(new FilterItem(colCurrency, FilterCriteria.EQ, currency));

        if (!isEmpty(acctype)) {
            boolean isPattern = acctype.contains("%") || acctype.contains("_");
            if ((acctype.length() < 9) || isPattern) {
                FilterCriteria accCriteria = (isPattern) ? FilterCriteria.LIKE : FilterCriteria.START_WITH;
                list.add(new FilterItem(colAccType, accCriteria, Integer.valueOf(acctype).toString()));
            }
            else {
                FilterCriteria accCriteria = FilterCriteria.EQ;
                list.add(new FilterItem(colAccType,accCriteria,acctype));
            }
        }
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

    public String getAcctype() {
        return acctype;
    }

    public void setAcctype(String acctype) {
        this.acctype = acctype;
    }
}
