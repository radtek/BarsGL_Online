package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.rbt.barsgl.ejb.bt.BalturRecalculator;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.cob.CobStatService;
import ru.rbt.barsgl.ejb.controller.operday.task.ExecutePreCOBTaskNew;
import ru.rbt.barsgl.ejb.controller.operday.task.PreCobBatchPostingTask;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLManualOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.bg.BatchPackageController;
import ru.rbt.barsgl.ejb.integr.bg.ManualOperationController;
import ru.rbt.barsgl.ejb.integr.oper.BatchPostingProcessor;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.cob.CobWrapper;
import ru.rbt.barsgl.shared.enums.*;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.datarec.DataRecordUtils;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.util.StringUtils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static ru.rbt.barsgl.ejb.common.CommonConstants.ETL_MONITOR_TASK;
import static ru.rbt.barsgl.ejbtest.BatchMessageIT.exampleBatchDateStr;
import static ru.rbt.barsgl.ejbtest.BatchMessageIT.loadPackage;
import static ru.rbt.barsgl.ejbtest.OperdayIT.shutdownJob;

/**
 * Created by ER18837 on 04.07.16.
 */
@SuppressWarnings("ALL")
public class ManualOperationPreCobIT extends AbstractTimerJobIT {

    private final Long USER_ID = 1L;

    @Before
    public  void before() {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
    }

    @Test
    public void testProcessPostings() throws SQLException {

        shutdownJob(ETL_MONITOR_TASK);

        // 1.CONTROL
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20202810_0001%1");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810_0001%2");
        ManualOperationWrapper wrapper1 = newOperationWrapper("А",
                "MOS", bsaDt, "RUR", new BigDecimal("12.056"),
                "MOS", bsaCt, "RUR", new BigDecimal("12.056")
        );

        BatchPosting posting = ManualOperationIT.createAuthorizedPosting(wrapper1, USER_ID, BatchPostStatus.CONTROL);
        Assert.assertNotNull(posting);
        wrapper1.setId(posting.getId());
        wrapper1.setStatus(posting.getStatus());

        // 2.CONTROL -> WORKING
        String bsaDt2 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20202810_0001%3");
        String bsaCt2 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810_0001%4");
        ManualOperationWrapper wrapper2 = newOperationWrapper("А",
                "MOS", bsaDt, "RUR", new BigDecimal("13.056"),
                "MOS", bsaCt, "RUR", new BigDecimal("13.056")
        );
        posting = ManualOperationIT.createAuthorizedPosting(wrapper2, USER_ID, BatchPostStatus.WORKING);
        Assert.assertNotNull(posting);
        wrapper2.setId(posting.getId());
        wrapper2.setStatus(posting.getStatus());

        // 3.SIGNEDDATE
        String bsaDt3 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20202810_0001%5");
        String bsaCt3 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810_0001%6");
        ManualOperationWrapper wrapper3 = newOperationWrapper("А",
                "MOS", bsaDt, "RUR", new BigDecimal("14.056"),
                "MOS", bsaCt, "RUR", new BigDecimal("14.056")
        );
        wrapper3.setPostDateStr(new SimpleDateFormat(wrapper3.dateFormat).format(getOperday().getLastWorkingDay()));
        wrapper3.setValueDateStr(wrapper3.getPostDateStr());

        posting = ManualOperationIT.createAuthorizedPosting(wrapper3, USER_ID, BatchPostStatus.SIGNEDDATE);
        Assert.assertNotNull(posting);
        wrapper3.setId(posting.getId());
        wrapper3.setStatus(posting.getStatus());

        // 4.COMPLETED -> WORKING
        String bsaDt4 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20202810_0001%7");
        String bsaCt4 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810_0001%8");
        ManualOperationWrapper wrapper4 = newOperationWrapper("А",
                "MOS", bsaDt, "RUR", new BigDecimal("15.056"),
                "MOS", bsaCt, "RUR", new BigDecimal("15.056")
        );

        posting = ManualOperationIT.createAuthorizedPosting(wrapper4, USER_ID, BatchPostStatus.SIGNED);
        Assert.assertNotNull(posting);
        wrapper4.setId(posting.getId());
        wrapper4.setStatus(posting.getStatus());
        GLManualOperation operation = remoteAccess.invoke(ManualOperationController.class, "processPosting", posting, false);
        Assert.assertNotNull(operation);
        baseEntityRepository.executeNativeUpdate("update GL_BATPST set STATE = ? where ID = ?", BatchPostStatus.WORKING.name(), wrapper4.getId());

        // 5.WAITDATE - ручной
        String bsaDt5 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20202810_0001%9");
        String bsaCt5 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810_0001%0");
        ManualOperationWrapper wrapper5 = newOperationWrapper("А",
                "MOS", bsaDt, "RUR", new BigDecimal("16.056"),
                "MOS", bsaCt, "RUR", new BigDecimal("16.056")
        );
        wrapper5.setPostDateStr(new SimpleDateFormat(wrapper5.dateFormat).format(getOperday().getLastWorkingDay()));
        wrapper5.setValueDateStr(wrapper5.getPostDateStr());

        posting = ManualOperationIT.createAuthorizedPosting(wrapper5, USER_ID, BatchPostStatus.WAITDATE);
        Assert.assertNotNull(posting);
        wrapper5.setId(posting.getId());
        wrapper5.setStatus(posting.getStatus());

        // 6.CONTROL -> WAITSRV
        String bsaDt6 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20202810_0001%3");
        String bsaCt6 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810_0001%5");
        ManualOperationWrapper wrapper6 = newOperationWrapper("А",
                "MOS", bsaDt, "RUR", new BigDecimal("17.056"),
                "MOS", bsaCt, "RUR", new BigDecimal("17.056")
        );
        posting = ManualOperationIT.createAuthorizedPosting(wrapper2, USER_ID, BatchPostStatus.WAITSRV);
        Assert.assertNotNull(posting);
        wrapper6.setId(posting.getId());
        wrapper6.setStatus(posting.getStatus());
        baseEntityRepository.executeNativeUpdate("update GL_BATPST set SRV_REF = 'TEST', SEND_SRV = systimestamp where ID = ?", wrapper6.getId());

        // проверить статусы
        BatchPosting posting1 = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper1.getId());
        Assert.assertEquals(BatchPostStatus.CONTROL, posting1.getStatus());
        BatchPosting posting2 = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper2.getId());
        Assert.assertEquals(BatchPostStatus.WORKING, posting2.getStatus());
        BatchPosting posting3 = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper3.getId());
        Assert.assertEquals(BatchPostStatus.SIGNEDDATE, posting3.getStatus());
        BatchPosting posting4 = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper4.getId());
        Assert.assertEquals(BatchPostStatus.WORKING, posting4.getStatus());
        BatchPosting posting5 = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper5.getId());
        Assert.assertEquals(BatchPostStatus.WAITDATE, posting5.getStatus());
        BatchPosting posting6 = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper6.getId());
        Assert.assertEquals(BatchPostStatus.WAITSRV, posting6.getStatus());

        // запустить обработку PreCob
        RpcRes_Base<CobWrapper> res = remoteAccess.invoke(CobStatService.class, "calculateCob");
        Assert.assertFalse(res.isError());
        CobWrapper wrapper = res.getResult();
        Assert.assertTrue(wrapper.getIdCob() > 0);
        
        remoteAccess.invoke(ExecutePreCOBTaskNew.class, "processUnprocessedBatchPostings", getOperday(), wrapper.getIdCob(), CobPhase.CobManualProc);

        // проверить статусы и видимость снова
        posting1 = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper1.getId());
        Assert.assertEquals(InvisibleType.S, posting1.getInvisible());
        posting2 = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper2.getId());
        Assert.assertEquals(InvisibleType.S, posting2.getInvisible());
        posting3 = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper3.getId());
        Assert.assertEquals(BatchPostStatus.COMPLETED, posting3.getStatus());
        posting4 = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper4.getId());
        Assert.assertEquals(BatchPostStatus.COMPLETED, posting4.getStatus());
        // TODO history
        posting5 = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper5.getId());
        Assert.assertEquals(BatchPostStatus.COMPLETED, posting5.getStatus());
        // TODO postDate
        posting6 = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper6.getId());
        Assert.assertEquals(InvisibleType.S, posting6.getInvisible());
        Assert.assertEquals(BatchPostStatus.TIMEOUTSRV, posting6.getStatus());

    }

    @Test
    public void testProcessPackage() throws ParseException {
        Operday od = getOperday();
        Date testdate = DateUtils.dbDateParse(exampleBatchDateStr);
        try {
            setOperday(testdate, DateUtils.addDays(testdate, -1), od.getPhase(), od.getLastWorkdayStatus(), od.getPdMode());
            // WAITDATE - пакет
            // создать пакет
            BatchMessageIT.PackageParam param = loadPackage(USER_ID);

            // передать на подпись
            ManualOperationWrapper wrapper = new ManualOperationWrapper();
            wrapper.setPkgId(param.getId());
            wrapper.setAction(BatchPostAction.SIGN);
            wrapper.setUserId(USER_ID);

            RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(BatchPackageController.class, "forSignPackageRq", wrapper);
            if (res.isError())
                System.out.println(res.getMessage());
            Assert.assertFalse(res.isError());

//            baseEntityRepository.executeNativeUpdate("update GL_BATPST set POSTDATE = ?, VDATE = ?, STATE = 'WAITDATE'" +
//                    " where ID_PKG = ?", curdate, curdate, param.getId());

            baseEntityRepository.executeNativeUpdate("update GL_BATPST set STATE = 'WAITDATE' where ID_PKG = ?", param.getId());


            // запустить обработку PreCob
            remoteAccess.invoke(PreCobBatchPostingTask.class, "executeWork");

            List<BatchPosting> postings = (List<BatchPosting>) baseEntityRepository.select(BatchPosting.class, "from BatchPosting p where p.packageId = ?1",
                    param.getId());

            for (BatchPosting posting : postings) {
                Assert.assertEquals(BatchPostStatus.COMPLETED, posting.getStatus());
            }
        } finally {
            setOperday(od.getCurrentDate(), od.getLastWorkingDay(), od.getPhase(), od.getLastWorkdayStatus(), od.getPdMode());
        }

    }

    @Test
    public void testBalturRecalcSuppress() throws SQLException, ParseException {
        Date dateFrom = new SimpleDateFormat("yyyy-MM-dd").parse("2015-02-01");
        DataRecord rec = getAccountInBaltur("40702", dateFrom); //30223
        Assert.assertNotNull("Не найден счет для тестирования", rec);
        String acid = rec.getString("ACID");
        String bsaacid = rec.getString("BSAACID");
        String sqlSelect = "select ACID, BSAACID, DAT, OBAC, OBBC, DTAC, DTBC, CTAC, CTBC from BALTUR";
        String sqlWhere = " where acid = ? and bsaacid = ? and dat >= ?";
        List<DataRecord> list0 = baseEntityRepository.select(sqlSelect + sqlWhere + " order by dat desc", acid, bsaacid, dateFrom);
        Assert.assertNotEquals(0, list0.size());
        baseEntityRepository.executeNativeUpdate("delete from GL_BSARC where ACID = ? and BSAACID = ? and DAT = ?"
                , acid, bsaacid, dateFrom);
        int cnt0 = baseEntityRepository.executeNativeUpdate("insert into GL_BSARC (ACID, BSAACID, DAT, RECTED) values (?, ?, ?, '0')"
                , acid, bsaacid, dateFrom);
        Assert.assertEquals(1, cnt0);

        int cnt = baseEntityRepository.executeNativeUpdate("update BALTUR set DTAC=0, DTBC=0, CTAC=0, CTBC=0" + sqlWhere, acid, bsaacid, dateFrom);
        Assert.assertEquals(cnt, list0.size());

        int res = remoteAccess.invoke(BalturRecalculator.class, "recalculateBaltur");
        System.out.println("res = " + res);
        List<DataRecord> list1 = baseEntityRepository.select(sqlSelect + sqlWhere + " order by dat desc", acid, bsaacid, dateFrom);
        Assert.assertEquals(list0.size(), list1.size());
        for (int r=0; r < list0.size(); r++) {
            DataRecord data0 = list0.get(r);
            DataRecord data1 = list1.get(r);
            for (int i=0; i< data0.getColumnCount(); i++) {
                Assert.assertEquals(data0.getObject(i), data1.getObject(i));
            }
        }
    };

    @Test
    public void testBalturRecalcSuppress2() throws SQLException, ParseException {

        setOndemanBalanceMode();

        GLAccount account = findAccount("40702%");

        baseEntityRepository.executeNativeUpdate("delete from baltur where bsaacid = ?", account.getBsaAcid());
        baseEntityRepository.executeNativeUpdate("delete from pst where bsaacid = ?", account.getBsaAcid());
        baseEntityRepository.executeNativeUpdate("delete from GL_BSARC where BSAACID = ?", account.getBsaAcid());

        createPd(getOperday().getLastWorkingDay(), account.getAcid(), account.getBsaAcid(), account.getCurrency().getCurrencyCode(), "@@GL1");
        baseEntityRepository.executeNativeUpdate("insert into GL_BSARC (ACID, BSAACID, DAT, RECTED) values (?, ?, ?, '0')"
                , account.getAcid(), account.getBsaAcid(), getOperday().getLastWorkingDay());

        Assert.assertNull(account.getBsaAcid() + " is exists", baseEntityRepository.selectFirst("select * from baltur where bsaacid = ?", account.getBsaAcid()));
        Assert.assertTrue(1 == (int)remoteAccess.invoke(BalturRecalculator.class, "recalculateBaltur"));
        List<DataRecord> balturs = baseEntityRepository.select("select * from baltur where bsaacid = ?", account.getBsaAcid());
        Assert.assertTrue(1 == balturs.size());
        Assert.assertTrue(DataRecordUtils.toString(balturs.get(0)), balturs.stream().anyMatch(((Predicate<DataRecord>)
                     r -> r.getLong("OBAC") == 0)
                .and(r -> r.getLong("OBBC") == 0)
                .and(r -> r.getLong("DTAC") == 0)
                .and(r -> r.getLong("DTBC") == 0)
                .and(r -> r.getLong("CTAC") == 100)
                .and(r -> r.getLong("CTBC") == 100)));

        DataRecord rected = Optional.ofNullable(baseEntityRepository.selectFirst("select * from GL_BSARC where bsaacid = ? ", account.getBsaAcid())).orElseThrow(() -> new RuntimeException("GL_BSARC record is absent"));
        Assert.assertTrue("1".equals(rected.getString("RECTED")));

        baseEntityRepository.executeNativeUpdate("update BALTUR set DTAC=0, DTBC=0, CTAC=0, CTBC=0 where bsaacid = ?", account.getBsaAcid());
        baseEntityRepository.executeNativeUpdate("update GL_BSARC set RECTED = '0' where bsaacid = ?", account.getBsaAcid());

        int cnt2 = (int)remoteAccess.invoke(BalturRecalculator.class, "recalculateBaltur");
        Assert.assertTrue(""+ cnt2, 1 == cnt2);

        balturs = baseEntityRepository.select("select * from baltur where bsaacid = ?", account.getBsaAcid());
        Assert.assertTrue(1 == balturs.size());
        Assert.assertTrue(DataRecordUtils.toString(balturs.get(0)), balturs.stream().anyMatch(((Predicate<DataRecord>)
                r -> r.getLong("OBAC") == 0)
                .and(r -> r.getLong("OBBC") == 0)
                .and(r -> r.getLong("DTAC") == 0)
                .and(r -> r.getLong("DTBC") == 0)
                .and(r -> r.getLong("CTAC") == 100)
                .and(r -> r.getLong("CTBC") == 100)));
    };

    /*
    * select ACID, BSAACID from ACCRLN where DRLNC > CURRENT DATE and ACC2 = '20208' and (BSAACID, ACID) not in (select BSAACID, ACID from BALTUR where DAT >= '2016-10-01');
    * */

    @Test
    public void testBalturRecalcFull() throws SQLException, ParseException {

        setOnlineBalanceMode();

        Operday od = getOperday();
        String[] dateStr = {"2015-02-02", "2015-02-04", "2015-02-10", "2015-02-12"};
        String[] acc2s = {"47425", "20208"}; // "20202"};
        Date[] dates = new Date[dateStr.length];
        String[] acids = new String[2];
        String[] bsaAcids = new String[2];
        try {
            Date testdate = new SimpleDateFormat("yyyy-MM-dd").parse(dateStr[3]);
            setOperday(testdate, DateUtils.addDays(testdate, -1), od.getPhase(), od.getLastWorkdayStatus(), od.getPdMode());

            for (int i = 0; i < dateStr.length; i++) {
                dates[i] = new SimpleDateFormat("yyyy-MM-dd").parse(dateStr[i]);
            }
            for (int i = 0; i < acc2s.length; i++) {
                String acc2 = acc2s[i];
                DataRecord rec = getAccountNotBaltur(acc2, dates[0]);
                Assert.assertNotNull("Не найден счет для тестирования c ACC2 = " + acc2, rec);
                acids[i] = rec.getString("ACID");
                bsaAcids[i] = rec.getString("BSAACID");
            }

            String acid = acids[0];
            String bsaAcid = bsaAcids[0];
            Date dateFrom = dates[1];

            Long[] operationIds = new Long[dateStr.length];

            updateOperdayMode(Operday.PdMode.DIRECT, ProcessingStatus.STARTED);

            GLOperation operation = createMosRurOperation(dates[0], bsaAcids[1], bsaAcids[0], new BigDecimal("100"));   // 1 - 4    0 0 100
            operationIds[0] = operation.getId();
            operation = createMosRurOperation(dates[1], bsaAcids[0], bsaAcids[1], new BigDecimal("10"));                // 5 - 6    100 -10 0
            operationIds[1] = operation.getId();
            operation = createMosRurOperation(dates[2], bsaAcids[1], bsaAcids[0], new BigDecimal("50"));                // 7 - 8    90 0 50
            operationIds[2] = operation.getId();
            operation = createMosRurOperation(dates[3], bsaAcids[0], bsaAcids[1], new BigDecimal("20"));                // 9 - 2029 140 -20 0
            operationIds[3] = operation.getId();

            String sqlSelect = "select DAT, ACID, BSAACID, OBAC, OBBC, DTAC, DTBC, CTAC, CTBC from BALTUR";
            String sqlWhere = " where acid = ? and bsaacid = ?";

            // TODO пока не проходит, т.к. не работает триггер на PST
            List<DataRecord> list0 = baseEntityRepository.select(sqlSelect + sqlWhere + " and dat >= ? order by dat", acid, bsaAcid, dates[0]);
            Assert.assertEquals(dateStr.length, list0.size());

            baseEntityRepository.executeNativeUpdate("delete from GL_BSARC where ACID = ? and BSAACID = ? and DAT = ?", acid, bsaAcid, dateFrom);
            baseEntityRepository.executeNativeUpdate("delete from GL_BSARC where RECTED = '0'");
            int cnt = baseEntityRepository.executeNativeUpdate("insert into GL_BSARC (ACID, BSAACID, DAT, RECTED) values (?, ?, ?, '0')"
                    , acid, bsaAcid, dateFrom);
            Assert.assertEquals(1, cnt);

    //        cnt = baseEntityRepository.executeNativeUpdate("update BALTUR set DTAC=0, DTBC=0, CTAC=0, CTBC=0" + sqlWhere + " and dat >= ?", acid, bsaAcid, dateFrom);
    //        Assert.assertEquals(3, cnt);
            cnt = baseEntityRepository.executeNativeUpdate("update BALTUR set DATTO = DATTO + 1" + sqlWhere + " and dat = ?", acid, bsaAcid, dates[0]);
            Assert.assertEquals(1, cnt);
            cnt = baseEntityRepository.executeNativeUpdate("update BALTUR set DAT = DAT + 1, DATTO = DATTO + 1" + sqlWhere + " and dat = ?", acid, bsaAcid, dates[1]);
            Assert.assertEquals(1, cnt);
            cnt = baseEntityRepository.executeNativeUpdate("update BALTUR set DAT = DAT + 1" + sqlWhere + " and dat = ?", acid, bsaAcid, dates[2]);
            Assert.assertEquals(1, cnt);

            updateOperdayMode(Operday.PdMode.DIRECT, ProcessingStatus.STOPPED);

            setOndemanBalanceMode();

            cnt = remoteAccess.invoke(BalturRecalculator.class, "recalculateBaltur");
            Assert.assertEquals(1, cnt);
            List<DataRecord> list1 = baseEntityRepository.select(sqlSelect + sqlWhere+ " and dat >= ? order by dat", acid, bsaAcid, dates[0]);
    //        Assert.assertEquals(list0.size() + 2, list1.size());

            // 2 - 3    0 0 10000
            // 4 - 4    10000 -1000 0
            // 5 - 9    10000 0 0
            // 10 - 10  9000 0 5000
            // 11 - 11  9000 0 0
            // 12 -     14000 -2000 0

            Long bal0 = list1.get(0).getLong("OBAC");
            for (int r=0; r < list1.size(); r++) {
                DataRecord data1 = list1.get(r);
                for (int i=0; i< data1.getColumnCount(); i++) {
                    System.out.print(data1.getObject(i) + " ");
                }
                System.out.println();
            }
            for (int r0 = 0, r1=0; r1 < list0.size(); r1++) {
                DataRecord data0 = list0.get(r0);
                DataRecord data1 = list1.get(r1);
                if (data0.getDate("DAT").equals(data1.getDate("DAT"))) {
                    for (int i = 3; i < data0.getColumnCount(); i++) {
                        Assert.assertEquals(data0.getLong(i), data1.getLong(i));
                    }
                    r0++;
                } else {
                    for (int i = 3; i < 5; i++) {
                        Assert.assertEquals(data0.getLong(i), data1.getLong(i));
                    }
                    for (int i = 5; i < data0.getColumnCount(); i++) {
                        Assert.assertEquals(0L, (long)data1.getLong(i));
                    }
                }
            }

            String operIds = StringUtils.listToString(Arrays.asList(operationIds), ",");
            cnt = baseEntityRepository.executeNativeUpdate("update PST set INVISIBLE = '1' where PCID in (select PCID from GL_POSTING where GLO_REF in (" + operIds + "))");
            cnt = baseEntityRepository.executeNativeUpdate("delete from BALTUR" + sqlWhere + " and dat >= ?", acids[0], bsaAcids[0], dates[0]);
            cnt = baseEntityRepository.executeNativeUpdate("delete from BALTUR" + sqlWhere + " and dat >= ?", acids[1], bsaAcids[1], dates[0]);

            updateOperdayMode(Operday.PdMode.DIRECT, ProcessingStatus.STARTED);

            DataRecord record = baseEntityRepository.selectFirst("select * from GL_BSARC r where r.bsaacid = ?", bsaAcid);
            Assert.assertEquals(BalturRecalculator.BalturRecalcState.PROCESSED.getValue(), record.getString("rected"));

        } finally {
            setOperday(od.getCurrentDate(), od.getLastWorkingDay(), od.getPhase(), od.getLastWorkdayStatus(), od.getPdMode());
        }
    };

    private DataRecord getAccountInBaltur(String acc2, Date dateFrom) throws SQLException {
        return baseEntityRepository.selectFirst("select ACID, BSAACID from GL_ACC where (DTC is null or DTC > sysdate) and CBCCN = '0001' and CCY = 'RUR'" +
                " and ACC2 = ? and (BSAACID, ACID) in (select BSAACID, ACID from BALTUR where DAT >= ?)", acc2, dateFrom);
    }

    private DataRecord getAccountNotBaltur(String acc2, Date dateFrom) throws SQLException {
        return baseEntityRepository.selectFirst("select ACID, BSAACID from GL_ACC where (DTC is null or DTC > sysdate) and CBCCN = '0001' and CCY = 'RUR'" +
                " and ACC2 = ? and (BSAACID, ACID) not in (select BSAACID, ACID from BALTUR where DAT >= ?)", acc2, dateFrom);
    }

    public GLOperation createMosRurOperation(Date postDate, String accountDebit, String accountCredit, BigDecimal amount  ) throws SQLException {
        return createManualOperation(postDate, "А", "MOS", accountDebit, "RUR", amount,
                                                         "MOS", accountCredit, "RUR", amount );
    }

    public GLOperation createManualOperation(Date postDate, String bsChapter,
                                             String filialDebit, String accountDebit, String currencyDebit, BigDecimal amountDebit,
                                             String filialCredit, String accountCredit, String currencyCredit, BigDecimal amountCredit  ) throws SQLException {

        ManualOperationWrapper wrapper = newOperationWrapper(postDate, bsChapter,
                filialDebit,  accountDebit,  currencyDebit,  amountDebit,
                filialCredit, accountCredit, currencyCredit, amountCredit
        );

        BatchPosting posting0 = createAuthorizedPosting(wrapper, USER_ID, BatchPostStatus.CONTROL);
        Assert.assertNotNull(posting0);
        wrapper.setId(posting0.getId());
        wrapper.setStatus(posting0.getStatus());
        GLManualOperation operation = remoteAccess.invoke(ManualOperationController.class, "processPosting", posting0, false);
        Assert.assertNotNull(operation);

        BatchPosting posting = (BatchPosting) baseEntityRepository.findById(BatchPosting.class, wrapper.getId());
        Assert.assertNotNull(posting);  // запрос
        Assert.assertEquals(BatchPostStatus.COMPLETED, posting.getStatus());
        return posting.getOperation();
    }

    private BatchPosting createAuthorizedPosting(ManualOperationWrapper wrapper, Long userId, BatchPostStatus status) throws SQLException {
        wrapper.setAction(BatchPostAction.SAVE);
        // создать запрос
        BatchPostStatus inputStatus = BatchPostStatus.CONTROL;
        BatchPosting posting = remoteAccess.invoke(BatchPostingProcessor.class, "createPosting", wrapper);       // создать операцию
        posting.setStatus(status);
        posting.setIsTech(YesNo.N); // TODO устанавливаем признак операции не по техсчетам
        posting = (BatchPosting) baseEntityRepository.save(posting);     // сохранить входящую операцию

//        RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(ManualPostingController.class, "saveOperationRqInternal", wrapper, inputStatus);
//        if (res.isError())
//            System.out.println(res.getMessage());
//        Assert.assertFalse(res.isError());
//        wrapper = res.getResult();
//        Assert.assertTrue(0 < wrapper.getId());

        if (status != inputStatus) {
            baseEntityRepository.executeNativeUpdate("update GL_BATPST set STATE = ?, " +
                    "USER_NAME = (select USER_NAME from GL_USER where ID_USER = ?) where ID = ?", status.name(), userId, wrapper.getId());
        }
        BatchPosting posting1 = (BatchPosting)baseEntityRepository.findById(BatchPosting.class, posting.getId());
        baseEntityRepository.refresh(posting1, true);
        return posting1;

    }

}

