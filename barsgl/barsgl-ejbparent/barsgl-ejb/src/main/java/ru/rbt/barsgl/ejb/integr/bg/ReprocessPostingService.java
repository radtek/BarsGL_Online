package ru.rbt.barsgl.ejb.integr.bg;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.sec.GLErrorRecord;
import ru.rbt.barsgl.ejb.repository.EtlPostingRepository;
import ru.rbt.barsgl.ejb.repository.GLErrorRepository;
import ru.rbt.barsgl.shared.ErrorList;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.ErrorCorrectType;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.ExceptionUtils;

import javax.ejb.EJB;
import javax.persistence.PersistenceException;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.List;

import static ru.rbt.barsgl.shared.enums.ErrorCorrectType.CorrectType.EDIT;
import static ru.rbt.barsgl.shared.enums.ErrorCorrectType.CorrectType.*;
import static ru.rbt.ejbcore.util.StringUtils.ifEmpty;

/**
 * Created by ER18837 on 01.03.17.
 */
public class ReprocessPostingService {

    @EJB
    private GLErrorRepository errorRepository;

    @EJB
    private EtlPostingRepository etlPostingRepository;

    @EJB
    private ReprocessPostingController reprocessController;

    @EJB
    AuditController auditController;

    public RpcRes_Base<Integer> correctErrors (List<Long> errorIdList, String comment, String idPstCorr, ErrorCorrectType correctType){
        try {
            ErrorList errorList = new ErrorList();
            String idList = StringUtils.listToString(errorIdList, ",");
            List<String> sources = errorRepository.getSourceList(idList);
            if (sources.size() != 1) {
                errorList.addErrorDescription("В списке более одного источника сделки: " + StringUtils.listToString(sources, ", ", "'"));
            }
            List<String> dates = errorRepository.getDateList(idList);
            if (dates.size() != 1) {
                errorList.addErrorDescription("В списке более одной даты опердня: " + StringUtils.listToString(dates, ", ", "'"));;
            }
            List<String> notUnique = errorRepository.getNotUniqueList(idList);
            if (notUnique.size() != 0) {
                errorList.addErrorDescription("В списке есть операции с совпадающим ID_PST: " + StringUtils.listToString(notUnique, ", ", "'"));
            }
            List<String> codes = errorRepository.getErrorCodeList(idList);
            if (codes.size() != 1) {
                errorList.addErrorDescription("В списке разные комбинации кодов ошибок");   // + StringUtils.listToString(codes, ", ", "'"));
            }
            List<String> states = errorRepository.getOperStateList(idList);
            if (states.size() != 1) {
                errorList.addErrorDescription("В списке разные операции с разными статусами: " + StringUtils.listToString(states, ", ", "'"));
            }
            String state = states.get(0);
            List<String> opers = errorRepository.getIdPstList(idList, OperState.WTAC);
            if (opers.size() != 0) {
                errorList.addErrorDescription("В списке есть операции со статусом 'WTAC', ID_PST: " + StringUtils.listToString(opers, ", ", "'"));
            }
            if (!StringUtils.isEmpty(idPstCorr)) {
                String err = checkIdPst(errorIdList.get(0), idPstCorr, correctType);
                if (!StringUtils.isEmpty(err))
                    errorList.addErrorDescription(err);
            }
            if (!errorList.isEmpty()) {
                return new RpcRes_Base<>(0, true, errorList.getErrorMessage());
            }

            auditController.info(AuditRecord.LogCode.ReprocessAEOper, String.format("Начало оброаботки '%s' ошибок ID : %s",
                    correctType.getTypeLabel(), idList));
            int cnt = reprocessController.correctPostingErrors(errorIdList, comment, idPstCorr, correctType
                    , (null != state ? OperState.valueOf(state) : null));
            String msg = String.format("Сообщения АЕ (%d) %s\nДата опердня: '%s'\nИсточник сделки: '%s'", cnt,
                    correctType.getTypeMessage(), dates.get(0), sources.get(0));
            auditController.info(AuditRecord.LogCode.ReprocessAEOper, msg);
            return new RpcRes_Base<>(errorIdList.size(), false, msg);
        } catch (Throwable t) {
            String msg = getErrorMessage(t);
            if (null != ExceptionUtils.findException(t, ValidationError.class)) {
                auditController.warning(AuditRecord.LogCode.ReprocessAEOper, msg);
                msg = ValidationError.getErrorText(msg);
            }
            else {
                auditController.error(AuditRecord.LogCode.ReprocessAEOper, getErrorMessage(t), null, t);
            }
            return new RpcRes_Base<>(0, true, msg);
        }
    }

    private String checkIdPst(Long idError, String idPstCorr, ErrorCorrectType correctType) {
        GLErrorRecord errorRecord = errorRepository.findById(GLErrorRecord.class, idError);
        if (EDIT == correctType.getCorrectType() && idPstCorr.equals(errorRecord.getAePostingIdNew())) {
            return null;
        }

        EtlPosting postingErr = etlPostingRepository.findById(EtlPosting.class, errorRecord.getEtlPostingRef());
        if (null == postingErr)
            return String.format("Для ошибки ID = %d не найдено входящее сообщение АЕ", idError);
        List<GLOperation> opers = errorRepository.getOperationCorrPost(idPstCorr, errorRecord.getSourcePosting());
        if (null == opers || opers.isEmpty())
            return String.format("По данному ID_PST '%s' нет операции с источнеиком сделки '%s' в статусе 'POST'",
                    idPstCorr, errorRecord.getSourcePosting());
        if (opers.size() > 1)
            return String.format("По данному ID_PST '%s' более одной операции (%d) в статусе 'POST'", idPstCorr, opers.size());
        GLOperation operCorr = opers.get(0);
        EtlPosting postingCorr = etlPostingRepository.findById(EtlPosting.class, operCorr.getEtlPostingRef());
        if (postingCorr == null) {
            return String.format("Не найдена проводка GL_ETLPST по PST_REF %s для сопоставления данных", operCorr.getEtlPostingRef());
        } else {
            // SRC_PST, EVT_ID, DEAL_ID, SUBDEALID, PMT_REF, NRT
            boolean suitable = isEqual(postingCorr.getEventId(), postingErr.getEventId())
                    || isEqual(postingCorr.getDealId(), postingErr.getDealId())
                    || isEqual(postingCorr.getPaymentRefernce(), postingErr.getPaymentRefernce())
                    || isEqual(postingCorr.getNarrative(), postingErr.getNarrative())
                    ;
            if (!suitable) {
                return String.format("Для сообщение АЕ с ID_PST '%s' найдена операция в статусе 'POST' c GLOID = %d" +
                        "\nУ исправительной и ошибочной операции должно совпадать хотя бы одно значение: " +
                        "\nИД события, ИД сделки (субделки), ИД платежа, Описание" +
                        "\nИсправительная операция не соответсвует ошибочному сообщению АЕ", idPstCorr, operCorr.getId());
            }
        }
        return null;
    }

    private boolean isEqual(Object ob1, Object ob2) {
        return null != ob1 && ob1.equals(ob2);
    }

    public String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, DefaultApplicationException.class,
                PersistenceException.class);
    }

}
