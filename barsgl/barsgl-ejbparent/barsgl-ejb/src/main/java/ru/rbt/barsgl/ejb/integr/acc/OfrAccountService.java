package ru.rbt.barsgl.ejb.integr.acc;

import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.ErrorList;
import ru.rbt.shared.ExceptionUtils;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Account;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.ejbcore.util.StringUtils.substr;
import static ru.rbt.ejbcore.validation.ErrorCode.*;
import static ru.rbt.ejbcore.validation.ValidationError.initSource;
/**
 * Created by ER18837 on 13.11.15.
 */
@Stateless
@LocalBean
public class OfrAccountService {

    @EJB
    private GLAccountRepository accountRepository;

    @EJB
    private GLOperationRepository operationRepository;

    @Inject
    private OfrAccountProcessor ofrAccountProcessor;

    @EJB
    private AuditController auditController;

    @Inject
    private ru.rbt.ejbcore.util.DateUtils dateUtils;

    public String findAccountCB(AccountKeys keys, Date dateFrom) throws Exception {
        return accountRepository.executeInNonTransaction(connection -> {
            try {
                return ofrAccountProcessor.findAccount(connection, keys, dateFrom);
            } catch (Exception e) {
                throw new DefaultApplicationException(getErrorMessage(e), e);
            }
        });
    }

    public String createAccountCB(AccountKeys keys, Date dateOpen, Date dateClose, Date dateFrom) throws Exception {
        return accountRepository.executeTransactionally(connection -> {
            try {
                return ofrAccountProcessor.createAccount(connection, keys, dateOpen, dateClose, dateFrom);
            } catch (Exception e) {
                throw new DefaultApplicationException(getErrorMessage(e), e);
            }
        });
    }

    public String createAccountCBnoTrans(AccountKeys keys, Date dateOpen, Date dateClose, Date dateFrom) throws Exception {
        return accountRepository.executeInNonTransaction(connection -> {
            try {
                return ofrAccountProcessor.createAccount(connection, keys, dateOpen, dateClose, dateFrom);
            } catch (Exception e) {
                throw new DefaultApplicationException(getErrorMessage(e), e);
            }
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
        return errorList.getErrorMessage();
    }

    public String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, DefaultApplicationException.class);
    }

}
