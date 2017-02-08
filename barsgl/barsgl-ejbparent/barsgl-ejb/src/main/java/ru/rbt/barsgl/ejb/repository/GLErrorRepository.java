package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.sec.GLErrorRecord;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.sql.Date;

import static ru.rbt.barsgl.ejbcore.util.StringUtils.substr;

/**
 * Created by ER18837 on 08.02.17.
 */
@Stateless
@LocalBean
public class GLErrorRepository  extends AbstractBaseEntityRepository<GLErrorRecord, Long> {
    public GLErrorRecord createGLErrorRecord(
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

}
