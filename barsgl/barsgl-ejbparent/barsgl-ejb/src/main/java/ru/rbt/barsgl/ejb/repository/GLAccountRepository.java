package ru.rbt.barsgl.ejb.repository;

import ru.rb.cfg.SystemConfiguration;
import ru.rb.cfg.db.DbConfiguration;
import ru.rb.cfg.exception.ConfigurationException;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.acc.GLAccountRequest;
import ru.rbt.barsgl.ejb.entity.dict.AccountingType;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejbcore.validation.ResultCode;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static ru.rbt.ejbcore.util.StringUtils.ifEmpty;
import static ru.rbt.ejbcore.util.StringUtils.substr;
import static ru.rbt.ejbcore.validation.ErrorCode.*;

/**
 * Created by ER18837 on 29.04.15.
 */
@Stateless
@LocalBean
public class GLAccountRepository extends AbstractBaseEntityRepository<GLAccount, Long> {

    private static final String CFG_NAME_446P = "446P";
    private static final String PROP_NAME_446P = "START_446P_DATE";
    private static final String CFG_NAME_SPOD = "SPOD";
    private static final String PROP_NAME_SPOD = "LAST_SPOD_DAY";
    public static final SimpleDateFormat spodDateFmt = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Проверяет наличие в БД счета Майдас
     *
     * @param acid - счет Майдас
     * @return - true - есть
     */
    public boolean checkMidasAccountExists(String acid, Date dateCurrent) {
        try {
            DataRecord res = selectFirst("select 1 from ACC A where A.ID = ?", acid);
            return null != res;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public Date getGlAccOpenDate(String bsaacid) throws Exception{
        try {
            DataRecord res = selectFirst("select dto from gl_acc where bsaacid=?", bsaacid);
            return res.getDate(0);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public void updGlAccOpenDate(String bsaacid, Date newDate) throws Exception{
            executeNativeUpdate("update gl_acc set dto=? where bsaacid=?", newDate, bsaacid);
    }

    public void updBsaaccOpenDate(String bsaacid, Date newDate) throws Exception {
        executeNativeUpdate("update Bsaacc set bsaaco=? where id=?", newDate, bsaacid);
    }

    public void updAccrlnOpenDate(String bsaacid, Date newDate) throws Exception {
        executeNativeUpdate("update Accrln set drlno=? where bsaacid=?", newDate, bsaacid);
    }

    /**
     * Проверяет наличие в БД счета ЦБ
     *
     * @param bsaAcid - счет ЦБ
     * @return - true - есть
     */
    public boolean checkBsaAccountExists(String bsaAcid) {
        try {
            DataRecord res = selectFirst("select count(1) from BSAACC B where B.ID = ?", bsaAcid);
            return res.getInteger(0) > 0;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public boolean checkBsaAccountOpen(String bsaAcid, Date dateCurrent) {
        try {
            DataRecord res = selectFirst("select count(1) from BSAACC B where B.ID = ?" +
                    " and B.BSAACO <= ? and (B.BSAACC is null or B.BSAACC >= ?)", bsaAcid, dateCurrent, dateCurrent);
            return res.getInteger(0) > 0;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public ResultCode checkBsaAccountAnal(String bsaAcid, Date vDate, Date operDate, Date lastOperDate) {
        try {
            DataRecord res = selectFirst("select case when BSAACO > ? then " + ResultCode.ACCOUNT_IS_OPEN_LATER.getValue() +
                                                " when BSAACC < ? then "+ ResultCode.ACCOUNT_IS_CLOSED_BEFOR.getValue() +
                                                " else "+ ResultCode.SUCCESS.getValue() +" end " +
                                         "from BSAACC where ID = ?"
                    , vDate, !vDate.before(lastOperDate)? vDate: operDate, bsaAcid);

            return (null == res) ? ResultCode.ACCOUNT_NOT_FOUND : ResultCode.valueOf(res.getInteger(0));
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public ResultCode checkBsaAccountGlAcc(String bsaAcid)
    {
        try {
            DataRecord res = selectFirst("select 0 from GL_ACC where bsaacid = ?", bsaAcid);

            return (null == res) ? ResultCode.ACCOUNT_NOT_FOUND : ResultCode.valueOf(res.getInteger(0));
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }


    public int checkBsaAccount(String bsaAcid, Date dateCurrent) {
        try {
            DataRecord res = selectFirst("select" +
                    " case when BSAACO > ? or BSAACC < ? then 0 else 1 end" +
                    " from BSAACC B where B.ID = ?"
                    , dateCurrent, dateCurrent, bsaAcid);
            return (null == res) ? -1 : res.getInteger(0);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public boolean checkAccountRlnExists(String bsaAcid, String acid) {
        try {
            DataRecord res = selectFirst("select count(1) from ACCRLN where BSAACID = ? and ACID = ?", bsaAcid, acid);
            return res.getInteger(0) > 0;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public boolean checkAccountRlnExists(String bsaAcid, String acid, String rlntype) {
        try {
            DataRecord res = selectFirst("select count(1) from ACCRLN where BSAACID = ? and ACID = ? and RLNTYPE = ?", bsaAcid, acid, rlntype);
            return res.getInteger(0) > 0;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public boolean checkAccountRlnNotPseudo(String bsaAcid) {
        try {
            DataRecord res = selectFirst("select count(1) from ACCRLN where BSAACID = ? and RLNTYPE in ('2', '5') and CBCCY='810'", bsaAcid);
            return res.getInteger(0) > 0;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public DataRecord getAcc2ByAcid(String acid, Date dateOpen) {
        try {
            String sql = "select ACC2, PSAV from ACCRLN where ACID = ? and RLNTYPE = '0' and ? between DRLNO and DRLNC";
            DataRecord res = selectFirst(sql, acid, dateOpen);
            return res;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Метод плучения записи из GL_ACCPARM
     * @param accType код типа счёта
     * @return даные из таблицы
     */
    public DataRecord getActParamByAccType(String accType)
    {
        try
        {
            String sql = "select ACC2, ACOD, AC_SQ from GL_ACTPARM where ACCTYPE = ? and CUSTYPE = '00' and TERM='00' and DTE IS NULL";
            DataRecord res = selectFirst(sql, accType);
            return res;
        }
        catch (SQLException e){
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public DataRecord getCbccy(String ccy)
    {
        try
        {
            String sql = "select CBCCY from CURRENCY where GLCCY = ?";
            DataRecord res = selectFirst(sql, ccy);
            return res;
        }
        catch (SQLException e){
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }


    public String getCBCC(String cbccn)
    {
        try
        {
            String sql = "select CCPCD from IMBCBCMP where CCBBR = ?";
            DataRecord res = selectFirst(sql, cbccn);
            return null!=res?res.getString("CCPCD"):"";
        }
        catch (SQLException e){
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public DataRecord getIMBCBBRP(String cbccn)
    {
        try
        {
            String sql = "select A8BRCD,A8BICN from IMBCBBRP where BCBBR = ?";
            DataRecord res = selectFirst(sql, cbccn);
            return res;
        }
        catch (SQLException e){
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Определяет код валюты ЦБ по номеру счета ЦБ
     *
     * @param bsaAcid - счет ЦБ
     * @return - код валюты (3 цифры)
     */
    public String getAccountCurrency(String bsaAcid) {
        return substr(bsaAcid, 5, 8);
    }

    public String getAccountCustNo(String bsaAcid) {
        try {
            String sql = "select BSAACNNUM from BSAACC where ID = ?";
            DataRecord res = selectFirst(sql, bsaAcid);
            return null != res ? res.getString(0) : "";
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }

    }
    /**
     * Определяет имя клиента по номеру клиента
     *
     * @param cnum - номер клиента (8 цифр)
     * @return - имя клиента
     */
    public String getCustomerName(String cnum) {
        try {
            String sql = "select BBCNA1 from SDCUSTPD where BBCUST = ?";
            DataRecord res = selectFirst(sql, cnum);
            return null != res ? res.getString(0) : "";
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Определяет тип клиента по номеру клиента
     *
     * @param cnum - номер клиента (8 цифр)
     * @return - тип клиента (может быть null)
     */
    public Short getCustomerType(String cnum) {
        try {
            String sql = "select BXCTYP from SDCUSTPD where BBCUST = ?";
            DataRecord res = selectFirst(sql, cnum);
            return null != res ? res.getShort(0) : null;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Определяет признак актив / пассив для балансового счета второго порядка)
     *
     * @param acc2 - балансового счет
     * @return - "П" / "А" (русские)
     */

    public String getPassiveActive(String acc2) {
        try {
            String sql = "select PSAV,TYPE from BSS where ACC2 = ?";
            DataRecord res = selectFirst(sql, acc2);
            return (null != res) ? res.getString(0) : "";
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public String[] getPassiveActiveType(String acc2) {
        try {
            String sql = "select PSAV,TYPE from BSS where ACC2 = ?";
            DataRecord res = selectFirst(sql, acc2);
            return (null != res) ? new String[]{res.getString(0),res.getString(1)} : new String[]{"",""};
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }
    /**
     * Определяет код компании по филиалу
     *
     * @param filial - филиал (3 буквы)
     * @return - код компании (4 цифры)
     */
    public String getCompanyCodeByFilial(String filial) {
        try {
            String sql = "select CCBBR from IMBCBCMP where CCPCD = ?";
            DataRecord res = selectFirst(sql, filial);
            return null != res ? res.getString(0) : "";
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Определяет филиал по коду компании
     *
     * @param cbccn - код компании (4 цифры)
     * @return - филиал (3 буквы)
     */
    public String getFilialByCompanyCode(String cbccn) {
        try {
            String sql = "select CCPCD from IMBCBCMP where CCBBR = ?";
            DataRecord res = selectFirst(sql, cbccn);
            return null != res ? res.getString(0) : "";
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Определяет символьный и числовой код филиала по бранчу
     *
     * @param branch - бранч (3 цифры)
     * @return - 0 - филиал (3 буквы), 1 - код компании (4 цифры)
     */
    public DataRecord getFilialByBranch(String branch) {
        try {
            String sql = "SELECT A8CMCD, BCBBR from IMBCBBRP  where A8BRCD = ?";
            DataRecord res = selectFirst(sql, branch);
            return res;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Определяет символьный и числовой код филиала по бранчу
     *
     * @param branchFlex - бранч FLEX (3 буквы)
     * @return - бранч Midas (3 цифры)
     */
    public String getBranchByFlex(String branchFlex) {
        try {
            String sql = "SELECT MIDAS_BRANCH BRANCH from DH_BR_MAP where FCC_BRANCH = ?";
            DataRecord res = selectFirst(sql, branchFlex);
            return null != res ? res.getString(0) : "";
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Определяет по справочнику параметры счета Майдас
     *
     * @param accType - тип счета
     * @return - 0 - AccountCode (4 цифры), 1 - AccountSequence (2 цифры))
     */
    public boolean checkAccountTypeExists(long accType) {
        try {
            String sql = "select 1 from GL_ACСPRM where ACCTYPE = ?";
            DataRecord res = selectFirst(sql, accType);
            return (null != res);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Получить параметры счета Midas по ключам счета
     *
     * @param accType
     * @param custype
     * @param term
     * @param curdate
     * @return
     */
    public DataRecord getAccountParams(String accType, String custype, String term, Date curdate) {
        try {
            Assert.notNull(curdate, "Параметр 'curdate' не может быть null");
            String sql = "select ACC2, PLCODE, ACOD, AC_SQ SQ, CUSTYPE from GL_ACTPARM " +
                    "where ACCTYPE = ? and CUSTYPE = ? and TERM = ? " +
                    "and DTB <= ? and (DTE is null or DTE >= ?)";
            DataRecord res = selectFirst(sql, accType, ifEmpty(normalizeString(custype), "00"), ifEmpty(term, "00"), curdate, curdate);
            if (res == null) {
                res = selectFirst(sql, accType, ifEmpty(custype, "00"), "00", curdate, curdate);
            }
            if (res == null) {
                res = selectFirst(sql, accType, "00", ifEmpty(term, "00"), curdate, curdate);
            }
            if (res == null) {
                res = selectFirst(sql, accType, "00", "00", curdate, curdate);
            }
            return res;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private static String normalizeString(String number) {
        if (!isEmpty(number) && !"0".equalsIgnoreCase(number) && !"00".equalsIgnoreCase(number)) {
            return Long.toString(Long.parseLong(number));
        } else if ("0".equalsIgnoreCase(number)) {
            return "00";
        } else {
            return number;
        }
    }

    /**
     * Получить параметры сиквенса Майдас по источнику сделки
     *
     * @param src
     * @param dateOpen
     * @return
     */
    public DataRecord getDealSQParams(String src, Date dateOpen) {
        try {
            String sql = "select * from GL_SQPARAM where SRC_PST = ? and (DTE is null or DTE >= ?)";
            DataRecord res = selectFirst(sql, src, dateOpen);
            return res;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Получить сиквенс Майдас для клиента по номеру сделки и валюте
     *
     * @return
     */
    public String getDealSQ(String custNo, String ccy, String dealId) {
        try {
            String sql = "select ACSQ from GL_SQVALUE where CUSTNO = ? and CCY = ? and DEAL_ID = ?";
            DataRecord res = selectFirst(sql, custNo, ccy, dealId);
            return null != res ? res.getString(0) : "";
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Получить максимальный сиквенс Майдас для клиента по валюте
     *
     * @return
     */
    public String getDealSQmax(String custNo, String ccy) {
        try {
            String sql = "select max(ACSQ) from GL_SQVALUE where CUSTNO = ? and CCY = ?";
            DataRecord res = selectFirst(sql, custNo, ccy);
            return null != res ? res.getString(0) : "";
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Получить добавить сиквенс Майдас для клиента по валюте и номеру сделке
     *
     * @return
     */
    public void addDealSQ(String custNo, String ccy, String dealId, String sq) {
        String sql = "insert into GL_SQVALUE (CUSTNO, DEAL_ID, CCY, ACSQ) values (?, ?, ?, ?)";
        executeNativeUpdate(sql, custNo, dealId, ccy, sq);
    }

    /**
     * Определяет название создаваемого счета по типу счета
     *
     * @param accType - тип счета
     * @return
     */
    public String getAccountNameByType(Long accType) {
        try {
            String sql = "select ACCNAME from GL_ACTNAME where ACCTYPE = ?";
            DataRecord res = selectFirst(sql, accType);
            return null != res ? res.getString(0) : "Тип счета: " + accType;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Формирует счет Майдас по параметрам счета
     *
     * @param cnum
     * @param ccy
     * @param branch
     * @param acod
     * @param seq
     * @return
     */
    public String makeMidasAccount(int cnum, String ccy, String branch, short acod, short seq) {
        String acid = format("%08d%s%04d%02d%s", cnum, ccy, acod, seq, branch);
        Assert.isTrue(20 == acid.length(), format("Неверная длина счета Майдас '%s' размер '%d'", acid, acid.length()));
        return acid;
    }

    /**
     * Находит счет в таблице GL_ACC по набору ключей (открытие из операции)
     *
     * @param branch         - бранч
     * @param currency       - валюта ЦБ
     * @param customerNumber - номер клиента
     * @param accountType    - тип счета
     * @param cbCustType     - тип клиента ЦБ
     * @param term           - код периода
     * @param glSeguence     - номер из АЕ
     * @return
     */
    public GLAccount findGLAccountAE(String branch, String currency, String customerNumber,
                                     String accountType, String cbCustType, String term, String glSeguence, Date dateCurrent) {
        try {
            //todo XX
            String addForXX = "";
            if (!isEmpty(glSeguence) && glSeguence.toUpperCase().startsWith("XX")){
                addForXX = " and (dealid is null or dealid ='' or dealid ='нет') and (subdealid is null or subdealid ='' or subdealid ='нет')";
            }
            DataRecord data = selectFirst("select ID from GL_ACC where " +
                            " BRANCH = ? and CCY = ? and CUSTNO = ? and ACCTYPE = ?" +
                            " and GL_SEQ = ?" +
                            " and (DTC is null or DTC > ?) and coalesce(CBCUSTTYPE, 0) = ? and coalesce(TERM, 0) = ? "
                            + addForXX
                    , branch, currency, customerNumber, accountType, glSeguence, dateCurrent
                    , ifEmpty(cbCustType, "0"), ifEmpty(term, "0"));
            return (null == data) ? null : findById(GLAccount.class, data.getLong(0));
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Находит счет в таблице GL_ACC по набору ключей (открытие вручную)
     *
     * @param branch         - бранч
     * @param currency       - валюта ЦБ
     * @param customerNumber - номер клиента
     * @param accountType    - тип счета
     * @param cbCustType     - тип клиента ЦБ
     * @param term           - код периода
     * @param dealSrc        - источник сделки
     * @param dealId         - ИД сделки
     * @param dateCurrent    - текущая дата
     * @return
     */
    public GLAccount findGLAccountMnl(String branch, String currency, String customerNumber,
                                      String accountType, String cbCustType, String term,
                                      String dealSrc, String dealId, String subdealId, Date dateCurrent) {
        try {
            DataRecord data = selectFirst("select ID from GL_ACC where " +
                            "BRANCH = ? and CCY = ? and CUSTNO = ? and ACCTYPE = ?" +
                            " and coalesce(DEALID, ' ') = ? and coalesce(SUBDEALID, ' ') = ?" +
                            " and (DTC is null or DTC > ?) and coalesce(CBCUSTTYPE, 0) = ? and coalesce(TERM, 0) = ? "
                    , branch, currency, customerNumber
                    , accountType, ifEmpty(dealId, " "), ifEmpty(subdealId, " "), dateCurrent
                    , ifEmpty(cbCustType, "0"), ifEmpty(term, "0"));
            return (null == data) ? null : findById(GLAccount.class, data.getLong(0));
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * "свои" счета доходов/расходов открытых в BARS GL
     */
    public GLAccount findGLPLAccount(String currency, String customerNumber,
                                      String accountType, String cbCustType, String term, String plcode, String cbccn,
                                      Date dateCurrent) {
        try {
            String custTypeField = "CBCUSTTYPE";
            String termField = "TERM";
            if (isEmpty(cbCustType)) {
                cbCustType = "-1";
                custTypeField = "coalesce(CBCUSTTYPE, -1)";
            }
            if (isEmpty(term)) {
                term = "-1";
                termField = "coalesce(TERM, -1)";
            }
            return Optional.ofNullable(selectFirst(
                            "select ID " +
                            "  from GL_ACC " +
                            " where CCY = ? and CUSTNO = ? and ACCTYPE = ? and PLCODE = ? and CBCCN = ?" +
                            "   and DTO <= ? and (DTC is null or DTC > ?) and RLNTYPE = ? AND "
                    + custTypeField + " = ? and " + termField + " = ? "
                    , currency, customerNumber, accountType, plcode, cbccn, dateCurrent, dateCurrent
                    , GLAccount.RelationType.FIVE.getValue(), cbCustType, term))
                    .map((record) -> findById(GLAccount.class, record.getLong("ID"))).orElse(null);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public List<DataRecord> findByAcidRlntype04(String acid, Date ondate) throws SQLException{
        return select("select * from gl_acc where acid=? and DTO <= ? and (DTC is null or DTC > ?) "+
                      "and rlntype in ('0','4') "+
                      "and (dealid is null or dealid ='' or dealid ='нет') "+
                      "and (subdealid is null or subdealid ='' or subdealid ='нет')",
               acid,  ondate, ondate);
    }

    /**
     * "свои" счета доходов/расходов открытых в BARS GL вручную!
     */
    public GLAccount findGLPLAccountMnl(String currency, String customerNumber,
                                     String accountType, String cbCustType, String term, String plcode, String acc2, String cbccn,
                                     Date dateCurrent) {
        try {
            return Optional.ofNullable(selectFirst(
                    "select ID from GL_ACC " +
                            " where CCY = ? and CUSTNO = ? and ACCTYPE = ? and PLCODE = ? and CBCCN = ?" +
                            "  and DTO <= ? and (DTC is null or DTC > ?)" +
                            "  and ACC2 = ? and CBCUSTTYPE = ? and TERM = ? "
                    , currency, customerNumber, accountType, plcode, cbccn, dateCurrent, dateCurrent
                    , acc2, cbCustType, term))
                    .map((record) -> findById(GLAccount.class, record.getLong("ID"))).orElse(null);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Обновляем RLNTYPE в GL_ACC и ACCRLN
     * @param account счет GL
     * @param relationType relationType
     * @return обновленный счет
     */
    public GLAccount updateRelationType(final GLAccount account, final GLAccount.RelationType relationType) {
        int count = executeUpdate("update GlAccRln r set r.relationType = ?1 where r.id.bsaAcid = ?2", relationType.getValue(), account.getBsaAcid());
        Assert.isTrue(1 == count
            , () -> new DefaultApplicationException(format("Неверное кол-во записей '%s' в ACCRLN по счету '%s'", count, account.getBsaAcid())));
        account.setRelationType(relationType);
        return update(account);
    }

    /**
     * Находит счет GLпо номеру счета ЦБ
     *
     * @param bsaAcid
     * @return
     */
    public GLAccount findGLAccount(String bsaAcid) {
        try {
            DataRecord data = selectFirst("select ID from GL_ACC where BSAACID = ?", bsaAcid);
            return (null == data) ? null : findById(GLAccount.class, data.getLong(0));
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }



    public boolean isExistsGLAccountByOpenType(String bsaAcid) {
        try {
            DataRecord data = selectFirst("select ID from GL_ACC where BSAACID = ? and opentype is not null and opentype!='MIGR'", bsaAcid);
            return null != data;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public BigDecimal getAccountBalance(String bsaAcid, String acid, Date datto) {
        try {
            DataRecord data = selectFirst("SELECT sum(OBAC) + sum(DTAC) + sum(CTAC) from" +
                    " (SELECT BSAACID, ACID, OBAC, DTAC, CTAC from BALTUR where DATTO = ? and ACID = ? and BSAACID = ? union all" +
                    " SELECT BSAACID, ACID, 0 as OBAC, DTAC, CTAC from GL_BALTUR WHERE MOVED = 'N' and DAT <= ? and ACID = ? and BSAACID = ?" +
                    ") T group by BSAACID, ACID", datto, acid, bsaAcid, datto, acid, bsaAcid);
            return (null == data) ? BigDecimal.ZERO : data.getBigDecimal(0);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public Boolean hasAccountBalanceBefore(String bsaAcid, String acid, Date dat) {
        try {
            DataRecord data = selectFirst("SELECT * from BALTUR where DAT < ? and ACID = ? and BSAACID = ? "
                    , dat, acid, bsaAcid);
            return (null != data);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public Boolean hasAccountBalanceAfter(String bsaAcid, String acid, Date dat) {
        try {
            DataRecord data = selectFirst("SELECT * from BALTUR where DAT > ? and ACID = ? and BSAACID = ? "
                    , dat, acid, bsaAcid);
            return (null != data);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public String getRequestCurrency(GLAccountRequest request) {
        try {
            DataRecord res = selectFirst("select CCY from GL_ACOPENRQ where REQUEST_ID = ?", request.getId());
            return null != res ? res.getString(0) : "";
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /*don't used IMBCBHBPN*/
    /**
     * Получить символ ОФР (PLCODE) по параметрам счета
     *
     * @param acod
     * @param sq
     * @param custType
     * @param curdate
     * @return
     */
    /*
    public String getPlCode(String acod, short sq, short custType, Date curdate) {
        try {
            DataRecord res = selectFirst("select HBITEM from IMBCBHBPN where HBMIAC = ? and HBMISQ in (?,0) and HBCTYP = ?" +
                    " and DAT <= ? and (DATTO = '2029-01-01' or DATTO >= ?)", acod, sq, custType, curdate, curdate);
            return null != res ? res.getString(0) : "";
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }
    */

    /**
     * Возвращает дату начала действия 446П
     *
     * @return
     */
    public Date getDateStart446pGl() {
        try {
            DataRecord res = selectOne("select DATE_VALUE from DB_CFG where CFG_NAME = ? and PROP_NAME = ?",
                    CFG_NAME_446P, PROP_NAME_446P);
            return null != res ? res.getDate(0) : null;
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private SystemConfiguration getCfg(String cfgName, Connection connection) throws ConfigurationException {
        SystemConfiguration cfg = SystemConfiguration.getInstance();
        try {
            cfg.get(cfgName);     // проверяем, что конфиг уже есть
        } catch (ConfigurationException e) {
            cfg.add(DbConfiguration.createDbConfiguration(cfgName, connection));
        }
        return cfg;
    }

    public Date getDateStart446p() {
        try {
            return executeInNonTransaction(connection -> {
                try {
                    SystemConfiguration cfg = getCfg(CFG_NAME_446P, connection);     // проверяем, что конфиг уже есть
                    return cfg.getDate(PROP_NAME_446P);
                } catch (Exception e) {
                    throw new DefaultApplicationException(e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public Date getLastSpodDate(Date operday) {
        try {
            return executeInNonTransaction(connection -> {
                try {
                    String strDate = getCfg(CFG_NAME_SPOD, connection).getString(PROP_NAME_SPOD);
                    Assert.isTrue(strDate.matches("-(0\\d|1[012])-([012]\\d|3[01])"),
                            format("Неверный формат даты в таблице конфигурации для '%s': '%s', должен быть '-MM-dd'", PROP_NAME_SPOD, strDate));

                    Calendar cal = Calendar.getInstance();
                    cal.setTime(operday);
                    return spodDateFmt.parse(Integer.toString(cal.get(Calendar.YEAR)) + strDate);
                } catch (Exception e) {
                    throw new DefaultApplicationException(e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public Date[] getSpodDates(Date operday) {
        try {
            return executeInNonTransaction(connection -> {
                try {
                    Date lastSpod = getLastSpodDate(operday);
                    Calendar cal446p = Calendar.getInstance();
                    cal446p.setTime(getCfg(CFG_NAME_446P, connection).getDate(PROP_NAME_446P));
                    Calendar calSpod = Calendar.getInstance();
                    calSpod.setTime(lastSpod);
                    calSpod.set(Calendar.YEAR, cal446p.get(Calendar.YEAR));

                    return new Date[] {calSpod.getTime(), lastSpod};
                } catch (Exception e) {
                    throw new DefaultApplicationException(e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public DataRecord getAccountTypeParams(String accType) {
        try {
            DataRecord res = selectFirst("select coalesce(PL_ACT, 'N') as PL_ACT, coalesce(FL_CTRL, 'N') as FL_CTRL" +
                    " from GL_ACTNAME n where n.ACCTYPE = ?", accType);
            if (null == res)
                throw new ValidationError(ACCOUNTING_TYPE_NOT_FOUND, accType);
            return res;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /*
    * -	счет Midas: ACID = Код клиента + ‘RUR’ + ACOD + SQ + «Отделение»
-	«Тип собственности» (CTYPE),
-	«Символ доходов/расходов» (PLCODE)
-	RLNTYPE = ‘2’
-	«Б/счет 2-го порядка» (из формы)
    */
    public DataRecord getAccountForPl(String acid, Short ctype, String plcode, String acc2) {
        try {
            DataRecord res = selectFirst("select a.BSAACID, a.ACID, a.CTYPE, a.PLCODE, a.ACC2" +
                    " from ACCRLN a exception join GL_ACC g" +
                    " on g.ACID = a.ACID and g.CBCUSTTYPE = a.CTYPE and g.PLCODE = a.PLCODE and g.ACC2 = a.ACC2" +
                    " where a.ACID = ? and a.CTYPE = ? and a.PLCODE = ? and a.ACC2 = ? and a.RLNTYPE = ?",
                    acid, ctype, plcode, acc2, '2');
            return res;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }

    }

    /**
     * Проверяет правильность корреспонденции счетов 9999
     * @param account1
     * @param account2
     * @param operSide
     * @return
     */
    public ValidationError checkAccount9999(String account1, String account2, GLOperation.OperSide operSide){
        if (isEmpty(account1) || !account1.startsWith("9999") || isEmpty(account2) || (account2.length() < 20) )
            return null;
        if (!checkAccountRlnExists(account1, " ", "T"))
            return new ValidationError(ACCOUNT_NOT_CORRECT, operSide.getMsgName(), account1);

        String psavName = "";
        char digit = '-';
        String[] items = getPassiveActiveType(account2.substring(0, 5));
        String psav = items[0];
        String type = items[1];
        switch (psav) {
            case "П":
                psavName = "пассивным";
                if("В".equals(type)) {
                    digit = '8';
                }else if("Г".equals(type)) {
                    digit = '6';
                }
                break;
            case "А":
                psavName = "активным";
                if("В".equals(type)) {
                    digit = '9';
                }else if("Г".equals(type)) {
                    digit = '7';
                }
                break;
        }
        if (digit != account1.charAt(4))
            return new ValidationError(ACCOUNT_NOT_CORRESP, account1, psavName, account2);

        return null;
    }

    /**
     * Возвращает парный (актив - пассив) счет для 9999
     * @param accountNotCorresp
     * @return
     * @throws SQLException
     */
    public String getAccount9999Corr(String accountNotCorresp) throws SQLException {
        DataRecord res = selectFirst("select ACC2, CCODE from ACCRLN where BSAACID = ? and ACID = ? and RLNTYPE = ?", accountNotCorresp, " ", "T");
        if (null == res)
            return "";
        String acc2 = res.getString(0);
        char corr;
        switch (acc2.charAt(4)) {
            case '9':
                corr = '8';
                break;
            case '8':
                corr = '9';
                break;
            case '7':
                corr = '6';
                break;
            case '6':
                corr = '7';
                break;
            default:
                corr = ' ';
        };
        String acc2corr = acc2.substring(0, 4) + corr;
        String filial = res.getString(1);
        res = selectFirst("select BSAACID from ACCRLN where ACC2 = ? and CCODE = ? and ACID = ? and RLNTYPE = ? ", acc2corr, filial, " ", "T");
        return null == res ? "" : res.getString(0);
    }

    /**
     * Поиск технического счета для переобработки проводок с ошибками по клиентским счетам
     * @param accountingType acctype
     * @param glccy код валюты
     * @param cbccn код филиала
     * @return счет
     */
    public GLAccount findTechnicalAccount(AccountingType accountingType, String glccy, String cbccn) {
        List<GLAccount> accrecs = findNative(GLAccount.class, "select * from gl_acc a where a.acctype = ? and a.ccy = ? and a.cbccn = ? and a.rlntype = ?"
                , 5, accountingType.getId(), glccy, cbccn, GLAccount.RelationType.E.getValue());
        if (accrecs.isEmpty()) {
            return null;
        }
        else if (accrecs.size() > 1)
        {
            throw new DefaultApplicationException(String.format("Найдено '%s' счетов по acctype='%s', ccy='%s', cbccn='%s'. Счета: %s"
                    , accrecs.size(), accountingType.getId(), glccy, cbccn, accrecs.stream().map(GLAccount::getBsaAcid).collect(Collectors.joining(","))));
        } else {
            return accrecs.get(0);
        }

    }

    public GLAccount findTechnicalAccountTH(AccountingType accountingType, String glccy, String cbccn) {
        List<GLAccount> accrecs = findNative(GLAccount.class, "select * from gl_acc a where a.acctype = ? and a.ccy = ? and a.cbccn = ? and a.rlntype = ?"
                , 5, accountingType.getId(), glccy, cbccn, GLAccount.RelationType.NINE.getValue());
        if (accrecs.isEmpty()) {
            return null;
        }
        else if (accrecs.size() > 1)
        {
            throw new DefaultApplicationException(String.format("Найдено '%s' счетов по acctype='%s', ccy='%s', cbccn='%s'. Счета: %s"
                    , accrecs.size(), accountingType.getId(), glccy, cbccn, accrecs.stream().map(GLAccount::getBsaAcid).collect(Collectors.joining(","))));
        } else {
            return accrecs.get(0);
        }

    }

}
