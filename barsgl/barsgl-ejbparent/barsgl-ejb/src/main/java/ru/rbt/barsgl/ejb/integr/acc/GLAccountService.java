package ru.rbt.barsgl.ejb.integr.acc;

import org.apache.log4j.Logger;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.bankjar.Constants;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeysBuilder;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.acc.GLAccountRequest;
import ru.rbt.barsgl.ejb.entity.dict.AccountingType;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.bg.EtlPostingController;
import ru.rbt.barsgl.ejb.repository.*;
import ru.rbt.barsgl.ejb.repository.dict.AccountingTypeRepository;
import ru.rbt.barsgl.ejb.security.UserContext;
import ru.rbt.barsgl.shared.ErrorList;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.security.ejb.repository.access.PrmValueRepository;
import ru.rbt.security.ejb.repository.access.SecurityActionRepository;
import ru.rbt.security.entity.access.PrmValue;
import ru.rbt.shared.Assert;
import ru.rbt.shared.ExceptionUtils;
import ru.rbt.shared.enums.SecurityActionCode;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Account;
import static ru.rbt.barsgl.ejb.entity.acc.GLAccount.RelationType.FIVE;
import static ru.rbt.barsgl.ejb.entity.acc.GLAccount.RelationType.FOUR;
import static ru.rbt.barsgl.ejb.entity.acc.GLAccount.RelationType.TWO;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperSide.C;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperSide.D;
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
    private ExcacRlnRepository excacRlnRepository;

    @EJB
    private OperdayController operdayController;

    @EJB
    private AuditController auditController;

    @Inject
    private BankCurrencyRepository bankCurrencyRepository;

    @EJB
    private GLAccountRequestRepository glAccountRequestRepository;

    @Inject
    private GLAccountProcessor glAccountProcessor;

    @Inject
    private GLAccountProcessorTech glAccountProcessorTech;

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
            glAccountProcessor.fillAccountKeysMidas(operSide, dateOpen, keys);
            String bsaacidXX = glAccountController.findBsaAcid_for_XX(keys.getAccountMidas(), keys, operSide.getMsgName());
            if (bsaacidXX != null) {
                return bsaacidXX;
            } else{ //создание в accrln,accrlnext(триггер),bsaacc,gl_acc
                checkNotStorno(operation, operSide);
                return glAccountController.createGLAccountXX(operation, operSide, dateOpen, keys, null);
            }
        } else if (!isEmpty(keys.getGlSequence())                           // счета ОФР
                && keys.getGlSequence().toUpperCase().startsWith("PL")) {
            return getAccountByPlcode(operation, operSide, keys, dateOpen);
        } else if (!isEmpty(keys.getGlSequence())                           // технические счета 99999, 99998
                && keys.getGlSequence().toUpperCase().startsWith("GL")) {
            // получаем счет ЦБ сразу из GL_ACC
            return glAccountRepository.findForSequenceGL(keys);
        } else if(!isEmpty(keys.getGlSequence())                           // технические счета 99999, 99998
                && keys.getGlSequence().toUpperCase().startsWith("TH")) {
            // заполнены и ключи и счет
            //glAccountController.fillAccountKeysMidas(operSide, dateOpen, keys);
            BankCurrency currency = bankCurrencyRepository.getCurrency(keys.getCurrency());
            keys.setCurrencyDigital(currency.getDigitalCode());
            String sAccType = keys.getAccountType();
            AccountingType accType = accountingTypeRepository.findById(AccountingType.class,sAccType);
            return Optional.ofNullable(glAccountController.findOrReopenTechnicalAccountTH(accType,keys.getCurrency(),keys.getCompanyCode())).orElseGet(() -> {
                try {
                    checkNotStorno(operation, operSide);
                    return glAccountController.findOrCreateGLAccountTH(operation, accType,operSide, dateOpen, keys);
                } catch (Exception e) {
                    throw new DefaultApplicationException(e);
                }
            }).getBsaAcid();
        // Этот функционал юольше не нужен. Если ключи и счет - счет
        } else // заполнены и ключи и счет
            if(isAccountWithKeys(operation, operSide)) {
                // заполнены и ключи и счет
                String bsaacid = glAccountController.getGlAccountNumberWithKeys(operation, operSide);
                return Optional.ofNullable(glAccountController.findGLAccount(bsaacid)).orElseGet(() -> {
                    throw new ValidationError(ACCOUNT_NOT_FOUND, operSide.getMsgName(), bsaacid, "");
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

        if ("7903".equals(keys.getAccountCode())
                || "7904".equals(keys.getAccountCode())) {
            final String optype = "N";
            keys.setPassiveActive("7904".equals(keys.getAccountCode()) ? Constants.PASIV : Constants.ACTIV);
            BankCurrency bankCurrency = (operSide.equals(operSide.D) ? operation.getCurrencyCredit() : operation.getCurrencyDebit());
            return Optional.ofNullable(excacRlnRepository.findForPlcode7903(keys, bankCurrency, optype))
                    .orElseGet(() -> glAccountController.createAccountsExDiff(operation, operSide, keys, bankCurrency, dateOpen, optype))
                    .getBsaAcid();
        } else {
            GLAccount.RelationType rlnType = accountingTypeRepository.findById(AccountingType.class, keys.getAccountType()).isBarsAllowed() ? FIVE : TWO;
            // Перенесено сюда, так как портит данные для других этапов
            glAccountProcessor.fillAccountOfrKeysMidas(operation, operSide, dateOpen, keys);
            glAccountProcessor.fillAccountOfrKeys(operSide, dateOpen, keys); // Обогащение

                return Optional.ofNullable(glAccountRepository
                        .findGLPLAccount(keys.getCurrency(), keys.getCustomerNumber(), keys.getAccountType()
                            , keys.getCustomerType(), keys.getTerm(), keys.getPlCode(), keys.getCompanyCode(), rlnType, dateOpen))
                        .map(GLAccount::getBsaAcid).orElseGet(() -> {
                            try {
                                return glAccountController.createGLPLAccount(keys, rlnType, operation, operSide);
                            } catch (Exception e) {
                                throw new DefaultApplicationException(e.getMessage(), e);
                            }
                        });
        }
    }


    private void checkAccountPermission(ManualAccountWrapper wrapper, FormAction action) {
        String acc2 = wrapper.getBalanceAccount2();
        if (isEmpty(acc2))
            acc2 = wrapper.getBsaAcid();
        Long userId = wrapper.getUserId();
        if (null == userId)
            userId = userContext.getUserId();
        String acc1 = substr(acc2, 3);
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
            if (null != (glAccount = glAccountController.findGLAccountMnl(keys, dateOpen))) {
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
     * Создание лицевого счета вручную
     * @param accountWrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualAccountWrapper> createManualAccountTech(ManualAccountWrapper accountWrapper) throws Exception {
        try {
            checkAccountPermission(accountWrapper, FormAction.CREATE);
            Date dateOpen = new SimpleDateFormat(ManualAccountWrapper.dateFormat).parse(accountWrapper.getDateOpenStr());
            //AccountKeys keys = glAccountController.createWrapperAccountKeys(accountWrapper, dateOpen);
            // Такой счет уже есть
            GLAccount glAccount = null;
            String accType = org.apache.commons.lang3.StringUtils.leftPad(accountWrapper.getAccountType().toString(),9,"0");
            AccountingType accTypeGL = (AccountingType) accountingTypeRepository.findById(AccountingType.class,accType);
            String glCCY = accountWrapper.getCurrency();
            String cbCCN = accountWrapper.getFilial();

            if (null != (glAccount = glAccountController.findTechnicalAccountTH(accTypeGL,glCCY,cbCCN))) {
                throw new ValidationError(ACCOUNTGLTH_ALREADY_EXISTS, glAccount.getBsaAcid());
            }

            BankCurrency bankCurrency  = bankCurrencyRepository.getCurrency(glCCY);

            AccountKeys accKey =  AccountKeysBuilder.create()
                                    .withAccountType(accType)
                                    .withCurrency(glCCY)
                                    .withCurrencyDigital(bankCurrency.getDigitalCode())
                                    .withDealSource(accountWrapper.getDealSource())
                                    .withCompanyCode(cbCCN).build();

            String bassid = glAccountController.getGlAccountNumberTHWithKeys(GLOperation.OperSide.N,accKey);

            glAccount = glAccountController.createAccountTH(bassid,null,GLOperation.OperSide.N,dateOpen,accKey, GLAccount.OpenType.MNL);
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
                if (null != (glAccount = glAccountController.findGLPLAccountMnl(keys, dateOpen))) {
                    throw new ValidationError(ACCOUNTGL_ALREADY_EXISTS, glAccount.getBsaAcid(), glAccount.getAcid());
                }

                plAccountProcessor.fillAccountKeysMidas(accountWrapper, keys);

                GLAccount.RelationType rlnType = plAccountProcessor.getRlnTypePlMnl(keys.getAccountType(),
                        accountWrapper.getCbCustomerType(), keys.getAccount2(), keys.getAccountMidas(), keys.getPlCode(), dateOpen);
                keys.setRelationType(rlnType.getValue());

                BankCurrency currency = bankCurrencyRepository.refreshCurrency(keys.getCurrency());
                keys.setCurrencyDigital(currency.getDigitalCode());
                keys.setFilial(glAccountRepository.getFilialByCompanyCode(keys.getCompanyCode()));
                glAccount = glAccountController.createGLPLAccountMnl(keys, rlnType, dateOpen, accountWrapper.getErrorList(), GLAccount.OpenType.MNL);
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
                throw new ValidationError(ACCOUNT_NOT_FOUND, "", accountWrapper.getBsaAcid(), "Счет ЦБ");
            }
            checkAccountPermission(accountWrapper, FormAction.UPDATE);
            String dateOpenStr = accountWrapper.getDateOpenStr();
            Date dateOpen = dateOpenStr == null ? null : new SimpleDateFormat(ManualAccountWrapper.dateFormat).parse(dateOpenStr);
            String dateCloseStr = accountWrapper.getDateCloseStr();
            Date dateClose = dateCloseStr == null ? null : new SimpleDateFormat(ManualAccountWrapper.dateFormat).parse(dateCloseStr);
            String act = (null == account.getDateClose() && null != dateClose) ? "Закрыт" : "Изменен";
            AccountKeys keys = glAccountController.createWrapperAccountKeys(accountWrapper, dateOpen);
            glAccountProcessor.validateGLAccountMnl(account, dateOpen, dateClose, keys, accountWrapper.getErrorList());
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

    public RpcRes_Base<ManualAccountWrapper> updateManualAccountTech(ManualAccountWrapper accountWrapper) throws Exception {

        try {
            GLAccount account = glAccountRepository.findById(GLAccount.class, accountWrapper.getId());
            if (null == account) {      // Такого счета нет!
                throw new ValidationError(ACCOUNT_NOT_FOUND, "", accountWrapper.getBsaAcid(), "Счет ЦБ");
            }
            checkAccountPermission(accountWrapper, FormAction.UPDATE);
            String dateOpenStr = accountWrapper.getDateOpenStr();
            Date dateOpen = dateOpenStr == null ? null : new SimpleDateFormat(ManualAccountWrapper.dateFormat).parse(dateOpenStr);
            String dateCloseStr = accountWrapper.getDateCloseStr();
            Date dateClose = dateCloseStr == null ? null : new SimpleDateFormat(ManualAccountWrapper.dateFormat).parse(dateCloseStr);
            String act = (null == account.getDateClose() && null != dateClose) ? "Закрыт" : "Изменен";

            DataRecord data = glAccountRepository.getAccountParams(Utils.fillUp(Long.toString(accountWrapper.getAccountType()), 9),
                    "00", "00", dateOpen);
            if (null == data) {
                  throw new ValidationError(ACCOUNT_PARAMS_NOT_FOUND, "", accountWrapper.getAccountType().toString(), "00", "00", dateUtils.onlyDateString(dateOpen));
            }

            String accType = org.apache.commons.lang3.StringUtils.leftPad(accountWrapper.getAccountType().toString(),9,"0");
            String glCCY = accountWrapper.getCurrency();
            String cbCCN = accountWrapper.getFilial();

            AccountKeys keys =  AccountKeysBuilder.create()
                    .withAccountType(accType)
                    .withCurrency(glCCY)
                    .withDealSource(accountWrapper.getDealSource())
                    .withCompanyCode(cbCCN).build();

            account.setDescription(accountWrapper.getDescription());

            String msg;
            msg = null != dateClose
                    ? "Уже существует счет с такой датой закрытия. Установите другую дату закрытия"
                    : "Уже существует открытый счет с таким номером.";
                      if (glAccountRepository.checkTechAccountExists(account.getId(), account.getBsaAcid() ,dateClose)){
                throw new Exception(msg);
            }

            if (glAccountRepository.checkTechAccountExistsInterval(account.getId(),account.getBsaAcid(), dateOpen,dateClose))
            {
                throw  new Exception(String.format("Найдены действующие счета в период с %s по %s",dateOpenStr, dateCloseStr == null ? "01.01.2029" : dateCloseStr));
            }

            glAccountProcessorTech.validateGLAccountMnlTech(account, dateOpen, dateClose, keys, accountWrapper.getErrorList());
            GLAccount glAccount = glAccountController.updateGLAccountMnlTech(account, dateOpen, dateClose, keys, accountWrapper.getErrorList());
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

    public RpcRes_Base<ManualAccountWrapper> closeManualAccountTech(ManualAccountWrapper accountWrapper) throws Exception {

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

                //GLAccount account = glAccountController.findGLAccount(accountWrapper.getBsaAcid());
                GLAccount account = glAccountRepository.findById(GLAccount.class, accountWrapper.getId());
                if (null == account) {      // Такого счета нет!
                    throw new ValidationError(ACCOUNT_NOT_FOUND, "", accountWrapper.getBsaAcid(), "Счет ЦБ");
                }
                String dateCloseStr = accountWrapper.getDateCloseStr();
                Date dateClose = dateCloseStr == null ? null : new SimpleDateFormat(ManualAccountWrapper.dateFormat).parse(dateCloseStr);

                String act;
                String msg;
                //Проверка на существование счета с таким номером
                if ((null == account.getDateClose() && null != dateClose)){
                    act = "Закрыт счет";
                     msg = "Уже существует счет с такой датой закрытия. Установите другую дату закрытия";
                } else {
                    act = "Отменено закрытие счета";
                    msg = "Уже существует открытый счет с таким номером.";
                }

                if (glAccountRepository.checkTechAccountExists(accountWrapper.getId(), accountWrapper.getBsaAcid() ,dateClose)){
                    throw new Exception(msg);
                }
                glAccountProcessorTech.checkDateCloseMnl(account, account.getDateOpen(), dateClose);
                GLAccount glAccount = glAccountController.closeGLAccountMnlTech(account, dateClose, accountWrapper.getErrorList());
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
    }

    public RpcRes_Base<ManualAccountWrapper> findManualAccountTech(ManualAccountWrapper accountWrapper) throws Exception {
        try{
            Long acct = accountWrapper.getAccountType();
            AccountingType accType = accountingTypeRepository.findById(AccountingType.class, Utils.fillUp(acct.toString(),9));
            String cbccn = glAccountRepository.getCompanyCodeByFilial(accountWrapper.getFilial());

            GLAccount account = glAccountController.findTechnicalAccountTH(accType, accountWrapper.getCurrency(), cbccn);
            if (account!=null) {
                accountWrapper.setBsaAcid(account.getBsaAcid());
            }
            else {
                accountWrapper.getErrorList().addErrorDescription("счёт не найден");
            }

            return new RpcRes_Base<ManualAccountWrapper>(accountWrapper,false,"счёт найден");

        } catch (Exception e) {
            String errMessage = accountErrorMessage(e, accountWrapper.getErrorList(), initSource());
            auditController.error(Account, format("Ошибка поиска счёта: '%s'",
                    accountWrapper.getAccountType()), null, e);
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
                    throw new ValidationError(ACCOUNT_NOT_FOUND, "", accountWrapper.getBsaAcid(), "Счет ЦБ");
                }
                String dateCloseStr = accountWrapper.getDateCloseStr();
                Date dateClose = dateCloseStr == null ? null : new SimpleDateFormat(ManualAccountWrapper.dateFormat).parse(dateCloseStr);
                String act = (null == account.getDateClose() && null != dateClose) ? "Закрыт счет" : "Отменено закрытие счета";
                glAccountProcessor.checkDateCloseMnl(account, account.getDateOpen(), dateClose);
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

    public GLAccount createBulkOpeningAccount(ManualAccountWrapper accountWrapper) throws Exception {
        try {
            Date dateOpen = new SimpleDateFormat(ManualAccountWrapper.dateFormat).parse(accountWrapper.getDateOpenStr());
            AccountKeys keys = glAccountController.createWrapperAccountKeys(accountWrapper, dateOpen);
            // Такой счет уже есть
//            GLAccount glAccount = glAccountController.findGLAccountMnl(keys);
//            if (glAccount != null) return glAccount.getBsaAcid();

            GLAccount glAccount = glAccountController.createGLAccountMnlInRequiredTrans(keys, FOUR, dateOpen, accountWrapper.getErrorList(), GLAccount.OpenType.MNL);
            auditController.info(Account, format("Создан счет '%s'  по массовому открытию счетов",
                    glAccount.getBsaAcid()), glAccount);
            glAccountController.fillWrapperFields(accountWrapper, glAccount);
            return glAccount;
        } catch (Throwable e) {
            accountErrorMessage(e, accountWrapper.getErrorList(), initSource());
            auditController.error(Account, format("Ошибка при создании счета по массовому открытию счетов для acid: '%s'",
                    accountWrapper.getAcid()), null, e);
            throw e;
        }
    }

/*
    public GLAccount createBulkOpeningAccount(AccountKeys keys, Date dateOpen) {
        //checkAccountPermission(accountWrapper, FormAction.CREATE);
        ErrorList errList = new ErrorList();

        GLAccount glAccount = null;
        try {
            glAccount = glAccountController.createGLAccountMnl(keys, dateOpen, errList, GLAccount.OpenType.MNL);
            auditController.info(Account, format("Создан счет '%s' по массовому открытию счетов",
                    glAccount.getBsaAcid()), glAccount);

            */
/*
        // Такой счет уже есть?
        GLAccount glAccount = glAccountController.findGLAccountMnl(keys);
             *//*

        } catch (Throwable e) {
            String errMessage = accountErrorMessage(e, errList, initSource());
            auditController.error(Account, format("Ошибка при создании счета по массовому открытию счетов для acid: '%s'",
                    keys.getAccountMidas()), null, e);
        }
        return glAccount;
    }
*/

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

}
