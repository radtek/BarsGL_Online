package ru.rbt.barsgl.ejb.integr.acc.wrap;

import ru.rbt.barsgl.ejb.entity.acc.GLAccount;

/**
 * Created by Ivan Sevastyanov on 19.10.2018.
 * используется для анализа создан ли счет в текущем потоке
 */
public class GLAccountCreated {

    private final GLAccount account;
    private final boolean newAccount;

    public GLAccountCreated(GLAccount account, boolean newAccount) {
        this.account = account;
        this.newAccount = newAccount;
    }

    public GLAccount getAccount() {
        return account;
    }

    public boolean isNewAccount() {
        return newAccount;
    }
}
