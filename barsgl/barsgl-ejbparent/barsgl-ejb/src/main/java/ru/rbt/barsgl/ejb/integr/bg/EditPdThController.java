package ru.rbt.barsgl.ejb.integr.bg;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.access.AccessServiceSupport;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.gl.AbstractPd;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GlPdTh;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountProcessor;
import ru.rbt.barsgl.ejb.integr.oper.BatchPostingProcessor;
import ru.rbt.barsgl.ejb.integr.oper.EditPdThProcessor;
import ru.rbt.barsgl.ejb.integr.oper.EditPostingGLPdProcessor;
import ru.rbt.barsgl.ejb.integr.oper.EditPostingProcessor;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.barsgl.ejb.repository.access.SecurityActionRepository;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.Assert;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.barsgl.shared.enums.SecurityActionCode;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.barsgl.shared.operation.ManualTechOperationWrapper;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.ManualOperation;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.listToString;
import static ru.rbt.barsgl.ejbcore.validation.ValidationError.initSource;
import static ru.rbt.barsgl.shared.enums.PostingChoice.PST_ALL;
import static ru.rbt.barsgl.shared.enums.PostingChoice.PST_ONE_OF;

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

        List<GlPdTh> pdList = editPdThProcessor.getOperationPdList(pdIdList);

        editPdThProcessor.suppressAllPostings(operationWrapper, pdList);
        editPdThProcessor.updatePd(pdList);
        // Обновить статус операции
        operationRepository.updateOperationParentStatus(operationWrapper.getId(),
                operationWrapper.isInvisible() ? OperState.INVISIBLE : OperState.POST);

        return editPdThProcessor.getOperationPdList(pdIdList);
    }

    public RpcRes_Base<ManualTechOperationWrapper> suppressPostingsWrapper(ManualTechOperationWrapper operationWrapper) {
        try {
            String msg = "Ошибка при подавлении прододки";
            try {
                accessServiceSupport.checkUserAccessToBackValueDate(dateUtils.onlyDateParse(operationWrapper.getPostDateStr()), operationWrapper.getUserId());

            } catch (ValidationError e) {
                String errMessage = manualPostingController.addOperationErrorMessage(e, msg, operationWrapper.getErrorList(), initSource());
                auditController.warning(ManualOperation, msg, "GL_OPER", operationWrapper.getId().toString(), e);
                return new RpcRes_Base<>( operationWrapper, true, errMessage);
            } catch (Exception e){
                auditController.error(ManualOperation, msg, "GL_OPER", operationWrapper.getId().toString(), e);
                return new RpcRes_Base<>(operationWrapper, true, e.getMessage());
            }

            List<GlPdTh> pdList = suppressPdTh(operationWrapper);
            msg = (operationWrapper.isInvisible() ? "Подавлены" : "Восстановлены") + " полупроводки c ID: " + listToString(pdList, ",");
            auditController.info(ManualOperation, msg, "GL_OPER", operationWrapper.getId().toString());
            return new RpcRes_Base<>( operationWrapper, false, msg);
        } catch (Exception e) {
            String errMessage = operationWrapper.getErrorMessage();
            return new RpcRes_Base<ManualTechOperationWrapper>( operationWrapper, true, errMessage);
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

    public RpcRes_Base<ManualOperationWrapper> updatePdThWrapper(ManualTechOperationWrapper operationWrapper) {
        try {
            String msg = "Ошибка при редактировании проводки";
            try {
                Date newDate = dateUtils.onlyDateParse(operationWrapper.getPostDateStr());
                Date editDay = org.apache.commons.lang3.time.DateUtils.addDays(operdayController.getOperday().getCurrentDate(), -30); // TODO -30
                // нельзя установить дату ранее 30 дней назад
                if (editDay.after(newDate))
                    throw new ValidationError(ErrorCode.POSTING_BACK_GT_30, dateUtils.onlyDateString(editDay));
                // для пользователей с OperPstChngDate не надо проверять колич-во дней назад
                if (!actionRepository.getAvailableActions(operationWrapper.getUserId()).contains(SecurityActionCode.OperPstChngDate) ) {
                    Date oldDate = operationRepository.findById(GLOperation.class, operationWrapper.getId()).getPostDate();
                    Date minDate = newDate.before(oldDate) ? newDate : oldDate;
                    accessServiceSupport.checkUserAccessToBackValueDate(minDate, operationWrapper.getUserId());
                }

            } catch (ValidationError e) {
                String errMessage = manualPostingController.addOperationErrorMessage(e, msg, operationWrapper.getErrorList(), initSource());
                auditController.warning(ManualOperation, msg, "PD", operationWrapper.getId().toString(), e);
                return new RpcRes_Base<>( operationWrapper, true, errMessage);
            } catch (Exception e){
                auditController.error(ManualOperation, msg, "PD", operationWrapper.getId().toString(), e);
                return new RpcRes_Base<>(operationWrapper, true, e.getMessage());
            }

            List<GlPdTh> pdList = updatePdTh(operationWrapper);
            msg = "Изменены полупроводки c ID: " + listToString(pdList, ",") + operationWrapper.getErrorMessage();
            auditController.info(ManualOperation, msg, "GL_OPER", operationWrapper.getId().toString());
            return new RpcRes_Base<>( operationWrapper, false, msg);
        } catch (Exception e) {
            String errMessage = operationWrapper.getErrorMessage();
            return new RpcRes_Base<>(operationWrapper, true, errMessage);
        }
    }

    public void checkDealId(ManualOperationWrapper wrapper ) {
        glAccountProcessor.checkDealId(manualTechOperationController.checkDateFormat(wrapper.getPostDateStr(), "Дата проводки"), wrapper.getDealSrc(),
                wrapper.getDealId(), wrapper.getSubdealId(), wrapper.getPaymentRefernce());
    }


}
