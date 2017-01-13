package ru.rbt.barsgl.ejbtest;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.Memorder;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

/**
 * Created by Ivan Sevastyanov
 * Расчет и заполнение атрибутов мемориального ордера
 * @fsd 7.10
 */
public class MemorderTest extends AbstractTimerJobTest {

    private static final Logger log = Logger.getLogger(MemorderTest.class);

    /**
     * Создание мемориального ордера при обработке операции в формате простой проводки
     * @fsd 7.10.2
     * @throws ParseException
     */
    @Test public void testMemorder() throws Exception {
        // мемориальный ордер
        String accCt = "40817036200012959997";
        String accDt = "40817036250010000018";
        createOrUpdateMask(Memorder.DocType.MEMORDER, accDt, accCt);

        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountCredit(accCt);
        pst.setAccountDebit(accDt);
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setAePostingId(System.currentTimeMillis() + "");

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());

        List<GLPosting> postings = getPostings(operation);
        Assert.assertNotNull(postings);                 // 1 проводка
        Assert.assertEquals(postings.size(), 1);

        for (GLPosting posting : postings) {
            Memorder order = getMemorder(posting);
            Assert.assertNotNull(order.getCancelFlag());
            Assert.assertNotNull(order.getDocType());
            Assert.assertNotNull(order.getNumber());
            Assert.assertNotNull(order.getPostDate());
            log.info("Memorder found: " + order.getId() + " / " + order.getNumber());
            Assert.assertNotNull(order);
            Assert.assertEquals(Memorder.DocType.MEMORDER, order.getDocType());
        }
    }

    @Test public void testBankOrder() throws SQLException {
        // банковский ордер
        String accCt = "40817036200012959997";
        String accDt = "40817036250010000018";

        createOrUpdateMask(Memorder.DocType.BANK_ORDER, accDt, accCt);
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountCredit(accCt);
        pst.setAccountDebit(accDt);
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setAePostingId(System.currentTimeMillis() + "");

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());

        List<GLPosting> postings = getPostings(operation);
        Assert.assertNotNull(postings);                 // 1 проводка
        Assert.assertEquals(postings.size(), 1);

        for (GLPosting posting : postings) {
            Memorder order = getMemorder(posting);
            Assert.assertNotNull(order.getCancelFlag());
            Assert.assertNotNull(order.getDocType());
            Assert.assertNotNull(order.getNumber());
            Assert.assertNotNull(order.getPostDate());
            log.info("Memorder found: " + order.getId() + " / " + order.getNumber());
            Assert.assertNotNull(order);
            Assert.assertEquals(Memorder.DocType.BANK_ORDER, order.getDocType());
        }
    }

    private Memorder getMemorder(GLPosting posting) {
        return (Memorder) baseEntityRepository.selectOne(Memorder.class, "from Memorder m where m.id = ?1", posting.getId());
    }

    private void createOrUpdateMask(Memorder.DocType docType, String accDt, String accCt) throws SQLException {
        if (Memorder.DocType.MEMORDER == docType) {
            int cnt = baseEntityRepository.executeNativeUpdate("delete from F067_MASK where substr(dt_mask, 1, 5) like ? and substr(ct_mask, 1, 5) like ?"
                , accDt.substring(0,5) + "%", accCt.substring(0,5) + "%");
            log.info("deleted " + cnt);
        } else {
            int cnt = baseEntityRepository
                    .selectFirst("select count(1) cnt from F067_MASK where substr(dt_mask, 1, 5) like ? and substr(ct_mask, 1, 5) like ?"
                            , accDt.substring(0,5) + "%", accCt.substring(0,5) + "%")
                    .getInteger(0);
            if (cnt == 0) {
                baseEntityRepository.executeNativeUpdate("insert into F067_MASK (rowid, dat, datto, client_ind, dt_mask, ct_mask)" +
                        " values (1, '2000-01-01', '2029-01-01', 'D', ?, ?)", accDt.substring(0,5) + "%", accCt.substring(0,5) + "%");
            }
        }

    }


}
