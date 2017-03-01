package ru.rbt.barsgl.ejb.integr.bg;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.repository.GLErrorRepository;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.util.StringUtils;

import javax.ejb.*;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.List;

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

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int correctPostings(List<Long> errorIdList, String comment, String idPstNew, boolean reprocess) throws SQLException {
        String idList = StringUtils.listToString(errorIdList, ",");
        int wasCorrected = errorRepository.getCorrectedCount(idList);
        if (wasCorrected > 0) {
            throw new DefaultApplicationException(String.format("В списке есть переобработанные (закрытые) операции: %d", wasCorrected));
        }
        return 0;
    }
}