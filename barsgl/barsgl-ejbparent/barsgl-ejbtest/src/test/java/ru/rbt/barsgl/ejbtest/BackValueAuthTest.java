package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLBackValueOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperationExt;
import ru.rbt.barsgl.ejb.integr.bg.BackValueOperationController;
import ru.rbt.barsgl.ejb.integr.bg.BackValuePostingController;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.criteria.*;
import ru.rbt.barsgl.shared.enums.BackValueAction;
import ru.rbt.barsgl.shared.enums.BackValueMode;
import ru.rbt.barsgl.shared.enums.BackValuePostStatus;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.barsgl.shared.operation.BackValueWrapper;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.AUD;
import static ru.rbt.barsgl.ejbtest.BackValueOperationTest.*;
import static ru.rbt.barsgl.shared.criteria.CriterionColumn.createCriterion;
import static ru.rbt.barsgl.shared.enums.BackValueAction.SIGN;
import static ru.rbt.barsgl.shared.enums.BackValueAction.TO_HOLD;
import static ru.rbt.barsgl.shared.enums.BackValueMode.ONE;
import static ru.rbt.barsgl.shared.enums.BackValuePostStatus.*;
import static ru.rbt.barsgl.shared.enums.DealSource.PaymentHub;

/**
 * Created by er18837 on 20.07.2017.
 */
public class BackValueAuthTest extends AbstractTimerJobTest {

    public static final Logger log = Logger.getLogger(BackValueAuthTest.class.getName());

    @BeforeClass
    public static void beforeAll() {

        try {
            setOperday(DateUtils.parseDate("27.02.2015", "dd.MM.yyyy"), DateUtils.parseDate("25.02.2015", "dd.MM.yyyy"), ONLINE, OPEN);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        setCalendar2015_02();

        saveTable("GL_BVPARM");
        saveTable("GL_CRPRD");

        setBVparams();
    }

    /**
     * Задержать одну операцию
     * @throws SQLException
     */
    @Test
    public void testHoldOne() throws SQLException {
        /**
         * создать операцию BV в статусе CONTROL
         * авторизовать
         * запустить обработку
         */
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817036%5");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817036%7");
        BigDecimal amt = new BigDecimal("44.88");
        BankCurrency currency = AUD;

        Date vdate = DateUtils.addDays(getOperday().getCurrentDate(), -7);
        EtlPosting pst = createEtlPosting(vdate, PaymentHub.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);
        Assert.assertNotNull(pst);

        GLBackValueOperation operation = (GLBackValueOperation) postingController.processMessage(pst);
        operation = (GLBackValueOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(pst.getValueDate(), operation.getPostDate());
        Assert.assertEquals(CONTROL, operation.getOperExt().getManualStatus());

        BackValueWrapper wrapper = createWrapper(vdate, CONTROL, TO_HOLD, ONE, new GLOperation[]{operation});
        wrapper.setComment("Тест 'testHoldOne'");
        RpcRes_Base<Integer> res = remoteAccess.invoke(BackValuePostingController.class, "processOperationBv", wrapper, createCriteria());
        System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        Assert.assertEquals(1, res.getResult().intValue());

        operation = (GLBackValueOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(pst.getValueDate(), operation.getPostDate());
        GLOperationExt operationExt = operation.getOperExt();
        Assert.assertEquals(HOLD, operationExt.getManualStatus());
        Assert.assertNotNull(operationExt.getConfirmTimestamp());       // TODO надо ?
        Assert.assertEquals(wrapper.getComment(), operationExt.getManualNarrative());
    }

    /**
     * авторизовать одну операцию
     * @throws SQLException
     */
    @Test
    public void testSignOne() throws SQLException {
        /**
         * создать операцию BV в статусе CONTROL
         * авторизовать
         * запустить обработку
         */
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817036%7");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817036%8");
        BigDecimal amt = new BigDecimal("55.99");
        BankCurrency currency = AUD;

        Date vdate = DateUtils.addDays(getOperday().getCurrentDate(), -7);
        EtlPosting pst = createEtlPosting(vdate, PaymentHub.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);
        Assert.assertNotNull(pst);

        GLBackValueOperation operation = (GLBackValueOperation) postingController.processMessage(pst);
        operation = (GLBackValueOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(pst.getValueDate(), operation.getPostDate());
        Assert.assertEquals(CONTROL, operation.getOperExt().getManualStatus());

        Date postdate = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateAfter", vdate, false);
        List<DataRecord> data = baseEntityRepository.select("select RATE from CURRATES where CCY = ? and DAT in (?, ?) order by DAT",
                currency.getCurrencyCode(), vdate, postdate);
        BigDecimal rate1 = data.get(0).getBigDecimal(0);
        BigDecimal rate2 = data.get(1).getBigDecimal(0);
        Assert.assertNotEquals(rate1, rate2);
        Assert.assertEquals(rate1, operation.getRateCredit());

        BackValueWrapper wrapper = createWrapper(postdate, CONTROL, SIGN, ONE, new GLOperation[]{operation});
        RpcRes_Base<Integer> res = remoteAccess.invoke(BackValuePostingController.class, "processOperationBv", wrapper, createCriteria());
        System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        Assert.assertEquals(1, res.getResult().intValue());

        operation = (GLBackValueOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(postdate, operation.getPostDate());
        Assert.assertEquals(SIGNEDDATE, operation.getOperExt().getManualStatus());
        Assert.assertNotNull(operation.getOperExt().getConfirmTimestamp());

        remoteAccess.invoke(BackValueOperationController.class, "processBackValueOperation", operation);
        operation = (GLBackValueOperation) baseEntityRepository.findById(GLBackValueOperation.class, operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertEquals(COMPLETED, operation.getOperExt().getManualStatus());
        Assert.assertEquals(rate2, operation.getRateCredit());
    }

    private BackValueWrapper createWrapper(Date postDate, BackValuePostStatus statusOld, BackValueAction action, BackValueMode mode, GLOperation[] opers) {
        BackValueWrapper wrapper = new BackValueWrapper();
        wrapper.setBvStatus(statusOld);
        wrapper.setPostDateStr(new SimpleDateFormat(wrapper.getDateFormat()).format(postDate));
        wrapper.setAction(action);
        wrapper.setMode(mode);
        wrapper.setGloIDs(Arrays.asList(opers).stream().map(operation -> operation.getId()).collect(Collectors.toList()));
        return wrapper;
    }

    private Criteria createCriteria() {
        List<Criterion> criterionList = new ArrayList<>();
        criterionList.add(createCriterion("GLOID", Operator.GT, 0));
        Criteria criteria = new Criteria(CriteriaLogic.OR, criterionList);
        return criteria;
    }
}
