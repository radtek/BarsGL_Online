package ru.rbt.barsgl.ejb.common.repository.od;

import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.shared.Assert;

import java.sql.SQLException;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov
 */
public class OperdayRepository extends AbstractBaseEntityRepository <Operday, Date> {

    @Override
    public Operday save(Operday entity) {
        throw new IllegalAccessError("Requested an invalid operation");
    }

    @Override
    public Operday update(Operday entity) {
        throw new IllegalAccessError("Requested an invalid operation");
    }

    public DataRecord findWorkprocStep(String stepId, Date operday) throws SQLException {
        return selectFirst("select * from workproc p where trim(p.id) = ? and p.dat = ?"
                , stepId, operday);
    }

    public void updateOperdayPhase(Operday.OperdayPhase phase) {
        int cnt = executeUpdate("update Operday o set o.phase = ?1", phase);
        Assert.isTrue(1 == cnt);
    }
}
