package ru.rbt.barsgl.ejb.rep;

/**
 * Created by akichigi on 03.04.17.
 */
public class NightErrRep {
    public String getSql(){
        return "select CUST_DR, CUST_CR, ID, ID_PST, ID_PKG, SRC_PST, EVT_ID, DEAL_ID, CHNL_NAME, " +
                "PMT_REF, DEPT_ID, VDATE, OTS, NRT, RNRTL, RNRTS, STRN, STRNRF, AC_DR, CCY_DR, " +
                "AMT_DR, AMTRU_DR, AC_CR, CCY_CR, AMT_CR, AMTRU_CR, FAN, PAR_RF, ECODE, EMSG, " +
                "ACCKEY_DR, ACCKEY_CR, EVTP, IS_VIP from V_GLA2_ERRORS";
    }
}
