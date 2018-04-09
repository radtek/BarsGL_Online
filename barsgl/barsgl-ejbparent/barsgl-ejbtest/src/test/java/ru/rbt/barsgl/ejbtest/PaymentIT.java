package ru.rbt.barsgl.ejbtest;

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
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.criteria.Criteria;
import ru.rbt.barsgl.shared.criteria.CriteriaBuilder;
import ru.rbt.barsgl.shared.criteria.CriteriaLogic;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.mapping.YesNo;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static ru.rbt.barsgl.shared.enums.DealSource.PaymentHub;
import static ru.rbt.ejbcore.util.StringUtils.rsubstr;

/**
 * Created by ER18837 on 05.06.15.
 * Тест заполнения полей PREF PNAR для платежей
 */
public class PaymentIT extends AbstractTimerJobIT {

    @BeforeClass
    public static void beforeClass() {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
    }

    /**
     * Тест заполнения полей PREF PNAR для платежеа в одном филиале в одной валюте
     */
    @Test
    public void testPaymentSimple() throws SQLException {
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
    public void testPaymentMfoExch() throws SQLException {
        long st = System.currentTimeMillis();

        final String parentRef = "PM_" + ("" + System.currentTimeMillis()).substring(6);

        Date operday = getOperday().getCurrentDate();
        List<EtlPosting> etlList = createMfoExchPostings(parentRef);
        List<GLOperation> operList = processPostings(etlList);
        Assert.assertEquals(2, operList.size());

        checkOperations(operList);
    }

    private List<EtlPosting> createSimplePostings(String parentRef) throws SQLException {
        long st = System.currentTimeMillis();
        EtlPackage etlPackage1 = newPackageNotSaved(st, "Checking payment posting logic simple");
        etlPackage1.setPostingCnt(2);                        // число операций
        etlPackage1 = (EtlPackage) baseEntityRepository.save(etlPackage1);

        String acc1 = findBsaAccount("40806810_0001%");
        String acc2 = findBsaAccount("40702810_0001%");
        String acc3 = findBsaAccount("40702810_0001%", getOperday().getCurrentDate(), notBsaAcid(acc2));
        List<EtlPosting> etlList = new ArrayList<EtlPosting>();
        // основная проводка
        EtlPosting etlPosting = createFanPosting(st, etlPackage1, acc1  // "40806810700010000465"
                , acc2, new BigDecimal("100.12"), BankCurrency.RUB      // "40702810100013995679"
                , new BigDecimal("100.12"), BankCurrency.RUB, parentRef, parentRef, YesNo.N);
        etlPosting.setSourcePosting(PaymentHub.getLabel());
        etlList.add(etlPosting);

        // неосновная проводка (счета открыты в одном филиале)
        etlPosting = createFanPosting(st, etlPackage1, acc1             // "40806810700010000465"
                , acc3, new BigDecimal("101.13"), BankCurrency.RUB      //"40702810900010002613"
                , new BigDecimal("101.13"), BankCurrency.RUB, parentRef + "_CHR", parentRef, YesNo.N);
        etlPosting.setSourcePosting(PaymentHub.getLabel());
        etlList.add(etlPosting);

        return etlList;
    }

    private List<EtlPosting> createMfoExchPostings (String parentRef) throws SQLException {
        long st = System.currentTimeMillis();
        EtlPackage etlPackage = newPackageNotSaved(st, "Checking payment posting logic with MFO with Exchange");
        etlPackage.setPostingCnt(2);                        // число перьев в веере
        etlPackage = (EtlPackage) baseEntityRepository.save(etlPackage);

        String acc1 = findBsaAccount("40806810_0001%");
        String acc2 = findBsaAccount("40702810_0001%");
        String acc3 = findBsaAccount("47427978_0040%");
        List<EtlPosting> etlList = new ArrayList<EtlPosting>();
        // основная проводка
        EtlPosting etlPosting = createFanPosting(st, etlPackage, acc1       // "40806810700010000465"
                , acc2, new BigDecimal("100.12"), BankCurrency.RUB          //  "40702810100013995679"
                , new BigDecimal("100.12"), BankCurrency.RUB, parentRef, parentRef, YesNo.N);
        etlPosting.setSourcePosting(PaymentHub.getLabel());
        etlList.add(etlPosting);

        // неосновная проводка (счета открыты в разных филиалах + курсовая разница)
        etlPosting = createFanPosting(st, etlPackage, acc1                  // "40806810700010000465"
                , acc3, new BigDecimal("100.12"), BankCurrency.RUB          // "47427978400404502369"
                , new BigDecimal("5.23"), BankCurrency.EUR, parentRef + "_CHR", parentRef, YesNo.N);
        etlPosting.setSourcePosting(PaymentHub.getLabel());
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

    protected Criteria notBsaAcid(String bsaacid) {
        return CriteriaBuilder.create(CriteriaLogic.AND).appendNOT("bsaacid", bsaacid).build();
    }
}
