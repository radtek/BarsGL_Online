package ru.rbt.barsgl.ejb.integr.oper;

import ru.rbt.barsgl.ejb.entity.gl.AbstractPd;
import ru.rbt.barsgl.ejb.entity.gl.GLPd;
import ru.rbt.barsgl.ejb.repository.GLPdRepository;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

/**
 * Created by ER18837 on 11.04.16.
 */
public class EditPostingGLPdProcessor extends EditPostingProcessor {

    @Inject
    private GLPdRepository glPdRepository;

    public List<Long> getOperationPdIdList(long parentId) {
        return glPdRepository.getOperationPdIdList(parentId);
    }

    public List<? extends AbstractPd> getOperationPdList(List<Long> pdIdList) {
        return glPdRepository.getOperationPdList(pdIdList);
    }

    @Override
    public void updateMemOrder(Date pod, boolean isCorrection, AbstractPd debit, AbstractPd credit) {
        ((GLPd)debit).setMemorderNumber(memorderController.nextMemorderNumber(pod, debit.getBsaAcid(), isCorrection));
        ((GLPd)debit).setDocType(memorderController.getDocTypeNotFan(isCorrection, debit.getBsaAcid(), credit.getBsaAcid(), pod));
    }


    @Override
    public void updatePd(List<? extends AbstractPd> pdList) {
        pdList.forEach(pd -> glPdRepository.update((GLPd)pd));
    }
}
