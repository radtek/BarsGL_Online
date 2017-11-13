package ru.rbt.barsgl.ejb.integr.pst;

import ru.rbt.barsgl.ejb.entity.dict.SourcesDeals;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.GlPdTh;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.barsgl.ejb.repository.GLPostingRepository;
import ru.rbt.barsgl.ejb.repository.GLTechOperationRepository;
import ru.rbt.barsgl.ejb.repository.GlPdThRepository;
import ru.rbt.barsgl.ejb.repository.dict.SourcesDealsRepository;
import ru.rbt.ejbcore.util.StringUtils;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by er23851 on 06.03.2017.
 */
public class TechOperationProcessor extends GLOperationProcessor
{
    @Inject
    private GLPostingRepository glPostingRepository;

    @Inject
    private GLTechOperationRepository glTechOperationRepository;

    @Inject
    private SourcesDealsRepository sourcesDealsRepository;

    @Inject
    private GLOperationRepository glOperationRepository;

    @Inject
    private GlPdThRepository glPdThRepository;

    //Берём все технические в том числе и стороно
    @Override
    public boolean isSupported(GLOperation operation) {
        return     !operation.isFan()                                           // не веер
                && !operation.isInterFilial()                                   // филиал один
                && !isStornoOneday(operation)
                && !operation.isExchangeDifferenceA()                           // нет курсовой разницы или не глава А
                && operation.isTech();                                         // операция по техническим счетам
    }

    @Override
    public GLOperation.OperType getOperationType() {
        return GLOperation.OperType.S;
    }

    @Override
    public void setSpecificParameters(GLOperation operation) {

    }

    @Override
    public List<GLPosting> createPosting(GLOperation operation) throws Exception {
        return null;
    }


    /**
     * Создает 1 проводку (2 полупроводки) в одном филиале с одной валютой
     * @param operation
     * @return
     * @throws Exception
     */
    public List<GlPdTh> createPdTh(GLOperation operation) throws Exception {

        List<GlPdTh> pdthList = new ArrayList<GlPdTh>();
        pdthList.add(this.getPdTh(operation, GLOperation.OperSide.D));
        pdthList.add(this.getPdTh(operation, GLOperation.OperSide.C));
        Collections.sort(pdthList);

        return pdthList;
    }

    private Long longExp(int n)
    {
        Long res = 10L;
        for(int i=1;i<n;i++)
        {
            res *=10;
        }

        return res;
    }

    @Override
    public void resolvePostingReference(GLOperation operation, List<GLPosting> postList)
    {
    }


    public GlPdTh getPdTh(GLOperation operation, GLOperation.OperSide operSide) throws SQLException {

        GlPdTh pdth = new GlPdTh(operSide);
        Long id = glPdThRepository.getID();
        pdth.setId(id);
        pdth.setPod(operation.getPostDate());
        pdth.setVald(operation.getValueDate());
        SourcesDeals dealSrc = sourcesDealsRepository.findCached(operation.getSourcePosting());

        if (dealSrc!=null) {
            pdth.setPbr("@@GL" + dealSrc.getShortName());
        }
        else {
            pdth.setPbr("@@GL-" + StringUtils.substr(operation.getSourcePosting(), 2));
        }
        pdth.setInvisible("0");

        if (operSide == GLOperation.OperSide.D) {
            pdth.setId(id);
            pdth.setBsaAcid(operation.getAccountDebit());
            pdth.setCcy(operation.getCurrencyDebit());
            pdth.setGlAcID(glPdThRepository.getAccID(operation.getAccountDebit()));

            BigDecimal amnt = BigDecimal.valueOf(-1).multiply(operation.getAmountDebit().movePointRight(operation.getCurrencyDebit().getScale().intValue()));
            pdth.setAmount(amnt);

            BigDecimal amntс = BigDecimal.valueOf(-1).multiply(operation.getAmountPosting().movePointRight(operation.getCurrencyDebit().getScale().intValue()));
            pdth.setAmountBC(amntс);
        }
        else if (operSide == GLOperation.OperSide.C)
        {
            pdth.setBsaAcid(operation.getAccountCredit());
            pdth.setCcy(operation.getCurrencyCredit());
            pdth.setGlAcID(glPdThRepository.getAccID(operation.getAccountCredit()));

            BigDecimal amnt = operation.getAmountCredit().movePointRight(operation.getCurrencyCredit().getScale().intValue());
            pdth.setAmount(amnt);

            BigDecimal amntс = operation.getAmountPosting().movePointRight(operation.getCurrencyCredit().getScale().intValue());
            pdth.setAmountBC(amntс);
        }

        pdth.setPnar(operation.getNarrative());
        pdth.setDepartment(operation.getDeptId()!=null ? operation.getDeptId() : " ");
        pdth.setRusNarrLong(operation.getRusNarrativeLong());
        pdth.setRusNarrShort(operation.getRusNarrativeShort());
        pdth.setGlOperationId(operation.getId());
        pdth.setEventType(operation.getEventType());
        pdth.setProcDate(operation.getProcDate());
        pdth.setDealId(operation.getDealId());
        pdth.setSubdealId(operation.getSubdealId());
        pdth.setEventId(operation.getEventId());
        pdth.setPaymentRef(operation.getPaymentRefernce());
        pdth.setIsCorrection(operation.getIsCorrection());
        pdth.setProfitCenter(operation.getProfitCenter());
        pdth.setNarrative(operation.getNarrative());

        return pdth;
    }

}
