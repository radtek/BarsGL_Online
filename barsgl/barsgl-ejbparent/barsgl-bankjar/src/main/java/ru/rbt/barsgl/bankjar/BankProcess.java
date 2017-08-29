package ru.rbt.barsgl.bankjar;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Ivan Sevastyanov
 */
//@LocalBean
public class BankProcess extends AbstractBankjarBean {

    private static final Logger logger = Logger.getLogger(BankProcess.class);


    @Inject
    private RequestContextHolder context;

    @EJB
    private OperdayController operdayController;


    public void insertAccount(String accountMidas, String accountCB, String type, Date openDay, Date closeDay, String relationType, int customerType)
            throws SQLException {
        String sql2 = "insert into bsaacc (id, bssac, ccy, bsakey, brca, bsacode, bsaaco, bsaacc, bsatype, bsagrp, bsasubtype) values (?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement statement = connection().prepareStatement(sql2);
        statement.setString(1, accountCB);
        statement.setString(2, accountCB.substring(0, 5));
        statement.setString(3, accountCB.substring(5, 8));
        statement.setString(4, accountCB.substring(8, 9));
        statement.setString(5, "00".concat(String.valueOf(String.valueOf(accountCB.substring(11, 13)))));
        statement.setString(6, accountCB.substring(13, 20));
        statement.setDate(7, openDay);
        statement.setDate(8, closeDay);
        statement.setString(9, type);
        statement.setString(10, relationType);
        String subType = "";
        if (accountCB.length() == 20) {
            String cCode = accountCB.substring(13, 15);
            if (cCode.equals("12"))
                subType = "P1";
            else if (cCode.equals("13"))
                subType = "P2";
        }
        statement.setString(11, subType);
        logger.debug(String.valueOf(String.valueOf((new StringBuffer("Create account CB=")).append(accountCB).append(" type=").append(type).append(" relation=").append(relationType))));
        statement.executeUpdate();
        // 16.04.2010 Dmitry Sedov: changed substring positions for accountMidas
        AccRln NewAccRln = new AccRln(accountMidas, accountCB, relationType, openDay, closeDay, customerType, accountMidas.length() >= 8 ? accountMidas.substring(0, 8) : accountMidas, type, accountMidas.length() >= 15 ? accountMidas.substring(11, 15) : "", accountCB.length() >= 8 ? accountCB.substring(5, 8) : "", "", "000");
        PreparedStatement stmtInsAccRln = AccRln.PrepareInsertStatement(connection(), schemaDWH());
        NewAccRln.Insert(stmtInsAccRln);
    }

    public boolean existBsaAccount(String bsaAccount)
            throws SQLException {
        String sql = "select id from bsaacc where id=? ";
        boolean exist;
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setString(1, bsaAccount);
            try (ResultSet result = statement.executeQuery()) {
                exist = false;
                if (result.next())
                    exist = true;
            }
        }
        return exist;
    }

    public Date currDay() {
        return context.getCurrDay();
    }

/*
    public CoreRepository getRepository() {
        return repository;
    }
*/

}
