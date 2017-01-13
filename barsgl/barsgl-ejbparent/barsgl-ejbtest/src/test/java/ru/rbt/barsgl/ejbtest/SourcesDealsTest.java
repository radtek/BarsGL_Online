package ru.rbt.barsgl.ejbtest;

import org.junit.Test;
import ru.rbt.barsgl.ejb.entity.dict.SourcesDeals;
import ru.rbt.barsgl.ejb.repository.dict.SourcesDealsRepository;
import ru.rbt.barsgl.shared.Assert;

import java.util.List;

/**
 * Created by Ivan Sevastyanov on 14.07.2016.
 */
public class SourcesDealsTest extends AbstractRemoteTest {

    @Test public void test() {
        List<SourcesDeals> list = remoteAccess.invoke(SourcesDealsRepository.class, "getAllObjectsCached");
        Assert.isTrue(1 < list.size());
    }
}
