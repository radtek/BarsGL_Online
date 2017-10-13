package ru.rbt.barsgl.ejb.integr.oper;

import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLBackValueOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;

/**
 * Created by er18837 on 29.06.2017.
 */
public class BackValuePostingProcessor extends IncomingPostingProcessor {

    @Override
    public boolean isSupported(EtlPosting posting) {
        return null != posting
                && !posting.isTech()
                && posting.isBackValue();   // !posting.isFan() && !posting.isStorno() &&
    }

    @Override
    public GLOperation newOperation(EtlPosting posting) {
        return new GLBackValueOperation();
    }

}
