package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.acc.AccRlnId;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.acc.GlAccRln;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
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
import static ru.rbt.ejbcore.validation.ErrorCode.ACCOUNT_RLN_INVALID;

/**
 * Created by ER18837 on 05.05.15.
 */
public class AccRlnRepository extends AbstractBaseEntityRepository<GlAccRln, AccRlnId> {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public GlAccRln createAccRln(GLAccount glAccount) {
        GlAccRln accRln = new GlAccRln();
        AccRlnId id = new AccRlnId();
        id.setAcid(ifEmpty(glAccount.getAcid(), " "));
        id.setBsaAcid(glAccount.getBsaAcid());
        accRln.setId(id);

        accRln.setRelationType(glAccount.getRelationType());
        accRln.setDateOpen(glAccount.getDateOpen());
        accRln.setDateClose(glAccount.getDateCloseNotNull());
        accRln.setCustomerType((null != glAccount.getCbCustomerType()) ? glAccount.getCbCustomerType() : 0);
        accRln.setCustomerNumber(glAccount.getCustomerNumber());
        accRln.setCompanyCode(glAccount.getCompanyCode());
        accRln.setBssAccount(glAccount.getBsaAcid().substring(0, 5));
        accRln.setPassiveActive(glAccount.getPassiveActive());
        accRln.setAccountCode(glAccount.getAccountCode() != null ? glAccount.getAccountCode().toString() : "");
        accRln.setCurrencyD(glAccount.getCurrency().getDigitalCode());
        accRln.setPlCode(glAccount.getPlCode());
        accRln.setIncludeExclude("0");
        accRln.setTransactSrc("000");
        accRln.setPairBsa("");

        return save(accRln);
    }

    public Optional<GlAccRln> findAccRln(GLAccount glAccount) {
        return Optional.ofNullable(selectFirst(GlAccRln.class, "from GlAccRln r where r.id.acid = ?1 and r.id.bsaAcid = ?2"
                , glAccount.getAcid(), glAccount.getBsaAcid()));
    }

    /**
     * Поиск по счету ЦБ
     *
     * @param bsaacid счет ЦБ
     * @return
     */
    public DataRecord findByBsaacid(String bsaacid) throws SQLException {
        return selectFirst("SELECT * FROM GL_ACC R WHERE R.BSAACID = ? ORDER BY R.DTC DESC", bsaacid);
    }

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

    public GlAccRln updateRelationType(AccRlnId id, GLAccount.RelationType relationType) {
        executeUpdate("update GlAccRln r set r.relationType = ?1 where r.id = ?2", relationType.getValue(), id);
        return findById(GlAccRln.class, id);
    }

    /**
     * Находит счет GLпо номеру счета ЦБ
     *
     * @param bsaAcid
     * @return
     */
/*
    public GlAccRln findAccRlnAccount(String bsaAcid) {
        try {
            DataRecord data = selectFirst("SELECT * FROM ACCRLN R WHERE R.BSAACID = ? ORDER BY R.DRLNC DESC", bsaAcid);

            return (null == data) ? null : findById(GlAccRln.class, new AccRlnId(data.getString("ACID"), bsaAcid));
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }
*/

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

    public DataRecord checkAccountBalance(GLAccount account, Date operDate, BigDecimal amount,GlAccRln tehover)
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
            DataRecord res = selectFirst(sql, operDate, account.getBsaAcid(), account.getAcid(), operDate, tehover.getId().getBsaAcid(), tehover.getId().getAcid(), operDate, account.getBsaAcid(), account.getAcid(), operDate,operDate, tehover.getId().getBsaAcid(), tehover.getId().getAcid(), operDate, amount);
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

    public GlAccRln findAccountTehover(String bsaAcid, String acid)
    {
        try {
            DataRecord data = selectFirst("select * from ACCPAIR where bsaacid = ? and acid = ? and datto='2029-01-01'", bsaAcid,acid);

            return (null == data) ? null : findById(GlAccRln.class, new AccRlnId(data.getString("PAIRACID"), data.getString("PAIRBSAACID")));
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }
}
