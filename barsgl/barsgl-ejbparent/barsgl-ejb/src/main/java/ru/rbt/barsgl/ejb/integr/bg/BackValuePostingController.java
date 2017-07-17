package ru.rbt.barsgl.ejb.integr.bg;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejbcore.page.SQL;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.criteria.Criteria;
import ru.rbt.barsgl.shared.operation.BackValueWrapper;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.ExceptionUtils;

import javax.ejb.EJB;
import javax.persistence.PersistenceException;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.BackValueOperation;
import static ru.rbt.barsgl.ejbcore.page.SqlPageSupportBean.prepareCommonSql;

/**
 * Created by er18837 on 13.07.2017.
 */
public class BackValuePostingController {

    @EJB
    private AuditController auditController;

    public RpcRes_Base<Integer> processOperationBv(BackValueWrapper wrapper, Criteria criteria) throws Exception {
        try {
/*
            switch (wrapper.getAction()) {
                case SIGN:              //("Подтвердить дату"),
                    return authorizeOperations(wrapper);
                case TO_HOLD:           //("Задержать до выяснения"),
                    return holdOperations(wrapper);
                case EDIT_DATE:         //("Изменить дату проводки"),
                    return editDateOperation(wrapper);
                case STAT:              //("Статистика")
                    return getStatistics(wrapper);
            }
            return new RpcRes_Base<>(wrapper, true, "Неверное действие");
            }
*/

            return new RpcRes_Base<Integer>(0, true, "Сформирован запрос:\n" + getSqlString(wrapper, criteria));

        } catch (Throwable t) {
            String msg = getErrorMessage(t);
            if (null != ExceptionUtils.findException(t, ValidationError.class)) {
                auditController.warning(AuditRecord.LogCode.ReprocessAEOper, msg);
                msg = ValidationError.getErrorText(msg);
            }
            else {
                auditController.error(AuditRecord.LogCode.ReprocessAEOper, getErrorMessage(t), null, t);
            }
            return new RpcRes_Base<Integer>(0, true, msg);
        }
    }

    private String getSqlString(BackValueWrapper wrapper, Criteria criteria) {
        SQL sql = prepareCommonSql(wrapper.getSql(), criteria);
        return sql.getQuery();
    }

    /**
     * авторизауия одной или списка операций с возможным изменением даты
     * @param wrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<Integer> authorizeOperations(BackValueWrapper wrapper) throws Exception {
        try {
            /**
             TODO
             сформировать запрос (по списку или фильтру)
             проверить, что список однородный и нет обработанных операций и статус (HOLD / CONTROL)
             получить количество и postDatePlan операции
             получить postDateNew из wrapper.postDateStr
             поверить postDateNew >= valueDate && postDateNew <= postDatePlan
             если postDateNew попадает в закрытый период
                проверить права суперпользователя
             в одной транзакции {
                если postDatePlan <> postDateNew
                    изменить GL_OPER.POSTDATE = postDate
                изменить GL_OPEREXT: MNL_STATUS = SIGNEDDATE, USER_AU3, OTS_AU3
             }
             */
            String message = getWrapperID(wrapper, 1) + " авторизована";
            return new RpcRes_Base<Integer>(2, false, message);
        } catch (ValidationError e) {
            String msg = "Ошибка при авторизации " + getWrapperID(wrapper, 1);
            auditController.error(BackValueOperation, msg, null, null, e);  // TODO Таблица и ID
            return new RpcRes_Base<Integer>(1, true, getErrorMessage(e));
        }
    }

    public RpcRes_Base<BackValueWrapper> holdOperations(BackValueWrapper wrapper) throws Exception {
        try {
            /**
             TODO
             сформировать запрос (по списку или фильтру)
             проверить, что список однородный и нет обработанных операций и статус = CONTROL
             в одной транзакции {
                 изменить GL_OPEREXT: MNL_STATUS = HOLD, MNL_NARRATIVE = wrapper.comment, USER_AU3, OTS_AU3
             }
             */
            String message = getWrapperID(wrapper, 1) + " задержана";
            return new RpcRes_Base<>(wrapper, false, message);
        } catch (ValidationError e) {
            String msg = "Ошибка при задержании " + getWrapperID(wrapper, 1);
            auditController.error(BackValueOperation, msg, null, null, e);  // TODO Таблица и ID
            return new RpcRes_Base<>(wrapper, true, getErrorMessage(e));
        }
    }

    public RpcRes_Base<BackValueWrapper> editDateOperation(BackValueWrapper wrapper) throws Exception {
        try {
            /**
             TODO
             проверить возможность редактирования - ONLINE ALLOWED;
             получить postDatePlan операции
             получить postDateNew из wrapper.postDateStr
             поверить postDateNew >= valueDate && postDateNew <= postDatePlan
             если postDateNew попадает в закрытый период
                проверить права суперпользователя
             обратиться к EditPostingController
             }
             */
            String message = getWrapperID(wrapper, 1) + ": изменена дата";
            return new RpcRes_Base<>(wrapper, false, message);
        } catch (ValidationError e) {
            String msg = "Ошибка при изменении даты " + getWrapperID(wrapper, 1);
            auditController.error(BackValueOperation, msg, null, null, e);  // TODO Таблица и ID
            return new RpcRes_Base<>(wrapper, true, getErrorMessage(e));
        }
    }

    public RpcRes_Base<BackValueWrapper> getStatistics(BackValueWrapper wrapper) throws Exception {
        try {
            String message = "Всего операций: \nУсловие: ";
            return new RpcRes_Base<>(wrapper, false, message);
        } catch (ValidationError e) {
            String msg = "Ошибка при получении статистики" + getWrapperID(wrapper, 1);
            auditController.error(BackValueOperation, msg, null, null, e);  // TODO Таблица и ID
            return new RpcRes_Base<>(wrapper, true, getErrorMessage(e));
        }
    }

    public String getWrapperID(BackValueWrapper wrapper, int count) {
        List<Long> gloIDs = wrapper.getGloIDs();
        switch (wrapper.getMode()) {
            case ONE:       return format("Операция ID = %d ", gloIDs.isEmpty() ? "" : gloIDs.get(0));
            case VISIBLE:   return format("Операции в количестве %d ", gloIDs.size());
            // TODO расшифровка фильтра и количество операций
            case ALL:       return format("Операции по фильтру в количестве %d ", count);
        }
        return "Неверный режим обработки";
    }

        public String getErrorMessage(Throwable throwable) {
            return ExceptionUtils.getErrorMessage(throwable,
                    ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, DefaultApplicationException.class,
                    PersistenceException.class);
        }

    }
