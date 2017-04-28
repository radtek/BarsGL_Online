package ru.rbt.barsgl.ejb.repository.dict;

import ru.rbt.barsgl.ejb.entity.dict.Acod;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by akichigi on 24.10.16.
 */
public class AcodRepository extends AbstractBaseEntityRepository<Acod, Long> {
    public Acod getNotUsedAcod(String acod) {
        return selectFirst(Acod.class, "from Acod T where T.acod = ?1 and upper(T.ename) = ?2", acod, "NOT USED");
    }
}
