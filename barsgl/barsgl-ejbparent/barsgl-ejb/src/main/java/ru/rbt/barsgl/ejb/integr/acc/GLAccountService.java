package ru.rbt.barsgl.ejb.integr.acc;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.bankjar.Constants;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.acc.GLAccountRequest;
import ru.rbt.security.entity.access.PrmValue;
import ru.rbt.barsgl.ejb.entity.dict.AccountingType;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.bg.EtlPostingController;
import ru.rbt.barsgl.ejb.repository.*;
import ru.rbt.security.ejb.repository.access.PrmValueRepository;
import ru.rbt.security.ejb.repository.access.SecurityActionRepository;
import ru.rbt.barsgl.ejb.repository.dict.AccountingTypeRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.security.UserContext;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.barsgl.shared.ErrorList;
import ru.rbt.shared.ExceptionUtils;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.shared.enums.SecurityActionCode;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperSide.C;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperSide.D;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Account;
import static ru.rbt.ejbcore.util.StringUtils.substr;
import static ru.rbt.ejbcore.validation.ErrorCode.*;
import static ru.rbt.ejbcore.validation.ValidationError.initSource;
import static ru.rbt.shared.enums.PrmValueEnum.Source;

/**
 * Created by ER18837 on 03.08.15.
 * Сервис для работы со счетами по ключам счета
 */
@Stateless
@LocalBean
public class GLAccountService {

    private static final Logger log = Logger.getLogger(EtlPostingController.class);

    @EJB
    private GLAccountController glAccountController;

    @EJB
    private GLAccountRepository glAccountRepository;

    @Inject
    private AccRlnRepository accRlnRepository;

    @Inject
    private ExcacRlnRepository excacRlnRepository;

    @EJB
    private OperdayController operdayController;

    @EJB
    private AuditController auditController;

    @Inject
    private BankCurrencyRepository bankCurrencyRepository;

    @Inject
    private AccRepository accRepository;

    @Inject
    private BsaAccRepository bsaAccRepository;

    @EJB
    private GLAccountRequestRepository glAccountRequestRepository;

    @Inject
    private GLAccountProcessor glAccountProcessor;

    @Inject
    private PLAccountProcessor plAccountProcessor;

    @Inject
    private AccountingTypeRepository accountingTypeRepository;

    @Inject
    private PrmValueRepository prmValueRepository;

    @Inject
    private DateUtils dateUtils;

    @Inject
    private SecurityActionRepository actionRepository;

    @Inject
    private UserContext userContext;

    public String getAccount(GLOperation operation, GLOperation.OperSide operSide, AccountKeys keys) throws Exception {
        // проверяем по какой ветке идти - искать в Майдас или создать счет ...
        Date dateOpen = operation.getValueDate();
//todo вход в GlSequence = XX
        if (!isEmpty(keys.getGlSequence())                                  // совместно используемые с Майдас счета
                && keys.getGlSequence().toUpperCase().startsWith("XX")) {
//          проверки fsd открытие счетов по ключам GL_SEQ=XX
            checkDealPlcode(keys, operSide.getMsgName());
            // ищем счет Майдас
            glAccountController.fillAccountKeysMidas(operSide, dateOpen, keys);
            String bsaacidXX = glAccountController.findBsaAcid_for_XX(keys.getAccountMidas(), keys, operSide.getMsgName());
            if (bsaacidXX != null) {
                return bsaacidXX;
            } else{//создание в accrln,accrlnext(триггер),bsaacc,gl_acc
                checkNotStorno(operation, operSide);
                return glAccountController.createGLAccountXX(operation, operSide, dateOpen, keys, null);
            }
        } else if (!isEmpty(keys.getGlSequence())                           // счета ОФР
                && keys.getGlSequence().toUpperCase().startsWith("PL")) {
            return getAccountByPlcode(operation, operSide, keys, dateOpen);
        } else if (!isEmpty(keys.getGlSequence())                           // технические счета 99999, 99998
                && keys.getGlSequence().toUpperCase().startsWith("GL")) {
            // получаем счет ЦБ сразу из ACCRLN
            BankCurrency currency = bankCurrencyRepository.refreshCurrency(keys.getCurrency());
            keys.setCurrencyDigital(currency.getDigitalCode());
            return accRlnRepository.findForSequenceGL(keys);
        } else // заполнены и ключи и счет
        if(isAccountWithKeys(operation, operSide)) {
            // заполнены и ключи и счет
            glAccountController.fillAccountKeysMidas(operSide, dateOpen, keys);
            return Optional.ofNullable(glAccountController.findGLAccountWithKeys(operation, operSide)).orElseGet(() -> {
                try {
                    checkNotStorno(operation, operSide);
                    return glAccountController.findOrCreateGLAccountAEWithKeys(operation, operSide, dateOpen, keys);
                } catch (Exception e) {
                    throw new DefaultApplicationException(e);
                }
            }).getBsaAcid();
        } else {
            // поиск открытого счета по ключам
            return Optional.ofNullable(glAccountController.findGLAccountAE(keys, operSide)).orElseGet(() -> {
                try {
                    checkNotStorno(operation, operSide);
                    // поиск счета и открытие нового счета, если его нет
                    GLAccount glAccountInternal = glAccountController.createGLAccountAE(operation, operSide, dateOpen, keys, null);
                    auditController.info(Account, format("Создан счет '%s' для операции '%d' %s",
                            glAccountInternal.getBsaAcid(), operation.getId(), operSide.getMsgName()), glAccountInternal);
                    return glAccountInternal;
                } catch (Exception e) {
                    throw new DefaultApplicationException(e);
                }
            }).getBsaAcid();
        }
    }

    public void checkDealPlcode(AccountKeys keys, String operside){
        if (!isEmpty(keys.getDealId())){
            throw new ValidationError(GL_SEQ_XX_KEY_WITH_DEAL
                    , operside
                    , defaultString(keys.getAccountType())
                    , defaultString(keys.getCustomerNumber())
                    , defaultString(keys.getAccountCode())
                    , defaultString(keys.getAccSequence())
                    , defaultString(keys.getDealId())
                    , defaultString(keys.getPlCode())
                    , defaultString(keys.getGlSequence()));
        }else if  (!isEmpty(keys.getSubDealId())){
            throw new ValidationError(GL_SEQ_XX_KEY_WITH_SUBDEAL
                    , operside
                    , defaultString(keys.getAccountType())
                    , defaultString(keys.getCustomerNumber())
                    , defaultString(keys.getAccountCode())
                    , defaultString(keys.getAccSequence())
                    , defaultString(keys.getDealId())
                    , defaultString(keys.getPlCode())
                    , defaultString(keys.getSubDealId())
                    , defaultString(keys.getGlSequence()));
        }else if  (!isEmpty(keys.getPlCode())){
            throw new ValidationError(GL_SEQ_XX_KEY_WITH_PLCODE
                    , operside
                    , defaultString(keys.getAccountType())
                    , defaultString(keys.getCustomerNumber())
                    , defaultString(keys.getAccountCode())
                    , defaultString(keys.getAccSequence())
                    , defaultString(keys.getDealId())
                    , defaultString(keys.getPlCode())
                    , defaultString(keys.getGlSequence()));
        }
    }


    /**
     * Заполнены ключи и счет по проверяемой стороне
     * @param operation
     * @param operSide
     * @return
     */
    public boolean isAccountWithKeys(GLOperation operation, GLOperation.OperSide operSide) {
        return (C == operSide && !isEmpty(operation.getAccountCredit())
                        && !isEmpty(operation.getAccountKeyCredit())) ||
                (D == operSide && !isEmpty(operation.getAccountDebit())
                        && !isEmpty(operation.getAccountKeyDebit()));
    }

    private void checkNotStorno(GLOperation operation, GLOperation.OperSide operSide) {
        if (operation.isStorno())      // по Storn не надо открывать счет
            throw new ValidationError(STORNO_ACCOUNT_NOT_FOUND, operSide.getMsgName()
                    , operation.getAePostingId(), operation.getDealId());

    }

    private String getAccountByPlcode(GLOperation operation, GLOperation.OperSide operSide, AccountKeys keys, Date dateOpen) throws Exception {
        // сиквенс не задан - ищем счет Майдас

        Assert.isTrue(BankCurrency.RUB.getCurrencyCode().equals(keys.getCurrency())
                , () -> new ValidationError(ErrorCode.ACCKEY_CURRENCY_NOT_VALID, keys.getCurrency()));

        if ("7903".equals(keys.getAccountCode()) || "7904".equals(keys.getAccountCode())) {
            String optype = "N";
            keys.setPassiveActive("7904".equals(keys.getAccountCode()) ? Constants.PASIV : Constants.ACTIV);
            BankCurrency bankCurrency = (operSide.equals(operSide.D) ? operation.getCurrencyCredit() : operation.getCurrencyDebit());
            String[] acids = excacRlnRepository.findForPlcode7903(keys, bankCurrency, optype);
            if (acids == null) {
                // Создаем счет как в курсовой разнице (ExDiff)
                acids = glAccountController.createAccountsExDiff(operation, operSide, keys, dateOpen, bankCurrency, optype);
            }
            return (acids!=null && acids.length==2) ? acids[1] : throwsPLAccountNotCreated().get();
        } else {
            // Перенесено сюда, так как портит данные для других этапов
            Date dateStart446P = glAccountRepository.getDateStart446p();
            glAccountController.fillAccountOfrKeysMidas(operSide, dateOpen, keys);
            glAccountController.fillAccountOfrKeys(operSide, dateOpen, keys); // Обогащение

            if (accountingTypeRepository.findById(AccountingType.class, keys.getAccountType()).isBarsAllowed()) {
                return Optional.ofNullable(glAccountRepository
                        .findGLPLAccount(keys.getCurrency(), keys.getCustomerNumber(), keys.getAccountType()
                            , keys.getCustomerType(), keys.getTerm(), keys.getPlCode(), keys.getCompanyCode(), dateOpen))
                        .map(GLAccount::getBsaAcid).orElseGet(() -> glAccountController.createGLPLAccount(keys, operation, operSide));
            } else {
                return processNotOwnPLAccount(operation,operSide,keys,dateOpen, dateStart446P);
            }
        }
    }

    /**
     * Поиск/открытие счетов доходов/расходов
     * Ищем не наш счет, иначе создаем в т.ч. у нас
     */
    private String processNotOwnPLAccount(GLOperation operation, GLOperation.OperSide operSide, AccountKeys keys, Date dateOpen, Date dateStart446P) throws Exception {
        String bsaasid = accRlnRepository.findForPlcodeNo7903(keys, dateOpen, dateStart446P);
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

            bsaasid = glAccountController.createPlAccount(keys, dateOpen, dateStart446P, operation, operSide);
            auditController.info(Account, format("Создан счет '%s' для операции '%d' %s",
                    bsaasid, operation.getId(), operSide.getMsgName()), operation);
        }
        return bsaasid;
    }

    private void checkAccountPermission(ManualAccountWrapper wrapper, FormAction action) {
        String acc2 = wrapper.getBalanceAccount2();
        if (isEmpty(acc2))
            acc2 = wrapper.getBsaAcid();
        Long userId = wrapper.getUserId();
        if (null == userId)
            userId = userContext.getUserId();
        String acc1 = StringUtils.substr(acc2, 3);
        if (FormAction.CREATE == action) {
            switch (acc1) {
                case "707":
                    if (!actionRepository.getAvailableActions(userId).contains(SecurityActionCode.Acc707Inp))
                        throw new ValidationError(ACCOUNT707_INP_NOT_ALLOWED);
                    break;
                case "706":
                    if (!actionRepository.getAvailableActions(userId).contains(SecurityActionCode.AccOFRInp))
                        throw new ValidationError(ACCOUNT706_INP_NOT_ALLOWED);
                    break;
                default:    // для остальных при открытии будет пусто
                    if (!actionRepository.getAvailableActions(userId).contains(SecurityActionCode.AccInp))
                        throw new ValidationError(ACCOUNT_INP_NOT_ALLOWED);
                    break;
            }
        } else if (FormAction.UPDATE == action) {
            switch (acc1) {
                case "707":
                    if (!actionRepository.getAvailableActions(userId).contains(SecurityActionCode.Acc707Chng))
                        throw new ValidationError(ACCOUNT707_CHNG_NOT_ALLOWED);
                    break;
                case "706":
                    if (!actionRepository.getAvailableActions(userId).contains(SecurityActionCode.AccOFRChng))
                        throw new ValidationError(ACCOUNT706_CHNG_NOT_ALLOWED);
                    break;
                default:
                    if (!actionRepository.getAvailableActions(userId).contains(SecurityActionCode.AccChng))
                        throw new ValidationError(ACCOUNT_CHNG_NOT_ALLOWED);
                    break;
            }
        }


    }

    /**
     * Создание лицевого счета вручную
     * @param accountWrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualAccountWrapper> createManualAccount(ManualAccountWrapper accountWrapper) throws Exception {
        try {
            checkAccountPermission(accountWrapper, FormAction.CREATE);
            Date dateOpen = new SimpleDateFormat(ManualAccountWrapper.dateFormat).parse(accountWrapper.getDateOpenStr());
            AccountKeys keys = glAccountController.createWrapperAccountKeys(accountWrapper, dateOpen);
            // Такой счет уже есть
            GLAccount glAccount = null;
            if (null != (glAccount = glAccountController.findGLAccountMnl(keys))) {
                throw new ValidationError(ACCOUNTGL_ALREADY_EXISTS, glAccount.getBsaAcid(), glAccount.getAcid());
            }
            String accType = accountWrapper.getAccountType().toString();
            DataRecord data = glAccountRepository.getAccountTypeParams(accType);
            if (data.getString("FL_CTRL").equals("Y")) {
                throw new ValidationError(ACCOUNT_IS_CONTROLABLE, accType);
            }
            glAccount = glAccountController.createGLAccountMnl(keys, dateOpen, accountWrapper.getErrorList(), GLAccount.OpenType.MNL);
            auditController.info(Account, format("Создан счет '%s' по ручному вводу",
                    glAccount.getBsaAcid()), glAccount);
            glAccountController.fillWrapperFields(accountWrapper, glAccount);
            return new RpcRes_Base<>(
                    accountWrapper, false, format("Создан счет ЦБ: '%s'", accountWrapper.getBsaAcid()));
        } catch (Throwable e) {
            String errMessage = accountErrorMessage(e, accountWrapper.getErrorList(), initSource());
            auditController.error(Account, format("Ошибка при создании счета по ручному вводу для AccountType = '%s'",
                    accountWrapper.getAccountType()), null, e);
            return new RpcRes_Base<>(accountWrapper, true, errMessage);

        }
    }

    /**
     * Создание счета доходов / расходов вручную
     * @param accountWrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualAccountWrapper> createManualPlAccount(ManualAccountWrapper accountWrapper) throws Exception {
        try {
            return glAccountRepository.executeInNewTransaction(persistence -> {
                checkAccountPermission(accountWrapper, FormAction.CREATE);
                Date dateOpen = new SimpleDateFormat(ManualAccountWrapper.dateFormat).parse(accountWrapper.getDateOpenStr());
                AccountKeys keys = glAccountController.createWrapperAccountKeys(accountWrapper, dateOpen);
                // поиск в GL_ACC
                GLAccount glAccount = null;
                if (null != (glAccount = glAccountController.findGLPLAccountMnl(keys))) {
                    throw new ValidationError(ACCOUNTGL_ALREADY_EXISTS, glAccount.getBsaAcid(), glAccount.getAcid());
                }
/*
                // для 707 проверяем дату открытия
                if (keys.getAccount2().substring(0,3).equals("707")) {
                    Date lastSpod = glAccountRepository.getLastSpodDate(operdayController.getOperday().getCurrentDate());   // TODO dateOpen ?
                    if (dateOpen.after(lastSpod)) {
                        throw new ValidationError(ACCOUNT_707_AFTER_SPOD, dateUtils.onlyDateString(lastSpod));
                    }
                }
*/

                plAccountProcessor.fillAccountKeysMidas(accountWrapper, keys);
                // проверяем признак PL_ACT
                DataRecord data = glAccountRepository.getAccountTypeParams(accountWrapper.getAccountType().toString());
                if (data.getString("PL_ACT").equals("Y")) {
                    keys.setRelationType("5");
                } else {
                    // поиск в ACCRLN
                    data = glAccountRepository.getAccountForPl(keys.getAccountMidas(),
                            accountWrapper.getCbCustomerType(), accountWrapper.getPlCode(), accountWrapper.getBalanceAccount2());
                    if (null != data ) {
                        throw new ValidationError(ACCOUNT_PL_ALREADY_EXISTS, data.getString(0), data.getString(1), keys.getAccountType());
                    }
                    keys.setRelationType("2");
                }
                if (keys.getAccount2().startsWith("707")) {
                    keys.setRelationType("4");
                }

                BankCurrency currency = bankCurrencyRepository.refreshCurrency(keys.getCurrency());
                keys.setCurrencyDigital(currency.getDigitalCode());
                keys.setFilial(glAccountRepository.getFilialByCompanyCode(keys.getCompanyCode()));
                glAccount = glAccountController.createGLPLAccountMnl(keys, dateOpen, accountWrapper.getErrorList(), GLAccount.OpenType.MNL);
                auditController.info(Account, format("Создан счет ОФР '%s' по ручному вводу",
                        glAccount.getBsaAcid()), glAccount);
                glAccountController.fillWrapperFields(accountWrapper, glAccount);
                return new RpcRes_Base<>(
                        accountWrapper, false, format("Создан счет ЦБ: '%s'", accountWrapper.getBsaAcid()));
            });
        } catch (Throwable e) {
            String errMessage = accountErrorMessage(e, accountWrapper.getErrorList(), initSource());
            auditController.error(Account, format("Ошибка при создании счета по ручному вводу для AccountType = '%s'",
                    accountWrapper.getAccountType()), null, e);
            return new RpcRes_Base<>(accountWrapper, true, errMessage);

        }
    }

    /**
     * Создание счета при загрузке картотеки
     * @param accountWrapper
     * @return
     * @throws Exception
     */
    public String createBsaacidAccount(ManualAccountWrapper accountWrapper) throws Exception {
        try {
            Date dateOpen = new SimpleDateFormat(ManualAccountWrapper.dateFormat).parse(accountWrapper.getDateOpenStr());
            AccountKeys keys = glAccountController.createWrapperAccountKeys(accountWrapper, dateOpen);
            // Такой счет уже есть
            GLAccount glAccount = glAccountController.findGLAccountMnl(keys);
            if (glAccount != null) return glAccount.getBsaAcid();

            glAccount = glAccountController.createGLAccountMnl(keys, dateOpen, accountWrapper.getErrorList(), GLAccount.OpenType.MNL);
            auditController.info(Account, format("Создан счет '%s' по ручному вводу",
                    glAccount.getBsaAcid()), glAccount);
            glAccountController.fillWrapperFields(accountWrapper, glAccount);
            return accountWrapper.getBsaAcid();
//            return new RpcRes_Base<>(
//                    accountWrapper, false, format("Создан счет ЦБ: '%s'", accountWrapper.getBsaAcid()));
        } catch (Throwable e) {
//            String errMessage = accountErrorMessage(e, accountWrapper.getErrorList(), initSource());
            auditController.error(Account, format("Ошибка при создании счета по ручному вводу для AccountType = '%s'",
                    accountWrapper.getAccountType()), null, e);
            return null;
        }
    }

    public RpcRes_Base<ManualAccountWrapper> updateManualAccount(ManualAccountWrapper accountWrapper) throws Exception {

        try {
            GLAccount account = glAccountRepository.findById(GLAccount.class, accountWrapper.getId());
            if (null == account) {      // Такого счета нет!
                throw new ValidationError(ACCOUNT_NOT_FOUND, accountWrapper.getBsaAcid(), "Счет ЦБ");
            }
            checkAccountPermission(accountWrapper, FormAction.UPDATE);
            String dateOpenStr = accountWrapper.getDateOpenStr();
            Date dateOpen = dateOpenStr == null ? null : new SimpleDateFormat(ManualAccountWrapper.dateFormat).parse(dateOpenStr);
            String dateCloseStr = accountWrapper.getDateCloseStr();
            Date dateClose = dateCloseStr == null ? null : new SimpleDateFormat(ManualAccountWrapper.dateFormat).parse(dateCloseStr);
            String act = (null == account.getDateClose() && null != dateClose) ? "Закрыт" : "Изменен";
            AccountKeys keys = glAccountController.createWrapperAccountKeys(accountWrapper, dateOpen);
            GLAccount glAccount = glAccountController.updateGLAccountMnl(account, dateOpen, dateClose, keys, accountWrapper.getErrorList());
            auditController.info(Account, format("%s счет '%s' по ручному вводу",
                    act, glAccount.getBsaAcid()), glAccount);
            glAccountController.fillWrapperFields(accountWrapper, glAccount);
            return new RpcRes_Base<>(
                    accountWrapper, false, format("%s счет ЦБ: '%s'", act, accountWrapper.getBsaAcid()));
        } catch (Throwable e) {
            String errMessage = accountErrorMessage(e, accountWrapper.getErrorList(), initSource());
            auditController.error(Account, format("Ошибка при изменении счета по ручному вводу: '%s'",
                    accountWrapper.getBsaAcid()), null, e);
            return new RpcRes_Base<ManualAccountWrapper>(
                    accountWrapper, true, errMessage);
        }
    }

    public RpcRes_Base<ManualAccountWrapper> closeManualAccount(ManualAccountWrapper accountWrapper) throws Exception {

        try {
            return glAccountRepository.executeInNewTransaction(persistence -> {
                List<PrmValue> prm = prmValueRepository.select(PrmValue.class,
                        "from PrmValue p where p.userId = ?1 and p.prmCode = ?2",
                        accountWrapper.getUserId(), Source);

                if (prm == null || prm.isEmpty() || prm.stream().filter(x -> x.getPrmValue().equals(accountWrapper.getDealSource()) ||
                        x.getPrmValue().equals("*")).count() == 0){
                    return new RpcRes_Base<ManualAccountWrapper>(
                            accountWrapper, true, format( "У Вас нет достаточных прав производить операции над счетом с источником сделки: %s", accountWrapper.getDealSource()));
                }

                GLAccount account = glAccountController.findGLAccount(accountWrapper.getBsaAcid());
                if (null == account) {      // Такого счета нет!
                    throw new ValidationError(ACCOUNT_NOT_FOUND, accountWrapper.getBsaAcid(), "Счет ЦБ");
                }
                String dateCloseStr = accountWrapper.getDateCloseStr();
                Date dateClose = dateCloseStr == null ? null : new SimpleDateFormat(ManualAccountWrapper.dateFormat).parse(dateCloseStr);
                String act = (null == account.getDateClose() && null != dateClose) ? "Закрыт счет" : "Отменено закрытие счета";
                GLAccount glAccount = glAccountController.closeGLAccountMnl(account, dateClose, accountWrapper.getErrorList());
                auditController.info(Account, format("%s '%s' по ручному вводу",
                        act, glAccount.getBsaAcid()), glAccount);
                glAccountController.fillWrapperFields(accountWrapper, glAccount);
                return new RpcRes_Base<>(
                        accountWrapper, false, format("%s ЦБ: '%s'", act, accountWrapper.getBsaAcid()));

            });
        } catch (Exception e) {
            String errMessage = accountErrorMessage(e, accountWrapper.getErrorList(), initSource());
            auditController.error(Account, format("Ошибка при закрытии счета по ручному вводу: '%s'",
                    accountWrapper.getBsaAcid()), null, e);
            return new RpcRes_Base<ManualAccountWrapper>(
                    accountWrapper, true, errMessage);
        }
/*        try {
            List<PrmValue> prm = prmValueRepository.select(PrmValue.class,
                    "from PrmValue p where p.userId = ?1 and p.prmCode = ?2",
                    accountWrapper.getUserId(), Source);

            if (prm == null || prm.isEmpty() || prm.stream().filter(x -> x.getPrmValue().equals(accountWrapper.getDealSource()) ||
                    x.getPrmValue().equals("*")).count() == 0){
                return new RpcRes_Base<ManualAccountWrapper>(
                        accountWrapper, true, format( "У Вас нет достаточных прав производить операции над счетом с источником сделки: %s", accountWrapper.getDealSource()));
            }

            GLAccount account = glAccountController.findGLAccount(accountWrapper.getBsaAcid());
            if (null == account) {      // Такого счета нет!
                throw new ValidationError(ACCOUNT_NOT_FOUND, accountWrapper.getBsaAcid(), "Счет ЦБ");
            }
            String dateCloseStr = accountWrapper.getDateCloseStr();
            Date dateClose = dateCloseStr == null ? null : new SimpleDateFormat(ManualAccountWrapper.dateFormat).parse(dateCloseStr);
            GLAccount glAccount = glAccountController.closeGLAccountMnl(account, dateClose, accountWrapper.getErrorList());
            auditController.info(Account, format("Изменен счет '%s'",
                    glAccount.getBsaAcid()), glAccount);
            glAccountController.fillWrapperFields(accountWrapper, glAccount);
            return new RpcRes_Base<>(
                    accountWrapper, false, format("Изменен счет ЦБ: '%s'", accountWrapper.getBsaAcid()));
        } catch (Exception e) {
            String errMessage = accountErrorMessage(e, accountWrapper.getErrorList(), initSource());
            auditController.error(Account, format("Ошибка при закрытии счета по ручному вводу: '%s'",
                    accountWrapper.getBsaAcid()), null, e);
            return new RpcRes_Base<ManualAccountWrapper>(
                    accountWrapper, true, errMessage);
        }*/
    }

    public GLAccount createRequestAccount(GLAccountRequest request) throws Exception {
        ErrorList errList = new ErrorList();
        try {
            Date dateOpen = request.getDateOpen();
            if (null == dateOpen) {
                dateOpen = operdayController.getOperday().getCurrentDate();
            }
            AccountKeys keys = glAccountController.createRequestAccountKeys(request, dateOpen);
            // Такой счет уже есть?
            GLAccount glAccount = glAccountController.findGLAccountMnl(keys);
            if (null != glAccount) {
                createRequestResult(request, glAccount, false, null, null);
                auditController.info(Account, format("Найден счет '%s' по запросу сервиса открытия счетов: '%s'",
                        glAccount.getBsaAcid(), request.getId()), glAccount);
            } else {
                glAccount = glAccountController.createGLAccountMnl(keys, dateOpen, errList, GLAccount.OpenType.SRV);
                createRequestResult(request, glAccount, true, null, null);
                auditController.info(Account, format("Создан счет '%s' по запросу сервиса открытия счетов: '%s'",
                        glAccount.getBsaAcid(), request.getId()), glAccount);
            }
            return glAccount;
        } catch (Exception e) {
            String errMessage = accountErrorMessage(e, errList, initSource());
            String errCode = errList.getErrorCode();
            if (isEmpty(errCode))
                errCode = "-1";
            createRequestResult(request, null, false, errCode, errMessage);
            auditController.error(Account, format("Ошибка при создании счета по запросу сервиса открытия счетов: '%s'",
                    request.getId()), null, e);
            return null;

        }
    }

    /**
     * Поиск/создание технического счета
     * @param accountingType
     * @param glccy
     * @param cbccn
     * @param dateOpen
     * @return
     */
    public GLAccount getTechnicalAccount(AccountingType accountingType, String glccy, String cbccn, Date dateOpen) {
        return Optional.ofNullable(glAccountController.findTechnicalAccount(accountingType, glccy, cbccn))
                .orElseGet(() -> {
                    try {
                        return glAccountController.createTechnicalAccount(accountingType, glccy, cbccn, dateOpen);
                    } catch (Exception e) {
                        throw new DefaultApplicationException(e.getMessage(), e);
                    }
                });
    }

    private void createRequestResult(GLAccountRequest request, GLAccount glAccount, boolean newAcc, String error, String descr) throws Exception {
        glAccountRequestRepository.executeInNewTransaction(persistence -> {
            glAccountRequestRepository.updateRequestStateProcessed(request, GLAccountRequest.RequestStatus.OK);
            glAccountRequestRepository.createResponse(request, glAccount, newAcc, error, descr);
            glAccountRequestRepository.createEvent(request);
            return null;
        });
    }

    private String accountErrorMessage(Throwable e, ErrorList errorList, String source) {
        String errMessage = getErrorMessage(e);
        final String start = "DefaultApplicationException: ";
        final String stop = "\r\n\tat";
        String errorCode = ValidationError.getErrorCode(errMessage);
        String errorMessage = substr(errMessage, start, stop);
        String errorText = ValidationError.getErrorText(errorMessage);
        errorList.addNewErrorDescription(errorText, errorCode);

        log.error(format("%s: %s. Обнаружена: %s\n'", errorList.getErrorMessage(), errMessage, source), e);
/*
        if (null != errorList && errorList.isEmpty()) {
            errMessage = ValidationError.getErrorText(errMessage);
            if (!isEmpty(errMessage))
                errorList.addNewErrorDescription("", "", errMessage);
        }
*/
        return errorList.getErrorMessage();
    }

    public String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, DefaultApplicationException.class,
                SQLIntegrityConstraintViolationException.class, IllegalArgumentException.class);
    }

    private Supplier<String> throwsPLAccountNotCreated() {
        return () -> {throw new DefaultApplicationException();};
    }

}
