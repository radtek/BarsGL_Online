package ru.rbt.barsgl.ejb.integr.bg;

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
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.List;

import static ru.rbt.barsgl.shared.enums.ErrorCorrectType.CLOSE_LIST;
import static ru.rbt.barsgl.shared.enums.ErrorCorrectType.CLOSE_ONE;

/**
 * Created by ER18837 on 01.03.17.
 */
public class ReprocessPostingService {

    @Inject
    private GLErrorRepository errorRepository;

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
                errorList.addErrorDescription("В списке более одного источника сделки: " + StringUtils.listToString(sources, ","));
            }
            List<String> dates = errorRepository.getDateList(idList);
            if (dates.size() != 1) {
                errorList.addErrorDescription("В списке более одной даты опердня: " + StringUtils.listToString(dates, ","));
            }
            if (!StringUtils.isEmpty(idPstCorr) && !errorRepository.isOperCorrPost(idPstCorr)) {
                errorList.addErrorDescription(String.format("По данному ID_PST '%s' нет операции в статусе 'POST'", idPstCorr));
            }
            if (!errorList.isEmpty()) {
                return new RpcRes_Base<>(0, true, errorList.getErrorMessage());
            }

            int cnt = reprocessController.correctPostings(errorIdList, comment, idPstCorr, correctType);

            String msg =  (CLOSE_LIST == correctType || CLOSE_ONE == correctType) ? "помечены закрытыми" : "отправлены на переобработку" ;
            return new RpcRes_Base<>(errorIdList.size(), false,
                    String.format("Сообщения АЕ (%d) %s\nДата опердня: '%s'\nИсточник сделки: '%s'", cnt, msg, dates.get(0), sources.get(0)));
        } catch (Throwable t) {
            return new RpcRes_Base<>(0, true, getErrorMessage(t));
        }
    }

    public String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, DefaultApplicationException.class,
                PersistenceException.class);
    }

}
