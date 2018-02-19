package ru.rbt.barsgl.ejb.integr.acc;

import org.apache.log4j.Logger;
import ru.rb.ucb.util.AccountUtil;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.acc.*;
import ru.rbt.barsgl.ejb.entity.dict.AccType.ActParm;
import ru.rbt.barsgl.ejb.entity.dict.AccountingType;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.dict.GLRelationAccountingType;
import ru.rbt.barsgl.ejb.entity.dict.GLRelationAccountingTypeId;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.repository.*;
import ru.rbt.barsgl.ejb.repository.dict.AccType.ActParmRepository;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.barsgl.shared.ErrorList;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.shared.ExceptionUtils;

import javax.ejb.*;
import javax.inject.Inject;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Account;
import static ru.rbt.barsgl.ejb.entity.acc.GLAccount.RelationType.E;
import static ru.rbt.barsgl.ejb.entity.acc.GLAccount.RelationType.FIVE;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperSide.C;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperSide.N;
import static ru.rbt.ejbcore.util.StringUtils.*;
import static ru.rbt.ejbcore.validation.ErrorCode.*;

/**
 * Created by ER18837 on 03.08.15.
 * Автоматическое создание счетов по параметрам, хаданным в проводке из АЕ
 */
@Singleton
@AccessTimeout(value = 15, unit = MINUTES)
public class GLAccountController {

    private static final Logger log = Logger.getLogger(GLAccountController.class);

    private java.util.concurrent.locks.Lock monitor = new ReentrantLock();

    @EJB
    private GLAccountRepository glAccountRepository;

    @EJB
    private GLAccountRequestRepository accountRequestRepository;

    @Inject
    private GLAccountProcessor glAccountProcessor;

    @Inject
    private GLAccountProcessorTech glAccountProcessorTech;

    @Inject
    private PLAccountProcessor plAccountProcessor;

    @Inject
    private OfrAccountProcessor ofrAccountProcessor;

    @Inject
    GLAccountFrontPartController glAccountFrontPartController;

    @Inject
    private BankCurrencyRepository bankCurrencyRepository;

    @Inject
    private OperdayController operdayController;

    @EJB
    private AuditController auditController;

    @Inject
    private AccRlnRepository accRlnRepository;

    @Inject
    private ExcacRlnRepository excacRlnRepository;

    @Inject
    private BsaAccRepository bsaAccRepository;

    @Inject
    private GLRelationAccountingTypeRepository relationAccountingTypeRepository;

    @Inject
    private DateUtils dateUtils;

    @Inject
    private ActParmRepository actParmRepository;

    @EJB
    private GLOperationRepository operationRepository;

    @Inject
    private AccRepository accRepository;

    @Inject
    private AccountCreateSynchronizer synchronizer;

    @Lock(LockType.READ)
    public GLAccount findGLAccount(String bsaAcid) {
        return glAccountRepository.findGLAccount(bsaAcid);
    }

    @EJB
    private PropertiesRepository propertiesRepository;

    @Lock(LockType.READ)
    public GLAccount findGLAccountAE(AccountKeys keys, GLOperation.OperSide side) {
        return findGLAccountAEnoLock(keys, side);
    }

    private GLAccount findGLAccountAEnoLock(AccountKeys keys, GLOperation.OperSide side) {
        return glAccountRepository.findGLAccountAE(
                keys.getBranch(), keys.getCurrency(), keys.getCustomerNumber(),
                keys.getAccountType(), keys.getCustomerType(), keys.getTerm(), keys.getGlSequence(),
                operdayController.getOperday().getCurrentDate());
    }

    @Lock(LockType.READ)
    public GLAccount findGLAccountMnl(AccountKeys keys) {
        return findGLAccountMnlnoLock(keys);
    }

    private GLAccount findGLAccountMnlnoLock(AccountKeys keys) {
        return glAccountRepository.findGLAccountMnl(
                keys.getBranch(), keys.getCurrency(), keys.getCustomerNumber(),
                keys.getAccountType(), keys.getCustomerType(), keys.getTerm(),
                keys.getDealSource(), keys.getDealId(), keys.getSubDealId(),
                operdayController.getOperday().getCurrentDate());
    }

    @Lock(LockType.READ)
    public GLAccount findGLPLAccountMnl(AccountKeys keys, Date dateOpen) {
        return findGLPLAccountMnlnoLock(keys, dateOpen);
    }

    private GLAccount findGLPLAccountMnlnoLock(AccountKeys keys, Date dateOpen) {
        return glAccountRepository.findGLPLAccountMnl(
                keys.getCurrency(), keys.getCustomerNumber(),
                keys.getAccountType(), keys.getCustomerType(), keys.getTerm(),
                keys.getPlCode(), keys.getAccount2(), keys.getCompanyCode(),
                dateOpen);
    }

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public GLAccount createGLAccountAE(GLOperation operation, GLOperation.OperSide operSide, Date dateOpen,
                                       AccountKeys keys, ErrorList descriptors) throws Exception {
        return synchronizer.callSynchronously(monitor, () -> {
            final GLAccount[] glAccount = {findGLAccountAEnoLock(keys, operSide)};     // счет создается вручную
            if (null != glAccount[0]) {
                return glAccount[0];
            }
            return glAccountRepository.executeInNewTransaction(persistence -> {
                // сгенерировать номер счета ЦБ
                String bsaAcid = getAccountNumber(operSide, dateOpen, keys);
                // создать счет с этим номером в GL и BARS
                glAccount[0] = createAccount(bsaAcid, operation, operSide, dateOpen, keys, GLAccount.OpenType.AENEW);
                return glAccount[0];
            });
        });
    }

    //todo XX
    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String createGLAccountXX(GLOperation operation, GLOperation.OperSide operSide, Date dateOpen,
                                       AccountKeys keys, ErrorList descriptors) throws Exception {
        return synchronizer.callSynchronously(monitor, () -> {
            String bsaacidXX = findBsaAcid_for_XX(keys.getAccountMidas(), keys, operSide.getMsgName());
            // счет создается вручную
            if (null != bsaacidXX) {
                return bsaacidXX;
            }

            if (keys.getAccSequence().equals("00")) {
                throw new ValidationError(GL_SEQ_XX_KEY_WITH_SQ_0, operSide.getMsgName(), defaultString(keys.getAccountType())
                        , defaultString(keys.getCustomerNumber()), defaultString(keys.getAccountCode()), defaultString(keys.getAccSequence())
                        , defaultString(keys.getDealId()), defaultString(keys.getPlCode()), defaultString(keys.getGlSequence()) );
            }
            DataRecord res = glAccountRepository.getAccountTypeParams(keys.getAccountType());
            if (res.getString("FL_CTRL").equals("Y")) {
                throw new ValidationError(GL_SEQ_XX_KEY_WITH_FL_CTRL, operSide.getMsgName(), defaultString(keys.getAccountType())
                        , defaultString(keys.getCustomerNumber()), defaultString(keys.getAccountCode()), defaultString(keys.getAccSequence())
                        , defaultString(keys.getDealId()), defaultString(keys.getPlCode()), defaultString(keys.getGlSequence()));
            }
            // сгенерировать номер счета ЦБ
            String bsaAcid = getAccountNumber(operSide, dateOpen, keys);
            // создать счет с этим номером в GL и BARS
            return createAccount(bsaAcid, operation, operSide, dateOpen, keys, GLAccount.OpenType.AENEW).getBsaAcid();
        });
    }

    /**
     * Открытие счета, когда заполнены ключи и счет
     * @param operation
     * @param operSide
     * @param dateOpen
     * @param keys
     * @return
     * @throws Exception
     */
    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public GLAccount findOrCreateGLAccountAEWithKeys(
            GLOperation operation, GLOperation.OperSide operSide, Date dateOpen, AccountKeys keys) throws Exception {
        return synchronizer.callSynchronously(monitor, () -> {
            GLAccount glAccount = findGLAccountWithKeys(operation, operSide);
            if (null != glAccount) {
                return glAccount;
            }

            DataRecord companyCode = Optional
                    .ofNullable(glAccountRepository.selectFirst("select bcbbr, a8cmcd from imbcbbrp where a8brcd = ?", keys.getBranch()))
                    .orElseThrow(() -> new ValidationError(ErrorCode.COMPANY_CODE_NOT_FOUND
                            , format("Не удалось определить код компании по бранчу '%s'", keys.getBranch())));
            keys.setCompanyCode(companyCode.getString("bcbbr"));
            keys.setFilial(companyCode.getString("a8cmcd"));

            // создать счет с этим номером в GL и BARS
            glAccount = createAccount(getGlAccountNumberWithKeys(operation, operSide), operation, operSide, dateOpen, keys, GLAccount.OpenType.AENEW);

            return glAccount;
        });
    }

    /**
     * Открытие техническолго счета, когда заполнены ключи и счет
     * @param operation
     * @param operSide
     * @param dateOpen
     * @param keys
     * @return
     * @throws Exception
     */
    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public GLAccount findOrCreateGLAccountTH(
            GLOperation operation, AccountingType accType,GLOperation.OperSide operSide, Date dateOpen, AccountKeys keys) throws Exception {
        return synchronizer.callSynchronously(monitor, () -> {
            GLAccount glAccount = findTechnicalAccountTH(accType,keys.getCurrency(),keys.getCompanyCode());
            if (null != glAccount) {
                return glAccountRepository.reopenAccountTH(glAccount);
            }
            // создать счет с этим номером в GL и BARS
            String bsaacid = this.getGlAccountNumberTHWithKeys(operation,operSide,keys);
            glAccount = createAccountTH(bsaacid, operation, operSide, dateOpen, keys, GLAccount.OpenType.AENEW);
            return glAccount;
        });
    }

    /**
     * Поиск открытого счета по стороне операции
     * @param operation операция GL
     * @param operSide сторона дебит/крети
     * @return счет
     */
    @Lock(LockType.READ)
    public GLAccount findGLAccountWithKeys(GLOperation operation, GLOperation.OperSide operSide) {
        return glAccountRepository.findGLAccount(getGlAccountNumberWithKeys(operation, operSide));
    }

    @Lock(LockType.READ)
    public String getGlAccountNumberWithKeys(GLOperation operation, GLOperation.OperSide operSide) {
        String bsaacid = C == operSide ? operation.getAccountCredit() : operation.getAccountDebit();
        Assert.isTrue(!isEmpty(bsaacid), format("Не заполнен счет ЦБ по стороне '%s'", operSide.getMsgName()));
        return bsaacid;
    }

    /**
     * Генерация номера для технического счёта
     * @param operation
     * @param operSide
     * @param keys
     * @return
     */
    @Lock(LockType.READ)
    public String getGlAccountNumberTHWithKeys(GLOperation operation, GLOperation.OperSide operSide, AccountKeys keys) {

        DataRecord drAccParm = glAccountRepository.getActParamByAccType(keys.getAccountType());
        if (drAccParm==null)
        {
            throw new ValidationError(ACCOUNT_PARAMS_NOT_FOUND,"");
        }

        String bsaacidMask = new StringBuilder().append(drAccParm.getString("ACC2")).append(keys.getCurrencyDigital()).append("0").append(keys.getCompanyCode()).append("0" + StringUtils.substr(keys.getAccountType(), 3, 9)).toString();
        String bsaacid = glAccountFrontPartController.calculateKeyDigit(bsaacidMask, keys.getCompanyCode());


        Assert.isTrue(!isEmpty(bsaacid), format("Не заполнен счет ЦБ по стороне '%s'", operSide.getMsgName()));


        return bsaacid;
    }

    /**
     * Генерация номера для технического счёта
     * @param operSide
     * @param keys
     * @return
     */
    @Lock(LockType.READ)
    public String getGlAccountNumberTHWithKeys(GLOperation.OperSide operSide, AccountKeys keys) {

        return getGlAccountNumberTHWithKeys(null, operSide, keys);
    }

    /**
     * Генерация номера для технического счёта
     * @param keys
     * @return
     */
    @Lock(LockType.READ)
    public String getGlAccountNumberTHWithKeys(AccountKeys keys) {

        return getGlAccountNumberTHWithKeys(null, GLOperation.OperSide.N, keys);
    }


    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public GLAccount createGLAccountMnl(AccountKeys keys, Date dateOpen, ErrorList descriptors, GLAccount.OpenType openType) throws Exception {
        return internalCreateGLAccountMnl(keys, descriptors, dateOpen, openType);
    }

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public GLAccount createGLAccountMnlInRequiredTrans(AccountKeys keys, Date dateOpen, ErrorList descriptors, GLAccount.OpenType openType) throws Exception {
        return internalCreateGLAccountMnl(keys, descriptors, dateOpen, openType);
    }

    private GLAccount internalCreateGLAccountMnl(AccountKeys keys, ErrorList descriptors, Date dateOpen, GLAccount.OpenType openType) throws Exception {
        return synchronizer.callSynchronously(monitor, () -> {
            GLAccount glAccount = findGLAccountMnlnoLock(keys);     // счет создается вручную
            if (null != glAccount) {
                return glAccount;
            }
            List<ValidationError> errors = glAccountProcessor.validate(keys, new ValidationContext());
            if (!errors.isEmpty()) {
                throw new DefaultApplicationException(glAccountProcessor.validationErrorMessage(GLOperation.OperSide.N, errors, descriptors));
            }
            // Убрала проверку dealId - не надо для клиентских счетов и счетов доходов-расходов
            //        glAccountProcessor.checkDealId(dateOpen, keys.getDealSource(), keys.getDealId(), keys.getSubDealId());

            // сгенерировать номер счета ЦБ
            String bsaAcid = getAccountNumber(GLOperation.OperSide.N, dateOpen, keys);
            // создать счет с этим номером в GL и BARS
            glAccount = createAccount(bsaAcid, null, GLOperation.OperSide.N, dateOpen, keys, openType);

            return glAccount;
        });
    }

    @Lock(LockType.READ)
    public void validateGLAccountMnl(GLAccount glAccount, Date dateOpen, Date dateClose,
                                        AccountKeys keys, ErrorList descriptors) throws Exception {
        List<ValidationError> errors = glAccountProcessor.validate(keys, new ValidationContext());
        if (!errors.isEmpty()) {
            throw new DefaultApplicationException(glAccountProcessor.validationErrorMessage(N, errors, descriptors));
        }
        // Убрала проверку dealId - не надо для клиентских счетов и счетов доходов-расходов
//        glAccountProcessor.checkDealId(dateOpen, keys.getDealSource(), keys.getDealId(), keys.getSubDealId());
        glAccountProcessor.checkDateOpenMnl(glAccount, dateOpen);
        if (null != dateClose) {
            glAccountProcessor.checkDateCloseMnl(glAccount, dateOpen, dateClose);
        }
    }

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public GLAccount updateGLAccountMnl(GLAccount glAccount, Date dateOpen, Date dateClose,
                                        AccountKeys keys, ErrorList descriptors) throws Exception {
        return synchronizer.callSynchronously(monitor, () -> {
            glAccount.setDateOpen(dateOpen);
            glAccount.setDateClose(dateClose);
            glAccount.setDealId(keys.getDealId());
            glAccount.setSubdealId(keys.getSubDealId());
            glAccount.setDescription(keys.getDescription());

            return glAccountRepository.update(glAccount);
        });
    }

    @Lock(LockType.READ)
    public void validateGLAccountMnlTech(GLAccount glAccount, Date dateOpen, Date dateClose,
                                     AccountKeys keys, ErrorList descriptors) throws Exception {
        List<ValidationError> errors = glAccountProcessorTech.validate(keys, new ValidationContext());
        if (!errors.isEmpty()) {
            throw new DefaultApplicationException(glAccountProcessor.validationErrorMessage(N, errors, descriptors));
        }
        glAccountProcessorTech.checkDateOpenMnl(glAccount, dateOpen);
        if (null != dateClose) {
            glAccountProcessorTech.checkDateCloseMnl(glAccount, dateOpen, dateClose);
        }
    }

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public GLAccount updateGLAccountMnlTech(GLAccount glAccount, Date dateOpen, Date dateClose,
                                        AccountKeys keys, ErrorList descriptors) throws Exception {
        return synchronizer.callSynchronously(monitor, () -> {
            glAccount.setDateOpen(dateOpen);
            glAccount.setDateClose(dateClose);

            return glAccountRepository.update(glAccount);
        });
    }

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public GLAccount closeGLAccountMnl(GLAccount glAccount, Date dateClose,
                                       ErrorList descriptors) throws Exception {
        return glAccountRepository.executeInNewTransaction(persistence -> {
            glAccount.setDateClose(dateClose);

            return glAccountRepository.update(glAccount);
        });
    }

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public GLAccount closeGLAccountMnlTech(GLAccount glAccount, Date dateClose,
                                       ErrorList descriptors) throws Exception {
        return glAccountRepository.executeInNewTransaction(persistence -> {
            glAccount.setDateClose(dateClose);

            return glAccountRepository.update(glAccount);
        });
    }

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String createPlAccount(AccountKeys keys, Date dateOpen, Date dateStart446P, GLOperation operation, GLOperation.OperSide operSide) throws Exception {
        return synchronizer.callSynchronously(monitor, () -> {
            try {
                return glAccountRepository.executeInNonTransaction(connection -> {
                    try {
                        // Поиск счета в таблице ACCRLN
                        String bsaAcid = ofrAccountProcessor.findAccount(connection, keys, dateStart446P);
                        if (bsaAcid == null) {  // нету - создаем
                            bsaAcid = ofrAccountProcessor.createAccount(connection, keys, dateOpen, DAY20290101, dateStart446P);     // 1.0.5
                            if (bsaAcid != null) {  // запись в GL_ACC
                                createAccount(bsaAcid, operation, operSide, dateOpen, keys, GLAccount.OpenType.AENEW);
                            }
                        }
                        return bsaAcid;
                    } catch (Exception e) {
                        throw new DefaultApplicationException(getErrorMessage(e), e);
                    }
                });
            } catch (SQLException e) {
                throw new DefaultApplicationException(getErrorMessage(e), e);
            }
        });
    }

    /**
     * GL счета доходов/расходов
     * @param keys
     * @param operation
     * @return
     */
    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String createGLPLAccount(final AccountKeys keys, final GLOperation operation, GLOperation.OperSide operSide) throws Exception {
        return synchronizer.callSynchronously(monitor, () -> {
            return Optional.ofNullable(glAccountRepository.findGLPLAccount(keys.getCurrency()
                    , keys.getCustomerNumber(), keys.getAccountType(), keys.getCustomerType()
                    , keys.getTerm(), keys.getPlCode(), keys.getCompanyCode(), operation.getValueDate()))
                    .orElseGet(() ->
                    {
                        try {
                            final String bsaacid = glAccountRepository
                                    .executeInNonTransaction(connection -> AccountUtil.generateAccountNumber(connection
                                            , keys.getAccount2(), keys.getCurrencyDigital(), keys.getCompanyCode(), keys.getPlCode()));
                            final GLAccount account = createAccount(bsaacid, operation, N, operation.getValueDate(), keys, GLAccount.OpenType.AENEW);
                            glAccountRepository.updateRelationType(account, FIVE);
                            GLRelationAccountingType rt = Optional.ofNullable(relationAccountingTypeRepository
                                    .findById(GLRelationAccountingType.class, new GLRelationAccountingTypeId(account.getBsaAcid()
                                            , account.getAcid()))).orElseGet(() -> relationAccountingTypeRepository.createRelation(account.getAcid()
                                    , account.getBsaAcid(), Long.toString(account.getAccountType())));
                            auditController.info(Account
                                    , format("Создан счет '%s' для операции '%d' '%s'", account.getBsaAcid(), operation.getId(), operSide.getMsgName()), operation);
                            return account;
                        } catch (Exception e) {
                            throw new DefaultApplicationException(e.getMessage(), e);
                        }
                    }).getBsaAcid();
        });
    }

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public GLAccount createGLPLAccountMnl(final AccountKeys keys, Date dateOpen, ErrorList descriptors, GLAccount.OpenType openType) throws Exception {
        return synchronizer.callSynchronously(monitor, () ->{
            GLAccount glAccount = findGLPLAccountMnlnoLock(keys, dateOpen);     // счет создается вручную
            if (null != glAccount) {
                return glAccount;
            }
            // TODO glPlAccountProcessor
            List<ValidationError> errors = plAccountProcessor.validate(keys, new ValidationContext());
            if (!errors.isEmpty()) {
                throw new DefaultApplicationException(glAccountProcessor.validationErrorMessage(GLOperation.OperSide.N, errors, descriptors));
            }

            final String bsaacid = getPlAccountNumber(keys.getAccount2(), keys.getCurrencyDigital(), keys.getCompanyCode(), keys.getPlCode());
            final GLAccount account = createAccount(bsaacid, null, N, dateOpen, keys, openType);
            Assert.notNull(keys.getRelationType(), "Не задан RLNTYPE для создания счета доходов / расходов");
            glAccountRepository.updateRelationType(account, GLAccount.RelationType.parse(keys.getRelationType()));
            relationAccountingTypeRepository.createRelation(account.getAcid()
                    , account.getBsaAcid(), Long.toString(account.getAccountType()));
//        auditController.info(Account
//                , format("Создан счет ОФР '%s' по ручному вводу", account.getBsaAcid()));
            return account;
        });
    }

    public String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, DefaultApplicationException.class);
    }

    private GLAccount createAccount(String bsaAcid, GLOperation operation, GLOperation.OperSide operSide, Date dateOpen,
                                    AccountKeys keys, GLAccount.OpenType openType) {

        // создать счет GL
        GLAccount glAccount = glAccountProcessor.createGlAccount(bsaAcid, operation, operSide, dateOpen, keys, openType);
        // определить дополнительные папаметры счета
        glAccountProcessor.enrichment(glAccount, keys);
        // сохранить счет GL
        return glAccountRepository.save(glAccount);
    }

    public GLAccount createAccountTH(String bsaAcid, GLOperation operation, GLOperation.OperSide operSide, Date dateOpen,
                                    AccountKeys keys, GLAccount.OpenType openType) {

        // создать счет GL
        GLAccount glAccount = glAccountProcessor.createGlAccountTH(bsaAcid, operation, operSide, dateOpen, keys, openType);
        return glAccountRepository.save(glAccount);
    }

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public GlAccRln createAccountsExDiff(GLOperation operation, GLOperation.OperSide operSide, AccountKeys keys, Date dateOpen, BankCurrency bankCurrency, String optype) {
        GlAccRln accRln = excacRlnRepository.findForPlcode7903(keys, bankCurrency, optype);
        if (accRln == null) {
            accRln = calculateAcidBsaacid(operSide, keys, bankCurrency, optype);
            // Возвращаемое значение
            GLAccount glAccount = new GLAccount();

            glAccount.setAcid(accRln.getId().getAcid());
            glAccount.setBsaAcid(accRln.getId().getBsaAcid());
            glAccount.setDateOpen(dateOpen);
            glAccount.setDateClose(GLAccountController.DAY20290101);
            glAccount.setCustomerNumber(accRln.getId().getAcid().substring(0, 8));
            glAccount.setCompanyCode(keys.getCompanyCode());
            glAccount.setPassiveActive(keys.getPassiveActive());
            glAccount.setAccountCode(Short.parseShort(keys.getAccountCode()));
            glAccount.setCurrency(bankCurrencyRepository.getCurrency("RUR"));
            glAccount.setPlCode(keys.getPlCode());
            glAccount.setRelationType(GLAccount.RelationType.TWO);

            accRlnRepository.createAccRln(glAccount);
            BsaAcc bsaAcc = bsaAccRepository.createBsaAcc(glAccount);

            glAccount.setCurrency(bankCurrency);

            GlExcacRln excacRln = excacRlnRepository.createExcacRln(glAccount, optype);
            auditController.info(Account, format("Создан счет курсовой разницы '%s' для операции '%d' %s",
                    bsaAcc.getId(), operation.getId(), operSide.getMsgName()), bsaAcc);
            accRln = new GlAccRln(excacRln.getId());
        }

        return accRln;
    }

    private GlAccRln calculateAcidBsaacid(GLOperation.OperSide operSide, AccountKeys keys, BankCurrency bankCurrency, String optype) {
        try {
            String psav = keys.getPassiveActive();
            DataRecord dataRecord = glAccountRepository.selectFirst("select ACC2, PLCODE, ACOD, ACSQ from excacparm where (CCY = ? or CCY = '000') and OPTYPE = ? and PSAV = ?",
                    bankCurrency.getCurrencyCode(), optype, psav);

            if (null == dataRecord) {
                throw new ValidationError(ACCOUNTEX_PARAMS_NOT_FOUND, operSide.getMsgName(),
                        bankCurrency.getCurrencyCode(), optype, psav);
            }

            keys.setAccount2(dataRecord.getString("ACC2"));
            keys.setAccSequence(dataRecord.getString("ACSQ"));
            keys.setAccountCode(dataRecord.getString("ACOD"));
            keys.setPlCode(dataRecord.getString("PLCODE"));

            String nccy = bankCurrency.getDigitalCode();
            String c1 = String.valueOf(nccy.charAt(0));
            String c2 = nccy.substring(1, 3);

            String accountMask = new StringBuilder().append(dataRecord.getString("ACC2")).append("810_").append(c1).append(keys.getCompanyCode().substring(1)).append(dataRecord.getString("PLCODE")).append(c2).toString();

            String bsaacid = glAccountFrontPartController.calculateKeyDigit(accountMask, keys.getCompanyCode());   // TODO надо CBCCN

            DataRecord a8bicnRec = Optional.ofNullable(glAccountRepository.selectFirst("select a8bicn from imbcbbrp where a8brcd = ?", keys.getBranch()))
                    .orElseThrow(() -> new ValidationError(ErrorCode.CLIENT_NOT_FOUND, format("Клиент не найден по бранчу %s", keys.getBranch())));

            String acsq = keys.getAccSequence();
            if (acsq.length() == 1) {
                acsq = "0" + acsq;
            }

            // Счет Midas
            String acid = new StringBuilder().append(a8bicnRec.getString("a8bicn")).append("RUR").append(keys.getAccountCode()).append(acsq).append(keys.getBranch()).toString();
            return new GlAccRln(acid, bsaacid);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }


    /**
     * Заполняет недостающие ключи счета Майдас для счетов ОФР (PL)
     *
     * @param side
     * @param dateOpen
     * @param keys
     * @return
     */
    public AccountKeys fillAccountOfrKeysMidas(GLOperation.OperSide side, Date dateOpen, AccountKeys keys) {
        // TODO заполнить ключи счета из справочников
        DataRecord data = glAccountRepository.getAccountParams(keys.getAccountType(), keys.getCustomerType(), keys.getTerm(), dateOpen);
        if (null == data) {
            throw new ValidationError(ACCOUNT_PARAMS_NOT_FOUND, side.getMsgName(),
                    keys.getAccountType(), keys.getCustomerType(), keys.getTerm(),
                    dateUtils.onlyDateString(dateOpen));
        }

        // параметры счета ЦБ
        keys.setAccount2(data.getString("ACC2"));       // ACC2
        keys.setPlCode(data.getString("PLCODE"));       // PLCODE

        // параметры счета Майдас
        keys.setAccountCode(data.getString("ACOD"));    // ACOD
        keys.setAccSequence(data.getString("SQ"));      // SQ

        // тип собственности
        keys.setCustomerType(data.getString("CUSTYPE"));

        // счет Майдас
        int cnum = (int) glAccountProcessor.stringToLong(side, "Customer number", keys.getCustomerNumber(), AccountKeys.getiCustomerNumber());
        short iacod = (short) glAccountProcessor.stringToLong(side, "ACOD Midas", keys.getAccountCode(), AccountKeys.getiAccountCode());
        short isq = (short) glAccountProcessor.stringToLong(side, "SQ Midas", keys.getAccSequence(), AccountKeys.getiAccSequence());
        String acid = glAccountRepository.makeMidasAccount(
                cnum,
                keys.getCurrency(),
                keys.getBranch(),
                iacod,
                isq
        );
        keys.setAccountMidas(acid);

        return keys;
    }

    /**
     * Заполняет недостающие параметры ключей счета ЦБ
     *
     * @param keys
     */
    public AccountKeys fillAccountOfrKeys(GLOperation.OperSide operSide, Date dateOpen, AccountKeys keys) {
        AccountKeys keys1 = fillAccountKeys(operSide, dateOpen, keys);

        keys.setPassiveActive(glAccountRepository.getPassiveActive(keys1.getAccount2()));  // TODO ???

        if (isEmpty(keys.getCustomerType())) {
            keys.setCustomerType("0");
        }
        keys.setRelationType("2");

        return keys;
    }

    /**
     * Заполняет недостающие ключи счета Майдас
     *
     * @param side
     * @param keys
     */
    public AccountKeys fillAccountKeysMidas(GLOperation.OperSide side, Date dateOpen, AccountKeys keys) {
        boolean isGlSeqXX = !isEmpty(keys.getGlSequence()) && keys.getGlSequence().toUpperCase().startsWith("XX");
        /* не будем подменять пришедшие реальные данные, чтоб потом не думать, что сохранять
        if (isEmpty(keys.getCustomerType())) {
            keys.setCustomerType("00");
        }
        if (isEmpty(keys.getTerm())) {
            keys.setTerm("00");
        }*/
        DataRecord data = glAccountRepository.getAccountParams(keys.getAccountType(), keys.getCustomerType(), keys.getTerm(), dateOpen);
        if (null == data) {
            throw new ValidationError(ACCOUNT_PARAMS_NOT_FOUND, side.getMsgName(),
                    keys.getAccountType(), keys.getCustomerType(), keys.getTerm(),
                    dateUtils.onlyDateString(dateOpen));
        }
        // ACC2, PLCODE
        String acc2 = data.getString("ACC2");
        if (isEmpty(keys.getAccount2())) {
            keys.setAccount2(acc2);
        } else if (!(keys.getAccount2().equals(acc2))) {
            final Error error = new ValidationError(ACCOUNT2_NOT_VALID, side.getMsgName(), keys.getAccount2(), acc2,
                    keys.getAccountType(), keys.getCustomerType(), keys.getTerm(), Long.toString(AccountKeys.getiAccountType()));
            if (isKPlusTP(keys)) {
                warnForParameterize(error);
            } else {
                throw error;
            }
        }
        if (isGlSeqXX && data.getString("PLCODE") != null){
            throw new ValidationError(GL_SEQ_XX_KEY_WITH_DB_PLCODE, side.getMsgName(), defaultString(keys.getAccountType()), defaultString(keys.getCustomerNumber()), defaultString(keys.getAccountCode())
                                                  , defaultString(keys.getAccSequence()), defaultString(keys.getDealId()), defaultString(keys.getPlCode()), defaultString(keys.getGlSequence()));
        }

        if (isEmpty(keys.getPlCode())) {
            keys.setPlCode(ifEmpty(data.getString("PLCODE"), ""));
        }
        // параметры счета Майдас
        String acod = data.getString("ACOD");
        String sq = data.getString("SQ");
        if (isEmpty(keys.getAccountCode())) {       // не задан ACOD:
            keys.setAccountCode(acod);                  // взять ACOD из параметров
        } else {    // ACOD задан: ACOD и SQ в ключах должны совпадать с определенными из параметров
            if ( !(acod.equals(keys.getAccountCode()) && (sq.equals(keys.getAccSequence()) || sq.equals("00"))) ) {
                final Error error = new ValidationError(MIDAS_PARAMS_NOT_VALID, side.getMsgName(), keys.getAccountCode(), keys.getAccSequence(), acod, sq,
                        keys.getAccountType(), keys.getCustomerType(), keys.getTerm(),
                        Long.toString(AccountKeys.getiAccountCode()) + "," + Long.toString(AccountKeys.getiAccSequence()));
                if (isKPlusTP(keys)) {
                    warnForParameterize(error);
                } else {
                    throw error;
                }
            }
        }
        // подмена сиквенса Майдас для сделок FCC
        keys.setAccSequence(getMidasSequenceForDeal(side, dateOpen, keys, sq));

        int cnum = (int) glAccountProcessor.stringToLong(side, "Customer number", keys.getCustomerNumber(), AccountKeys.getiCustomerNumber());
        short iacod = (short) glAccountProcessor.stringToLong(side, "ACOD Midas", keys.getAccountCode(), AccountKeys.getiAccountCode());
        short isq = (short) glAccountProcessor.stringToLong(side, "SQ Midas", keys.getAccSequence(), AccountKeys.getiAccSequence());
        String acid = glAccountRepository.makeMidasAccount(
                cnum,
                keys.getCurrency(),
                keys.getBranch(),
                iacod,
                isq
        );
        keys.setAccountMidas(acid);
        return keys;
    }

    /**
     * Определяет SQ на замену для формирования счета Midas
     *
     * @param keys    - ключи счета
     * @param paramSQ - SQ, определенный по таблице параметров
     * @return
     */
    private String getMidasSequenceForDeal(GLOperation.OperSide side, Date dateOpen, AccountKeys keys, final String paramSQ) {
        final String keysSQ = keys.getAccSequence();                  // SQ из ключей
        final String defaultSQ = isEmpty(keysSQ) ? paramSQ : keysSQ;  // SQ по умолчанию
        // номер сделки
        String dealId = keys.getDealId();
        if (isEmpty(dealId)) {
            return defaultSQ;              // номер сделки не задан, подмена не нужна
        }
        // параметры по источнику сделки
        DataRecord param = glAccountRepository.getDealSQParams(keys.getDealSource(), dateOpen);
        if (null == param) {
            return defaultSQ;              // для этого источника подмена не нужна
        }

        // найти SQ по номеру клиента, валюте и номеру сделки
        final String dealIDfrom = param.getString("DEALID");
        if (!dealIDfrom.equals("Y")) {
            dealId = keys.getSubDealId();       // номер сделки из субсделки
            if (isEmpty(dealId)) {              // не задан subDealID
                throw new ValidationError(SUBDEAL_ID_IS_EMPTY, side.getMsgName());
            }
        }
        final String custNo = keys.getCustomerNumber();
        final String ccy = keys.getCurrency();
        final String dealSQ = glAccountRepository.getDealSQ(custNo, ccy, dealId);
        if (!isEmpty(dealSQ)) {                 // SQ для сделки уже задан
            if (!isEmpty(keysSQ) && !keysSQ.equals(dealSQ)) { // если SQ в ключах задан, он должен совпадать с SQ сделки
                throw new ValidationError(MIDAS_SQ_IS_DIFFERENT, side.getMsgName(), keysSQ, dealSQ, dealId);
            }
            return dealSQ;
        }

        // определим, надо ли новый номер SQ
        final String check = param.getString("CHECK");
        if (check.equals("Y")) {
            return paramSQ;             // не надо, возвращаем из параметров
        }

        // надо
        if (isEmpty(keysSQ)) {      // SQ в ключах не был задан, возвращаем SQ из параметров
            return paramSQ;
        } else {                    // SQ в ключах задан
            if (!paramSQ.equals(keysSQ)) {      // записать новый SQ для сделки& если не совпадает со стандартным
                glAccountRepository.addDealSQ(custNo, ccy, dealId, keysSQ);
            }
            return keysSQ;          // возвращаем SQ из ключей
        }
    }

    /**
     * Заполняет недостающие параметры ключей счета ЦБ
     *
     * @param keys
     */
    public AccountKeys fillAccountKeys(GLOperation.OperSide operSide, Date dateOpen, AccountKeys keys) {
        // валюты ЦБ
        BankCurrency currency = bankCurrencyRepository.refreshCurrency(keys.getCurrency());
        keys.setCurrencyDigital(currency.getDigitalCode());

        // филиал и код компанаа
        DataRecord data = glAccountRepository.getFilialByBranch(keys.getBranch());
        if (null == data) {
            throw new ValidationError(BRANCH_NOT_FOUND, "", keys.getBranch(), Long.toString(AccountKeys.getiBranch()));
        }
        keys.setFilial(data.getString(0));
        String cbccn = data.getString(1);
        if (isEmpty(keys.getCompanyCode())) {
            keys.setCompanyCode(data.getString(1));
        } else {
            if (!keys.getCompanyCode().equals(cbccn)) {
                throw new ValidationError(COMPANY_CODE_NOT_VALID, "",
                        keys.getCompanyCode(), Long.toString(AccountKeys.getiCompanyCodeN()),
                        keys.getBranch(), Long.toString(AccountKeys.getiBranch()));
            }

        }
        return keys;
    }

    /**
     * Создает уникальный номер счета по ключам счета
     *
     * @param keys - ключи счета
     * @return
     */
    public String getAccountNumber(GLOperation.OperSide operSide, Date dateOpen, AccountKeys keys) {
        // заполнить недостающие ключи счета Майдас
        fillAccountKeysMidas(operSide, dateOpen, keys);
        return getPureAccountNumber(operSide, dateOpen, keys);
    }

    /**
     * Генерация счета ЦБ без привязки к счету Майдас
     * @return номер счета
     */
    public String getPureAccountNumber(GLOperation.OperSide operSide, Date dateOpen, AccountKeys keys) {
        // заполнить недостающие ключи счета ЦБ
        fillAccountKeys(operSide, dateOpen, keys);

        String acc2 = keys.getAccount2();
        String ccyn = keys.getCurrencyDigital();
        String cbccn = keys.getCompanyCode();
        String plcode = keys.getPlCode();

        // сгенерить номер счета ЦБ
        boolean isNewNumber = false;
        String bsaAcid = "";
        int count = 0;
        int tocnt = getAccountIterateCount();
        String bsaacidFrom = null;
        String bsaacidTo = null;
        do {
            // сформировать строку номера счета
            bsaAcid = generateAccountNumber(acc2, ccyn, cbccn, plcode);
            // проверить наличие счета в БД
            isNewNumber = !glAccountRepository.checkAccountExists(bsaAcid);
            if (!isNewNumber){
                log.warn("Generated BSAACID: '" + bsaAcid + "' already exists");
                if (bsaacidFrom == null) bsaacidFrom = bsaAcid;
                bsaacidTo = bsaAcid;
            }
        } while (!isNewNumber && count++ < tocnt - 1);
        Assert.isTrue(isNewNumber, format("Не удалось сформировать номер счета по ключам: acc2='%s', ccyn='%s', cbccn='%s', plcode='%s'. Начальный счет: '%s' Конечный счет '%s' итераций '%s'",
                acc2, ccyn, cbccn, plcode, bsaacidFrom, bsaacidTo, tocnt));

        return bsaAcid;
    }

    /**
     * Формарует номер счета ЦБ по ключам
     *
     * @param acc2   - балансовый счет 2-го порядка
     * @param ccyn   - код валюты ЦБ
     * @param cbccn  - код компании
     * @param plcode - символ доходов / расходов
     * @return
     */
    private String generateAccountNumber(String acc2, String ccyn, String cbccn, String plcode) {
        String face = glAccountFrontPartController.getNextFrontPartNumber(acc2, ccyn, cbccn, plcode);
        String account = makeAccountPattern(acc2, ccyn, cbccn, plcode, face);
        return glAccountFrontPartController.calculateKeyDigit(account, cbccn);
    }

    /**
     * Формирует счет ЦБ по шаблону по полному набору значений
     *
     * @return
     */
    private String makeAccountPattern(String acc2, String ccyn, String cbccn, String plcode, String face) {

        Assert.isTrue((null != acc2) && (acc2.length() == 5), format("Неверный балансовый счет: %s", acc2));
        Assert.isTrue((null != ccyn) && (ccyn.length() == 3), format("Неверный код валюты: %s", ccyn));
        Assert.isTrue((null != cbccn) && (cbccn.length() == 4), format("Неверный код филиала: %s", cbccn));

        StringBuilder accBuilder = new StringBuilder();
        if (isEmpty(plcode)) {              // GLAccountCounterType.ASSET_LIABILITY;
            Assert.isTrue((null != face) && (face.length() == 8), format("Неверный формат лицевой части счета: %s", face));

            accBuilder.append(acc2).append(ccyn).append("0");
            accBuilder.append(substr(face, 1));
            accBuilder.append(rsubstr(cbccn, 3));
            accBuilder.append(rsubstr(face, 7));
        } else {                            // GLAccountCounterType.PROFIT_LOSS;
            Assert.isTrue((plcode.length() == 5), format("Неверный символ доходов / расходов: %s", cbccn));
            Assert.isTrue((null != face) && (face.length() == 4), format("Неверный формат лицевой части счета: %s", face));

            accBuilder.append(acc2).append(ccyn).append("0");
            accBuilder.append(substr(face, 2));
            accBuilder.append(rsubstr(cbccn, 2));
            accBuilder.append(plcode);
            accBuilder.append(rsubstr(face, 2));
        }

        return accBuilder.toString();
    }

    ;

    /**
     * Создает уникальный номер счета PL
     *
     * @return
     */
    public String getPlAccountNumber(String acc2, String ccyn, String cbccn, String plcode) throws Exception {
        // сгенерить номер счета ЦБ
        boolean isNewNumber = false;
        String bsaAcid = "";
        int count = 0;
        do {
            // сформировать строку номера счета
            bsaAcid = glAccountRepository
                    .executeInNonTransaction(connection -> AccountUtil.generateAccountNumber(connection
                            , acc2, ccyn, cbccn, plcode));
            // проверить наличие счета в БД
            isNewNumber = !glAccountRepository.checkAccountExists(bsaAcid);
            if (!isNewNumber)
                log.warn("Generated PL BSAACID: '" + bsaAcid + "' already exists");
        } while (!isNewNumber && count++ < 100);
        Assert.isTrue(isNewNumber, format("Не удалось сформировать номер счета PL по ключам: acc2='%s', ccyn='%s', cbccn='%s', plcode='%s'",
                acc2, ccyn, cbccn, plcode));

        return bsaAcid;
    }

    public AccountKeys createWrapperAccountKeys(ManualAccountWrapper accountWrapper, Date dateOpen) {
        AccountKeys keys = AccountKeysBuilder.create()
                .withBranch(accountWrapper.getBranch())
                .withCompanyCode(accountWrapper.getCompanyCode())
                .withCurrency(accountWrapper.getCurrency())
                .withCustomerNumber(accountWrapper.getCustomerNumber())
                .withAccountType(accountWrapper.getAccountType().toString())
                .withDealId(accountWrapper.getDealId())
                .withSubDealId(accountWrapper.getSubDealId())
                .withDealSource(accountWrapper.getDealSource())
                .build();

        String cnum = accountWrapper.getCustomerNumber();
        if (isEmpty(glAccountRepository.getCustomerName(cnum))) {
            throw new ValidationError(CUSTOMER_NUMBER_NOT_FOUND, "", cnum, "Клиент");
        }

        if (null != accountWrapper.getCbCustomerType()) {
            keys.setCustomerType(accountWrapper.getCbCustomerType().toString());
        } else {
            Short custType = glAccountRepository.getCustomerType(cnum);
            if (custType != null && custType > 3) {
                keys.setCustomerType(custType.toString());
            }
        }
        if (null != accountWrapper.getTerm()) {
            keys.setTerm(format("%02d", accountWrapper.getTerm()));
        }

        DataRecord data = glAccountRepository.getAccountParams(Long.toString(accountWrapper.getAccountType()),
                keys.getCustomerType(), keys.getTerm(), dateOpen);
        if (null == data) {
            throw new ValidationError(ACCOUNT_PARAMS_NOT_FOUND, N.getMsgName(),
                    keys.getAccountType(), keys.getCustomerType(), keys.getTerm(), dateUtils.onlyDateString(dateOpen));
        } else {
            String acc2 = data.getString("ACC2");
            String plcode = data.getString("PLCODE");
            // это задается во Wrapper только для ручных PL-счетов
            String acc2Wr = accountWrapper.getBalanceAccount2();
            String plcodeWr = accountWrapper.getPlCode();
            if (null == acc2Wr) {
                keys.setAccount2(acc2);
            } else if (acc2.equals(acc2Wr) || acc2.equals("706" + StringUtils.substr(acc2Wr, 3, 5))) {
                keys.setAccount2(acc2Wr);
            } else {
                throw new ValidationError(ACCOUNT2_NOT_CORRECT, acc2Wr, keys.getAccountType(), keys.getCustomerType(), keys.getTerm(), acc2);
            }
            if (isEmpty(plcodeWr) || plcodeWr.equals(plcode)) {
                keys.setPlCode(plcode);
            } else {
                throw new ValidationError(PLCODE_NOT_CORRECT, plcodeWr, keys.getAccountType(), keys.getCustomerType(), keys.getTerm(), plcode);
            }
        }

        if (null != accountWrapper.getAccountSequence()) {
            keys.setAccSequence(format("%02d", accountWrapper.getAccountSequence()));
        }

        keys.setDescription(accountWrapper.getDescription());
        return keys;
    }

    public void fillWrapperFields(ManualAccountWrapper accountWrapper, GLAccount account) {
        accountWrapper.setId(account.getId());
        accountWrapper.setBsaAcid(account.getBsaAcid());
        accountWrapper.setFilial(account.getFilial());
        accountWrapper.setCompanyCode(account.getCompanyCode());
        accountWrapper.setCbCustomerType(account.getCbCustomerType());
        accountWrapper.setBalanceAccount2(account.getBalanceAccount2());
        accountWrapper.setPlCode(account.getPlCode());

        accountWrapper.setAccountCode(account.getAccountCode());
        accountWrapper.setAccountSequence(account.getAccountSequence());
        accountWrapper.setAcid(account.getAcid());

        accountWrapper.setDescription(account.getDescription());
        accountWrapper.setPassiveActive(account.getPassiveActive());
    }

    /**
     * Заполняет по запросу открытия счета обертку ручного открытия счета
     * Может сразу заполнять ключи ?
     *
     * @param request
     * @return
     */
    public AccountKeys createRequestAccountKeys(GLAccountRequest request, Date dateOpen) {
        ManualAccountWrapper wrapper = new ManualAccountWrapper();
        String branch = glAccountRepository.getBranchByFlex(request.getBranchFlex());
        if (isEmpty(branch)) {
            throw new ValidationError(BRANCH_FLEX_NOT_FOUND, "", request.getBranchFlex(), request.getColumnName("branchFlex"));
        }
        wrapper.setBranch(branch);
        BankCurrency currency = bankCurrencyRepository.refreshCurrency(request.getCurrency());
        if (null == currency) {
            throw new ValidationError(CURRENCY_CODE_IS_EMPTY, "",
                    glAccountRepository.getRequestCurrency(request), request.getColumnName("currency"));
        }
        wrapper.setCurrency(currency.getCurrencyCode());
        wrapper.setCustomerNumber(request.getCustomerNumber());
        try {
            wrapper.setAccountType(Long.parseLong(trimstr(request.getAccountType())));
        } catch (NumberFormatException e) {
            throw new ValidationError(ACCOUNT_TYPE_IS_NOT_NUMBER, "", request.getAccountType(), request.getColumnName("accountType"));
        }
        try {
            wrapper.setCbCustomerType(isEmpty(request.getCbCustomerType()) ? null : Short.parseShort(trimstr(request.getCbCustomerType())));
        } catch (NumberFormatException e) {
            throw new ValidationError(CUST_TYPE_IS_NOT_NUMBER, "", request.getCbCustomerType(), request.getColumnName("cbCustomerType"));
        }
        try {
            wrapper.setTerm(isEmpty(request.getTerm()) ? null : Short.parseShort(trimstr(request.getTerm())));
        } catch (NumberFormatException e) {
            throw new ValidationError(TERM_IS_NOT_NUMBER, "", request.getTerm(), request.getColumnName("term"));
        }
        wrapper.setDealSource(request.getDealSource());
        wrapper.setDealId(request.getDealId());
        wrapper.setSubDealId(request.getSubDealId());

        return createWrapperAccountKeys(wrapper, dateOpen);
    }

    @Lock(LockType.READ)
    public GLAccount findTechnicalAccount(AccountingType accountingType, String glccy, String cbccn) {
        return glAccountRepository.findTechnicalAccount(accountingType, glccy, cbccn);
    }

    @Lock(LockType.READ)
    public GLAccount findTechnicalAccountTH(AccountingType accountingType, String glccy, String cbccn) {
        GLAccount account = glAccountRepository.findTechnicalAccountTH(accountingType, glccy, cbccn,operdayController.getOperday().getCurrentDate());

        return account;
    }

    @Lock(LockType.READ)
    public GLAccount findOrReopenTechnicalAccountTH(AccountingType accountingType, String glccy, String cbccn) {
        GLAccount account = glAccountRepository.findTechnicalAccountTH(accountingType, glccy, cbccn,operdayController.getOperday().getCurrentDate());

        return glAccountRepository.reopenAccountTH(account);
    }

    /**
     * Открытие технического счета без привязки к счету Майдас.
     * Используется при автоматическом открытии счета при обработке ошибок по клиентским счетам.
     * @param accountingType
     * @param glccy
     * @param cbccn
     * @param dateOpen
     * @return
     * @throws Exception
     */
    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public GLAccount createTechnicalAccount(AccountingType accountingType, String glccy, String cbccn, Date dateOpen) throws Exception {
        return synchronizer.callSynchronously(monitor, () -> {
            return Optional.ofNullable(findTechnicalAccount(accountingType, glccy, cbccn)).orElseGet(() -> {

                try {
                    ActParm actParm = Optional.ofNullable(actParmRepository.findTechnicalActparm(accountingType))
                            .orElseThrow(() -> new DefaultApplicationException(format("Не найден gl_actparm по acctype '%s'", accountingType.getId())));

                    ErrorList descriptors = new ErrorList();

                    DataRecord headData = Optional.ofNullable(operationRepository.getHeadDataByCCode(cbccn))
                            .orElseThrow(() -> new DefaultApplicationException(format("Не найден IMBCBBRP по cbccn '%s'", cbccn)));
                    AccountKeys keys = AccountKeysBuilder.create()
                            .withBranch(headData.getString("branch"))
                            .withCurrency(glccy)
                            .withCustomerNumber(headData.getString("custno"))
                            .withAccountType(accountingType.getId())
                            .withCompanyCode(cbccn)
                            .withAcc2(actParm.getId().getAcc2())
                            .withDealSource("BARSGL")
                            .withAccountMidas("")
                            .build();
                    List<ValidationError> errors = glAccountProcessor.validate(keys, new ValidationContext());
                    if (!errors.isEmpty()) {
                        throw new DefaultApplicationException(glAccountProcessor.validationErrorMessage(GLOperation.OperSide.N, errors, descriptors));
                    }
                    // сгенерировать номер счета ЦБ
                    String bsaacid = getPureAccountNumber(GLOperation.OperSide.N, dateOpen, keys);
                    GLAccount account = createAccount(bsaacid, null, N, dateOpen, keys, GLAccount.OpenType.AENEW);
                    // меняем RLNTYPE
                    account.setRelationType(E);
                    accRlnRepository.updateRelationType(new AccRlnId("", account.getBsaAcid()), E);
                    return glAccountRepository.update(account);
                } catch (SQLException e) {
                    throw new DefaultApplicationException(e.getMessage(), e);
                }

            });
        });
    }

    private boolean isKPlusTP(AccountKeys keys) {
        return !isEmpty(keys.getDealSource()) && keys.getDealSource().equalsIgnoreCase("K+TP");
    }

    private void warnForParameterize(Error error) {
        auditController.warning(AuditRecord.LogCode.Account, "Предупреждение для параметризации АЕ", null, error);
    }

    public static Date DAY20290101;

    static {
        SimpleDateFormat sf = new SimpleDateFormat("dd.MM.yyyy");
        try {
            DAY20290101 = new Date(sf.parse("01.01.2029").getTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Lock(LockType.READ)
    public String findBsaAcid_for_XX(String acid, AccountKeys keys, String operside) throws SQLException {
        final Date operday = operdayController.getOperday().getCurrentDate();
        final List<DataRecord> glacc = glAccountRepository.findByAcidRlntype04(requiredNotEmpty(acid, ""), operday);
        if (1 == glacc.size()) {
            return glacc.get(0).getString("BSAACID");
//            return glAccountRepository.findById(GLAccount.class, glacc.get(0).getLong("ID"));
        }else if (1 < glacc.size()){
            String keyAcctype = defaultString(keys.getAccountType());
            final List<DataRecord> filtered = glacc.stream().filter(
                    r -> r.getString("ACCTYPE").equals(keyAcctype)).collect(toList());
            if (filtered.size() == 1){
                return filtered.get(0).getString("BSAACID");
//                return glAccountRepository.findById(GLAccount.class,filtered.get(0).getLong("ID"));
            }else if (filtered.size() > 1){
                throw new ValidationError(GL_SEQ_XX_GL_ACC_NOT_FOUND, operside
                        , defaultString(keys.getAccountType())
                        , defaultString(keys.getCustomerNumber())
                        , defaultString(keys.getAccountCode())
                        , defaultString(keys.getAccSequence())
                        , defaultString(keys.getDealId())
                        , defaultString(keys.getPlCode())
                        , defaultString(keys.getGlSequence())
                        , acid);
            }
        }else{
            final List<DataRecord> accrln = accRlnRepository.findByAcid_Rlntype0(acid, operday);
            if (accrln.size() == 1) return accrln.get(0).getString("BSAACID");
//            if (accrln.size() == 1) return glAccountRepository.findById(GLAccount.class,accrln.get(0).getLong("ID"));
            else if (accrln.size() > 1){
                throw new ValidationError(GL_SEQ_XX_ACCRLN_NOT_FOUND, operside
                        , defaultString(keys.getAccountType())
                        , defaultString(keys.getCustomerNumber())
                        , defaultString(keys.getAccountCode())
                        , defaultString(keys.getAccSequence())
                        , defaultString(keys.getDealId())
                        , defaultString(keys.getPlCode())
                        , defaultString(keys.getGlSequence())
                        , acid );
            }
        }
        return null;
    }

    @Lock(LockType.READ)
    public String findForPlcodeNo7903(AccountKeys keys, Date dateOpen, Date dateStart446P) {
        return accRlnRepository.findForPlcodeNo7903(keys, dateOpen, dateStart446P);
    }

    /**
     * Поиск/открытие счетов доходов/расходов
     * Ищем не наш счет, иначе создаем в т.ч. у нас
     */
    @Lock(LockType.WRITE)
    public String processNotOwnPLAccount(GLOperation operation, GLOperation.OperSide operSide, AccountKeys keys, Date dateOpen, Date dateStart446P) throws Exception {
        return synchronizer.callSynchronously(monitor, () -> {
            try {
                String bsaasid = findForPlcodeNo7903(keys, dateOpen, dateStart446P);
                if (bsaasid == null) {
                    if (!glAccountRepository.checkMidasAccountExists(keys.getAccountMidas(), dateOpen)) {
                        //todo создать запись в АСС
                        //                throw new ValidationError(ACCOUNT_MIDAS_NOT_FOUND, keys.getAccountMidas());
                        GLAccount glAccount = new GLAccount();
                        glAccount.setAcid(keys.getAccountMidas());
                        glAccount.setBranch(keys.getBranch());
                        glAccount.setCustomerNumberD(Integer.parseInt(keys.getCustomerNumber()));
                        BankCurrency bankCurrency = new BankCurrency(keys.getCurrency());
                        bankCurrency.setDigitalCode(keys.getCurrencyDigital());
                        glAccount.setCurrency(bankCurrency);
                        glAccount.setAccountCode(Short.parseShort(keys.getAccountCode()));
                        glAccount.setAccountSequence(Short.parseShort(keys.getAccSequence()));
                        glAccount.setDateOpen(dateOpen);
                        glAccount.setDateClose(Date.from(LocalDate.of(2029, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
                        glAccount.setDescription("");
                        accRepository.createAcc(glAccount);
                    }

                    bsaasid = createPlAccount(keys, dateOpen, dateStart446P, operation, operSide);
                    auditController.info(Account, format("Создан счет '%s' для операции '%d' %s",
                            bsaasid, operation.getId(), operSide.getMsgName()), operation);
                }
                return bsaasid;
            } catch (Exception e) {
                throw new DefaultApplicationException(e.getMessage(), e);
            }
        });
    }
    private int getAccountIterateCount() {
        try {
            return propertiesRepository.getNumber("account.iterate.count").intValue();
        } catch (ExecutionException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

}
