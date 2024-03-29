package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.PreCobStepController;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.sec.GLErrorRecord;
import ru.rbt.barsgl.ejb.repository.GLErrorRepository;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import static ru.rbt.barsgl.shared.enums.OperState.ERCHK;

/**
 * Created by ER18837 on 14.02.17.
 */
public class FanValidationIT extends AbstractTimerJobIT {

    public static final Logger log = Logger.getLogger(FanIT.class.getName());

    @BeforeClass
    public static void beforeClass() {
        initCorrectOperday();
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
    }

    /**
     * FAN_IS_ONLY_ONE 1009
     * проверка ошибки обработки веера - одна операция по вееру
     */
    @Test
    public void testFanOnlyOne() throws SQLException {
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20202810%");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40802810%");

        final long st = System.currentTimeMillis();
        EtlPackage etlPackage = newPackageNotSaved(st, "Checking fan posting validation");
        etlPackage.setAccountCnt(1);                        // число перьев в веере
        etlPackage = (EtlPackage) baseEntityRepository.save(etlPackage);

        String paymentRef = "PM_" + ("" + System.currentTimeMillis()).substring(5);

        // основная проводка
        EtlPosting pst1 = createFanPosting(st, etlPackage, bsaDt, bsaCt
                , new BigDecimal("123.45"), BankCurrency.RUB
                , new BigDecimal("123.45"), BankCurrency.RUB, paymentRef, paymentRef, YesNo.Y);

        GLOperation oper1 = (GLOperation) postingController.processMessage(pst1);
        Assert.assertNotNull(oper1);

        // process fan fully
        List<GLOperation> operList = fanPostingController.processOperations(paymentRef);
        Assert.assertEquals(0, operList.size());

        checkErrorRecord(oper1, "1009");
    }

    /**
     * FAN_PARENT_NOT_EXISTS 1010
     * проверка ошибки обработки веера - не найдена родительская операция по вееру
     */
    @Test
    public void testFanNoParent() throws SQLException {
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40806810%");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40702810%");

        final long st = System.currentTimeMillis();
        EtlPackage etlPackage = newPackageNotSaved(st, "Checking fan posting validation");
        etlPackage.setAccountCnt(2);                        // число перьев в веере
        etlPackage = (EtlPackage) baseEntityRepository.save(etlPackage);

        String parentRef = "PM_" + ("" + System.currentTimeMillis()).substring(5);
        String paymentRef1 = "PM1_" + ("" + System.currentTimeMillis()).substring(5);
        String paymentRef2 = "PM2_" + ("" + System.currentTimeMillis()).substring(5);

        // основная проводка
        EtlPosting pst1 = createFanPosting(st, etlPackage, bsaDt, bsaCt
                , new BigDecimal("123.45"), BankCurrency.RUB
                , new BigDecimal("123.45"), BankCurrency.RUB, paymentRef1, parentRef, YesNo.Y);

        GLOperation oper1 = (GLOperation) postingController.processMessage(pst1);
        Assert.assertNotNull(oper1);

        // основная проводка
        EtlPosting pst2 = createFanPosting(st, etlPackage, bsaCt, bsaDt
                , new BigDecimal("123.45"), BankCurrency.RUB
                , new BigDecimal("123.45"), BankCurrency.RUB, paymentRef2, parentRef, YesNo.Y);

        GLOperation oper2 = (GLOperation) postingController.processMessage(pst2);
        Assert.assertNotNull(oper2);
        // process fan fully
        List<GLOperation> operList = fanPostingController.processOperations(parentRef);
        Assert.assertEquals(0, operList.size());

        checkErrorRecord(oper1, "1010");
        checkErrorRecord(oper2, "1010");
    }

    /**
     * FAN_SIDE_NOT_DEFINED 1011
     * проверка ошибки обработки веера - не удалось определить сторону веера
     */
    @Test
    public void testFanNoSide() throws SQLException {
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40806810%3");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40702810%3");

        final long st = System.currentTimeMillis();
        EtlPackage etlPackage = newPackageNotSaved(st, "Checking fan posting validation");
        etlPackage.setAccountCnt(2);                        // число перьев в веере
        etlPackage = (EtlPackage) baseEntityRepository.save(etlPackage);

        String parentRef = "PM_" + ("" + System.currentTimeMillis()).substring(5);
        String paymentRef1 = "PM1_" + ("" + System.currentTimeMillis()).substring(5);
        String paymentRef2 = "PM2_" + ("" + System.currentTimeMillis()).substring(5);

        // основная проводка
        EtlPosting pst1 = createFanPosting(st, etlPackage, bsaDt, bsaCt
                , new BigDecimal("123.45"), BankCurrency.RUB
                , new BigDecimal("123.45"), BankCurrency.RUB, parentRef, parentRef, YesNo.Y);

        GLOperation oper1 = (GLOperation) postingController.processMessage(pst1);
        Assert.assertNotNull(oper1);

        // основная проводка
        EtlPosting pst2 = createFanPosting(st, etlPackage, bsaCt, bsaDt
                , new BigDecimal("123.45"), BankCurrency.RUB
                , new BigDecimal("123.45"), BankCurrency.RUB, paymentRef2, parentRef, YesNo.Y);

        GLOperation oper2 = (GLOperation) postingController.processMessage(pst2);
        Assert.assertNotNull(oper2);
        // process fan fully
        List<GLOperation> operList = fanPostingController.processOperations(parentRef);
        Assert.assertEquals(0, operList.size());

        checkErrorRecord(oper1, "1011");
        checkErrorRecord(oper2, "1011");
    }

    /**
     * FAN_PARENT_NOT_SINGLE 1015
     * проверка ошибки обработки веера - более одной родительской операции по вееру
     */
    @Test
    public void testFanManyParent() throws SQLException {
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40806810%4");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40702810%4");

        final long st = System.currentTimeMillis();
        EtlPackage etlPackage = newPackageNotSaved(st, "Checking fan posting validation");
        etlPackage.setAccountCnt(2);                        // число перьев в веере
        etlPackage = (EtlPackage) baseEntityRepository.save(etlPackage);

        String parentRef = "PM_" + ("" + System.currentTimeMillis()).substring(5);
        String paymentRef1 = "PM1_" + ("" + System.currentTimeMillis()).substring(5);
        String paymentRef2 = "PM2_" + ("" + System.currentTimeMillis()).substring(5);

        // основная проводка
        EtlPosting pst1 = createFanPosting(st, etlPackage, bsaDt, bsaCt
                , new BigDecimal("123.45"), BankCurrency.RUB
                , new BigDecimal("123.45"), BankCurrency.RUB, parentRef, parentRef, YesNo.Y);

        GLOperation oper1 = (GLOperation) postingController.processMessage(pst1);
        Assert.assertNotNull(oper1);

        // основная проводка
        EtlPosting pst2 = createFanPosting(st, etlPackage, bsaCt, bsaDt
                , new BigDecimal("123.45"), BankCurrency.RUB
                , new BigDecimal("123.45"), BankCurrency.RUB, parentRef, parentRef, YesNo.Y);

        GLOperation oper2 = (GLOperation) postingController.processMessage(pst2);
        Assert.assertNotNull(oper2);
        // process fan fully
        List<GLOperation> operList = fanPostingController.processOperations(parentRef);
        Assert.assertEquals(0, operList.size());

        checkErrorRecord(oper1, "1015");
        checkErrorRecord(oper2, "1015");
    }

    /**
     * FAN_INVALID_STATE 1019
     * проверка ошибки обработки веера - найдены частичные веерные операции не в статусе LOAD, WTAC
     * TODO пока эта ошибка не пишется в GL_ERRORS. Если надо - добавить в PreCobStepController
     */
    @Test
    public void testFanNoRate() throws SQLException {
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40806810%5");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40702810%5");
        String bsaCt2 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "47423975%"); // BGN	975

        DataRecord rate = baseEntityRepository.selectFirst("select DAT, CCY, RATE, AMNT, RATE0 from CURRATES where CCY = ? and  DAT = ?",
                BankCurrency.BGN.getCurrencyCode(), getOperday().getCurrentDate());

        if (null != rate) {
            baseEntityRepository.executeNativeUpdate("delete from CURRATES where CCY = ? and  DAT = ?",
                    BankCurrency.BGN.getCurrencyCode(), getOperday().getCurrentDate());
        }

        final long st = System.currentTimeMillis();
        EtlPackage etlPackage = newPackageNotSaved(st, "Checking fan posting validation");
        etlPackage.setAccountCnt(2);                        // число перьев в веере
        etlPackage = (EtlPackage) baseEntityRepository.save(etlPackage);

        String parentRef = "PM_" + ("" + System.currentTimeMillis()).substring(5);
        String paymentRef1 = "PM1_" + ("" + System.currentTimeMillis()).substring(5);
        String paymentRef2 = "PM2_" + ("" + System.currentTimeMillis()).substring(5);

        // основная проводка
        EtlPosting pst1 = createFanPosting(st, etlPackage, bsaDt, bsaCt
                , new BigDecimal("123.45"), BankCurrency.RUB
                , new BigDecimal("123.45"), BankCurrency.RUB, parentRef, parentRef, YesNo.Y);

        GLOperation oper1 = (GLOperation) postingController.processMessage(pst1);
        Assert.assertNotNull(oper1);

        // основная проводка
        EtlPosting pst2 = createFanPosting(st, etlPackage, bsaDt, bsaCt2
                , new BigDecimal("123.45"), BankCurrency.RUB
                , new BigDecimal("5000"), BankCurrency.BGN, paymentRef2, parentRef, YesNo.Y);

        GLOperation oper2 = (GLOperation) postingController.processMessage(pst2);
        Assert.assertNotNull(oper2);

        // process fan fully
        remoteAccess.invoke(PreCobStepController.class, "processFan");
        List<GLOperation> operList = remoteAccess.invoke(GLOperationRepository.class, "getFanOperationByRef", parentRef, YesNo.N);

        Assert.assertEquals(2, operList.size());

        checkErrorRecord(oper1, "1019");
        checkErrorRecord(oper2, "1003", ERCHK);

        if (null != rate) {
            baseEntityRepository.executeNativeUpdate("insert into CURRATES (DAT, CCY, RATE, AMNT, RATE0) values (?, ?, ?, ?, ?)",
                    rate.getDate(0), rate.getString(1), rate.getBigDecimal(2), rate.getBigDecimal(3), rate.getBigDecimal(4));
        }
    }

    public static void checkErrorRecord(GLOperation operation, String errorCode) {
        checkErrorRecord(operation, errorCode, OperState.ERPROC);
    }

    public static void checkErrorRecord(GLOperation operation, String errorCode, OperState state) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {}
        Assert.assertNotNull(operation);
        Long pstRef = operation.getEtlPostingRef();
        Long gloRef = operation.getId();
        GLOperation oper = (GLOperation) baseEntityRepository.refresh(operation, true);
        if (null != oper.getErrorMessage())
            System.out.println(oper.getErrorMessage());
        Assert.assertEquals(state, oper.getState());
        String errorMessage = oper.getErrorMessage();
        Assert.assertTrue(errorMessage.contains(errorCode));

//        GLErrorRecord errorRecord = remoteAccess.invoke(GLErrorRepository.class, "getRecordByRef", pstRef, null);
        List<GLErrorRecord> errorRecords = baseEntityRepository.select(GLErrorRecord.class, "from GLErrorRecord g where g.etlPostingRef = ?1 order by g.id desc", pstRef);
        Assert.assertNotNull("Error record has not found by ref=" + pstRef, errorRecords);
        Assert.assertNotEquals("Error record has not found by ref=" + pstRef, 0, errorRecords.size());
        boolean isErrorRecord = false;
        for (GLErrorRecord errorRecord : errorRecords) {
            Assert.assertEquals(gloRef, errorRecord.getGlOperRef());
            isErrorRecord |= (errorCode.equals(errorRecord.getErrorCode()));
        }
        Assert.assertTrue("Error record has not found by ref=" + + pstRef + " with errorCode=" + errorCode, isErrorRecord);
    }
}
