package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.acc.AccCardId;
import ru.rbt.barsgl.ejb.entity.acc.GLAccCard;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * Created by er18837 on 21.08.2017.
 */
public class AccCardRepository extends AbstractBaseEntityRepository<GLAccCard, AccCardId> {
    public GLAccCard createAccCardRecord(String bsaAcid, Date startDate, BigDecimal turnover, GLAccount glAcc) {
        GLAccCard accCard = new GLAccCard(bsaAcid, startDate);
        accCard.setAcid(glAcc.getAcid());
        accCard.setFilial(glAcc.getFilial());
        accCard.setCompanyCode(glAcc.getCompanyCode());
        accCard.setBranch(glAcc.getBranch()); // TODO Substr(gl_acc.acid.18,3)
        accCard.setCcy(glAcc.getCurrency().getCurrencyCode());
        accCard.setCard(glAcc.getSubDealId());
        accCard.setStartBalance(BigDecimal.ZERO);
        accCard.setTurnovers(turnover);
        return accCard;
    };

    public GLAccCard createAccCardRecord(String bsaAcid, Date startDate, Date endDate,  BigDecimal turnover, GLAccCard cardPrev) {
        GLAccCard accCard = new GLAccCard(bsaAcid, startDate);
        accCard.setEndDate(endDate);
        accCard.setAcid(cardPrev.getAcid());
        accCard.setFilial(cardPrev.getFilial());
        accCard.setCompanyCode(cardPrev.getCompanyCode());
        accCard.setBranch(cardPrev.getBranch());
        accCard.setCcy(cardPrev.getCcy());
        accCard.setCard(cardPrev.getCard());
        accCard.setStartBalance(cardPrev.getStartBalance().add(cardPrev.getTurnovers()));
        accCard.setTurnovers(turnover);
        return accCard;
    };

    public Date getMaxDateBefore(String bsaAcid, Date dat) throws SQLException {
        DataRecord res = selectFirst("select max(DAT) from GL_ACCCARD where BSAACID = ? and DAT < ?", bsaAcid, dat);
        return null != res ? res.getDate(0) : null;
    }

    public Date getMinDateAfter(String bsaAcid, Date dat) throws SQLException {
        DataRecord res = selectFirst("select min(DAT) from GL_ACCCARD where BSAACID = ? and DAT > ?", bsaAcid, dat);
        return null != res ? res.getDate(0) : null;
    }

    public List<DataRecord> getAccTurnovers(Date procdate, String filial) throws SQLException {
        return select(
                "select POD, BSAACID, sum(AMNT * 0.01) DtCt from V_GL_CARDPD" +
                        "where PROCDATE = ? and FILIAL = ?" +
                        "group by BSAACID, POD" +
                        "order by BSAACID, POD", procdate, filial);

    }
}
