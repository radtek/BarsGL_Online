package ru.rbt.barsgl.ejb.repository;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.controller.operday.task.LoadBranchDictTask;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.*;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static ru.rbt.audit.entity.AuditRecord.LogCode.LoadBranchDict;
import static ru.rbt.barsgl.shared.Repository.BARSGLNOXA;

/**
 * Created by er22317 on 08.02.2018.
 */
@Stateless
@LocalBean
public class BranchDictRepository<E extends BaseEntity<String>> extends AbstractBaseEntityRepository<E, String> {

    @EJB
    private AuditController auditController;

    @EJB
    private CoreRepository repository;

    public List<DataRecord> getMapAll() throws SQLException {
        return select("select * from dh_br_map", null);
    }

//   public boolean isTaskProcessed(Date dtl) throws SQLException {
//        return 0 < selectOne("select count(*) cnt from gl_loadstat where stream_id=? and dtl=? and status='P'", LoadBranchDictTask.streamId, dtl).getInteger(0);
//   }
    public boolean isTaskProcessed(Date dtl) throws SQLException {
        return null != selectFirst("select 1 from dual where exists (select 1 from gl_loadstat where stream_id=? and dtl=? and status='P')", LoadBranchDictTask.STREAM_ID, dtl);
    }

    public Date getMaxLoadDate() throws Exception {
        String sql = "select MAX_LOAD_DATE from V_GL_DWH_LOAD_STATUS";
        return Optional.ofNullable(selectOne(BARSGLNOXA, sql, null))
                .orElseThrow(()->new DefaultApplicationException(sql + " is null")).getDate(0);
    }

    public long insGlLoadStat(Date dtl, Date operday) throws Exception {
        Long id = selectOne(BARSGLNOXA, "select SEQ_GL_DEALCLOSE.nextval from dual", null).getLong(0);
        repository.executeInNewTransaction(getPersistence(BARSGLNOXA), p -> {
            executeNativeUpdate("insert into GL_LOADSTAT(ID, STREAM_ID, DTL, STATUS, OPERDAY, START_LOAD ) values(?,?,?,?,?,SYSDATE)", id, LoadBranchDictTask.STREAM_ID, dtl, "N", operday);
            return null;
        });
        return id;
    }

    public void updGlLoadStat(Long id, String status) throws Exception {
        repository.executeInNewTransaction(getPersistence(BARSGLNOXA), p -> {
            executeNativeUpdate(BARSGLNOXA, "update GL_LOADSTAT set STATUS=?, END_LOAD=SYSDATE where ID=?", status, id);
            return null;
        });
    }

//   @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<E> tableToList(Class<E> clazz, String nativeSql) throws Exception {
        return (List<E>) getPersistence(BARSGLNOXA).createNativeQuery(nativeSql, clazz).getResultList();
    }

   @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
   public void listToTable(List<E> list) throws Exception{
        for(E item: list){
            save(getPersistence(BARSGLNOXA), item);
        }
    }

   public void saveEntityNoFlash(E entity) throws Exception {
        save(getPersistence(BARSGLNOXA), entity, false);
   }

   public void nativeUpdate(String sql, Object[] params) {
       try {
            executeNativeUpdate(BARSGLNOXA, sql, params);
       } catch (Throwable e){
            auditController.error(LoadBranchDict, sql + " vs " + Arrays.stream(params).map(x->x.toString()).collect( Collectors.joining(",")), null, e);
            throw new DefaultApplicationException(e.getMessage(), e);
       }
   }

   public void jpaUpdateNoFlash(E entity) throws Exception {
        update(getPersistence(BARSGLNOXA), entity, false);
   }

   public E findByIdNoXa(Class<E> clazz, String primaryKey) throws Exception {
        return findById(clazz, primaryKey, getPersistence(BARSGLNOXA));
   }

    public <E> List<E> getAll(Class<E> clazz) throws Exception {
        return select( BARSGLNOXA, clazz, "select t from " + clazz.getName() + " t", new Object[]{});
    }
}
