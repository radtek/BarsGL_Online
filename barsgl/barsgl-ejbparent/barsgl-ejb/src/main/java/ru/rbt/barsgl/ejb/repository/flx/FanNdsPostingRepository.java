package ru.rbt.barsgl.ejb.repository.flx;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import ru.rbt.barsgl.ejb.entity.flx.FanNdsPosting;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by Ivan Sevastyanov on 23.11.2016.
 */
@Stateless
@LocalBean
public class FanNdsPostingRepository extends AbstractBaseEntityRepository <FanNdsPosting, Long> {

    public Long nextId() {
        return nextId("SEQ_GL_FLF");
    }
}
