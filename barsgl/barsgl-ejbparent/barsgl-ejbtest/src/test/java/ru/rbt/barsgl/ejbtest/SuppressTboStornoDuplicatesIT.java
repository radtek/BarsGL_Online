package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.dict.SourcesDeals;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejb.integr.oper.SuppressStornoTboController;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static ru.rbt.barsgl.shared.enums.DealSource.KondorPlus;
import static ru.rbt.ejbcore.mapping.YesNo.Y;

/**
 * Created by ER21006 on 15.01.2016.
 */
public class SuppressTboStornoDuplicatesIT extends AbstractRemoteIT {

    @BeforeClass
    public static void init() {
        initCorrectOperday();
    }

    /**
     * Подавление дублирующих проводок по сделкам TBO
     * выполняется в PRE_COB в самом конце
     */
    @Test
    public void testSuppress() {

        Date firstOperday = getOperday().getCurrentDate();
//        baseEntityRepository.executeNativeUpdate("update gl_oper set vdate = vdate - 10, procdate = procdate - 10");

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = createPosting(stamp, pkg);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());       // операция создана
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(operation.getState(), OperState.POST);
        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);                 // 1 проводка
        Assert.assertEquals(postList.size(), 1);

        // Сторно операция - сдвигем день вперед на 1
        setOperday(DateUtils.addDays(firstOperday, 1), firstOperday, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.CLOSED);
        stamp = System.currentTimeMillis();
        pkg = newPackage(stamp, "SimpleStornoBack");
        Assert.assertTrue(pkg.getId() > 0);
        EtlPosting pstStrn = createStornoPosting(stamp, pkg, pst, firstOperday);

        GLOperation stornoOper = (GLOperation) postingController.processMessage(pstStrn);
        Assert.assertTrue(0 < stornoOper.getId());       // операция создана

        stornoOper = (GLOperation) baseEntityRepository.findById(stornoOper.getClass(), stornoOper.getId());
        Assert.assertEquals(stornoOper.getState(), OperState.POST);
        Assert.assertEquals(stornoOper.getStornoOperation().getId(), operation.getId());        // ссылка на сторно операцию

        List<GLPosting> postListStrn = getPostings(stornoOper);
        Assert.assertNotNull(postListStrn);                 // 1 проводка
        Assert.assertEquals(postListStrn.size(), 1);

        // создаем проводку аналогичную прямой - не сторно
        EtlPosting grandPosting = pst;
        grandPosting.setId(baseEntityRepository.nextId("GL_SEQ_PST"));
        grandPosting = (EtlPosting) baseEntityRepository.save(grandPosting);

        GLOperation grandOper = (GLOperation) postingController.processMessage(grandPosting);
        Assert.assertTrue(0 < grandOper.getId());       // операция создана
        final int result = remoteAccess.invoke(SuppressStornoTboController.class, "suppress");
        Assert.assertTrue(result + "", 2 == result);

        stornoOper = (GLOperation) baseEntityRepository.findById(stornoOper.getClass(), stornoOper.getId());
        Assert.assertEquals(stornoOper.getState(), OperState.INVISIBLE);

        grandOper = (GLOperation) baseEntityRepository.findById(grandOper.getClass(), grandOper.getId());
        Assert.assertEquals(grandOper.getState(), OperState.INVISIBLE);

        Assert.assertTrue(findPds(stornoOper).stream().allMatch(pd -> pd.getInvisible().equals("1")));
        Assert.assertTrue(findPds(grandOper).stream().allMatch(pd -> pd.getInvisible().equals("1")));

    }

    private EtlPosting createPosting(long stamp, EtlPackage pkg) {
        EtlPosting pst = newPosting(stamp, pkg);
        Date operday = getOperday().getCurrentDate();
        pst.setValueDate(operday);

        pst.setAccountCredit("40817036200012959997");
        pst.setAccountDebit("40817036250010000018");
        pst.setAmountCredit(new BigDecimal("12.006"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setDealId(StringUtils.rsubstr(Long.toString(stamp), 4));
        pst.setSourcePosting(KondorPlus.getLabel());
        return (EtlPosting) baseEntityRepository.save(pst);
    }

    private EtlPosting createStornoPosting(long stamp, EtlPackage pkg, EtlPosting pst, Date firstOperday) {
        EtlPosting pstS = newStornoPosting(stamp, pkg, pst);
        pstS.setValueDate(firstOperday);
        pstS.setStorno(Y);
        pstS.setSourcePosting(pst.getSourcePosting());
        return (EtlPosting) baseEntityRepository.save(pstS);
    }

    private List<Pd> findPds(GLOperation operation) {
       GLPosting posting =  (GLPosting) baseEntityRepository.selectOne(GLPosting.class, "from GLPosting gp where gp.operation = ?1", operation);
       Assert.assertNotNull(posting);

       List<Pd> pds = baseEntityRepository.select(Pd.class, "from Pd p where p.pcId = ?1", posting.getId());
       Assert.assertNotNull(pds);
       Assert.assertFalse(pds.isEmpty());

        return pds;
    }
}
