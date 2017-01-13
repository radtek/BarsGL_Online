package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.repository.GLPostingRepository;

import java.sql.SQLException;

/**
 * Created by ER21006 on 18.04.2016.
 */
public class CoreRepositoryTest extends AbstractRemoteTest {

    @Test public void testSequence() throws SQLException {
        Assert.assertTrue((Integer)remoteAccess.invoke(GLPostingRepository.class, "nextIntegerId", "SEQ_GL_AUT") > 0);
    }
}
