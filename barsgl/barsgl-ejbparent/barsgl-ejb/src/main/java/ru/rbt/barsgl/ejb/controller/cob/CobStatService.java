package ru.rbt.barsgl.ejb.controller.cob;

import ru.rbt.barsgl.ejb.entity.cob.CobStatistics;
import ru.rbt.barsgl.ejb.repository.cob.CobStatRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.cob.CobWrapper;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by ER18837 on 10.03.17.
 */
public class CobStatService {
    @Inject
    private CobStatRepository statRepository;

    public RpcRes_Base<CobWrapper> getInfo(Long idCob) {
        CobWrapper wrapper = new CobWrapper();
        try {
            if (null == idCob)
                idCob = statRepository.getMaxCobId();
            if (null == idCob) {
                return new RpcRes_Base<CobWrapper>(wrapper, true, "Нет ни одной записи о расчете COB");
            }
            List<CobStatistics> stepList = statRepository.getCobSteps(idCob);

            return new RpcRes_Base<CobWrapper>(wrapper, false, "");
        } catch (Throwable t) {
            return new RpcRes_Base<CobWrapper>(wrapper, true, t.getMessage());
        }
    }
}
