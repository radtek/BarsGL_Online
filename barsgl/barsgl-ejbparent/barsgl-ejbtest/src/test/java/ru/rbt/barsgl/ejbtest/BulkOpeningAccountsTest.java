/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2017
 */
package ru.rbt.barsgl.ejbtest;

import java.util.Properties;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.BulkOpeningAccountsTask;
import ru.rbt.barsgl.ejb.controller.operday.task.LoadCurratesTask;
import static ru.rbt.barsgl.ejbtest.AbstractRemoteTest.baseEntityRepository;
import static ru.rbt.barsgl.ejbtest.AbstractRemoteTest.remoteAccess;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;

/**
 *
 * @author Andrew Samsonov
 */
public class BulkOpeningAccountsTest extends AbstractRemoteTest{
    @Test
    public void bulkOpeningAccount(){
        baseEntityRepository.executeNativeUpdate(
                "insert into dwh.gl_openacc values "
                        + "('001','00000018','RUR','3213','01','3213','01','90702','851030300','Бланки собственных векселей Банка','2017-05-26')");

        remoteAccess.invoke(BulkOpeningAccountsTask.class, "bulkOpeningAccounts");        
    }
}
