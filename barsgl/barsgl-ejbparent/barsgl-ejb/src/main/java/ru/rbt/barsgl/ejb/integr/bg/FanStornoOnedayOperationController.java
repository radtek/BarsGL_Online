package ru.rbt.barsgl.ejb.integr.bg;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.integr.fan.FanOperationProcessor;
import ru.rbt.barsgl.ejb.integr.fan.FanStornoOnedayOperationProcessor;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.enums.OperState;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.FanOperation;
import static ru.rbt.ejbcore.validation.ErrorCode.*;
import static ru.rbt.ejbcore.validation.ValidationError.initSource;

/**
 * Created by ER18837 on 18.05.15.
 * Обраюлтка сторно веерной операции в тот же день
 */
@Stateless
@LocalBean
public class FanStornoOnedayOperationController extends FanOperationController {

    @EJB
    private GLOperationRepository glOperationRepository;

    @Inject
    private FanStornoOnedayOperationProcessor stornoOnedayOperationProcessor;

    @Resource
    private EJBContext context;

    @EJB
    private AuditController auditController;

    @Override
    public YesNo getStorno() {
        return YesNo.Y;
    }

    @Override
    public boolean isWtacEnabled() {
        return true;
    }

    @Override
    public List<GLOperation> processOperations(String parentReference) {
        String msgCommon = format(" веерной операции СТОРНО с референсом '%s'", parentReference);
        auditController.info(FanOperation, "Начало обработки" + msgCommon);
        YesNo storno = YesNo.Y;
        try {
            List<GLOperation> operList;
            try {
                // получить список всех операций веера
                operList = getFanOperations(parentReference, storno);
            } catch (Throwable e) {
                String msg = "Ошибка определения данных" + msgCommon;
                auditController.error(FanOperation, msg, null, e);
                operationFanErrorMessage(e, msg, parentReference, storno, OperState.ERPROC, initSource());
                return Collections.emptyList();
            }

            // отменить операции
            return createPostings(operList);

        } catch (Exception e) {
            auditController.error(FanOperation, "Ошибка при обработке" + msgCommon, null, e);
            context.setRollbackOnly();
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Для каждой операции находит и отменяет сторнируемую операцию
     * @param operList  - список операций
     * @return
     */
    public List<GLOperation> createPostings (List<GLOperation> operList) {
        String parentReference = operList.get(0).getParentReference();
        for (GLOperation operation : operList) {
            String msgCommon = format(" частичной веерной операции СТОРНО '%d'", operation.getId());
            auditController.info(FanOperation, "Начало обработки" + msgCommon, operation);
            FanOperationProcessor operationProcessor = stornoOnedayOperationProcessor; //findOperationProcessor(operation);
            try {
                // заполняет ссылки на сторно операцию и обновляет
                updateOperation(operationProcessor, operation);
            }catch(Throwable e){
                String msg = "Ошибка заполнения данных" + msgCommon;
                auditController.error(FanOperation, msg, operation, e);
                operationErrorMessage(e, msg, operation, OperState.ERCHK, initSource(e));
                return Collections.emptyList();
            }
            try {
                // устанавливает статус сторнируемой и сторнирующей операций
                finalOperation(operationProcessor, operation);
                auditController.info(FanOperation, "Успешное завершение обработки" + msgCommon, operation);
            }catch(Throwable e){
                String msg = "Ошибка обработки" + msgCommon;
                auditController.error(FanOperation, msg, operation, e);
                operationErrorMessage(e, msg, operation, OperState.ERCHK, initSource(e));
                return Collections.emptyList();
            }
        }
        String msgCommon = format(" обработки веерной операции СТОРНО с референсом '%s'", parentReference);
        try {
            glOperationRepository.updateOperationFanStatusSuccess(parentReference, YesNo.Y, OperState.SOCANC);
            auditController.info(FanOperation, "Успешное завершение" + msgCommon);
        } catch (Throwable e) {
            String msg = "Ошибка" + msgCommon;
            auditController.error(AuditRecord.LogCode.FanOperation, msg, null, e);
            operationFanErrorMessage(e, msg, parentReference, YesNo.Y, OperState.ERPOST, initSource());
            return Collections.emptyList();
        }
        return operList;
    }

    private void finalOperation(FanOperationProcessor operationProcessor, GLOperation operation) throws Exception {
        glOperationRepository.executeInNewTransaction(persistence -> {
            GLOperation operationTx = glOperationRepository.refresh(operation, true);
            List<GLPosting> postList = new ArrayList<>();
            operationProcessor.addPosting(operationTx, postList);
            return null;
        });
    }

}
