package ru.rbt.barsgl.ejb.integr.bg;

import org.apache.commons.lang3.ArrayUtils;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.entity.dict.ClosedPeriodView;
import ru.rbt.barsgl.ejb.entity.gl.AbstractPd;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.oper.EditPostingGLPdProcessor;
import ru.rbt.barsgl.ejb.integr.oper.EditPostingPdProcessor;
import ru.rbt.barsgl.ejb.integr.oper.EditPostingProcessor;
import ru.rbt.barsgl.ejb.repository.BackValueOperationRepository;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.barsgl.ejb.repository.dict.ClosedPeriodCashedRepository;
import ru.rbt.barsgl.ejb.security.UserContext;
import ru.rbt.barsgl.ejbcore.page.SQL;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.criteria.Criteria;
import ru.rbt.barsgl.shared.enums.*;
import ru.rbt.barsgl.shared.operation.BackValueWrapper;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.gwt.security.ejb.repository.access.AccessServiceSupport;
import ru.rbt.security.ejb.repository.access.SecurityActionRepository;
import ru.rbt.shared.Assert;
import ru.rbt.shared.ExceptionUtils;
import ru.rbt.shared.enums.SecurityActionCode;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import javax.validation.ValidationException;
import java.io.Serializable;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.BackValueOperation;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejbcore.page.SqlPageSupportBean.prepareCommonSql;
import static ru.rbt.barsgl.shared.enums.BackValueAction.SIGN;
import static ru.rbt.barsgl.shared.enums.BackValueAction.TO_HOLD;
import static ru.rbt.barsgl.shared.enums.BackValueMode.ALL;
import static ru.rbt.barsgl.shared.enums.BackValueMode.ONE;
import static ru.rbt.barsgl.shared.enums.BackValuePostStatus.CONTROL;
import static ru.rbt.barsgl.shared.enums.BackValuePostStatus.HOLD;
import static ru.rbt.barsgl.shared.enums.DealSource.withTechWorkDay;
import static ru.rbt.barsgl.shared.enums.OperState.BLOAD;
import static ru.rbt.barsgl.shared.enums.OperState.BWTAC;
import static ru.rbt.barsgl.shared.enums.OperState.POST;
import static ru.rbt.barsgl.shared.enums.PostingChoice.PST_ONE_OF;
import static ru.rbt.ejbcore.validation.ErrorCode.BV_MANUAL_ERROR;
import static ru.rbt.ejbcore.validation.ValidationError.initSource;

/**
 * Created by er18837 on 13.07.2017.
 */
public class BackValuePostingController {

    final private String operStateAuth = format("'%s'", POST.name());
    final private String operStateNotAuth = StringUtils.arrayToString(new Object[] {BLOAD, BWTAC}, ", ", "'");
    final private BackValueAction[] actionNotAuth = new BackValueAction[]{SIGN, TO_HOLD};

    @EJB
    private AuditController auditController;

    @EJB
    private BackValueOperationRepository bvOperationRepository;

    @Inject
    private GLOperationRepository operationRepository;

    @EJB
    private ClosedPeriodCashedRepository closedPeriodRepository;

    @Inject
    private BankCalendarDayRepository calendarDayRepository;

    @Inject
    protected OperdayController operdayController;

    @Inject
    private EditPostingPdProcessor editPdProcessor;

    @Inject
    private EditPostingGLPdProcessor editGLPdProcessor;

    @Inject
    private ManualPostingController manualPostingController;

    @Inject
    SecurityActionRepository actionRepository;

    @Inject
    private AccessServiceSupport accessServiceSupport;

    @Inject
    private UserContext userContext;

    @Inject
    private DateUtils dateUtils;

    public RpcRes_Base<Integer> processOperationBv(BackValueWrapper wrapper, Criteria criteria) throws Exception {
        try {
            if (null == wrapper.getUserId())
                wrapper.setUserId(userContext.getUserId());

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

        } catch (Throwable t) {
            auditController.error(BackValueOperation, "Ошибка при ручной обработке операций BackValue", null, t);
            return new RpcRes_Base(0, true, getErrorMessage(t));
        }
    }

    /**
     * авторизауия одной или списка операций с возможным изменением даты
     * @param wrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<Integer> authorizeOperations(BackValueWrapper wrapper, Criteria criteria) throws Exception {
        try {
            // получить параметры операций
            OperationParameters parameters = getOperationsParameters(wrapper, criteria);

            // проверить корректность статуса
            checkOperationStatus(wrapper, parameters, CONTROL, HOLD);

            // проверить postDateNew на выходной день и на допустимый диапазон
            checkPostDate(parameters.getSourcePosting(), parameters.getPostDateNew(), parameters.getValueDate());

            // проверить postDateNew на закрытый период
            checkClosedPeriod(wrapper.getUserId(), parameters.getPostDateNew());

            // проверить права доступа к прошлой дате
            checkUserAccessToBackValue(wrapper.getUserId(), parameters.getPostDateNew(), parameters.getPostDate());

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
            auditController.error(BackValueOperation, msg, null, e.getMessage(), e);  // TODO Таблица и ID
            return new RpcRes_Base<>(0, true, ValidationError.getErrorText(e.getMessage()));
        }
    }

    public RpcRes_Base<Integer> holdOperations(BackValueWrapper wrapper, Criteria criteria) throws Exception {
        try {
            // получить параметры операций
            OperationParameters parameters = getOperationsParameters(wrapper, criteria);

            // проверить корректность статуса
            checkOperationStatus(wrapper, parameters, CONTROL);

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
            auditController.error(BackValueOperation, msg, null, e.getMessage(), e);  // TODO Таблица и ID
            return new RpcRes_Base<>(0, true, ValidationError.getErrorText(e.getMessage()));
        }
    }

    public RpcRes_Base<Integer> getStatistics(BackValueWrapper wrapper, Criteria criteria) throws Exception {
        try {
            OperationParameters parameters = createOperationsParameters(wrapper, criteria);
            DataRecord data = bvOperationRepository.selectFirst("select count(1) " + parameters.getFrom(), parameters.getSqlParams());
            parameters.setOperCount(data.getInteger(0));
            String message = format("Всего операций по условию: %d", parameters.getOperCount());
            auditController.info(BackValueOperation, message);
            return new RpcRes_Base<>(parameters.getOperCount(), false, message);
        } catch (ValidationError e) {
            String msg = "Ошибка при получении статистики: " + getResultMessage(wrapper, 0);
            auditController.error(BackValueOperation, msg, null, e.getMessage(), e);  // TODO Таблица и ID
            return new RpcRes_Base<>(0, true, ValidationError.getErrorText(e.getMessage()));
        }
    }

    public RpcRes_Base<Integer> editDateOperation(BackValueWrapper wrapper) throws Exception {

        Operday operday = operdayController.getOperday();
        if (ONLINE != operday.getPhase()) {
            throw new ValidationError(ErrorCode.OPERDAY_NOT_ONLINE, operdayController.getOperday().getPhase().name());
        }

        if ( !operdayController.isProcessingAllowed()) {
            throw new ValidationError(ErrorCode.OPERDAY_IN_SYNCHRO);
        }

        try {
            // получить параметры операций
            OperationParameters parameters = getOperationParameters(wrapper);

            // проверить postDateNew на выходной день и на допустимый диапазон
            checkPostDate(parameters.getSourcePosting(), parameters.getPostDateNew(), parameters.getValueDate());

            // проверить postDateNew на закрытый период
            checkClosedPeriod(wrapper.getUserId(), parameters.getPostDateNew());

            // проверить права доступа к прошлой дате
            checkUserAccessToBackValue(wrapper.getUserId(), parameters.getPostDateNew(), parameters.getPostDate());

            List<? extends AbstractPd> pdList = updatePostingsDate(wrapper, parameters);
            String message = String.format("%s: изменена дата проводки в полупроводках с ID %s \n(была: '%s', стала: '%s')",
                    getResultMessage(wrapper, 1),
                    StringUtils.listToString(pdList, ","),
                    dateUtils.onlyDateString(parameters.getPostDate()), wrapper.getPostDateStr());
            return new RpcRes_Base<>(1, false, message);
        } catch (ValidationError e) {
            String msg = "Ошибка при изменении даты: " + getResultMessage(wrapper, 0);
            auditController.error(BackValueOperation, msg, null, e.getMessage(), e);  // TODO Таблица и ID
            return new RpcRes_Base<>(0, true, ValidationError.getErrorText(e.getMessage()));
        }
    }

    /**
     * изменение даты проводки с формы BackValue (проверка корректности даты проводки  - снаружи)
     * @return
     * @throws Exception
     */
    public List<? extends AbstractPd> updatePostingsDate(BackValueWrapper wrapper, OperationParameters parameters) throws Exception {
        // заполнить ManualOperationWrapper как при редактировании проводки
        ManualOperationWrapper operationWrapper = new ManualOperationWrapper();
        operationWrapper.setPostingChoice(PostingChoice.PST_SINGLE);
        operationWrapper.setId(parameters.getParentId());
        operationWrapper.setPostDateStr(wrapper.getPostDateStr());
        operationWrapper.setCorrection(YesNo.Y.name().equals(parameters.getIsCorrection()));

        EditPostingProcessor editPostingProcessor = editGLPdProcessor;
        List<Long> pdIdList = editPostingProcessor.getOperationPdIdList(operationWrapper.getId());
        if (pdIdList.isEmpty()) {
            editPostingProcessor = editPdProcessor;
            pdIdList = editPostingProcessor.getOperationPdIdList(operationWrapper.getId());
        }
        Assert.isTrue(null != pdIdList && !pdIdList.isEmpty(),
                ()-> new ValidationError(BV_MANUAL_ERROR, format("Для операции '%d' не найдено ни одной проводки", operationWrapper.getId())));

        List<? extends AbstractPd> pdList = editPostingProcessor.getOperationPdList(pdIdList);

        editPostingProcessor.updateWithMemorder(operationWrapper, pdList, isBufferMode());
        editPostingProcessor.updatePd(pdList);

        operationRepository.updateOperationParentPostDate(operationWrapper.getId(), parameters.getPostDateNew());

        return editPostingProcessor.getOperationPdList(pdIdList);
    }

    private boolean isBufferMode() {
        return operdayController.getOperday().getPdMode().equals(Operday.PdMode.BUFFER);
    }

    public String getResultMessage(BackValueWrapper wrapper, int count) {
        return getResultMessage(wrapper, count, "", "");
    }

    public String getResultMessage(BackValueWrapper wrapper, int count, String one, String many) {
        List<Long> gloIDs = wrapper.getGloIDs();
        switch (wrapper.getMode()) {
            case ONE:
                return format("Операция ID = %d ", gloIDs.isEmpty() ? "" : gloIDs.get(0)) + one;
            case VISIBLE:
                return format("Операции в количестве %d ", count) + many;
            case ALL:       // TODO расшифровка фильтра
                return format("Операции по фильтру в количестве %d ", count) + many;
            default:
                return "Неверный режим обработки";
        }
    }

    public String getErrorMessage(Throwable throwable) {
            return ExceptionUtils.getErrorMessage(throwable,
                    ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, DefaultApplicationException.class,
                    PersistenceException.class);
    }

    private OperationParameters createOperationsParameters(BackValueWrapper wrapper, Criteria criteria) {
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

        // from ... where ...
        parameters.setNotAuthorized(ArrayUtils.contains(actionNotAuth, wrapper.getAction()));
        parameters.setFrom(format(" from GL_OPER o join GL_OPEREXT e on o.GLOID = e.GLOID where o.GLOID in (%s)%s", parameters.getGloidIn(),
                parameters.isNotAuthorized() ? " and o.STATE in (" + operStateNotAuth + ")" : ""));

        return parameters;
    }

    /**
     * получает основные параметры операций
     * проверяет однородность списка операций для неавторизованных
     * @param wrapper
     * @param criteria
     * @throws SQLException
     */
    private OperationParameters getOperationsParameters(BackValueWrapper wrapper, Criteria criteria) throws SQLException, ParseException {
        OperationParameters parameters = createOperationsParameters(wrapper, criteria);

        // получить общие параметры операций
        List<DataRecord> commonParams = bvOperationRepository.select("select distinct o.SRC_PST, o.VDATE, o.POSTDATE, e.POSTDATE_PLAN, e.MNL_STATUS "
                + parameters.getFrom(), parameters.getSqlParams());

        String inStatus = parameters.isNotAuthorized() ? (" в статусе " + operStateNotAuth) : "";
        if (commonParams.isEmpty()) {
            throw new ValidationError(BV_MANUAL_ERROR,
                    format("По заданному условию операции%s не найдены", inStatus));
        }
        else if (commonParams.size() != 1) {
            throw new ValidationError(BV_MANUAL_ERROR,
                    "По заданному условию найдены операции с различным набором параметров (источник, дата проводки, статус)");
        }
        DataRecord rec = commonParams.get(0);
        parameters.setSourcePosting(rec.getString(0));
        parameters.setValueDate(rec.getDate(1));
        parameters.setPostDate(rec.getDate(2));
        parameters.setPostDatePlan(rec.getDate(3));
        parameters.setBvStatus(BackValuePostStatus.valueOf(rec.getString(4)));

        parameters.setPostDateNew(new SimpleDateFormat(wrapper.getDateFormat()).parse(wrapper.getPostDateStr()));

        // сравнить количество
        DataRecord data = bvOperationRepository.selectFirst("select count(1) " + parameters.getFrom(), parameters.getSqlParams());
        int count = data.getInteger(0);
        if (wrapper.getMode() != ALL && wrapper.getGloIDs().size() != count) {
            throw new ValidationError(BV_MANUAL_ERROR,
                    format("По заданному условию найдено %d операций%s, ожидалось %d", count, inStatus, wrapper.getGloIDs().size()));
        }
        parameters.setOperCount(count);

        return parameters;
    }

    /**
     * получает основные параметры операции (при редактировании)
     * @param wrapper
     * @throws SQLException
     */
    private OperationParameters getOperationParameters(BackValueWrapper wrapper) throws SQLException, ParseException {
        if (ONE != wrapper.getMode()) {
            throw new ValidationError(BV_MANUAL_ERROR, format("Неверный режим обработки операций - ожидается '%s'", ONE.name()));
        }

        OperationParameters parameters = createOperationsParameters(wrapper, null);

        // получить общие параметры операций

        GLOperation operation = operationRepository.findById(GLOperation.class, wrapper.getGloIDs().get(0));
        if (null == operation || POST != operation.getState()) {
            throw new ValidationError(BV_MANUAL_ERROR, format("По заданному условию операция в статусе '%s' не найдена", POST.name()));
        }
        parameters.setSourcePosting(operation.getSourcePosting());
        parameters.setValueDate(operation.getValueDate());
        parameters.setPostDate(operation.getPostDate());
        parameters.setIsCorrection(operation.getIsCorrection().name());
        parameters.setParentId(operation.isFan() && operation.isChild() ? operation.getParentOperation().getId() : operation.getId());

        parameters.setPostDateNew(new SimpleDateFormat(wrapper.getDateFormat()).parse(wrapper.getPostDateStr()));
        parameters.setOperCount(1);

        return parameters;
    }

    private boolean checkOperationStatus(BackValueWrapper wrapper, OperationParameters parameters, BackValuePostStatus... enabledStatus) {
        BackValuePostStatus statusIs = parameters.getBvStatus();
        if (!statusIs.equals(wrapper.getBvStatus())) {
            throw new ValidationError(BV_MANUAL_ERROR, format("%s, статус: '%s' ('%s').\n Обновите информацию и выполните действие повторно",
                    getResultMessage(wrapper, parameters.getOperCount(), "изменена", "изменены"), statusIs.name(), statusIs.getLabel()));
        }
        if (enabledStatus.length == 0)
            return true;
        for (BackValuePostStatus status : enabledStatus) {
            if (status.equals(statusIs)) {
                return true;
            }
        }
        throw new ValidationError(BV_MANUAL_ERROR, format("Нельзя '%s' операции в статусе: '%s' ('%s')",
                wrapper.getAction().getLabel(), statusIs.name(), statusIs.getLabel()));
    }

    public void checkPostDate(String sourcePosting, Date postDateNew, Date valueDate) throws SQLException {
        boolean withTech = withTechWorkDay(sourcePosting);

        // проверить postDateNew на выходной день
        if(!calendarDayRepository.isWorkday(postDateNew, withTech)) {
            throw new ValidationError(BV_MANUAL_ERROR, String.format("Действие запрещено. выбранная дата проводки '%s' – выходной"
                    , dateUtils.onlyDateString(postDateNew)));
        }

        // проверить postDateNew на допустимый диапазон
        Date postDateMin = calendarDayRepository.isWorkday(valueDate, withTech)
                ? valueDate
                : calendarDayRepository.getWorkDateBefore(valueDate, withTech);
        Date postDateMax = operdayController.getOperday().getCurrentDate();
        if (postDateNew.before(postDateMin) || postDateNew.after(postDateMax)) {
            throw new ValidationError(BV_MANUAL_ERROR, String.format("Действие запрещено.\n" +
                            "Дата проводки '%s' вышла за пределы допустимого диапазона с '%s' по '%s'"
                    , dateUtils.onlyDateString(postDateNew), dateUtils.onlyDateString(postDateMin), dateUtils.onlyDateString(postDateMax)));
        }
    }

    public void checkClosedPeriod(Long userId, Date postDateNew) {
        ClosedPeriodView period = closedPeriodRepository.getPeriod();
        if(!postDateNew.after(period.getLastDate()) &&                // разрешено только для суперпользователя
                !actionRepository.getAvailableActions(userId).contains(SecurityActionCode.OperHand3Super)) {
            throw new ValidationError(BV_MANUAL_ERROR, String.format("Действие запрещено.\n" +
                            "Дата проводки '%s' попадает в закрытый отчетный период до '%s', который закрыт с '%s'"
                    , dateUtils.onlyDateString(postDateNew)
                    , dateUtils.onlyDateString(period.getLastDate())
                    , dateUtils.onlyDateString(period.getCutDate())));
        }
    }

    public void checkUserAccessToBackValue(Long userId, Date postDateNew, Date postDateOld) throws Exception {
        if (!actionRepository.getAvailableActions(userId).contains(SecurityActionCode.OperPstChngDate) ) {
            Date minDate = postDateNew.before(postDateOld) ? postDateNew : postDateOld;
            accessServiceSupport.checkUserAccessToBackValueDate(minDate, userId);
        }
    }

    private class OperationParameters implements Serializable{

        private String sourcePosting;           // источник
        private Date valueDate;                 // дата валютирования
        private Date postDate;                  // дата проводки
        private Date postDatePlan;              // плановая дата проводки
        private Date postDateNew;               // заданная дата проводки
        private String isCorrection;            // сторно
        private BackValuePostStatus bvStatus;   // текущий статус обработки
        private Long parentId;

        private String gloidIn;
        private String from;
        private Object[] sqlParams;
        private boolean notAuthorized;
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

        public String getIsCorrection() {
            return isCorrection;
        }

        public void setIsCorrection(String isCorrection) {
            this.isCorrection = isCorrection;
        }

        public BackValuePostStatus getBvStatus() {
            return bvStatus;
        }

        public void setBvStatus(BackValuePostStatus bvStatus) {
            this.bvStatus = bvStatus;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public boolean isNotAuthorized() {
            return notAuthorized;
        }

        public void setNotAuthorized(boolean notAuthorized) {
            this.notAuthorized = notAuthorized;
        }

        public Long getParentId() {
            return parentId;
        }

        public void setParentId(Long parentId) {
            this.parentId = parentId;
        }
    }
}
