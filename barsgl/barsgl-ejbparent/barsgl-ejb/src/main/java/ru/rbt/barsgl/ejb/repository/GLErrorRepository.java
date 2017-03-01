package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.sec.GLErrorRecord;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.sql.SQLException;
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

    public List<String> getListSources(String idList) throws SQLException {
        List<DataRecord> res = select("select distinct SRC_PST from GL_ERRORS where ID in (" + idList + ")") ;
        return res.stream().map(r -> r.getString(0)).collect(Collectors.toList());
    }

    public List<Date> getListDates(String idList) throws SQLException {
        List<DataRecord> res = select("select distinct PROCDATE from GL_ERRORS where ID in (" + idList + ")") ;
        return res.stream().map(r -> r.getDate(0)).collect(Collectors.toList());
    }

    public int getCorrectedCount(String idList) throws SQLException {
        DataRecord res = selectFirst("select count(1) from GL_ERRORS where CORRECT = 'Y' and ID in (" + idList + ")");
        return res.getInteger(0);
    }
}
