package ru.rbt.barsgl.ejb.repository.dict;

import ru.rbt.barsgl.ejb.entity.dict.ProfitCenter;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by akichigi on 04.08.16.
 */
public class ProfitCenterRepository extends AbstractBaseEntityRepository<ProfitCenter, String> {
    public boolean isProfitCenterExists(String id) {
        return null != selectFirst(ProfitCenter.class, "from ProfitCenter T where T.id = ?1", id);
    }

    public boolean isProfitCenterExists(String id, String name) {
        return null != selectFirst(ProfitCenter.class, "from ProfitCenter T where T.name = ?1 and T.id <> ?2",
                 name, id);
    }
}
