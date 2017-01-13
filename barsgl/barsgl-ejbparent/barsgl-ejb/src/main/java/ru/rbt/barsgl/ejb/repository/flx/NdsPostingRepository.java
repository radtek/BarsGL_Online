package ru.rbt.barsgl.ejb.repository.flx;

import ru.rbt.barsgl.ejb.entity.flx.NdsPosting;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * Created by Ivan Sevastyanov on 23.11.2016.
 */
@Stateless
@LocalBean
public class NdsPostingRepository extends AbstractBaseEntityRepository<NdsPosting, Long> {
}
