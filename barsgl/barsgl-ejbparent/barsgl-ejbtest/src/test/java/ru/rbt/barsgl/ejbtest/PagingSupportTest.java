package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.shared.criteria.Criterion;
import ru.rbt.barsgl.shared.criteria.CriterionColumn;
import ru.rbt.barsgl.shared.criteria.OrderByColumn;
import ru.rbt.barsgl.shared.criteria.OrderByType;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Ivan Sevastyanov
 * @notdoc
 */
public class PagingSupportTest extends AbstractRemoteTest {

    @Test public void test() throws SQLException {
        final String sql = "select * from gl_oper where 1 = 1";
        DataRecord one = baseEntityRepository.selectFirst(sql);
        DataRecord two = baseEntityRepository.selectFirst("select * from gl_oper where GLOID > ?", one.getLong(0));

        final Criterion criterion = CriterionColumn.IN("GLOID", Arrays.asList(one.getLong("GLOID"), two.getLong(0)));

        final OrderByColumn ASC = new OrderByColumn("GLOID", OrderByType.ASC);
        List<DataRecord> list = pagingSupport.select(sql
                , criterion, 100, 1, ASC);
        Assert.assertEquals(2, list.size());
        Assert.assertEquals(one.getLong(0), list.get(0).getLong(0));
        Assert.assertEquals(two.getLong(0), list.get(1).getLong(0));

        list = pagingSupport.select(sql
                , criterion, 100, 1, new OrderByColumn("GLOID", OrderByType.DESC));
        Assert.assertEquals(2, list.size());
        Assert.assertEquals(two.getLong(0), list.get(0).getLong(0));
        Assert.assertEquals(one.getLong(0), list.get(1).getLong(0));

        int cnt = pagingSupport.count(sql, criterion);
        Assert.assertEquals(2, cnt);

        list = pagingSupport.selectRows(sql, criterion, 2, 1, ASC);
        Assert.assertEquals(2, list.size());
        Assert.assertEquals(one.getLong(0), list.get(0).getLong(0));
        Assert.assertEquals(two.getLong(0), list.get(1).getLong(0));

        list = pagingSupport.selectRows(sql, criterion, 2, 2, ASC);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(two.getLong(0), list.get(0).getLong(0));
    }

}
