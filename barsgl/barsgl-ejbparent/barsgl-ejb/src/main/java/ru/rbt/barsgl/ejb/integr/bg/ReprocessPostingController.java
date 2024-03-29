package ru.rbt.barsgl.ejb.integr.bg;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.repository.GLErrorRepository;
import ru.rbt.barsgl.ejb.security.UserContext;
import ru.rbt.barsgl.shared.enums.ErrorCorrectType;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;

import javax.ejb.*;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MINUTES;
import static ru.rbt.barsgl.shared.enums.OperState.*;

/**
 * Created by ER18837 on 27.02.17.
 * переобработка операций АЕ
 */
@Singleton
@AccessTimeout(value = 5, unit = MINUTES)
public class ReprocessPostingController {

    private static final Logger log = Logger.getLogger(ReprocessPostingController.class);

    @EJB
    private GLErrorRepository errorRepository;

    @Inject
    private UserContext userContext;

    @Inject
    BackValueOperationController bvOperationController;

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int correctPostingErrors(List<Long> errorIdList, String comment, String idPstNew, ErrorCorrectType correctType, OperState state) throws Exception {
        switch (correctType.getCorrectType()) {
            case NEW:
                return closePostingErrors(errorIdList, comment, idPstNew, correctType);
            case REPROC:
                return reprocessPostingErrors(errorIdList, comment, idPstNew, correctType, state);
            case EDIT:
                return editPostingErrors(errorIdList, comment, idPstNew, correctType);
            default:
                throw new DefaultApplicationException("Неверный тип корректировки");
        }
    }

    public int closePostingErrors(List<Long> errorIdList, String comment, String idPstNew, ErrorCorrectType correctType) throws SQLException {
        String idList = StringUtils.listToString(errorIdList, ",");
        List<String> opers = errorRepository.getOperCorrList(idList, YesNo.Y);
        if (opers.size() != 0) {
            throw new ValidationError(ErrorCode.REPROCESS_ERROR, "В списке есть переобработанные (закрытые) операции, ID_PST: " + StringUtils.listToString(opers, ", ", "'"));
        }
        int cnt = errorRepository.setErrorsCorrected(idList, correctType.getTypeName(), comment, idPstNew, userContext.getTimestamp(), userContext.getUserName());
        checkUpdate(errorIdList.size(), cnt);

        return cnt;
    }

    public int reprocessPostingErrors(List<Long> errorIdList, String comment, String idPstNew, ErrorCorrectType correctType, OperState state) throws Exception {
        String idList = StringUtils.listToString(errorIdList, ",");
        List<String> opers = errorRepository.getOperCorrList(idList, YesNo.Y);
        if (opers.size() != 0) {
            throw new ValidationError(ErrorCode.REPROCESS_ERROR, "В списке есть переобработанные (закрытые) операции, ИД АЕ: " + StringUtils.listToString(opers, ", ", "'"));
        }
        opers = errorRepository.getIdPstList(idList, OperState.POST);
        if (opers.size() != 0) {
            throw new ValidationError(ErrorCode.REPROCESS_ERROR, "В списке есть успешно обработанные операции со статусом 'POST', ID_PST: " + StringUtils.listToString(opers, ", ", "'"));
        }
        int cnt = errorRepository.setErrorsCorrected(idList, correctType.getTypeName(), comment, idPstNew, userContext.getTimestamp(), userContext.getUserName());
        checkUpdate(errorIdList.size(), cnt);

        if (BERCHK == state) {
            String gloids = StringUtils.listToString(errorRepository.getOperationIdList(idList), ",");
            errorRepository.updateBvOperationsStateReprocess(gloids, BLOAD);
        } else if (BERWTAC == state) {
            List<Long> gloidList = errorRepository.getOperationIdList(idList);
            String gloids = StringUtils.listToString(gloidList , ",");
            errorRepository.executeInNewTransaction(persistence -> {errorRepository.updateBvOperationsStateReprocess(gloids, BWTAC); return null;});
            bvOperationController.reprocessWtacBackValue(gloidList);
        } else {
            // получить список из GL_ETLPST: ID, PKG_ID
            List<DataRecord> postingList = errorRepository.getPostingIdList(idList);
            String idPsts = StringUtils.listToString(postingList.stream().map(r -> r.getLong(0)).collect(Collectors.toList()), ",");
            String idPkgs = StringUtils.listToString(postingList.stream().map(r -> r.getLong(1)).collect(Collectors.toList()), ",");
            errorRepository.updatePostingsStateReprocess(idPsts, idPkgs);
        }

        return cnt;
    }

    public int closeReprocessOperErrors(Long gloid, String comment) throws SQLException {
        List<String> errorIdList = errorRepository.getRecordIdsByRef(null, gloid, YesNo.N);
        if (null == errorIdList || errorIdList.isEmpty())
            return 0;

        String idList = StringUtils.listToString(errorIdList, ",");
        int cnt = errorRepository.setErrorsCorrected(idList, ErrorCorrectType.CorrectType.REPROC.name(), comment, null, userContext.getTimestamp(), userContext.getUserName());

        return cnt;
    }


    public int editPostingErrors(List<Long> errorIdList, String comment, String idPstNew, ErrorCorrectType correctType) throws SQLException {
        String idList = StringUtils.listToString(errorIdList, ",");
        List<String> opers = errorRepository.getOperCorrList(idList, YesNo.N);
        if (opers.size() != 0) {
            throw new ValidationError(ErrorCode.REPROCESS_ERROR, "В списке есть не переобработанные операции, ID_PST: " + StringUtils.listToString(opers, ","));
        }
        int cnt;
        if (!StringUtils.isEmpty(idPstNew)) {
            cnt = errorRepository.updateErrorsCorrected(idList, comment, idPstNew, userContext.getTimestamp(), userContext.getUserName());
        }
        else {
            cnt = errorRepository.updateErrorsCorrected(idList, comment, userContext.getTimestamp(), userContext.getUserName());
        }
        checkUpdate(errorIdList.size(), cnt);

        return cnt;
    }

    private void checkUpdate(int countNeed, int countIs) {
        if (countNeed != countIs) {
            throw new DefaultApplicationException(String.format("Ошибка обновления таблицы GL_ERRORS. В списке %d записей, обновлено %d",
                    countNeed, countIs));
        }

    }

}