package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.sec.GLErrorRecord;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.ejbcore.util.StringUtils;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static ru.rbt.barsgl.shared.enums.OperState.BLOAD;
import static ru.rbt.ejbcore.util.StringUtils.substr;

/**
 * Created by ER18837 on 08.02.17.
 */
@Stateless
@LocalBean
public class GLErrorRepository  extends AbstractBaseEntityRepository<GLErrorRecord, Long> {

    public GLErrorRecord createErrorRecord(
            Long etlPostingRef, Long glOperRef, String aePostingId, String sourcePosting,
             String errorType, String errorCode, String errorMessage, Date procDate) {
        GLErrorRecord errorRecord = new GLErrorRecord();
        errorRecord.setEtlPostingRef(etlPostingRef);
        errorRecord.setGlOperRef(glOperRef);
        errorRecord.setAePostingId(StringUtils.ifEmpty(substr(aePostingId, 128), " "));
        errorRecord.setSourcePosting(StringUtils.ifEmpty(substr(sourcePosting, 128), " "));
        errorRecord.setErrorType(StringUtils.ifEmpty(substr(errorType, 512), " "));
        errorRecord.setErrorCode(StringUtils.ifEmpty(substr(errorCode, 64), " "));
        errorRecord.setErrorMessage(StringUtils.ifEmpty(substr(errorMessage, 4000), " "));
        errorRecord.setProcDate(procDate);
        errorRecord.setCorrect(YesNo.N);
        return errorRecord;
    }

/*
    public GLErrorRecord getRecordByPstRef(Long pstRef) {
        return selectFirst(GLErrorRecord.class, "from GLErrorRecord g where g.etlPostingRef = ?1 order by g.id desc", pstRef);
    }

    public GLErrorRecord getRecordByGloRef(Long gloRef) {
        return selectFirst(GLErrorRecord.class, "from GLErrorRecord g where g.glOperRef = ?1 order by g.id desc", gloRef);
    }
*/

    public GLErrorRecord getRecordByRef(Long pstRef, Long gloRef) {
        if (null != pstRef) {
            return selectFirst(GLErrorRecord.class, "from GLErrorRecord g where g.etlPostingRef = ?1 order by g.id desc", pstRef);
        } else
        if (null != gloRef) {
            return selectFirst(GLErrorRecord.class, "from GLErrorRecord g where g.glOperRef = ?1 order by g.id desc", gloRef);
        } else
            return null;
    }

    public List<String> getRecordIdsByRef(Long pstRef, Long gloRef, YesNo isCorrect) throws SQLException {
        List<DataRecord> res = Collections.EMPTY_LIST;
        if (null != pstRef) {
            res = select("select e.ID from GL_ERRORS e where e.PST_REF = ? and coalesce(e.CORRECT, 'N') = ?", pstRef, isCorrect.name()) ;
        } else
        if (null != gloRef) {
            res = select("select e.ID from GL_ERRORS e where e.GLO_REF = ? and coalesce(e.CORRECT, 'N') = ?", gloRef, isCorrect.name()) ;
        }
        return res.stream().map(r -> r.getString(0)).collect(Collectors.toList());
    }

    public List<String> getNotUniqueList(String idList) throws SQLException {
        List<DataRecord> res = select("select ID_PST from GL_ERRORS where ID in (" + idList + ") group by ID_PST having count(1) > 1") ;
        return res.stream().map(r -> r.getString(0)).collect(Collectors.toList());
    }

    public List<String> getSourceList(String idList) throws SQLException {
        List<DataRecord> res = select("select distinct SRC_PST from GL_ERRORS where ID in (" + idList + ")") ;
        return res.stream().map(r -> r.getString(0)).collect(Collectors.toList());
    }

    public List<String> getDateList(String idList) throws SQLException {
        List<DataRecord> res = select("select distinct PROCDATE from GL_ERRORS where ID in (" + idList + ")") ;
        return res.stream().map(r -> new SimpleDateFormat("dd.MM.yyyy").format(r.getDate(0))).collect(Collectors.toList());
    }

    public List<String> getErrorCodeList(String idList) throws SQLException {
        List<DataRecord> res = select("select distinct ERR_CODE from GL_ERRORS where ID in (" + idList + ")") ;
        return res.stream().map(r -> r.getString(0)).collect(Collectors.toList());
    }

    public List<String> getOperStateList(String idList) throws SQLException {
        List<DataRecord> res = select("select distinct o.STATE from GL_ERRORS e left join GL_OPER o on e.ID_PST = o.ID_PST" +
                " where e.ID in (" + idList + ")") ;
        return res.stream().map(r -> r.getString(0)).collect(Collectors.toList());
    }

    public List<String> getIdPstList(String idList, OperState state) throws SQLException {
        List<DataRecord> res = select("select distinct e.ID_PST from GL_ERRORS e left join GL_OPER o on e.ID_PST = o.ID_PST" +
                " where e.ID in (" + idList + ") and o.STATE = ?", state.name()) ;
        return res.stream().map(r -> r.getString(0)).collect(Collectors.toList());
    }

    public List<String> getOperCorrList(String idList, YesNo isCorrect) throws SQLException {
        List<DataRecord> res = select("select e.ID_PST from GL_ERRORS e " +
                " where e.ID in (" + idList + ") and coalesce(e.CORRECT, 'N') = ?", isCorrect.name()) ;
        return res.stream().map(r -> r.getString(0)).collect(Collectors.toList());
    }

    public List<Long> getOperationIdList(String idList) throws SQLException {
        List<DataRecord> res = select("select e.GLO_REF from GL_ERRORS e where e.ID in (" + idList + ")") ;
        return res.stream().map(r -> r.getLong(0)).collect(Collectors.toList());
    }

    public List<GLOperation> getOperationCorrPost(String idPstNew, String srcPst) {
        return select(GLOperation.class, "from GLOperation o where o.sourcePosting = ?1 and o.aePostingId = ?2 and o.state = ?3"
                , srcPst, idPstNew, OperState.POST ) ;
    }

    public List<DataRecord> getPostingIdList(String idList) throws SQLException {
        return select("select distinct p.ID, p.ID_PKG from GL_ERRORS e join GL_ETLPST p on e.PST_REF = p.ID" +
                " where e.ID in (" + idList + ")") ;
    }

    public int setErrorsCorrected(String idList, String corrType, String comment, String idPstNew, Date timestamt, String userName) {
        int cnt =  executeNativeUpdate("update GL_ERRORS e set e.CORRECT = ?, e.\"COMMENT\" = ?, e.ID_PST_NEW = ?, e.OTS_PROC = ?, e.USER_NAME = ?, e.CORR_TYPE = ?," +
                " e.ID_PKG = (select ID_PKG from GL_ETLPST p where p.ID = e.PST_REF)" +
                " where e.ID in (" + idList + ") and coalesce(e.CORRECT, 'N') = ?",
                YesNo.Y.name(), comment, idPstNew, timestamt, userName, corrType, YesNo.N.name());
        executeNativeUpdate("update GL_ERRORS e set e.OLD_PKG_DT = (select DT_LOAD from GL_ETLPKG g where g.ID_PKG = e.ID_PKG)" +
                " where e.ID in (" + idList + ") and e.OLD_PKG_DT is null");
        return cnt;
    }

    public void updatePostingsStateReprocess(String idPstList, String idPkgList) {
        executeNativeUpdate("update GL_ETLPST p set p.ECODE = ?, p.EMSG = ? where p.ID in (" + idPstList + ")",
                null, null);
        executeNativeUpdate("update GL_ETLPKG p set p.STATE = ?, p.DT_LOAD = greatest(p.DT_LOAD, systimestamp - 5)" +
                " where p.ID_PKG in (" + idPkgList + ")",
                EtlPackage.PackageState.LOADED.name());
    }

    public void updateBvOperationsStateReprocess(String idOperList, OperState state) {
        executeNativeUpdate("update GL_OPER o set o.STATE = ?, o.EMSG = ? where o.GLOID in (" + idOperList + ")",
                state.name(), null);
    }

    public int updateErrorsCorrected(String idList, String comment, Date timestamt, String userName) {
        return executeNativeUpdate("update GL_ERRORS e set e.\"COMMENT\" = ?, e.OTS_PROC = ?, e.USER_NAME = ?" +
                " where e.ID in (" + idList + ") and e.CORRECT = ?", comment, timestamt, userName, YesNo.Y.name());
    }

    public int updateErrorsCorrected(String idList, String comment, String idPstNew, Date timestamt, String userName) {
        return executeNativeUpdate("update GL_ERRORS e set e.\"COMMENT\" = ?, e.ID_PST_NEW = ?, e.OTS_PROC = ?, e.USER_NAME = ?" +
                " where e.ID in (" + idList + ") and e.CORRECT = ?", comment, idPstNew, timestamt, userName, YesNo.Y.name());
    }

}
