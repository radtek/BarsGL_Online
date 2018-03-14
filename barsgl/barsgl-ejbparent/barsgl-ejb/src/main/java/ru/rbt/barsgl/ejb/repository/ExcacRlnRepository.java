package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.acc.*;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by ER18837 on 05.05.15.
 */
public class ExcacRlnRepository extends AbstractBaseEntityRepository<GlExcacRln, String> {

    public GlExcacRln createExcacRln(GLAccount glAccount, String exchCcy, String optype) {
        GlExcacRln excacRln = new GlExcacRln();
        AccRlnId id = new AccRlnId();
        id.setAcid(glAccount.getAcid());
        id.setBsaAcid(glAccount.getBsaAcid());
        excacRln.setId(id);

        excacRln.setCompanyCode(glAccount.getCompanyCode());
        excacRln.setCurrency(exchCcy);
        excacRln.setPassiveActive(glAccount.getPassiveActive());
        excacRln.setCash(optype);

        save(excacRln);
        return excacRln;
    }

    public GLAccParam findForPlcode7903(AccountKeys keys, BankCurrency bankCurrency, String optype) {
                // select bcbbr from imbcbbrp where a8brcd='001' ccode by branch
        try {
            return findAccountExchange(keys.getCompanyCode(), bankCurrency.getCurrencyCode(), keys.getPassiveActive(), optype);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public GLAccParam findAccountExchange(String ccode, String ccy, String psav, String optype) throws SQLException {
        // select bcbbr from imbcbbrp where a8brcd='001' ccode by branch
        List<DataRecord> results = select(
                "select acid, bsaacid from excacrln where CCODE = ? and CCY = ? and CASH = ? and PSAV = ?"
                , ccode, ccy, optype, psav);

        if (1 == results.size()) {
            return new GLAccParam(results.get(0).getString("ACID"), results.get(0).getString("BSAACID"));
        } else if (1 < results.size()) {
            throw new ValidationError(ErrorCode.TOO_MANY_ACCRLN_ENTRIES, ccode, ccy, "N", psav);
        } else {
            return null;
        }
    }
}
