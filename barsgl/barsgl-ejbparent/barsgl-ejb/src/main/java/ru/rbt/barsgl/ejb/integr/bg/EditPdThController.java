package ru.rbt.barsgl.ejb.integr.bg;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.entity.dict.ClosedReportPeriodView;
import ru.rbt.barsgl.ejb.repository.dict.ClosedPeriodCashedRepository;
import ru.rbt.gwt.security.ejb.repository.access.AccessServiceSupport;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GlPdTh;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountProcessor;
import ru.rbt.barsgl.ejb.integr.oper.BatchPostingProcessor;
import ru.rbt.barsgl.ejb.integr.oper.EditPdThProcessor;
import ru.rbt.barsgl.ejb.integr.oper.EditPostingGLPdProcessor;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.security.ejb.repository.access.SecurityActionRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.shared.enums.SecurityActionCode;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.barsgl.shared.operation.ManualTechOperationWrapper;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.ManualOperation;
import static ru.rbt.ejbcore.util.StringUtils.listToString;
import static ru.rbt.ejbcore.validation.ErrorCode.BV_MANUAL_ERROR;
import static ru.rbt.ejbcore.validation.ValidationError.initSource;

/**
 * Created by er23851 on 20.04.2017.
 */
public class EditPdThController {
    private static final Logger log = Logger.getLogger(EtlPostingController.class);

    @Inject
    private EditPdThProcessor editPdThProcessor;

    @EJB
    private GLOperationRepository operationRepository;

    @Inject
    private ManualPostingController manualPostingController;

    @Inject
    private AccessServiceSupport accessServiceSupport;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private AuditController auditController;

    @Inject
    private GLAccountProcessor glAccountProcessor;

    @Inject
    private ManualTechOperationController manualTechOperationController;

    @EJB
    private ClosedPeriodCashedRepository closedPeriodRepository;

    @Inject
    private SecurityActionRepository actionRepository;

    @Inject
    private OperdayController operdayController;

    @Inject
    private EditPostingGLPdProcessor editPostingProcessor;

    @Inject
    private BatchPostingProcessor manualPostingProcessor;


    public List<GlPdTh> suppressPdTh(ManualTechOperationWrapper operationWrapper) throws Exception {

        List<Long> pdIdList = operationWrapper.getPdIdList();
        Assert.isTrue(null != pdIdList, ()-> new DefaultApplicationException(format("Для операции '%d' не %s ни одной проводки"
                , operationWrapper.getId(), "найдено")));

        List<GlPdTh> pdList = editPdThProcessor.getOperationPdList(pdIdList.get(0));

        editPdThProcessor.suppressAllPostings(operationWrapper, pdList);
        editPdThProcessor.updatePd(pdList);
        // Обновить статус операции
        operationRepository.updateOperationParentStatus(operationWrapper.getId(),
                operationWrapper.isInvisible() ? OperState.INVISIBLE : OperState.POST);

        return editPdThProcessor.getOperationPdList(pdIdList);
    }

    public RpcRes_Base<ManualTechOperationWrapper> suppressPostingsWrapper(ManualTechOperationWrapper operationWrapper) {
        String msg = "Ошибка при подавлении проводки";
        try {
            try {
                accessServiceSupport.checkUserAccessToBackValueDate(dateUtils.onlyDateParse(operationWrapper.getPostDateStr()), operationWrapper.getUserId());

            } catch (ValidationError e) {
                return showWarning(operationWrapper, msg, e);
            } catch (Exception e){
                return showError(operationWrapper, msg, e);
            }

            List<GlPdTh> pdList = operationRepository.executeInNewTransaction(persistence -> suppressPdTh(operationWrapper));
            msg = (operationWrapper.isInvisible() ? "Подавлены" : "Восстановлены") + " полупроводки по тех.счетам c ID: " + listToString(pdList, ",");
            auditController.info(ManualOperation, msg, "GL_PDTH", operationWrapper.getId().toString());
            return new RpcRes_Base<>( operationWrapper, false, msg);
        } catch (Throwable e) {
            return showError(operationWrapper, msg, e);
        }
    }

    public List<GlPdTh> updatePdTh(ManualTechOperationWrapper operationWrapper) throws Exception {
        //Operday.PdMode pdMode = Operday.PdMode.valueOf(operationWrapper.getPdMode());

        List<ValidationError> errors = editPdThProcessor.validate(operationWrapper, new ValidationContext());

        if (errors.isEmpty()) {
            checkDealId(operationWrapper);

            List<Long> pdIdList = editPdThProcessor.getOperationPdIdList(operationWrapper.getId());

            Assert.isTrue(null != pdIdList || !pdIdList.isEmpty(), ()-> new DefaultApplicationException(format("Для операции '%d' не %s ни одной проводки"
                    , operationWrapper.getId(),  "найдено")));

            List<GlPdTh> pdList = editPdThProcessor.getOperationPdList(pdIdList);

            editPdThProcessor.updateAllPostings(operationWrapper, pdList);
            editPdThProcessor.updatePd(pdList);

            GLOperation operation = operationRepository.findById(GLOperation.class, operationWrapper.getId());
            editPostingProcessor.updateOperation(operationWrapper, operation);
            operationRepository.update(operation);

            return editPdThProcessor.getOperationPdList(pdIdList);
        } else {
            // TODO
            throw new DefaultApplicationException(manualPostingProcessor.validationErrorMessage(errors, operationWrapper.getErrorList()));
        }
    }

    public RpcRes_Base<ManualTechOperationWrapper> updatePdThWrapper(ManualTechOperationWrapper operationWrapper) {
        String msg = "Ошибка при редактировании проводки";
        try {
            try {
                Date oldDate = operationRepository.findById(GLOperation.class, operationWrapper.getId()).getPostDate();
                Date newDate = dateUtils.onlyDateParse(operationWrapper.getPostDateStr());
                Date editDay = org.apache.commons.lang3.time.DateUtils.addDays(operdayController.getOperday().getCurrentDate(), -30); // TODO -30
                // нельзя установить дату ранее 30 дней назад
                if (editDay.after(newDate))
                    throw new ValidationError(ErrorCode.POSTING_BACK_GT_30, dateUtils.onlyDateString(editDay));

                // проверка закрытого отчетного периода
                checkClosedPeriod(operationWrapper.getUserId(), oldDate);
                checkClosedPeriod(operationWrapper.getUserId(), newDate);

                // для пользователей с OperPstChngDate не надо проверять колич-во дней назад
                if (!actionRepository.getAvailableActions(operationWrapper.getUserId()).contains(SecurityActionCode.TechOperPstChngDate) ) {
                    Date minDate = newDate.before(oldDate) ? newDate : oldDate;
                    accessServiceSupport.checkUserAccessToBackValueDate(minDate, operationWrapper.getUserId());
                }

            } catch (ValidationError e) {
                return showError(operationWrapper, msg, e);
            } catch (Exception e){
                return showError(operationWrapper, msg, e);
            }

            List<GlPdTh> pdList = operationRepository.executeInNewTransaction(persistence -> updatePdTh(operationWrapper));
            msg = "Изменены полупроводки по тех.счетам c ID: " + listToString(pdList, ",") + operationWrapper.getErrorMessage();
            auditController.info(ManualOperation, msg, "GL_PDTH", operationWrapper.getId().toString());
            return new RpcRes_Base<>(operationWrapper, false, msg);
        } catch (Throwable e) {
            return showError(operationWrapper, msg, e);
        }
    }

    public void checkDealId(ManualOperationWrapper wrapper ) {
        glAccountProcessor.checkDealId(manualTechOperationController.checkDateFormat(wrapper.getPostDateStr(), "Дата проводки"), wrapper.getDealSrc(),
                wrapper.getDealId(), wrapper.getSubdealId(), wrapper.getPaymentRefernce());
    }

    public void checkClosedPeriod(Long userId, Date postDateNew) {
        ClosedReportPeriodView period = closedPeriodRepository.getPeriod();
        if(!postDateNew.after(period.getLastDate()) &&                // разрешено только для суперпользователя
                !actionRepository.getAvailableActions(userId).contains(SecurityActionCode.TechOperPstChngSuper)) {
            throw new ValidationError(BV_MANUAL_ERROR, String.format("Действие запрещено.\n" +
                            "Дата проводки '%s' попадает в закрытый отчетный период до '%s', который закрыт с '%s'"
                    , dateUtils.onlyDateString(postDateNew)
                    , dateUtils.onlyDateString(period.getLastDate())
                    , dateUtils.onlyDateString(period.getCutDate())));
        }
    }

    private RpcRes_Base<ManualTechOperationWrapper> showError(ManualTechOperationWrapper operationWrapper, String msg, Throwable t) {
        auditController.error(ManualOperation, msg, "GL_OPER", operationWrapper.getId().toString(), t);
        String errorMsg = manualPostingController.addOperationErrorMessage(t, msg, operationWrapper.getErrorList(), initSource());
        return new RpcRes_Base<>(operationWrapper, true, errorMsg);
    }

    private RpcRes_Base<ManualTechOperationWrapper> showWarning(ManualTechOperationWrapper operationWrapper, String msg, Throwable t) {
        auditController.warning(ManualOperation, msg, "GL_OPER", operationWrapper.getId().toString(), t);
        String errorMsg = manualPostingController.addOperationErrorMessage(t, msg, operationWrapper.getErrorList(), initSource());
        return new RpcRes_Base<>(operationWrapper, true, errorMsg);
    }

}
