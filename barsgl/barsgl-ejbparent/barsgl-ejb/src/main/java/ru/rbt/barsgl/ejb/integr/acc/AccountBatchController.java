package ru.rbt.barsgl.ejb.integr.acc;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.repository.AccountBatchPackageRepository;
import ru.rbt.barsgl.shared.NotAuthorizedUserException;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.AccountBatchPackageState;
import ru.rbt.barsgl.shared.enums.BatchPostStep;
import ru.rbt.barsgl.shared.operation.AccountBatchWrapper;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.ExceptionUtils;

import javax.ejb.EJB;
import javax.persistence.PersistenceException;

import java.sql.DataTruncation;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountBatch;
import static ru.rbt.barsgl.shared.enums.AccountBatchPackageState.IS_LOAD;
import static ru.rbt.barsgl.shared.enums.AccountBatchPackageState.ON_VALID;
import static ru.rbt.ejbcore.validation.ErrorCode.ACCOUNT_BATCH_ERROR;
import static ru.rbt.ejbcore.validation.ValidationError.initSource;

import static ru.rbt.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.ejbcore.util.StringUtils.listToString;

/**
 * Created by er18837 on 22.10.2018.
 */
public class AccountBatchController {
    private static final String tableName = "GL_BATPKG";

    @EJB
    private AccountBatchPackageRepository packageRepository;

    @EJB
    private AuditController auditController;

    public RpcRes_Base<AccountBatchWrapper> processAccountBatchRq(AccountBatchWrapper wrapper) throws Exception {
        String msg = "Ошибка обработки пакета на открытие счета ID = " + wrapper.getPackageId();
        try {
            switch (wrapper.getAction()) {
                case OPEN:              // пакет на открытие
                    return toOpenAccounts(wrapper);
//                case DELETE:            // удалить пакет
//                    return deletePackage(wrapper);
            }
            return new RpcRes_Base<AccountBatchWrapper>(
                    wrapper, true, "Неверное действие");
        } catch (NotAuthorizedUserException e) {
            auditController.warning(AccountBatch, msg, tableName, getPackageId(wrapper), e);
            return new RpcRes_Base<>(wrapper, true, e.getMessage());
        } catch (ValidationError e) {
            auditController.warning(AccountBatch, msg, tableName, getPackageId(wrapper), e);
            return new RpcRes_Base<>(wrapper, true, getErrorMessage(e));
        } catch (Throwable e) {
            auditController.error(AccountBatch, msg, tableName, getPackageId(wrapper), e);
            return new RpcRes_Base<>(wrapper, true, getErrorMessage(e));
        }
    }

    public RpcRes_Base<AccountBatchWrapper> toOpenAccounts(AccountBatchWrapper wrapper) throws Exception {
        AccountBatchPackage pkg = getPackageWithCheck(wrapper, IS_LOAD);
/*      // TODO
        BatchPosting posting0 = postingRepository.getOnePostingByPackageWithStatus(pkg.getId(), INPUT);
        try {
            checkFilialPermission(wrapper.getPkgId(), wrapper.getUserId());
        } catch (ValidationError e) {
            String msg = "Ошибка при передаче на открытие счетов пакета ID = " + wrapper.getPackageId();
            String errMessage = postingController.addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.warning(BatchOperation, msg, new BatchPosting().getTableName(), wrapper.getId().toString(), e);
            return new RpcRes_Base<>( wrapper, true, errMessage);
        }
        checkPostingsStatusNotError(wrapper.getPkgId(), INPUT);
        boolean needHistory = createPackageHistory(pkg, BatchPostStep.HAND1, wrapper.getAction());
        return setPackageRqStatusControl(wrapper, pkg, needHistory);
*/
        wrapper.setPackageState(ON_VALID);
        String msg = "Пакет счетов ID = " + wrapper.getPackageId() + " передан на обработку";
        auditController.info(AccountBatch, msg, tableName, getPackageId(wrapper));
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    private AccountBatchPackage getPackageWithCheck(AccountBatchWrapper wrapper, AccountBatchPackageState... packageState) throws SQLException {
        Long id = wrapper.getPackageId();
        final AccountBatchPackage pkg = packageRepository.findById(id);
        if (null == pkg ) {
            throw new ValidationError(ACCOUNT_BATCH_ERROR, "Не найден пакет с ID = " + id + ". Обновите информацию");
        }
        Set<AccountBatchPackageState> states = new HashSet<AccountBatchPackageState>();
        if (packageState != null) {// !packageState.equals(pkg.getPackageState())) {
            states.addAll(Arrays.asList(packageState));
            if (!states.contains(pkg.getState())) {
                throw new ValidationError(ACCOUNT_BATCH_ERROR,
                        String.format("Пакет счетов ID = %s: нельзя открыть счета пакета в статусе: '%s' ('%s'). Обновите информацию",
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


}
