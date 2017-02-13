package ru.rbt.barsgl.ejb.controller.operday.task;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejbcore.controller.etc.TextResourceController;
import ru.rbt.barsgl.ejb.repository.WorkprocRepository;
import ru.rbt.barsgl.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.datarec.DefaultJdbcAdapter;
import ru.rbt.barsgl.ejbcore.datarec.JdbcAdapter;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.barsgl.ejbcore.util.DateUtils;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.barsgl.audit.entity.AuditRecord.LogCode.GLVD_BAL4;
import static ru.rbt.barsgl.audit.entity.AuditRecord.LogCode.GLVD_PST_DU;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.trim;

/**
 * Created by ER22317 on 18.04.2016.
 */
public class RemoteOpersTask implements ParamsAwareRunnable {
    private static final Logger log = Logger.getLogger(RemoteOpersTask.class);

    public static final String STEP_NAME_KEY = "stepName";

    @EJB
    private BeanManagedProcessor beanManagedProcessor;
    @EJB
    private AuditController auditController;
    @Inject
    private TextResourceController textResourceController;
    @Inject
    private DwhTools dwhTools;

    @Inject
    private WorkprocRepository workprocRepository;

    @Inject
    private DateUtils dateUtils;

    @Override
    public void run(String jobName, Properties properties) throws Exception {

        int count = 0;
        long id = 0l;
        java.sql.Date operDay = getOperday(properties);

        BankmatCheck bankmatCheck = checkRunUnloadBankomat(properties, operDay);
        if (BankmatCheck.BANKMAT_REQUIRED_N == bankmatCheck) {
            return;
        }
        String mess = "RemoteOpersTask запущена, operDay = " + new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(operDay.getTime()));
        log.info(mess);
        auditController.info(GLVD_PST_DU,mess);
// "GLVD_PST_DU"
        try {
            if (!dwhTools.isStepInOperDay(DwhUnloadParams.UnloadremoteChanges.getParamDesc(), operDay)) {
                id = dwhTools.startGlEtldwhs(DwhUnloadParams.UnloadremoteChanges.getParamDesc(), operDay);
                removeOldPst();
                count = fillPst();
                dwhTools.endGlEtldwhs(id, 1);
                String result = DwhUnloadParams.UnloadremoteChanges.getParamDesc() + " выполнено, обработано записей " + count;
                log.info(result);
                auditController.info(GLVD_PST_DU,result);
            }else{
                String err = DwhUnloadParams.UnloadremoteChanges.getParamDesc()+" в operDay = " + operDay + " уже выполнялся";
                log.info(err);
                auditController.error( GLVD_PST_DU, err, null, err);
                return;
            }
        } catch (Exception e){
            dwhTools.endGlEtldwhs(id, 2);
            String err = "Ошибка "+DwhUnloadParams.UnloadremoteChanges.getParamDesc()+" = " + e.getMessage();
            log.error(err);
            auditController.error( GLVD_PST_DU, err, null, e);
            e.printStackTrace();
        }
// "GLVD_BAL4"
        try {
            if (dwhTools.isStepInOperDay(DwhUnloadParams.UnloadremoteChanges.getParamDesc(), operDay) && !dwhTools.isStepInOperDay(DwhUnloadParams.UnloadremoteBal.getParamDesc(), operDay)) {
                id = dwhTools.startGlEtldwhs(DwhUnloadParams.UnloadremoteBal.getParamDesc(), operDay);
                removeOldBal();
                count = fillBal();
                dwhTools.endGlEtldwhs(id, 1);
                String result = DwhUnloadParams.UnloadremoteBal.getParamDesc()+" выполнено, обработано записей " + count;
                log.info(result);
                auditController.info(GLVD_BAL4,result);
            }else{
                String err = "Ошибка при обработке "+DwhUnloadParams.UnloadremoteChanges.getParamDesc()+ " в operday = "+operDay+" GLVD_PST_DU не выполнялся или "+DwhUnloadParams.UnloadremoteBal.getParamDesc()+" уже выполнялся";
                log.info(err);
                auditController.error( GLVD_BAL4, err, null, err);
            }
        }catch (Exception e){
            dwhTools.endGlEtldwhs(id, 2);
            String err = "Ошибка заполнения bal4 = " + e.getMessage();
            log.error(err);
            auditController.error( GLVD_BAL4, err, null, e);
            e.printStackTrace();
        }
//
        try {
            if (BankmatCheck.BANKMAT_REQUIRED_Y == bankmatCheck) {
                auditController.info(GLVD_BAL4, format("Выгружено остатков по счетам банкоматов: %s", unloadBankomat(operDay)));
            }
        } catch (Exception e) {
            auditController.error(GLVD_BAL4, "Ошибка при выгрузке остатков по счетам банкоматов", null, e);
        }
        log.info("RemoteOpersTask завершена");
        auditController.info(GLVD_PST_DU,"RemoteOpersTask завершена");
    }

    final static int SUM_OBAL = 0, SUM_DTAC = 1, SUM_CTAC = 2, SUM_OBBC = 3, SUM_DTBC = 4, SUM_CTBC = 5, SUM_BALOUT = 6, SUM_BALRUROUT = 7;
    public int fillBal() throws Exception {
        final JdbcAdapter adapter = new DefaultJdbcAdapter();
//        Calendar operCal = Calendar.getInstance();
//        Calendar endCal = Calendar.getInstance();

        return beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
            int count = 0;
            java.util.Date endDate = null, curDate = null;
            try(PreparedStatement selEndDate = connection.prepareStatement("select case when phase='ONLINE' then lwdate else curdate end enddate, curdate from gl_od");
                ResultSet rsEndDate = selEndDate.executeQuery(); ){
                rsEndDate.next();
                endDate = rsEndDate.getDate(1);
                curDate = rsEndDate.getDate(2);
            }


            try(PreparedStatement selGlAccU26 = connection.prepareStatement(textResourceController.getContent("ru/rbt/barsgl/ejb/controller/od/gl_acc_u26.sql"));
                PreparedStatement selGlAccU25 = connection.prepareStatement(textResourceController.getContent("ru/rbt/barsgl/ejb/controller/od/gl_acc_u25.sql"));
                PreparedStatement selGlAccU2_26 = connection.prepareStatement(textResourceController.getContent("ru/rbt/barsgl/ejb/controller/od/gl_acc_u2_26.sql"));
                PreparedStatement selGlAccU2_25 = connection.prepareStatement(textResourceController.getContent("ru/rbt/barsgl/ejb/controller/od/gl_acc_u2_25.sql"));
                PreparedStatement selBaltur = connection.prepareStatement("select * from baltur b where b.bsaacid = ? and b.dat <= ? and b.datto >= ?");
                PreparedStatement selAccrln = connection.prepareStatement("select bsaacid from accrln where acid=? and rlntype in ('0','1','2')");
                PreparedStatement updAccUbyId = connection.prepareStatement("update gl_acc_u set unf='Y' where id=?");
                PreparedStatement updAccU2byId = connection.prepareStatement("update gl_acc_u2 set unf='Y' where id=?");
                PreparedStatement updAccUbyAcid = connection.prepareStatement("update gl_acc_u set unf='Y' where bsaacid in (select bsaacid from accrln where acid=?)");
                PreparedStatement updAccU2byAcid = connection.prepareStatement("update gl_acc_u2 set unf='Y' where bsaacid in (select bsaacid from accrln where acid=?)");
                //  1   2    3       4      5    6    7    8          9       10      11      12     13
                PreparedStatement insBal = connection.prepareStatement("insert into GLVD_BAL4(DAT,ACID,BSAACID,GLACID,OBAL,DTRN,CTRN,UNLOAD_DAT,OBALRUR,DTRNRUR,CTRNRUR,BALOUT,BALRUROUT) values(?,?,?,?,?,?,?,?,?,?,?,?,?)")){
//25
                count += process25(selGlAccU25, updAccUbyId, selBaltur, insBal, endDate);
                count += process25(selGlAccU2_25, updAccU2byId, selBaltur, insBal, endDate);
//26
                count += process26(selGlAccU26, selAccrln, updAccUbyAcid, selBaltur, insBal, endDate);
                count += process26(selGlAccU2_26, selAccrln, updAccU2byAcid, selBaltur, insBal, endDate);

                insBal.executeBatch();
//                updAccUbyId.executeBatch();
//                updAccUbyAcid.executeBatch();
            }
            return count;
        }, 60 * 60);
    }

    private int process26(PreparedStatement selGlAccU26, PreparedStatement selAccrln,
                          PreparedStatement updAccUbyAcid, PreparedStatement selBaltur,
                          PreparedStatement insBal, java.util.Date endDate)throws Exception {
        int count = 0;
        Calendar operCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();
        long[] sum = new long[8];
        try(ResultSet rsGlAccU26 = selGlAccU26.executeQuery()) {
            while (rsGlAccU26.next()) {
                count++;
                DataRecord recGlAccU26 = new DataRecord(rsGlAccU26, adapter);
                String acid = recGlAccU26.getString("acid");
                java.util.Date begDate = recGlAccU26.getDate("date_upl");
                endCal.setTime(endDate);
                endCal.add(Calendar.DATE, 1);
                for (operCal.setTime(begDate); operCal.before(endCal); operCal.add(Calendar.DATE, 1)) {
                    selAccrln.setString(1, acid);
                    try (ResultSet rslAccrln = selAccrln.executeQuery()) {
                        for (int l = 0; l < sum.length; l++) {
                            sum[l] = 0;
                        }
                        while (rslAccrln.next()) {
                            selBaltur.setString(1, rslAccrln.getString("bsaacid"));
                            selBaltur.setDate(2, new java.sql.Date(operCal.getTimeInMillis()));
                            selBaltur.setDate(3, new java.sql.Date(operCal.getTimeInMillis()));
                            try (ResultSet rsBalture = selBaltur.executeQuery()) {
                                if (rsBalture.next()) {
                                    DataRecord recBalture = new DataRecord(rsBalture, adapter);
                                    Calendar datCal = Calendar.getInstance();
                                    datCal.setTime(recBalture.getDate("dat"));
                                    if (operCal.compareTo(datCal) == 0) {
                                        sum[SUM_OBAL] += recBalture.getLong("obac");
                                        sum[SUM_DTAC] += recBalture.getLong("dtac");
                                        sum[SUM_CTAC] += recBalture.getLong("ctac");
                                        sum[SUM_OBBC] += recBalture.getLong("obbc");
                                        sum[SUM_DTBC] += recBalture.getLong("dtbc");
                                        sum[SUM_CTBC] += recBalture.getLong("ctbc");
                                    } else {
                                        sum[SUM_OBAL] += recBalture.getLong("obac") +
                                                recBalture.getLong("dtac") +
                                                recBalture.getLong("ctac");
                                        sum[SUM_OBBC] += recBalture.getLong("obbc") +
                                                recBalture.getLong("dtbc") +
                                                recBalture.getLong("ctbc");
                                    }
                                    sum[SUM_BALOUT] += recBalture.getLong("obac") +
                                            recBalture.getLong("dtac") +
                                            recBalture.getLong("ctac");
                                    sum[SUM_BALRUROUT] += recBalture.getLong("obbc") +
                                            recBalture.getLong("dtbc") +
                                            recBalture.getLong("ctbc");
                                }
                            }
                        }
                    }catch(Exception e){
                        auditController.error( GLVD_BAL4, "error in ACID = " + acid , null, e);
                    }
                    insBal4rec(operCal, acid, "", 0, sum[SUM_OBAL], sum[SUM_DTAC], sum[SUM_CTAC],
                            sum[SUM_OBBC], sum[SUM_DTBC], sum[SUM_CTBC],
                            sum[SUM_BALOUT], sum[SUM_BALRUROUT], insBal, count);
                }
                updAccUbyAcid.setString(1, acid);
                updAccUbyAcid.addBatch();
                if (count % 100 == 0) {
                    updAccUbyAcid.executeBatch();
                }

            }
        }
        updAccUbyAcid.executeBatch();
        return count;
    }

    private int process25(PreparedStatement selGlAccU25, PreparedStatement updAccUbyId,
                          PreparedStatement selBaltur, PreparedStatement insBal,
                          java.util.Date endDate) throws Exception {
        int count = 0;
        Calendar operCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();
        try(ResultSet rsGlAccU25 = selGlAccU25.executeQuery()) {
            while (rsGlAccU25.next()) {
                count++;
                DataRecord recGlAccU25 = new DataRecord(rsGlAccU25, adapter);
                String bsaacid = recGlAccU25.getString("bsaacid");
                Long glacid = recGlAccU25.getLong("glacid");
                Long uid = recGlAccU25.getLong("uid");
                java.util.Date begDate = recGlAccU25.getDate("date_upl");
                endCal.setTime(endDate);
                endCal.add(Calendar.DATE, 1);
                for (operCal.setTime(begDate); operCal.before(endCal); operCal.add(Calendar.DATE, 1)) {
                    selBaltur.setString(1, bsaacid);
                    selBaltur.setDate(2, new java.sql.Date(operCal.getTimeInMillis()));
                    selBaltur.setDate(3, new java.sql.Date(operCal.getTimeInMillis()));
                    try (ResultSet rsBalture = selBaltur.executeQuery()) {
                        if (rsBalture.next()) {
                            DataRecord recBalture = new DataRecord(rsBalture, adapter);
                            Calendar datCal = Calendar.getInstance();
                            datCal.setTime(recBalture.getDate("dat"));
                            Long vBalOut = getNullLong(recBalture.getLong("obac")) +
                                    getNullLong(recBalture.getLong("dtac")) +
                                    getNullLong(recBalture.getLong("ctac"));
                            Long vBalRurOut = getNullLong(recBalture.getLong("obbc")) +
                                    getNullLong(recBalture.getLong("dtbc")) +
                                    getNullLong(recBalture.getLong("ctbc"));
                            if (operCal.compareTo(datCal) == 0) {
                                insBal4rec(operCal, recBalture.getString("ACID"), recBalture.getString("BSAACID"),
                                        glacid, getNullLong(recBalture.getLong("OBAC")),
                                        getNullLong(recBalture.getLong("dtac")),
                                        getNullLong(recBalture.getLong("ctac")),
                                        getNullLong(recBalture.getLong("obbc")),
                                        getNullLong(recBalture.getLong("dtbc")),
                                        getNullLong(recBalture.getLong("ctbc")),
                                        vBalOut, vBalRurOut, insBal, count);
                            } else {
                                insBal4rec(operCal, recBalture.getString("ACID"), recBalture.getString("BSAACID"),
                                        glacid, vBalOut, 0, 0, vBalRurOut, 0, 0,
                                        vBalOut, vBalRurOut, insBal, count);
                            }
                        }
                    }
                }
                updAccUbyId.setLong(1, uid);
                updAccUbyId.addBatch();
                if (count % 100 == 0) {
                    updAccUbyId.executeBatch();
                }
            }
        }
        updAccUbyId.executeBatch();
      return count;
    }

    private void insBal4rec(Calendar operCal, String acid, String bsaacid, long glacid,
                            long obal, long dtac, long ctac, long obbc, long dtbc, long ctbc,
                            long vBalOut, long vBalRurOut, PreparedStatement insBal, int count)throws Exception{
        insBal.setDate( 1, new java.sql.Date(operCal.getTimeInMillis()));
        insBal.setString( 2, acid);
        insBal.setString( 3, bsaacid);
        insBal.setLong( 4, glacid);
        insBal.setLong( 5, obal);
        insBal.setLong( 6, Math.abs(dtac));
        insBal.setLong( 7, ctac);
        insBal.setDate( 8, new java.sql.Date(new java.util.Date().getTime()));
        insBal.setLong( 9, obbc);
        insBal.setLong( 10, Math.abs(dtbc));
        insBal.setLong(11, ctbc);
        insBal.setLong( 12, vBalOut);
        insBal.setLong( 13, vBalRurOut);
        insBal.addBatch();
        if (count % 100 == 0) {
            insBal.executeBatch();
        }
    }

    public void removeOldBal() throws Exception {
        beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
            try (PreparedStatement ins = connection.prepareStatement(
                    "insert into GLVD_BAL4H(DAT,ACID,BSAACID,GLACID,OBAL,DTRN,CTRN,UNLOAD_DAT,OBALRUR,DTRNRUR,CTRNRUR,BALOUT,BALRUROUT) select * from GLVD_BAL4")) {
                ins.executeUpdate();
            }
            try (PreparedStatement del = connection.prepareStatement("delete from GLVD_BAL4")) {
                del.executeUpdate();
            }
            return null;
        }, 60 * 60);
    }

    private static Long getNullLong(Long f){return f==null? Long.valueOf(0): f;
    }

    final JdbcAdapter adapter = new DefaultJdbcAdapter();
    public int fillPst() throws Exception {
        return beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
            int count = 1;
            DataRecord rec = null;
            long curPcid = 0;
            try (PreparedStatement sel = connection.prepareStatement(textResourceController.getContent("ru/rbt/barsgl/ejb/controller/od/gl_pdjchg.sql"));
                 PreparedStatement selDSecond = connection.prepareStatement(textResourceController.getContent("ru/rbt/barsgl/ejb/controller/od/remote_ops_second.sql"));
                 PreparedStatement selPstU = connection.prepareStatement(textResourceController.getContent("ru/rbt/barsgl/ejb/controller/od/remote_ops_U.sql"));
                 PreparedStatement updPdjchg = connection.prepareStatement("update gl_pdjchg set unf='Y' where id=?");
                 PreparedStatement insD = connection.prepareStatement(
                   //                            1    2      3    4   5    6    7    8       9    10   11   12    13   14   15
                   "insert into GLVD_PST_D (PDID,GLACID,CNUM,CCY,ACOD,ACSQ,PSTA,PSTARUR,DRCR,BRCA,PREF,DLREF,OTRF,PSTB,VALB) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
                 );
                 PreparedStatement insU = connection.prepareStatement(
                   //                       1    2      3    4   5    6    7    8       9    10   11   12    13   14   15   16   17      18     19    20      21   22   23   24   25   26  27    28  29      30
                   "insert into GLVD_PST_U (PDID,GLACID,CNUM,CCY,ACOD,ACSQ,PSTA,PSTARUR,DRCR,BRCA,PREF,DLREF,OTRF,PSTB,VALB,PCID,BSAACID,EVT_ID,FCHNG,PRFCNTR,EVTP,ASOC,PNAR,SPOS,DPMT,RNARLNG,MO_NO,FAN,FAN_CCY,FAN_AMT) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
                 PreparedStatement insGlAccU = connection.prepareStatement(
                   //                           1     2        3
                   "insert into GL_ACC_U (BSAACID,Date_UPL) values(?,?)");
                 PreparedStatement updGlAccU = connection.prepareStatement("update GL_ACC_U set Date_UPL=? where BSAACID=?");
                 PreparedStatement selGlAccU = connection.prepareStatement("select Date_UPL from GL_ACC_U where BSAACID=?");
                 PreparedStatement selPdBsaacid = connection.prepareStatement("select d.bsaacid, d.pod from pd d where d.id=?");
                 PreparedStatement selAsocPstU = connection.prepareStatement("select pcid, max(cnum) cnum from GLVD_PST_U where not exists (SELECT 1 FROM DWH.IMBCBBRP I WHERE I.A8BICN=CNUM) group by pcid");
                 PreparedStatement updGlvdPstU = connection.prepareStatement("update GLVD_PST_U set ASOC=? where pcid = ?");
                 PreparedStatement selGlvdPstD = connection.prepareStatement("select 1 from GLVD_PST_D where PDID=?");
                 PreparedStatement selGlvdPstU = connection.prepareStatement("select 1 from GLVD_PST_U where PDID=?");
                         ResultSet rs = sel.executeQuery()){
//                long pcid1 = 0,pcid2 = 0;
                ArrayList<DataRecord> like = new ArrayList<DataRecord>();
                if (rs.next()){
                    rec = new DataRecord(rs, adapter);
                    curPcid = rec.getLong("PCID");
                    like.add(rec);
                }

                while(rs.next()){
                    count++;
                    rec = new DataRecord(rs, adapter);
                    if (curPcid != rec.getLong("PCID")){
                       insLike(like, updPdjchg, insD, selPstU, insU, selPdBsaacid, selGlAccU, insGlAccU, updGlAccU, selDSecond, selGlvdPstD, selGlvdPstU);
                       curPcid = rec.getLong("PCID");
                       like.add(rec);
                    }
                    else like.add(rec);

//                    if (rec1 == null){
//                        rec1 = new DataRecord(rs, adapter);
//                        pcid1 = rec1.getLong("PCID");
//                        if (rs.next()) {
//                            rec2 = new DataRecord(rs, adapter);
//                            pcid2 = rec2.getLong("PCID");
//                            count++;
//                        }else break;
//                    }else{
//                        rec2 = new DataRecord(rs, adapter);
//                        pcid2 = rec2.getLong("PCID");
//                    }
//
//                    if (pcid1 == pcid2) {
//                        fillDRec(rec1, insD);
//                        selPstU.setBigDecimal(1, DwhTools.getNumber(rec1.getLong("PDID")));
//                        try (ResultSet rsPstU = selPstU.executeQuery()) {
//                          if (rsPstU.next()) fillURec(new DataRecord(rsPstU, adapter), insU);
//                        }
//                        updPdjchg.setBigDecimal(1, DwhTools.getNumber(rec1.getLong("PDID")));
//                        updPdjchg.executeUpdate();

//                        fillDRec(rec2, insD);
//                        selPstU.setBigDecimal(1, DwhTools.getNumber(rec2.getLong("PDID")));
//                        try (ResultSet rsPstU = selPstU.executeQuery()) {
//                           if (rsPstU.next()) fillURec(new DataRecord(rsPstU, adapter), insU);
//                        }
//                        procAccURec( rec1, selPdBsaacid, selGlAccU, insGlAccU, updGlAccU);
//                        procAccURec( rec2, selPdBsaacid, selGlAccU, insGlAccU, updGlAccU);
//
//                        updPdjchg.setBigDecimal(1, DwhTools.getNumber(rec2.getLong("PDID")));
//                        updPdjchg.executeUpdate();
//                        rec1 = null;
//                        rec2 = null;
//                        pcid1 = 0;
//                        pcid2 = 0;
//                    }
//                    else {
//                        procRecs( rec1, insD, selPstU, insU, selDSecond, selPdBsaacid, selGlAccU, insGlAccU, updGlAccU, updPdjchg);
//                        rec1 = rec2;
//                        rec2 = null;
//                        pcid1 = pcid2;
//                        pcid2 = 0;
//                    }
                }//while
//                if ((rec2 == null) && (rec1 != null)) {
//                    procRecs( rec1, insD, selPstU, insU, selDSecond, selPdBsaacid, selGlAccU, insGlAccU, updGlAccU, updPdjchg);
//                }
                insLike(like, updPdjchg, insD, selPstU, insU, selPdBsaacid, selGlAccU, insGlAccU, updGlAccU, selDSecond, selGlvdPstD, selGlvdPstU);
                //set ASOC in GLVD_PST_U
                fillAsoc2PstU(selAsocPstU, updGlvdPstU);
            }catch(Exception e) {
                auditController.info(GLVD_PST_DU, "Сломалось на PCID = " + curPcid + "; PDID = "+(rec != null?rec.getLong("PDID"):"пусто"));
             throw new Exception(e);
            }
            return count;
        }, 60 * 60);
    }

    private void insLike(ArrayList<DataRecord> Recs, PreparedStatement updPdjchg, PreparedStatement insD,
                         PreparedStatement selPstU, PreparedStatement insU, PreparedStatement selPdBsaacid,
                         PreparedStatement selGlAccU, PreparedStatement insGlAccU, PreparedStatement updGlAccU,
                         PreparedStatement selDSecond, PreparedStatement selGlvdPstD, PreparedStatement selGlvdPstU) throws Exception{

        if (Recs.isEmpty()) return;

        if (Recs.size() > 1){
          for(DataRecord rec: Recs){
             fillDRec(rec, insD, selGlvdPstD);
             selPstU.setBigDecimal(1, DwhTools.getNumber(rec.getLong("PDID")));
             try (ResultSet rsPstU = selPstU.executeQuery()) {
                if (rsPstU.next()) fillURec(new DataRecord(rsPstU, adapter), insU, selGlvdPstU);
             }
             procAccURec( rec, selPdBsaacid, selGlAccU, insGlAccU, updGlAccU);
             updPdjchg.setBigDecimal(1, DwhTools.getNumber(rec.getLong("PDID")));
             updPdjchg.executeUpdate();
          }
        }else{
            procRecs( Recs.get(0), insD, selPstU, insU, selDSecond, selPdBsaacid, selGlAccU, insGlAccU, updGlAccU, updPdjchg, selGlvdPstD, selGlvdPstU);
        }

        Recs.clear();
    }

    private void fillAsoc2PstU(PreparedStatement selAsocPstU, PreparedStatement updGlvdPstU) throws Exception{
        final JdbcAdapter adapter = new DefaultJdbcAdapter();
        try(ResultSet rsAsocPst = selAsocPstU.executeQuery()){
            while(rsAsocPst.next()){
                DataRecord rec = new DataRecord(rsAsocPst, adapter);
                String cnum = trim(rec.getString("CNUM"));
                if (cnum != null && !cnum.isEmpty()) {
                    updGlvdPstU.setString( 1, cnum.substring(2, 8));
                    updGlvdPstU.setLong(2, rec.getLong("PCID"));
                    updGlvdPstU.executeUpdate();
                }
            }
        }
    }

    private void procAccURec(DataRecord rec, PreparedStatement selPdBsaacid, PreparedStatement selGlAccU,
                             PreparedStatement insGlAccU, PreparedStatement updGlAccU)throws Exception{
        final JdbcAdapter adapter = new DefaultJdbcAdapter();

        if ( (rec.getString("pacid") == null || rec.getString("pacid").isEmpty()) &&
             (rec.getString("jacid") == null || rec.getString("jacid").isEmpty()) ) return;

        selPdBsaacid.setBigDecimal(1, DwhTools.getNumber(rec.getLong("PDID")));
        try(ResultSet rsPdBsaacid = selPdBsaacid.executeQuery()){
            if (rsPdBsaacid.next()) {
                DataRecord pd = new DataRecord(rsPdBsaacid, adapter);
                if (!pd.getString("BSAACID").equals(rec.getString("BSAACID"))) {
                    fillAccURec(selGlAccU, pd, insGlAccU, updGlAccU);
                    fillAccURec(selGlAccU, rec, insGlAccU, updGlAccU);
                }else{
                    if (pd.getDate("POD").before(rec.getDate("POD"))) fillAccURec(selGlAccU, pd, insGlAccU, updGlAccU);
                    else fillAccURec(selGlAccU, rec, insGlAccU, updGlAccU);
                }

            }
//            else throw new Exception("Not found PD.Bsaacid for pd.id = "+rec.getLong("PDID"));
        }
    }

    private void procRecs(DataRecord rec1, PreparedStatement insD, PreparedStatement selPstU,
                          PreparedStatement insU, PreparedStatement selDSecond, PreparedStatement selPdBsaacid,
                          PreparedStatement selGlAccU, PreparedStatement insGlAccU, PreparedStatement updGlAccU,
                          PreparedStatement updPdjchg, PreparedStatement selGlvdPstD, PreparedStatement selGlvdPstU) throws Exception{
        final JdbcAdapter adapter = new DefaultJdbcAdapter();
        fillDRec(rec1, insD, selGlvdPstD);
        selPstU.setBigDecimal(1, DwhTools.getNumber(rec1.getLong("PDID")));
        try(ResultSet rsPstU = selPstU.executeQuery()){
            if (rsPstU.next()) fillURec(new DataRecord(rsPstU, adapter), insU, selGlvdPstU);
        }
        selDSecond.setBigDecimal(1, DwhTools.getNumber(rec1.getLong("PCID")));
        selDSecond.setBigDecimal(2, DwhTools.getNumber(rec1.getLong("PDID")));
        DataRecord dr = null;
        try(ResultSet rsSecond = selDSecond.executeQuery()) {
            if (rsSecond.next()) {
                dr = new DataRecord(rsSecond, adapter);
                fillDRec(dr, insD, selGlvdPstD);
                selPstU.setBigDecimal(1, DwhTools.getNumber(dr.getLong("PDID")));
                try (ResultSet rsPstU = selPstU.executeQuery()) {
                    if (rsPstU.next()) fillURec(new DataRecord(rsPstU, adapter), insU, selGlvdPstU);
                }
            }
//            else throw new Exception("Empty remote_ops_second.sql for pcid = "+rec1.getLong("PCID"));
        }
        procAccURec( rec1, selPdBsaacid, selGlAccU, insGlAccU, updGlAccU);
        if (dr != null)
            procAccURec( dr, selPdBsaacid, selGlAccU, insGlAccU, updGlAccU);

        updPdjchg.setBigDecimal(1, DwhTools.getNumber(rec1.getLong("PDID")));
        updPdjchg.executeUpdate();
    }

    private void fillAccURec(PreparedStatement selGlAccU, DataRecord rec, PreparedStatement insGlAccU, PreparedStatement updGlAccU)throws SQLException{
        selGlAccU.setString(1, rec.getString("BSAACID"));
//        java.util.Date dPod = rec.getDate("POD");
        try(ResultSet rsAccUDate = selGlAccU.executeQuery()){
            if (!rsAccUDate.next()){
                insGlAccU.setString(1,rec.getString("BSAACID"));
                insGlAccU.setDate(2, new java.sql.Date(rec.getDate("POD").getTime()));
                insGlAccU.executeUpdate();
            }else{
//                java.util.Date dUpl = rsAccUDate.getDate("DATE_UPL");
                if (rsAccUDate.getDate("DATE_UPL").after(rec.getDate("POD"))){
                    updGlAccU.setDate(1,new java.sql.Date(rec.getDate("POD").getTime()));
                    updGlAccU.setString(2,rec.getString("BSAACID"));
                    updGlAccU.executeUpdate();
                }
            }
        }
    }

    private void fillURec(DataRecord rec, PreparedStatement insU, PreparedStatement selGlvdPstU) throws SQLException {
        BigDecimal pdid = DwhTools.getNumber(rec.getLong("PDID"));

        selGlvdPstU.setBigDecimal(1, pdid);
        try(ResultSet rsSelGlvdPstU = selGlvdPstU.executeQuery()){
            if (rsSelGlvdPstU.next()) return;
        }

        insU.setBigDecimal(1, pdid);
        insU.setBigDecimal(2, DwhTools.getNumber(rec.getLong("GLACID")));
//        String cnum = rec.getString("CNUM");
        Long asoc = Long.parseLong(rec.getString("CNUM_ASOC"));
        insU.setString( 3, rec.getString("CNUM"));
//        if (cnum.equals(asoc)) insU.setString( 3, cnum);
//        else insU.setString( 3, asoc);
        insU.setString( 4, rec.getString("CCY"));
        insU.setBigDecimal(5, DwhTools.getNumber(rec.getLong("ACOD")));
        insU.setBigDecimal(6, DwhTools.getNumber(rec.getLong("ACSQ")));
        insU.setBigDecimal(7, DwhTools.getNumber(rec.getLong("PSTA")));
        insU.setBigDecimal(8, DwhTools.getNumber(rec.getLong("PSTARUR")));
        insU.setBigDecimal(9, DwhTools.getNumber(rec.getLong("DRCR")));
        insU.setString(10, rec.getString("BRCA"));
        insU.setString( 11, rec.getString("PREF"));
        insU.setString( 12, rec.getString("DLREF"));
        insU.setString( 13, rec.getString("OTRF"));
        insU.setDate(14,  new java.sql.Date(rec.getDate("PSTB").getTime()));
        insU.setDate(15,  new java.sql.Date(rec.getDate("VALB").getTime()));
        insU.setBigDecimal(16, DwhTools.getNumber(rec.getLong("PCID")));
        insU.setString(17, rec.getString("BSAACID")==null?"": rec.getString("BSAACID"));
        insU.setString( 18, rec.getString("EVT_ID"));
        insU.setString( 19, rec.getString("FCHNG"));
        insU.setString( 20, rec.getString("PRFCNTR"));
        insU.setString( 21, rec.getString("EVTP"));
        insU.setBigDecimal(22, DwhTools.getNumber(asoc));
        insU.setString(23, rec.getString("PNAR"));
        insU.setString(24, rec.getString("SPOS"));
        insU.setString(25, rec.getString("DPMT"));
        insU.setString(26, rec.getString("RNARLNG"));
        insU.setString(27, rec.getString("MO_NO"));
        insU.setString(28, rec.getString("FAN"));
        insU.setString(29, rec.getString("FAN_CCY"));
        insU.setBigDecimal(30, DwhTools.getNumber(rec.getLong("FAN_AMT")));
        insU.executeUpdate();
    }

    private void fillDRec(DataRecord rec, PreparedStatement insD, PreparedStatement selGlvdPstD) throws SQLException {

        if ( (rec.getString("pacid") == null || rec.getString("pacid").isEmpty()) &&
             (rec.getString("jacid") == null || rec.getString("jacid").isEmpty()) ) return;

        //если уже есть, не вставлять
        BigDecimal pdid = DwhTools.getNumber(rec.getLong("PDID"));
        selGlvdPstD.setBigDecimal(1, pdid);
        try(ResultSet rsSelD = selGlvdPstD.executeQuery()){
          if (rsSelD.next()) return;
        }

        insD.setBigDecimal(1, pdid);
        insD.setBigDecimal(2, DwhTools.getNumber(rec.getLong("GLACID")));
        insD.setString( 3, rec.getString("CNUM"));
        insD.setString( 4, rec.getString("CCY"));
        insD.setBigDecimal(5, DwhTools.getNumber(rec.getLong("ACOD")));
        insD.setBigDecimal(6, DwhTools.getNumber(rec.getLong("ACSQ")));
        insD.setBigDecimal(7, DwhTools.getNumber(rec.getLong("PSTA")));
        insD.setBigDecimal(8, DwhTools.getNumber(rec.getLong("PSTARUR")));
        insD.setBigDecimal(9, DwhTools.getNumber(rec.getLong("DRCR")));
        insD.setString(10, rec.getString("BRCA"));
        insD.setString( 11, rec.getString("PREF"));
        insD.setString( 12, rec.getString("DLREF"));
        insD.setString( 13, rec.getString("OTRF"));
        insD.setDate(14,  new java.sql.Date(rec.getDate("PSTB").getTime()));
        insD.setDate(15,  new java.sql.Date(rec.getDate("VALB").getTime()));
        insD.executeUpdate();
    }

    public void removeOldPst() throws Exception {
        beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
            try (PreparedStatement ins = connection.prepareStatement(
                    "insert into GLVD_PST_DH (PDID,GLACID,CNUM,CCY,ACOD,ACSQ,PSTA,PSTARUR,DRCR,BRCA,PREF,DLREF,OTRF,PSTB,VALB) select * from GLVD_PST_D")){
                ins.executeUpdate();
            }
            try (PreparedStatement del = connection.prepareStatement("delete from GLVD_PST_D")){
                del.executeUpdate();
            }
            return null;
        }, 60 * 60);

        beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
            try (PreparedStatement ins = connection.prepareStatement(
                    "insert into GLVD_PST_UH (PDID,GLACID,CNUM,CCY,ACOD,ACSQ,PSTA,PSTARUR,DRCR,BRCA,PREF,DLREF,OTRF,PSTB,VALB,PCID,BSAACID,EVT_ID,FCHNG,PRFCNTR,EVTP,ASOC,PNAR,SPOS,DPMT,RNARLNG,MO_NO,FAN,FAN_CCY,FAN_AMT) select * from GLVD_PST_U")){
                ins.executeUpdate();
            }
            try (PreparedStatement del = connection.prepareStatement("delete from GLVD_PST_U")){
                del.executeUpdate();
            }
            return null;
        }, 60 * 60);

        beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
            try (PreparedStatement del = connection.prepareStatement("delete from GL_ACC_U")){
                del.executeUpdate();
            }
            return null;
        }, 60 * 60);

    }
    
    private int unloadBankomat(Date operday) throws Exception {
        return beanManagedProcessor.executeInNewTxWithDefaultTimeout(((persistence, connection) -> {
            String sql = textResourceController
                    .getContent("ru/rbt/barsgl/ejb/controller/operday/task/remote_cassa.sql").replace("$1", dateUtils.dbDateString(operday));

            return workprocRepository.executeNativeUpdate(sql);
        }));
    }

    public BankmatCheck checkRunUnloadBankomat(Properties properties, Date operday) throws SQLException {
        if (TaskUtils.getCheckRun(properties, false)) {
//            String message = TaskUtils.getStringValue(properties, STEP_NAME_KEY, );
            if (!workprocRepository.isStepOK("MI3GL", operday)) {
                auditController.warning(GLVD_BAL4, format("Запуск выгрузки остатков 20208 (GLVD_BAL4) невозможен: не завершен шаг '%s'", "MI3GL"), null, "");
                return BankmatCheck.BANKMAT_REQUIRED_N;
            } else {
                return BankmatCheck.BANKMAT_REQUIRED_Y;
            }
        } else {
            return BankmatCheck.BANKMAT_NOT_NEEDED;
        }
    }

    private java.sql.Date getOperday(Properties properties) throws Exception {
        java.sql.Date operDay;
        String od = properties.getProperty("OperDay");
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
        return operDay;
    }

    /**
     * как выгружать с банкомати или без них
     * выгрузка с банкоматами нужна (проверка прошла)
     * выгрузка с банкоматами нужна (проверка не прошла)
     * выгрузка с банкоматами не нужна
     */
    public enum BankmatCheck {
        /**
         * выгрузка с банкоматами нужна (проверка прошла)
         */
        BANKMAT_REQUIRED_Y
        /**
         * выгрузка с банкоматами нужна (проверка не прошла)
         */
        , BANKMAT_REQUIRED_N
        /**
         * выгрузка с банкоматами не нужна
         */
        , BANKMAT_NOT_NEEDED
    }

}
