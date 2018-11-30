package ru.rbt.barsgl.ejb.integr.acc;

import org.apache.log4j.Logger;
import ru.rb.ucb.util.AccountUtil;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.acc.*;
import ru.rbt.barsgl.ejb.entity.dict.AccType.ActParm;
import ru.rbt.barsgl.ejb.entity.dict.AccountingType;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.dict.GLRelationAccountingType;
import ru.rbt.barsgl.ejb.entity.dict.GLRelationAccountingTypeId;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.acc.wrap.GLAccountCreated;
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
import static ru.rbt.barsgl.ejb.entity.acc.GLAccount.RelationType.*;
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

    private static final ThreadLocal<GLAccountCreated> isNewAccount = new ThreadLocal<>();

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
    GLAccountFrontPartController glAccountFrontPartController;

    @Inject
    private BankCurrencyRepository bankCurrencyRepository;

    @Inject
    private OperdayController operdayController;

    @EJB
    private AuditController auditController;

    @Inject
    private ExcacRlnRepository excacRlnRepository;

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
    private PdRepository pdRepository;

    @Inject
    private AccountCreateSynchronizer synchronizer;

    @EJB
    private PropertiesRepository propertiesRepository;

    @Lock(LockType.READ)
    public GLAccount findGLAccount(String bsaAcid) {
        return glAccountRepository.findGLAccount(bsaAcid);
    }

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
    public GLAccount findGLAccountMnl(AccountKeys keys, Date dateOpen) {
        Date dtOpen = (null != dateOpen) ? dateOpen : operdayController.getOperday().getCurrentDate();
        return findGLAccountMnlnoLock(keys, dtOpen);
    }

    @Lock(LockType.READ)
    public GLAccount findGLAccountMnl(AccountKeys keys) {
        return findGLAccountMnlnoLock(keys);
    }

    private GLAccount findGLAccountMnlnoLock(AccountKeys keys) {
        return findGLAccountMnlnoLock(keys, operdayController.getOperday().getCurrentDate());
    }

    private GLAccount findGLAccountMnlnoLock(AccountKeys keys, Date dateOpen) {
        return glAccountRepository.findGLAccountMnl(
                keys.getBranch(), keys.getCurrency(), keys.getCustomerNumber(),
                keys.getAccountType(), keys.getCustomerType(), keys.getTerm(),
                keys.getDealSource(), keys.getDealId(), keys.getSubDealId(),
                dateOpen);
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

    @Lock(LockType.READ)
    public GLAccount createGLAccountAE(GLOperation operation, GLOperation.OperSide operSide, Date dateOpen,
                                       AccountKeys keys, ErrorList descriptors) throws Exception {
        SyncKey key = SyncKey.builder().fromAccountKeys(keys).build();
        return synchronizer.callSynchronously(key, 15*60,
                () -> Optional.ofNullable(findGLAccountAEnoLock(keys, operSide)).orElseGet(() -> {
            try {
                return glAccountRepository.executeInNewTransaction(persistence -> {
                    // сгенерировать номер счета ЦБ
                    String bsaAcid = getAccountNumber(operSide, dateOpen, keys);
                    // создать счет с этим номером в GL и BARS
                    return createAccount(bsaAcid, operation, operSide, dateOpen, keys, FOUR, GLAccount.OpenType.AENEW);
                });
            } catch (Exception e) {
                throw new DefaultApplicationException(e.getMessage(), e);
            }
        }));
    }

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void insertIbcb(String brcaFrom, String brcaTo, String glccy, String cb1, String cb2,
                             String cb305, String cb306)throws Exception{
        String s =  synchronizer.callSynchronously(monitor, () -> {
            if (!glAccountRepository.existIbcb(cb1, cb2, glccy )){
                glAccountRepository.executeNativeUpdate("insert into ibcb (ibbrnm, ibcbrn, ibccy, ibacou, ibacin, iba305, iba306) values (?,?,?,?,?,?,?)",
                        brcaFrom, brcaTo, glccy, cb1, cb2, cb305, cb306);
            }
            return "";
        });
    }

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String createGLAccountMF(String bsaAcid, String psav, Date currDate, String glccy)throws Exception{
        return synchronizer.callSynchronously(monitor, () -> {
            return Optional.ofNullable(findGLAccount(bsaAcid)).orElseGet(()->{
                String vCcode = "00" + bsaAcid.substring(11,13);
                String[] bic_branch = glAccountRepository.getIMBCBBRP_bic_branch(vCcode);

                GLAccount glAccount = new GLAccount();
                glAccount.setBsaAcid(bsaAcid);
                glAccount.setAcid(" ");
                glAccount.setPassiveActive(psav);
                glAccount.setFilial(glAccountRepository.getCBCC(vCcode));
                glAccount.setCompanyCode(vCcode);
                glAccount.setBranch(bic_branch[1]);
                glAccount.setCurrency(bankCurrencyRepository.getCurrency(glccy));
                glAccount.setCustomerNumber(bic_branch[0]);
                glAccount.setAccountType(0);
                glAccount.setCbCustomerType((short)0);
                glAccount.setBalanceAccount2(bsaAcid.substring(0, 5));
                glAccount.setDateOpen(currDate);
                glAccount.setDateModify(currDate);
                glAccount.setDateRegister(currDate);
                glAccount.setOpenType("BARSGL");
                glAccount.setRelationType(GLAccount.RelationType.T);
                glAccount.setTransactSrc("000");

                glAccountRepository.save(glAccount);
                return glAccount;
               }
            ).getBsaAcid();
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
            return createAccount(bsaAcid, operation, operSide, dateOpen, keys, FOUR, GLAccount.OpenType.AENEW).getBsaAcid();
        });
    }

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public GLAccount createGLAccountNotify(String bsaAcid, Date dateOpen, AccountKeys keys) throws Exception {
        return synchronizer.callSynchronously(monitor, () -> {
            final GLAccount glAccount = findGLAccount(bsaAcid);     // TODO с учетом даты!
            if (null != glAccount) {
                return glAccount;
            }
            return glAccountRepository.executeInNewTransaction(persistence -> {
                // создать счет с этим номером в GL и BARS
                GLAccount newAccount = createAccount(bsaAcid, null, GLOperation.OperSide.N, dateOpen, keys, ZERO, GLAccount.OpenType.NOTIF, false);
                return newAccount;
            });
        });
    }

    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void closeGLAccountNotify(String bsaAcid, Date dateClose) throws Exception {
        int count = glAccountRepository.updGlAccCloseDate(bsaAcid, dateClose);
        if (0 == count) {
            auditController.warning(Account, format("Не прошло обновление даты закрытия счета. Счет '%s', дата закрытия '%s'", bsaAcid, dateUtils.onlyDateString(dateClose)));
        }
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
/*
    @Lock(LockType.READ)
    public GLAccount findGLAccountWithKeys(GLOperation operation, GLOperation.OperSide operSide) {
        return glAccountRepository.findGLAccount(getGlAccountNumberWithKeys(operation, operSide));
    }
*/

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


    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public GLAccount createGLAccountMnl(AccountKeys keys, Date dateOpen, ErrorList descriptors, GLAccount.OpenType openType) throws Exception {
        return internalCreateGLAccountMnl(keys, FOUR, descriptors, dateOpen, openType);
    }

    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public GLAccount createGLAccountMnlInRequiredTrans(AccountKeys keys, GLAccount.RelationType rlnType, Date dateOpen, ErrorList descriptors, GLAccount.OpenType openType) throws Exception {
        return internalCreateGLAccountMnl(keys, rlnType, descriptors, dateOpen, openType);
    }

    private GLAccount internalCreateGLAccountMnl(AccountKeys keys, GLAccount.RelationType rlnType, ErrorList descriptors, Date dateOpen, GLAccount.OpenType openType) throws Exception {
        SyncKey key = SyncKey.builder().fromAccountKeys(keys).build();
        return synchronizer.callSynchronously(key, 15 * 60, () -> Optional.ofNullable(findGLAccountMnlnoLock(keys, dateOpen)).orElseGet(() -> {
                    try {
                        return pdRepository.executeInNewTransaction(persistence -> {
                            List<ValidationError> errors = glAccountProcessor.validate(keys, new ValidationContext());
                            if (!errors.isEmpty()) {
                                throw new DefaultApplicationException(glAccountProcessor.validationErrorMessage(GLOperation.OperSide.N, errors, descriptors));
                            }
                            // Убрала проверку dealId - не надо для клиентских счетов и счетов доходов-расходов
                            //        glAccountProcessor.checkDealId(dateOpen, keys.getDealSource(), keys.getDealId(), keys.getSubDealId());

                            // сгенерировать номер счета ЦБ
                            String bsaAcid = getAccountNumber(GLOperation.OperSide.N, dateOpen, keys);
                            // создать счет с этим номером в GL и BARS
                            GLAccount glAccount = createAccount(bsaAcid, null, GLOperation.OperSide.N, dateOpen, keys, rlnType, openType);
                            isNewAccount.set(new GLAccountCreated(glAccount, true));
                            return glAccount;
                        });
                    } catch (Throwable t) {
                        throw new DefaultApplicationException(t.getMessage(), t);
                    }
                }
        ));
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

/*
    // перенесено в GLAccountProcessorTech
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
*/

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
    public GLAccount closeGLAccountDeals(GLAccount glAccount, Date dateClose, GLAccount.CloseType closeType) throws Exception {
        return synchronizer.callSynchronously(monitor, () -> {
            if (!closeType.equals(GLAccount.CloseType.Normal)){
                glAccount.setOpenType(GLAccount.OpenType.ERR.name());
            }
            glAccount.setDateClose(dateClose);
            return glAccountRepository.update(glAccount);
        });
    }

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public GLAccount updateGLAccountOpenType(GLAccount glAccount, GLAccount.OpenType openType) throws Exception {
        return synchronizer.callSynchronously(monitor, () -> {
            glAccount.setOpenType(openType.name());
            return glAccountRepository.update(glAccount);
        });
    }

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public GLAccount closeGLAccountMnl(GLAccount glAccount, Date dateClose,
                                       ErrorList descriptors) throws Exception {
//        return glAccountRepository.executeInNewTransaction(persistence -> {
        return synchronizer.callSynchronously(monitor, () -> {
            glAccount.setDateClose(dateClose);
            return glAccountRepository.update(glAccount);
        });
    }

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public GLAccount closeGLAccountMnlTech(GLAccount glAccount, Date dateClose,
                                       ErrorList descriptors) throws Exception {
        return synchronizer.callSynchronously(monitor, () -> {
            glAccount.setDateClose(dateClose);
            return glAccountRepository.update(glAccount);
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
    public String createGLPLAccount(final AccountKeys keys, GLAccount.RelationType rlnType, final GLOperation operation, GLOperation.OperSide operSide) throws Exception {
        return synchronizer.callSynchronously(monitor, () -> {
            return Optional.ofNullable(glAccountRepository.findGLPLAccount(keys.getCurrency()
                    , keys.getCustomerNumber(), keys.getAccountType(), keys.getCustomerType()
                    , keys.getTerm(), keys.getPlCode(), keys.getCompanyCode(), rlnType, operation.getValueDate()))
                    .orElseGet(() ->
                    {
                        try {
                            final String bsaacid = glAccountRepository
                                    .executeInNonTransaction(connection -> AccountUtil.generateAccountNumber(connection
                                            , keys.getAccount2(), keys.getCurrencyDigital(), keys.getCompanyCode(), keys.getPlCode()));
                            final GLAccount account = createAccount(bsaacid, operation, operSide, operation.getValueDate(), keys, rlnType, GLAccount.OpenType.AENEW);
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

    @Lock(LockType.READ)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public GLAccount createGLPLAccountMnl(final AccountKeys keys, GLAccount.RelationType rlnType, Date dateOpen, ErrorList descriptors, GLAccount.OpenType openType) throws Exception {
        return synchronizer.callSynchronously(monitor, () -> Optional.ofNullable(findGLPLAccountMnlnoLock(keys, dateOpen)).orElseGet(() -> {
            try {
                // счет создается вручную
                List<ValidationError> errors = plAccountProcessor.validate(keys, new ValidationContext());
                if (!errors.isEmpty()) {
                    throw new DefaultApplicationException(glAccountProcessor.validationErrorMessage(GLOperation.OperSide.N, errors, descriptors));
                }
                final String bsaacid = getPlAccountNumber(keys.getAccount2(), keys.getCurrencyDigital(), keys.getCompanyCode(), keys.getPlCode());
                final GLAccount account = createAccount(bsaacid, null, N, dateOpen, keys, rlnType, openType);
                Assert.notNull(rlnType, "Не задан RLNTYPE для создания счета доходов / расходов");
                relationAccountingTypeRepository.createRelation(account.getAcid()
                        , account.getBsaAcid(), Long.toString(account.getAccountType()));

                isNewAccount.set(new GLAccountCreated(account, true));
                return account;
            } catch (Exception e) {
                throw new DefaultApplicationException(e.getMessage(), e);
            }
        }));
    }

    private String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, DefaultApplicationException.class);
    }

    private GLAccount createAccount(String bsaAcid, GLOperation operation, GLOperation.OperSide operSide, Date dateOpen,
                                    AccountKeys keys, GLAccount.RelationType rlnType, GLAccount.OpenType openType) {
        return createAccount(bsaAcid, operation, operSide, dateOpen, keys, rlnType, openType, true);
    }

    private GLAccount createAccount(String bsaAcid, GLOperation operation, GLOperation.OperSide operSide, Date dateOpen,
                                    AccountKeys keys, GLAccount.RelationType rlnType, GLAccount.OpenType openType, boolean checkCustomer) {

        if (checkCustomer) {
            String customerName = glAccountRepository.getCustomerName(keys.getCustomerNumber());
            if (StringUtils.isEmpty(customerName)) {
                throw new ValidationError(CUSTOMER_NUMBER_NOT_FOUND,
                        bsaAcid, keys.getCustomerNumber(), Long.toString(AccountKeys.getiCustomerNumber()), "номер клиента");
            }
        }

        // создать счет GL
        GLAccount glAccount = glAccountProcessor.createGlAccount(bsaAcid, operation, operSide, dateOpen, keys, rlnType, openType);
        // определить дополнительные папаметры счета
        glAccountProcessor.enrichment(glAccount, keys);
        // сохранить счет GL
        return glAccountRepository.save(glAccount);
    }

    @Lock(LockType.WRITE)
    public GLAccount createAccountTH(String bsaAcid, GLOperation operation, GLOperation.OperSide operSide, Date dateOpen,
                                    AccountKeys keys, GLAccount.OpenType openType) {
        // создать счет GL
        GLAccount glAccount = glAccountProcessor.createGlAccountTH(bsaAcid, operation, operSide, dateOpen, keys, openType);
        return glAccountRepository.save(glAccount);
    }

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public GLAccParam createAccountsExDiff(GLOperation operation, GLOperation.OperSide operSide, AccountKeys keys, BankCurrency exchCurrency, Date dateOpen, String optype) {
        GLAccParam accountId = excacRlnRepository.findForPlcode7903(keys, exchCurrency, optype);
        if (accountId == null) {
            fillExDiffAccountKeys(operSide, keys, exchCurrency, optype);
            accountId = calculateExDiffAccount(operSide, keys , exchCurrency, optype);

            GLAccount glAccount = createAccount(accountId.getBsaAcid(), operation, operSide, dateOpen, keys, TWO, GLAccount.OpenType.BARSGL);
            GlExcacRln excacRln = excacRlnRepository.createExcacRln(glAccount, exchCurrency.getCurrencyCode(), optype);

            auditController.info(Account, format("Создан счет курсовой разницы '%s' для операции '%d' %s",
                    accountId.getBsaAcid(), operation.getId(), operSide.getMsgName()), excacRln);
            accountId = new GLAccParam(excacRln.getId());
        }

        return accountId;
    }

    private AccountKeys fillExDiffAccountKeys(GLOperation.OperSide operSide, AccountKeys keys, BankCurrency bankCurrency, String optype) {
        try {
            String psav = keys.getPassiveActive();

            // параметры курсовой разницы
            keys.setExchangeCurrency(bankCurrency.getCurrencyCode());
            DataRecord excacParam = Optional.ofNullable(glAccountRepository.
                    selectFirst("select ACC2, PLCODE, ACOD, ACSQ, case when CCY = '000' then 0 else 1 end ccy000 " +
                                    "from EXCACPARM where (CCY = ? or CCY = '000') and OPTYPE = ? and PSAV = ? order by ccy000 desc",
                            bankCurrency.getCurrencyCode(), optype, psav))
                    .orElseThrow(() -> new ValidationError(ACCOUNTEX_PARAMS_NOT_FOUND, operSide.getMsgName(), bankCurrency.getCurrencyCode(), optype, psav));
            keys.setAccount2(excacParam.getString("ACC2"));
            keys.setPlCode(excacParam.getString("PLCODE"));
            keys.setAccountCode(excacParam.getString("ACOD"));
            String sq = excacParam.getString("ACSQ");
            keys.setAccSequence(sq.length() == 1 ? "0" + sq : sq);

            // AccountType
            DataRecord accTypeParam = Optional.ofNullable(glAccountRepository.
                    selectFirst("select ACCTYPE from GL_ACC2EXCH_ACCTYPE where ACC2 = ? and ACOD = ? and CASH = ?",
                            keys.getAccount2(), keys.getAccountCode(), optype))
                    .orElseThrow(() -> new ValidationError(ACCTYPE_EXDIFF_NOT_FOUND, keys.getAccount2(), keys.getAccountCode(), optype));
            String accType = accTypeParam.getString(0);
            if (isEmpty(keys.getAccountType()))
                keys.setAccountType(accType);
            else if (!accType.equals(keys.getAccountType()))
                throw new ValidationError(ACCTYPE_EXDIFF_BAD, keys.getAccountType(), accType, keys.getAccount2(), keys.getAccountCode(), optype);
            // Название счета заполнится в enrichment

            // параметры бранча
            DataRecord branchParam = Optional.ofNullable(operationRepository.getBranchParameters(keys.getBranch()))
                    .orElseThrow(() -> new ValidationError(ErrorCode.BRANCH_NOT_FOUND, operSide.getMsgName(), keys.getBranch(), ""+keys.getiBranch()));
            keys.setCustomerNumber(branchParam.getString("CNUM"));
            keys.setFilial(branchParam.getString("CBCC"));
            if (isEmpty(keys.getCompanyCode()))
                keys.setCompanyCode(branchParam.getString("CBCCN"));

            // общие параметры
            keys.setCurrency("RUR");
            keys.setCurrencyDigital("810");
            if (isEmpty(keys.getCustomerType()))
                keys.setCustomerType("0");
            keys.setTerm(null);
            keys.setGlSequence(null);
            keys.setDealSource("BARSGL");
            keys.setDealId(null);
            keys.setSubDealId(null);

            return keys;

        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    };

    private GLAccParam calculateExDiffAccount(GLOperation.OperSide operSide, AccountKeys keys, BankCurrency bankCurrency, String optype) {
            String nccy = bankCurrency.getDigitalCode();
//            String c1 = String.valueOf(nccy.charAt(0));
            String c1 = currencyFirstCharToNum(nccy.substring(0,1));
            String c2 = nccy.substring(1, 3);

            String accountMask = new StringBuilder().append(keys.getAccount2()).append("810_").append(c1).append(keys.getCompanyCode().substring(1)).append(keys.getPlCode()).append(c2).toString();
            String bsaacid = glAccountFrontPartController.calculateKeyDigit(accountMask, keys.getCompanyCode());

            // Счет Midas
            String acid = new StringBuilder().append(keys.getCustomerNumber()).append("RUR").append(keys.getAccountCode()).append(keys.getAccSequence()).append(keys.getBranch()).toString();
            keys.setAccountMidas(acid);
            return new GLAccParam(acid, bsaacid);
    }

    // ====================================================================================
    // методы по заполнению ключей перенесены в GLAccountProcessor

    /**
     * Создает уникальный номер счета по ключам счета
     *
     * @param keys - ключи счета
     * @return
     */
    public String getAccountNumber(GLOperation.OperSide operSide, Date dateOpen, AccountKeys keys) {
        // заполнить недостающие ключи счета Майдас
        glAccountProcessor.fillAccountKeysMidas(operSide, dateOpen, keys);
        return getPureAccountNumber(operSide, dateOpen, keys);
    }

    /**
     * Генерация счета ЦБ без привязки к счету Майдас
     * @return номер счета
     */
    public String getPureAccountNumber(GLOperation.OperSide operSide, Date dateOpen, AccountKeys keys) {
        // заполнить недостающие ключи счета ЦБ
        glAccountProcessor.fillAccountKeys(operSide, dateOpen, keys);

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
        String branch = glAccountRepository.getMidasBranchByFlex(request.getBranchFlex());
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
                            .withAccountCode("0")
                            .withAccSequence("0")
                            .build();
                    List<ValidationError> errors = glAccountProcessor.validate(keys, new ValidationContext());
                    if (!errors.isEmpty()) {
                        throw new DefaultApplicationException(glAccountProcessor.validationErrorMessage(GLOperation.OperSide.N, errors, descriptors));
                    }
                    // сгенерировать номер счета ЦБ
                    String bsaacid = getPureAccountNumber(GLOperation.OperSide.N, dateOpen, keys);
                    GLAccount account = createAccount(bsaacid, null, N, dateOpen, keys, E, GLAccount.OpenType.AENEW);
                    return glAccountRepository.update(account);
                } catch (SQLException e) {
                    throw new DefaultApplicationException(e.getMessage(), e);
                }

            });
        });
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
        }else if (1 < glacc.size()){
            String keyAcctype = defaultString(keys.getAccountType());
            final List<DataRecord> filtered = glacc.stream().filter(
                    r -> r.getString("ACCTYPE").equals(keyAcctype)).collect(toList());
            if (filtered.size() == 1){
                return filtered.get(0).getString("BSAACID");
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
        }
        return null;
    }

    @Lock(LockType.READ)
    public String findForPlcodeNo7903(AccountKeys keys, Date dateOpen, Date dateStart446P) {
        return glAccountRepository.findForPlcodeNo7903(keys, dateOpen, dateStart446P);
    }

    /**
     * <pre>
     * Определяет создан ли счет в текущем потоке
     * состояние сохранияется только на один вызов ru.rbt.barsgl.ejb.integr.acc.GLAccountController#createGLAccountMnl
     * при следующем вызове значение изменяется
     * после получения статуса значение обнуляется
     * Пример использования
     * </pre>
     * <pre>
     *     // Single Thread execution
     *     GLAccount glAccount = glAccountController.createGLAccountMnl(keys, dateOpen, accountWrapper.getErrorList(), GLAccount.OpenType.MNL);
     *     // true если создан в этом вызове
     *     boolean isNewAccount = glAccountController.isNewAccount(glAccount.getBsaAcid());
     *     // ..... some code is skipped
     *     // Always false!!!
     *     boolean isNewAccount2 = glAccountController.isNewAccount(glAccount.getBsaAcid());
     *
     * </pre>
     *
     * @param account для проверки соответствия номера счета
     * @return true, если счет создан в текущем потоке
     */
    public boolean isNewAccount(String account) {
        try {
            return null != isNewAccount.get()
                    && isNewAccount.get().getAccount().getBsaAcid().equals(account)
                    && isNewAccount.get().isNewAccount();
        } finally {
            isNewAccount.set(null);
        }
    }

    /**
     * Поиск/открытие счетов доходов/расходов
     * Ищем не наш счет, иначе создаем в т.ч. у нас
     */
/*
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
*/
    private int getAccountIterateCount() {
        try {
            return propertiesRepository.getNumber("account.iterate.count").intValue();
        } catch (ExecutionException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

}
