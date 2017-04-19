package ru.rbt.barsgl.ejbtest;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.barsgl.ejbtest.AbstractTimerJobTest;
import ru.rbt.barsgl.ejbtest.EtlStructureMonitorTest;

import java.math.BigDecimal;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Created by ER22317 on 20.12.2016.
 */
public class EtlMessFor707Test extends AbstractTimerJobTest {
    private static final Logger log = Logger.getLogger(EtlStructureMonitorTest.class.getName());

//    @Before
//    public void beforeClass() {
//        initCorrectOperday();
//    }

    @Test
    @Ignore
    public void etl707Test() throws Exception {
        Long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackageNotSaved(stamp, "Тестовый пакет " + stamp);
        pkg.setDateLoad(getSystemDateTime());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(3);
        pkg = (EtlPackage) baseEntityRepository.save(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("20461643A");
        pst1.setEventId("201264A");
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Reserve creation");
        pst1.setRusNarrativeLong("Формирование резервов");
        pst1.setRusNarrativeShort("Формирование резервов");
        pst1.setStorno(YesNo.N);
        pst1.setAccountCredit("47425810400014560229");
//        pst1.setAccountDebit("20202840000010609639");
        pst1.setAmountCredit(new BigDecimal("4380.450"));
        pst1.setAmountDebit(new BigDecimal("4380.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setValueDate(DateUtils.dbDateParse("2016-12-31"));
        pst1.setDealId("47423810000014544715");
        pst1.setSourcePosting("ARMPRO");
        pst1.setFan(YesNo.N);
//        pst1.setErrorCode(0);
        pst1.setAccountKeyDebit("001;RUR;00000018;759090100;18;;PL00003112;0001;70606;;;;ARMPRO;;");
//        pst1.setParentReference("0092");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        EtlPosting pst2 = newPosting(stamp, pkg);
        pst2.setAePostingId("20461643B");
        pst2.setEventId("201264B");
        pst2.setOperationTimestamp(new Date());
        pst2.setNarrative("Reserve creation");
        pst2.setRusNarrativeLong("Формирование резервов");
        pst2.setRusNarrativeShort("Формирование резервов");
        pst2.setStorno(YesNo.N);
        pst2.setAccountCredit("47425810400014560229");
//        pst2.setAccountDebit("20202840000010609639");
        pst2.setAmountCredit(new BigDecimal("14380.450"));
        pst2.setAmountDebit(new BigDecimal("14380.450"));
        pst2.setCurrencyCredit(BankCurrency.RUB);
        pst2.setCurrencyDebit(BankCurrency.RUB);
        pst2.setValueDate(DateUtils.dbDateParse("2016-12-31"));
        pst2.setDealId("47423810000014544711");
        pst2.setSourcePosting("ARMPRO");
        pst2.setFan(YesNo.N);
//        pst2.setErrorCode(0);
        pst2.setAccountKeyDebit("001;RUR;00000018;759090100;10;;PL00013112;0001;70606;;;;ARMPRO;;");
//        pst2.setParentReference("0092");
        pst2 = (EtlPosting) baseEntityRepository.save(pst2);

        EtlPosting pst3 = newPosting(stamp, pkg);
        pst3.setAePostingId("20461643C");
        pst3.setEventId("201264C");
        pst3.setOperationTimestamp(new Date());
        pst3.setNarrative("Reserve creation");
        pst3.setRusNarrativeLong("Формирование резервов");
        pst3.setRusNarrativeShort("Формирование резервов");
        pst3.setStorno(YesNo.N);
        pst3.setAccountCredit("47425810400014560229");
//        pst3.setAccountDebit("20202840000010609639");
        pst3.setAmountCredit(new BigDecimal("24380.450"));
        pst3.setAmountDebit(new BigDecimal("24380.450"));
        pst3.setCurrencyCredit(BankCurrency.RUB);
        pst3.setCurrencyDebit(BankCurrency.RUB);
        pst3.setValueDate(DateUtils.dbDateParse("2016-12-31"));
        pst3.setDealId("47423810000014544712");
        pst3.setSourcePosting("ARMPRO");
        pst3.setFan(YesNo.N);
//        pst3.setErrorCode(0);
        pst3.setAccountKeyDebit("001;RUR;00000018;759090100;11;;PL00023112;0001;70606;;;;ARMPRO;;");
//        pst3.setParentReference("0092");
        pst3 = (EtlPosting) baseEntityRepository.save(pst3);

    }
}
