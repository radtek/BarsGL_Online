package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.acc.*;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static ru.rbt.ejbcore.util.StringUtils.ifEmpty;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by ER18837 on 05.05.15.
 */
@Deprecated
public class AccRlnRepository <E extends BaseEntity<String>> extends AbstractBaseEntityRepository<E, String> {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Поиск счета MIDAS по ключам
     *
     * @param keys
     * @return счет Midas
     * @throws Exception
     */
    public String findForPlcodeNo7903(AccountKeys keys, Date dateOpen, Date dateStart446P) {
        try {

            String m_acid = keys.getAccountMidas();

            // todo перенести или нет в метод подготовки ключей
            // Встречается keys.getCustomerType() == ""
            int customerType = isEmpty(keys.getCustomerType()) ? 0 : Integer.parseInt(keys.getCustomerType());

            List<DataRecord> results = select("select BSAACID from GL_ACC where ACID = ? and RLNTYPE = ?" +
                            " and PLCODE = ? and CBCUSTTYPE = ? and DTO >= ? and (DTC is null or DTC >= ?) "
                    , m_acid, "2", keys.getPlCode(), customerType, dateStart446P, dateOpen);

            if (1 == results.size()) {
                String bsaacid = results.get(0).getString("BSAACID");
                return bsaacid;
            } else if (1 < results.size()) {
                throw new ValidationError(ErrorCode.TOO_MANY_ACCRLN_ENTRIES, m_acid, "2", keys.getPlCode(), keys.getCustomerType(), sdf.format(dateOpen));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public DataRecord checkAccountBalance(GLAccount account, Date operDate, BigDecimal amount)
    {
        try{
            String where = "П".equalsIgnoreCase(account.getPassiveActive())?"outrest<0":"outrest>0";
            DataRecord res = selectFirst(String.format("with ACC_TOVER as (" +
                        "select  DAT,  DTAC + CTAC + BUF_DTAC + BUF_CTAC as BAC from V_GL_ACC_TOVER where DAT > CAST(? AS DATE) and bsaacid = ? and acid = ? " +
                        "UNION ALL " +
                        "select CAST(? AS DATE) as dat, NVL(GET_BALANCE(CAST(? AS VARCHAR2(20)),CAST(? AS VARCHAR2(20)),CAST(? AS DATE)),0) as bac from dual " +
                        ") " +
                        "select * " +
                        "FROM " +
                        "( " +
                        "select dat,(select sum(bac) from acc_tover a where a.dat <= o.dat) as bac,(select sum(bac) from acc_tover a where a.dat <= o.dat) + ? as outrest " +
                        "from ACC_TOVER o " +
                        ")t " +
                        "where %s order by dat",where), operDate, account.getBsaAcid(), account.getAcid(), operDate, account.getBsaAcid(), account.getAcid(), operDate, amount);
            return res;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public DataRecord checkAccountBalance(GLAccount account, Date operDate, BigDecimal amount, GLAccParam tehover)
    {
        try {
            String where = "П".equalsIgnoreCase(account.getPassiveActive())?"outrest<0":"outrest>0";
            String sql = String.format("with ACC_TOVER as (" +
                    "SELECT DAT,SUM(BAC) as bac " +
                    "FROM(" +
                    "select  DAT,  DTAC + CTAC + BUF_DTAC + BUF_CTAC as BAC from V_GL_ACC_TOVER where DAT > CAST(? AS DATE) and bsaacid = ? and acid = ?" +
                    " UNION ALL " +
                    "select  DAT,  DTAC + CTAC + BUF_DTAC + BUF_CTAC as BAC from V_GL_ACC_TOVER where DAT > CAST(? AS DATE) and bsaacid = ? and acid = ?" +
                    ") T1 " +
                    " GROUP BY DAT " +
                    " UNION ALL " +
                    "SELECT dat,sum(bac) as bac " +
                    "FROM(" +
                    "select CAST(? AS DATE) as dat, NVL(GET_BALANCE(CAST(? AS VARCHAR2(20)),CAST(? AS VARCHAR2(20)),CAST(? AS DATE)),0) as bac from dual" +
                    " UNION ALL " +
                    "select CAST(? AS DATE) as dat, NVL(GET_BALANCE(CAST(? AS VARCHAR2(20)),CAST(? AS VARCHAR2(20)),CAST(? AS DATE)),0) as bac from dual" +
                    ") t2" +
                    " group by dat) " +
                    "select * FROM (" +
                    "select dat,(select sum(bac) from acc_tover a where a.dat <= o.dat) as bac,(select sum(bac) from acc_tover a where a.dat <= o.dat) + ? as outrest " +
                    "from ACC_TOVER o)t " +
                    " where %s ",where);
            DataRecord res = selectFirst(sql, operDate, account.getBsaAcid(), account.getAcid(), operDate, tehover.getBsaAcid(), tehover.getAcid(), operDate, account.getBsaAcid(), account.getAcid(), operDate,operDate, tehover.getBsaAcid(), tehover.getAcid(), operDate, amount);
            return res;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public boolean checkAccointIsPair(String bsaAcid)
    {
        try {
            DataRecord res = selectFirst("SELECT * FROM ACCOCREP WHERE RECNTR <> 'КОРСЧ' and recbac = ?",bsaAcid.substring(0,5));
            return res!=null?true:false;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public GLAccParam findAccountTehover(String bsaAcid, String acid)
    {
        try {
            DataRecord data = selectFirst("select * from ACCPAIR where bsaacid = ? and acid = ? and datto='2029-01-01'", bsaAcid,acid);

            return (null == data) ? null : new GLAccParam(data.getString("PAIRACID"), data.getString("PAIRBSAACID"));
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }
}
