package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;

import javax.ejb.EJB;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * Created by ER22317 on 18.05.2016.
 */
public class DwhTools {
    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    public void insTable(String insTab) throws Exception {
        beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
            try (PreparedStatement ins = connection.prepareStatement(insTab)){
                ins.executeUpdate();
            }
            return null;
        }, 60 * 60);
    }

    public void delTable(String delTab) throws Exception {
        beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
            try (PreparedStatement del = connection.prepareStatement(delTab)){
                del.executeUpdate();
            }
            return null;
        }, 60 * 60);

    }

    public java.sql.Date getWorkDay() throws Exception {
        return beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
            try (PreparedStatement selWorkDay = connection.prepareStatement("select WORKDAY from WORKDAY");
                 ResultSet rsWorkDay = selWorkDay.executeQuery();){
                if (rsWorkDay.next()) return rsWorkDay.getDate("WORKDAY");
                else throw new Exception("WORKDAY not found");
            }
        }, 60 * 60);
    }

    public java.sql.Date getOperDay() throws Exception {
        return beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
            try (PreparedStatement selOperDay = connection.prepareStatement("select case when PHASE = 'ONLINE' then LWDATE else CURDATE end from Gl_OD");
                 ResultSet rsOperDay = selOperDay.executeQuery();){
                if (rsOperDay.next()) return rsOperDay.getDate(1);
                else throw new Exception("OPERDAY not found");
            }
        }, 60 * 60);
    }

    public boolean isStepInOperDay(String stepName, java.sql.Date operDay) throws Exception {
        return beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
            try (PreparedStatement selStep = connection.prepareStatement("select operday from Gl_Etldwhs where pardesc=? and parvalue='1' and operday=?")){
                 selStep.setString(1, stepName);
                 selStep.setDate(2, operDay);
                 try(ResultSet rsStep = selStep.executeQuery()) {
                     return rsStep.next();
                 }
            }
        }, 60 * 60);
    }

    public Long getPdidMax(java.sql.Date operDay) throws Exception {
        return beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
            try (PreparedStatement selGlVprj = connection.prepareStatement("SELECT PDID_MAX FROM GL_VPRJ WHERE LOAD_TS = (SELECT MAX(LOAD_TS) FROM GL_VPRJ " +
                                                                           "WHERE LOAD_OD = (SELECT MAX(LOAD_OD) FROM GL_VPRJ WHERE LOAD_OD < ?))")){
                selGlVprj.setDate(1, operDay);
                try(ResultSet rsGlVprj = selGlVprj.executeQuery()) {
                    if (rsGlVprj.next()) return rsGlVprj.getLong(1);
                    else return Long.valueOf(0);
//                    else throw new Exception("PDID_MAX not found");
                }
            }
        }, 60 * 60);
    }

    public int endGlEtldwhs(long id, long parvalue) throws Exception {
        return beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
            int i;
            try(PreparedStatement updGlEtldwhs = connection.prepareStatement("update Gl_Etldwhs set PARVALUE=?, END_LOAD=? where id=?");
            ){
                updGlEtldwhs.setLong(1, parvalue);
                updGlEtldwhs.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                updGlEtldwhs.setLong(3, id);
                i = updGlEtldwhs.executeUpdate();
            }
            return i;
        }, 60 * 60);
    }

    public long startGlEtldwhs(String pardesc, java.sql.Date operDay) throws Exception {
        return beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
            //GL_BATPST_SEQ.nextval
            try(
                //       1       2        3       4       5          6
                PreparedStatement insGlEtldwhs = connection.prepareStatement("insert into Gl_Etldwhs(ID,PARNAME,PARVALUE,PARDESC,OPERDAY,START_LOAD) values(GL_ETLDWHS_SEQ.NEXTVAL,'BARS_GL_DWH',0,?,?,?)");
                PreparedStatement idValStmnt = connection.prepareStatement("SELECT GL_ETLDWHS_SEQ.CURRVAL id FROM DUAL");
                ){
                insGlEtldwhs.setString(1, pardesc);
                insGlEtldwhs.setDate(2, operDay);
                insGlEtldwhs.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                insGlEtldwhs.executeUpdate();
                try(ResultSet rs = idValStmnt.executeQuery();){
                    if (rs.next()) {
                        return rs.getLong(1);
                    }else throw new Exception("DwhTools.startGlEtldwhs() id not found");
                }
            }
        }, 60 * 60);

    }

//    public long startGlEtldwhs(String pardesc, java.sql.Date operDay) throws Exception {
//        return beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
//            try(PreparedStatement selCurDate = connection.prepareStatement("select curdate from gl_od");
//                //       1       2        3       4       5          6
//                PreparedStatement insGlEtldwhs = connection.prepareStatement("insert into Gl_Etldwhs(PARNAME,PARVALUE,PARDESC,OPERDAY,START_LOAD) values('BARS_GL_DWH',0,?,?,?)", Statement.RETURN_GENERATED_KEYS);
//                ResultSet rsCurDate = selCurDate.executeQuery();){
//                rsCurDate.next();
//                java.util.Date curDate = rsCurDate.getDate(1);
//                insGlEtldwhs.setString(1, pardesc);
//                insGlEtldwhs.setDate(2, new java.sql.Date(curDate.getTime()));
//                insGlEtldwhs.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
//                insGlEtldwhs.executeUpdate();
//                ResultSet rs = insGlEtldwhs.getGeneratedKeys();
//                if (rs.next()) {
//                    return rs.getLong(1);
//                }else throw new Exception("DwhTools.startGlEtldwhs() id not found");
//            }
//        }, 60 * 60);
//
//    }

    public static BigDecimal getNumber(Long f){
        return f==null? null: new BigDecimal( f);
    }
}
