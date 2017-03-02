package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.sec.GLErrorRecord;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static ru.rbt.barsgl.ejbcore.util.StringUtils.substr;

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
        errorRecord.setAePostingId(substr(aePostingId, 128));
        errorRecord.setSourcePosting(substr(sourcePosting, 128));
        errorRecord.setErrorType(substr(errorType, 512));
        errorRecord.setErrorCode(substr(errorCode, 64));
        errorRecord.setErrorMessage(substr(errorMessage, 4000));
        errorRecord.setProcDate(procDate);
        errorRecord.setCorrect(YesNo.N);
        return errorRecord;
    }

    public GLErrorRecord getRecordByPstRef(Long pstRef) {
        return selectFirst(GLErrorRecord.class, "from GLErrorRecord g where g.etlPostingRef = ?1 order by g.id desc", pstRef);
    }

    public GLErrorRecord getRecordByGloRef(Long gloRef) {
        return selectFirst(GLErrorRecord.class, "from GLErrorRecord g where g.glOperRef = ?1 order by g.id desc", gloRef);
    }

    public GLErrorRecord getRecordByRef(Long pstRef, Long gloRef) {
        if (null != pstRef) {
            return getRecordByPstRef(pstRef);
        } else
        if (null != gloRef) {
            return getRecordByGloRef(gloRef);
        } else
            return null;
    }

    public List<String> getSourceList(String idList) throws SQLException {
        List<DataRecord> res = select("select distinct SRC_PST from GL_ERRORS where ID in (" + idList + ")") ;
        return res.stream().map(r -> r.getString(0)).collect(Collectors.toList());
    }

    public List<String> getDateList(String idList) throws SQLException {
        List<DataRecord> res = select("select distinct PROCDATE from GL_ERRORS where ID in (" + idList + ")") ;

        return res.stream().map(r -> new SimpleDateFormat("dd.MM.yyyy").format(r.getDate(0))).collect(Collectors.toList());
    }

    public List<DataRecord> getPostingIdList(String idList) throws SQLException {
        return select("select distinct p.ID, p.ID_PKG from GL_ERRORS e join GL_ETLPST p on e.PST_REF = p.ID" +
                " where e.ID in (" + idList + ")") ;
    }

    public List<GLErrorRecord> getErrorsForReprocess(String idList) {
        return select(GLErrorRecord.class, "from GLErrorRecord e where e.id in (" + idList + ") and e.correct = ?1", YesNo.N);
//        DataRecord res = selectFirst("select count(1) from GL_ERRORS where value(CORRECT, 'N') = 'N' and ID in (" + idList + ")");
//        return res.getInteger(0);
    }

    public int setErrorsCorrected(String idList, String comment, String idPstNew, Date timestamt, String userName) {
        return executeNativeUpdate("update GL_ERRORS e set e.CORRECT = ?1, e.COMMENT = ?2, e.ID_PST_NEW = ?3, e.OTS_PROC = ?4, e.USER_NAME = ?5" +
                " where e.ID in (" + idList + ") and value(e.CORRECT, 'N') = ?6",
                YesNo.Y.name(), comment, idPstNew, timestamt, userName, YesNo.N.name());
    }

    public void updatePostingsStateReprocess(String idPstList, String idPkgList) {
        executeNativeUpdate("update GL_ETLPST p set p.ECODE = ?1, p.EMSG = ?2 where p.ID in (" + idPstList + ")",
                null, null);
        executeNativeUpdate("update GL_ETLPKG p set p.STATE = ?1, p.DT_LOAD = max(p.DT_LOAD, CURRENT TIMESTAMP - 5 DAYS)" +
                " where p.ID_PKG in (" + idPkgList + ")",
                EtlPackage.PackageState.LOADED.name());
    }

}
