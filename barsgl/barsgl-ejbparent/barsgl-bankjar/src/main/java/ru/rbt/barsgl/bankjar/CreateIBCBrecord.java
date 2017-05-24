// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3)
// Source File Name:   CreateIBCBrecord.java

package ru.rbt.barsgl.bankjar;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**Класс создает МФО счета для заданной пары филиалов и валюты.
 * Заполняетс справочник IBCB .*/
public class CreateIBCBrecord {

    private static final Logger logger = Logger.getLogger(CreateIBCBrecord.class);

    public static Date MAXDAY;
    public static Date DAY20080101;
    public static Date DAY20130101;

    static {
        try {
            SimpleDateFormat sf = new SimpleDateFormat("dd.MM.yyyy");
            MAXDAY = new Date(sf.parse("01.01.2029").getTime());
            DAY20080101 = new Date(sf.parse("01.01.2008").getTime());
            DAY20130101 = new Date(sf.parse("01.01.2013").getTime());
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
    }


    private CBAccount cBaccount = new CBAccount();

    @Inject
    private BankProcess process;

    @Inject
    private RequestContextHolder context;

    @EJB
    private OperdayController operdayController;

    public void calculateIBCBaccount(String brcaFrom, String brcaTo, String ccy)
            throws SQLException {

        // обязательна инициализация контекста при работе с компонентами "BANKJAR"
        context.setCurrDay(new Date(operdayController.getOperday().getCurrentDate().getTime()));

        String cbccy = getCbCurrencyCode(ccy);
        String fromCodes[] = getBicControlCode(brcaFrom);
        String toCodes[] = getBicControlCode(brcaTo);
        String lcvFrom = "" , lcvFrom5 = "";
        String lcvTo = "" , lcvTo5 = "";
        //Для Мросквы и Питера изначально перепутаны номера в хвосте счета.
        //Для новых счетов 30305/6 можно делать по обшей схеме.
        if(brcaFrom.equals("MOS") && brcaTo.equals("SPB"))
        {
            lcvFrom = "0000001";
            lcvTo = "0000002";
            lcvFrom5 = "0000002";
            lcvTo5 = "0000001";
        } else
        if(brcaFrom.equals("SPB") && brcaTo.equals("MOS"))
        {
            lcvFrom = "0000002";
            lcvTo = "0000001";
            lcvFrom5 = "0000001";
            lcvTo5 = "0000002";
        } else
        {
            String space = "0000000";
            lcvFrom = String.valueOf(space.substring(0, 7 - toCodes[0].length())) + String.valueOf(toCodes[0]);
            lcvTo = String.valueOf(space.substring(0, 7 - fromCodes[0].length())) + String.valueOf(fromCodes[0]);
            lcvFrom5 = lcvFrom;
            lcvTo5 = lcvTo;
        }
        calculateIBCBaccount(ccy, cbccy, fromCodes[1], fromCodes[0], lcvFrom, brcaFrom, brcaTo, lcvFrom5);
        calculateIBCBaccount(ccy, cbccy, toCodes[1], toCodes[0], lcvTo, brcaTo, brcaFrom, lcvTo5);
    }

    private void calculateIBCBaccount(String ccy, String cbccy, String bicCode, String brca1,
                              String lclCode, String brcaFrom, String brcaTo, String lclCode5) throws SQLException {

        String cb1 = cBaccount.calculateControlNumber(bicCode, "30301"+cbccy+"0"+brca1+lclCode);
        /*process.*/logger.debug("Calculate account =" + cb1);
        if(!process.existBsaAccount(cb1))
            process.insertAccount(" ", cb1, Constants.PASIV, process.currDay(), MAXDAY, "T", 0);

        String cb2 = cBaccount.calculateControlNumber(bicCode, "30302"+cbccy+"0"+brca1+lclCode);
        /*process.*/logger.debug("Calculate account ="+cb2);
        if(!process.existBsaAccount(cb2))
            process.insertAccount(" ", cb2, Constants.ACTIV, process.currDay(), MAXDAY, "T", 0);

        String cb305 = cBaccount.calculateControlNumber(bicCode, "30305"+cbccy+"0"+brca1+lclCode5);
        /*process.*/logger.debug("Calculate account ="+cb305);
        if(!process.existBsaAccount( cb305))
            process.insertAccount(" ", cb305, Constants.PASIV, process.currDay(), MAXDAY, "T", 0);

        String cb306 = cBaccount.calculateControlNumber(bicCode, "30306"+cbccy+"0"+brca1+lclCode5);
        /*process.*/logger.debug("Calculate account ="+cb306);
        if(!process.existBsaAccount( cb306))
            process.insertAccount(" ", cb306, Constants.ACTIV, process.currDay(), MAXDAY, "T", 0);

        if(!existIbcb(cb1, cb2, cbccy ))
            insertIbcb(brcaFrom, brcaTo, ccy, cb1, cb2, cb305, cb306);
    }

    private boolean existIbcb(String cb1, String cb2, String cbccy)
            throws SQLException
    {

        String sql = "select * from ibcb where ibacou=? and ibacin=? and ibccy=?";
        try (PreparedStatement statement = process.connection().prepareStatement(sql)){
            statement.setString(1, cb1);
            statement.setString(2, cb2);
            statement.setString(3, cbccy);
            try (ResultSet result = statement.executeQuery()){
                boolean exist = false;
                if (result.next())
                    exist = true;
                return exist;
            }
        }
    }

    private void insertIbcb(String brcaFrom, String brcaTo, String ccy, String cb1, String cb2,
                           String cb305, String cb306)
            throws SQLException {

        String sql = "insert into ibcb (ibbrnm, ibcbrn, ibccy, ibacou, ibacin, iba305, iba306) values (?,?,?,?,?,?,?) ";
        try (PreparedStatement  statement = process.connection().prepareStatement(sql)){
            statement.setString(1, brcaFrom);
            statement.setString(2, brcaTo);
            statement.setString(3, ccy);
            statement.setString(4, cb1);
            statement.setString(5, cb2);
            statement.setString(6, cb305);
            statement.setString(7, cb306);
            int i = statement.executeUpdate();
            if(i != 1) {
                logger.error("Not inserted in IBCB "+brcaFrom+" "+brcaTo+" "+ccy+" "+cb1+" "+cb2);
            }
        }
    }

    private String getCbCurrencyCode(String ccy) throws SQLException {
        String sql = "select cbccy from currency where glccy=?";
        try (PreparedStatement statement = process.connection().prepareStatement(sql)){
            statement.setString(1, ccy);
            try (ResultSet result = statement.executeQuery()){
                String cbccy = null;
                if(result.next())
                    cbccy = result.getString(1);
                return cbccy;
            }
        }
    }

    private String[] getBicControlCode(String branch) throws SQLException {
        String sql = "select ccbbr, bxbicc from imbcbcmp p, imbcbbrp r left join sdcustpd c on c.bbcust=r.a8bicn " +
                "where '0'||r.a8brcd=p.ccbbr and p.ccpcd=? ";
        try (PreparedStatement statement = process.connection().prepareStatement(sql)){
            statement.setString(1, branch);
            try (ResultSet result = statement.executeQuery()){
                String code = null;
                String bic = null;
                if(result.next())
                {
                    code = result.getString(1);
                    String _bic = result.getString(2);
                    if(_bic.length() >= 9)
                        bic = _bic.substring(6, 9);
                }
                return (new String[] {code, bic});
            }
        }
    }

}
