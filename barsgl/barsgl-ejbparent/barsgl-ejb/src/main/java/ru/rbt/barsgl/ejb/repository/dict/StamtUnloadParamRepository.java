package ru.rbt.barsgl.ejb.repository.dict;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import ru.rbt.barsgl.ejb.entity.dict.StamtUnloadParam;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by ER21006 on 19.01.2016.
 */
@Stateless
@LocalBean
public class StamtUnloadParamRepository extends AbstractBaseEntityRepository<StamtUnloadParam, String> {
}
