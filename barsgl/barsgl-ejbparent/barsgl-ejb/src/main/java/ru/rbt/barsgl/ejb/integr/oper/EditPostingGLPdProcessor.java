package ru.rbt.barsgl.ejb.integr.oper;

import ru.rbt.barsgl.ejb.entity.gl.AbstractPd;
import ru.rbt.barsgl.ejb.entity.gl.GLPd;
import ru.rbt.barsgl.ejb.entity.gl.Memorder;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejb.repository.GLPdRepository;
import ru.rbt.ejbcore.DefaultApplicationException;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

/**
 * Created by ER18837 on 11.04.16.
 */
public class EditPostingGLPdProcessor extends EditPostingProcessor {

    @Inject
    private GLPdRepository glPdRepository;

    @Override
    public List<Long> getOperationPdIdList(long parentId) {
        return glPdRepository.getOperationPdIdList(parentId);
    }

    @Override
    public List<? extends AbstractPd> getOperationPdList(List<Long> pdIdList) {
        return glPdRepository.getOperationPdList(pdIdList);
    }

    @Override
    public void updateMemOrder(AbstractPd debit, String memorderNumber, Memorder.DocType docType) {
        ((GLPd)debit).setMemorderNumber(memorderNumber);
        ((GLPd)debit).setDocType(docType);
    }

    @Override
    public void updatePd(List<? extends AbstractPd> pdList) {
        pdList.forEach(pd -> glPdRepository.update((GLPd)pd, false));
        pdRepository.flush();
    }

}
