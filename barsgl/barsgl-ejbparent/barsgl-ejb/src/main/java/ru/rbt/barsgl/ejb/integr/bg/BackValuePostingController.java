package ru.rbt.barsgl.ejb.integr.bg;

import org.apache.commons.lang3.ArrayUtils;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.entity.dict.ClosedPeriodView;
import ru.rbt.barsgl.ejb.repository.BackValueOperationRepository;
import ru.rbt.barsgl.ejb.repository.dict.ClosedPeriodCashedRepository;
import ru.rbt.barsgl.ejb.security.UserContext;
import ru.rbt.barsgl.ejbcore.page.SQL;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.criteria.Criteria;
import ru.rbt.barsgl.shared.enums.BackValueAction;
import ru.rbt.barsgl.shared.enums.BackValueMode;
import ru.rbt.barsgl.shared.operation.BackValueWrapper;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.security.ejb.repository.access.SecurityActionRepository;
import ru.rbt.shared.ExceptionUtils;
import ru.rbt.shared.enums.SecurityActionCode;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.io.Serializable;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.BackValueOperation;
import static ru.rbt.barsgl.ejbcore.page.SqlPageSupportBean.prepareCommonSql;
import static ru.rbt.barsgl.shared.enums.BackValueAction.SIGN;
import static ru.rbt.barsgl.shared.enums.BackValueAction.TO_HOLD;
import static ru.rbt.barsgl.shared.enums.BackValueMode.ALL;
import static ru.rbt.barsgl.shared.enums.DealSource.withTechWorkDay;
import static ru.rbt.barsgl.shared.enums.OperState.BLOAD;
import static ru.rbt.barsgl.shared.enums.OperState.BWTAC;

/**
 * Created by er18837 on 13.07.2017.
 */
public class BackValuePostingController {

    final private String operStateNotAuth = StringUtils.arrayToString(new Object[] {BLOAD, BWTAC}, ", ", "'");;
    final private BackValueAction[] actionNotAuth = new BackValueAction[]{SIGN, TO_HOLD};

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

    @Inject
    private DateUtils dateUtils;

    public RpcRes_Base<Integer> processOperationBv(BackValueWrapper wrapper, Criteria criteria) throws Exception {
        try {

            switch (wrapper.getAction()) {
                case SIGN:              //("Подтвердить дату"),
                    return authorizeOperations(wrapper, criteria);
                case TO_HOLD:           //("Задержать до выяснения"),
                    return holdOperations(wrapper, criteria);
                case STAT:              //("Статистика")
                    return getStatistics(wrapper, criteria);
                case EDIT_DATE:         //("Изменить дату проводки"),
                    return editDateOperation(wrapper);
            }
            return new RpcRes_Base<>(0, true, "Неверное действие");
//            return new RpcRes_Base<Integer>(0, true, "Сформирован запрос:\n" + getSqlString(wrapper, criteria));

        } catch (Throwable t) {
            String msg = getErrorMessage(t);
            if (null != ExceptionUtils.findException(t, ValidationError.class)) {
                auditController.warning(BackValueOperation, msg);
                msg = ValidationError.getErrorText(msg);
            }
            else {
                auditController.error(BackValueOperation, getErrorMessage(t), null, t);
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

            // сформировать запрос
            // проверить, что список однородный (источник, дата, статус) и нет обработанных операций и статус (HOLD / CONTROL)
            // получить ключевые параметры операций
            OperationParameters parameters = getOperationParameters(wrapper, criteria);

            // проверить postDateNew на выходной день и на допустимый диапазон
            checkPostDate(wrapper, parameters);

            // проверить postDateNew на закрытый период
            chackClosedPeriod(wrapper, parameters);

            bvOperationRepository.executeInNewTransaction(persistence -> {
                int cnt = 0;
                if (!parameters.getPostDate().equals(parameters.getPostDateNew())) {
                    // изменить GL_OPER.POSTDATE = postDate
                    cnt = bvOperationRepository.updatePostDate(parameters.getPostDateNew(), parameters.getPostDate(), operStateNotAuth,
                            parameters.getGloidIn(), parameters.getSqlParams());
                    if (cnt != parameters.getOperCount()) {
                        throw new DefaultApplicationException(format("Не удалось изменить дату проводки, обновлено записей %d, ожидалось %d", cnt, parameters.getOperCount()));
                    }
                }
                // изменить GL_OPEREXT: MNL_STATUS = SIGNEDDATE, USER_AU3, OTS_AU3
                cnt = bvOperationRepository.updateOperationsSigneddate(wrapper.getBvStatus(), userContext.getUserName(), operdayController.getSystemDateTime(),
                        parameters.getGloidIn(), parameters.getSqlParams());
                if (cnt != parameters.getOperCount()) {
                    throw new DefaultApplicationException(format("Не удалось авторизовать операции: обновлено записей %d, ожидалось %d", cnt, parameters.getOperCount()));
                }
                return null;
            });
            String message = getResultMessage(wrapper, parameters.getOperCount(), "авторизована", "авторизованы");
            auditController.info(BackValueOperation, message);
            return new RpcRes_Base<>(parameters.getOperCount(), false, message);
        } catch (ValidationError e) {
            String msg = "Ошибка при авторизации: " + getResultMessage(wrapper, 0);
            auditController.error(BackValueOperation, msg, null, null, e);  // TODO Таблица и ID
            return new RpcRes_Base<>(0, true, getErrorMessage(e));
        }
    }

    public RpcRes_Base<Integer> holdOperations(BackValueWrapper wrapper, Criteria criteria) throws Exception {
        try {
             // сформировать запрос (по списку или фильтру)
             // проверить, что список однородный и нет обработанных операций и статус = CONTROL
            OperationParameters parameters = getOperationParameters(wrapper, criteria);
            bvOperationRepository.executeInNewTransaction(persistence -> {
                int cnt = 0;
                // изменить GL_OPEREXT: MNL_STATUS = HOLD, MNL_NARRATIVE = wrapper.comment, USER_AU3, OTS_AU3
                cnt = bvOperationRepository.updateOperationsHold(parameters.getGloidIn(), wrapper.getComment(),
                        userContext.getUserName(), operdayController.getSystemDateTime(), parameters.getSqlParams());
                if (cnt != parameters.getOperCount()) {
                    throw new DefaultApplicationException(format("Не удалось изменить статус операций: обновлено записей %d, ожидалось %d", cnt, parameters.getOperCount()));
                }
                return null;
            });
            String message = getResultMessage(wrapper, parameters.getOperCount(), "задержана", "задержаны") + " до выяснения";
            auditController.info(BackValueOperation, message);
            return new RpcRes_Base<>(parameters.getOperCount(), false, message);
        } catch (ValidationError e) {
            String msg = "Ошибка при задержании: " + getResultMessage(wrapper, 0);
            auditController.error(BackValueOperation, msg, null, null, e);  // TODO Таблица и ID
            return new RpcRes_Base<>(0, true, getErrorMessage(e));
        }
    }

    public RpcRes_Base<Integer> getStatistics(BackValueWrapper wrapper, Criteria criteria) throws Exception {
        try {
            OperationParameters parameters = getOperationParameters(wrapper, criteria);
            String message = format("Всего операций по условию: %d", parameters.getOperCount());
            auditController.info(BackValueOperation, message);
            return new RpcRes_Base<>(parameters.getOperCount(), false, message);
        } catch (ValidationError e) {
            String msg = "Ошибка при получении статистики: " + getResultMessage(wrapper, 0);
            auditController.error(BackValueOperation, msg, null, null, e);  // TODO Таблица и ID
            return new RpcRes_Base<>(0, true, getErrorMessage(e));
        }
    }

    public RpcRes_Base<Integer> editDateOperation(BackValueWrapper wrapper) throws Exception {
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
            String message = getResultMessage(wrapper, 1) + ": изменена дата";
            return new RpcRes_Base<>(1, false, message);
        } catch (ValidationError e) {
            String msg = "Ошибка при изменении даты: " + getResultMessage(wrapper, 0);
            auditController.error(BackValueOperation, msg, null, null, e);  // TODO Таблица и ID
            return new RpcRes_Base<>(0, true, getErrorMessage(e));
        }
    }

    public String getResultMessage(BackValueWrapper wrapper, int count) {
        return getResultMessage(wrapper, count, "", "");
    }

    public String getResultMessage(BackValueWrapper wrapper, int count, String one, String many) {
        List<Long> gloIDs = wrapper.getGloIDs();
        String res = (count == 1) ? one : many;
        switch (wrapper.getMode()) {
            case ONE:
                return format("Операция ID = %d ", gloIDs.isEmpty() ? "" : gloIDs.get(0)) + res;
            case VISIBLE:
                return format("Операции в количестве %d ", count) + res;
            case ALL:       // TODO расшифровка фильтра
                return format("Операции по фильтру в количестве %d ", count) + res;
            default:
                return "Неверный режим обработки";
        }
    }

    public String getErrorMessage(Throwable throwable) {
            return ExceptionUtils.getErrorMessage(throwable,
                    ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, DefaultApplicationException.class,
                    PersistenceException.class);
    }

    /**
     * проверяет совместимость списка операций
     * @param wrapper
     * @param criteria
     * @throws SQLException
     */
    private OperationParameters getOperationParameters(BackValueWrapper wrapper, Criteria criteria) throws SQLException, ParseException {
        OperationParameters parameters = new OperationParameters();
        if (ALL == wrapper.getMode()) {
            // сформировать запрос по фильтру
            SQL formSql = prepareCommonSql(wrapper.getSql(), criteria);
            parameters.setGloidIn("select t.GLOID from (" + formSql.getQuery() + ") t ");
            parameters.setSqlParams(formSql.getParams());
        } else {
            // сформировать запрос по списку
            parameters.setGloidIn(StringUtils.listToString(wrapper.getGloIDs(), ", "));
            parameters.setSqlParams(new Object[0]);
        }
        boolean notAuthorized = ArrayUtils.contains(actionNotAuth, wrapper.getAction());
        String from = " from GL_OPER o join GL_OPEREXT e on o.GLOID = e.GLOID " +
                " where o.GLOID in (" + parameters.getGloidIn() + ")" +
                (notAuthorized ? " and o.STATE in (" + operStateNotAuth + ")" : "");

        // получить общие параметры операций
        List<DataRecord> dictinct = bvOperationRepository.select("select distinct o.VDATE, o.POSTDATE, e.POSTDATE_PLAN, o.SRC_PST, e.MNL_STATUS "
                + from, parameters.getSqlParams());
        if (dictinct.isEmpty()) {
            throw new DefaultApplicationException("По заданному условию операции не найдены");
        }
        else if (dictinct.size() != 1) {
            throw new DefaultApplicationException("По заданному условию найдены операции с различным набором параметров (источник, дата проводки, статус)");
        }
        DataRecord rec = dictinct.get(0);
        String status = rec.getString(4);
        if (!wrapper.getBvStatus().name().equals(status)) {
            throw new DefaultApplicationException(format("Статус операции изменен: '%s', ожидается '%s'. Обновите список операций",
                    status, wrapper.getBvStatus().name()) );
        }

        parameters.setValueDate(rec.getDate(0));
        parameters.setPostDate(rec.getDate(1));
        parameters.setPostDatePlan(rec.getDate(2));
        parameters.setSourcePosting(rec.getString(3));
        parameters.setPostDateNew(new SimpleDateFormat(wrapper.getDateFormat()).parse(wrapper.getPostDateStr()));

        // сравнить количество
        DataRecord data = bvOperationRepository.selectFirst("select count(1) " + from, parameters.getSqlParams());
        int count = data.getInteger(0);
        if (wrapper.getMode() != ALL && wrapper.getGloIDs().size() != count) {
            throw new DefaultApplicationException(String.format("По заданному условию найдено %d операций, ожидалось %d", count, wrapper.getGloIDs().size()));
        }
        parameters.setOperCount(count);

        return parameters;
    }

    private void checkPostDate(BackValueWrapper wrapper, OperationParameters parameters) throws SQLException {
        Date valueDate = parameters.getValueDate();
        Date postDateNew = parameters.getPostDateNew();
        boolean withTech = withTechWorkDay(parameters.getSourcePosting());

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
                    , wrapper.getPostDateStr(), dateUtils.onlyDateString(postDateMin), dateUtils.onlyDateString(postDateMax)));
        }

    }

    private void chackClosedPeriod(BackValueWrapper wrapper, OperationParameters parameters) {
        ClosedPeriodView period = closedPeriodRepository.getPeriod();
        Long userId = wrapper.getUserId();  // TODO userContext.getUserId()
        if(!parameters.getPostDateNew().after(period.getLastDate()) &&                // разрешено только для суперпользователя
                !actionRepository.getAvailableActions(userId).contains(SecurityActionCode.OperHand3Super)) {
            throw new DefaultApplicationException(String.format("Подтверждение запрещено.\n" +
                            "Дата проводки '%s' попала в отчетный период до '%s', который закрыт '%s'"
                    , wrapper.getPostDateStr(), dateUtils.onlyDateString(period.getLastDate()), dateUtils.onlyDateString(period.getCutDate())));
        }
    }

    private class OperationParameters implements Serializable{

        private String sourcePosting;           // источник
        private Date valueDate;                 // дата валютирования
        private Date postDate;                  // дата проводки
        private Date postDatePlan;              // плановая дата проводки
        private Date postDateNew;               // заданная дата проводки

        private String gloidIn;
        private Object[] sqlParams;
        private int operCount;

        public OperationParameters() {
            operCount = 0;
        }

        public String getSourcePosting() {
            return sourcePosting;
        }

        public void setSourcePosting(String sourcePosting) {
            this.sourcePosting = sourcePosting;
        }

        public Date getValueDate() {
            return valueDate;
        }

        public void setValueDate(Date valueDate) {
            this.valueDate = valueDate;
        }

        public Date getPostDate() {
            return postDate;
        }

        public void setPostDate(Date postDate) {
            this.postDate = postDate;
        }

        public Date getPostDatePlan() {
            return postDatePlan;
        }

        public void setPostDatePlan(Date postDatePlan) {
            this.postDatePlan = postDatePlan;
        }

        public Date getPostDateNew() {
            return postDateNew;
        }

        public void setPostDateNew(Date postDateNew) {
            this.postDateNew = postDateNew;
        }

        public String getGloidIn() {
            return gloidIn;
        }

        public void setGloidIn(String gloidIn) {
            this.gloidIn = gloidIn;
        }

        public Object[] getSqlParams() {
            return sqlParams;
        }

        public void setSqlParams(Object[] sqlParams) {
            this.sqlParams = sqlParams;
        }

        public int getOperCount() {
            return operCount;
        }

        public void setOperCount(int operCount) {
            this.operCount = operCount;
        }
    }
}
