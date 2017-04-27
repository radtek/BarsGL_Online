package ru.rbt.barsgl.ejb.repository.flx;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import ru.rbt.barsgl.ejb.entity.flx.TransitNdsReference;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by Ivan Sevastyanov on 23.11.2016.
 */
@Stateless
@LocalBean
public class TransitNdsReferenceRepository extends AbstractBaseEntityRepository<TransitNdsReference, String> {
}
