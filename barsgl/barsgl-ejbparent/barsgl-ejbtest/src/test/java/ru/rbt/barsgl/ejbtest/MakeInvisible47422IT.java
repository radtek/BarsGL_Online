package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.MakeInvisible47422Task;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.util.StringUtils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.EUR;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.RUB;

/**
 * Created by er18837 on 06.09.2018.
 */
public class MakeInvisible47422IT extends AbstractRemoteIT {

    @Test
    public void testFirst() {
        Properties props = new Properties();
        props.setProperty("depth", "30");
        props.setProperty("withClosedPeriod", "true");
//        props.setProperty("mode", "Glue");
        props.setProperty("mode", "Full");
        remoteAccess.invoke(MakeInvisible47422Task.class, "testExec", null, props);
    }

    @Test
    public void testSimpleOneday() throws SQLException {
        Date od = getOperday().getCurrentDate();
        String dogDate = new SimpleDateFormat("dd.MM.yyyy").format(new Date());
        String ndog = generateNdog();
        BankCurrency ccy = RUB;
        String accTech = Utl4Tests.findBsaacid(baseEntityRepository, od, "47422810__040%");        // 47422810000404876516 - EKB
        BigDecimal amnt = new BigDecimal("315.45");

        EtlPackage pkgPh = newPackage(System.currentTimeMillis(), "Ph47422");
        EtlPosting pstPh = createPosting(pkgPh, "PH", "30102810__040%", accTech, ccy, amnt,
                "Оплата по договору " + ndog + " от " + dogDate);
        Assert.assertTrue(pstPh.getId() > 0);
        GLOperation operationPh = (GLOperation) postingController.processMessage(pstPh);

        EtlPackage pkgFc = newPackage(System.currentTimeMillis(), "Fc47422");
        EtlPosting pstFc = createPosting(pkgFc, "FC12_CL", accTech, "47427810__040%", ccy, amnt,
                "Оплата по договору " + ndog + " от " + dogDate);
        Assert.assertTrue(pstFc.getId() > 0);
        GLOperation operationFc = (GLOperation) postingController.processMessage(pstFc);

    }

    private String generateNdog() throws SQLException {
        DataRecord rec = baseEntityRepository.selectFirst("select max(ID) from GL_ETLPST");
        String num = rec.getString(0);
        if (num.length() < 9) {
            num = StringUtils.leftPad(num, 9 - num.length(),"0");
        }
        return StringUtils.substr(num, 0, 3) + "/" + StringUtils.substr(num, 3, 7) + "L/" + StringUtils.substr(num, 7, 9);
    }

    public EtlPosting createPosting(EtlPackage pkg, String src, String acDt, String acCt, BankCurrency ccy, BigDecimal sum, String rnar) throws SQLException {
        EtlPosting pst = ReprocessErrorIT.newPosting(pkg, src, acDt, acCt, ccy, ccy, sum, sum);
        pst.setRusNarrativeLong(rnar);
        return (EtlPosting) baseEntityRepository.save(pst);
    }

}
