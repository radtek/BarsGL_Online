import org.junit.Test;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejbtest.AbstractTimerJobTest;

/**
 * Created by ER22317 on 23.03.2017.
 */
public class TmpTest extends AbstractTimerJobTest {

    @Test
    public void test01() throws Exception {
        long stamp = System.currentTimeMillis();
        EtlPackage pkg = (EtlPackage) baseEntityRepository.findById(EtlPackage.class, 147252);
//        EtlPosting pst1

    }
}
