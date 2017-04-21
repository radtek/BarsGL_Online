package ru.rbt.barsgl.ejb.integr.bg;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.integr.fan.FanOperationProcessor;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.barsgl.ejb.repository.GLPostingRepository;
import ru.rbt.barsgl.ejb.repository.MemorderRepository;
import ru.rbt.barsgl.ejb.repository.PdRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.shared.enums.OperState;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.FanOperation;
import static ru.rbt.ejbcore.validation.ValidationError.initSource;

/**
 * Контроллер для обработки веерных операций, создающих проводки
 * Created by ER18837 on 26.05.15.
 */
public abstract class FanPostingOperationController extends FanOperationController {

    @EJB
    private GLOperationRepository glOperationRepository;

    @Inject
    private GLPostingRepository glPostingRepository;

    @EJB
    private PdRepository pdRepository;

    @EJB
    private MemorderRepository memorderRepository;

    @EJB
    private AuditController auditController;

    @EJB
    private OperdayController operdayController;

    @Override
    public List<GLOperation> processOperations(String parentReference) {
        try {
            return (List<GLOperation>)pdRepository.executeInNewTransaction(persistence -> {
                YesNo storno = getStorno();
                String msgCommon = format(" веерной операции с референсом '%s', %s", parentReference,
                        storno.equals(YesNo.Y) ? "СТОРНО с прошедшей датой" : "");
                auditController.info(FanOperation, "Начало обработки" + msgCommon);
                try {
                    List<GLOperation> operList;
                    GLOperation mainOperation;
                    GLOperation.OperSide fpSide;
                    try {
                        // получить список всех операций веера
                        operList = getFanOperations(parentReference, storno);
                        // найти основную операцию
                        mainOperation = getMainOperation(operList, parentReference);
                        // определить сторону веера
                        fpSide = defineFpSide(operList, mainOperation);
                        BigDecimal amt = BigDecimal.ZERO;
                        BigDecimal amtru = BigDecimal.ZERO;
                        for (GLOperation operation : operList) {
                            amt = amt.add(fpSide.equals(GLOperation.OperSide.C) ? operation.getAmountDebit() : operation.getAmountCredit());
                            amtru = amtru.add(operation.getAmountPosting());
                        }

                        // обновить параметры по вееру
                        for (GLOperation operation : operList) {
                            updateFanParameters(operation, mainOperation, fpSide, amt, amtru);
                        }

                    } catch (Throwable e) {
                        String msg = "Ошибка определения данных" + msgCommon;
                        auditController.error(FanOperation, msg, null, e);
                        operationFanErrorMessage(e, msg, parentReference, storno, OperState.ERPROC, initSource());
                        return Collections.emptyList();
                    }
                    return createPostings(operList, mainOperation, storno);

                } catch (Exception e) {
                    auditController.error(FanOperation, "Ошибка при обработке" + msgCommon, null, e);
                    throw new DefaultApplicationException(e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            throw new DefaultApplicationException(e);
        }
    }

    protected List<GLOperation> createPostings (List<GLOperation> operList, GLOperation mainOperation, YesNo storno) throws Exception {
        String parentReference = mainOperation.getParentReference();
        // создать список проводок
        List<GLPosting> postList = new ArrayList<>();
        // создать пустую веерную проводку и добавить в список
        GLPosting fanPosting = new GLPosting(pdRepository.getNextId(), mainOperation, GLPosting.PostingType.FanMain);
        postList.add(fanPosting);

        for (GLOperation operation : operList) {
            String msgCommon = format(" частичной веернойй операции '%d'", operation.getId());
            auditController.info(FanOperation, "Начало обработки" + msgCommon, operation);
            operation = glOperationRepository.refresh(operation);
            glOperationRepository.setFilials(operation);
            FanOperationProcessor operationProcessor = findOperationProcessor(operation);
            try {
                operation = updateOperation(operationProcessor, operation);
            }catch(Throwable e){
                String msg = "Ошибка заполнения данных" + msgCommon;
                auditController.error(FanOperation, msg, operation, e);
                operationErrorMessage(e, msg, operation, OperState.ERCHK, initSource(e));
                return Collections.emptyList();
            }
            try {
                addPosting(operation, operationProcessor, postList);
                auditController.info(FanOperation, "Успешное завершение обработки" + msgCommon, operation);
            }catch(Throwable e){
                String msg = "Ошибка обработки" + msgCommon;
                auditController.error(FanOperation, msg, operation, e);
                operationErrorMessage(e, msg, operation, OperState.ERCHK, initSource(e));
                return Collections.emptyList();
            }
        }
        String msgCommon = format(" веерной операции с референсом '%s', %s", parentReference,
                storno.equals(YesNo.Y) ? "СТОРНО с прошедшей датой" : "");
        try {
            // создать суммарную проводку по вееру
            FanOperationProcessor operationProcessor = findOperationProcessor(mainOperation);
            addMainPd(mainOperation, operationProcessor, fanPosting);
            // записать проводки
            finalOperation(postList);
            glOperationRepository.updateOperationFanStatusSuccess(parentReference, storno, OperState.POST);
            auditController.info(FanOperation, "Успешное завершение обработки" + msgCommon );
        } catch (Throwable e) {
            String msg = "Ошибка создания проводок по" + msgCommon;
            auditController.error(FanOperation, msg, mainOperation, e);
            operationFanErrorMessage(e, msg, parentReference, storno, OperState.ERPOST, initSource());
            return Collections.emptyList();
        }
        return operList;
    }

    /**
     * Добавляет проводки по перьям в список проводок
     * @param operation             - операция
     * @param operationProcessor    - процессор для обработки операции
     * @param postList              - список проводок
     * @throws Exception
     */
    protected void addPosting(GLOperation operation, FanOperationProcessor operationProcessor, List<GLPosting> postList) throws Exception {
        operationProcessor.addPosting(operation, postList);
        operationProcessor.resolvePostingReference(operation, postList);
    }

    /**
     * Добавляет основную веерную полупроводку в основную проводку
     * @param operation
     * @param operationProcessor
     * @param posting
     * @throws java.sql.SQLException
     */
    protected void addMainPd(GLOperation operation, FanOperationProcessor operationProcessor, GLPosting posting) throws SQLException {
        glPostingRepository.addPostingPdFan(operation,
                posting, operation.getFbSide(), GLPosting.PostingType.FanMain,
                operation.getAccountDebit(), operation.getCurrencyDebit(),
                operation.getAccountCredit(), operation.getCurrencyCredit(),
                operation.getAmountFan(), operation.getAmountFanRu());
        operationProcessor.resolvePostingReference(operation, new ArrayList<>(Arrays.asList(posting)));
    }

    /**
     * создаем проводки, мемордера, пересчет (локализация)
     * @param pstList       - список постингов. Первый - веерная проводка
     * @throws Exception
     */
    private void finalOperation(List<GLPosting> pstList) throws Exception {
        if (operdayController.getOperday().getPdMode() == Operday.PdMode.DIRECT) {
            glOperationRepository.executeInNewTransaction(persistence -> {
                if (!pstList.isEmpty()) {                                                   // создать проводки
                    pdRepository.processPosting(pstList);                             // обработать / записать проводки
                }
                return null;
            });
        } else {
            throw new DefaultApplicationException("Текущий режим обработки должен быть DIRECT");
        }
    }

}
