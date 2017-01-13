package ru.rbt.barsgl.ejbtest.utl;

import org.junit.Test;
import ru.rbt.barsgl.ejbtest.AbstractRemoteTest;
import ru.rbt.barsgl.ejb.repository.GLPostingRepository;

/**
 * Created by ER22317 on 03.12.2015.
 */
public class GLPostingRep extends AbstractRemoteTest {

    @Test
    public void testAccount2(){
        String ret = remoteAccess.invoke(GLPostingRepository.class, "getExchangeAccount2", "222");
        System.out.println("testAccount2 = " + ret);
    }
}
