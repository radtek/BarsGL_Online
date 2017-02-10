package ru.rbt.barsgl.ejb.controller.excel;

import org.apache.commons.lang3.StringUtils;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.card.CardPst;
import ru.rbt.barsgl.ejb.entity.card.CardXls;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.BatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.entity.sec.AuditRecord;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountController;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountService;
import ru.rbt.barsgl.ejb.integr.oper.BatchPostingProcessor;
import ru.rbt.barsgl.ejb.repository.BankCurrencyRepository;
import ru.rbt.barsgl.ejb.repository.BatchPackageRepository;
import ru.rbt.barsgl.ejb.repository.BatchPostingRepository;
import ru.rbt.barsgl.ejb.repository.RateRepository;
import ru.rbt.barsgl.ejb.repository.dict.CardPostingRepository;
import ru.rbt.barsgl.ejb.repository.dict.CardXlsRepository;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.ejbcore.security.RequestContextBean;
import ru.rbt.barsgl.ejbcore.util.ExcelParser;
import ru.rbt.barsgl.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.barsgl.shared.ctx.UserRequestHolder;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.enums.InvisibleType;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

import static ru.rbt.barsgl.ejbcore.util.StringUtils.substr;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.listToString;
/**
 * Created by ER22317 on 21.09.2016.
 */
@Stateless
@LocalBean
public class CardMessageProcessorBean implements CardMessageProcessor {
    private static int START_ROW = 1;
    private static String LIST_DELIMITER = "#";

    @Inject
    private RequestContextBean contextBean;

    @Inject
    private RateRepository rateRepository;

    @EJB
    OperdayController operdayController;

    @Inject
    private CardPostingRepository cardPostingRepository;
    @Inject
    private BankCurrencyRepository bankCurrencyRepository;

    @Inject
    private CardXlsRepository cardXlsRepository;

    @Inject
    GLAccountController glAccountController;
    @EJB
    private BatchPostingRepository postingRepository;
    @Inject
//    private ManualPostingProcessor postingProcessor;
    private BatchPostingProcessor postingProcessor;
    @EJB
    private AuditController auditController;
    @Inject
    private BatchPackageRepository packageRepository;
    @Inject
    private GLAccountService glAccountService;

    @Override
    public String processMessage(File file, Map<String,String> params) throws Exception {
        String fileName = params.get("filename");
        String userIdStr = params.get("userid");
        Long userId = userIdStr == null ? null : Long.valueOf(userIdStr);
        boolean movementOff = "true".equals(params.get("movement_off"));
        String dealSource = params.get("source");
        String department = params.get("department");

        final UserRequestHolder requestHolder = contextBean.getRequest().orElse(UserRequestHolder.empty());
        String userName = requestHolder.getUser();
        String filial = requestHolder.getUserWrapper().getFilial();
        if (null == userId) {
            userId = requestHolder.getUserWrapper().getId();
        }

        BatchPackage pkg = createPackage(userName, fileName);
        loadPackage(pkg, file);
        List<CardXls> cardxls = cardXlsRepository.getOkCardByPkg(pkg);
        for (CardXls card:cardxls) {
            try {
                CardPst cardPst = convert(pkg.getId(), card, dealSource);
                cardPostingRepository.save(cardPst);
            } catch(Exception ex) {
                auditController.error(AuditRecord.LogCode.CardMessageProcessorBean,"заполнение gl_cardpst",null,ex);
            }
        }
        return processCardpstPackage(pkg, userId, filial, fileName, movementOff, dealSource, department);
    }

    private CardXls convert(List<Object> row, Long pkgId, int rowNum, List<Object> header) {
        CardXls card = new CardXls();
        final int FieldNum = 9;
        try {
            card.setPackageId(pkgId);
            card.setRowNumber(rowNum);
            StringBuilder err =  new StringBuilder();
            if (hasNull(row, header, err)) {
                card.setEcode("1");
                card.setEmsg(err.toString().substring(0, 255));
            }else if (row.size() != FieldNum){
                card.setEcode("1");
                card.setEmsg("Строка " + rowNum + ": должно быть " +FieldNum + " полей");
            }else {
                card.setDc((String) row.get(0));
                if (row.get(1) instanceof java.util.Date)
                    card.setValueDate((Date) row.get(1));
                else
                    card.setValueDate(new SimpleDateFormat("dd-MM-yyyy").parse(row.get(1).toString()));

                if (row.get(2) instanceof String)
                    card.setAmount(new BigDecimal(row.get(2).toString()));
                else if (row.get(2) instanceof Double)
                    card.setAmount(new BigDecimal((Double)row.get(2)));
                else if (row.get(2) instanceof Integer)
                    card.setAmount(new BigDecimal((Integer)row.get(2)));
                else throw new IllegalArgumentException("Формат поля суммы может быть строковым или числовым");

                card.setCnum(StringUtils.leftPad((String) row.get(3), 8, "0"));
                card.setMdacc((String) row.get(4));
                card.setBsaacid((String) row.get(5));
                card.setCard((String) row.get(6));
                card.setPnar((String) row.get(7));
                card.setRnrtl((String) row.get(8));
//throw new Exception("hhhhhhhhh");
                card.setEcode("0");
            }
        } catch (Exception ee) {
            try {
                postingRepository.executeInNewTransaction(persistence -> {
                    writeErr2Card(card, ee);
                    return null;
                });
            }catch (Exception e){}
        }
        return card;
    }

    private boolean hasNull(List<Object> r, List<Object> h, StringBuilder err){
        for(int i = 0; i < r.size(); i++) {
            if ( (r.get(i) == null || ((r.get(i) instanceof String) && ((String) r.get(i)).trim().isEmpty()))
                    &&(i != 5 )&&(i != 7 )&&(i != 8)) {
               err.append("пустая колонка ").append(h.get(i)).append(";") ;
            }
        }
        return err.length() > 0;
    }

    private BatchPackage createPackage(String userName, String fileName) {
        BatchPackage pkg = new BatchPackage();
        pkg.setUserName(userName);
        pkg.setPackageState(BatchPackage.PackageState.INPROGRESS);
        pkg.setFileName(fileName);
        pkg.setDateLoad(new Date());
        pkg = packageRepository.save(pkg);
        return pkg;
    }

    private void loadPackage(BatchPackage pkg, File file) throws Exception {
        try (
                InputStream is = new FileInputStream(file);
                ExcelParser parser = new ExcelParser(is);
        ) {
            Iterator<List<Object>> it = parser.parseSafe(0);
            if(!it.hasNext()) {
                throw new Exception("Нет строк для загрузки!");
            }
            List<Object> header = it.next();
            int rowNum = START_ROW;
            while (it.hasNext()) {
                List<Object> row = it.next();
                CardXls card = convert(row, pkg.getId(), rowNum, header);
                cardXlsRepository.save(card);
                rowNum++;
            }
        }
    }

    private boolean isCustomerExists(CardXls card) throws SQLException {
        return null != cardXlsRepository.selectFirst("select * from sdcustpd where bbcust=?", new Object[]{card.getCnum()});
    }

    public String getAcid(CardXls card) {
        return card.getCnum() + card.getMdacc();
    }

    public List<DataRecord> getBsaacidByAcid(CardXls card) throws SQLException {
        String vAcid = getAcid(card);
        List<DataRecord> bsaacids = cardXlsRepository.select("select bsaacid from accrln where acid=? and drlnc='2029-01-01'", new Object[]{vAcid});
        return bsaacids;
    }

    private static final Map<String, String> mAccType = new HashMap<String, String>() {{
        put("1.1","865010101");
        put("1.2","865010102");
        put("2",  "865010200");
        put("4",  "865010300");
    }};

    public String getAccountType(CardXls card) {
        return mAccType.get(card.getCard());
    }

    public String getAcc2(CardXls card) throws Exception {
        String vAcctype = getAccountType(card);
        if (null == vAcctype) {
            throw new Exception("Картотека "+vAcctype+" не найдена");
        }
        List<DataRecord> acc2 = cardXlsRepository.select("select acc2 from gl_actparm where acctype=?", new Object[]{vAcctype});
        if (acc2.isEmpty()) {
            throw new Exception("acc2 " + acc2 + " не найдено");
        } else {
            return acc2.get(0).getString(0);
        }
    }

    public String getDealId(CardXls card) throws Exception {
        switch (card.getCard()) {
            case "1.1":
                return substr(card.getMdacc(),7, 9);
            case "1.2":
            case "2":
            case "4":
                return card.getBsaacid();
            default:
                throw new Exception("DealId для "+card.getCard()+" не найдено");
        }
    }

    public String getBranch(CardXls card) {
        return substr(card.getMdacc(), 9, 12);
    }

    public String getFilial(CardXls card) throws Exception {
        String vBranch = getBranch(card);
        List<DataRecord> fil = cardXlsRepository.select("select bcbbr from imbcbbrp where a8brcd=?", new Object[]{vBranch});
        if (fil.isEmpty()) {
            throw new Exception("филиал "+vBranch+" не найден");
        } else {
            return fil.get(0).getString(0);
        }
    }

    public String getCurrency(CardXls card) {
        return card.getMdacc().substring(0, 3);
    }

    public String getBsaacid(CardXls card, String currency, String dealSource) throws Exception {
        List<DataRecord> bsaacids = getBsaacidByAcid(card);
        if (bsaacids.isEmpty()) {
            String vDealId = getDealId(card);
            String vCum = card.getCnum();

            ManualAccountWrapper wrapper = new ManualAccountWrapper();
            wrapper.setBranch(getBranch(card));
            wrapper.setCurrency(currency);
            wrapper.setCustomerNumber(vCum);
            wrapper.setAccountType(Long.parseLong(getAccountType(card)));
            wrapper.setDealId(vDealId);
            wrapper.setSubDealId(card.getCard());
            wrapper.setAccountCode(Short.parseShort(card.getMdacc().substring(3,7)));
            wrapper.setAccountSequence(Short.parseShort(card.getMdacc().substring(7,9)));
            wrapper.setDealSource(dealSource);
            wrapper.setDateOpenStr(new SimpleDateFormat(wrapper.dateFormat).format(operdayController.getOperday().getCurrentDate()));
//            RpcRes_Base<ManualAccountWrapper> res = remoteAccess.invoke(GLAccountService.class, "createManualAccount", wrapper);
            return glAccountService.createBsaacidAccount(wrapper);
        } else if (bsaacids.size() > 1) {
            throw new Exception("Счетов ЦБ больше 1 acid = "+getAcid(card));
        } else {
            return bsaacids.get(0).getString(0);
        }
    }

    private Map<String, String> mContrAcc;

    public String getContrAcc(String filial) throws Exception {
        if(null == mContrAcc) {
            mContrAcc = new HashMap<String, String>();
            fillContrAcc(mContrAcc);
        }
        return mContrAcc.get(filial);
    }

    private static final SimpleDateFormat ddMMyyyy = new SimpleDateFormat("ddMMyyyy");

    public String getAccountDebit(CardXls card, String bsaacid, String contrAcc) {
        return isDebit(card) ? bsaacid : contrAcc;
    }

    public String getAccountCredit(CardXls card, String bsaacid, String contrAcc) {
        return isCredit(card) ? bsaacid : contrAcc;
    }

    public BankCurrency getCurrencyDebit(CardXls card, String currency) {
        return isDebit(card) ? new BankCurrency(currency) : BankCurrency.RUB;
    }

    public BankCurrency getCurrencyCredit(CardXls card, String currency) {
        return isCredit(card) ? new BankCurrency(currency) : BankCurrency.RUB;
    }

    public BigDecimal getAmountDebit(CardXls card, String currency) throws Exception {
        return isDebit(card) ? card.getAmount() :
                card.getAmount().multiply(rateRepository.getRate(new BankCurrency(currency), card.getValueDate()));
    }

    public BigDecimal getAmountCredit(CardXls card, String currency) throws Exception {
        return isCredit(card) ? card.getAmount() :
                card.getAmount().multiply(rateRepository.getRate(new BankCurrency(currency), card.getValueDate()));
    }

    public boolean isDebit(CardXls card) {
        return "D".equals(card.getDc());
    }

    public boolean isCredit(CardXls card) {
        return "C".equals(card.getDc());
    }

    public CardPst convert(Long pkgId, CardXls card, String dealSource) throws Exception {
        if (!isCustomerExists(card)) {
            throw new Exception("Клиент "+card.getCnum()+" не найден");
        }
        String vFil = getFilial(card);
        String vCCy = getCurrency(card);
        String vBsaacid = getBsaacid(card, vCCy, dealSource);
        String vContrAcc = getContrAcc(vFil);
        String accDebit = getAccountDebit(card, vBsaacid, vContrAcc);
        String accCredit = getAccountCredit(card, vBsaacid, vContrAcc);
        BankCurrency curDebit = getCurrencyDebit(card, vCCy);
        BankCurrency curCredit = getCurrencyCredit(card, vCCy);
        BigDecimal amountDebit = getAmountDebit(card, vCCy);
        BigDecimal amountCredit = getAmountCredit(card, vCCy);

        CardPst cp = new CardPst();
        cp.setPackageId(pkgId);
        cp.setRowNumber(card.getRowNumber());
        cp.setValueDate(card.getValueDate());
        cp.setPaymentRefernce(card.getPnar());
        cp.setAccountDebit(accDebit);
        cp.setCurrencyDebit(curDebit);
        cp.setAmountDebit(amountDebit);

        cp.setAccountCredit(accCredit);
        cp.setCurrencyCredit(curCredit);
        cp.setAmountCredit(amountCredit);
        cp.setNrt(card.getPnar());
        cp.setRnrtl(card.getRnrtl());
        cp.setRnrts(substr(card.getRnrtl(), 100));

        return cp;
    }

    public List<DataRecord> getCardPstByPkg(Long pkgId) throws Exception {
        List<DataRecord> cardpsts = cardPostingRepository.select("select * from GL_CARDPST where ID_PKG=?", new Object[]{pkgId});
        return cardpsts;
    }

    private String processCardpstPackage(BatchPackage pkg, Long userId, String filial, String fileName, boolean movementOff, String sourcePosting, String department) throws Exception {
        List<String> errorList = new ArrayList<String>();
        Date curdate = operdayController.getOperday().getCurrentDate();

        List<DataRecord> cardpsts = getCardPstByPkg(pkg.getId());
        if (cardpsts == null || cardpsts.isEmpty()) return "Файл не загружен pkg_id = " +pkg.getId();
        List<BatchPosting> postings = new ArrayList<>();
        int row = 0;
        BatchPosting posting = createPosting(errorList, cardpsts.get(row), sourcePosting, department);

        Date postDate0 = posting.getPostDate();
        if (null != postDate0) {
            checkBackvaluePermission(postDate0, userId);
            checkFilialPermission(posting.getFilialDebit(), posting.getFilialCredit(), userId);
            postings.add(posting);
            for (++row; row < cardpsts.size(); row++) {
                posting = createPosting(errorList, cardpsts.get(row), sourcePosting, department);
                checkDateEquals(postDate0, posting.getPostDate());
                checkFilialPermission(posting.getFilialDebit(), posting.getFilialCredit(), userId);
                postings.add(posting);
                if (errorList.size() > 10)
                    break;
            }
        }

        if (errorList.size() > 0) {
            throw new ParamsParserException(listToString(errorList, LIST_DELIMITER));
        };
        int errorCount = 0;
        for (BatchPosting it : postings) {
            enrichmentPosting(it, curdate, pkg.getUserName(), filial);
            if (!validatePosting(it))  errorCount++;
            it.setPackageId(pkg.getId());
            postingRepository.save(it);
        }

        pkg.setPostingCount(postings.size());
        pkg.setErrorCount(errorCount);
        pkg.setFileName(fileName);
        pkg.setDateLoad(new Date());
        pkg.setMovementOff(movementOff ? YesNo.Y : YesNo.N);
        pkg.setPackageState(errorCount > 0 ? BatchPackage.PackageState.ERROR : BatchPackage.PackageState.LOADED);
        packageRepository.save(pkg);

        String result = new StringBuffer().append(LIST_DELIMITER)
                .append("ID пакета: ").append(pkg.getId()).append(LIST_DELIMITER)
                .append("Загружено строк всего: ").append(pkg.getPostingCount()).append(LIST_DELIMITER)
                .append("Загружено с ошибкой: ").append(pkg.getErrorCount()).append(LIST_DELIMITER)
                .append(listToString(errorList, LIST_DELIMITER)).append(LIST_DELIMITER)
                .toString();
        auditController.info(AuditRecord.LogCode.CardMessageProcessorBean, "Загружен пакет из файла.\n" + result, BatchPackage.class.getName(), pkg.getId().toString());
        return result;
    }

    public BatchPosting createPosting( List<String> errorList, DataRecord cardpst, String sourcePosting, String department) {
        BatchPosting posting = new BatchPosting();
         try {
             posting.setValueDate(cardpst.getDate("PDATE"));
             posting.setPostDate(cardpst.getDate("PDATE"));
             posting.setDealId(cardpst.getString("DEAL_ID"));
             posting.setSubDealId(cardpst.getString("SUBDEALID"));
             posting.setPaymentRefernce(cardpst.getString("PMT_REF"));

             posting.setAccountDebit(cardpst.getString("AC_DR"));
             posting.setCurrencyDebit(bankCurrencyRepository.findById(BankCurrency.class, cardpst.getString("CCY_DR")));
             posting.setAmountDebit(cardpst.getBigDecimal("AMT_DR"));
             posting.setAccountCredit(cardpst.getString("AC_CR"));
             posting.setCurrencyCredit(bankCurrencyRepository.findById(BankCurrency.class, cardpst.getString("CCY_CR")));
             posting.setAmountCredit(cardpst.getBigDecimal("AMT_CR"));
             posting.setAmountRu(cardpst.getBigDecimal("AMTRU"));

             posting.setNarrative(cardpst.getString("NRT"));
             posting.setRusNarrativeLong(cardpst.getString("RNRTL"));
             posting.setRusNarrativeShort(cardpst.getString("RNRTS"));
             posting.setProfitCenter(cardpst.getString("PRFCNTR"));
             if (cardpst.getString("FGHNG") != null) {
                 posting.setIsCorrection(YesNo.getValue(cardpst.getString("FGHNG").equals("Y")));
             } else
                 posting.setIsCorrection(YesNo.N);

             posting.setCreateTimestamp(new Date());

             posting.setSourcePosting(sourcePosting);
             posting.setDeptId(department);

             posting.setRowNumber(cardpst.getInteger("NROW"));  // TODO передавать?
             posting.setFilialDebit(postingProcessor.getFilial(posting.getAccountDebit()));
             posting.setFilialCredit(postingProcessor.getFilial(posting.getAccountCredit()));
         }catch (Exception e){
             errorList.add(e.getMessage());
         }
        return posting;
    }

    private boolean validatePosting(BatchPosting posting) {
        ManualOperationWrapper wrapper = postingProcessor.createOperationWrapper(posting);
        List<ValidationError> errors = postingProcessor.validate(wrapper, new ValidationContext());

        if (!errors.isEmpty()) {
            posting.setErrorCode(1);
            posting.setErrorMessage(postingProcessor.validationErrorMessage(errors, wrapper.getErrorList()));
            return false;
        }
        return true;
    }

    private BatchPosting enrichmentPosting(BatchPosting posting, Date curdate, String userName, String filial) {
        posting.setStatus(BatchPostStatus.INPUT);
        posting.setInputMethod(InputMethod.F);
        posting.setInvisible(InvisibleType.N);
//        posting.setFilialDebit(postingProcessor.getFilial(posting.getAccountDebit()));
//        posting.setFilialCredit(postingProcessor.getFilial(posting.getAccountCredit()));

        // опердень создания
        posting.setProcDate(curdate);
        // текущее системное время
        posting.setCreateTimestamp(new Date());
        // создатель
        posting.setUserName(userName);
        posting.setUserFilial(filial);
        return posting;
    }

    public void checkFilialPermission(String filialDebit, String filialCredit, Long userId) throws Exception {
        try {
            postingProcessor.checkFilialPermission(filialDebit, filialCredit, userId);
        } catch (ValidationError e) {
            String msg = ErrorCode.PACKAGE_FILIAL_NOT_ALLOWED.getRawMessage();
            auditController.warning(AuditRecord.LogCode.CardMessageProcessorBean, msg, null, e);
            throw new ParamsParserException(msg);
        }
    }

    public void checkBackvaluePermission(Date postDate, Long userId) throws Exception {
        try {
            postingProcessor.checkBackvaluePermission(postDate, userId);
        } catch (ValidationError e) {
            String msg = "Недопустимая дата проводки:\n" + ValidationError.getErrorText(e.getMessage());
            auditController.warning(AuditRecord.LogCode.CardMessageProcessorBean, msg, null, e);
            throw new ParamsParserException(msg);
        }
    }

    public void checkDateEquals(Date postDate0, Date postDate) throws ParamsParserException {
        if (!postDate0.equals(postDate)) {
            throw new ParamsParserException("Пакет содержит проводки с разной датой");
        }
    }

    private void fillContrAcc(Map<String, String> m) throws Exception{
        List<DataRecord> fils = cardXlsRepository.select("select ccode,bsaacid from accrln where rlntype='T' and acc2='99999'", null);
        for(DataRecord it: fils){
            m.put(it.getString(0),it.getString(1));
        }
    }

    private void writeErr2Card(CardXls c, Exception err){
//        try {
            c.setEcode("1");
            c.setEmsg(err.getMessage());
            cardXlsRepository.update(c);
            auditController.error(AuditRecord.LogCode.CardMessageProcessorBean,"Ошибка записи gl_exlcard",null, err);
//        } catch (Exception ex) {
//            auditController.error(AuditRecord.LogCode.GLVD_PSTR_LOAD,"Ошибка записи ошибки",null, ex);
//        }
    }
}
