package ru.rbt.barsgl.shared.monitoring;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by ER22317 on 17.04.2017.
 */
public class ReplTableItem2 implements Serializable {
    private String name;
    private int cntProc;
    private int cntWait;
    private int cntErr;
    private String strRestTime;
    private String strSpeed;

    public ReplTableItem2() {
    }

    //	SELECT TABLE_NAME,
//    count(case when IS_PROCESSED=0 then 1 else null end) is_wait,
//    count(case when IS_PROCESSED=1 then 1 else null end) is_proc,
//    count(case when IS_PROCESSED=-1 then 1 else null end) is_err
//FROM DWH.BARS_JRN
//GROUP BY TABLE_NAME

    public ReplTableItem2(String name, int cntProc, int cntWait, int cntErr ) {
        this.setName(name);
        this.setCntProc(cntProc);
        this.setCntWait(cntWait);
        this.setCntErr(cntErr);
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCntProc() {
        return cntProc;
    }

    public void setCntProc(int cntProc) {
        this.cntProc = cntProc;
    }

    public int getCntErr() {
        return cntErr;
    }

    public void setCntErr(int cntErr) {
        this.cntErr = cntErr;
    }

    public int getCntWait() {
        return cntWait;
    }

    public void setCntWait(int cntWait) {
        this.cntWait = cntWait;
    }

    public String getStrSpeed() {
        return strSpeed;
    }

    public void setStrSpeed(String strSpeed) {
        this.strSpeed = strSpeed;
    }

    public String getStrRestTime() {
        return strRestTime;
    }

    public void setStrRestTime(String strRestTime) {
        this.strRestTime = strRestTime;
    }
}
