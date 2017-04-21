package ru.rbt.barsgl.ejb.entity.dict;

import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.barsgl.shared.enums.StamtUnloadParamType;
import ru.rbt.barsgl.shared.enums.StamtUnloadParamTypeCheck;

import javax.persistence.*;

/**
 * Created by ER21006 on 19.01.2016.
 */
@Entity
@Table(name = "GL_STMPARM")
public class StamtUnloadParam extends BaseEntity<String> {

    @Id
    @Column(name = "ACCOUNT")
    private String account;

    @Column(name = "ACCTYPE")
    @Enumerated(EnumType.STRING)
    private StamtUnloadParamType type;

    @Column(name = "INCLUDE")
    @Enumerated(EnumType.ORDINAL)
    private StamtUnloadParamTypeCheck check;

    @Column(name = "INCLUDEBLN")
    @Enumerated(EnumType.ORDINAL)
    private StamtUnloadParamTypeCheck checkBln;

    public StamtUnloadParam() {
    }

    public StamtUnloadParam(String account, StamtUnloadParamType type, StamtUnloadParamTypeCheck check,  StamtUnloadParamTypeCheck checkBln) {
        this.account = account;
        this.type = type;
        this.check = check;
        this.checkBln = checkBln;
    }

    @Override
    public String getId() {
        return account;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public StamtUnloadParamType getType() {
        return type;
    }

    public void setType(StamtUnloadParamType type) {
        this.type = type;
    }

    public StamtUnloadParamTypeCheck getCheck() {
        return check;
    }

    public void setCheck(StamtUnloadParamTypeCheck check) {
        this.check = check;
    }

    public StamtUnloadParamTypeCheck getCheckBln() {
        return checkBln;
    }

    public void setCheckBln(StamtUnloadParamTypeCheck checkBln) {
        this.checkBln = checkBln;
    }
}
