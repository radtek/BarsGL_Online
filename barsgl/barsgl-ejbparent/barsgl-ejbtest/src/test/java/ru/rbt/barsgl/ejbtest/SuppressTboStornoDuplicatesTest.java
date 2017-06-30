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
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.barsgl.shared.enums.OperState;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static ru.rbt.barsgl.shared.enums.DealSource.KondorPlus;
import static ru.rbt.ejbcore.mapping.YesNo.Y;

/**
 * Created by ER21006 on 15.01.2016.
 */
public class SuppressTboStornoDuplicatesTest extends AbstractRemoteTest {

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

        baseEntityRepository.executeNativeUpdate("update gl_oper set vdate = vdate - 10 day, procdate = procdate - 10 day");

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        Date firstOperday = getOperday().getCurrentDate();
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
        pst.setAmountCredit(new BigDecimal("12.0056"));
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
        return baseEntityRepository.findNative(Pd.class,
                "select * from pd d where exists (select 1 from gl_posting p where p.glo_ref = ?1 and p.pcid = d.pcid)", 100, operation.getId());
    }

}
