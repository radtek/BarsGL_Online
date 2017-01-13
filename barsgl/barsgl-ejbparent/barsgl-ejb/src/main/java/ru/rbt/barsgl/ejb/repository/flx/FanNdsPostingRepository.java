package ru.rbt.barsgl.ejb.repository.flx;

import ru.rbt.barsgl.ejb.entity.flx.FanNdsPosting;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by Ivan Sevastyanov on 23.11.2016.
 */
public class FanNdsPostingRepository extends AbstractBaseEntityRepository <FanNdsPosting, Long> {

    public Long nextId() {
        return nextId("SEQ_GL_FLF");
    }
}
