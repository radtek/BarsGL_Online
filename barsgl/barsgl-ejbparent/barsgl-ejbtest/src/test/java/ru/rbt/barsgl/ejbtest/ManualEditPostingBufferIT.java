package ru.rbt.barsgl.ejbtest;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.entity.gl.AbstractPd;
import ru.rbt.barsgl.ejb.entity.gl.GLManualOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPd;
import ru.rbt.barsgl.ejb.integr.bg.EditPostingController;
import ru.rbt.barsgl.ejb.integr.bg.ManualOperationController;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.barsgl.shared.enums.PostingChoice;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.StringUtils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by ER18837 on 22.04.16.
 */
public class ManualEditPostingBufferIT  extends AbstractTimerJobIT {

    private final Long USER_ID = 2L;

    @Before
    public void beforeClass() {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN, Operday.PdMode.BUFFER);
    }

    /**
     * Редактирование общих полей для операции по ручному вводу (Deal, subDeal, profitCenter) в режиме БУФЕР
     */
    @Test
    public void testEditManualOperationBuffer() throws SQLException {
        // создаем ручную операцию
        BigDecimal sum = new BigDecimal("50.25");
        String bsaDt = Utl4Tests.findBsaacidBal(baseEntityRepository, getOperday(), "47425810_0001%", sum);
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40702810_0001%");
        ManualOperationWrapper wrapper = newOperationWrapper("А",
                "MOS", bsaDt, "RUR", sum,
                "MOS", bsaCt, "RUR", sum
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

        BatchPosting posting = ManualOperationIT.createAuthorizedPosting(wrapper, USER_ID);
        Assert.assertNotNull(posting);

        GLManualOperation operation = remoteAccess.invoke(ManualOperationController.class, "processPosting", posting, false);
        Assert.assertNotNull(operation);

        operation = (GLManualOperation) baseEntityRepository.findById(GLManualOperation.class, operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        List<AbstractPd> pdList = getGLPdList(operation);
        checkCommonParams(operation, pdList, dealId, subDealId, profit);

        final ArrayList<Long> idList = new ArrayList<>();
        idList.add(pdList.get(0).getId());
        idList.add(pdList.get(1).getId());
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
        wrapper.setId(operation.getId());

        RpcRes_Base<ManualOperationWrapper> res2 = remoteAccess.invoke(EditPostingController.class, "updatePostingsWrapper", wrapper);
        Assert.assertFalse(res2.isError());
        pdList = getGLPdList(operation);
        checkCommonParams(operation, pdList, dealId2, subDealId2, profit2);
    }

    /**
     * Редактирование полей для проводки АЕ в режиме БУФЕР
     *
     * @throws SQLException
     */
    @Test
    public void testEditAeOperationBuffer() throws SQLException {
        GLOperation operation = createExchOperation();
        List<AbstractPd> pdList = getGLPdList(operation);
        Assert.assertNotNull(pdList);
        final String dealId = operation.getDealId();
        final String subDealId = operation.getSubdealId();
        checkCommonParams(operation, pdList, dealId, subDealId, null);

        final ArrayList<Long> idList = new ArrayList<>();
        idList.add(pdList.get(0).getId());
        idList.add(pdList.get(1).getId());
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
        pdList = getGLPdList(operation);
        checkCommonParams(operation, pdList, dealId, subDealId, null);
        checkMemorder(pdList, isCorrection);

        // отменить коррекцию
        isCorrection = false;
        wrapper.setCorrection(isCorrection);

        RpcRes_Base<ManualOperationWrapper> res3 = remoteAccess.invoke(EditPostingController.class, "updatePostingsWrapper", wrapper);
        Assert.assertFalse(res2.isError());
        checkMemorder(pdList, isCorrection);

        // изменить основане для первой проводки
        List<String> engList = new ArrayList<>();
        List<String> rusList = new ArrayList<>();
        getGLPdList(operation).forEach(pd -> {
            engList.add(pd.getNarrative());
            rusList.add(pd.getRusNarrLong());
        });
        wrapper.setNarrative("CHG_" + engList.get(0));
        wrapper.setRusNarrativeLong("CHG_" + rusList.get(0));
        wrapper.setPostingChoice(PostingChoice.PST_ONE_OF);

        RpcRes_Base<ManualOperationWrapper> res4 = remoteAccess.invoke(EditPostingController.class, "updatePostingsWrapper", wrapper);
        Assert.assertFalse(res2.isError());
        pdList = getGLPdList(operation);
        checkNarrative(pdList.subList(0, 2), wrapper.getNarrative(), wrapper.getRusNarrativeLong());
        for (int i = 2; i < pdList.size(); i++) {
            checkNarrative(pdList.subList(i, i + 1), engList.get(i), rusList.get(i));
        }
    }

    @Test
    public void testSuppressOperationBuffer() throws SQLException {
        GLOperation operation = createExchOperation();
        List<AbstractPd> pdList = getGLPdList(operation);
        Assert.assertNotNull(pdList);

        final ArrayList<Long> idList = new ArrayList<>();
        idList.add(pdList.get(0).getId());
        idList.add(pdList.get(1).getId());
        ManualOperationWrapper wrapper = newOperationWrapper(operation);
        wrapper.setPdIdList(idList);
        wrapper.setPostingChoice(PostingChoice.PST_ALL);
        wrapper.setPdMode(getOperday().getPdMode().name());

        // подавить
        boolean isInvisible = true;
        wrapper.setInvisible(isInvisible);

        RpcRes_Base<ManualOperationWrapper> res2 = remoteAccess.invoke(EditPostingController.class, "suppressPostingsWrapper", wrapper);
        Assert.assertFalse(res2.isError());
        pdList = getGLPdList(operation);
        checkInvisible(pdList, isInvisible);

        // восстановить
        isInvisible = false;
        wrapper.setInvisible(isInvisible);

        RpcRes_Base<ManualOperationWrapper> res3 = remoteAccess.invoke(EditPostingController.class, "suppressPostingsWrapper", wrapper);
        Assert.assertFalse(res2.isError());
        pdList = getGLPdList(operation);
        checkInvisible(pdList, isInvisible);

    }

    private List<AbstractPd> getGLPdList(GLOperation operation) {
        Map<String, String> map = ImmutableMap.<String, String>builder()
                .put("javax.persistence.cache.storeMode", "REFRESH").build();
        return baseEntityRepository.selectHinted(GLPd.class, "from GLPd p where p.glOperationId = ?1 order by p.id",
                new Object[]{operation.getId()}, map);
    }

    private void checkMemorder(List<AbstractPd> pdList, boolean isCorrection) throws SQLException {
        String pcids = StringUtils.listToString(pdList, ",");
        List<DataRecord> res = baseEntityRepository.select("select MO_NO from GL_PD where ID in (" + pcids + ") and AMNTBC < 0");
        char flag = (isCorrection ? '9' : '0');
        res.forEach(mo -> Assert.assertEquals(flag, mo.getString(0).charAt(5)));
    }

    private void checkInvisible(List<AbstractPd> pdList, boolean isInvisible) {
        String invisible = isInvisible ? "1" : "0";
        pdList.forEach(pd -> Assert.assertEquals(invisible, pd.getInvisible()));
    }

    private void checkCommonParams(GLOperation operation, List<AbstractPd> pdList, String dealId, String subDealId, String profit) {
        ManualEditPostingDirectIT.checkCommonParams(operation, pdList, dealId, subDealId, profit);
    }

    private void checkNarrative(List<AbstractPd> pdList, String narrEng, String narrRus) {
        ManualEditPostingDirectIT.checkNarrative(pdList, narrEng, narrRus);
    }

    private GLOperation createMfoExchOperation() throws SQLException {
        return ManualEditPostingDirectIT.createMfoExchOperation();
    }

    private GLOperation createExchOperation() throws SQLException {
        return ManualEditPostingDirectIT.createExchOperation();
    }
}
