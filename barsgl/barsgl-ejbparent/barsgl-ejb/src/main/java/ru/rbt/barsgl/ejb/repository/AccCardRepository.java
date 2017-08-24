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

import static ru.rbt.ejbcore.util.StringUtils.substr;

/**
 * Created by er18837 on 21.08.2017.
 */
public class AccCardRepository extends AbstractBaseEntityRepository<GLAccCard, AccCardId> {
    public GLAccCard createAccCardRecord(GLAccount glAcc, Date startDate, Date endDate, BigDecimal turnover) {
        GLAccCard accCard = new GLAccCard(glAcc.getBsaAcid(), startDate);
        accCard.setEndDate(endDate);
        String acid = glAcc.getAcid();
        accCard.setAcid(acid);
        accCard.setFilial(glAcc.getFilial());
        accCard.setCompanyCode(glAcc.getCompanyCode());
        accCard.setBranch(substr(acid, 17, 20));        // последние 3 символа
        accCard.setCcy(glAcc.getCurrency().getCurrencyCode());
        accCard.setCard(glAcc.getSubDealId());
        accCard.setStartBalance(BigDecimal.ZERO);
        accCard.setTurnovers(turnover);
        return accCard;
    };

    public GLAccCard createAccCardRecord(GLAccCard cardPrev, Date startDate, Date endDate, BigDecimal turnover) {
        GLAccCard accCard = new GLAccCard(cardPrev.getId().getBsaAcid(), startDate);
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

    public List<DataRecord> getFilalTurnoversOnDate(String filial, Date procdate) throws SQLException {
        return select(  "select BSAACID, POD, sum(AMNT * 0.01) DtCt from V_GL_CARDPD" +
                        " where FILIAL = ? and PROCDATE = ? " +
                        " group by BSAACID, POD" +
                        " order by BSAACID, POD", filial, procdate);
    }

    public List<DataRecord> getAccTurnoversAfterDate(String bsaacid, Date procdate) throws SQLException {
        return select(  "select BSAACID, POD, sum(AMNT * 0.01) DtCt from V_GL_CARDPD" +
                        " where (AC_DR = ? or AC_CR = ?) and BSAACID = ? and PROCDATE > ? " +
                        " group by BSAACID, POD" +
                        " order by BSAACID, POD", bsaacid, bsaacid, bsaacid, procdate);
    }

    public int deleteAccBalanceAfterDate(String bsaacid, Date afterdate) throws SQLException {
        return executeNativeUpdate(
                "delete from GL_ACCCARD where BSAACID = ? and PROCDATE > ? ", bsaacid, afterdate);
    }
}
