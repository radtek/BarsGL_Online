package ru.rbt.barsgl.ejb.controller.operday.task.md;

import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.shared.Repository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DBParam;
import ru.rbt.ejbcore.datarec.DBParams;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.EJB;
import java.sql.SQLException;
import java.util.Date;
import java.util.Optional;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.controller.operday.task.md.DismodOutState.L;
import static ru.rbt.barsgl.ejb.controller.operday.task.md.DismodOutState.S;
import static ru.rbt.ejbcore.datarec.DBParam.DBParamDirectionType.OUT;
import static ru.rbt.ejbcore.datarec.DBParam.DbParamType.*;

/**
 * Created by Ivan Sevastyanov on 29.11.2018.
 */
public class DismodRepository extends AbstractBaseEntityRepository {

    @EJB
    private CoreRepository repository;

    public long createDismodHeader (DismodParam param, Date operday) throws Exception {
        return (long) repository.executeInNewTransaction(persistence -> {
            DBParams result = repository.executeCallable(
                    "declare\n" +
                            "    l_id number;\n" +
                            "begin\n" +
                            "    insert into gl_md_log (id_key, parname, status, operday, start_load, end_load)\n" +
                            "                    values (seq_dismod.nextval, ?, ?, ?, systimestamp, null) returning id_key into l_id;\n" +
                            "    ? := l_id;\n" +
                            "end;"
                    , DBParams.createParams(new DBParam(VARCHAR, param.name()), new DBParam(VARCHAR, L.name())
                            , new DBParam(DATE, operday), new DBParam(LONG, OUT)));
            return result.getParams().stream().filter(p -> p.getDirectionType() == OUT).findFirst()
                    .map(p-> (long)p.getValue()).orElseThrow(() -> new DefaultApplicationException("out parameter is not found"));
        });
    }

    public void updateDismodHeader(long id, DismodOutState state) throws Exception {
        repository.executeInNewTransaction(persistence ->
                repository.executeNativeUpdate("update gl_md_log set status = ?, end_load = systimestamp where id_key = ?", state.name(), id));
    }

    public Optional<DataRecord> getDismodHeader(Date operday, DismodOutState state, DismodParam param) throws SQLException {
        return Optional.ofNullable(selectFirst("select * from gl_md_log where operday = ? and status = ? and parname = ?"
            , operday, state.name(), param.name()));
    }

    public Optional<DataRecord> getDismodOutState(String outLogTablename, String processName, Date operday) throws SQLException {
        return Optional.ofNullable(selectFirst(getDataSource(Repository.BARSGLNOXA), format("select * from %s where operday = ? and status = ? and process_nm = ?"
                , outLogTablename), operday, S.name(), processName));
    }

    public int executeNOXA(String sql, Object ... params) throws SQLException {
        return executeNativeUpdate(getDataSource(Repository.BARSGLNOXA), sql, params);
    }

}
