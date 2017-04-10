package ru.rbt.barsgl.ejb.security;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.sec.GLErrorRecord;
import ru.rbt.barsgl.ejb.repository.EtlPostingRepository;
import ru.rbt.barsgl.ejb.repository.GLErrorRepository;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.ExceptionUtils;

import javax.ejb.*;
import javax.inject.Inject;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created by ER18837 on 08.02.17.
 */
@SuppressWarnings("ALL")    // TODO надо это?
@Stateless
@LocalBean
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class GLErrorController {

    private static final Logger log = Logger.getLogger(GLErrorController.class);
    public static final String ERR_DELIMITER = "; \n";

    @EJB
    GLErrorRepository errorRepository;

    @Inject
    EtlPostingRepository postingRepository;

    @Inject
    OperdayController operdayController;

    public void error(String message, BaseEntity entity, Throwable e) {
        String errorCode = "";
        String errorMessage = "";
        ValidationError error = ExceptionUtils.findException(e, ValidationError.class);
        if (null == error) {    // это нераспознанная ошибка
            errorMessage = ExceptionUtils.getErrorMessage(e,
                    NullPointerException.class, DataTruncation.class, SQLException.class, DefaultApplicationException.class);
        } else {
            errorCode = error.getErrorCode();
            errorMessage = error.getRowMessage();
        }
        error(message, entity, errorCode, errorMessage);
    }

    public void error(String message, BaseEntity entity, List<ValidationError> errors) {
        String[] codeMessage = getListMessages(errors);
        error(message, entity, codeMessage[0] , codeMessage[1] );
    }

    public void error(String message, BaseEntity entity, String errorCode, String errorMessage) {
        Long etlPostingRef = null, glOperRef = null;
        String aePostingId = null, sourcePosting = null;

        try {
            if (null == entity) {
                log.error("Не задана ссылка на ошибочную проводку / операцию при записи в таблицу ошибок: " + message);
                etlPostingRef = 0L;
            } else if (entity instanceof EtlPosting) {
                EtlPosting posting = (EtlPosting) entity;
                etlPostingRef = posting.getId();
                aePostingId = posting.getAePostingId();
                sourcePosting = posting.getSourcePosting();
            } else if (entity instanceof GLOperation) {
                GLOperation operation = (GLOperation) entity;
                glOperRef = operation.getId();
                etlPostingRef = operation.getEtlPostingRef();
                aePostingId = operation.getAePostingId();
                sourcePosting = operation.getSourcePosting();
            } else {
                log.error(String.format("Задана неверная ссылка: '%s', '%s' на ошибочную проводку / операцию при записи в таблицу ошибок",
                        entity.getClass().getName(), entity.getId().toString()));
                return;
            }
        } catch (Throwable t) {
            log.error("Ошибка подготовки данных для записи в журнал ошибок: " + t.getMessage());
        }

        Date procdate = operdayController.getOperday().getCurrentDate();
        createErrorRecord(etlPostingRef, glOperRef, aePostingId, sourcePosting,
                message, errorCode, errorMessage, procdate);
    }

    public static String[] getListMessages(List<ValidationError> errors) {
        Iterator<ValidationError> it = errors.iterator();
        if (! it.hasNext())
            return new String[] {"", ""};

        StringBuilder codes = new StringBuilder();
        StringBuilder messages = new StringBuilder();
        for (;;) {
            ValidationError e = it.next();
            codes.append(e.getErrorCode());
            messages.append(e.getRowMessage());
            if (! it.hasNext())
                return new String[] {codes.toString(), messages.toString()};
            codes.append(ERR_DELIMITER);
            messages.append(ERR_DELIMITER);
        }
    }

    private void createErrorRecord(
            Long etlPostingRef, Long glOperRef, String aePostingId, String sourcePosting,
            String errorType, String errorCode, String errorMessage, Date procDate) {
        try {
            errorRepository.invokeAsynchronous(persistence -> {
                GLErrorRecord errorRecord = errorRepository.createErrorRecord(
                        etlPostingRef, glOperRef, aePostingId, sourcePosting,
                        errorType, errorCode, errorMessage, procDate);
                return errorRepository.save(errorRecord);
            });
        } catch (Throwable e) {
            log.error("Ошибка записи в системный журнал: " + e.getMessage(), e);
        }
    }


}
