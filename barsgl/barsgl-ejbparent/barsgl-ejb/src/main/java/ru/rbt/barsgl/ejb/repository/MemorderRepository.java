package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.Memorder;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejb.integr.pst.MemorderController;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.shared.Assert;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;

import static ru.rbt.barsgl.ejb.entity.gl.Memorder.CancelFlag.N;

/**
 * Created by Ivan Sevastyanov
 */
@Stateless
public class MemorderRepository extends AbstractBaseEntityRepository<Memorder, Long> {

    @EJB
    private MemorderController memorderController;

    public void createMemorders(List<GLPosting> postings) {
        for (GLPosting posting : postings) {
            Assert.isTrue(!posting.getPdList().isEmpty(), "Пустой список полупроводок");
            final Pd pd = posting.getPdList().get(0);

            Memorder memorder = new Memorder();
            memorder.setId(posting.getId());
            memorder.setCancelFlag(N);
            memorder.setNumber(memorderController.nextMemorderNumber(pd.getPod(), pd.getBsaAcid()
                    , posting.getOperation().isCorrection()));
            memorder.setDocType(memorderController.getDocType(posting));
            memorder.setPostDate(pd.getPod());
            save(memorder);
        }
    }

}
