package ru.rbt.barsgl.ejb.integr.pst;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.gl.*;
import ru.rbt.barsgl.ejb.integr.ValidationAwareHandler;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountService;
import ru.rbt.barsgl.ejb.repository.*;
import ru.rbt.barsgl.ejbcore.validation.ResultCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.barsgl.shared.enums.BackValuePostStatus;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.List;

import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperSide.C;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperSide.D;
import static ru.rbt.ejbcore.util.StringUtils.ifEmpty;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.ejbcore.validation.ErrorCode.*;

/**
 * Created by Ivan Sevastyanov
 * Выполняет обработку
 */
public abstract class GLOperationProcessor extends ValidationAwareHandler<GLOperation> {

    @EJB
    protected PdRepository pdRepository;

    @EJB
    private GLOperationRepository glOperationRepository;

    @EJB
    private GLTechOperationRepository glTechOperationRepository;

    @EJB
    private GLAccountRepository glAccountRepository;

    @Inject
    private GLPostingRepository glPostingRepository;

    @Inject
    protected OperdayController operdayController;

    @Inject
    private GLAccountService accountService;

    @Inject
    private DateUtils dateUtils;

    public abstract boolean isSupported(GLOperation operation);
    public abstract GLOperation.OperType getOperationType();
    public abstract void setSpecificParameters(GLOperation operation) throws Exception;
    public abstract List<GLPosting> createPosting(GLOperation operation) throws Exception;

    public OperState getSuccessStatus() {
        return OperState.POST;
    }

    public void resolveOperationReference(GLOperation operation) throws Exception {
    };

    public void resolvePostingReference(GLOperation operation, List<GLPosting> postList) {
    }

    /**
     * Заполняем контекс проверками
     * @param operation операция
     * @param context контекст
     */
    public void fillValidationContext(GLOperation operation, ValidationContext context) {
//        final boolean accDebitOpen = glAccountRepository.checkBsaAccountOpen(operation.getAccountDebit(), operation.getValueDate());
//        final boolean accCreditOpen = glAccountRepository.checkBsaAccountOpen(operation.getAccountCredit(), operation.getValueDate());

        //Определения технического счёта по ключу. По идее надо ввести признак некий технической операции.
        //пока так, а там посмотрим.
        ResultCode accDebitResult;
        ResultCode accCreditResult;
        if (GLOperation.flagTechOper.equals(operation.getBsChapter()))
        {
            accDebitResult = glAccountRepository.checkBsaAccountGlAcc(operation.getAccountDebit());
            accCreditResult = glAccountRepository.checkBsaAccountGlAcc(operation.getAccountCredit());

        }
        else {
            accDebitResult = glAccountRepository.checkBsaAccountAnal(operation.getAccountDebit(), operation.getValueDate(), operdayController.getOperday().getCurrentDate(), operdayController.getOperday().getLastWorkingDay());
            accCreditResult = glAccountRepository.checkBsaAccountAnal(operation.getAccountCredit(), operation.getValueDate(), operdayController.getOperday().getCurrentDate(), operdayController.getOperday().getLastWorkingDay());
        }

        // Счет по дебету
        context.addValidator(() -> {
            if (accDebitResult == ResultCode.ACCOUNT_NOT_FOUND) {
                throw new ValidationError(ACCOUNT_NOT_FOUND, "по дебету",
                        ifEmpty(operation.getAccountDebit(), ""), operation.getColumnName("accountDebit"));
            }
        });
        context.addValidator(() -> {
            if (accDebitResult == ResultCode.ACCOUNT_IS_CLOSED_BEFOR) {
                throw new ValidationError(ACCOUNT_IS_CLOSED_BEFOR, "по дебету",
                        ifEmpty(operation.getAccountDebit(), ""), operation.getColumnName("accountDebit"));
            }
        });
        context.addValidator(() -> {
            if (accDebitResult == ResultCode.ACCOUNT_IS_OPEN_LATER) {
                throw new ValidationError(ACCOUNT_DB_IS_OPEN_LATER, "по дебету",
                        ifEmpty(operation.getAccountDebit(), ""), operation.getColumnName("accountDebit"));
            }
        });

        // Счет по кредиту
        context.addValidator(() -> {
            if (accCreditResult == ResultCode.ACCOUNT_NOT_FOUND) {
                throw new ValidationError(ACCOUNT_NOT_FOUND, "по кредиту",
                        ifEmpty(operation.getAccountCredit(), ""), operation.getColumnName("accountCredit"));
            }
        });
        context.addValidator(() -> {
            if (accCreditResult == ResultCode.ACCOUNT_IS_CLOSED_BEFOR) {
                throw new ValidationError(ACCOUNT_IS_CLOSED_BEFOR, "по кредиту",
                        ifEmpty(operation.getAccountCredit(), ""), operation.getColumnName("accountCredit"));
            }
        });
        context.addValidator(() -> {
            if (accCreditResult == ResultCode.ACCOUNT_IS_OPEN_LATER) {
                throw new ValidationError(ACCOUNT_CR_IS_OPEN_LATER, "по кредиту",
                        ifEmpty(operation.getAccountCredit(), ""), operation.getColumnName("accountCredit"));
            }
        });

        // Филиал по дебету
        context.addValidator(() -> {
            if (accDebitResult == ResultCode.SUCCESS && !operation.isFilialDebit()) {
                throw new ValidationError(FILIAL_NOT_DEFINED, "по дебету",
                        operation.getAccountDebit(), operation.getColumnName("filialDebit"));
            }
        });
        // Филиал по кредиту
        context.addValidator(() -> {
            if (accCreditResult == ResultCode.SUCCESS && !operation.isFilialCredit()) {
                throw new ValidationError(FILIAL_NOT_DEFINED, "по кредиту",
                        operation.getAccountCredit(), operation.getColumnName("filialCredit"));
            }
        });
        // Глава баланса определена
        context.addValidator(() -> {
            if (accDebitResult == ResultCode.SUCCESS && accCreditResult == ResultCode.SUCCESS
                    && isEmpty(operation.getBsChapter())) {
                throw new ValidationError(BALANSE_CHAPTER_IS_DIFFERENT,
                        operation.getAccountDebit(), operation.getColumnName("accountDebit"),
                        operation.getAccountCredit(), operation.getColumnName("accountCredit"));
            }
        });
        // Глава баланса для МФО
        context.addValidator(() -> {
            if (operation.isInterFilial()
                    && !isEmpty(operation.getBsChapter())
                    && !BalanceChapter.A.name().equals(operation.getBsChapter())) {
                throw new ValidationError(MFO_CHAPTER_NOT_A,
                        operation.getAccountDebit(), operation.getColumnName("accountDebit"),
                        operation.getAccountCredit(), operation.getColumnName("accountCredit"));
            }
        });
        // ИД сделки
        context.addValidator(() -> {
            if ( null == pdRepository.getPref(operation)) {
                throw new ValidationError(DEALID_PYMANTREF_IS_EMPTY,
                        operation.getColumnName("dealId"), operation.getColumnName("paymentRefernce"));
            }
        });

        context.addValidator(() -> {
            ValidationError error = glAccountRepository.checkAccount9999(operation.getAccountDebit(), operation.getAccountCredit(), GLOperation.OperSide.D);
            if (null != error)
                throw error;

            error = glAccountRepository.checkAccount9999(operation.getAccountCredit(), operation.getAccountDebit(), GLOperation.OperSide.D);
            if (null != error)
                throw error;
        });

        // дата валютирования больше текущего ОД
        context.addValidator(() -> {
            final Operday operday = operdayController.getOperday();
            if (operday.getCurrentDate().before(operation.getValueDate())){
                throw new ValidationError(ErrorCode.DATE_NOT_VALID
                        , "валютирования больше текущего ОД"
                        , dateUtils.onlyDateString(operation.getValueDate())
                        , dateUtils.onlyDateString(operday.getCurrentDate()), operday.getPhase().name()
                        , dateUtils.onlyDateString(operday.getLastWorkingDay()), operday.getLastWorkdayStatus().name()
                );
            }
        });

    }

    /**
     * Заполняет в GL operation поля для межфилиальных расчетов
     * @param operation
     * @throws Exception
     */
    protected final void setMfoParameters(GLOperation operation) throws Exception {

        // определить валюту МФО (TODO перенести в класс OperationProcessor)
        BankCurrency ccyDebit = operation.getCurrencyDebit();
        String filialDebit = operation.getFilialDebit();

        BankCurrency ccyCredit = operation.getCurrencyCredit();
        String filialCredit = operation.getFilialCredit();

        BankCurrency ccyMfo;
        if (ccyDebit.equals(BankCurrency.RUB)) {
            ccyMfo = ccyDebit;
            operation.setCcyMfoSide(D);
        } else {
            ccyMfo = ccyCredit;
            operation.setCcyMfoSide(C);
        }

        boolean isClients = glPostingRepository.isMfoClientPosting(operation.getAccountDebit(),
                operation.getAccountCredit());
        String[] mfoAccounts = glPostingRepository.getMfoAccounts(ccyMfo, filialDebit, filialCredit, isClients);
        if (null == mfoAccounts) {      // нет нужных счетов
            throw new ValidationError ( MFO_ACCOUNT_NOT_FOUND,
                            ccyMfo.getCurrencyCode(), filialDebit, filialCredit );
        }

        operation.setCurrencyMfo(ccyMfo);
        operation.setAccountLiability(mfoAccounts[0]);
        operation.setAccountAsset(mfoAccounts[1]);
    }

    /**
     * Заполняет в GL operation поле счет курсовой разницы
     * @param operation     - операция
     * @param bsaAcid       - счет ЦБ, по которому определяется глава и счет курсовой разности
     * @throws Exception
     */
    public final void setAccountExchange(GLOperation operation, String bsaAcid) throws Exception {
        // определить счет курсовой разницы ТОЛЬКО для главы А
        String exchAccount = glPostingRepository.getExchangeAccount(operation);
        if (exchAccount.isEmpty()) { // не задан счет курсовой разницы
            throw new ValidationError(EXCH_ACCOUNT_NOT_FOUND,
                    operation.getExchangeDifference().signum() > 0 ? "+" : "-", glOperationRepository.getBranch(bsaAcid, operation.getPostDate()));
        }
        operation.setAccountExchange(exchAccount);
    }

    //        Bug_CCYExch+Interbanch v0.01.doc
//    public final void setMfoAccountExchange(GLOperation operation, String bsaAcid) throws Exception {
//        // определить счет курсовой разницы ТОЛЬКО для главы А
//        String exchAccount = glPostingRepository.getMfoExchangeAccount(operation);
//        if (exchAccount.isEmpty()) { // не задан счет курсовой разницы
//            throw new ValidationError(EXCH_ACCOUNT_NOT_FOUND,
//                    operation.getExchangeDifference().signum() > 0 ? "+" : "-", glOperationRepository.getBranch(bsaAcid, operation.getPostDate()));
//        }
//        operation.setAccountExchange(exchAccount);
//    }

    public void setStornoOperation(GLOperation operation) throws Exception {
        if (!operation.isStorno())
            return;
        Long stornoID = null;
        if (operation.isTech())
        {
            stornoID = glTechOperationRepository.getStornoOperationID(operation);
        }
        else {
            stornoID = glOperationRepository.getStornoOperationID(operation);
        }
        if (null == stornoID) {
            throw new ValidationError(STORNO_REF_NOT_FOUND,
                    operation.getStornoReference(), operation.getDealId(), operation.getPaymentRefernce(), operation.getValueDate().toString());
        }
        GLOperation stornoOperation = glOperationRepository.findById(GLOperation.class, stornoID);
        operation.setStornoOperation(stornoOperation);
//        operation.setStornoRegistration(stornoType);
    }

    public final boolean isStornoOneday(GLOperation operation) {
        return (operation.isStorno())                                                           // сторно
                && operation.stornoOneday(operdayController.getOperday().getCurrentDate());     // обе операции в опердень
    }

    public final boolean isStornoBackvalue(GLOperation operation) {
        return (operation.isStorno())                                                           // сторно
                && !operation.stornoOneday(operdayController.getOperday().getCurrentDate());    // хотя бы одна операция в другой день
    }

    public final List<Pd> getPostingPd(GLPosting posting) {
        return glOperationRepository.select(Pd.class, "from Pd p where p.pcId = ?1 order by p.id",
                new Object[]{posting.getId()});
    }

    /**
     * Создает счет по дебету / кредиту в случае его отсутствия
     * @param operation
     * @param operSide      - сторона операции
     * @throws Exception
     */
    public final void createAccount(GLOperation operation, GLOperation.OperSide operSide) throws Exception {
        if (D == operSide) {
            if (isEmpty(operation.getAccountDebit())
                    || accountService.isAccountWithKeys(operation, operSide)) {
                String accDebit = accountService.getAccount(operation, D,
                        new AccountKeys(operation.getAccountKeyDebit()));
                operation.setAccountDebit(accDebit);
            }
        } else {
            if (isEmpty(operation.getAccountCredit())
                    || accountService.isAccountWithKeys(operation, operSide)) {
                String accCredit = accountService.getAccount(operation, C,
                        new AccountKeys(operation.getAccountKeyCredit()));
                operation.setAccountCredit(accCredit);
            }
        }
    }

    public final GLOperation createOperationExt(GLBackValueOperation operation) {

        Assert.isTrue(null == operation.getOperExt(), String.format("По операции ID = '%d' уже есть запись в GL_OPEREXT", operation.getId()));
        GLOperationExt operExt = new GLOperationExt(operation.getId(), operation.getPostDate());
        operExt.setCreateTimestamp(operdayController.getSystemDateTime());
        operExt.setManualStatus(BackValuePostStatus.CONTROL);

        // TODO где хранить эти даты и причину... в GLOperation ?
        BackValueParameters parameters = operation.getBackValueParameters();
        if (null != parameters) {
            operExt.setManualReason(parameters.getReason());
            operExt.setDepthCutDate(parameters.getDepthCutDate());
            operExt.setCloseCutDate(parameters.getCloseCutDate());
            operExt.setCloseLastDate(parameters.getCloseLastDate());
        }

        operation.setOperExt(operExt);
        return operation;
    }
}
