package ru.rbt.barsgl.ejb.repository;

import com.google.common.collect.ImmutableList;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.bankjar.Constants;
import ru.rbt.barsgl.bankjar.CreateIBCBrecord;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeysBuilder;
import ru.rbt.barsgl.ejb.entity.acc.GlAccRln;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.dict.SourcesDeals;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountController;
import ru.rbt.barsgl.ejb.repository.dict.SourcesDealsRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.shared.Assert;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.find;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static ru.rb.ucb.util.StringUtils.isEmpty;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.RUB;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperSide.C;
import static ru.rbt.ejbcore.util.StringUtils.substr;

/**
 * Created by Ivan Sevastyanov
 */
public class GLPostingRepository extends AbstractBaseEntityRepository<GLPosting, Long> {

    public static final Logger logger = Logger.getLogger(GLPostingRepository.class.getName());

    private List<String[]> pdNarrParm;

    @Inject
    private BankCurrencyRepository bankCurrencyRepository;

    @Inject
    private CreateIBCBrecord createIBCB;

    @Inject
    private ExcacRlnRepository excacRlnRepository;

    @Inject
    private GLOperationRepository glOperationRepository;

    @EJB
    private GLAccountController glAccountController;

    @EJB
    private PdRepository pdRepository;

    @EJB
    private OperdayController operdayController;

    @EJB
    private BackvalueJournalRepository journalRepository;

    @EJB
    private AuditController auditController;

    @EJB
    private SourcesDealsRepository sourcesDealsRepository;

    /**
     * Создает пару полупроводок (дебет - кредит)
     *
     * @param operation  - GL операция
     * @param postType   - тип проводки (простая, МФО-дебет, МФО-кредит, курсовая разница)
     * @param bsaAcidDr  - счет ЦБ дебета
     * @param ccyDr      - валюта дебета
     * @param amountDr   - сумма дебета
     * @param bsaAcidCr  - счет ЦБ кредита
     * @param ccyCr      - валюта дебета
     * @param amountCr   - сумма дебета
     * @param eqvivalent - рублевый эквивалент (одинаковый для дебета и кредита)
     * @return - проводку, включая список сформированных полупроводок
     * @throws SQLException
     */
    public GLPosting createPosting(GLOperation operation, GLPosting.PostingType postType,
                                   String bsaAcidDr, BankCurrency ccyDr, BigDecimal amountDr,
                                   String bsaAcidCr, BankCurrency ccyCr, BigDecimal amountCr,
                                   BigDecimal eqvivalent)
            throws SQLException {
        long pcId = pdRepository.getNextId();
        GLPosting posting = new GLPosting(pcId, operation, postType);

        String pbr = getPdPbr(operation, postType);
        String evType = getEventType(operation, postType);
        String narrlong = getRusNarrLong(operation, bsaAcidDr, bsaAcidCr);

        Pd pdDr = pdRepository.createPd(operation, pcId, pcId, pbr, evType, narrlong, bsaAcidDr, ccyDr,
                -bankCurrencyRepository.getMinorAmount(ccyDr, amountDr),
                -bankCurrencyRepository.getMinorAmount(bankCurrencyRepository.refreshCurrency(RUB), eqvivalent));
        posting.addPd(pdDr);

        Pd pdCr = pdRepository.createPd(operation, 0, pcId, pbr, evType, narrlong, bsaAcidCr, ccyCr,
                bankCurrencyRepository.getMinorAmount(ccyCr, amountCr),
                bankCurrencyRepository.getMinorAmount(bankCurrencyRepository.refreshCurrency(RUB), eqvivalent));
        posting.addPd(pdCr);
;

        return posting;
    }

    /**
     * Создает и добавляет две пп для простой проводки или одну пп (перо) для веерной
     * @param operation  - GL операция
     * @param posting    - GL проводка
     * @param fpSide     - сторона ПЕРЬЕВ веера или N
     * @param postType   - тип проводки (простая, МФО-дебет, МФО-кредит, курсовая разница)
     * @param bsaAcidDr  - счет ЦБ дебета
     * @param ccyDr      - валюта дебета
     * @param amountDr   - сумма дебета
     * @param bsaAcidCr  - счет ЦБ кредита
     * @param ccyCr      - валюта дебета
     * @param amountCr   - сумма дебета
     * @param eqvivalent - рублевый эквивалент (одинаковый для дебета и кредита)
     * @return - проводку, включая список сформированных полупроводок
     * @return
     */
    public GLPosting addPostingPdWithSkip(GLOperation operation, GLPosting posting,
                                          GLOperation.OperSide fpSide, GLPosting.PostingType postType,
                                          String bsaAcidDr, BankCurrency ccyDr, BigDecimal amountDr,
                                          String bsaAcidCr, BankCurrency ccyCr, BigDecimal amountCr,
                                          BigDecimal eqvivalent) {

        long pcId = posting.getId();
        // если веерная проводка, то генерим новый ID по дебету
        // если обычная проводка, то ID дебета = PCID
        long drId = (fpSide == GLOperation.OperSide.N ? pcId : 0);
        // по кредиту всегда новый ID
        long crId = 0;
        boolean skipDebit = (fpSide == C);  // пропустить дебет, если перья веера по кредиту
        boolean skipCredit = (fpSide == GLOperation.OperSide.D);  // пропустить кредит, если перья веера по дебету

        String pbr = getPdPbr(operation, postType);
        String evType = getEventType(operation, postType);

        try {
            if (!skipDebit) {
                Pd pdDr = pdRepository.createPd(operation, drId, pcId, pbr, evType, bsaAcidDr, ccyDr,
                        -bankCurrencyRepository.getMinorAmount(ccyDr, amountDr),
                        -bankCurrencyRepository.getMinorAmount(ccyDr, eqvivalent));
                posting.addPd(pdDr);
            }

            if (!skipCredit) {
                Pd pdCr = pdRepository.createPd(operation, crId, pcId, pbr, evType, bsaAcidCr, ccyCr,
                        bankCurrencyRepository.getMinorAmount(ccyCr, amountCr),
                        bankCurrencyRepository.getMinorAmount(ccyCr, eqvivalent));
                posting.addPd(pdCr);
            }

            return posting;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }
   
    /**
     * Создает и добавляет одну пп (ручку) для веерной
     * @param operation  - GL операция
     * @param posting    - GL проводка
     * @param fbSide     - сторона РУЧКИ веера или N
     * @param postType   - тип проводки (простая, МФО-дебет, МФО-кредит, курсовая разница)
     * @param bsaAcidDr  - счет ЦБ дебета
     * @param ccyDr      - валюта дебета
     * @param bsaAcidCr  - счет ЦБ кредита
     * @param ccyCr      - валюта дебета
     * @param eqvivalent - рублевый эквивалент (одинаковый для дебета и кредита)
     * @return
     * @throws SQLException
     */
    public GLPosting addPostingPdFan(GLOperation operation, GLPosting posting,
                                     GLOperation.OperSide fbSide, GLPosting.PostingType postType,
                                      String bsaAcidDr, BankCurrency ccyDr,
                                      String bsaAcidCr, BankCurrency ccyCr,
                                     BigDecimal amount, BigDecimal eqvivalent) throws SQLException {

        long pcId = posting.getId();

        String pbr = getPdPbr(operation, postType);
        String evType = getEventType(operation, postType);

        if (fbSide == GLOperation.OperSide.D) {                  // РУЧКА веера по дебету
            Pd pdDr = pdRepository.createPd(operation, pcId, pcId, pbr, evType, bsaAcidDr, ccyDr,
                    -bankCurrencyRepository.getMinorAmount(ccyDr, amount),
                    -bankCurrencyRepository.getMinorAmount(ccyDr, eqvivalent));
            posting.addPd(pdDr);
        }
        else if (fbSide == C) {             // РУЧКА веера по кредиту
            Pd pdCr = pdRepository.createPd(operation, pcId, pcId, pbr, evType, bsaAcidCr, ccyCr,
                    bankCurrencyRepository.getMinorAmount(ccyCr, amount),
                    bankCurrencyRepository.getMinorAmount(ccyCr, eqvivalent));
            posting.addPd(pdCr);
        }

        return posting;
    }

    /**
     * @param operation
     * @param bsaAcidDr
     * @param ccyDr
     * @param bsaAcidCr
     * @param ccyCr
     * @return
     * @throws SQLException
     */
    public GLPosting createExchPosting(GLOperation operation, GLPosting.PostingType postType,
                                       String bsaAcidDr, BankCurrency ccyDr,
                                       String bsaAcidCr, BankCurrency ccyCr) {
        // счет, с которого отводится курсовая разница
        String bsaAcidDiff = RUB.equals(ccyDr) ? bsaAcidCr : bsaAcidDr;
        // счет, на который отводится курсовая разница
        String bsaAcidExch = operation.getAccountExchange();
        // сумма курсовой разницы
        BigDecimal exchDeffirence = operation.getExchangeDifference();
        BigDecimal exchDeffirenceAbs = exchDeffirence.abs();
        BankCurrency ccyRUR = bankCurrencyRepository.refreshCurrency(RUB); // ??
        GLPosting postingExch;
        // проводка по курсовой разнице
        try {
            if (exchDeffirence.signum() > 0) {      // положительная
                postingExch = createPosting(operation, postType,
                        bsaAcidDiff, ccyRUR, BigDecimal.ZERO,
                        bsaAcidExch, ccyRUR, exchDeffirenceAbs,
                        exchDeffirenceAbs);
            } else {                                // отрицательная
                postingExch = createPosting(operation, postType,
                        bsaAcidExch, ccyRUR, exchDeffirenceAbs,
                        bsaAcidDiff, ccyRUR, BigDecimal.ZERO,
                        exchDeffirenceAbs);
            }
            return postingExch;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * определяет, является перевод клиентским
     *
     * @param bsaAcidDebit  - счет дебета
     * @param bsaAcidCredit - счет кредита
     * @return - true - перевод клиентский
     * @throws SQLException
     */
//    public boolean isClientPosting(String bsaAcidDebit, String bsaAcidCredit) throws SQLException {
//        String acc2Debit = bsaAcidDebit.substring(0, 5);
//        String acc2Credit = bsaAcidCredit.substring(0, 5);
//        String sql = "select ACC2 from IBCBACC2 where ACC2 in (?, ?)";
//        DataRecord res = selectFirst(sql, acc2Debit, acc2Credit);
//        return (null != res);
//    }

    /**
     * определяет, является перевод клиентским при межфилиале
     *
     * @param bsaAcidDebit  - счет дебета
     * @param bsaAcidCredit - счет кредита
     * @return - true - перевод клиентский
     * @throws SQLException
     */
    public boolean isMfoClientPosting(String bsaAcidDebit, String bsaAcidCredit) throws SQLException {
        String acc2Debit = bsaAcidDebit.substring(0, 5);
        String acc2Credit = bsaAcidCredit.substring(0, 5);
//        Bug_CCYExch+Interbanch v0.01.doc
        if  (selectFirst("select ACC2 from IBCB305ACC2 where ACC2 in (?, ?)", acc2Debit, acc2Credit) != null)
          return false;
        return selectFirst("select ACC2 from IBCBACC2 where ACC2 in (?, ?)", acc2Debit, acc2Credit) != null;
    }

    /**
     * Определяет счета межфилиальных проводок
     *
     * @param currency     - валюта проводки
     * @param filialDebit  - филиал дебета
     * @param filialCredit - филиал кредита
     * @return - поля AC_MFOLIAB - счет по дебету, AC_MFOASST - счет по кредиту
     * @throws SQLException
     */
    public String[] getMfoAccounts(BankCurrency currency, String filialDebit, String filialCredit, boolean isClients) throws SQLException
    {
        String mfoDebit, mfoCredit;
        String ccy = currency.getCurrencyCode();
        String mfoFields = isClients ?                  // перевод клиентский ?
                "IBACOU as AC_MFOLIAB, IBACIN as AC_MFOASST" :  // да
                "IBA305 as AC_MFOLIAB, IBA306 as AC_MFOASST";  // нет
        String sql = "select " + mfoFields + " from IBCB where IBCCY = ? and IBBRNM = ? and IBCBRN = ?";
        DataRecord res = selectFirst(sql, ccy, filialDebit, filialCredit);
        if (null == res) {
            createIBCB.calculateIBCBaccount(filialDebit, filialCredit, ccy);
            res = selectFirst(sql, ccy, filialDebit, filialCredit);
            Assert.notNull(res, format("Не удалось создать счет МФО для филиалов '%s' - '%s' валюта '%s'",
                    filialDebit, filialCredit, ccy));
        }
        mfoDebit = res.getString(0);
        res = selectFirst(sql, ccy, filialCredit, filialDebit);
        if (null == res) {
            createIBCB.calculateIBCBaccount(filialCredit, filialDebit, ccy);
            res = selectFirst(sql, ccy, filialCredit, filialDebit);
            Assert.notNull(res, format("Не удалось создать счет МФО для филиалов '%s' - '%s' валюта '%s'",
                    filialCredit, filialDebit, ccy));
        }
        mfoCredit = res.getString(1);
        return new String[]{mfoDebit, mfoCredit};
    }

//    /**
//     * Определяет счет для отведения курсовой разницы
//     *
//     * @param branch  - бранч
//     * @param chapter - глава баланса
//     * @param sign    - знак курсовой разницы
//     * @return
//     * @throws SQLException
//     */
//    public String getExchangeAccount(String branch, String chapter, int sign, Date postDate) throws SQLException {
//        // определить счет курсовой разницы ТОЛЬКО для главы А
//        if (chapter.equals(BalanceChapter.A.name())) {
//            String col = (sign > 0) ? "ACEXCHPA" : "ACEXCHLA";
//            String sql = "select " + col + " from GL_ACCEXCH " +
//                    "where BRANCH = ?" +
//                    "and ? between DATEDEF and DATEEND";
//            DataRecord res = selectFirst(sql, branch, postDate);
//            String acc = (null != res) ? res.getString(0) : "";
//            return acc;
//        } else {
//            return "";
//        }
//    }

    // TODO getExchangeAccount2
    /**
     * Определяет счет для отведения курсовой разницы
     *
     * @param operation  - запись в GL_OPER
     * @return счет цб
     * @throws SQLException
     */
    public String getExchangeAccount(GLOperation operation) throws Exception{

        String bsaAcid = operation.getCcyMfoSide() == C ? operation.getAccountDebit() : operation.getAccountCredit();
        BankCurrency bankCurrency = BankCurrency.RUB.equals(operation.getCurrencyDebit()) ? operation.getCurrencyCredit() : operation.getCurrencyDebit();

        // поиск существующего счета курсовой разницы
        String ccode = glOperationRepository.getCompanyCode(bsaAcid);
        String psav = operation.getExchangeDifference().signum() > 0 ? Constants.PASIV : Constants.ACTIV;
        String optype = isCach(operation);
        GlAccRln accRln = excacRlnRepository.findAccountExchange(ccode, bankCurrency.getCurrencyCode(), psav, optype);

        if (accRln == null) {
        // создание счета курсовой разницы
            AccountKeys keys = AccountKeysBuilder.create().
                    withCompanyCode(ccode).
                    withPassiveActive(psav).
                    withBranch(glOperationRepository.getHeadBranchByCCode(ccode)).build();
            accRln = glAccountController.createAccountsExDiff(operation, GLOperation.OperSide.N, keys, operation.getPostDate(), bankCurrency, optype);
        }

        return accRln.getId().getBsaAcid();
    }

    /**
     * Определяет счет для отведения курсовой разницы при межфилиальных расчетах
     * Bug_CCYExch+Interbanch v0.01.doc
     *
     * @param operation  - запись в GL_OPER
     * @return счет цб
     * @throws SQLException
     */
//    public String getMfoExchangeAccount(GLOperation operation) throws Exception{
//        String bsaAcid = null;
//        BankCurrency bankCurrency = null;
//        //сторона списания курсовой разницы - CR
//        if (operation.getCurrencyDebit().equals(BankCurrency.RUB)) {
//            bsaAcid = operation.getAccountCredit();
//            bankCurrency = operation.getCurrencyCredit();
//        }
//        //сторона списания курсовой разницы - DR
//        else {
//            bsaAcid = operation.getAccountDebit();
//            bankCurrency = operation.getCurrencyDebit();
//        }
//        // поиск существующего счета курсовой разницы
//        String ccode = glOperationRepository.getCompanyCode(bsaAcid);
//        String psav = operation.getExchangeDifference().signum() > 0 ? Constants.PASIV : Constants.ACTIV;
//        String optype = isCach(operation);
//        String[] acids = excacRlnRepository.findAccountExchange(ccode, bankCurrency.getCurrencyCode(), psav, optype);
//
//        if (acids == null) {
//            // создание счета курсовой разницы
//            AccountKeys keys = AccountKeysBuilder.create().
//                    withCompanyCode(ccode).
//                    withPassiveActive(psav).
//                    withBranch(glOperationRepository.getHeadBranchByCCode(ccode)).build();
//            acids = glAccountController.createAccountsExDiff(operation, GLOperation.OperSide.N, keys, operation.getPostDate(), bankCurrency, optype);
//        }
//
//        return (acids != null) ? acids[1] : "";
//    }


    private String isCach(GLOperation operation) {
/*
* Определяем
ACC2.DR=AcctDR[1,5] (первые5 знаков)
ACC2.CR=AcctCR[1,5] (первые5 знаков)
Если ACC2.DR или ACC2.CR=’20202’ или ‘20208’, то I.OPTYPE=’Y’, иначе = ‘N’
*/
        String acc2dr = operation.getAccountDebit().substring(0, 5);
        String acc2cr = operation.getAccountCredit().substring(0, 5);
        return ("20202".equals(acc2cr) || "20202".equals(acc2dr) || "20208".equals(acc2cr) || "20208".equals(acc2dr)) ? "Y" : "N";
    }

    /**
     * Формирует поле PBR для таблицы PD
     *
     * @param operation
     * @return
     */
    public String getPdPbr(GLOperation operation, GLPosting.PostingType postType) {
        String pbr = "@@GL";
        if (postType.equals(GLPosting.PostingType.ExchDiff)) {
            pbr = pbr + "RCA";
        } else {
            String code = getSourceOfPosting(operation);
            pbr = pbr + code;
        }
        return substr(pbr, 7);
    }

    /**
     * "Мягко" ругаемся на источник, если не найден
     * @param operation
     */
    private String getSourceOfPosting(final GLOperation operation) {
        String src = operation.getSourcePosting();
        List<SourcesDeals> sourcesDealses = sourcesDealsRepository.getAllObjectsCached();
        Optional<SourcesDeals> optional
                = sourcesDealses.stream().filter(s -> s.getId().equalsIgnoreCase(src)).findFirst();
        if (optional.isPresent()) {
            return optional.get().getShortName();
        } else {
            auditController.warning(AuditRecord.LogCode.Posting
                    , format("Не найдено ни одного источника '%s' в справочнике GL_SRCPST : [%s]"
                            , src, sourcesDealses.stream().map(source -> source.getId()).collect(joining("','", "'", "'"))), operation, "");
            return "-" + src;
        }
    }

    public String getEventType(GLOperation operation, GLPosting.PostingType postType) {
        if (GLPosting.PostingType.ExchDiff.equals(postType)) {
            return null;        // для курсовой разницы не нужен тип события
        } else {
            return operation.getEventType();
        }
    }

    /* Формирует поле PDEXT2.RNARLNG для проводок по курсовой разнице
    * @return
            */
    public String getRusNarrLong(GLOperation operation, String bsaAcidDr, String bsaAcidCr) {
        String narr = getRusNarrLong(bsaAcidDr);
        if (isEmpty(narr))
            narr = getRusNarrLong(bsaAcidCr);
        if (isEmpty(narr))
            narr = operation.getRusNarrativeLong();
        return narr;
    }

    /**
     * Формирует поле PDEXT2.RNARLNG для счета по курсовой разнице
     * @return
     */
    public String getRusNarrLong(String bsaAcid) {
        String narr = "";
        if (bsaAcid.startsWith("706")) {
            String plcode = bsaAcid.substring(13, 18);
            for(String[] parm: pdNarrParm){
                if (plcode.matches(parm[0])){
                    narr = parm[1];
                    break;
                }
            }
        }
        return narr;
    }

    /**
    /**
     * Выбирает проводку нужного типа из списка проводок
     * @param postings
     * @param postType
     * @return
     */
    public long getPcidByType(List<GLPosting> postings, final String postType) {
        if (postings != null && !postings.isEmpty()) {
                GLPosting posting = find(postings, (GLPosting post) -> post.getPostType().equals(postType), postings.get(0));
                return posting.getId();
        }
        return 0L;
    }

    @PostConstruct
    public void postConstruct() {
        try {
            pdNarrParm = ImmutableList.<String[]>builder()
                    .addAll(select("select PLCODREGEX, RNARLNG from EXPDNARPARM").stream()
                            .map(record -> new String[]{record.getString("PLCODREGEX"), record.getString("RNARLNG")})
                            .collect(Collectors.toList())).build();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error on initializing exchange narralive parameters", e);
            pdNarrParm = Collections.emptyList();
        }
    }

}