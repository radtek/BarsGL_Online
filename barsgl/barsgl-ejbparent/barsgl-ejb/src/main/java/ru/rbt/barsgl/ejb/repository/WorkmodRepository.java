package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.shared.Repository;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by SotnikovAV on 11.09.2017.
 */
public class WorkmodRepository extends AbstractBaseEntityRepository {

    public boolean isStepAlreadyRunning(Repository enumRepository, String stepCode) throws Exception {
        final DataRecord countWorkproc = selectFirst(getDataSource(enumRepository),
                "select count(1) cnt\n" +
                        "  from workmod w \n" +
                        " where trim(code) = ? \n" +
                        " and run = 'Y' " +
                        " with NC", stepCode);
        return countWorkproc.getInteger("cnt") > 0;
    }

}
