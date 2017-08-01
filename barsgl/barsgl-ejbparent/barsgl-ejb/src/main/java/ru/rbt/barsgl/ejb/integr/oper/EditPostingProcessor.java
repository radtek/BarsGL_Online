package ru.rbt.barsgl.ejb.integr.oper;

import ru.rbt.barsgl.ejb.bt.BalturRecalculator;
import ru.rbt.barsgl.ejb.entity.gl.AbstractPd;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPd;
import ru.rbt.barsgl.ejb.entity.gl.Memorder;
import ru.rbt.barsgl.ejb.integr.ValidationAwareHandler;
import ru.rbt.barsgl.ejb.integr.pst.MemorderController;
import ru.rbt.barsgl.ejb.repository.BackvalueJournalRepository;
import ru.rbt.barsgl.ejb.repository.BatchPostingRepository;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.barsgl.ejb.repository.PdRepository;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.find;
import static java.util.stream.Collectors.toList;
import static ru.rbt.barsgl.shared.enums.DealSource.PaymentHub;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.ejbcore.validation.ErrorCode.*;

/**
 * Created by ER18837 on 11.04.16.
 */
public abstract class EditPostingProcessor extends ValidationAwareHandler<ManualOperationWrapper> {

    @EJB
    protected PdRepository pdRepository;

    @EJB
    protected MemorderController memorderController;

    @EJB
    BalturRecalculator balturRecalculator;

    @EJB
    BackvalueJournalRepository backvalueRepository;

    @EJB
    private GLOperationRepository operationRepository;

    @Inject
    private BatchPostingRepository batchPostingRepository;

    @Inject
    BatchPostingProcessor postingProcessor;

    @Inject
    private DateUtils dateUtils;

    public abstract List<Long> getOperationPdIdList(long parentId);
    public abstract List<? extends AbstractPd> getOperationPdList(List<Long> pdIdList);
    public abstract void updateMemOrder(AbstractPd debit, String memorderNumber, Memorder.DocType docType);
    public abstract void updatePd(List<? extends AbstractPd> pdList);

    @Override
    public void fillValidationContext(ManualOperationWrapper target, ValidationContext context) {
        // ============== Общее ===============
        // Value Date
        context.addValidator(() -> {
            postingProcessor.checkDate(target, target.getValueDateStr(), "валютирования", false);
        });
        // Posting Date
        context.addValidator(() -> {
            postingProcessor.checkDate(target, target.getPostDateStr(), "проводки", true);
            Date pdate = postingProcessor.checkDateFormat(target.getPostDateStr(), "Дата проводки");
            Date vdate = postingProcessor.checkDateFormat(target.getValueDateStr(), "Дата валютирования");
            if ( (null != pdate )&& (null != vdate) && vdate.after(pdate)) {
                throw new ValidationError(POSTDATE_NOT_VALID,
                        target.getPostDateStr(),
                        target.getValueDateStr());
            }
        });
/*
        // Источник сделки
        context.addValidator(() -> {
            String fieldName = "Источник сделки";
            String fieldValue = target.getDealSrc();
            int maxLen = 7;
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            } else if (maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
*/
        // ИД платежа
        context.addValidator(() -> {
            String fieldName = "Номер платежного документа";
            String fieldValue = target.getPaymentRefernce();
            int maxLen = 20;
            if ( null != fieldValue && maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        // Номер сделки
        context.addValidator(() -> {
            String fieldName = "Номер сделки";
            String fieldValue = target.getDealId();
            int maxLen = 20;
            if (null != fieldValue && maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        // Номер субсделки
        context.addValidator(() -> {
            String fieldName = "Номер субсделки";
            String fieldValue = target.getSubdealId();
            int maxLen = 20;
            if (null != fieldValue) {
                if (maxLen < fieldValue.length())
                    throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });

        // описания
        context.addValidator(() -> {
            String fieldName = "Основание ENG";
            String fieldValue = target.getNarrative();
            int maxLen = 300;
            if (null != fieldValue && maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        context.addValidator(() -> {
            String fieldName = "Основание RUS";
            String fieldValue = target.getRusNarrativeLong();
            int maxLen = 300;
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            } else if (maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
    }

    /**
     * Обновляем в одной проводке Deal / Subdeal
     * @param wrapper
     * @param pdList
     */
    public void updateOnePosting(ManualOperationWrapper wrapper, List<? extends AbstractPd> pdList) {
        pdList.forEach(pd -> {
            pd.setNarrative(wrapper.getNarrative());
            pd.setRusNarrLong(wrapper.getRusNarrativeLong());
        });
    };

    /**
     * Обновляем для всех проводок по операции: Deal / Subdeal - только для ручных, Профитцентр - для всех операций
     * @param wrapper
     * @param pdList
     */
    public void updateAllPostings(ManualOperationWrapper wrapper, List<? extends AbstractPd> pdList) throws Exception {
        Date valueDate = postingProcessor.checkDateFormat(wrapper.getValueDateStr(), "Дата валютирования");
        for (AbstractPd pd : pdList) {
            if (InputMethod.AE != wrapper.getInputMethod()) {       // M || F
                pd.setDealId(wrapper.getDealId());
                pd.setSubdealId(wrapper.getSubdealId());
                pd.setPaymentRef(wrapper.getDealId());
                pd.setPref(pdRepository.getPrefManual(wrapper.getDealId(), wrapper.getSubdealId(), wrapper.getPaymentRefernce(),
                        PaymentHub.getLabel().equals(wrapper.getDealSrc())));
                pd.setPnar(pdRepository.getPnarManual(wrapper.getDealId(), wrapper.getSubdealId(), wrapper.getPaymentRefernce()));
            }
            pd.setVald(valueDate);
            pd.setProfitCenter(wrapper.getProfitCenter());  // TODO возможно, надо будет перенести в OnePosting
        };
    };

    /**
     * изменение признака "Корректирующая" с перегенерацией мемордера
     * @param wrapper
     * @param pdList
     */
    public Date updateWithMemorder(ManualOperationWrapper wrapper, final List<? extends AbstractPd> pdList, boolean isBuffer) throws Exception {
        YesNo isCorrection = YesNo.getValue(wrapper.isCorrection());
        Date postDate = postingProcessor.checkDateFormat(wrapper.getPostDateStr(), "Дата проводки");
        Date pod = pdList.get(0).getPod();
        boolean changeDate = !pod.equals(postDate);
        boolean changeCorrection = (pdList.get(0).getIsCorrection() != isCorrection);
        if (!changeDate && !changeCorrection)      // дата и признак коррекции не изменились
            return postDate;

        checkStornoDate(wrapper, postDate); // проверить дату со сторно
        if (changeDate) {
            checkControllable(pdList, "Нельзя изменить дату проводок");
            // сообщение а аудит
            wrapper.getErrorList().addErrorDescription(String.format(" (дата проводки была: '" + dateUtils.onlyDateString(pod) + "'" +
                    ", стала: '" + wrapper.getPostDateStr() + "')"));
        }

        Date dateMin = pod.before(postDate) ? pod : postDate;
        // обновить поля в полупроводках
        for (AbstractPd pd : pdList) {
            pd.setIsCorrection(isCorrection);
            pd.setPod(postDate);
            if (changeDate) {
                // запись в журнал для пересчета баланса
                balturRecalculator.registerChangeMarker(pd.getBsaAcid(), pd.getAcid(), dateMin);
                if (isBuffer) {
                    // запись в журнал для пересчета и локазизации по счету
                    backvalueRepository.registerBackvalueJournalAcc(pd.getBsaAcid(), pd.getAcid(), dateMin);
                }
            }
        };

        // обновить мемордера
        List<AbstractPd> pdListDebit = pdList.stream().filter(p -> Objects.equals(p.getId(), p.getPcId())).collect(Collectors.toList());
        for (AbstractPd pdDebit : pdListDebit) {
            List<AbstractPd> debits = pdList.stream().filter(p -> p.getAmountBC() < 0
                    && Objects.equals(p.getPcId(), pdDebit.getPcId())).collect(toList());
            Assert.isTrue(debits.size() >= 1, "Не найдена проводка по дебету");
            List<AbstractPd> credits = pdList.stream().filter(p -> p.getAmountBC() > 0
                    && Objects.equals(p.getPcId(), pdDebit.getPcId())).collect(Collectors.toList());
            Assert.isTrue(credits.size() >= 1, "Не найдена проводка по кредиту");

            String memorderNumber = memorderController.nextMemorderNumber(pdDebit.getPod(), pdDebit.getBsaAcid(), wrapper.isCorrection());
            Memorder.DocType docTyoe = memorderController.getDocType(wrapper.isCorrection(), debits, credits, pdDebit.getPod());
            updateMemOrder(pdDebit, memorderNumber, docTyoe);
        }
        return postDate;
    }

    /**
     * изменяет поля в операции, если меняются все проводки по операции
     */
    public void updateOperation(ManualOperationWrapper wrapper, GLOperation operation) {
        if (InputMethod.AE != wrapper.getInputMethod()) {       // M || F
            operation.setDealId(wrapper.getDealId());
            operation.setSubdealId(wrapper.getSubdealId());
            operation.setPaymentRefernce(wrapper.getPaymentRefernce());
        }
        operation.setValueDate(postingProcessor.checkDateFormat(wrapper.getValueDateStr(), "Дата валютирования"));
        operation.setPostDate(postingProcessor.checkDateFormat(wrapper.getPostDateStr(), "Дата проводки"));
        operation.setNarrative(wrapper.getNarrative());
        operation.setRusNarrativeLong(wrapper.getRusNarrativeLong());
        operation.setIsCorrection(YesNo.getValue(wrapper.isCorrection()));
        operation.setProfitCenter(wrapper.getProfitCenter());
    }

    public void suppressAllPostings(ManualOperationWrapper wrapper, final List<? extends AbstractPd> pdList, boolean isBuffer)  throws Exception {
        checkStornoStatus(wrapper);
        checkControllable(pdList, "Нельзя " + (wrapper.isInvisible() ? "подавить" : "восстановить") + " проводки");

        // TODO при подавлении и изменении даты проводки (брать мин дату)
        // registerBackvalueJournal - только в режиме BUFFER
        // balturRecalculator.registerChangeMarker - всегда
        String invisible = wrapper.isInvisible() ? "1" : "0";
        for (AbstractPd pd : pdList) {
            pd.setInvisible(invisible);
            // запись в журнал для пересчета и локазизации по счету
            balturRecalculator.registerChangeMarker(pd.getBsaAcid(), pd.getAcid(), pd.getPod());
            if (isBuffer) {
                backvalueRepository.registerBackvalueJournalAcc(pd.getBsaAcid(), pd.getAcid(), pd.getPod());
            }
        };
    }

    /**
     * Проверяет правильность дат со связанной по сторно операцией
     */
    public void checkStornoDate(ManualOperationWrapper wrapper, Date postDate) {
        Long operationId = wrapper.getId();
        GLOperation operation = operationRepository.findById(GLOperation.class, operationId);
        Assert.notNull(operation, "Не найдена операция GLOID = " + operationId);
        if (operation.isStorno()) {         // это сторно
            GLOperation parent = operation.getStornoOperation();
            if (postDate.before(parent.getPostDate())) { // сторно раньше родительской
                throw new ValidationError(POSTDATE_LT_PARENT,
                        dateUtils.onlyDateString(postDate),
                        dateUtils.onlyDateString(parent.getPostDate()),
                        parent.getId().toString());
            }
        } else {        // проверяем, нет ли сторнирующей
            GLOperation storno = operationRepository.getStornoOperationByGloRef(operationId);
            if ((null != storno) && postDate.after(storno.getPostDate())) { // родительская позже сторно
                throw new ValidationError(POSTDATE_GT_STORNO,
                        dateUtils.onlyDateString(postDate),
                        dateUtils.onlyDateString(storno.getPostDate()),
                        storno.getId().toString());
            }
        }
    }

    /**
     * Проверяет возможность подавления по сторно операцией
     */
    public void checkStornoStatus(ManualOperationWrapper wrapper) {
        if (wrapper.isStorno())
            return;             // сторно не проверяем
        Long operationId = wrapper.getId();
        GLOperation operation = operationRepository.findById(GLOperation.class, operationId);
        Assert.notNull(operation, "Не найдена операция GLOID = " + operationId);
        // проверяем, нет ли сторнирующей
        GLOperation storno = operationRepository.getStornoOperationByGloRef(operationId);
        if ((null != storno) && OperState.INVISIBLE != storno.getState()) { // есть сторно, не подавленная
            throw new ValidationError(OPERATION_HAS_STORNO, storno.getId().toString(), storno.getState().name());
        }
    }

    /**
     * Проверяет, есть ли проводки по контролируемым счетам
     * @param pdList
     * @param message
     */
    private void checkControllable(List<? extends AbstractPd> pdList, String message) {
        List<String> accounts = new ArrayList<String>();
        for (AbstractPd pd : pdList) {
            if (batchPostingRepository.isControlableAccount(pd.getBsaAcid())) {
                accounts.add(pd.getBsaAcid());
            }
        }
        if (!accounts.isEmpty()) {
            throw new ValidationError(ErrorCode.POSTINGS_CONTROLLABLE, message, StringUtils.listToString(accounts, "', '"));
        }
    }

}
