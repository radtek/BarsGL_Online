package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.acc.AccountBatchStateController;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchRequest;
import ru.rbt.barsgl.shared.enums.AccountBatchPackageState;
import ru.rbt.barsgl.shared.enums.AccountBatchState;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static ru.rbt.barsgl.shared.enums.AccountBatchPackageState.*;
import static ru.rbt.barsgl.shared.enums.AccountBatchState.COMPLETED;
import static ru.rbt.ejbcore.mapping.YesNo.Y;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by Ivan Sevastyanov on 16.10.2018.
 */
public class ExcelAccOpenIT extends AbstractRemoteIT {

    private static final Logger log = Logger.getLogger(ExcelAccOpenIT.class.getName());

    @Test public void test() {

        AccountBatchPackage package1 = createPackage();
        package1 = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, package1.getId());
        Assert.assertEquals(YesNo.N, package1.getInvisible());

        package1.setInvisible(Y);
        baseEntityRepository.update(package1);

        package1 = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, package1.getId());
        Assert.assertEquals(Y, package1.getInvisible());
    }

    @Test public void testRequest() {

        AccountBatchPackage package1 = createPackage();

        AccountBatchRequest request = new AccountBatchRequest();
        request.setBatchPackage(package1);
        request.setLineNumber(1L);
        request.setState(AccountBatchState.LOAD);
        request.setInBranch("001");
        request.setInCcy("RUR");
        request.setInCustno("001");
        request.setInAcctype("090909090");
        request.setInDealsrc("IMEX");

        request = (AccountBatchRequest) baseEntityRepository.save(request);

        Assert.assertTrue(0 < request.getId());
    }

    @Test public void testProcess() throws SQLException {

        final String acctype = "911020416";
        final String custno = "00151555";
        final String dealId = "511_A073C_20";

        DataRecord acctypeRecord = baseEntityRepository.selectFirst(
                "select * from gl_actparm p where p.acod is not null and plcode is null and acctype = ?", acctype);
        baseEntityRepository.executeNativeUpdate("delete from GL_ACBATREQ r where exists (select 1 from gl_acc a where a.id = r.glacid and dealid = ? and acctype = ? and custno = ?)"
                , dealId, acctype, custno);
        baseEntityRepository.executeNativeUpdate("delete from gl_acc where dealid = ? and acctype = ? and custno = ?", dealId, acctype, custno);

        final String branch = "001";
        final String ccy = "RUR";
        final String cbcusttype = acctypeRecord.getString("custype");
        final String term = acctypeRecord.getString("term");
        final String sealSrc = "IMEX";
        final String subdealid = "00151555RURCL0P00300";

        // готовим счет
        log.info("deleted accounts: " + baseEntityRepository.executeNativeUpdate(
                "delete from gl_acc where BRANCH = ? and CCY = ? and CUSTNO = ? and ACCTYPE = ? and CBCUSTTYPE = ? and TERM = ? and DEALID = ? and SUBDEALID = ?"
            ,branch,ccy,custno,acctype,cbcusttype,term,dealId,subdealid));

        // формируем пакет
        AccountBatchPackage pkg = createPackage();

        AccountBatchRequest request1 = createBatchRequest(pkg, 1L, branch, ccy, custno, acctype, dealId, subdealid, sealSrc, term);
        AccountBatchRequest request2 = createBatchRequest(pkg, 2L, branch, ccy, custno, acctype, dealId, subdealid, sealSrc, "05");

        // валидируем
        remoteAccess.invoke(AccountBatchStateController.class, "sendToValidation", pkg);

        // проверяем состояние

        pkg = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, pkg.getId());
        Assert.assertEquals(ON_VALID, pkg.getState());

        remoteAccess.invoke(AccountBatchStateController.class, "startValidation", pkg);
        pkg = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, pkg.getId());
        Assert.assertEquals(IS_VALID, pkg.getState());

        request1 = (AccountBatchRequest) baseEntityRepository.findById(AccountBatchRequest.class, request1.getId());
        Assert.assertEquals(AccountBatchState.VALID, request1.getState());

        request2 = (AccountBatchRequest) baseEntityRepository.findById(AccountBatchRequest.class, request2.getId());
        Assert.assertEquals(AccountBatchState.VALID, request2.getState());

        remoteAccess.invoke(AccountBatchStateController.class, "startProcess", pkg);
        pkg = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, pkg.getId());
        Assert.assertEquals(PROCESSED, pkg.getState());

        List<AccountBatchRequest> requests = baseEntityRepository.select(AccountBatchRequest.class, "from AccountBatchRequest p where p.batchPackage = ?1", pkg);
        Assert.assertTrue(requests.stream().map(r -> r.getId() + ":" + r.getState() + ":" + r.getNewAccount()).collect(Collectors.joining(" ,"))
                , requests.stream().anyMatch(r -> !isEmpty(r.getBsaAcid()) && COMPLETED == r.getState() && Y == r.getNewAccount()));

    }

    @Test public void testValidationErrorMulti() {
        final String branch = "001";
        final String ccy = "RUR";
        final String custno = "00151555";
        final String acctype = "356020202";
        final String cbcusttype = "00";
        final String term = "00";
        final String sealSrc = "IMEX";
        final String dealid = "511_A073C_18";
        final String subdealid = "00151555RURCL0P00171";

        final String b1ranch = "001";
        final String c1cy = "RUR";
        final String c1ustno = "00151555";
        final String a1cctype = "712010100";
        //final String c1bcusttype = "18";
        final String t1erm = "06";
        final String s1ealSrc = "IMEX";
//        final String d1ealid = "511_A073C_18";
//        final String s1ubdealid = "00151555RURCL0P00171";

        // готовим счет
        log.info("deleted accounts: " + baseEntityRepository.executeNativeUpdate(
                "delete from gl_acc where BRANCH = ? and CCY = ? and CUSTNO = ? and ACCTYPE = ? and CBCUSTTYPE = ? and TERM = ? and DEALID = ? and SUBDEALID = ?"
                ,branch,ccy,custno,acctype,cbcusttype,term,dealid,subdealid));

        // формируем пакет
        AccountBatchPackage pkg = createPackage();

        AccountBatchRequest request1 = createBatchRequest(pkg, 1L, branch, ccy, custno, acctype, dealid, subdealid, sealSrc, term);
        AccountBatchRequest request2 = createBatchRequest(pkg, 2L, b1ranch, c1cy, c1ustno, a1cctype, "", "", s1ealSrc, t1erm);

        // валидируем
        remoteAccess.invoke(AccountBatchStateController.class, "sendToValidation", pkg);
        pkg = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, pkg.getId());
        Assert.assertEquals(ON_VALID, pkg.getState());

        // проверяем состояние

        pkg = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, pkg.getId());
        Assert.assertEquals(ON_VALID, pkg.getState());

        remoteAccess.invoke(AccountBatchStateController.class, "startValidation", pkg);
        pkg = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, pkg.getId());
        Assert.assertEquals(ERROR, pkg.getState());
        Assert.assertTrue(pkg.getCntErrors() + "", 1 == pkg.getCntErrors());

        request1 = (AccountBatchRequest) baseEntityRepository.findById(AccountBatchRequest.class, request1.getId());
        Assert.assertEquals(AccountBatchState.ERRCHK, request1.getState());

        request2 = (AccountBatchRequest) baseEntityRepository.findById(AccountBatchRequest.class, request2.getId());
        Assert.assertEquals(AccountBatchState.VALID, request2.getState());
        Assert.assertTrue(!isEmpty(request2.getCalcPlcodeParm()));
    }

    @Test
    public void testValidation() {
        final String branch = "001";
        final String ccy = "RUR";
        final String custno = "00151555";
        final String acctype = "161020100";

        AccountBatchPackage pkg = createPackage();
        pkg.setOperday(getOperday().getLastWorkingDay());
        pkg.setState(ON_VALID);
        baseEntityRepository.update(pkg);

        AccountBatchRequest request = createBatchRequest(pkg, 1L, branch, ccy, custno, acctype, "001", "0003", "IMEX", "00");

        remoteAccess.invoke(AccountBatchStateController.class, "startValidation", pkg);

        pkg = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, pkg.getId());
        Assert.assertEquals(ERROR, pkg.getState());

    }

    private AccountBatchPackage createPackage() {

        AccountBatchPackage package1 = new AccountBatchPackage();
        package1.setOperday(getOperday().getCurrentDate());
        package1.setLoadedDate(new Date());
        package1.setState(AccountBatchPackageState.IS_LOAD);
        package1.setLoadUser("system");
        package1.setFileName("file1.xlsx");
        package1.setProcUser("er21006");
        package1 = (AccountBatchPackage) baseEntityRepository.save(package1);

        Assert.assertTrue(0 < package1.getId());

        return  (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, package1.getId());
    }

    private AccountBatchRequest createBatchRequest(AccountBatchPackage pkg, Long lineNumber, String branch, String ccy
            , String custno, String acctype, String dealId, String subdealId, String dealSrc, String term) {
        AccountBatchRequest request = new AccountBatchRequest();
        request.setBatchPackage(pkg);
        request.setLineNumber(lineNumber);
        request.setState(AccountBatchState.LOAD);
        request.setInBranch(branch);
        request.setInCcy(ccy);
        request.setInCustno(custno);
        request.setInAcctype(acctype);
        request.setInDealsrc(dealSrc);
        request.setInDealid(dealId);
        request.setInSubdealid(subdealId);
        request.setInTerm(term);
        return  (AccountBatchRequest) baseEntityRepository.save(request);
    }

}
