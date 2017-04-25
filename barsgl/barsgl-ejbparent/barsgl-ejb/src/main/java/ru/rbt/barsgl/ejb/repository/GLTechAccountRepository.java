package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;

/**
 * Created by er23851 on 19.04.2017.
 */
public class GLTechAccountRepository extends AbstractBaseEntityRepository<GLAccount,Long> {

    public BigDecimal getAccountBalance(String bsaAcid, Long glacid, Date datto) {
        try {
            DataRecord data = selectFirst("SELECT sum(OBAC) + sum(DTAC) + sum(CTAC) \n" +
                    "from (SELECT BSAACID, GLACID, OBAC, DTAC, CTAC from GL_BTTH where DATTO = ? and GLACID = ? and BSAACID = ?) T \n" +
                    "group by BSAACID, GLACID", datto, glacid, bsaAcid);
            return (null == data) ? BigDecimal.ZERO : data.getBigDecimal(0);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public Boolean hasAccountBalanceBefore(String bsaAcid, Long glacid, Date dat) {
        try {
            DataRecord data = selectFirst("SELECT * from GL_BTTH where DATTO < ? and GLACID = ? and BSAACID = ? "
                    , dat, glacid, bsaAcid);
            return (null != data);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public Boolean hasAccountBalanceAfter(String bsaAcid, Long glacid, Date dat) {
        try {
            DataRecord data = selectFirst("SELECT * from GL_BTTH where DAT > ? and GLACID = ? and BSAACID = ? "
                    , dat, glacid, bsaAcid);
            return (null != data);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }
}
