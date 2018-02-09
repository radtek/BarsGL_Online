package ru.rbt.barsgl.ejb.integr.bg;

import org.apache.log4j.Logger;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.gwt.security.ejb.repository.access.AccessServiceSupport;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.BackvalueJournalController;
import ru.rbt.barsgl.ejb.entity.gl.AbstractPd;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.oper.BatchPostingProcessor;
import ru.rbt.barsgl.ejb.integr.oper.EditPostingGLPdProcessor;
import ru.rbt.barsgl.ejb.integr.oper.EditPostingPdProcessor;
import ru.rbt.barsgl.ejb.integr.oper.EditPostingProcessor;
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
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static org.apache.commons.lang3.time.DateUtils.addDays;
import static ru.rbt.audit.entity.AuditRecord.LogLevel.Warning;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.audit.entity.AuditRecord.LogCode.ManualOperation;
import static ru.rbt.ejbcore.util.StringUtils.listToString;
import static ru.rbt.ejbcore.validation.ValidationError.initSource;
import static ru.rbt.barsgl.shared.enums.PostingChoice.PST_ALL;
import static ru.rbt.barsgl.shared.enums.PostingChoice.PST_ONE_OF;

/**
 * Created by ER18837 on 11.04.16.
 */
public class EditPostingController {
    private static final Logger log = Logger.getLogger(EtlPostingController.class);
    public enum PostingAction{PST_EDIT, PST_SUPP};

    @EJB
    private AuditController auditController;

    @Inject
    private OperdayController operdayController;

    @Inject
    private ManualPostingController manualPostingController;

    @Inject
    private BackValuePostingController backValuePostingController;

    @Inject
    private EditPostingPdProcessor editPdProcessor;

    @Inject
    private EditPostingGLPdProcessor editGLPdProcessor;

    @Inject
    private BatchPostingProcessor manualPostingProcessor;

    @Inject
    private BackvalueJournalController journalController;

    @EJB
    private GLOperationRepository operationRepository;

    @Inject
    private AccessServiceSupport accessServiceSupport;

    @Inject
    private DateUtils dateUtils;

    @Inject
    private SecurityActionRepository actionRepository;

    /**
     * отработка действия на форме "Проводки"
     * @param operationWrapper
     * @param postingAction
     * @return
     */
    public List<? extends AbstractPd> processMessage(ManualOperationWrapper operationWrapper, PostingAction postingAction) throws Exception {
        Operday operday = operdayController.getOperday();
        if (ONLINE != operday.getPhase()) {
            throw new ValidationError(ErrorCode.OPERDAY_NOT_ONLINE, "Изменение проводок невозможно", operdayController.getOperday().getPhase().name());
        }
        if ( !operdayController.isProcessingAllowed()) {
            throw new ValidationError(ErrorCode.OPERDAY_IN_SYNCHRO, "Изменение проводок невозможно");
        }

        return operationRepository.executeInNewTransaction(persistence1 -> {
            switch (postingAction) {
                case PST_EDIT:
                    return updatePostings(operationWrapper);
                case PST_SUPP:
                    return suppressPostings(operationWrapper);
                default:
                    return null;
            }
        });
    }

    /**
     * редактирование проводок
     * @param operationWrapper
     * @return
     * @throws Exception
     */
    public List<? extends AbstractPd> updatePostings(ManualOperationWrapper operationWrapper) throws Exception {
        Operday.PdMode pdMode = Operday.PdMode.valueOf(operationWrapper.getPdMode());
        EditPostingProcessor editPostingProcessor =
                (pdMode == Operday.PdMode.DIRECT) ? editPdProcessor : editGLPdProcessor;
        List<ValidationError> errors = editPostingProcessor.validate(operationWrapper, new ValidationContext());

        if (errors.isEmpty()) {
            manualPostingProcessor.checkDealId(operationWrapper);

            List<Long> pdIdList = null;
            boolean toGetPd = (operationWrapper.getPostingChoice() == PST_ALL);
            if (toGetPd)
                pdIdList = editPostingProcessor.getOperationPdIdList(operationWrapper.getId());
            else
                pdIdList = operationWrapper.getPdIdList();
            Assert.isTrue(null != pdIdList, ()-> new DefaultApplicationException(format("Для операции '%d' не %s ни одной проводки"
                    , operationWrapper.getId(), toGetPd ? "найдено" : "задано")));

            List<? extends AbstractPd> pdList = editPostingProcessor.getOperationPdList(pdIdList);

            if (operationWrapper.getPostingChoice() != PST_ALL) {
                editPostingProcessor.updateOnePosting(operationWrapper, pdList);
            }

            if (operationWrapper.getPostingChoice() != PST_ONE_OF) {
                editPostingProcessor.updateWithMemorder(operationWrapper, pdList, isBufferMode());
                editPostingProcessor.updateAllPostings(operationWrapper, pdList);
            }
            editPostingProcessor.updatePd(pdList);

            if (operationWrapper.getPostingChoice() != PST_ONE_OF) {
                GLOperation operation = operationRepository.findById(GLOperation.class, operationWrapper.getId());
                editPostingProcessor.updateOperation(operationWrapper, operation);
                operationRepository.update(operation);
            }

            return editPostingProcessor.getOperationPdList(pdIdList);
        } else {
            // TODO
            throw new DefaultApplicationException(manualPostingProcessor.validationErrorMessage(errors, operationWrapper.getErrorList()));
        }
    }

    /**
     * подавление проводок
     * @param operationWrapper
     * @return
     * @throws Exception
     */
    public List<? extends AbstractPd> suppressPostings(ManualOperationWrapper operationWrapper) throws Exception {
        Operday.PdMode pdMode = Operday.PdMode.valueOf(operationWrapper.getPdMode());
        EditPostingProcessor editPostingProcessor =
                (pdMode == Operday.PdMode.DIRECT) ? editPdProcessor : editGLPdProcessor;

        List<Long> pdIdList;
        boolean toGetPd = (operationWrapper.getPostingChoice() == PST_ALL);
        if (toGetPd)
            pdIdList = editPostingProcessor.getOperationPdIdList(operationWrapper.getId());
        else
            pdIdList = operationWrapper.getPdIdList();
        Assert.isTrue(null != pdIdList, ()-> new DefaultApplicationException(format("Для операции '%d' не %s ни одной проводки"
                , operationWrapper.getId(), toGetPd ? "найдено" : "задано")));

        List<? extends AbstractPd> pdList = editPostingProcessor.getOperationPdList(pdIdList);

        if (operationWrapper.getPostingChoice() == PST_ONE_OF) {
            throw new DefaultApplicationException("Нельзя подавить одну проводку из группы связанных!");
        }

        editPostingProcessor.suppressAllPostings(operationWrapper, pdList, isBufferMode());
        editPostingProcessor.updatePd(pdList);
        // Обновить статус операции
        operationRepository.updateOperationParentStatus(operationWrapper.getId(),
                operationWrapper.isInvisible() ? OperState.INVISIBLE : OperState.POST);

        return editPostingProcessor.getOperationPdList(pdIdList);
    }

    private boolean isBufferMode() {
        return operdayController.getOperday().getPdMode().equals(Operday.PdMode.BUFFER);
    }

    public RpcRes_Base<ManualOperationWrapper> updatePostingsWrapper(ManualOperationWrapper operationWrapper) {
        String msg = "Ошибка при редактировании проводки";
        try {
            try {
                Date postDateOld = operationRepository.findById(GLOperation.class, operationWrapper.getId()).getPostDate();
                Date postDateNew = dateUtils.onlyDateParse(operationWrapper.getPostDateStr());
                Date valueDateNew = dateUtils.onlyDateParse(operationWrapper.getValueDateStr());

                // проверка даты проводки
                backValuePostingController.checkPostDate(operationWrapper.getDealSrc(), postDateNew, valueDateNew);

                // TODO нельзя установить дату ранее 30 дней назад - наверно уже лишнее
                Date minEditDay = addDays(operdayController.getOperday().getCurrentDate(), -30);
                if (minEditDay.after(postDateNew))
                    throw new ValidationError(ErrorCode.POSTING_BACK_GT_30, dateUtils.onlyDateString(minEditDay));

                // проверка закрытого отчетного периода
                backValuePostingController.checkClosedPeriod(operationWrapper.getUserId(), postDateOld);
                backValuePostingController.checkClosedPeriod(operationWrapper.getUserId(), postDateNew);

                // для пользователей с OperPstChngDate не надо проверять колич-во дней назад
                backValuePostingController.checkUserAccessToBackValue(operationWrapper.getUserId(), postDateNew, postDateOld);

            } catch (ValidationError e) {
                return showWarning(operationWrapper, msg, e);
            } catch (Exception e){
                return showError(operationWrapper, msg, e);
            }

            List<? extends AbstractPd> pdList = processMessage(operationWrapper, PostingAction.PST_EDIT);
            msg = "Изменены полупроводки c ID: " + listToString(pdList, ",") + operationWrapper.getErrorMessage();
            auditController.info(ManualOperation, msg, "GL_OPER", operationWrapper.getId().toString());
            return new RpcRes_Base<>( operationWrapper, false, msg);
        } catch (Throwable e) {
            return showError(operationWrapper, msg, e);
        }
    }

    public RpcRes_Base<ManualOperationWrapper> suppressPostingsWrapper(ManualOperationWrapper operationWrapper) {
        String msg = "Ошибка при подавлении прододки";
        try {
            try {
                accessServiceSupport.checkUserAccessToBackValueDate(dateUtils.onlyDateParse(operationWrapper.getPostDateStr()), operationWrapper.getUserId());

            } catch (ValidationError e) {
                return showWarning(operationWrapper, msg, e);
            } catch (Exception e){
                return showError(operationWrapper, msg, e);
            }

            List<? extends AbstractPd> pdList = processMessage(operationWrapper, PostingAction.PST_SUPP);
            msg = (operationWrapper.isInvisible() ? "Подавлены" : "Восстановлены") + " полупроводки c ID: " + listToString(pdList, ",");
            auditController.info(ManualOperation, msg, "GL_OPER", operationWrapper.getId().toString());
            return new RpcRes_Base<>( operationWrapper, false, msg);
        } catch (Throwable e) {
            return showError(operationWrapper, msg, e);
        }
    }

    private RpcRes_Base<ManualOperationWrapper> showError(ManualOperationWrapper operationWrapper, String msg, Throwable t) {
        auditController.error(ManualOperation, msg, "GL_OPER", operationWrapper.getId().toString(), t);
        String errorMsg = manualPostingController.addOperationErrorMessage(t, msg, operationWrapper.getErrorList(), initSource());
        return new RpcRes_Base<>(operationWrapper, true, errorMsg);
    }

    private RpcRes_Base<ManualOperationWrapper> showWarning(ManualOperationWrapper operationWrapper, String msg, Throwable t) {
        auditController.warning(ManualOperation, msg, "GL_OPER", operationWrapper.getId().toString(), t);
        String errorMsg = manualPostingController.addOperationErrorMessage(t, msg, operationWrapper.getErrorList(), initSource());
        return new RpcRes_Base<>(operationWrapper, true, errorMsg);
    }

}
