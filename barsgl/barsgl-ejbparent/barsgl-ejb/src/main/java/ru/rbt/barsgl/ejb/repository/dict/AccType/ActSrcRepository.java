package ru.rbt.barsgl.ejb.repository.dict.AccType;

import ru.rbt.barsgl.ejb.entity.dict.AccType.ActSrc;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import java.util.List;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * Created by akichigi on 30.09.16.
 */
@Stateless
@LocalBean
public class ActSrcRepository extends AbstractBaseEntityRepository<ActSrc, Long> {

    public List<ActSrc> getActSrcByAct(String acttype) {
       return  select(ActSrc.class, "from ActSrc r where r.acctype = ?1", acttype);
    }
}
