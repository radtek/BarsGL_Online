package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchRequest;
import ru.rbt.barsgl.shared.enums.AccountBatchPackageState;
import ru.rbt.barsgl.shared.enums.AccountBatchState;
import ru.rbt.ejbcore.mapping.YesNo;

import java.util.Date;

/**
 * Created by Ivan Sevastyanov on 16.10.2018.
 */
public class ExcelAccOpenIT extends AbstractRemoteIT {

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
        request.setBatchState(AccountBatchState.LOAD);
        request.setInBranch("001");
        request.setInCcy("RUR");
        request.setInCustno("001");
        request.setInAcctype("090909090");

        request = (AccountBatchRequest) baseEntityRepository.save(request);

        Assert.assertTrue(0 < request.getId());
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

}
