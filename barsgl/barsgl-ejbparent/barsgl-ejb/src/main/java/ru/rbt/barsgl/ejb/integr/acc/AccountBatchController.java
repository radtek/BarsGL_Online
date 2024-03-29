package ru.rbt.barsgl.ejb.integr.acc;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.ejb.repository.AccountBatchPackageRepository;
import ru.rbt.barsgl.ejb.security.UserContext;
import ru.rbt.barsgl.shared.NotAuthorizedUserException;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.AccountBatchPackageState;
import ru.rbt.barsgl.shared.operation.AccountBatchWrapper;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.security.ejb.repository.access.SecurityActionRepository;
import ru.rbt.shared.ExceptionUtils;
import ru.rbt.shared.enums.SecurityActionCode;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.PersistenceException;

import java.sql.DataTruncation;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountBatch;
import static ru.rbt.barsgl.shared.enums.AccountBatchPackageState.ERROR;
import static ru.rbt.barsgl.shared.enums.AccountBatchPackageState.IS_LOAD;
import static ru.rbt.barsgl.shared.enums.AccountBatchPackageState.ON_VALID;
import static ru.rbt.barsgl.shared.enums.AccountBatchState.LOAD;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.ejbcore.validation.ErrorCode.ACCOUNT_BATCH_ERROR;
import static ru.rbt.ejbcore.validation.ValidationError.initSource;

import static ru.rbt.ejbcore.util.StringUtils.listToString;
import static ru.rbt.shared.enums.SecurityActionCode.AccPkgFileDel;
import static ru.rbt.shared.enums.SecurityActionCode.AccPkgFileOpen;

/**
 * Created by er18837 on 22.10.2018.
 */
public class AccountBatchController {
    private static final String tableName = "GL_BATPKG";

    @EJB
    private AccountBatchPackageRepository packageRepository;

    @EJB
    private AuditController auditController;

    @Inject
    private OperdayController operdayController;

    @Inject
    private SecurityActionRepository actionRepository;

    @Inject
    private DateUtils dateUtils;

    @Inject
    private UserContext userContext;

    public RpcRes_Base<AccountBatchWrapper> processAccountBatchRq(AccountBatchWrapper wrapper) throws Exception {
        String msg = "Ошибка обработки пакета на открытие счета ID = " + wrapper.getPackageId();
        try {
            switch (wrapper.getAction()) {
                case OPEN:              // пакет на открытие
                    return authorizePackage(wrapper);
                case DELETE:            // удалить пакет
                    return deletePackage(wrapper);
            }
            return new RpcRes_Base<AccountBatchWrapper>(
                    wrapper, true, "Неверное действие");
        } catch (NotAuthorizedUserException e) {
            auditController.warning(AccountBatch, msg, tableName, getPackageId(wrapper), e);
            return new RpcRes_Base<>(wrapper, true, e.getMessage());
        } catch (ValidationError e) {
            auditController.warning(AccountBatch, msg, tableName, getPackageId(wrapper), e);
            return new RpcRes_Base<>(wrapper, true, ValidationError.getErrorText(getErrorMessage(e)));
        } catch (Throwable e) {
            auditController.error(AccountBatch, msg, tableName, getPackageId(wrapper), e);
            return new RpcRes_Base<>(wrapper, true, getErrorMessage(e));
        }
    }

    public RpcRes_Base<AccountBatchWrapper> authorizePackage(AccountBatchWrapper wrapper) throws Exception {
        AccountBatchPackage pkg = getPackageWithCheck(wrapper, "открыть счета пакета", IS_LOAD);

        Date curdate = operdayController.getOperday().getCurrentDate();
        if (!curdate.equals(pkg.getOperday()))
            throw new ValidationError(ACCOUNT_BATCH_ERROR, String.format("Нельзя передать пакет счетов с ID = %s на обработку," +
                    " дата создания пакета '%s' не равна текущему опердню '%s'",
                     pkg.getId(), dateUtils.onlyDateString(pkg.getOperday()), dateUtils.onlyDateString(curdate)));
        if (!packageRepository.checkAccountRequestState(pkg.getId(), LOAD))
            throw new ValidationError(ACCOUNT_BATCH_ERROR, String.format("Нельзя передать пакет счетов с ID = %s на обработку," +
                    " в пакете есть счета не в статусе '%s'", pkg.getId(), LOAD.name()));

        String userProc = getUserName();
        if (!userProc.equals(pkg.getLoadUser()))
            checkUserPermission(pkg.getLoadUser(), wrapper);

        packageRepository.executeInNewTransaction(persistence -> {
            packageRepository.updateAccountPackageValid(pkg, ON_VALID, userProc);
            return null;
        });
        wrapper.setPackageState(ON_VALID);
        String msg = "Пакет счетов с ID = " + wrapper.getPackageId() + " передан на обработку";
        auditController.info(AccountBatch, msg, tableName, getPackageId(wrapper));
        return new RpcRes_Base<>(wrapper, false, msg +
                "\n\nСчета будут открыты после успешной валидации всего пакета" +
                "\nТекущее состояние и результат обработки пакета можно" +
                "\nпосмотреть на странице пакетов или счетов по ID пакета = " + wrapper.getPackageId());
    }

    public RpcRes_Base<AccountBatchWrapper> deletePackage(AccountBatchWrapper wrapper) throws Exception {
        AccountBatchPackage pkg = getPackageWithCheck(wrapper, "удалить пакет счетов", IS_LOAD, ERROR);

        AccountBatchPackageState packageState = pkg.getState();
        if (!(packageState == IS_LOAD || packageState == ERROR))
            throw new ValidationError(ACCOUNT_BATCH_ERROR, String.format("Нельзя удалить пакет счетов с ID = %s," +
                    " статус пакета '%s' (допустимые статусы '%s', '%s')", pkg.getId(),
                    packageState.name(), IS_LOAD.name(), ERROR.name()));
        YesNo invisible = pkg.getInvisible();
/*
        if (invesible == YesNo.Y)
            throw new ValidationError(ACCOUNT_BATCH_ERROR, String.format("Нельзя удалить пакет счетов с ID = %s," +
                            " пакет уже удален", pkg.getId(),
                    packageState.name(), IS_LOAD.name(), ERROR.name()));
*/

        String userProc = getUserName();
        if (userProc.equals(pkg.getLoadUser())) {
            packageRepository.executeInNewTransaction(persistence -> {
                packageRepository.deleteAccountPackageOwn(pkg, packageState);
                return null;
            });
        } else {
            if (invisible == YesNo.Y)
                throw new ValidationError(ACCOUNT_BATCH_ERROR, String.format("Нельзя удалить пакет счетов с ID = %s," +
                                " пакет уже удален", pkg.getId(),
                        packageState.name(), IS_LOAD.name(), ERROR.name()));

            checkUserPermission(pkg.getLoadUser(), wrapper);
            packageRepository.executeInNewTransaction(persistence -> {
                packageRepository.deleteAccountPackageOther(pkg, packageState, userProc);
                return null;
            });
        }

        String msg = String.format("Пакет счетов с ID = %d, загруженный пользователем %s, удален пользователем %s",
                wrapper.getPackageId(), pkg.getLoadUser(), userProc );
        auditController.info(AccountBatch, msg, tableName, getPackageId(wrapper));
        return new RpcRes_Base<>(wrapper, false, "Пакет счетов с ID = " + wrapper.getPackageId() + " удален");
    }

    private AccountBatchPackage getPackageWithCheck(AccountBatchWrapper wrapper, String msg, AccountBatchPackageState... allowedPackageState) throws SQLException {
        Long id = wrapper.getPackageId();
        final AccountBatchPackage pkg = packageRepository.findById(id);
        if (null == pkg ) {
            throw new ValidationError(ACCOUNT_BATCH_ERROR, "Не найден пакет с ID = " + id + ". Обновите информацию");
        }
        Set<AccountBatchPackageState> states = new HashSet<AccountBatchPackageState>();
        if (allowedPackageState != null) {
            states.addAll(Arrays.asList(allowedPackageState));
            if (!states.contains(pkg.getState())) {
                throw new ValidationError(ACCOUNT_BATCH_ERROR,
                        String.format("Пакет ID = %s: нельзя " + msg + " в статусе: '%s' ('%s'). Обновите информацию",
                                id, pkg.getState(), pkg.getState().getLabel()));
            }
        }
        return pkg;
    }


    private String getPackageId(AccountBatchWrapper wrapper) {
        return null == wrapper.getPackageId() ? "" : wrapper.getPackageId().toString();
    }

    public String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class,
                SQLIntegrityConstraintViolationException.class, PersistenceException.class, IllegalArgumentException.class,
                DefaultApplicationException.class);
    }

    public void checkUserPermission(String userLoad, AccountBatchWrapper wrapper) throws NotAuthorizedUserException {
        String act = "";
        SecurityActionCode actionCode = null;
        switch (wrapper.getAction()) {
            case OPEN:
                act = "передать на обработку";
                actionCode = AccPkgFileOpen;
                break;
            case DELETE:
                act = "удалить";
                actionCode = AccPkgFileDel;
                break;
        }
        if(!actionRepository.getAvailableActions(userContext.getUserId()).contains(actionCode)) {       {
            throw new ValidationError(ACCOUNT_BATCH_ERROR,
                    String.format("Пакет ID = %s: нельзя %s пакет, загруженный другим пользователем ('%s')",
                            wrapper.getPackageId(), act, userLoad));
            }
        }
    }

    public String getUserName() throws NotAuthorizedUserException {
        String userName = userContext.getUserName();
        if (isEmpty(userName)) {
            throw new NotAuthorizedUserException();
        }
        return userName;
    }

}
