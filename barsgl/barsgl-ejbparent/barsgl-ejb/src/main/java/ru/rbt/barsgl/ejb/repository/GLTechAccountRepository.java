package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;

/**
 * Created by er23851 on 19.04.2017.
 */
public class GLTechAccountRepository extends AbstractBaseEntityRepository<GLAccount,Long> {

    public BigDecimal getAccountBalance(String bsaAcid, Long glacid, Date datto) {
        try {
            DataRecord data = selectFirst("select PKG_CHK_ACC.GET_BALANCE_TODATE_TECH(?, ?, ?) from dual", bsaAcid, glacid, datto);
            return (null == data) ? BigDecimal.ZERO : data.getBigDecimal(0);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public Boolean hasAccountBalanceBefore(String bsaAcid, Long glacid, Date dat) {
        try {
            DataRecord data = selectFirst("select PKG_CHK_ACC.HAS_BALANCE_BEFORE_TECH(?, ?, ?) from dual", bsaAcid, glacid, dat);
            return (null != data && data.getInteger(0) == 1);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public Boolean hasAccountBalanceAfter(String bsaAcid, Long glacid, Date dat) {
        try {
            DataRecord data = selectFirst("select PKG_CHK_ACC.HAS_BALANCE_AFTER_TECH(?, ?, ?) from dual", bsaAcid, glacid, dat);
            return (null != data && data.getInteger(0) == 1);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }
}
