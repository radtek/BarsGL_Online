package ru.rbt.barsgl.ejb.integr.oper;

import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;

import javax.ejb.EJB;

/**
 * Created by Ivan Sevastyanov
 * Обработчик сторно двойных проводок в режиме online
 */
public class StornoPostingProcessor extends IncomingPostingProcessor {

    @EJB
    private GLOperationRepository glOperationRepository;

    @Override
    public boolean isSupported(EtlPosting posting) {
        return null != posting
                && !posting.isFan() && posting.isStorno() && !posting.isTech();
    }

}
