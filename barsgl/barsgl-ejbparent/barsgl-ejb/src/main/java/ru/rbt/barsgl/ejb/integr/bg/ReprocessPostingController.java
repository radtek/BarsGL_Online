package ru.rbt.barsgl.ejb.integr.bg;

import org.apache.log4j.Logger;

import javax.ejb.*;
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

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int correctPostings(List<Long> errorsList, String comment, String idPstNew, boolean reprocess) {
        return 0;
    }
}