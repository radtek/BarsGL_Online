package ru.rbt.barsgl.ejb.integr.bg;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.common.repository.od.OperdayRepository;
import ru.rbt.barsgl.ejb.entity.dict.ClosedPeriodView;
import ru.rbt.barsgl.ejb.repository.BackValueOperationRepository;
import ru.rbt.barsgl.ejb.repository.dict.ClosedPeriodCashedRepository;
import ru.rbt.barsgl.ejb.security.UserContext;
import ru.rbt.barsgl.ejbcore.page.SQL;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.criteria.Criteria;
import ru.rbt.barsgl.shared.enums.BackValueMode;
import ru.rbt.barsgl.shared.operation.BackValueWrapper;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.security.ejb.repository.access.SecurityActionRepository;
import ru.rbt.shared.ExceptionUtils;
import ru.rbt.shared.enums.SecurityActionCode;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.BackValueOperation;
import static ru.rbt.barsgl.ejbcore.page.SqlPageSupportBean.prepareCommonSql;
import static ru.rbt.barsgl.shared.enums.BackValueMode.ALL;
import static ru.rbt.barsgl.shared.enums.BackValueMode.ONE;
import static ru.rbt.barsgl.shared.enums.DealSource.withTechWorkDay;
import static ru.rbt.barsgl.shared.enums.OperState.BLOAD;
import static ru.rbt.barsgl.shared.enums.OperState.BWTAC;

/**
 * Created by er18837 on 13.07.2017.
 */
public class BackValuePostingController {

    @EJB
    private AuditController auditController;

    @EJB
    private BackValueOperationRepository bvOperationRepository;

    @EJB
    private ClosedPeriodCashedRepository closedPeriodRepository;

    @Inject
    private BankCalendarDayRepository calendarDayRepository;

    @Inject
    protected OperdayController operdayController;

    @Inject
    SecurityActionRepository actionRepository;

    @Inject
    private UserContext userContext;

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

//    private Date parseDate(String dateStr, SimpleDateFormat format){
//        return ;
//    };

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
    public RpcRes_Base<Integer> authorizeOperations(BackValueWrapper wrapper, Criteria criteria) throws Exception {
        try {
            BackValueMode mode = wrapper.getMode();
            SimpleDateFormat dateFormat = new SimpleDateFormat(wrapper.getDateFormat());

//             TODO
//             проверить, что список однородный (источник, дата, статус) и нет обработанных операций и статус (HOLD / CONTROL)
            String gloidIn = "";
            Object[] paramsIn = null;
            if (ALL == wrapper.getMode()) {
                // сформировать запрос по фильтру
                SQL formSql = prepareCommonSql(wrapper.getSql(), criteria);
                gloidIn = "select GLOID from (" + formSql.getQuery() + ")";
                paramsIn = formSql.getParams();
            } else {
                // сформировать запрос по списку
                gloidIn = StringUtils.listToString(wrapper.getGloIDs(), ", ");
                paramsIn = new Object[0];
            }
            String stateIn = StringUtils.arrayToString(new Object[] {BLOAD, BWTAC}, ", ", "'");
            String from = " from GL_OPER o join GL_OPEREXT e on o.GLOID = e.GLOID " +
                    " where GLOID in (" + gloidIn + ") and o.STATE in (" + stateIn + ")";
            // получить количество и postDatePlan операции
            List<DataRecord> dictinct = bvOperationRepository.select("select distinct o.VDATE, e.POSTDATE_PLAN, o.SRC_PST, e.MNL_STATUS " + from, paramsIn);
            if (dictinct.isEmpty()) {
                throw new DefaultApplicationException("По заданному условию операции не найдены");
            }
            else if (dictinct.size() != 1) {
                throw new DefaultApplicationException("По заданному условию найдены операции с различным набором параметров (источник, дата проводки, статус)");
            }
            DataRecord data = bvOperationRepository.selectFirst("select count(1) " + from, paramsIn);
            int count = data.getInteger(0);
            if (mode != ALL && wrapper.getGloIDs().size() != count) {
                throw new DefaultApplicationException(String.format("По заданному условию найдено %d операций, ожидалось %d", count, wrapper.getGloIDs().size()));
            }
            Date valueDate = dictinct.get(0).getDate(0);
            Date postDatePlan = dictinct.get(0).getDate(1);
            String sourcePosting = dictinct.get(0).getString(2);
            boolean withTech = withTechWorkDay(sourcePosting);
            Date postDateNew = dateFormat.parse(wrapper.getPostDateStr());

            // проверить postDateNew на выходной день
            if(!calendarDayRepository.isWorkday(postDateNew, withTech)) {
                throw new DefaultApplicationException(String.format("Подтверждение запрещено.\nВыбранная дата проводки '%s' – выходной"
                        , wrapper.getPostDateStr()));   // добавить дату валютирования и плановую

            }

            // проверить postDateNew на допустимый диапазон
            Date postDateMin = calendarDayRepository.isWorkday(valueDate, withTech)
                        ? valueDate
                        : calendarDayRepository.getWorkDateBefore(valueDate, withTech);
            Date postDateMax = operdayController.getOperday().getCurrentDate();
            if (postDateNew.before(postDateMin) || postDateNew.after(postDateMax)) {
                throw new DefaultApplicationException(String.format("Подтверждение запрещено.\n" +
                        "Дата проводки '%s' вышла за пределы допустимого диапазона с '%s' по '%s'"
                        , wrapper.getPostDateStr(), dateFormat.format(postDateMin), dateFormat.format(postDateMax)));
            }

            // проверить postDateNew на закрытый период
            ClosedPeriodView period = closedPeriodRepository.getPeriod();
            if(!postDateNew.after(period.getLastDate()) &&                // разрешено только для суперпользователя
                !actionRepository.getAvailableActions(userContext.getUserId()).contains(SecurityActionCode.Acc707Inp)) {
                throw new DefaultApplicationException(String.format("Подтверждение запрещено.\n" +
                                "Дата проводки '%s' попала в отчетный период до '%s', который зарыт '%s'"
                        , wrapper.getPostDateStr(), dateFormat.format(period.getLastDate()), dateFormat.format(period.getCutDate())));
            }
//             в одной транзакции {
//                если postDatePlan <> postDateNew
//                    изменить GL_OPER.POSTDATE = postDate
//                изменить GL_OPEREXT: MNL_STATUS = SIGNEDDATE, USER_AU3, OTS_AU3
//             }
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
