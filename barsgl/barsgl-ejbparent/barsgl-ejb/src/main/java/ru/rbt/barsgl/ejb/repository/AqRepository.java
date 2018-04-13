package ru.rbt.barsgl.ejb.repository;

import ru.rbt.ejbcore.controller.etc.TextResourceController;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by Ivan Sevastyanov on 13.04.2018.
 */
public class AqRepository extends AbstractBaseEntityRepository {

    @Inject
    private TextResourceController textResourceController;

    public String getNormalQueueName() throws SQLException {
        return selectOne("SELECT GLAQ_PKG_CONST.GET_BALANCE_QUEUE_NAME QNAME FROM DUAL").getString("QNAME");
    }

    public String getExceptionQueueName() throws SQLException {
        return selectOne("SELECT GLAQ_PKG_CONST.GET_BALANCE_EXC_QUEUE_NAME QNAME FROM DUAL").getString("QNAME");
    }

    public List<DataRecord> getQueuesStats() throws Exception {
        return select(textResourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/aq/queue_stats.sql"));
    }

}
