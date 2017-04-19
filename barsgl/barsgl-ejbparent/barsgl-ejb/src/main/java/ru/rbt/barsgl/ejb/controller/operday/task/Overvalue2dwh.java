package ru.rbt.barsgl.ejb.controller.operday.task;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.common.repository.od.OperdayRepository;
import ru.rbt.ejbcore.controller.etc.TextResourceController;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.datarec.DefaultJdbcAdapter;
import ru.rbt.ejbcore.datarec.JdbcAdapter;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static ru.rbt.audit.entity.AuditRecord.LogCode.*;

/**
 * Created by ER22317 on 17.05.2016.
 */
public class Overvalue2dwh implements ParamsAwareRunnable {
    private static final Logger log = Logger.getLogger(Overvalue2dwh.class);
    @EJB
    private BeanManagedProcessor beanManagedProcessor;
    @EJB
    private AuditController auditController;
    @Inject
    private TextResourceController textResourceController;
    @Inject
    private DwhTools dwhTools;
    @Inject
    private OperdayRepository repository;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        int count = 0;
        long id = 0l;
        java.sql.Date operDay = null, workDay = null;
        Long pdidMax = 1l;
        String od = properties.getProperty("OperDay");
        String wd = properties.getProperty("WorkDay");
        String propdidMax = properties.getProperty("pdidMax");
        boolean isSkipMI4GL = properties.getProperty("skipMI4GL").equals("true");
        boolean isWorkDayFromBase = true;

        if (od == null) {
            operDay = dwhTools.getOperDay();
            log.info("OperDay from base = " + operDay);
        }else{
            try {
                operDay = new java.sql.Date(new SimpleDateFormat("yyyy-MM-dd").parse(od).getTime());
                log.info("OperDay from properties = " + od);
            }catch (Exception e){
                log.error("error OperDay = " + od);
                throw e;
            }
        }
        if (wd == null){
            workDay = dwhTools.getWorkDay();
            log.info("WorkDay from base = " + workDay);
        }else{
            try {
                workDay = new java.sql.Date(new SimpleDateFormat("yyyy-MM-dd").parse(wd).getTime());
                isWorkDayFromBase = false;
                log.info("WorkDay from properties = " + wd);
            }catch (Exception e){
                log.error("error WorkDay = " + wd);
                throw e;
            }
        }
        if (propdidMax == null){
           pdidMax = dwhTools.getPdidMax(operDay);
        }else{
            try {
               pdidMax = Long.parseLong(propdidMax);
            }catch (Exception e){
                log.error("error propdidMax = " + propdidMax);
                throw e;
            }
        }

        String mess = "Overvalue2dwh запущена, OperDay = "+od + "; WorkDay = "+wd;
        log.info(mess);
        auditController.info(PSTR_ACC_R_LOAD, mess);

        if (!isSkipMI4GL && !isMI4GLfinished(workDay)){
            log.info("MI4GL не закончен");
            auditController.info(PSTR_ACC_R_LOAD, "MI4GL не закончен");
            return;
        }
//1 Accounts "PSTR_ACC_R LOAD"
        try {
            if (!dwhTools.isStepInOperDay(DwhUnloadParams.UnloadOverValueAcc.getParamDesc(), operDay)) {
                id = dwhTools.startGlEtldwhs(DwhUnloadParams.UnloadOverValueAcc.getParamDesc(), operDay);
                count = unLoadAcc(workDay);
                dwhTools.endGlEtldwhs(id, 1);
                String result = DwhUnloadParams.UnloadOverValueAcc.getParamDesc()+" выполнено, обработано записей " + count;
                log.info(result);
                auditController.info(PSTR_ACC_R_LOAD, result);
            }
            else {
                log.info(DwhUnloadParams.UnloadOverValueAcc.getParamDesc()+" в operDay = " + operDay + " уже выполнялся");
                auditController.error( PSTR_ACC_R_LOAD, DwhUnloadParams.UnloadOverValueAcc.getParamDesc()+" в operDay = " + operDay + " уже выполнялся", null, "");
                return;
            }
        }catch(Exception e){
            dwhTools.endGlEtldwhs(id, 2);
            String Err = "Ошибка "+DwhUnloadParams.UnloadOverValueAcc.getParamDesc()+" = " + e.getMessage();
            log.error(Err);
            auditController.error( PSTR_ACC_R_LOAD, Err, null, e);
            e.printStackTrace();
        }
//3 Changes "GLVD_PSTR_UD"
        try {
            if ( dwhTools.isStepInOperDay(DwhUnloadParams.UnloadOverValueAcc.getParamDesc(), operDay) && !dwhTools.isStepInOperDay(DwhUnloadParams.UnloadOverValueChanges.getParamDesc(), operDay)) {
                id = dwhTools.startGlEtldwhs(DwhUnloadParams.UnloadOverValueChanges.getParamDesc(), operDay);
                count = unLoadChange(workDay, operDay, pdidMax);
                dwhTools.endGlEtldwhs(id, 1);
                String result = DwhUnloadParams.UnloadOverValueChanges.getParamDesc()+" выполнено, обработано записей " + count;
                log.info(result);
                auditController.info(GLVD_PSTR_UD, result);
            }else{
                String Err = DwhUnloadParams.UnloadOverValueChanges.getParamDesc()+" в operday = "+operDay+" уже выполнялся или шаг "+DwhUnloadParams.UnloadOverValueAcc.getParamDesc()+" не выполнялся";
                log.error(Err);
                auditController.error( GLVD_PSTR_UD, "Ошибка при обработке "+DwhUnloadParams.UnloadOverValueChanges.getParamDesc(), null, Err);
                return;
            }
        }catch(Exception e){
            dwhTools.endGlEtldwhs(id, 2);
            String Err = "Ошибка "+DwhUnloadParams.UnloadOverValueChanges.getParamDesc()+" = " + e.getMessage();
            log.error(Err);
            auditController.error( GLVD_PSTR_UD, Err, null, e);
            e.printStackTrace();
        }
//2 Statements "GLVD_PSTR LOAD"
        try {
            if (dwhTools.isStepInOperDay(DwhUnloadParams.UnloadOverValueChanges.getParamDesc(), operDay) && !dwhTools.isStepInOperDay(DwhUnloadParams.UnloadOverValueStatements.getParamDesc(), operDay)) {
                id = dwhTools.startGlEtldwhs(DwhUnloadParams.UnloadOverValueStatements.getParamDesc(), operDay);
                repository.executeNativeUpdate("call GL_DWHLD_R(?,?)", new Object[]{workDay, (isWorkDayFromBase ? "Y" : "N")});
                dwhTools.endGlEtldwhs(id, 1);
                String result = DwhUnloadParams.UnloadOverValueStatements.getParamDesc() + " выполнено";
                log.info(result);
                auditController.info(GLVD_PSTR_LOAD, result);
            }else{
                String Err = DwhUnloadParams.UnloadOverValueStatements.getParamDesc() + " в operday = "+operDay+" уже выполнялся или шаг "+DwhUnloadParams.UnloadOverValueChanges.getParamDesc()+" не выполнялся";
                log.error(Err);
                auditController.error( GLVD_PSTR_LOAD, Err, null, Err);
                return;
            }
        }catch(Exception e){
            dwhTools.endGlEtldwhs(id, 2);
            String Err = "Ошибка "+DwhUnloadParams.UnloadOverValueStatements.getParamDesc()+" = " + e.getMessage();
            log.error(Err);
            auditController.error( GLVD_PSTR_LOAD, Err, null, e);
            e.printStackTrace();
        }
//4 Balance "GLVD_BAL_R"
        try {
            if (dwhTools.isStepInOperDay(DwhUnloadParams.UnloadOverValueStatements.getParamDesc(), operDay) && !dwhTools.isStepInOperDay(DwhUnloadParams.UnloadOverValueBal.getParamDesc(), operDay)){
                id = dwhTools.startGlEtldwhs(DwhUnloadParams.UnloadOverValueBal.getParamDesc(), operDay);
                count = unLoadBal(workDay, operDay);
                dwhTools.endGlEtldwhs(id, 1);
                String result = DwhUnloadParams.UnloadOverValueBal.getParamDesc()+" выполнено, обработано записей " + count;
                log.info(result);
                auditController.info(GLVD_BAL_R, result);
            }else{
                String Err = DwhUnloadParams.UnloadOverValueBal.getParamDesc()+" в operday = "+operDay+" уже выполнялся или шаг "+DwhUnloadParams.UnloadOverValueStatements.getParamDesc()+" не выполнялся";
                log.error(Err);
                auditController.error( GLVD_BAL_R, Err, null, Err);
            }
        }catch(Exception e){
            dwhTools.endGlEtldwhs(id, 2);
            String Err = "Ошибка "+DwhUnloadParams.UnloadOverValueBal.getParamDesc()+" = " + e.getMessage();
            log.error(Err);
            auditController.error( GLVD_BAL_R, Err, null, e);
            e.printStackTrace();
        }
        log.info("Overvalue2dwh завершена");
        auditController.info(PSTR_ACC_R_LOAD, "Overvalue2dwh завершена");
    }

    private int unLoadChange(java.sql.Date workDay, java.sql.Date operDay, Long pdidMax)throws Exception {
        final JdbcAdapter adapter = new DefaultJdbcAdapter();
        dwhTools.insTable("insert into GLVD_PSTR_DH (PDID,GLACID,CNUM,CCY,ACOD,ACSQ,PSTA,PSTARUR,DRCR,BRCA,PREF,DLREF,OTRF,PSTB,VALB) select * from GLVD_PSTR_D");
        dwhTools.delTable("delete from GLVD_PSTR_D");
        dwhTools.insTable("insert into GLVD_PSTR_UH (PDID,GLACID,CNUM,CCY,ACOD,ACSQ,PSTA,PSTARUR,DRCR,BRCA,PREF,DLREF,OTRF,PSTB,VALB,PCID,BSAACID,EVT_ID,FCHNG,PRFCNTR,EVTP,ASOC,PNAR,SPOS,DPMT,RNARLNG) select * from GLVD_PSTR_U");
        dwhTools.delTable("delete from GLVD_PSTR_U");
        dwhTools.delTable("delete from GL_RVACUL");

//        Long pdidMax = dwhTools.getPdidMax(operDay);

        return beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
            int count = 0;
            try (PreparedStatement selPdjchgr = connection.prepareStatement(textResourceController.getContent("ru/rbt/barsgl/ejb/controller/od/gl_pdjchgr.sql"))) {
                selPdjchgr.setDate(1, operDay);
                selPdjchgr.setLong(2, pdidMax);
                selPdjchgr.setDate(3, workDay);
                try (ResultSet rsPdjchgr = selPdjchgr.executeQuery();
                     PreparedStatement insD = connection.prepareStatement(
                             //                        1    2      3    4   5    6    7    8       9    10   11   12
                             "insert into GLVD_PSTR_D (PDID,GLACID,CNUM,CCY,ACOD,ACSQ,PSTA,PSTARUR,DRCR,BRCA,OTRF,PSTB) values(?,?,?,?,?,?,?,?,?,?,?,?)"
                     );
                     PreparedStatement insU = connection.prepareStatement(
                             //                       1    2      3    4   5    6    7    8        9    10   11   12   13   14      15     16    17      18   19   20   21   22   23
                             "insert into GLVD_PSTR_U (PDID,GLACID,CNUM,CCY,ACOD,ACSQ,PSTA,PSTARUR,DRCR,BRCA,OTRF,PSTB,PCID,BSAACID,EVT_ID,FCHNG,PRFCNTR,EVTP,ASOC,PNAR,SPOS,DPMT,RNARLNG) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
                     PreparedStatement insRvacul = connection.prepareStatement("insert into gl_rvacul (BSAACID,DATE_UPL) values(?,?)");
//                     PreparedStatement selRvacul = connection.prepareStatement("select id from gl_rvacul where BSAACID = ?");
                     PreparedStatement updRvacul = connection.prepareStatement("update gl_rvacul set DATE_UPL=? where BSAACID=?");
                     PreparedStatement selPstU = connection.prepareStatement(textResourceController.getContent("ru/rbt/barsgl/ejb/controller/od/remote_ops_UR.sql"));
                     PreparedStatement selDSecond = connection.prepareStatement(textResourceController.getContent("ru/rbt/barsgl/ejb/controller/od/overvalue_ops_second.sql"));
                     PreparedStatement updPdjchgr = connection.prepareStatement("update gl_pdjchgr set unf='Y' where id=?");
//                     PreparedStatement selPdBsaacid = connection.prepareStatement("select d.bsaacid, d.pod from pd d where d.id=?");
                ) {
                    DataRecord rec1 = null, rec2 = null;
                    long pcid1 = 0,pcid2 = 0;
                    while(rsPdjchgr.next()){
                        count++;
                        if (rec1 == null){
                            rec1 = new DataRecord(rsPdjchgr, adapter);
                            pcid1 = rec1.getLong("PCID");
                            if (rsPdjchgr.next()) {
                                rec2 = new DataRecord(rsPdjchgr, adapter);
                                pcid2 = rec2.getLong("PCID");
                                count++;
                            }else break;
                        }else{
                            rec2 = new DataRecord(rsPdjchgr, adapter);
                            pcid2 = rec2.getLong("PCID");
                        }

                        if (pcid1 == pcid2) {
                            fillDRec(rec1, insD, insRvacul, updRvacul);
                            selPstU.setBigDecimal(1, DwhTools.getNumber(rec1.getLong("PDID")));
                            try(ResultSet rsPstU = selPstU.executeQuery()){
                                if (rsPstU.next()) fillURec(new DataRecord(rsPstU, adapter), insU, insRvacul, updRvacul);
                            }
                            updPdjchgr.setBigDecimal(1, DwhTools.getNumber(rec1.getLong("PDID")));
                            updPdjchgr.executeUpdate();
                            fillDRec(rec2, insD, insRvacul, updRvacul);
                            selPstU.setBigDecimal(1, DwhTools.getNumber(rec2.getLong("PDID")));
                            try(ResultSet rsPstU = selPstU.executeQuery()){
                                if (rsPstU.next()) fillURec(new DataRecord(rsPstU, adapter), insU, insRvacul, updRvacul);
                            }
//                            procAccURec( rec1, selPdBsaacid, selGlAccU, insGlAccU, updGlAccU);
//                            procAccURec( rec2, selPdBsaacid, selGlAccU, insGlAccU, updGlAccU);

                            updPdjchgr.setBigDecimal(1, DwhTools.getNumber(rec2.getLong("PDID")));
                            updPdjchgr.executeUpdate();
                            rec1 = null;
                            rec2 = null;
                            pcid1 = 0;
                            pcid2 = 0;
                        }
                        else {
                            procRecs( rec1, insD, selPstU, insU, selDSecond, /*selPdBsaacid,*/ updPdjchgr, insRvacul, updRvacul);
                            rec1 = rec2;
                            rec2 = null;
                            pcid1 = pcid2;
                            pcid2 = 0;
                        }
                    }//while
                    if ((rec2 == null) && (rec1 != null)) {
                        procRecs( rec1, insD, selPstU, insU, selDSecond, /*selPdBsaacid,*/ updPdjchgr, insRvacul, updRvacul);
                    }
                }
            }
            return count;
        }, 60 * 60);
    }

    private void procRecs(DataRecord rec1, PreparedStatement insD, PreparedStatement selPstU,
                          PreparedStatement insU, PreparedStatement selDSecond, /*PreparedStatement selPdBsaacid*/
                          PreparedStatement updPdjchgr, PreparedStatement insRvacul, PreparedStatement updRvacul) throws Exception{
        final JdbcAdapter adapter = new DefaultJdbcAdapter();
        fillDRec(rec1, insD, insRvacul, updRvacul);
        selPstU.setBigDecimal(1, DwhTools.getNumber(rec1.getLong("PDID")));
        try(ResultSet rsPstU = selPstU.executeQuery()){
            if (rsPstU.next()) fillURec(new DataRecord(rsPstU, adapter), insU, insRvacul, updRvacul);
        }
        selDSecond.setBigDecimal(1, DwhTools.getNumber(rec1.getLong("PCID")));
        selDSecond.setBigDecimal(2, DwhTools.getNumber(rec1.getLong("PDID")));
        DataRecord dr = null;
        try(ResultSet rsSecond = selDSecond.executeQuery()) {
            if (rsSecond.next()) {
                dr = new DataRecord(rsSecond, adapter);
                fillDRec(dr, insD, insRvacul, updRvacul);
                selPstU.setBigDecimal(1, DwhTools.getNumber(dr.getLong("PDID")));
                try (ResultSet rsPstU = selPstU.executeQuery()) {
                    if (rsPstU.next()) fillURec(new DataRecord(rsPstU, adapter), insU, insRvacul, updRvacul);
                }
            }else throw new Exception("Empty overvalue_ops_second.sql for pcid = "+rec1.getLong("PCID")+ "; pdid = "+rec1.getLong("PDID"));
        }

        updPdjchgr.setBigDecimal(1, DwhTools.getNumber(rec1.getLong("PDID")));
        updPdjchgr.executeUpdate();
    }

    private void fillURec(DataRecord rec, PreparedStatement insU, PreparedStatement insRvacul,
                          PreparedStatement updRvacul) throws SQLException {
        insU.setBigDecimal(1, DwhTools.getNumber(rec.getLong("PDID")));
        insU.setBigDecimal(2, DwhTools.getNumber(rec.getLong("GLACID")));
        insU.setString( 3, rec.getString("CNUM"));
        insU.setString( 4, rec.getString("CCY"));
        insU.setBigDecimal(5, DwhTools.getNumber(rec.getLong("ACOD")));
        insU.setBigDecimal(6, DwhTools.getNumber(rec.getLong("ACSQ")));
        insU.setBigDecimal(7, DwhTools.getNumber(rec.getLong("PSTA")));
        insU.setBigDecimal(8, DwhTools.getNumber(rec.getLong("PSTARUR")));
        insU.setBigDecimal(9, DwhTools.getNumber(rec.getLong("DRCR")));
        insU.setString(10, rec.getString("BRCA"));
//        insU.setString( 11, rec.getString("PREF"));
//        insU.setString( 12, rec.getString("DLREF"));
        insU.setString( 11, rec.getString("OTRF"));
        insU.setDate(12,  new java.sql.Date(rec.getDate("PSTB").getTime()));
//        insU.setDate(15,  new java.sql.Date(rec.getDate("VALB").getTime()));
        insU.setBigDecimal(13, DwhTools.getNumber(rec.getLong("PCID")));
        insU.setString( 14, rec.getString("BSAACID")==null?"": rec.getString("BSAACID"));
        insU.setString( 15, rec.getString("EVT_ID"));
        insU.setString( 16, rec.getString("FCHNG"));
        insU.setString( 17, rec.getString("PRFCNTR"));
//        insU.setString( 18, rec.getString("EVTP"));
        insU.setString( 18, null);
//        Long asoc = Long.parseLong(rec.getString("CNUM_ASOC"));
        insU.setBigDecimal(19, DwhTools.getNumber(rec.getString("CNUM_ASOC").isEmpty()?null: Long.parseLong(rec.getString("CNUM_ASOC"))));
        insU.setString(20, rec.getString("PNAR"));
        insU.setString(21, rec.getString("SPOS"));
        insU.setString(22, rec.getString("DPMT"));
        insU.setString(23, rec.getString("RNARLNG"));
        insU.executeUpdate();

        fillRvacul( insRvacul, updRvacul, rec.getString("rev_fl"), rec.getString("absaacid"), rec.getDate("PDPOD"),
                    rec.getDate("date_upl"));
//        java.util.Date podDate = rec.getDate("PDPOD");
//        if (rec.getString("absaacid") != null && rec.getString("rev_fl").equals("Y")){
//            if (rec.getDate("date_upl") == null ){
//                insRvacul.setString( 1, rec.getString("absaacid"));
//                insRvacul.setDate( 2, new java.sql.Date(podDate.getTime()));
//                insRvacul.executeUpdate();
//            }else if(podDate.before(rec.getDate("date_upl"))){
//                updRvacul.setDate( 1, new java.sql.Date(podDate.getTime()));
//                updRvacul.setString( 2, rec.getString("absaacid"));
//                updRvacul.executeUpdate();
//            }
//        }
    }

    private void fillDRec(DataRecord rec, PreparedStatement insD, PreparedStatement insRvacul,
                          PreparedStatement updRvacul) throws SQLException {

//        if ( (rec.getString("pacid") == null || rec.getString("pacid").isEmpty()) &&
//                (rec.getString("jacid") == null || rec.getString("jacid").isEmpty()) ) return;

        insD.setBigDecimal( 1, DwhTools.getNumber(rec.getLong("PDID")));
        insD.setBigDecimal( 2, DwhTools.getNumber(rec.getLong("GLACID")));
        insD.setString( 3, rec.getString("CNUM"));
        insD.setString( 4, rec.getString("CCY"));
        insD.setBigDecimal( 5, DwhTools.getNumber(rec.getLong("ACOD")));
        insD.setBigDecimal( 6, DwhTools.getNumber(rec.getLong("ACSQ")));
        insD.setBigDecimal( 7, DwhTools.getNumber(rec.getLong("PSTA")));
        insD.setBigDecimal( 8, DwhTools.getNumber(rec.getLong("PSTARUR")));
        insD.setBigDecimal( 9, DwhTools.getNumber(rec.getLong("DRCR")));
        insD.setString( 10, rec.getString("BRCA"));
//        insD.setString( 11, rec.getString("PREF"));
//        insD.setString( 12, rec.getString("DLREF"));
        insD.setString( 11, rec.getString("OTRF"));
        insD.setDate( 12, new java.sql.Date(rec.getDate("PSTB").getTime()));
//        insD.setDate( 13, new java.sql.Date(rec.getDate("VALB").getTime()));
        insD.executeUpdate();

        fillRvacul( insRvacul, updRvacul, rec.getString("rev_fl"), rec.getString("absaacid"), rec.getDate("PSTB"),
                    rec.getDate("date_upl"));
//        java.util.Date podDate = rec.getDate("PSTB");
//        if (rec.getString("absaacid") != null && rec.getString("rev_fl").equals("Y")){
//            if (rec.getDate("date_upl") == null ){
//                insRvacul.setString( 1, rec.getString("absaacid"));
//                insRvacul.setDate( 2, new java.sql.Date(podDate.getTime()));
//                insRvacul.executeUpdate();
//            }else if(podDate.before(rec.getDate("date_upl"))){
//                updRvacul.setDate( 1, new java.sql.Date(podDate.getTime()));
//                updRvacul.setString( 2, rec.getString("absaacid"));
//                updRvacul.executeUpdate();
//            }
//        }
    }

    private void fillRvacul(PreparedStatement insRvacul, PreparedStatement updRvacul,
                            String RevFl, String bsaacid, java.util.Date podDate,
                            java.util.Date dateUpl) throws SQLException {
            if (bsaacid != null && bsaacid.trim().isEmpty() && RevFl.equals("Y")){
            if (dateUpl == null ){
                insRvacul.setString( 1,bsaacid);
                insRvacul.setDate( 2, new java.sql.Date(podDate.getTime()));
                insRvacul.executeUpdate();
            }else if(podDate.before(dateUpl)){
                updRvacul.setDate( 1, new java.sql.Date(podDate.getTime()));
                updRvacul.setString( 2, bsaacid);
                updRvacul.executeUpdate();
            }
        }

//        selRvacul.setString( 1, bsaacid);
//        try(ResultSet rsRvacul = selRvacul.executeQuery()) {
//            if (rsRvacul.next()) {
//                if(podDate.before(dateUpl)) {
//                    Long id = rsRvacul.getLong(0);
//                    updRvacul.setDate(1, new java.sql.Date(podDate.getTime()));
//                    updRvacul.setLong(2, id);
//                    insRvacul.executeUpdate();
//                }
//            }else{
//                insRvacul.setString( 1, bsaacid);
//                insRvacul.setDate( 2, new java.sql.Date(podDate.getTime()));
//                insRvacul.executeUpdate();
//            }
//        }
    }

    private int unLoadBal(java.sql.Date workDay, java.sql.Date operDay)throws Exception {
        dwhTools.insTable("insert into GLVD_BAL_RH (DAT,ACID,BSAACID,GLACID,OBAL,UNLOAD_DAT,DTRN,CTRN,OBALRUR,DTRNRUR,CTRNRUR) select * from GLVD_BAL_R");
        dwhTools.delTable("delete from GLVD_BAL_R");

        return beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
            int count = 0;
            try (
                    PreparedStatement insGlvdBalR = connection.prepareStatement(
                    "insert into GLVD_BAL_R (DAT,BSAACID,GLACID,OBAL,DTRN,CTRN,UNLOAD_DAT,OBALRUR,DTRNRUR,CTRNRUR) "+
//                    "insert into GLVD_BAL_R (DAT,BSAACID,GLACID,OBAL,DTRN,CTRN,OBALRUR,DTRNRUR,CTRNRUR) "+
                    "select b.dat "+
                    ",acc.bsaacid "+
                    ",acc.id glacid "+
                    ",b.obac obal "+
                    ",abs(b.dtac) dtrn "+
                    ",b.ctac ctrn "+
                    ",'"+new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(operDay.getTime()))+ "' UNLOAD_DAT "+
                    ",b.obbc obalrur "+
                    ",abs(dtbc) dtrnrur "+
                    ",b.ctbc ctrnrur "+
                    "from baltur b, gl_acc acc, GL_RVACUL cul "+
                    "where b.bsaacid = acc.bsaacid and cul.bsaacid = b.bsaacid and cul.DATE_UPL <= dat and dat <=?");){
                    insGlvdBalR.setDate(1, workDay);
                    count = insGlvdBalR.executeUpdate();
            }
            return count;
        }, 60 * 60);
    }

    private int unLoadAcc(java.sql.Date workDay) throws Exception {
        final JdbcAdapter adapter = new DefaultJdbcAdapter();

        return beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
            int count = 0;

            try (PreparedStatement selAccrln = connection.prepareStatement(textResourceController.getContent("ru/rbt/barsgl/ejb/controller/od/accrln.sql"));
                 PreparedStatement selGlAcc = connection.prepareStatement("select 1 from gl_acc where bsaacid=? and acid=?");
                 PreparedStatement insGlAcc = connection.prepareStatement(
//                                   1       2    3     4      5   6      7       8          9    10     11   12     13   14 15   16   17      18     19        20          21  22  23  24  25       26    27     28      29       30        31
                 "insert into gl_acc(BSAACID,CBCC,CBCCN,BRANCH,CCY,CUSTNO,ACCTYPE,CBCUSTTYPE,TERM,GL_SEQ,ACC2,PLCODE,ACOD,SQ,ACID,PSAV,DEALSRS,DEALID,SUBDEALID,DESCRIPTION,DTO,DTC,DTR,DTM,OPENTYPE,GLOID,GLO_DC,RLNTYPE,ACID_DWH,REV_CCY,REV_FL) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'Y')"
                 );
                ResultSet rsAccrln = selAccrln.executeQuery()) {
                DataRecord recAccrln = null;
                while(rsAccrln.next()) {
                    count++;
                    recAccrln = new DataRecord(rsAccrln, adapter);
                    String bsaacid = recAccrln.getString("BSAACID");
                    String acid = recAccrln.getString("ACID");
                    String custno = recAccrln.getString("CUSTNO");
                    String ccy = recAccrln.getString("CCY");
                    Integer acod = mapAcod.get(recAccrln.getString("ACC2"));
                    long sq = recAccrln.getLong("SQ");
                    long branch = recAccrln.getLong("BRANCH");
                    String aciddwh = custno + ccy + String.valueOf(acod) + String.format("%02d", sq) + String.format("%03d", branch);
//                    if (acid == null || acid.isEmpty()){
//                        acid = aciddwh;
//                    }
                    selGlAcc.setString(1, bsaacid);
                    selGlAcc.setString(2, acid);
                    try(ResultSet rsGlAcc = selGlAcc.executeQuery()){
                        if (rsGlAcc.next()) continue;
                    }
//insert GL_ACC
                    insGlAcc.setString(1, bsaacid);
                    insGlAcc.setString(2, recAccrln.getString("CBCC"));
                    insGlAcc.setString(3, recAccrln.getString("CBCCN"));
                    insGlAcc.setString(4, recAccrln.getString("BRANCH"));
                    insGlAcc.setString(5, ccy);
                    insGlAcc.setString(6, custno);
                    insGlAcc.setInt(7, mapAccType.get(recAccrln.getString("ACC2")));
                    insGlAcc.setBigDecimal(8, recAccrln.getBigDecimal("CBCUSTTYPE"));
                    insGlAcc.setBigDecimal(9, null);
                    insGlAcc.setString(10, null);
                    insGlAcc.setString(11, recAccrln.getString("ACC2"));
                    insGlAcc.setString(12, recAccrln.getString("PLCODE"));
                    insGlAcc.setInt(13, acod);
                    insGlAcc.setBigDecimal(14, recAccrln.getBigDecimal("SQ"));
                    insGlAcc.setString(15, acid);
                    insGlAcc.setString(16, recAccrln.getString("PSAV"));
                    insGlAcc.setString(17, null);
                    insGlAcc.setString(18, null);
                    insGlAcc.setString(19, null);
                    insGlAcc.setString(20, null);
                    insGlAcc.setDate(21, recAccrln.getSqlDate("DTO"));
                    insGlAcc.setDate(22, recAccrln.getSqlDate("DTC"));
                    insGlAcc.setDate(23, workDay);
                    insGlAcc.setDate(24, workDay);
                    insGlAcc.setString(25, null);
                    insGlAcc.setString(26, null);
                    insGlAcc.setString(27, null);
                    insGlAcc.setString(28, null);
                    insGlAcc.setString(29, aciddwh);
                    insGlAcc.setString(30, recAccrln.getString("REV_CCY"));
                    insGlAcc.addBatch();
                    if (count % 100 == 0) {
                        insGlAcc.executeBatch();
                    }
                }
                insGlAcc.executeBatch();
            }
            return count;
        }, 60 * 60);

    }

    private boolean isMI4GLfinished(java.sql.Date workDay) throws Exception {
        return beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
            try (PreparedStatement selWorkProc = connection.prepareStatement("select RESULT from workproc where id='MI4GL' and dat=?")){
                 selWorkProc.setDate(1, workDay);
                 try(ResultSet rsWorkProc = selWorkProc.executeQuery()) {
                     if (rsWorkProc.next()) return rsWorkProc.getString("RESULT").equals("O");
                     else return false;
                 }
            }
        }, 60 * 60);
    }

    private static Map<String,Integer> mapAccType = new HashMap<String,Integer>() {
        {
            put("70603", 671010101);
            put("70608", 771010101);
            put("99996", 918010101);
            put("99997", 917010101);
            put("99998", 892030100);
            put("99999", 832010100);
        };
    };
    private static Map<String,Integer> mapAcod = new HashMap<String,Integer>() {
        {
            put("70603", 7902);
            put("70608", 7901);
            put("99996", 0111);
            put("99997", 0112);
            put("99998", 0113);
            put("99999", 0114);
        }
    };

}
