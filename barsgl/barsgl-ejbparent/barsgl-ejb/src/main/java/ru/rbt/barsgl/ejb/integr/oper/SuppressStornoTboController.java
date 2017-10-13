package ru.rbt.barsgl.ejb.integr.oper;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.shared.enums.OperState;

import javax.ejb.EJB;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.PreCob;
import static ru.rbt.barsgl.shared.enums.DealSource.KondorPlus;
import static ru.rbt.ejbcore.mapping.YesNo.N;
import static ru.rbt.ejbcore.mapping.YesNo.Y;
import static ru.rbt.barsgl.shared.enums.OperState.POST;

/**
 * Created by ER21006 on 14.01.2016.
 *
 * Подавление дублирующих проводок по сделкам TBO
 * выполняется в PRE_COB в самом конце
 */
public class SuppressStornoTboController {

    @EJB
    private GLOperationRepository repository;

    @EJB
    private OperdayController operdayController;

    @EJB
    private AuditController auditController;

    public int suppress() throws Exception {
        return repository.executeTransactionally(connection -> {
            int count = 0;
            try (PreparedStatement queryStatement
                         = connection.prepareStatement(
                    "select o.gloid, o.strn_glo, p.pcid \n" +
                    "  from gl_oper o, gl_posting p \n" +
                    " where o.procdate = ? and o.src_pst = ? \n" +
                    "   and o.strn = ? and o.state = ? \n" +
                    "   and o.gloid = p.glo_ref")) {
                bindParams1(queryStatement);
                try (ResultSet rsSetStorno = queryStatement.executeQuery()) {
                    while (rsSetStorno.next()) {
                        final Optional<DataRecord> parentOper = findParentOperation(rsSetStorno.getLong("strn_glo"));
                        Optional<DataRecord> grandParentOper = Optional.empty();
                        if (!parentOper.isPresent()) {
                            auditController.warning(PreCob, "Подавление дублирующихся проводок по сделкам TBO"
                                    , null, format("Не найдена сторнируемая операция '%s' для сторно операции '%s'"
                                        , rsSetStorno.getLong("strn_glo"), rsSetStorno.getLong("gloid")));
                        } else {
                            grandParentOper = findGrandParentOperation(parentOper.get(), rsSetStorno.getLong("strn_glo"));
                            if (grandParentOper.isPresent()) {
                                repository.executeNativeUpdate("update pd p set p.invisible = '1' where p.pcid in (?,?)"
                                        , rsSetStorno.getLong("pcid"), grandParentOper.get().getLong("pcid"));
                                repository.executeNativeUpdate("update gl_oper o set o.state = ? where o.gloid in (?,?)"
                                        , OperState.INVISIBLE.name(), rsSetStorno.getLong("gloid"), grandParentOper.get().getLong("gloid"));
                                auditController.info(PreCob, format("Подавлены дублирующиеся проводки по сделкам ТВО %s, %s"
                                        , rsSetStorno.getLong("pcid"), grandParentOper.get().getLong("pcid")));
                                count += 2;
                            }
                        }
                    }
                }
            }
            return count;
        });
    }

    private void bindParams1(PreparedStatement queryStatement) throws SQLException {
        queryStatement.setDate(1, operdayController.getOperday().getCurrentSqlDate());
        queryStatement.setString(2, KondorPlus.getLabel());
        queryStatement.setString(3, Y.name());
        queryStatement.setString(4, POST.name());
    }

    private Optional<DataRecord> findParentOperation(long parentGloid) throws SQLException {
        return Optional.ofNullable(repository.selectFirst(
                "select p.* " +
                        "  from gl_oper p" +
                        " where p.gloid = ? and p.state = ?", parentGloid, POST.name()));
    }

    private Optional<DataRecord> findGrandParentOperation(DataRecord parentOperation, long notEquals) throws SQLException {
        DataRecord record = repository.selectFirst(
                "select o.gloid, p.pcid from gl_oper o, gl_posting p \n" +
                        " where deal_id = ? \n" +
                        "   and ac_dr = ? and ac_cr = ? and amt_dr = ? \n" +
                        "   and amt_cr = ? and vdate = ? and state = ? \n" +
                        "   and o.gloid = p.glo_ref and o.gloid <> ? \n" +
                        "   and o.procdate = ? and o.strn = ?"
                , parentOperation.getString("deal_id")
                , parentOperation.getString("ac_dr")
                , parentOperation.getString("ac_cr")
                , parentOperation.getBigDecimal("amt_dr")
                , parentOperation.getBigDecimal("amt_cr")
                , parentOperation.getDate("vdate")
                , POST.name()
                , notEquals
                , operdayController.getOperday().getCurrentDate()
                , N.name());

        return Optional.ofNullable(record);
    }
}
