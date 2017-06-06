package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.oper.OrdinaryPostingProcessor;
import ru.rbt.barsgl.ejbtesting.test.DdlSupportBeanTesting;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;

import static ru.rbt.ejbcore.mapping.YesNo.N;
import static ru.rbt.ejbcore.mapping.YesNo.Y;

/**
 * Created by Ivan Sevastyanov Настройка определения даты проводки
 */
public class PostingDateIT extends AbstractRemoteIT {

    @AfterClass
    public static void teardownClass() {
        remoteAccess.invoke(DdlSupportBeanTesting.class, "createGlOperPodViewOld", new Object[]{});

    }

    /**
     * Вычисление даты проводки в соответствии с текущей настройкой
     *
     * @fsd
     * @throws ParseException
     */
    @Test
    public void test() throws ParseException {

        Operday operday = getOperday();

//        baseEntityRepository.executeNativeUpdate(NEW_VIEW);
        //baseEntityRepository.executeNativeUpdate(AUTH);

        remoteAccess.invoke(DdlSupportBeanTesting.class, "createGlOperPodViewNew", new Object[]{});

        GLOperation operation = new GLOperation();
        operation.setStorno(N);
        operation.setFan(N);
        operation.setAccountCredit("123");
        operation.setAmountCredit(new BigDecimal("123.00"));
        operation.setAmountDebit(new BigDecimal("123.00"));
        operation.setAccountDebit("123");
        operation.setAePostingId(System.currentTimeMillis() + "");
        operation.setEventId(System.currentTimeMillis() + "_e");
        operation.setOperationTimestamp(new Date());
        operation.setSourcePosting("srcpst");
        operation.setValueDate(getOperday().getCurrentDate());
        operation.setCurrencyCredit(BankCurrency.AUD);
        operation.setCurrencyDebit(BankCurrency.AUD);
        operation.setDealId(System.currentTimeMillis() + "_d");
        operation.setPostDate(DateUtils.parseDate("2012-12-13", "yyyy-MM-dd"));
        operation = (GLOperation) baseEntityRepository.save(operation);

        //GLOperation operation = (GLOperation) baseEntityRepository.selectFirst(GLOperation.class, "from GLOperation o order by 1 desc");
        baseEntityRepository.executeNativeUpdate("update gl_oper o set o.fan = ?, o.STRN = ? where o.gloid = ?", Y.name(), N.name(), operation.getId());
        Date pod = remoteAccess.invoke(OrdinaryPostingProcessor.class, "calculatePostingDate", operation);
        Assert.assertEquals(operday.getCurrentDate(), pod);

        baseEntityRepository.executeNativeUpdate("update gl_oper o set o.fan = ?, o.STRN = ? where o.gloid = ?", N.name(), N.name(), operation.getId());
        pod = remoteAccess.invoke(OrdinaryPostingProcessor.class, "calculatePostingDate", operation);
        Assert.assertEquals(DateUtils.parseDate("2012-12-12", "yyyy-MM-dd"), pod);

        teardownClass();
        baseEntityRepository.executeNativeUpdate("update gl_oper o set o.oper_class = ? where o.gloid = ?", GLOperation.OperClass.MANUAL.name(), operation.getId());
        pod = remoteAccess.invoke(OrdinaryPostingProcessor.class, "calculatePostingDate", operation);
        Assert.assertEquals(DateUtils.parseDate("2012-12-13", "yyyy-MM-dd"), pod);

        baseEntityRepository.executeNativeUpdate("update gl_oper o set o.oper_class = ? where o.gloid = ?", GLOperation.OperClass.AUTOMATIC.name(), operation.getId());
        pod = remoteAccess.invoke(OrdinaryPostingProcessor.class, "calculatePostingDate", operation);
        Assert.assertEquals(getOperday().getCurrentDate(), pod);

    }

}
