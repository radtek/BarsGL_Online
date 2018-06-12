package ru.rbt.barsgl.ejb.repository;

import ru.rb.cfg.SystemConfiguration;
import ru.rb.cfg.db.DbConfiguration;
import ru.rb.cfg.exception.ConfigurationException;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.GLAccParam;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.acc.GLAccountRequest;
import ru.rbt.barsgl.ejb.entity.dict.AccountingType;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejbcore.validation.ResultCode;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
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

    private static final String CFG_NAME_CARD = "CARD";
    private static final String PROP_NAME_CARD = "START_PH_CARD_DATE";
    private static final String CFG_NAME_446P = "446P";
    private static final String PROP_NAME_446P = "START_446P_DATE";
    private static final String CFG_NAME_SPOD = "SPOD";
    private static final String PROP_NAME_SPOD = "LAST_SPOD_DAY";
    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Проверяет наличие в БД счета Майдас
     *
     * @param acid - счет Майдас
     * @return - true - есть
     */
    public boolean checkMidasAccountExists(String acid, Date dateCurrent) {
        try {
            DataRecord res = selectFirst("select 1 from GL_ACC A where A.ACID = ?", acid);
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

    /**
     * Проверяет наличие в БД счета ЦБ
     *
     * @param bsaAcid - счет ЦБ
     * @return - true - есть
     */
    public boolean checkAccountExists(String bsaAcid) {
        try {
            DataRecord res = selectFirst("select 1 from GL_ACC a where a.BSAACID = ?", bsaAcid);
            return null != res;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

//    public boolean checkBsaAccountOpen(String bsaAcid, Date dateCurrent) {
//        try {
//            DataRecord res = selectFirst("select count(1) from BSAACC B where B.ID = ?" +
//                    " and B.BSAACO <= ? and (B.BSAACC is null or B.BSAACC >= ?)", bsaAcid, dateCurrent, dateCurrent);
//            return res.getInteger(0) > 0;
//        } catch (SQLException e) {
//            throw new DefaultApplicationException(e.getMessage(), e);
//        }
//    }

    public ResultCode checkBsaAccountAnal(String bsaAcid, Date vDate, Date operDate, Date lastOperDate) {
        try {
            DataRecord res = selectFirst("select case when DTO > ? then " + ResultCode.ACCOUNT_IS_OPEN_LATER.getValue() +
                            " when not DTC is null and  DTC < ? then "+ ResultCode.ACCOUNT_IS_CLOSED_BEFOR.getValue() +
                            " else "+ ResultCode.SUCCESS.getValue() +" end " +
                            "from GL_ACC where BSAACID = ?"
                    , vDate, !vDate.before(lastOperDate)? vDate: operDate, bsaAcid);

            return (null == res) ? ResultCode.ACCOUNT_NOT_FOUND : ResultCode.valueOf(res.getInteger(0));
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public String findForSequenceGL(AccountKeys keys) throws Exception {
        List<DataRecord> results = select("select BSAACID from GL_ACC where CBCCN = ? and CCY = ? and ACC2 = ? and RLNTYPE = 'T'" // and ACOD = ' '" - для GL_ACC это поле заполнено
                , keys.getCompanyCode(), keys.getCurrency(), keys.getAccount2());
        if (1 == results.size()) {
            return results.get(0).getString("BSAACID");
        } else if (1 < results.size()) {
            throw new ValidationError(ErrorCode.TOO_MANY_ACCRLN_ENTRIES, keys.getCompanyCode(), keys.getCurrencyDigital(), keys.getAccount2());
        } else {
            throw new ValidationError(ErrorCode.NOT_FOUND_ACCRLN_GL, keys.getCompanyCode(), keys.getCurrencyDigital(), keys.getAccount2());
        }
    }

    public GLAccount getDealSubDealGlAcc(String bsaAcid){
        try{
            return selectFirst(GLAccount.class, "select a from GlAccDeals d, GLAccount a where a.bsaAcid=?1 and d.acc2=substring(a.bsaAcid,1, 5) and d.flag_off='N'", new Object[]{bsaAcid});
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

//    public DataRecord getDealSubDealGlAcc(String bsaAcid){
//        try{
//           return selectFirst("select dealid, subdealid from GL_ACCDEALS d, gl_acc a where a.bsaacid=? and d.acc2=substr(a.bsaacid,1, 5) and d.flag_off='N'", bsaAcid);
//        } catch (SQLException e) {
//            throw new DefaultApplicationException(e.getMessage(), e);
//        }
//    }

    public ResultCode checkBsaAccountGlAcc(String bsaAcid)
    {
        try {
            DataRecord res = selectFirst("select 0 from GL_ACC where bsaacid = ?", bsaAcid);

            return (null == res) ? ResultCode.ACCOUNT_NOT_FOUND : ResultCode.valueOf(res.getInteger(0));
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public int checkAccountDate(String bsaAcid, Date dateCurrent) {
        try {
            DataRecord res = selectFirst("select" +
                    " case when DTO > ? or DTC < ? then 0 else 1 end" +
                    " from GL_ACC a where a.BSAACID = ?"
                    , dateCurrent, dateCurrent, bsaAcid);
            return (null == res) ? -1 : res.getInteger(0);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public boolean checkAccountRlnExists(String bsaAcid, String acid, String rlntype) {
        try {
            DataRecord res = selectFirst("select 1 from GL_ACC where BSAACID = ? and ACID = ? and RLNTYPE = ?", bsaAcid, acid, rlntype);
            return null != res;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public boolean checkAccountRlnNotPseudo(String bsaAcid) {
        try {
            DataRecord res = selectFirst("select 1 from GL_ACC where BSAACID = ? and RLNTYPE in ('2', '5') and CCY='RUR'", bsaAcid);
            return null != res;
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

    public String getCbccy(String ccy)
    {
        try
        {
            String sql = "select CBCCY from CURRENCY where GLCCY = ?";
            DataRecord res = selectFirst(sql, ccy);
            return Optional.ofNullable(res).orElseThrow(()->new SQLException("not found " + sql + " vs " + ccy)).getString(0);
        }
        catch (SQLException e){
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public void getBicControlCode(String branch, StringBuilder code, StringBuilder bic) throws SQLException {
        String sql = "select ccbbr code, bxbicc bic from imbcbcmp p "+
                     "join imbcbbrp r on p.ccbbr=r.bcbbr and r.br_head='Y' "+
                     "left join sdcustpd c on c.bbcust=r.a8bicn where p.ccpcd=?";
        Optional.ofNullable(selectFirst( sql, branch))
                .map(res->{
                    code.append(res.getString(0));
                    if (res.getString(1).length() >= 9){
                        bic.append(res.getString(1).substring(6, 9));
                    }else {
                        bic.append(res.getString(1));
                    }
                   return 1;
                })
                .orElseThrow(()->new SQLException(" not found " + sql+" vs " + branch));
    }

//    public void getBicControlCode(String branch, StringBuilder code, StringBuilder bic) {
//        try {
//            String sql = "select ccbbr code, bxbicc bic from imbcbcmp p, imbcbbrp r left join sdcustpd c on c.bbcust=r.a8bicn " +
//                         "where '0'||r.a8brcd=p.ccbbr and p.ccpcd=? ";
//            DataRecord res = selectFirst( sql, branch);
//            if (res != null){
//                code.append(res.getString(0));
//                if (res.getString(1).length() >= 9){
//                    bic.append(res.getString(1).substring(6, 9));
//                }else {
//                    bic.append(res.getString(1));
//                }
//            }else{
//                throw new SQLException("not found " + sql + " vs " + branch);
//            }
//        }catch (SQLException e){
//                throw new DefaultApplicationException(e.getMessage(), e);
//        }
//    }



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

    public String[] getIMBCBBRP_bic_branch(String bcbbr){
        try{
            String sql = "select a8bicn, a8brcd from imbcbbrp where bcbbr=? and br_head='Y'";
            return Optional.ofNullable(selectFirst(sql, bcbbr))
                            .map(res-> {
                                return new String[]{res.getString(0), res.getString(1)};
                            })
                            .orElseThrow(()->new SQLException(" not found "+sql+" vs "+bcbbr));
        }catch (SQLException e){
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public boolean existIbcb(String cb1, String cb2, String cbccy){
        try{
            String sql = "select 1 from ibcb where ibacou=? and ibacin=? and ibccy=?";
            return null != selectFirst(sql, cb1, cb2, cbccy);
        }catch (SQLException e){
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

//    public String getAccountCustNo(String bsaAcid) {
//        try {
//            String sql = "select BSAACNNUM from BSAACC where ID = ?";
//            DataRecord res = selectFirst(sql, bsaAcid);
//            return null != res ? res.getString(0) : "";
//        } catch (SQLException e) {
//            throw new DefaultApplicationException(e.getMessage(), e);
//        }
//
//    }
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

    public Short getCustomerType(String cnum, String acc2) throws SQLException {
        DataRecord dataRecord = selectFirst("SELECT BXCTYP FROM SDCUSTPD WHERE BBCUST = ?", cnum);
        if (dataRecord != null && dataRecord.getShort(0) != null) {
            return dataRecord.getShort(0);
        }

        dataRecord = selectFirst("SELECT CTYPE FROM GL_SVTYPNO WHERE ACC2 = ?", acc2);
        if (dataRecord != null && dataRecord.getShort(0) != null) {
            return dataRecord.getShort(0);
        }
        return 0;
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
    public String getMidasBranchByFlex(String branchFlex) {
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
        if (0 == accType)
            return null;          // не наши счета
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
            return Optional.ofNullable(selectFirst(
                            "select ID " +
                            "  from GL_ACC " +
                            " where CCY = ? and CUSTNO = ? and ACCTYPE = ? and PLCODE = ? and CBCCN = ?" +
                            "   and DTO <= ? and (DTC is null or DTC > ?) and RLNTYPE = ?" +
                            "   and coalesce(CBCUSTTYPE, 0) = ? and coalesce(TERM, 0) = ? "
                    , currency, customerNumber, accountType, plcode, cbccn, dateCurrent, dateCurrent
                    , GLAccount.RelationType.FIVE.getValue(), ifEmpty(cbCustType, "0"), ifEmpty(term, "0")))
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
                            " and (DTC is null or DTC > ?)" +   //and DTO <= ?   убрали тк может быть сразу 2 открытых счета
                            " and ACC2 = ? and coalesce(CBCUSTTYPE, 0) = ? and coalesce(TERM, 0) = ? "
                    , currency, customerNumber, accountType, plcode, cbccn, dateCurrent
                    , acc2, ifEmpty(cbCustType, "0"), ifEmpty(term, "0")))
                    .map((record) -> findById(GLAccount.class, record.getLong("ID"))).orElse(null);
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
    public DataRecord getAccountForPl(String acid, Short ctype, String plcode, String acc2, Date dateOpen) {
        try {
            DataRecord res = selectFirst("select BSAACID, ACID, CBCUSTTYPE, PLCODE, ACC2, ACCTYPE from GL_ACC" +
                            " where ACID = ? and CBCUSTTYPE = ? and PLCODE = ? and ACC2 = ?" +
                            "   and RLNTYPE = '2' and (DTC is null or DTC > ?)",    // с датой закрытия > даты открытия нового счета
                    acid, ctype, plcode, acc2, dateOpen);
            return res;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
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

    public boolean isExistsGLAccount(String bsaAcid, Date toDate) {
        try {
            DataRecord data = selectFirst("select ID from GL_ACC where BSAACID = ? and (DTC is null or DTC >= ?)", bsaAcid, toDate);
            return null != data;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }
    /**
     * Находит счет GLпо номеру счета ЦБ
     *
     * @param bsaAcid
     * @return
     */
    public GLAccount findGLAccount(String bsaAcid, String acid) {
        try {
            DataRecord data = selectFirst("select ID from GL_ACC where BSAACID = ? and ACID = ?", bsaAcid, acid);
            return (null == data) ? null : findById(GLAccount.class, data.getLong(0));
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public GLAccount findGLAccountByDeal(String bsaAcid, String dealid) {
        try {
            DataRecord data = selectFirst("select ID from GL_ACC where BSAACID = ? and DEALID = ?", bsaAcid, dealid);
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
            DataRecord data = selectFirst("select PKG_CHK_ACC.GET_BALANCE_TODATE(?, ?, ?) from dual"
                    , bsaAcid, acid, datto);
            return ( null == data || null == data.getBigDecimal(0) ) ? BigDecimal.ZERO : data.getBigDecimal(0);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public boolean isAccountBalanceZero(String bsaAcid, String acid, Date datto) {
        BigDecimal bal = getAccountBalance(bsaAcid, acid, datto);
        return BigDecimal.ZERO.equals(bal);
    }

    public Boolean hasAccountBalanceBefore(String bsaAcid, String acid, Date dat) {
        try {
            DataRecord data = selectFirst("select PKG_CHK_ACC.HAS_BALANCE_BEFORE(?, ?, ?) from dual"
                    , bsaAcid, acid, dat);
            return (null != data && data.getInteger(0) == 1);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public Boolean hasAccountBalanceAfter(String bsaAcid, String acid, Date dat) {
        try {
            DataRecord data = selectFirst("select PKG_CHK_ACC.HAS_BALANCE_AFTER(?, ?, ?) from dual"
                    , bsaAcid, acid, dat);
            return (null != data && data.getInteger(0) == 1);
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

        if (cfg.containsConfig(cfgName)) {
            cfg.get(cfgName);     // проверяем, что конфиг уже есть
        } else {
            synchronized (cfg) {
                if (!cfg.containsConfig(cfgName)) {
                    cfg.add(DbConfiguration.create(cfgName, connection));
                }
            }
        }
        return cfg;
    }

    public Date getDateStartCardPH() {
        try {
            return executeInNonTransaction(connection -> {
                SystemConfiguration cfg;
                try {
                    cfg = getCfg(CFG_NAME_CARD, connection);     // проверяем, что конфиг уже есть
                } catch (Exception e) {
                    throw new DefaultApplicationException(e.getMessage(), e);
                }
                try {
                    return cfg.getDate(PROP_NAME_CARD);
                } catch (Exception e) {
                    return null;
                }
            });
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
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
                    return sdf.parse(Integer.toString(cal.get(Calendar.YEAR)) + strDate);
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
        DataRecord res = selectFirst("select ACC2, CBCCN from GL_ACC where BSAACID = ? and ACID = ? and RLNTYPE = ?", accountNotCorresp, " ", "T");
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
        res = selectFirst("select BSAACID from GL_ACC where ACC2 = ? and CBCCN = ? and ACID = ? and RLNTYPE = ? ", acc2corr, filial, " ", "T");
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

    public GLAccount findTechnicalAccountTH(AccountingType accountingType, String glccy, String cbccn, Date currentDate) {
        List<GLAccount> accrecs = findNative(GLAccount.class, "select * from gl_acc a where a.acctype = ? and a.ccy = ? and a.cbccn = ? and a.rlntype = ? and (DTC is null or DTC > ?) order by a.dto desc"
                , 1, accountingType.getId(), glccy, cbccn, GLAccount.RelationType.NINE.getValue(),currentDate);
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

    public GLAccount reopenAccountTH(GLAccount account)
    {
        if (account!=null) {
            if (account.getDateClose()!=null) {
                account.setDateClose(null);
                account = save(account);
            }
        }
        return account;
    }

    public boolean checkTechAccountExists(Long glaccid, String bsaAcid, Date date) {
        try {
            DataRecord res = selectFirst("select count(1) from GL_ACC where BSAACID = ?" +
                    " and coalesce(DTC, TO_DATE('2029-01-01')) = ? and ID <> ?", bsaAcid,  date == null ? new Date(129, 0, 1) : date,glaccid);
            return res.getInteger(0) > 0;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public boolean checkTechAccountExistsInterval(Long glaccid, String bsaAcid, Date dateOpen, Date dateClose)
    {
        try {
            DataRecord res = selectFirst("select count(1) from GL_ACC where BSAACID = ?" +
                    " and DTO <= ? and coalesce(DTC, TO_DATE('2029-01-01')) >= ? and ID <> ?",
                        bsaAcid, dateClose == null ? new Date(129, 0, 1) : dateClose, dateOpen, glaccid);

            //" and (? between DTO and coalesce(DTC, TO_Date('2029-01-01')) or ? between DTO and coalesce(DTC, TO_Date('2029-01-01')) or ? <= DTO) and ID <> ?",

            boolean result = res.getInteger(0) > 0;
            return result;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public void updGlAccOpenDate(String bsaacid, Date newDate) throws Exception{
        executeNativeUpdate("update gl_acc set dto=? where bsaacid=?", newDate, bsaacid);
    }

    public void updGlAccCloseDate(String bsaacid, Date dateClose) {
        executeNativeUpdate("UPDATE GL_ACC SET DTC=? WHERE BSAACID=?", dateClose, bsaacid);
    }

    public boolean validateBranch(String branch) throws SQLException {
        DataRecord dataRecord = selectFirst("SELECT * FROM IMBCBBRP WHERE A8BRCD = (SELECT MIDAS_BRANCH FROM DH_BR_MAP WHERE FCC_BRANCH=?)", branch);
        return dataRecord != null;
    }

    // ---------- перенесено из AccRlnRepository -----------

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
            int customerType = StringUtils.isEmpty(keys.getCustomerType()) ? 0 : Integer.parseInt(keys.getCustomerType());

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
