package ru.rbt.barsgl.ejb.repository;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.controller.operday.task.LoadBranchDictTask;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.*;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static ru.rbt.audit.entity.AuditRecord.LogCode.LoadBranchDict;

/**
 * Created by er22317 on 08.02.2018.
 */
@Stateless
@LocalBean
public class BranchDictRepository<E extends BaseEntity<String>> extends AbstractBaseEntityRepository<E, String> {
    @EJB
    private AuditController auditController;

   public boolean isTaskProcessed(Date dtl) throws SQLException {
        return 0 < selectOne("select count(*) cnt from gl_loadstat where stream_id=? and dtl=? and status='P'", LoadBranchDictTask.streamId, dtl).getInteger(0);
   }


   @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
   public Date getMaxLoadDate() throws SQLException {
        return selectOne("select MAX_LOAD_DATE from V_GL_DWH_LOAD_STATUS", null).getDate(0);
   }
   public long insGlLoadStat(Date dtl, Date operday) throws SQLException {
        Long id = selectOne("select GL_LOADSTAT_SEQ.nextval from dual", new Object[]{}).getLong(0);
        executeNativeUpdate("insert into GL_LOADSTAT(ID, STREAM_ID, DTL, STATUS, OPERDAY, START_LOAD ) values(?,?,?,?,?,SYSDATE)", id, LoadBranchDictTask.streamId, dtl, "N", operday);
        return id;
   }
   public void updGlLoadStat(Long id, String status) throws SQLException {
        executeNativeUpdate("update GL_LOADSTAT set STATUS=?, END_LOAD=SYSDATE where ID=?", status, id);
   }

   @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
   public List<E> tableToList(Class<E> clazz, String nativeSql){
        return (List<E>) getPersistence().createNativeQuery( nativeSql, clazz).getResultList();
   }

    public void listToTable(List<E> list){
        list.forEach((item) -> save(item));
    }

    public void saveEntityNoFlash(E entity){
        save(entity, false);
    }

   public void nativeUpdate(String sql, Object[] params) {
        try {
            executeNativeUpdate(sql, params);
        } catch (Throwable e){
            auditController.error(LoadBranchDict, sql + " vs " + Arrays.stream(params).map(x->x.toString()).collect( Collectors.joining(",")), null, e);
            throw new DefaultApplicationException(e.getMessage(), e);
        }

    }
    public void jpaUpdateNoFlash(E entity){
        update(entity, false);
    }

   public <E> List<E> getAll(Class<E> clazz) {
        return select(clazz, "select t from " + clazz.getName() + " t", new Object[]{});
   }

}
