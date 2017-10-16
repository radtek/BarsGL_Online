package ru.rbt.barsgl.ejb.integr.oper;

import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLBackValueOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;

import javax.inject.Inject;

/**
 * Created by er18837 on 30.06.2017.
 */
public class TechBackValuePostingProcessor extends TechAccPostingProcessor {

    @Inject
    BackValuePostingProcessor backValuePostingProcessor;

    @Override
    public boolean isSupported(EtlPosting posting) {
        return null != posting
                && !posting.isFan() && posting.isTech()
                && posting.isBackValue();
    }

    @Override
    public GLOperation newOperation(EtlPosting posting) {
        return backValuePostingProcessor.newOperation(posting);
    }

}
