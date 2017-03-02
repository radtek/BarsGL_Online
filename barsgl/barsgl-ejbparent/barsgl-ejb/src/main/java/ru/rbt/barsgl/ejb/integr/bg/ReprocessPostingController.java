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

import static java.util.concurrent.TimeUnit.MINUTES;
import static ru.rbt.barsgl.shared.enums.ErrorCorrectType.CLOSE_LIST;
import static ru.rbt.barsgl.shared.enums.ErrorCorrectType.CLOSE_ONE;

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
        int cnt = errorRepository.setErrorsCorrected(idList, comment, idPstNew, userContext.getTimestamp(), userContext.getUserName());
        if (errorIdList.size() != cnt) {
            throw new DefaultApplicationException(String.format("В списке есть переобработанные (закрытые) операции: %d", errorIdList.size() - cnt));
        }

        if(CLOSE_LIST == correctType || CLOSE_ONE == correctType) {
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