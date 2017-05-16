package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import org.apache.commons.lang3.time.DateUtils;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.BaseEntityRepository;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static java.lang.String.format;
import static org.apache.commons.lang3.time.DateUtils.parseDate;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.COB;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.controller.operday.task.stamt.StamtUnloadController.STAMT_UNLOAD_FULL_DATE_KEY;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by Ivan Sevastyanov on 18.02.2016.
 */
public class TaskUtils {
    @Inject
    private OperdayController operdayController;
    @EJB
    private GLOperationRepository repository;

    public static final String CHECK_RUN_KEY = "checkRun";

    public static Boolean getCheckRun(Properties properties, boolean defaultValue) {
        return Boolean.parseBoolean(Optional.ofNullable(
                properties.getProperty(CHECK_RUN_KEY)).orElse(Boolean.toString(defaultValue)).trim());
    }

    public static Date getExecuteDate(String dateKey, Properties properties, Date defaultDate) throws ParseException {
        String propday = Optional.ofNullable(properties.getProperty(dateKey)).orElse("");
        Date operday;
        if (isEmpty(propday)) {
            operday = defaultDate;
        } else {
            operday = DateUtils.parseDate(propday, "dd.MM.yyyy", "yyyy-MM-dd");
        }
        return operday;
    }

    /**
     * Вычисляется дату за которую формируем выгрузку
     * @param properties может быть передана в <code>properties</code>
     * @param repository для работы с БД
//     * @param operday операционный день
     * @return дата выгрузки (операционый день)
     * @throws SQLException если ошибка на уровне
     * @throws ParseException ошибка парсинге даты переданной в <code>properties</code>
     */
    public static Date getDateFromGLOD(Properties properties, BaseEntityRepository repository, Operday operday) throws SQLException, ParseException {
        String propday = properties.getProperty(STAMT_UNLOAD_FULL_DATE_KEY);
            if (propday == null || propday.isEmpty()) {
                DataRecord dateRecord = repository.selectOne("SELECT * FROM GL_OD");
            if (dateRecord != null) {
                if (ONLINE.name().equals(dateRecord.getString("PHASE"))) {
                    return dateRecord.getDate("LWDATE");
                } else if (COB.name().equals(dateRecord.getString("PHASE"))) {
                    return dateRecord.getDate("CURDATE");
                } else {
                    throw new ValidationError(ErrorCode.TASK_ERROR, "Illegal operday state: " + dateRecord.getString("PHASE"));
                }
            } else {
                return operday.getCurrentDate();
            }
            } else {
                return parseDate(propday, "dd.MM.yyyy", "yyyy-MM-dd");
            }
    }

    public static String getStringValue(Properties properties, String key, String defaultValue) {
        return Optional.ofNullable(properties.getProperty(key)).orElse(defaultValue);
    }

    public static long getDwhAlreadyHeaderCount(Date executeDate, DwhUnloadParams params, BaseEntityRepository repository) throws SQLException {
        return repository.selectFirst(
                "select count(1) cnt from GL_ETLDWHS " +
                        "where PARNAME = ? and PARVALUE in (0, 1) and PARDESC = ? and OPERDAY = ?"
                , params.getParamName(), params.getParamDesc(), executeDate).getLong(0);
    }

    public long createHeaders(DwhUnloadParams unloadParams) throws Exception {
        return createHeaders(unloadParams, operdayController.getOperday().getCurrentDate());
    }

    public long createHeaders(DwhUnloadParams unloadParams, Date operdate) throws Exception {
        return repository.executeInNewTransaction(persistence -> {
            // нужна запись о готовности выгрузки счетов, так как это вьюха - GLVD_ACC
            repository.executeNativeUpdate(
                    "insert into GL_ETLDWHS (PARNAME,PARVALUE,PARDESC,OPERDAY,START_LOAD,END_LOAD) values (?,?,?,?,?,?)"
                    , unloadParams.getParamName(), DwhUnloadStatus.STARTED.getFlag(), unloadParams.getParamDesc()
                    , operdate, operdayController.getSystemDateTime(), null);
            return repository.selectFirst("SELECT IDENTITY_VAL_LOCAL() id FROM SYSIBM.SYSDUMMY1").getLong("id");
        });
    }

    public void setResultStatus(long headerId, DwhUnloadStatus status) throws Exception {
        if (headerId > 0) {
            int count = repository.executeInNewTransaction(persistence ->
                    repository.executeNativeUpdate("update GL_ETLDWHS set PARVALUE = ?, END_LOAD = ? where ID = ?"
                            , status.getFlag(), operdayController.getSystemDateTime(), headerId));
            Assert.isTrue(1 == count, format("Нет данных для обновления <%d>", headerId));
        }
    }

}
