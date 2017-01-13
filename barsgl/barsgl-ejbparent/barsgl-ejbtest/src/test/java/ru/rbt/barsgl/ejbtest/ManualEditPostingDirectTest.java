package ru.rbt.barsgl.ejbtest;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.*;
import ru.rbt.barsgl.ejb.integr.bg.EditPostingController;
import ru.rbt.barsgl.ejb.integr.bg.ManualPostingController;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.util.StringUtils;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.barsgl.shared.enums.PostingChoice;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by ER18837 on 21.04.16.
 */
public class ManualEditPostingDirectTest extends AbstractTimerJobTest {


    @Before
    public void beforeClass() {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
    }
    /**
     * Редактирование общих полей для операции по ручному вводу (Deal, subDeal, profitCenter)
     */
    @Test
    public void testEditManualOperationDirect() throws SQLException {
        // создаем ручную операцию
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817840_0040%");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810_0016%");
        ManualOperationWrapper wrapper = newOperationWrapper("А",
                "EKB", bsaDt, "USD", new BigDecimal("1000"),
                "CHL", bsaCt, "RUR", new BigDecimal("65000.25")
        );

        final String src = "FC12_CL";
        final String dealId = "DD_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 4);
        final String subDealId = "SD_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 4);
        final String profit = "FD";
        wrapper.setDealSrc(src);
        wrapper.setSubdealId(subDealId);
        wrapper.setDealId(dealId);
        wrapper.setProfitCenter(profit);
        wrapper.setInputMethod(InputMethod.M);

        RpcRes_Base<ManualOperationWrapper> res = remoteAccess.invoke(ManualPostingController.class, "processMessageWrapper", wrapper);
        Assert.assertFalse(res.isError());
        wrapper = res.getResult();
        Assert.assertTrue(0 < wrapper.getId());

        GLManualOperation operation = (GLManualOperation) baseEntityRepository.findById(GLManualOperation.class, wrapper.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);
        postList.forEach(post -> checkCommonParams(operation, getPdList(post), dealId, subDealId, profit));

        final ArrayList<Long> idList = new ArrayList<>();
        getPdList(postList.get(0)).forEach(pd -> idList.add(pd.getId()));
        wrapper.setPdIdList(idList);
        wrapper.setPostingChoice(PostingChoice.PST_ALL);
        wrapper.setPdMode(getOperday().getPdMode().name());

        // изменить общие параметры
        final String dealId2 = "DD_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 4);
        final String subDealId2 = "SD_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 4);
        final String profit2 = "FDM";
        wrapper.setSubdealId(subDealId2);
        wrapper.setDealId(dealId2);
        wrapper.setProfitCenter(profit2);

        RpcRes_Base<ManualOperationWrapper> res2 = remoteAccess.invoke(EditPostingController.class, "updatePostingsWrapper", wrapper);
        Assert.assertFalse(res2.isError());
        postList.forEach(post -> checkCommonParams(operation, getPdList(post), dealId2, subDealId2, profit2));
    }

    /**
     * Редактирование полей для проводки АЕ
     * @throws SQLException
     */
    @Test
    public void testEditAeOperationDirect() throws SQLException {
        GLOperation operation = createMfoExchOperation();
        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);
        final String dealId = operation.getDealId();
        final String subDealId = operation.getSubdealId();
        postList.forEach(post -> checkCommonParams(operation, getPdList(post), dealId, subDealId, null));

        final ArrayList<Long> idList = new ArrayList<>();
        final GLPosting postEdit = postList.get(0);
        getPdList(postEdit).forEach(pd -> idList.add(pd.getId()));
        ManualOperationWrapper wrapper = newOperationWrapper(operation);
        wrapper.setPdIdList(idList);
        wrapper.setPostingChoice(PostingChoice.PST_ALL);
        wrapper.setPdMode(getOperday().getPdMode().name());

        // изменить общие параметры и коррекция
        final String dealId2 = "DD_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 4);
        final String subDealId2 = "SD_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 4);
        wrapper.setSubdealId(subDealId2);
        wrapper.setDealId(dealId2);
        boolean isCorrection = true;
        wrapper.setCorrection(isCorrection);

        RpcRes_Base<ManualOperationWrapper> res2 = remoteAccess.invoke(EditPostingController.class, "updatePostingsWrapper", wrapper);
        Assert.assertFalse(res2.isError());
        postList.forEach(post -> checkCommonParams(operation, getPdList(post), dealId, subDealId, null));  // не должны измениться
        checkMemorder(postList, isCorrection);

        // отменить коррекцию
        isCorrection = false;
        wrapper.setCorrection(isCorrection);

        RpcRes_Base<ManualOperationWrapper> res3 = remoteAccess.invoke(EditPostingController.class, "updatePostingsWrapper", wrapper);
        Assert.assertFalse(res2.isError());
        checkMemorder(postList, isCorrection);

        // изменить основане для первой проводки
        List<String> engList = new ArrayList<>();
        List<String> rusList = new ArrayList<>();
        postList.forEach(post -> {
            AbstractPd pd = getPdList(post).get(0);
            engList.add(pd.getNarrative());
            rusList.add(pd.getRusNarrLong());
        });
        wrapper.setNarrative("CHG_" + engList.get(0));
        wrapper.setRusNarrativeLong("CHG_" + rusList.get(0));
        wrapper.setPostingChoice(PostingChoice.PST_ONE_OF);

        RpcRes_Base<ManualOperationWrapper> res4 = remoteAccess.invoke(EditPostingController.class, "updatePostingsWrapper", wrapper);
        Assert.assertFalse(res2.isError());
        checkNarrative(getPdList(postEdit), wrapper.getNarrative(), wrapper.getRusNarrativeLong());
        for (int i = 1; i < postList.size(); i++) {
            checkNarrative(getPdList(postList.get(i)), engList.get(i), rusList.get(i));
        }
    }

    @Test
    public void testSuppressOperationDirect() throws SQLException {
        GLOperation operation = createMfoExchOperation();
        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);

        final ArrayList<Long> idList = new ArrayList<>();
        final GLPosting postEdit = postList.get(0);
        getPdList(postEdit).forEach(pd -> idList.add(pd.getId()));
        ManualOperationWrapper wrapper = newOperationWrapper(operation);
        wrapper.setPdIdList(idList);
        wrapper.setPostingChoice(PostingChoice.PST_ALL);
        wrapper.setPdMode(getOperday().getPdMode().name());

        // подавить
        boolean isInvisible = true;
        wrapper.setInvisible(isInvisible);

        RpcRes_Base<ManualOperationWrapper> res2 = remoteAccess.invoke(EditPostingController.class, "suppressPostingsWrapper", wrapper);
        Assert.assertFalse(res2.isError());
        checkInvisible(postList, isInvisible);

        // восстановить
        isInvisible = false;
        wrapper.setInvisible(isInvisible);

        RpcRes_Base<ManualOperationWrapper> res3 = remoteAccess.invoke(EditPostingController.class, "suppressPostingsWrapper", wrapper);
        Assert.assertFalse(res2.isError());
        checkInvisible(postList, isInvisible);
    }

    private List<AbstractPd> getPdList(GLPosting posting) {
        Map<String,String> map = ImmutableMap.<String,String>builder()
                .put("javax.persistence.cache.storeMode", "REFRESH").build();
        return baseEntityRepository.selectHinted(Pd.class, "from Pd p where p.pcId = ?1 order by p.id",
                new Object[]{posting.getId()}, map);
    }

    private void checkMemorder(List<GLPosting> postList, boolean isCorrection) throws SQLException {
        String pcids = StringUtils.listToString(postList, ",");
        List<DataRecord> res = baseEntityRepository.select("select MO_NO from PCID_MO where PCID in (" + pcids + ")");
        char flag = (isCorrection ? '9' : '0');
        res.forEach(mo-> Assert.assertEquals(flag, mo.getString(0).charAt(5)));
    }

    private void checkInvisible(List<GLPosting> postList, boolean isInvisible) {
        String invisible = isInvisible ? "1" : "0";
        postList.forEach(post -> {
            getPdList(post).forEach(pd -> Assert.assertEquals(invisible, pd.getInvisible()));
        });
    }

    public static void checkCommonParams(GLOperation operation, List<AbstractPd> pdList, String dealId, String subDealId, String profit) {
        pdList.forEach(pd -> {
            Assert.assertEquals(profit, pd.getProfitCenter());
            Assert.assertEquals(dealId, pd.getDealId());
            Assert.assertEquals(subDealId, pd.getSubdealId());
            String pnar = operation.getInputMethod().equals(InputMethod.AE) ? getPnar(operation) : getPnarManual(dealId, subDealId, null);
            Assert.assertEquals(pnar, pd.getPnar());
        });
    }

    public static void checkNarrative(List<AbstractPd> pdList, String narrEng, String narrRus) {
        pdList.forEach(pd -> {
            Assert.assertEquals(narrEng, pd.getNarrative());
            Assert.assertEquals(narrRus, pd.getRusNarrLong());
        });
    }

    public static GLOperation createMfoExchOperation() throws SQLException {
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "MfoExchange");
        Assert.assertTrue(pkg.getId() > 0);

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810_0050%");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817840_0045%");
        EtlPosting pst = newPosting(stamp, pkg);
        pst.setSourcePosting("FC12_CL");
        pst.setDealId("DD_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 4));
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountDebit(bsaDt);    // "NVS"
        pst.setCurrencyDebit(BankCurrency.RUB);
        pst.setAmountDebit(new BigDecimal("200.00"));

        pst.setAccountCredit(bsaCt);     // "NNV"
        pst.setCurrencyCredit(BankCurrency.USD);
        pst.setAmountCredit(new BigDecimal("13000.78"));

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());       // операция создана
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(operation.getState(), OperState.POST);
        return operation;
    }
}
