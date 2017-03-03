package ru.rbt.barsgl.ejb.integr.bg;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.repository.GLErrorRepository;
import ru.rbt.barsgl.ejb.security.UserContext;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.util.StringUtils;
import ru.rbt.barsgl.shared.enums.ErrorCorrectType;

import javax.ejb.*;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import static ru.rbt.barsgl.shared.enums.ErrorCorrectType.CorrectType.*;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Created by ER18837 on 27.02.17.
 * переобработка операций АЕ
 */
@Singleton
@AccessTimeout(value = 5, unit = MINUTES)
public class ReprocessPostingController {

    private static final Logger log = Logger.getLogger(ReprocessPostingController.class);

    @Inject
    private GLErrorRepository errorRepository;

    @Inject
    private UserContext userContext;

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int correctPostings(List<Long> errorIdList, String comment, String idPstNew, ErrorCorrectType correctType) throws SQLException {
        String idList = StringUtils.listToString(errorIdList, ",");
        if (REPROC  == correctType.getCorrectType()) {
            List<String> opers = errorRepository.getOperPostList(idList);
            if (opers.size() != 0) {
                throw new DefaultApplicationException("В списке есть успешно обработанные операции со статусом 'POST', ИД АЕ: " + StringUtils.listToString(opers, ","));
            }
        }
        List<String> opers = errorRepository.getOperCorrList(idList);
        if (opers.size() != 0) {
            throw new DefaultApplicationException("В списке есть переобработанные (закрытые) операции, ИД АЕ: " + StringUtils.listToString(opers, ","));
        }
        int cnt = errorRepository.setErrorsCorrected(idList, correctType.getTypeName(), comment, idPstNew, userContext.getTimestamp(), userContext.getUserName());
        if (errorIdList.size() != cnt) {
            throw new DefaultApplicationException(String.format("Ошибка обновления таблицы GL_ERRORS. В списке %d записей, обновлено %d",
                    errorIdList.size(), cnt));
        }

        if(NEW  == correctType.getCorrectType()) {
            return cnt;
        }

        // получить список из GL_ETLPST: ID, PKG_ID
        List<DataRecord> postingList  = errorRepository.getPostingIdList(idList);
        String idPstList = StringUtils.listToString(postingList.stream().map(r -> r.getLong(0)).collect(Collectors.toList()), ",");
        String idPkgList = StringUtils.listToString(postingList.stream().map(r -> r.getLong(1)).collect(Collectors.toList()), ",");

        errorRepository.updatePostingsStateReprocess(idPstList, idPkgList);

        return cnt;
    }
}