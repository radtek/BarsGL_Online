package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.shared.enums.OperState;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static ru.rbt.ejbcore.util.StringUtils.rsubstr;

/**
 * Created by ER18837 on 05.06.15.
 * Тест заполнения полей PREF PNAR для платежей
 */
public class PaymentTest extends AbstractTimerJobTest {

    @BeforeClass
    public static void beforeClass() {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
    }

    /**
     * Тест заполнения полей PREF PNAR для платежеа в одном филиале в одной валюте
     */
    @Test
    public void testPaymentSimple() {
        long st = System.currentTimeMillis();

        final String parentRef = "PM_" + ("" + System.currentTimeMillis()).substring(6);

        Date operday = getOperday().getCurrentDate();
        List<EtlPosting> etlList = createSimplePostings(parentRef);
        List<GLOperation> operList = processPostings(etlList);
        Assert.assertEquals(2, operList.size());

        checkOperations(operList);
    }

    /**
     * Тест заполнения полей PREF PNAR для межфилиального платежеа с курсовой разницей
     */
    @Test
    public void testPaymentMfoExch() {
        long st = System.currentTimeMillis();

        final String parentRef = "PM_" + ("" + System.currentTimeMillis()).substring(6);

        Date operday = getOperday().getCurrentDate();
        List<EtlPosting> etlList = createMfoExchPostings(parentRef);
        List<GLOperation> operList = processPostings(etlList);
        Assert.assertEquals(2, operList.size());

        checkOperations(operList);
    }

    private List<EtlPosting> createSimplePostings(String parentRef) {
        long st = System.currentTimeMillis();
        EtlPackage etlPackage1 = newPackageNotSaved(st, "Checking payment posting logic simple");
        etlPackage1.setPostingCnt(2);                        // число операций
        etlPackage1 = (EtlPackage) baseEntityRepository.save(etlPackage1);

        List<EtlPosting> etlList = new ArrayList<EtlPosting>();
        // основная проводка
        EtlPosting etlPosting = createFanPosting(st, etlPackage1, "40806810700010000465"
                , "40702810100013995679", new BigDecimal("100.12"), BankCurrency.RUB
                , new BigDecimal("100.12"), BankCurrency.RUB, parentRef, parentRef, YesNo.N);
        etlPosting.setSourcePosting(GLOperation.srcPaymentHub);
        etlList.add(etlPosting);

        // неосновная проводка (счета открыты в одном филиале)
        etlPosting = createFanPosting(st, etlPackage1, "40806810700010000465"
                , "40702810900010002613", new BigDecimal("101.13"), BankCurrency.RUB
                , new BigDecimal("101.13"), BankCurrency.RUB, parentRef + "_CHR", parentRef, YesNo.N);
        etlPosting.setSourcePosting(GLOperation.srcPaymentHub);
        etlList.add(etlPosting);

        return etlList;
    }

    private List<EtlPosting> createMfoExchPostings (String parentRef) {
        long st = System.currentTimeMillis();
        EtlPackage etlPackage = newPackageNotSaved(st, "Checking payment posting logic with MFO with Exchange");
        etlPackage.setPostingCnt(2);                        // число перьев в веере
        etlPackage = (EtlPackage) baseEntityRepository.save(etlPackage);

        List<EtlPosting> etlList = new ArrayList<EtlPosting>();
        // основная проводка
        EtlPosting etlPosting = createFanPosting(st, etlPackage, "40806810700010000465"
                , "40702810100013995679", new BigDecimal("100.12"), BankCurrency.RUB
                , new BigDecimal("100.12"), BankCurrency.RUB, parentRef, parentRef, YesNo.N);
        etlPosting.setSourcePosting(GLOperation.srcPaymentHub);
        etlList.add(etlPosting);

        // неосновная проводка (счета открыты в разных филиалах + курсовая разница)
        etlPosting = createFanPosting(st, etlPackage, "40806810700010000465"
                , "47427978400404502369", new BigDecimal("100.12"), BankCurrency.RUB
                , new BigDecimal("5.23"), BankCurrency.EUR, parentRef + "_CHR", parentRef, YesNo.N);
        etlPosting.setSourcePosting(GLOperation.srcPaymentHub);
        etlList.add(etlPosting);

        return etlList;
    }

    private void checkOperations(List<GLOperation> operList) {
        int i = 0;
        for (GLOperation oper : operList) {
            oper = (GLOperation) baseEntityRepository.findById(oper.getClass(), oper.getId());
            Assert.assertEquals(OperState.POST, oper.getState());

            String pref = rsubstr(oper.hasParent() ? oper.getParentReference() : oper.getPaymentRefernce(), 15);
            String pnar = getPnar(oper);
//            if (oper.isChild()) {
//                pnar = (oper.isStorno() ? "*" : "") + "CHARGE " + pref;
//            } else {
//                if(InputMethod.AE == oper.getInputMethod())
//                    pnar = substr(oper.getNarrative(), 30); // для AE - из NRT
//                else                    
//                    pnar = substr(oper.getRusNarrativeShort(), 30);
//            }

            List<GLPosting> postList = getPostings(oper);
            Assert.assertNotNull(postList);

            for (GLPosting post : postList) {
                List<Pd> pdList = getPostingPd(post);
                Assert.assertNotNull(pdList);
                for(Pd pd : pdList) {
                    Assert.assertEquals(pref, pd.getPref());
                    Assert.assertEquals(pnar, pd.getPnar());
                }
            }
            i++;
        }
    }
}
