package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Date;

import static ru.rbt.barsgl.ejb.repository.WorkprocRepository.WorkprocState.E;
import static ru.rbt.barsgl.ejb.repository.WorkprocRepository.WorkprocState.W;

/**
 * Created by Ivan Sevastyanov on 17.02.2016.
 */
public class WorkprocRepository extends AbstractBaseEntityRepository {

    public enum WorkprocState {
        E("E"),O("O"),W("");

        private String value;

        WorkprocState(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public boolean isStepOK(DataSource dataSource, String stepName, Date operdate) throws SQLException {
        final DataRecord countWorkproc = selectFirst(dataSource,
                "select count(1) cnt\n" +
                        "  from workproc w \n" +
                        " where w.dat = ? \n" +
                        "   and trim(w.id) = ? and w.result = 'O'", operdate, stepName);
        return countWorkproc.getInteger("cnt") != 0;
    }

    /**
     * Наличие ожидающего (без ошибок '' или с ошибками <code>'E'</code>) шага <code>stepName</code> в операционном дне <code>operdate</code>
     * @param stepName шан загрузчика
     * @param operdate дата ОД (workporc.dat = operdate)
     * @return true если есть такой шаг с <code>result in '', 'E'</code>
     * @throws SQLException
     */
    public boolean isWaitingStepPresent(DataSource dataSource, String stepName, Date operdate) throws SQLException {
        final DataRecord countWorkproc = selectFirst(dataSource,
                "select count(1) cnt\n" +
                        "  from workproc w \n" +
                        " where w.dat = ? \n" +
                        "   and trim(w.id) = ? and w.result in (?, ?)", operdate, stepName, W.getValue(), E.getValue());
        return countWorkproc.getInteger("cnt") != 0;
    }

    /**
     * Обновляем статус ожидающего шага
     * @param stepName
     * @param operdate
     * @param state
     * @param message
     * @return
     */
    public int updateWorkproc(EntityManager persistence, String stepName, Date operdate, WorkprocState state, String message) {
        return executeNativeUpdate(persistence, "update workproc p set result = ?, msg = ?, count = value(count,0) + 1, endtime = current timestamp where dat = ? and id = ?"
            , state.getValue(), message, operdate, stepName);
    }

    /**
     * Обновление сообщения
     * @param stepName
     * @param operdate
     * @param message
     * @return
     */
    public int updateWorkprocMessage(EntityManager persistence,String stepName, Date operdate, String message) {
        return executeNativeUpdate(persistence, "update workproc p set msg = ? where dat = ? and id = ?"
            , message, operdate, stepName);
    }

    public boolean isStepOK(DataSource dataSource, String stepName, Date operdate, String message) throws SQLException {
        final DataRecord countWorkproc = selectFirst(dataSource,
                "select count(1) cnt\n" +
                        "  from workproc w \n" +
                        " where w.dat = ? \n" +
                        "   and trim(w.id) = ? and w.result = 'O' and trim(w.msg) = ?", operdate, stepName, message);
        return countWorkproc.getInteger("cnt") != 0;
    }

    public boolean isStepOK(String stepName, Date operdate) throws SQLException {
        final DataRecord countWorkproc = selectFirst(
                "select count(1) cnt\n" +
                        "  from workproc w \n" +
                        " where w.dat = ? \n" +
                        "   and trim(w.id) = ? and w.result = 'O'", operdate, stepName);
        return countWorkproc.getInteger("cnt") != 0;
    }

    /**
     * Наличие ожидающего (без ошибок '' или с ошибками <code>'E'</code>) шага <code>stepName</code> в операционном дне <code>operdate</code>
     * @param stepName шан загрузчика
     * @param operdate дата ОД (workporc.dat = operdate)
     * @return true если есть такой шаг с <code>result in '', 'E'</code>
     * @throws SQLException
     */
    public boolean isWaitingStepPresent(String stepName, Date operdate) throws SQLException {
        final DataRecord countWorkproc = selectFirst(
                "select count(1) cnt\n" +
                        "  from workproc w \n" +
                        " where w.dat = ? \n" +
                        "   and trim(w.id) = ? and w.result in (?, ?)", operdate, stepName, W.getValue(), E.getValue());
        return countWorkproc.getInteger("cnt") != 0;
    }

    /**
     * Обновляем статус ожидающего шага
     * @param stepName
     * @param operdate
     * @param state
     * @param message
     * @return
     */
    public int updateWorkproc(String stepName, Date operdate, WorkprocState state, String message) {
        return executeNativeUpdate("update workproc p set result = ?, msg = ?, count = value(count,0) + 1, endtime = current timestamp where dat = ? and id = ?"
                , state.getValue(), message, operdate, stepName);
    }

    /**
     * Обновление сообщения
     * @param stepName
     * @param operdate
     * @param message
     * @return
     */
    public int updateWorkprocMessage(String stepName, Date operdate, String message) {
        return executeNativeUpdate("update workproc p set msg = ? where dat = ? and id = ?"
                , message, operdate, stepName);
    }

    public boolean isStepOK(String stepName, Date operdate, String message) throws SQLException {
        final DataRecord countWorkproc = selectFirst(
                "select count(1) cnt\n" +
                        "  from workproc w \n" +
                        " where w.dat = ? \n" +
                        "   and trim(w.id) = ? and w.result = 'O' and trim(w.msg) = ?", operdate, stepName, message);
        return countWorkproc.getInteger("cnt") != 0;
    }

    public DataRecord getWorkprocRecord(String stepId, Date dat) throws SQLException {
        return selectFirst("select * from workproc where id = ? and dat = ?", stepId, dat);
    }
}
