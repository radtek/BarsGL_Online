package ru.rbt.barsgl.ejb.entity.acc;

import ru.rbt.barsgl.shared.Builder;

/**
 * Created by Ivan Sevastyanov
 */
public class AccountKeysBuilder implements Builder<AccountKeys> {

    private AccountKeys keys;

    private AccountKeysBuilder() {}

    public static AccountKeysBuilder create() {
        AccountKeysBuilder builder = new AccountKeysBuilder();
        builder.keys = new AccountKeys("");
        return builder;
    }

    public static AccountKeysBuilder createDefault() {
        AccountKeysBuilder builder = new AccountKeysBuilder();
        builder.keys = new AccountKeys("001.USD.00200428.501020501.0.0.09..47408....DEALSRC.DEALID.SUBDEALID");
        return builder;
    }

    public AccountKeysBuilder withAcc2(String acc2) {
        keys.setAccount2(acc2);
        return this;
    }

    public AccountKeysBuilder withCurrency(String currencyCode) {
        keys.setCurrency(currencyCode);
        return this;
    }

    public AccountKeysBuilder withCurrencyDigital(String currencyCode) {
        keys.setCurrencyDigital(currencyCode);
        return this;
    }

    public AccountKeysBuilder withBranch(String branch) {
        keys.setBranch(branch);
        return this;
    }

    public AccountKeysBuilder withCompanyCode(String companyCode) {
        keys.setCompanyCode(companyCode);
        return this;
    }

    public AccountKeysBuilder withPlCode(String plCode) {
        keys.setPlCode(plCode);
        return this;
    }

    public AccountKeysBuilder withCustomerNumber(String customerNumber) {
        keys.setCustomerNumber(customerNumber);
        return this;
    }

    public AccountKeysBuilder withAccountType(String accountType) {
        keys.setAccountType(accountType);
        return this;
    }

    public AccountKeysBuilder withGlSequence(String glSequence) {
        keys.setGlSequence(glSequence);
        return this;
    }

    public AccountKeysBuilder withAccountCode(String accountCode) {
        keys.setAccountCode(accountCode);
        return this;
    }

    public AccountKeysBuilder withAccSequence(String accSequence) {
        keys.setAccSequence(accSequence);
        return this;
    }

    public AccountKeysBuilder withCustomerType(String customerType) {
        keys.setCustomerType(customerType);
        return this;
    }

    public AccountKeysBuilder withTerm(String term) {
        keys.setTerm(term);
        return this;
    }

    public AccountKeysBuilder withDealSource(String dealSrc) {
        keys.setDealSource(dealSrc);
        return this;
    }

    public AccountKeysBuilder withDealId(String dealId) {
        keys.setDealId(dealId);
        return this;
    }

    public AccountKeysBuilder withSubDealId(String subDealId) {
        keys.setSubDealId(subDealId);
        return this;
    }

    public AccountKeysBuilder withFilial(String filial) {
        keys.setFilial(filial);
        return this;
    }

    public AccountKeysBuilder withRelationType(String value) {
        keys.setRelationType(value);
        return this;
    }

    public AccountKeysBuilder withPassiveActive(String value) {
        keys.setPassiveActive(value);
        return this;
    }

    public AccountKeysBuilder withAccountMidas(String value) {
        keys.setAccountMidas(value);
        return this;
    }

    @Override
    public AccountKeys build() {
        return keys;
    }

}
