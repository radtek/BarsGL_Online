package ru.rbt.barsgl.ejbtest;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by Ivan Sevastyanov
 * @notdoc
 */
public class TemporaryAETest extends AbstractRemoteTest {

    private static final Logger log = Logger.getLogger(TemporaryAETest.class);

    @Test
    @Ignore
    public void test() {

        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);

        final long pkgIdStart = 492L;
        final long pkgIdEnd = 492L;
        final int pstSize = 4;

/*
        final String pkgId = "197, 198, 199, 203, 207, 208, 222";
        final int pstSize = 26;
*/

        List<EtlPosting> postings = baseEntityRepository.select(EtlPosting.class,
                "from EtlPosting p where p.etlPackage.id between ?1 and ?2", pkgIdStart, pkgIdEnd);
//                  "from EtlPosting p where p.etlPackage.id in (197, 198, 199, 203, 207, 208, 222)"); 198
        Assert.assertEquals(pstSize, postings.size());

        for (EtlPosting posting : postings) {
            if (null != posting.getErrorCode()) {
                log.info("Posting is already proccessed, id: " + posting.getId());
                continue;
            }
            try {
                log.info("Posting is proccessing, id: " + posting.getId());
                GLOperation operation = (GLOperation) postingController.processMessage(posting);
                if (null == operation) {
                    throw new DefaultApplicationException("Operation is not created. See posting error message");
                }
                log.info("Operation is created succesfully id: " + operation.getId());
                List<GLPosting> glpostings = getPostings(operation);
                log.info("postings size: " + glpostings.size());
                for (GLPosting glposting : glpostings) {
                    List<Pd> pds = getPostingPd(glposting);
                    log.info("pd size: " + pds.size() + " on gl_post: " + glposting.getId());
                }
            } catch (Exception e) {
                log.error("Error on operation creation. Posting id: " + posting.getId() + ". " + e.getMessage());
                log.error(e.getMessage(), e);
            }
        }
    }

    @Test public void test2() throws SQLException {

//        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);

//        List<EtlPosting> postings = baseEntityRepository.select(EtlPosting.class, "from EtlPosting p where p.etlPackage.id between ?1 and ?2", 327L, 333L);
        //List<DataRecord> postings = baseEntityRepository.select("select * from gl_etlpst where vdate = date '2015-07-08' and evt_id like 'EVT\\_ID%' escape '\\'", new Object[]{});
        List<DataRecord> postings = baseEntityRepository.select("select * from gl_etlpst where vdate = date '2014-12-29' and evt_id like 'evtid%'", new Object[]{});
        //Assert.assertEquals(2, postings.size());

        for (DataRecord record : postings) {
            EtlPosting posting = (EtlPosting) baseEntityRepository.findById(EtlPosting.class, record.getLong("ID"));
            try {
                GLOperation operation = (GLOperation) postingController.processMessage(posting);
                if (null == operation) {
                    throw new DefaultApplicationException("Operation is not created. See posting error message");
                }
                log.info("Operation is created succesfully id: " + operation.getId());
                List<GLPosting> glpostings = getPostings(operation);
                log.info("postings size: " + glpostings.size());
                for (GLPosting glposting : glpostings) {
                    List<Pd> pds = getPostingPd(glposting);
                    log.info("pd size: " + pds.size() + " on gl_post: " + glposting.getId());
                }
            } catch (Exception e) {
                log.error("Error on operation creation. Posting id: " + posting.getId() + ". " + e.getMessage());
                log.error(e.getMessage(), e);
            }
        }
    }
}
