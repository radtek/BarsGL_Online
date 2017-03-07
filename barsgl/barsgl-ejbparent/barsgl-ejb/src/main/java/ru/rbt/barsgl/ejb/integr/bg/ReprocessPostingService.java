package ru.rbt.barsgl.ejb.integr.bg;

import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.sec.GLErrorRecord;
import ru.rbt.barsgl.ejb.repository.EtlPostingRepository;
import ru.rbt.barsgl.ejb.repository.GLErrorRepository;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.util.StringUtils;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.ErrorList;
import ru.rbt.barsgl.shared.ExceptionUtils;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.ErrorCorrectType;

import javax.ejb.EJB;
import javax.persistence.PersistenceException;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.List;

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
                errorList.addErrorDescription("В списке более одного источника сделки: " + StringUtils.listToString(sources, ", "));
            }
            List<String> dates = errorRepository.getDateList(idList);
            if (dates.size() != 1) {
                errorList.addErrorDescription("В списке более одной даты опердня: " + StringUtils.listToString(dates, ", "));
            }
            List<String> notUnique = errorRepository.getNotUniqueList(idList);
            if (notUnique.size() != 0) {
                errorList.addErrorDescription("В списке есть операции с совпадающим ID_PST: " + StringUtils.listToString(notUnique, ", "));
            }
            if (!StringUtils.isEmpty(idPstCorr)) {
                String err = checkIdPst(errorIdList.get(0), idPstCorr);
                if (!StringUtils.isEmpty(err))
                    errorList.addErrorDescription(err);
            }
            if (!errorList.isEmpty()) {
                return new RpcRes_Base<>(0, true, errorList.getErrorMessage());
            }

            int cnt = reprocessController.correctPostingErrors(errorIdList, comment, idPstCorr, correctType);

            return new RpcRes_Base<>(errorIdList.size(), false,
                    String.format("Сообщения АЕ (%d) %s\nДата опердня: '%s'\nИсточник сделки: '%s'", cnt,
                            correctType.getTypeMessage(), dates.get(0), sources.get(0)));
        } catch (Throwable t) {
            return new RpcRes_Base<>(0, true, getErrorMessage(t));
        }
    }

    private String checkIdPst(Long idError, String idPstCorr) {
        GLErrorRecord errorRecord = errorRepository.findById(GLErrorRecord.class, idError);
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
