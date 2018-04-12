package ru.rbt.barsgl.ejb.common.controller.od;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.controller.etc.TextResourceController;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.CallableStatement;
import java.sql.Types;
import java.util.logging.Logger;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.KillSession;

@Stateless
@LocalBean
public class OperdaySupportBean {


    private static final Logger log = Logger.getLogger(OperdaySupportBean.class.getName());

    public static final String PST_TABLE_NAME = "PST";

    @Inject
    private TextResourceController textResourceController;

    @EJB
    private CoreRepository repository;

    @EJB
    private AuditController auditController;

    /**
     * завершаем задачи которые блокируют table
     * @param table таблица
     * @throws Exception
     */
    public void removeLock(String table) throws Exception {
        log.info("Try remove lock from " + table);
        String killSessionBlock = textResourceController.getContent("ru/rbt/barsgl/ejb/controller/od/kill_sessions.sql");
        repository.executeTransactionally(connection -> {
            try (CallableStatement st = connection.prepareCall(killSessionBlock)) {
                st.setString(1, table.toUpperCase());
                st.registerOutParameter(2, Types.INTEGER);
                st.executeUpdate();
                int killed = st.getInt(2);
                auditController.warning(KillSession, format("Удалено сеансов '%s' блокирующих таблицу '%s'", killed, table));
            }
            return null;
        });
    }


}
