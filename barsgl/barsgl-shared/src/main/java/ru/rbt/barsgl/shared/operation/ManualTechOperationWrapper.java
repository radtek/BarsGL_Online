package ru.rbt.barsgl.shared.operation;

import ru.rbt.barsgl.shared.account.ManualAccountWrapper;

/**
 * Created by er23851 on 31.03.2017.
 */
public class ManualTechOperationWrapper extends ManualOperationWrapper {

    protected String accountTypeDebit;
    protected String accountTypeCredit;

    public String getAccountTypeDebit() {
        return accountTypeDebit;
    }

    public void setAccountTypeDebit(String accountTypeDebit) {
        this.accountTypeDebit = accountTypeDebit;
    }

    public String getAccountTypeCredit() {
        return accountTypeCredit;
    }

    public void setAccountTypeCredit(String accountTypeCredit) {
        this.accountTypeCredit = accountTypeCredit;
    }
}
