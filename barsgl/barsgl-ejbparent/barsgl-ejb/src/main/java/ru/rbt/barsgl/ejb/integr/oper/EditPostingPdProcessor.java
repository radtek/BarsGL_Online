package ru.rbt.barsgl.ejb.integr.oper;

import ru.rbt.barsgl.ejb.entity.gl.AbstractPd;
import ru.rbt.barsgl.ejb.entity.gl.Memorder;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejb.repository.MemorderRepository;
import ru.rbt.shared.Assert;

import javax.ejb.EJB;
import java.util.Date;
import java.util.List;

/**
 * Created by ER18837 on 11.04.16.
 */
public class EditPostingPdProcessor extends EditPostingProcessor {

    @EJB
    private MemorderRepository memorderRepository;

    public List<Long> getOperationPdIdList(long parentId) {
        return pdRepository.getOperationPdIdList(parentId);
    }

    public List<? extends AbstractPd> getOperationPdList(List<Long> pdIdList) {
        return pdRepository.getOperationPdList(pdIdList);
    }

    @Override
    public void updateMemOrder(AbstractPd debit, String memorderNumber, Memorder.DocType docType) {
        Memorder mo = memorderRepository.findById(Memorder.class, debit.getPcId());
        Assert.isTrue(null != mo, "Не найден мемордер для PCID = " + debit.getPcId());
        mo.setPostDate(debit.getPod());
        mo.setNumber(memorderNumber);
        mo.setDocType(docType);
        memorderRepository.update(mo);
    }

    @Override
    public void updatePd(List<? extends AbstractPd> pdList) {
        pdList.forEach(pd -> {
            pdRepository.update((Pd)pd);
        });
    }

}
