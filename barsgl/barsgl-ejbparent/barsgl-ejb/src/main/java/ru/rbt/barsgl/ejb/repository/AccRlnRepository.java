package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.acc.AccRlnId;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.acc.GlAccRln;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;

import javax.persistence.NoResultException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static ru.rbt.barsgl.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.ACCOUNT_RLN_INVALID;

/**
 * Created by ER18837 on 05.05.15.
 */
public class AccRlnRepository extends AbstractBaseEntityRepository<GlAccRln, AccRlnId> {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public GlAccRln createAccRln(GLAccount glAccount) {
        GlAccRln accRln = new GlAccRln();
        AccRlnId id = new AccRlnId();
        id.setAcid(glAccount.getAcid());
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
     * Поиск по счету Майдас
     *
     * @param acid   счет Майдас
     * @param ondate на дату
     * @return
     */
    public List<DataRecord> findByAcid(String acid, String acc2, Date ondate, boolean plCodeExists) throws SQLException {
        return select("select * from ACCRLN r where R.ACID = ? and ACC2 = ? and R.DRLNC > ? and RLNTYPE in (" +
                (plCodeExists ? "'2'" : "'0', '4'") + ")"
            , acid, acc2, DateUtils.onlyDate(ondate));
    }
    public List<DataRecord> findByAcid_Rlntype0(String acid, Date ondate) throws SQLException {
        return select("select * from ACCRLN r where R.ACID = ? and R.DRLNC > ? and RLNTYPE='0'", acid, DateUtils.onlyDate(ondate));
    }
    /**
     * Поиск по счету ЦБ
     *
     * @param bsaacid счет ЦБ
     * @return
     */
    public DataRecord findByBsaacid(String bsaacid) throws SQLException {
        return selectFirst("SELECT * FROM ACCRLN R WHERE R.BSAACID = ? ORDER BY R.DRLNC DESC", bsaacid);
    }
//
//    public Optional<GlAccRln> findByBsaacid (String bsaacid) {
//        return Optional.ofNullable(selectFirst(GlAccRln.class, "FROM ACCRLN WHERE BSAACID = ? ORDER BY DRLNC DESC", bsaacid));
//    }

    /**
     * Поиск счета ЦБ по ключам для GLSEQ="GL%"
     *
     * @param keys
     * @return счет ЦБ
     */
    public String findForSequenceGL(AccountKeys keys) throws Exception {
        List<DataRecord> results = select("select bsaacid from accrln where CCODE = ? and CBCCY = ? and ACC2 = ? and RLNTYPE = 'T' and GLACOD = ''"
            , keys.getCompanyCode(), keys.getCurrencyDigital(), keys.getAccount2());
        if (1 == results.size()) {
            return results.get(0).getString("BSAACID");
        } else if (1 < results.size()) {
            throw new ValidationError(ErrorCode.TOO_MANY_ACCRLN_ENTRIES, keys.getCompanyCode(), keys.getCurrencyDigital(), keys.getAccount2());
        } else {
            throw new ValidationError(ErrorCode.NOT_FOUND_ACCRLN_GL, keys.getCompanyCode(), keys.getCurrencyDigital(), keys.getAccount2());
        }
    }

    /**
     * Обновляет дату закрытия счета
     *
     * @param glAccount
     */
    public void setDateOpen(GLAccount glAccount) {
        int cnt = executeNativeUpdate("update ACCRLN set DRLNO = ? where ACID = ? and BSAACID = ?",
            glAccount.getDateOpen(), glAccount.getAcid(), glAccount.getBsaAcid());
        if (1 != cnt) {
            throw new ValidationError(ACCOUNT_RLN_INVALID, glAccount.getBsaAcid(), glAccount.getAcid());
        }
    }

    /**
     * Обновляет дату закрытия счета
     *
     * @param glAccount
     */
    public void setDateClose(GLAccount glAccount) {
        int cnt = executeNativeUpdate("update ACCRLN set DRLNC = ? where ACID = ? and BSAACID = ?",
            glAccount.getDateCloseNotNull(), glAccount.getAcid(), glAccount.getBsaAcid());
        if (1 != cnt) {
            throw new ValidationError(ACCOUNT_RLN_INVALID, glAccount.getBsaAcid(), glAccount.getAcid());
        }
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

            List<DataRecord> results = select("select bsaacid from accrln where ACID = ? and RLNTYPE = ? and PLCODE = ? and CTYPE = ? and DRLNO >= ? and DRLNC >= ? "
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
}
