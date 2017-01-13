package ru.rbt.barsgl.shared.dict;

import com.google.gwt.user.client.rpc.IsSerializable;
import ru.rbt.barsgl.shared.enums.StamtUnloadParamType;
import ru.rbt.barsgl.shared.enums.StamtUnloadParamTypeCheck;

import java.io.Serializable;

/**
 * Created by ER21006 on 19.01.2016.
 */
public class StamtUnloadParamWrapper implements Serializable,IsSerializable {

    private String account;
    private StamtUnloadParamType paramType;
    private StamtUnloadParamTypeCheck paramTypeCheck;
    private StamtUnloadParamTypeCheck paramTypeCheckBln;

    public StamtUnloadParamWrapper() {
    }

    public StamtUnloadParamWrapper(String account, StamtUnloadParamType paramType, StamtUnloadParamTypeCheck paramTypeCheck, StamtUnloadParamTypeCheck paramTypeCheckBln) {
        this.account = account;
        this.paramType = paramType;
        this.paramTypeCheck = paramTypeCheck;
        this.paramTypeCheckBln = paramTypeCheckBln;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public StamtUnloadParamType getParamType() {
        return paramType;
    }

    public void setParamType(StamtUnloadParamType paramType) {
        this.paramType = paramType;
    }

    public StamtUnloadParamTypeCheck getParamTypeCheck() {
        return paramTypeCheck;
    }

    public void setParamTypeCheck(StamtUnloadParamTypeCheck paramTypeCheck) {
        this.paramTypeCheck = paramTypeCheck;
    }

    public StamtUnloadParamTypeCheck getParamTypeCheckBln() {
        return paramTypeCheckBln;
    }

    public void setParamTypeCheckBln(StamtUnloadParamTypeCheck paramTypeCheckBln) {
        this.paramTypeCheckBln = paramTypeCheckBln;
    }
}
