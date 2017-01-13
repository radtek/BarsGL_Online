package ru.rbt.barsgl.bankjar;

import java.sql.Date;

/**
 * Created by Ivan Sevastyanov
 */
public class RequestContextHolder extends AbstractBankjarBean {

    private Date currDay;

    public Date getCurrDay() {
        return currDay;
    }

    public void setCurrDay(Date currDay) {
        this.currDay = currDay;
    }
}
