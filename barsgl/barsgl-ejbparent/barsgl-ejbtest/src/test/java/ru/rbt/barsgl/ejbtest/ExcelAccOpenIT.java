package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.acc.AccountBatchStateController;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchRequest;
import ru.rbt.barsgl.shared.enums.AccountBatchPackageState;
import ru.rbt.barsgl.shared.enums.AccountBatchState;
import ru.rbt.ejbcore.mapping.YesNo;

import java.util.Date;
import java.util.logging.Logger;

import static ru.rbt.barsgl.shared.enums.AccountBatchPackageState.IS_VALID;

/**
 * Created by Ivan Sevastyanov on 16.10.2018.
 */
public class ExcelAccOpenIT extends AbstractRemoteIT {

    private static final Logger log = Logger.getLogger(ExcelAccOpenIT.class.getName());

    @Test public void test() {

        AccountBatchPackage package1 = createPackage();
        package1 = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, package1.getId());
        Assert.assertEquals(YesNo.N, package1.getInvisible());

        package1.setInvisible(YesNo.Y);
        baseEntityRepository.update(package1);

        package1 = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, package1.getId());
        Assert.assertEquals(YesNo.Y, package1.getInvisible());
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

        request = (AccountBatchRequest) baseEntityRepository.save(request);

        Assert.assertTrue(0 < request.getId());
    }


    @Test public void testProcess() {

        final String branch = "001";
        final String ccy = "RUR";
        final String custno = "00151555";
        final String acctype = "161020100";
        final String cbcusttype = "18";
        final String term = "7";
        final String dealid = "511_A073C_18";
        final String subdealid = "00151555RURCL0P00171";

        // готовим счет
        log.info("deleted accounts: " + baseEntityRepository.executeNativeUpdate(
                "delete from gl_acc where BRANCH = ? and CCY = ? and CUSTNO = ? and ACCTYPE = ? and CBCUSTTYPE = ? and TERM = ? and DEALID = ? and SUBDEALID = ?"
            ,branch,ccy,custno,acctype,cbcusttype,term,dealid,subdealid));

        // формируем пакет
        AccountBatchPackage pkg = createPackage();

        AccountBatchRequest request = createBatchRequest(pkg, 1L, branch, ccy, custno, acctype);

        // валидируем
        remoteAccess.invoke(AccountBatchStateController.class, "startValidation", pkg);

        // проверяем состояние

        pkg = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, pkg.getId());
        Assert.assertEquals(IS_VALID, pkg.getState());

        request = (AccountBatchRequest) baseEntityRepository.findById(AccountBatchRequest.class, request.getId());
        Assert.assertEquals(AccountBatchState.VALID, request.getState());

        // обрабатываем

    }

    private AccountBatchPackage createPackage() {

        AccountBatchPackage package1 = new AccountBatchPackage();
        package1.setOperday(getOperday().getCurrentDate());
        package1.setLoadedDate(new Date());
        package1.setState(AccountBatchPackageState.IS_LOAD);
        package1.setLoadUser("system");
        package1.setFileName("file1.xlsx");
        package1 = (AccountBatchPackage) baseEntityRepository.save(package1);

        Assert.assertTrue(0 < package1.getId());

        return  (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, package1.getId());
    }

    private AccountBatchRequest createBatchRequest(AccountBatchPackage pkg, Long lineNumber, String branch, String ccy, String custno, String acctype) {
        AccountBatchRequest request = new AccountBatchRequest();
        request.setBatchPackage(pkg);
        request.setLineNumber(lineNumber);
        request.setState(AccountBatchState.LOAD);
        request.setInBranch(branch);
        request.setInCcy(ccy);
        request.setInCustno(custno);
        request.setInAcctype(acctype);
        return  (AccountBatchRequest) baseEntityRepository.save(request);
    }

}
