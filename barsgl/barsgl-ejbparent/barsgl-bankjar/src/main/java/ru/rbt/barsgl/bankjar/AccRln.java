// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   AccRln.java

package ru.rbt.barsgl.bankjar;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AccRln
{

    public AccRln(String acid, String bsaacid, String rlntype, Date drlno, Date drlnc, int ctype, String cnum,
                  String psav, String glacod, String cbccy, String pairbsa, String transactsrc)
    {
        this.acid = acid;
        this.bsaacid = bsaacid;
        this.rlntype = rlntype;
        this.drlno = drlno;
        this.drlnc = drlnc;
        this.ctype = ctype;
        this.cnum = cnum;
        setCcode(bsaacid);
        setAcc2(bsaacid);
        this.psav = psav;
        this.glacod = glacod;
        this.cbccy = cbccy;
        setPlcode(bsaacid, rlntype, psav);
        setIncl(bsaacid);
        this.pairbsa = pairbsa;
        this.transactsrc = transactsrc;
    }

    public void setPlcode(String parBsaacid, String parRlntype, String parPsav)
    {
        if(parRlntype.equals("2"))
        {
            if(parBsaacid.startsWith("706") || parBsaacid.startsWith("707"))
            {
                if(parPsav.startsWith(Constants.ACTIV))
                    plcode = "2".concat(String.valueOf(String.valueOf(parBsaacid.substring(13, 17))));
                else
                if(parPsav.startsWith(Constants.PASIV))
                    plcode = "1".concat(String.valueOf(String.valueOf(parBsaacid.substring(13, 17))));
                else
                    plcode = parBsaacid.substring(13, 18);
            } else
            {
                plcode = parBsaacid.substring(13, 18);
            }
        } else
        {
            plcode = "";
        }
    }

    public void setIncl(String parBsaacid)
    {
        incl = "0";
        if(parBsaacid.startsWith("7") && parBsaacid.length() > 8 && !parBsaacid.substring(5, 8).equals("810"))
            incl = "1";
    }

    public void setCcode(String parBsaacid)
    {
        if(parBsaacid.length() < 13)
            ccode = "";
        else
            ccode = "00".concat(String.valueOf(String.valueOf(parBsaacid.substring(11, 13))));
    }

    public void setAcc2(String parBsaacid)
    {
        if(parBsaacid.length() < 13)
            acc2 = "";
        else
            acc2 = parBsaacid.substring(0, 5);
    }

    public static PreparedStatement PrepareInsertStatement(Connection connection, String schemaDWH)
        throws SQLException
    {
        PreparedStatement stmt = null;
        //String strStmtSQL = String.valueOf(String.valueOf((new StringBuffer("insert into ")).append(schemaDWH).append(".accrln ").append("(acid, bsaacid, rlntype, ").append(" drlno, drlnc, ctype, ").append(" cnum,  ccode, acc2,  ").append(" psav, glacod, cbccy, ").append(" plcode, incl, pairbsa,transactsrc) ").append(" values (?,?,?, ").append("         ?,?,?, ").append("         ?,?,?, ").append("         ?,?,?, ").append("         ?,?,?,? ) ")));
        String strStmtSQL = String.valueOf(String.valueOf((new StringBuffer("insert into ")).append("accrln ").append("(acid, bsaacid, rlntype, ").append(" drlno, drlnc, ctype, ").append(" cnum,  ccode, acc2,  ").append(" psav, glacod, cbccy, ").append(" plcode, incl, pairbsa,transactsrc) ").append(" values (?,?,?, ").append("         ?,?,?, ").append("         ?,?,?, ").append("         ?,?,?, ").append("         ?,?,?,? ) ")));
        stmt = connection.prepareStatement(strStmtSQL);
        return stmt;
    }

    public int Insert(PreparedStatement stmt)
        throws SQLException
    {
        int insert_cnt = 0;
        stmt.setString(1, acid);
        stmt.setString(2, bsaacid);
        stmt.setString(3, rlntype);
        stmt.setDate(4, drlno);
        stmt.setDate(5, drlnc);
        stmt.setInt(6, ctype);
        stmt.setString(7, cnum);
        stmt.setString(8, ccode);
        stmt.setString(9, acc2);
        stmt.setString(10, psav);
        stmt.setString(11, glacod);
        stmt.setString(12, cbccy);
        stmt.setString(13, plcode);
        stmt.setString(14, incl);
        stmt.setString(15, pairbsa);
        stmt.setString(16, transactsrc);
        insert_cnt = stmt.executeUpdate();
        return insert_cnt;
    }

    public static PreparedStatement PrepareUpdateDRLNCstatement(Connection connection, String schemaDWH)
        throws SQLException
    {
        PreparedStatement stmt = null;
        //String stmtText = String.valueOf(String.valueOf((new StringBuffer("update ")).append(schemaDWH).append(".accrln ").append("set DRLNC = ? ").append("where acid = ? and rlntype = ? and DRLNC = cast('1.1.2029' as date) and ctype = ? ")));
        String stmtText = String.valueOf(String.valueOf((new StringBuffer("update ")).append("accrln ").append("set DRLNC = ? ").append("where acid = ? and rlntype = ? and DRLNC = cast('1.1.2029' as date) and ctype = ? ")));
        stmt = connection.prepareStatement(stmtText);
        return stmt;
    }

    public int setDRLNC(PreparedStatement stmt)
        throws SQLException
    {
        int updated_cnt = 0;
        stmt.setDate(1, drlnc);
        stmt.setString(2, acid);
        stmt.setString(3, rlntype);
        stmt.setInt(4, ctype);
        updated_cnt = stmt.executeUpdate();
        return updated_cnt;
    }

    public static final String TRANSACTSRC_DEFAULT_VALUE = "000";
    public static final String PAIRBSA_DEFAULT_VALUE = "";
    public String acid;
    public String bsaacid;
    public String rlntype;
    public Date drlno;
    public Date drlnc;
    public int ctype;
    public String cnum;
    public String ccode;
    public String acc2;
    public String psav;
    public String glacod;
    public String cbccy;
    public String plcode;
    public String incl;
    public String pairbsa;
    public String transactsrc;

}
