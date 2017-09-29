package ru.rbt.barsgl.ejb.integr.oper;

import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;

/**
 * Created by ER18837 on 04.03.15.
 * Обработчик двойных проводок в режиме online
 */
public class OrdinaryPostingProcessor extends IncomingPostingProcessor {

    @Override
    public boolean isSupported(EtlPosting posting) {
        return null != posting
                && !posting.isTech()
                && !posting.isBackValue();   // !posting.isFan() && !posting.isStorno() &&
    }

}
