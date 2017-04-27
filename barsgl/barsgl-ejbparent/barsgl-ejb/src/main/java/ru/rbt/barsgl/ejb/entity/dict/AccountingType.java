/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.ejb.entity.dict;

import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.ejbcore.mapping.YesNo;

import javax.persistence.*;

/**
 * @author Andrew Samsonov
 */
@Entity
@Table(name = "GL_ACTNAME")
public class AccountingType extends BaseEntity<String> {

    @Id
    @Column(name = "ACCTYPE", nullable = false, length = 10)
    private String id;

    @Column(name = "ACCNAME", nullable = false, length = 255)
    private String accountName;

    @Column(name = "PL_ACT")
    @Enumerated(EnumType.STRING)
    private YesNo barsAllowed;

    @Column(name = "FL_CTRL")
    @Enumerated(EnumType.STRING)
    private YesNo checkedAccount;

    @Override
    public String getId() {
        return id;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public AccountingType() {
    }

    public AccountingType(String id, String accountName) {
        this.id = id;
        this.accountName = accountName;
    }

    public AccountingType(String id, String accountName, YesNo barsAllowed, YesNo checkedAccount) {
        this.id = id;
        this.accountName = accountName;
        this.barsAllowed = barsAllowed;
        this.checkedAccount = checkedAccount;
    }

    public YesNo getBarsAllowed() {
        return barsAllowed;
    }

    public boolean isBarsAllowed() {
        return barsAllowed == YesNo.Y;
    }

    public void setBarsAllowed(YesNo barsAllowed) {
        this.barsAllowed = barsAllowed;
    }

    public YesNo getCheckedAccount() {
        return checkedAccount;
    }

    public void setCheckedAccount(YesNo checkedAccount) {
        this.checkedAccount = checkedAccount;
    }
}
