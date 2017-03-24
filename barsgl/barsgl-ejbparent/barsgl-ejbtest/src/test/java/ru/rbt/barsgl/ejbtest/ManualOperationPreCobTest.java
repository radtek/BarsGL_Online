package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.rbt.barsgl.ejb.bt.BalturRecalculator;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.ExecutePreCOBTask;
import ru.rbt.barsgl.ejb.controller.operday.task.PreCobBatchPostingTask;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLManualOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.bg.BatchPackageController;
import ru.rbt.barsgl.ejb.integr.bg.ManualOperationController;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.util.StringUtils;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.BatchPostAction;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.InvisibleType;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static ru.rbt.barsgl.ejbtest.BatchMessageTest.loadPackage;

/**
 * Created by ER18837 on 04.07.16.
 */
public class ManualOperationPreCobTest extends AbstractTimerJobTest {

    private final Long USER_ID = 2L;

    @Before
    public  void before() {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
    }

    @Test
    public void testProcessPostings() throws SQLException {

        // 1.CONTROL
        String bsaDt1 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "4081703605%1");
        String bsaCt1 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "4081703605%3");
        ManualOperationWrapper wrapper1 = newOperationWrapper("А",
                "MOS", bsaDt1, "AUD", new BigDecimal("161.057"),
                "MOS", bsaCt1, "AUD", new BigDecimal("161.057")
        );

        BatchPosting posting = ManualOperationTest.createAuthorizedPosting(wrapper1, USER_ID, BatchPostStatus.CONTROL);
        Assert.assertNotNull(posting);
        wrapper1.setId(posting.getId());
        wrapper1.setStatus(posting.getStatus());

        // 2.CONTROL -> WORKING
        String bsaDt2 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "4081703605%2");
        String bsaCt2 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "4081703605%4");
        ManualOperationWrapper wrapper2 = newOperationWrapper("А",
                "MOS", bsaDt2, "AUD", new BigDecimal("162.057"),
                "MOS", bsaCt2, "AUD", new BigDecimal("162.057")
        );
        posting = ManualOperationTest.createAuthorizedPosting(wrapper2, USER_ID, BatchPostStatus.WORKING);
        Assert.assertNotNull(posting);
        wrapper2.setId(posting.getId());
        wrapper2.setStatus(posting.getStatus());

        // 3.SIGNEDDATE
        String bsaDt3 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "4081703605%3");
        String bsaCt3 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "4081703605%5");
        ManualOperationWrapper wrapper3 = newOperationWrapper("А",
                "MOS", bsaDt3, "AUD", new BigDecimal("163.057"),
                "MOS", bsaCt3, "AUD", new BigDecimal("163.057")
        );
        wrapper3.setPostDateStr(new SimpleDateFormat(wrapper3.dateFormat).format(getOperday().getLastWorkingDay()));
        wrapper3.setValueDateStr(wrapper3.getPostDateStr());

        posting = ManualOperationTest.createAuthorizedPosting(wrapper3, USER_ID, BatchPostStatus.SIGNEDDATE);
        Assert.assertNotNull(posting);
        wrapper3.setId(posting.getId());
        wrapper3.setStatus(posting.getStatus());

        // 4.COMPLETED -> WORKING
        String bsaDt4 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "4081703605%4");
        String bsaCt4 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "4081703605%6");
        ManualOperationWrapper wrapper4 = newOperationWrapper("А",
                "MOS", bsaDt4, "AUD", new BigDecimal("164.057"),
                "MOS", bsaCt4, "AUD", new BigDecimal("164.057")
        );

        posting = ManualOperationTest.createAuthorizedPosting(wrapper4, USER_ID, BatchPostStatus.SIGNED);
        Assert.assertNotNull(posting);
        wrapper4.setId(posting.getId());
        wrapper4.setStatus(posting.getStatus());
        GLManualOperation operation = remoteAccess.invoke(ManualOperationController.class, "processPosting", posting, false);
        Assert.assertNotNull(operation);
        baseEntityRepository.executeNativeUpdate("update GL_BATPST set STATE = ? where ID = ?", BatchPostStatus.WORKING.name(), wrapper4.getId());

        // 5.WAITDATE - ручной
        String bsaDt5 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "4081703605%5");
        String bsaCt5 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "4081703605%7");
        ManualOperationWrapper wrapper5 = newOperationWrapper("А",
                "MOS", bsaDt5, "AUD", new BigDecimal("165.057"),
                "MOS", bsaCt5, "AUD", new BigDecimal("165.057")
        );
        wrapper5.setPostDateStr(new SimpleDateFormat(wrapper5.dateFormat).format(getOperday().getLastWorkingDay()));
        wrapper5.setValueDateStr(wrapper5.getPostDateStr());

        posting = ManualOperationTest.createAuthorizedPosting(wrapper5, USER_ID, BatchPostStatus.WAITDATE);
        Assert.assertNotNull(posting);
        wrapper5.setId(posting.getId());
        wrapper5.setStatus(posting.getStatus());

        // 6.CONTROL -> WAITSRV
        String bsaDt6 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "4081703605%2");
        String bsaCt6 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "4081703605%4");
        ManualOperationWrapper wrapper6 = newOperationWrapper("А",
                "MOS", bsaDt6, "AUD", new BigDecimal("162.057"),
                "MOS", bsaCt6, "AUD", new BigDecimal("162.057")
        );
        posting = ManualOperationTest.createAuthorizedPosting(wrapper2, USER_ID, BatchPostStatus.WAITSRV);
        Assert.assertNotNull(posting);
        wrapper6.setId(posting.getId());
        wrapper6.setStatus(posting.getStatus());
        baseEntityRepository.executeNativeUpdate("update GL_BATPST set SRV_REF = 'TEST', SEND_SRV = CURRENT TIMESTAMP where ID = ?", wrapper6.getId());

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
        remoteAccess.invoke(ExecutePreCOBTask.class, "processUnprocessedBatchPostings");

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
    public void testProcessPackage() {
        // TODO WAITDATE - пакет
        // создать пакет
        BatchMessageTest.PackageParam param = loadPackage(USER_ID);

        // передать на подпись
        ManualOperationWrapper wrapper = new ManualOperationWrapper();
        wrapper.setPkgId(param.getId());
        wrapper.setAction(BatchPostAction.SIGN);
        wrapper.setUserId(USER_ID);

        RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(BatchPackageController.class, "forSignPackageRq", wrapper);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());

        Date curdate = getOperday().getCurrentDate();
        baseEntityRepository.executeNativeUpdate("update GL_BATPST set POSTDATE = ?, VDATE = ?, STATE = 'WAITDATE'" +
                " where ID_PKG = ?", curdate, curdate, param.getId());

        // запустить обработку PreCob
        remoteAccess.invoke(PreCobBatchPostingTask.class, "executeWork");

        List<BatchPosting> postings = (List<BatchPosting>) baseEntityRepository.select(BatchPosting.class, "from BatchPosting p where p.packageId = ?1",
                param.getId());

        for (BatchPosting posting: postings) {
            Assert.assertEquals(BatchPostStatus.COMPLETED, posting.getStatus());
        }
    }

    @Test
    public void testBalturRecalcSuppress() throws SQLException, ParseException {
        Date dateFrom = new SimpleDateFormat("yyyy-MM-dd").parse("2015-02-01");
        DataRecord rec = getAccountInBaltur("30223", dateFrom);
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

    /*
    * select ACID, BSAACID from ACCRLN where DRLNC > CURRENT DATE and ACC2 = '20208' and (BSAACID, ACID) not in (select BSAACID, ACID from BALTUR where DAT >= '2016-10-01');
    * */

    @Test
    public void testBalturRecalcFull() throws SQLException, ParseException {

        String[] dateStr = {"2015-02-02", "2015-02-04", "2015-02-10", "2015-02-12"};
        String[] acc2s = {"20208", "30126"};
        Date[] dates = new Date[dateStr.length];
        String[] acids = new String[2];
        String[] bsaAcids = new String[2];
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

        List<DataRecord> list0 = baseEntityRepository.select(sqlSelect + sqlWhere + " and dat >= ? order by dat", acid, bsaAcid, dates[0]);
        Assert.assertEquals(dateStr.length, list0.size());

        baseEntityRepository.executeNativeUpdate("delete from GL_BSARC where ACID = ? and BSAACID = ? and DAT = ?", acid, bsaAcid, dateFrom);
        baseEntityRepository.executeNativeUpdate("delete from GL_BSARC where RECTED = '0'");
        int cnt = baseEntityRepository.executeNativeUpdate("insert into GL_BSARC (ACID, BSAACID, DAT, RECTED) values (?, ?, ?, '0')"
                , acid, bsaAcid, dateFrom);
        Assert.assertEquals(1, cnt);

//        cnt = baseEntityRepository.executeNativeUpdate("update BALTUR set DTAC=0, DTBC=0, CTAC=0, CTBC=0" + sqlWhere + " and dat >= ?", acid, bsaAcid, dateFrom);
//        Assert.assertEquals(3, cnt);
        cnt = baseEntityRepository.executeNativeUpdate("update BALTUR set DATTO = DATTO + 1 DAYS" + sqlWhere + " and dat = ?", acid, bsaAcid, dates[0]);
        Assert.assertEquals(1, cnt);
        cnt = baseEntityRepository.executeNativeUpdate("update BALTUR set DAT = DAT + 1 DAYS, DATTO = DATTO + 1 DAYS" + sqlWhere + " and dat = ?", acid, bsaAcid, dates[1]);
        Assert.assertEquals(1, cnt);
        cnt = baseEntityRepository.executeNativeUpdate("update BALTUR set DAT = DAT + 1 DAYS" + sqlWhere + " and dat = ?", acid, bsaAcid, dates[2]);
        Assert.assertEquals(1, cnt);

        updateOperdayMode(Operday.PdMode.DIRECT, ProcessingStatus.STOPPED);

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
        cnt = baseEntityRepository.executeNativeUpdate("update PD set INVISIBLE = '1' where PCID in (select PCID from GL_POSTING where GLO_REF in (" + operIds + "))");
        cnt = baseEntityRepository.executeNativeUpdate("delete from BALTUR" + sqlWhere + " and dat >= ?", acids[0], bsaAcids[0], dates[0]);
        cnt = baseEntityRepository.executeNativeUpdate("delete from BALTUR" + sqlWhere + " and dat >= ?", acids[1], bsaAcids[1], dates[0]);

        updateOperdayMode(Operday.PdMode.DIRECT, ProcessingStatus.STARTED);

        DataRecord record = baseEntityRepository.selectFirst("select * from GL_BSARC r where r.bsaacid = ?", bsaAcid);
        Assert.assertEquals(BalturRecalculator.BalturRecalcState.PROCESSED.getValue(), record.getString("rected"));

    };

    private DataRecord getAccountInBaltur(String acc2, Date dateFrom) throws SQLException {
        return baseEntityRepository.selectFirst("select ACID, BSAACID from ACCRLN where DRLNC > CURRENT DATE and CCODE = '0001' and CBCCY = '810'" +
                " and ACC2 = ? and (BSAACID, ACID) in (select BSAACID, ACID from BALTUR where DAT >= ?)", acc2, dateFrom);
    }

    private DataRecord getAccountNotBaltur(String acc2, Date dateFrom) throws SQLException {
        return baseEntityRepository.selectFirst("select ACID, BSAACID from ACCRLN where DRLNC > CURRENT DATE and CCODE = '0001' and CBCCY = '810'" +
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

        BatchPosting posting0 = ManualOperationTest.createAuthorizedPosting(wrapper, USER_ID, BatchPostStatus.CONTROL);
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

}

