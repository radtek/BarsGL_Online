package ru.rbt.barsgl.ejb.integr.pst;

import org.apache.poi.util.StringUtil;
import ru.rbt.barsgl.ejb.entity.acc.Acc;
import ru.rbt.barsgl.ejb.entity.dict.AccountingType;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.GlPdTh;
import ru.rbt.barsgl.ejb.repository.GLPostingRepository;
import ru.rbt.barsgl.ejb.repository.GlPdThRepositoty;
import ru.rbt.barsgl.ejbcore.util.StringUtils;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by er23851 on 06.03.2017.
 */
public class TechOperationProcessor extends GLOperationProcessor
{
    @Inject
    private GLPostingRepository glPostingRepository;

    @Inject
    private GlPdThRepositoty glPdThRepositoty;

    @Override
    public boolean isSupported(GLOperation operation) {
        return     !operation.isFan()                                           // не веер
                && !operation.isStorno()                                        // не сторно
                && !operation.isInterFilial()                                   // филиал один
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
        Long id = glPdThRepositoty.getID();
        pdth.setId(id);
        pdth.setPod(operation.getPostDate());
        pdth.setVald(operation.getValueDate());
        pdth.setPbr("@@GL-"+StringUtils.substr(operation.getSourcePosting(),2));
        pdth.setInvisible("0");

        if (operSide == GLOperation.OperSide.D) {
            pdth.setId(id);
            pdth.setBsaAcid(operation.getAccountDebit());
            pdth.setCcy(operation.getCurrencyDebit());

            BigDecimal amnt = BigDecimal.valueOf(-1).multiply(operation.getAmountDebit().multiply(BigDecimal.valueOf(10).pow(operation.getCurrencyDebit().getScale().intValue())));
            pdth.setAmount(amnt);

            BigDecimal amntс = BigDecimal.valueOf(-1).multiply(operation.getAmountPosting().multiply(BigDecimal.valueOf(10).pow(operation.getCurrencyDebit().getScale().intValue())));
            pdth.setAmountBC(amntс);
        }
        else if (operSide == GLOperation.OperSide.C)
        {
            pdth.setBsaAcid(operation.getAccountCredit());
            pdth.setCcy(operation.getCurrencyCredit());

            BigDecimal amnt = operation.getAmountCredit().multiply(BigDecimal.valueOf(10).pow(operation.getCurrencyCredit().getScale().intValue()));
            pdth.setAmount(amnt);

            BigDecimal amntс = operation.getAmountPosting().multiply(BigDecimal.valueOf(10).pow(operation.getCurrencyCredit().getScale().intValue()));
            pdth.setAmountBC(amntс);
        }

        pdth.setPnar(operation.getNarrative().substring(0,operation.getNarrative().length()>30?29:operation.getNarrative().length()-1));
        pdth.setDepartment(operation.getDeptId());
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
        pdth.setGlAcID(glPdThRepositoty.getAccID(operation.getAccountDebit()));

        return pdth;
    }

}
