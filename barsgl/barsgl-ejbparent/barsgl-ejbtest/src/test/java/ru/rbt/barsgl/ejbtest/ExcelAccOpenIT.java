package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.BankCalendarDay;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.controller.acc.AccountBatchStateController;
import ru.rbt.barsgl.ejb.controller.operday.task.AccountBatchOpenTask;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchRequest;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.shared.enums.AccountBatchPackageState;
import ru.rbt.barsgl.shared.enums.AccountBatchState;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.shared.enums.AccountBatchPackageState.*;
import static ru.rbt.barsgl.shared.enums.AccountBatchState.*;
import static ru.rbt.ejbcore.mapping.YesNo.Y;
import static ru.rbt.ejbcore.util.DateUtils.addDays;
import static ru.rbt.ejbcore.util.DateUtils.onlyDate;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by Ivan Sevastyanov on 16.10.2018.
 */
public class ExcelAccOpenIT extends AbstractRemoteIT {

    private static final Logger log = Logger.getLogger(ExcelAccOpenIT.class.getName());

    @BeforeClass
    public static void beforeClass() {
        try {
            Date curr = DateUtils.parseDate("15.01.2019", "dd.MM.yyyy");
            Date prev = DateUtils.parseDate("14.01.2019", "dd.MM.yyyy");
            setOperday(curr, prev, ONLINE, OPEN);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

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
        request.setState(LOAD);
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

        // PL счет
        final String b1ranch = "001";
        final String c1cy = "RUR";
        final String c1ustno = "00151555";
        final String a1cctype = "712010100";
        final String t1erm = "06";
        final String s1ealSrc = "IMEX";

        // готовим счет
        log.info("deleted accounts: " + baseEntityRepository.executeNativeUpdate(
                "delete from gl_acc where BRANCH = ? and CCY = ? and CUSTNO = ? and ACCTYPE = ? and CBCUSTTYPE = ? and TERM = ? and DEALID = ? and SUBDEALID = ?"
            ,branch,ccy,custno,acctype,cbcusttype,term,dealId,subdealid));

        // формируем пакет
        AccountBatchPackage pkg = createPackage();

        AccountBatchRequest request1 = createBatchRequest(pkg, 1L, branch, ccy, custno, acctype, dealId, subdealid, sealSrc, term);
        AccountBatchRequest request2 = createBatchRequest(pkg, 2L, branch, ccy, custno, acctype, dealId, subdealid, sealSrc, "05");
        AccountBatchRequest request3 = createBatchRequest(pkg, 3L, b1ranch, c1cy, c1ustno, a1cctype, "", "", s1ealSrc, t1erm);

        // проверяем дату в будущем
        final Date afterDate = addDays(getOperday().getCurrentDate(), 5);
        final Date inOpendate = Optional.ofNullable((BankCalendarDay)remoteAccess
                .invoke(BankCalendarDayRepository.class, "getWorkdayAfter", afterDate))
                .map(b -> b.getId().getCalendarDate())
                .orElseThrow(() -> new RuntimeException(format("Error on calc open date after '%s'", onlyDate(afterDate))));

        baseEntityRepository.executeUpdate("update AccountBatchRequest r set r.inOpendate = ?1 where r in (from AccountBatchRequest r2 where r2.batchPackage = ?2)"
            , inOpendate, pkg);

        // валидируем
        remoteAccess.invoke(AccountBatchStateController.class, "sendToValidation", pkg);

        // проверяем состояние

        pkg = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, pkg.getId());
        Assert.assertEquals(ON_VALID, pkg.getState());

        remoteAccess.invoke(AccountBatchStateController.class, "startValidation", pkg);
        pkg = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, pkg.getId());
        Assert.assertEquals(IS_VALID, pkg.getState());

        request1 = findRequest(request1.getId());
        Assert.assertEquals(AccountBatchState.VALID, request1.getState());

        request2 = findRequest(request2.getId());
        Assert.assertEquals(AccountBatchState.VALID, request2.getState());

        request3 = findRequest(request3.getId());
        Assert.assertEquals(AccountBatchState.VALID, request3.getState());

        remoteAccess.invoke(AccountBatchStateController.class, "startProcess", pkg);
        pkg = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, pkg.getId());
        Assert.assertEquals(PROCESSED, pkg.getState());

        List<AccountBatchRequest> requests = baseEntityRepository.select(AccountBatchRequest.class, "from AccountBatchRequest p where p.batchPackage = ?1", pkg);
        Assert.assertTrue(requests.stream().map(r -> r.getId() + ":" + r.getState() + ":" + r.getNewAccount()).collect(Collectors.joining(" ,"))
                , requests.stream().allMatch(r -> !isEmpty(r.getBsaAcid()) && COMPLETED == r.getState()));

        log.info("update: " + baseEntityRepository.executeNativeUpdate("update gl_acbatreq set glacid = null"));
        log.info("deleted: " + baseEntityRepository.executeNativeUpdate("delete from gl_acc where bsaacid in (select bsaacid from GL_ACBATREQ r where r.id_pkg = ?)", pkg.getId()));

        baseEntityRepository.executeNativeUpdate("update gl_acbatpkg set state = ?, TS_STARTV = null, TS_ENDV = null, TS_STARTP = null, TS_ENDP = null, CNT_ERR = null, CNT_FOUND = null where id_pkg = ?"
            , ON_VALID.name(), pkg.getId());

        baseEntityRepository.executeNativeUpdate("update gl_acbatreq set state = ?, bsaacid = null where id_pkg = ?", LOAD.name(), pkg.getId());

        // повторная обработка
        remoteAccess.invoke(AccountBatchStateController.class, "startValidation", pkg);
        pkg = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, pkg.getId());
        Assert.assertEquals(IS_VALID, pkg.getState());
        requests = baseEntityRepository.select(AccountBatchRequest.class, "from AccountBatchRequest p where p.batchPackage = ?1", pkg);
        Assert.assertTrue(requests.stream().allMatch(r -> r.getState() == VALID));

        remoteAccess.invoke(AccountBatchStateController.class, "startProcess", pkg);
        pkg = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, pkg.getId());
        Assert.assertEquals(PROCESSED, pkg.getState());
        requests = baseEntityRepository.select(AccountBatchRequest.class, "from AccountBatchRequest p where p.batchPackage = ?1", pkg);
        Assert.assertTrue(requests.stream().allMatch(r -> r.getState() == COMPLETED  && Y == r.getNewAccount()));

        List<DataRecord> accounts = baseEntityRepository.select("select * from gl_acc where bsaacid in (select bsaacid from GL_ACBATREQ r where r.id_pkg = ?)", pkg.getId());
        Assert.assertEquals(3, accounts.size());
        Assert.assertTrue(accounts.stream().allMatch(r -> r.getDate("dto").equals(inOpendate)));

        pkg = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, pkg.getId());
        Assert.assertTrue(pkg.getCntFound()+"", 0 == pkg.getCntFound());
    }

    private AccountBatchRequest findRequest(long requestId) {
        return (AccountBatchRequest) baseEntityRepository.findById(AccountBatchRequest.class, requestId);
    }

    @Test public void testValidationErrorMulti() {
        final String branch = "001";
        final String ccy = "RUR";
        final String custno = "151555";
        final String acctype = "356020202";
        final String cbcusttype = "00";
        final String term = "00";
        final String sealSrc = "IMEX";
        final String dealid = "511_A073C_18";
        final String subdealid = "00151555RURCL0P00171";

        // PL account
        final String b1ranch = "001";
        final String c1cy = "RUR";
        final String c1ustno = "00151555";
        final String a1cctype = "712010100";
        final String t1erm = "06";
        final String s1ealSrc = "IMEX";

        // готовим счет
        log.info("deleted accounts: " + baseEntityRepository.executeNativeUpdate(
                "delete from gl_acc where BRANCH = ? and CCY = ? and CUSTNO = ? and ACCTYPE = ? and CBCUSTTYPE = ? and TERM = ? and DEALID = ? and SUBDEALID = ?"
                ,branch,ccy,custno,acctype,cbcusttype,term,dealid,subdealid));

        // формируем пакет
        AccountBatchPackage pkg = createPackage();

        AccountBatchRequest request1 = createBatchRequest(pkg, 1L, branch, ccy, custno, acctype, dealid, subdealid, sealSrc, term);
        AccountBatchRequest request2 = createBatchRequest(pkg, 2L, b1ranch, c1cy, c1ustno, a1cctype, "", "", s1ealSrc, t1erm);
        AccountBatchRequest request3 = createBatchRequest(pkg, 3L, b1ranch, c1cy, c1ustno, a1cctype, "", "", s1ealSrc, t1erm.replaceAll("0",""));

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
        Assert.assertTrue(pkg.getCntErrors() + "", 2 == pkg.getCntErrors());

        request1 = (AccountBatchRequest) baseEntityRepository.findById(AccountBatchRequest.class, request1.getId());
        Assert.assertEquals(AccountBatchState.ERRCHK, request1.getState());

        request2 = (AccountBatchRequest) baseEntityRepository.findById(AccountBatchRequest.class, request2.getId());
        Assert.assertEquals(AccountBatchState.VALID, request2.getState());
        Assert.assertTrue(!isEmpty(request2.getCalcPlcodeParm()));

        request3 = (AccountBatchRequest) baseEntityRepository.findById(AccountBatchRequest.class, request3.getId());
        Assert.assertEquals(AccountBatchState.ERRCHK, request3.getState());
        Assert.assertTrue(request3.getErrorMessage().contains("GL_DICTERM"));
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

    @Test public void testTask() throws Exception {

//        AccountBatchOpenTask
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

        // PL счет
        final String b1ranch = "001";
        final String c1cy = "RUR";
        final String c1ustno = "00151555";
        final String a1cctype = "712010100";
        final String t1erm = "06";
        final String s1ealSrc = "IMEX";

        // формируем пакет
        AccountBatchPackage pkg = createPackage();

        createBatchRequest(pkg, 1L, branch, ccy, custno, acctype, dealId, subdealid, sealSrc, term);
        createBatchRequest(pkg, 2L, branch, ccy, custno, acctype, dealId, subdealid, sealSrc, "05");
        createBatchRequest(pkg, 3L, b1ranch, c1cy, c1ustno, a1cctype, "", "", s1ealSrc, t1erm);

        // валидируем
        remoteAccess.invoke(AccountBatchStateController.class, "sendToValidation", pkg);
        pkg = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, pkg.getId());
        Assert.assertEquals(ON_VALID, pkg.getState());

        SingleActionJob job = SingleActionJobBuilder.create().withClass(AccountBatchOpenTask.class).build();
        jobService.executeJob(job);

        // проверяем состояние
        pkg = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, pkg.getId());
        Assert.assertEquals(PROCESSED, pkg.getState());
        List<AccountBatchRequest> requests = baseEntityRepository.select(AccountBatchRequest.class, "from AccountBatchRequest p where p.batchPackage = ?1", pkg);
        Assert.assertTrue(requests.stream().map(r -> r.getId() + ":" + r.getState() + ":" + r.getNewAccount()).collect(Collectors.joining(" ,"))
                , requests.stream().allMatch(r -> !isEmpty(r.getBsaAcid()) && COMPLETED == r.getState()));
    }

    @Test
    public void testActparmDate() throws Exception {
        DataRecord data = baseEntityRepository.selectFirst("select a1.acctype, a1.custype, a1.term, a1.acc2 acc2_new, a2.acc2 acc2_old " +
                "from gl_actparm a1 join gl_actparm a2 on a2.acctype = a1.acctype and a1.custype = a2.custype and a1.term = a2.term and not a2.dte is null " +
                "where a1.custype = '00' and a1.term = '00' and a1.dtb = '2019-01-01' and (a1.acc2 <> a2.acc2 )");
        final String acctype = data.getString("acctype");
        final String cbcusttype = data.getString("custype");
        final String term = data.getString("term");
        final String acc2[] = new String[3];
        acc2[0] = data.getString("acc2_old");
        acc2[2] = acc2[1] = data.getString("acc2_new");

        final String branch = "001";
        final String ccy = "RUR";
        final String custno = "00151555";
        final String dealSrc = "IMEX";
        final String dealid = "dealid";
        final String subdealid = "subdealid";

        // удаляем старые счета
        String sqlAcc = "from gl_acc a where a.BRANCH = ? and a.CCY = ? and a.CUSTNO = ? and a.ACCTYPE = ? and a.TERM = ? and a.DEALID like ? and a.SUBDEALID like ?";
        baseEntityRepository.executeNativeUpdate("delete from GL_ACBATREQ r where exists (select 1 " + sqlAcc + ")", branch, ccy, custno, acctype, term, dealid+"%", subdealid+"%");
        log.info("deleted accounts: " + baseEntityRepository.executeNativeUpdate("delete " + sqlAcc, branch, ccy, custno, acctype, term, dealid+"%", subdealid+"%"));

        // формируем пакет
        AccountBatchPackage pkg = createPackage();

        Date openDates[] = new Date[3];
        openDates[0] = DateUtils.parseDate("20.12.2018", "dd.MM.yyyy");
        openDates[1] = DateUtils.parseDate("10.01.2019", "dd.MM.yyyy");
        openDates[2] = null;

        for (int i=0; i<3; i++) {
            createBatchRequest(pkg, (long)i, branch, ccy, custno, acctype, dealid + i, subdealid + i, dealSrc, term, openDates[i]);
        }

        // на валидацию
        remoteAccess.invoke(AccountBatchStateController.class, "sendToValidation", pkg);
        pkg = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, pkg.getId());
        Assert.assertEquals(ON_VALID, pkg.getState());

        SingleActionJob job = SingleActionJobBuilder.create().withClass(AccountBatchOpenTask.class).build();
        jobService.executeJob(job);

        // проверяем состояние
        pkg = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, pkg.getId());
        Assert.assertEquals(PROCESSED, pkg.getState());
        List<AccountBatchRequest> requests = baseEntityRepository.select(AccountBatchRequest.class, "from AccountBatchRequest p where p.batchPackage = ?1 order by p.id", pkg);
        Assert.assertTrue(requests.stream().map(r -> r.getId() + ":" + r.getState() + ":" + r.getNewAccount()).collect(Collectors.joining(" ,"))
                , requests.stream().allMatch(r -> !isEmpty(r.getBsaAcid()) && COMPLETED == r.getState()));

        List<DataRecord> accList = baseEntityRepository.select("select recno, r.bsaacid, a.acc2, a.dto from gl_acbatreq r join gl_acc a on r.bsaacid = a.bsaacid" +
                " where r.id_pkg = ? order by recno", pkg.getId());
        Assert.assertEquals(3, accList.size());

        openDates[2] = getOperday().getCurrentDate();
        for (int i=0; i<3; i++) {
            Assert.assertEquals(String.format("Неверный счет второго порядка для счета %d", i), acc2[i], accList.get(i).getString("acc2"));
            Assert.assertEquals(String.format("Неверная дата открытия для счета %d", i), openDates[i], accList.get(i).getDate("dto"));
        }
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

    private AccountBatchRequest createBatchRequestNotSaved(AccountBatchPackage pkg, Long lineNumber, String branch, String ccy
            , String custno, String acctype, String dealId, String subdealId, String dealSrc, String term, Date openDate) {
        AccountBatchRequest request = new AccountBatchRequest();
        request.setBatchPackage(pkg);
        request.setLineNumber(lineNumber);
        request.setState(LOAD);
        request.setInBranch(branch);
        request.setInCcy(ccy);
        request.setInCustno(custno);
        request.setInAcctype(acctype);
        request.setInDealsrc(dealSrc);
        request.setInDealid(dealId);
        request.setInSubdealid(subdealId);
        request.setInTerm(term);
        request.setInOpendate(openDate);
        return request;
    }

    private AccountBatchRequest createBatchRequest(AccountBatchPackage pkg, Long lineNumber, String branch, String ccy
            , String custno, String acctype, String dealId, String subdealId, String dealSrc, String term, Date openDate) {
        AccountBatchRequest request = createBatchRequestNotSaved(pkg, lineNumber, branch, ccy
                , custno, acctype, dealId, subdealId, dealSrc, term, openDate);
        return  (AccountBatchRequest) baseEntityRepository.save(request);
    }

    private AccountBatchRequest createBatchRequest(AccountBatchPackage pkg, Long lineNumber, String branch, String ccy
            , String custno, String acctype, String dealId, String subdealId, String dealSrc, String term) {
/*
        AccountBatchRequest request = new AccountBatchRequest();
        request.setBatchPackage(pkg);
        request.setLineNumber(lineNumber);
        request.setState(LOAD);
        request.setInBranch(branch);
        request.setInCcy(ccy);
        request.setInCustno(custno);
        request.setInAcctype(acctype);
        request.setInDealsrc(dealSrc);
        request.setInDealid(dealId);
        request.setInSubdealid(subdealId);
        request.setInTerm(term);
*/
        AccountBatchRequest request = createBatchRequestNotSaved(pkg, lineNumber, branch, ccy
                , custno, acctype, dealId, subdealId, dealSrc, term, null);
        return  (AccountBatchRequest) baseEntityRepository.save(request);
    }

}
