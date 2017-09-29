package ru.rbt.barsgl.ejb.integr.oper;

import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;

import javax.inject.Inject;

/**
 * Created by ER18837 on 05.03.15.
 * Обработчик сторно веерных проводок в режиме online
 */
@Deprecated
public class FanStornoPostingProcessor { // extends IncomingPostingProcessor {

/*
    @Inject
    private FanPostingProcessor fanPostingProcessor;

    @Override
    public boolean isSupported(EtlPosting posting) {
        return null != posting
                && posting.isFan() && posting.isStorno() && !posting.isTech();
    }
*/

    /*
    // TODO удалить потом - стандарнтое определение даты проводки
    @Override
    protected Date getStandardPostingDate(Date valueDate) {
        return fanPostingProcessor.getStandardPostingDate(valueDate);
    }*/
}