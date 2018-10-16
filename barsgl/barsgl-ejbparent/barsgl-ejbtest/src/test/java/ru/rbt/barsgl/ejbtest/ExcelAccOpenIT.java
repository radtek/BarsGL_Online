package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.shared.enums.AccountBatchPackageState;
import ru.rbt.ejbcore.mapping.YesNo;

import java.util.Date;

/**
 * Created by Ivan Sevastyanov on 16.10.2018.
 */
public class ExcelAccOpenIT extends AbstractRemoteIT {

    @Test public void test() {

        AccountBatchPackage package1 = new AccountBatchPackage();
        package1.setOperday(getOperday().getCurrentDate());
        package1.setLoadedDate(new Date());
        package1.setState(AccountBatchPackageState.IS_LOAD);
        package1.setLoadUser("system");
        package1.setFileName("file1.xlsx");
        package1 = (AccountBatchPackage) baseEntityRepository.save(package1);

        Assert.assertTrue(0 < package1.getId());

        package1 = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, package1.getId());
        Assert.assertEquals(YesNo.N, package1.getInvisible());

        package1.setInvisible(YesNo.Y);
        baseEntityRepository.update(package1);

        package1 = (AccountBatchPackage) baseEntityRepository.findById(AccountBatchPackage.class, package1.getId());
        Assert.assertEquals(YesNo.Y, package1.getInvisible());
    }
}
