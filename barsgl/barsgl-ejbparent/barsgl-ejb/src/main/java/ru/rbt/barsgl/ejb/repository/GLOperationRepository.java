package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.BalanceChapter;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.integr.pst.GLOperationProcessor;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.shared.Assert;
import ru.rbt.barsgl.shared.enums.OperState;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static ru.rbt.ejbcore.util.StringUtils.*;


/**
 * Created by ER18837 on 24.02.15.
 */
@Stateless
@LocalBean
public class GLOperationRepository extends AbstractBaseEntityRepository<GLOperation, Long> {

    @Inject
    private DateUtils dateUtils;

    @EJB
    private PropertiesRepository propertiesRepository;

    @EJB
    private OperdayController operdayController;

    @EJB
    private GLAccountRepository glAccountRepository;

    /**
     * Определяет код компании (числовой код филиала) для счета
     *
     * @param bsaAcid - счет ЦБ
     * @return - код филиала (4 цифры)
     * @throws SQLException
     */
    public String getCompanyCode(String bsaAcid) {
        try {
            String sql = "select BRCA from BSAACC B where B.ID = ? with ur";
            DataRecord res = selectFirst(sql, bsaAcid);
            return (null != res) ? res.getString("BRCA") : "";
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public String getFilialCBCCNbyCBCC(String cbcc)
    {
        try {
            String sql = "select * from IMBCBCMP where CCPCD = ?";
            DataRecord res = selectFirst(sql, cbcc);
            return (null != res) ? res.getString("CCBBR") : "";
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Определяет филиал, в котором открыт счет
     *
     * @param bsaAcid - номер счета ЦБ
     * @return - филиал (3 буквы)
     * @throws SQLException
     */
    public String getFilialByAccount(String bsaAcid) {
        try {
            String sql = "select I.CCPCD as CBCC from BSAACC B join IMBCBCMP I on I.CCBBR = B.BRCA where B.ID = ?";
            DataRecord res = selectFirst(sql, bsaAcid);
            return (null != res) ? res.getString("CBCC") : "";
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Определяет филиал, которому принадлежит бранч
     *
     * @param branch - бранч
     * @return - филиал (3 буквы)
     * @throws SQLException
     */
    public String getFilialByBranch(String branch) {
        if (isEmpty(branch))
            return "";
        try {
            String sql = "select A8CMCD CBCC from IMBCBBRP where A8BRCD = ?";
            DataRecord res = selectFirst(sql, branch);
            return (null != res) ? res.getString("CBCC") : "";
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Определяет головного клиента для бранча
     *
     * @param branch
     * @return
     */
    public DataRecord getBranchParameters(String branch) {
        if (isEmpty(branch))
            return null;
        try {
            String sql = "select A8BRNM NAME, A8CMCD CBCC, BCBBR CBCCN, A8BICN CNUM from IMBCBBRP where A8BRCD = ?";
            DataRecord res = selectFirst(sql, branch);
            return res;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Определяет филиал по дебету / кредиту для операции
     *
     * @param bsaAcid       - номер счета ЦБ
     * @param accountParams - параметры открываемого счета
     * @return - филиал (3 буквы)
     * @throws SQLException
     */
    public String getFilial(String bsaAcid, AccountKeys accountParams) {
        if (!isEmpty(bsaAcid)) {
            if ((null!=accountParams) && (accountParams.getGlSequence().startsWith("TH")))
            {
                GLAccount account = glAccountRepository.findGLAccount(bsaAcid);
                return account.getFilial();
            }
            else {
                return getFilialByAccount(bsaAcid);
            }
        } else if (null != accountParams) {
            return getFilialByBranch(accountParams.getBranch());
        } else {
            return "";
        }
    }

    public void setFilials(GLOperation operation) throws SQLException {
        if (isEmpty(operation.getFilialDebit())) {
            if (operation.getAccountKeyDebit()!=null) {
                operation.createAccountParamDebit();
            }
            operation.setFilialDebit(getFilial(operation.getAccountDebit(), operation.getAccountParamDebit()));
        }
        if (isEmpty(operation.getFilialCredit())) {
            if (operation.getAccountKeyCredit()!=null) {
                operation.createAccountParamCredit();
            }
            operation.setFilialCredit(getFilial(operation.getAccountCredit(), operation.getAccountParamCredit()));
        }
    }

    /**
     * Определяет главу баланса для операции
     *
     * @param operation
     */
    public void setBsChapter(GLOperation operation) throws SQLException {
        // глава балансового счета
        String chapterDebit = getBSChapter(operation.getAccountDebit(), operation.getAccountParamDebit());
        String chapterCredit = getBSChapter(operation.getAccountCredit(), operation.getAccountParamCredit());
        // Задаем главу баланса только когда она совпадает по дебету и кредиту
        if (!isEmpty(chapterDebit) && chapterDebit.equals(chapterCredit)) {
            operation.setBsChapter(chapterDebit);
        }
    }


    /**
     * Определяет головной бранч для филиала
     *
     * @param filial - филиал (3 буквы)
     * @return - бранч (3 цифры)
     * @throws SQLException
     */
    public String getHeadBranch(String filial) throws SQLException {
        String sql = "select A8BRCD BRANCH from IMBCBBRP where A8CMCD = ? and BR_HEAD = 'Y'";
        DataRecord res = selectFirst(sql, filial);
        return (null != res) ? res.getString("BRANCH") : "";
    }

    /**
     * Определяет головной бранч для филиала
     *
     * @param ccode - код филиала (4 цифры)
     * @return - бранч (3 цифры)
     * @throws SQLException
     */
    public String getHeadBranchByCCode(String ccode) throws SQLException {
        String sql = "select A8BRCD BRANCH from IMBCBBRP where BCBBR = ? and BR_HEAD = 'Y'";
        DataRecord res = selectFirst(sql, ccode);
        return (null != res) ? res.getString("BRANCH") : "";
    }

    /**
     * branch и custno одним запросом
     * @param ccode
     * @return
     * @throws SQLException
     */
    public DataRecord getHeadDataByCCode(String ccode) throws SQLException {
        String sql = "select A8BRCD BRANCH, IMBCBBRP.A8BICN CUSTNO from IMBCBBRP where BCBBR = ? and BR_HEAD = 'Y'";
        return selectFirst(sql, ccode);
    }

    /**
     * Определяет бранч, в котором открыт счет
     *
     * @param bsaAcid  - номер счета ЦБ
     * @param postDate - дата проводки (нужна для определения счета Midas)
     * @return - бранч (3 цифры)
     * @throws SQLException
     */
    public String getBranch(String bsaAcid, Date postDate) throws SQLException {
        String acid = getAcidByAccRln(bsaAcid, postDate).getString("ACID");
        String branch = (acid.length() == 20) ?
                            acid.substring(17, 20) :
                            getHeadBranch(getFilialByAccount(bsaAcid));
        return branch;
    }

    /**
     * Определяет главу балансового счета
     *
     * @param acc2 - балансовый счет второго порядка
     * @return - глава баланса (пока на латинице)
     */
    public BalanceChapter getBalanceChapterAcc2(String acc2) {
        try {
            DataRecord res = selectFirst("select TYPE from BSS where ACC2 = ?", acc2);
            String chapter = null != res ? res.getString(0) : "";
            return BalanceChapter.parseChapter(chapter);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Определяет главу балансового счета
     *
     * @param acc2 - балансовый счет второго порядка
     * @return - глава баланса (пока на латинице)
     * @throws SQLException
     */
    public String getBSChapterAcc2(String acc2) {
        final BalanceChapter enumChapter = getBalanceChapterAcc2(acc2);
        return null != enumChapter ? enumChapter.name() : "";
    }

    /**
     * Определяет главу балансового счета
     *
     * @param bsaAcid - номер счета ЦБ
     * @return - глава баланса (пока на латинице)
     */
    public BalanceChapter getBalanceChapter(String bsaAcid) {
        return getBalanceChapterAcc2(substr(bsaAcid, 5));
    }

    /**
     * Определяет главу балансового счета
     *
     * @param bsaAcid - номер счета ЦБ
     * @return - глава баланса (пока на латинице)
     * @throws SQLException
     */
    public String getBSChapter(String bsaAcid) {
        return getBSChapterAcc2(substr(bsaAcid, 5));
    }

    /**
     * Определяет главу балансового счета
     *
     * @param bsaAcid     - номер счета ЦБ
     * @param accountKeys - параметры открываемого счета
     * @return - глава баланса (пока на латинице)
     * @throws SQLException
     */
    public String getBSChapter(String bsaAcid, AccountKeys accountKeys) throws SQLException {
        if (!isEmpty(bsaAcid))
            return getBSChapter(bsaAcid);
        else if (null != accountKeys)
                if (null!= accountKeys.getGlSequence() && accountKeys.getGlSequence().startsWith("TH"))
                {
                    return GLOperation.flagTechOper;
                }
                else {
                    return getBSChapterAcc2(accountKeys.getAccount2());
                }
        else return "";
    }

    public void updateOperationParentStatus(Long operationId, OperState state) {
        executeUpdate("update GLOperation o set o.state = ?1 where o.id = ?2 or o.parentOperation.id = ?3", state, operationId, operationId);
    }

    public void updateOperationStatus(GLOperation operation, OperState state) {
        executeUpdate("update GLOperation o set o.state = ?1 where o = ?2", state, operation);
    }

    public void updateOperationStatusSuccess(GLOperation operation, OperState state) {
        // TODO а зачем обнулять сообщение об ошибке? пусть остается, если было
        executeUpdate("update GLOperation o set o.state = ?1, o.errorMessage = ?2 where o = ?3", state, null, operation);
    }

    public void updateOperationStatusError(GLOperation operation, OperState state, String message) {
        executeUpdate("update GLOperation o set o.state = ?1, o.errorMessage = ?2 where o = ?3", state, substr(message, 4000), operation);
    }

    public void updateOperationFanStatusSuccess(String parentRef, YesNo storno, OperState state) {
        executeUpdate("update GLOperation o set o.state = ?1 where o.parentReference = ?2 and o.storno = ?3",
            state, parentRef, storno);
    }

    public void updateOperationFanStatusError(String parentRef, YesNo storno, OperState state, String message) {
        executeUpdate("update GLOperation o set o.state = ?1, o.errorMessage = ?2 where o.parentReference = ?3 and o.storno = ?4 and o.errorMessage is null",
            state, message, parentRef, storno);
    }

    public List<GLPosting> getPostings(GLOperation operation) {
        return select(GLPosting.class, "from GLPosting p where p.operation = ?1 order by p.id",
            new Object[]{operation});
    }

    public Long getStornoOperationID(GLOperation operation) throws SQLException {

        String sql = "select GLOID from GL_OPER where EVT_ID = ?" +
                         " and VALUE(DEAL_ID, '') = ? and VALUE(PMT_REF, '') = ?" +                // TODO проверка на null

                         // Исправлено Ициксон Е.А. 23.03.2016 task #52
                         //" and VDATE = ?" +
                         " and VALUE(SUBDEALID,'') = ? AND SRC_PST = ?" +

                         " and AC_DR = ? and AMT_DR = ? and AC_CR = ? and AMT_CR = ? and STATE in ('LOAD', 'POST', 'WTAC')";

//        String dealId = (null == operation.getDealId()) ? "" : operation.getDealId();
//        String paymentRefernce = (null == operation.getPaymentRefernce()) ? "" : operation.getPaymentRefernce();
        DataRecord res = selectFirst(sql,
            operation.getStornoReference(),
            ifEmpty(operation.getDealId(), ""),             //operation.getDealId(),
            ifEmpty(operation.getPaymentRefernce(), ""),    //operation.getPaymentRefernce(),

            // Исправлено Ициксон Е.А. 23.03.2016 task #52
            // operation.getValueDate(),
            ifEmpty(operation.getSubdealId(), ""), operation.getSourcePosting(),

            operation.getAccountCredit(), operation.getAmountCredit(),
            operation.getAccountDebit(), operation.getAmountDebit());
        return (null != res) ? res.getLong(0) : null;
    }


    public GLOperation getStornoOperationByGloRef(Long gloId) {
        String sql = "from GLOperation o where o.stornoOperation.id = ?1";
        return selectFirst(GLOperation.class, sql, gloId);
    }

    public List<EtlPosting> getFanPostingByRef(String parentReference, YesNo storno) {
        return select(EtlPosting.class
                , "from EtlPosting p where p.parentReference = ?1 and p.storno = ?2 and p.fan = ?3 and p.valueDate >= ?4 order by p.id"
                , parentReference, storno, YesNo.Y, getFanVdatefrom());
    }

    public List<GLOperation> getFanOperationByRef(String parentReference, YesNo storno) {
        return select(GLOperation.class
                , "from GLOperation o where o.parentReference = ?1 and o.storno = ?2 and o.fan = ?3 and o.valueDate >= ?4 order by o.fbSide, o.id"
                , parentReference, storno, YesNo.Y, getFanVdatefrom());
    }

    public List<String> getFanOperationLoad(Date procdate) {
        try {
            List<DataRecord> res = select("select DISTINCT PAR_RF from GL_OPER where FAN = 'Y' and PROCDATE = ? and STATE = ?",
                    procdate, OperState.LOAD.name());
            return res.stream().map(r -> r.getString(0)).collect(Collectors.toList());
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public List<String> getFanOperationProcessed(Date procdate, List<String> refs) {
        try {
            List<DataRecord> res = select("select DISTINCT PAR_RF from GL_OPER where FAN = 'Y' and PROCDATE = ? and STATE in (?, ?) and PAR_RF in (" +
                    StringUtils.listToString(refs, ", ", "'") + ") ",
                    procdate, OperState.POST.name(), OperState.SOCANC.name());
            return res.stream().map(r -> r.getString(0)).collect(Collectors.toList());
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private Date getFanVdatefrom () {
        try {
            return dateUtils.addDays(operdayController.getOperday().getCurrentDate()
                    , -propertiesRepository.getNumber("fan.vdateFrom").intValue());
        } catch (ExecutionException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public List<GLOperation> getFanOperationById(Long gloId) {
        String sql = "from GLOperation o where o.parentOperation.id = ?1";
        List<GLOperation> res = select(GLOperation.class, sql, gloId);
        return res;
    }

    ;

    /**
     * Обновляет операции веера - заполняет сторону веера и общую сумму (в валюте и в рублях)
     *
     * @param mainOperation
     */
    public DataRecord getFanAmounts(GLOperation mainOperation) throws SQLException {
        Assert.notNull(mainOperation.getParentReference(), "Для веерной операции не задан PAR_RF");
        String sql = "select sum(case FB_SIDE when 'D' then AMT_DR else AMT_CR end), sum(AMTR_POST) " +
                         "from GL_OPER where PAR_RF = ? and STRN = ?";
        DataRecord res = selectFirst(sql, mainOperation.getParentReference(), mainOperation.getStorno().name());
        return res;
    }

    public Integer getOperationsByAccount(String bsaAcid, OperState state) {
        try {
            DataRecord data = selectFirst("select count(1) from GL_OPER where (AC_DR = ? or AC_CR = ?)", bsaAcid, bsaAcid);
            return (null == data) ? 0 : data.getInteger(0);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public Integer getOperationsByAccountBefore(String bsaAcid, Date date) {
        try {
            DataRecord data = selectFirst("select count(1) from GL_OPER where (AC_DR = ? or AC_CR = ?) and POSTDATE < ? and STATE <> ?",
                bsaAcid, bsaAcid, date, OperState.ERCHK.name());
            return (null == data) ? 0 : data.getInteger(0);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public Integer getOperationsByAccountAfter(String bsaAcid, Date date) {
        try {
            DataRecord data = selectFirst("select count(1) from GL_OPER where (AC_DR = ? or AC_CR = ?)" +
                                              " and (POSTDATE > ? and STATE <> ? or STATE = ?)",
                bsaAcid, bsaAcid, date, OperState.ERCHK.name(), OperState.LOAD.name());
            return (null == data) ? 0 : data.getInteger(0);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }


    /**
     * Определяет счет Midas по счету ЦБ
     *
     * @param bsaAcid - счет ЦБ
     * @param valDate - дата валидности
     * @return - счет Midas
     * @throws SQLException
     */
    public DataRecord getAcidByAccRln(String bsaAcid, Date valDate) {
        String rln = bsaAcid.substring(0, 1).equals("7") ? "'2', '5'" : "'0', '4', '1'";   // RLNTYPE
        String sql = "select VALUE(ACID, '') ACID, PLCODE, CTYPE from ACCRLN " +
                         "where ? = BSAACID and ? between DRLNO and DRLNC " +
                         "and RLNTYPE in (" + rln + ") order by RLNTYPE with UR";
        try {
            DataRecord res = selectFirst(sql, bsaAcid, valDate);
            return res;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

}
