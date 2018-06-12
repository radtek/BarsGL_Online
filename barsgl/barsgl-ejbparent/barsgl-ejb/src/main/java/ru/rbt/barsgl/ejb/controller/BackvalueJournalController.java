package ru.rbt.barsgl.ejb.controller;

import ru.rb.cfg.CryptoUtil;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.gl.BackvalueJournal.BackvalueJournalState;
import ru.rbt.barsgl.ejb.etc.SshProcedureRunner;
import ru.rbt.barsgl.ejb.props.PropertyName;
import ru.rbt.barsgl.ejb.repository.BackvalueJournalRepository;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.DataAccessCallback;
import ru.rbt.ejbcore.controller.etc.TextResourceController;
import ru.rbt.ejbcore.datarec.DBParam;
import ru.rbt.ejbcore.datarec.DBParams;
import ru.rbt.ejbcore.datarec.DataRecord;

import javax.ejb.AccessTimeout;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MINUTES;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Localization;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Task;
import static ru.rbt.barsgl.ejb.controller.operday.task.StartLoaderTask.StartLoaderType.ssh;
import static ru.rbt.barsgl.ejb.entity.gl.BackvalueJournal.BackvalueJournalState.ERROR_LC;
import static ru.rbt.barsgl.ejb.entity.gl.BackvalueJournal.BackvalueJournalState.NEW;

/**
 * Created by Ivan Sevastyanov
 * доступ к пересчету/локализации должен быть синхронизирован
 */
@Singleton
@AccessTimeout(value = 15, unit = MINUTES)
public class BackvalueJournalController {

    public static final Logger logger = Logger.getLogger(BackvalueJournalController.class.getName());

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @Inject
    private ru.rbt.ejbcore.util.DateUtils dateUtils;

    @EJB
    private BackvalueJournalRepository journalRepository;

    @EJB
    private AuditController auditController;

    @EJB
    private OperdayController operdayController;

    @EJB
    private PropertiesRepository propertiesRepository;

    @Inject
    private SshProcedureRunner sshProcedureRunner;

    @Inject
    private TextResourceController textController;

    /**
     * полный пересчет/локализация и пересчет остатков по БС2
     * @throws Exception
     */
    public void recalculateBackvalueJournal() throws Exception {
        journalRepository.executeInNewTransaction(persistence -> recalculateLocal());
    }

    /**
     * пересчет/локализация
     * @return кол-во пересчитанных счетов
     * todo refactoring не будет работать в сеансах локализации
     */
    public int recalculateLocal() throws Exception {
        return processRecalcException(connection -> {
            auditController.info(Localization, format("Начало пересчета/локализации по остаткам backvalue buffer"));
            // пересчет производится по таблице GL_LOCACC
            journalRepository.executeInNewTransaction(persistence -> {
                DBParams result = journalRepository.executeCallable(textController.getContent("ru/rbt/barsgl/ejb/controller/local/InsertLocal.sql")
                        , DBParams.createParams(new DBParam(Types.INTEGER, DBParam.DBParamDirectionType.OUT)
                                , new DBParam(Types.INTEGER, DBParam.DBParamDirectionType.OUT)));
                auditController.info(Localization, format("Записей для локализации backvalue %s, всего сырых записей %s"
                        , result.getParams().get(0).getValue(), result.getParams().get(1).getValue()));
                return result.getParams().get(0).getValue();
            });
            // пересчет - локализация
            callGlCorrLocal();
            int accs = (Integer) journalRepository.executeInNewTransaction(persistence -> {
                DBParams result = journalRepository.executeCallable(textController.getContent("ru/rbt/barsgl/ejb/controller/local/UpdateLocal.sql")
                        , DBParams.createParams(new DBParam(Types.INTEGER, DBParam.DBParamDirectionType.OUT)));
                auditController.info(Localization, format("Всего установлен статус 'ОБРАБОТАНО' для '%s' сырых записей ", result.getParams().get(0).getValue()));
                return result.getParams().get(0).getValue();
            });

            return accs;

        }, ERROR_LC, NEW, "пересчет/локализация");
    }

    public int recalculateLocalIncrementBuffer() throws Exception {
        auditController.info(Task, format("Начало пересчета/локализации по остаткам backvalue buffer"));
        // пересчет производится по таблице GL_LOCACC
        int accs = journalRepository.executeInNewTransaction(persistence -> {
            logger.info(format("deleted from GL_LOCACC: %s", journalRepository.executeNativeUpdate("delete from GL_LOCACC")));
            return journalRepository.executeTransactionally(connection -> {
                int count[] = {0};
                try (PreparedStatement statement = connection.prepareStatement(
                        "select bsaacid, acid, min(dat) dat \n" +
                        " from gl_baltur \n" +
                        "where moved = 'Y' \n" +
                        "  and pkg_localize_filter.check_total(bsaacid,acid, dat) = '1' \n" +
                        "group by bsaacid, acid ");
                     ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        journalRepository.executeNativeUpdate("insert into GL_LOCACC (bsaacid,acid,pod) values (?,?,?)"
                                , rs.getString(1), rs.getString(2), rs.getDate(3));
                        count[0]++;
                    }
                }
                return count[0];
            });
        });
        auditController.info(Task, format("Записей для локализации backvalue %s", accs));
        // пересчет - локализация
        callGlCorrLocal();

        return accs;
    }

    public void callGlCorrLocal() {
        try {
            String barsGlLoaderType = propertiesRepository.getString(PropertyName.BARSGL_LOCALIZATION_TYPE.getName());
            if(ssh.name().equals(barsGlLoaderType)){
                String host = propertiesRepository.getString(PropertyName.BARSGL_LOCALIZATION_SSH_HOST.getName());
                Long portObj = propertiesRepository.getNumber(PropertyName.BARSGL_LOCALIZATION_SSH_PORT.getName());
                int port = (portObj == null) ? 22 : portObj.intValue();
                String user = propertiesRepository.getString(PropertyName.BARSGL_LOCALIZATION_SSH_USER.getName());
                String ecryptedPswd = propertiesRepository.getString(PropertyName.BARSGL_LOCALIZATION_SSH_PSWD.getName());
                String pswd = CryptoUtil.decrypt(ecryptedPswd);
                String cmd = propertiesRepository.getString(PropertyName.BARSGL_LOCALIZATION_SSH_RUN_CMD.getName());
                auditController.info(Localization, String.format("Вызов пересчета/локализации чере SSH <%s:%s>: команда  %s", host, port, cmd));
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                sshProcedureRunner.executeSshCommand(host, user, pswd, port, cmd, os);
                auditController.stat(Localization, "Пересчет/локализации по SSH прошел успешно", new String(os.toByteArray(), "cp1251"));
            } else {
                journalRepository.executeInNewTransaction(pers -> {
                    journalRepository.executeNativeUpdate("{call GL_CORRLOCAL}");
                    return null;
                });
            }
        } catch (Throwable e) {
            auditController.error(Task, "Ошибка при пересчете/локализации", null, e);
        }
        
    }

    private int setBackvalueJournalState(BackvalueJournalState targetState, BackvalueJournalState from) {
        return journalRepository.executeUpdate("update BackvalueJournal j set j.state = ?1 where j.state = ?2"
                , targetState, from);
    }

    private int processRecalcException(DataAccessCallback<Integer> callback, BackvalueJournalState errorState, BackvalueJournalState fromState, String actionName) throws Exception {
        try {
            return journalRepository.executeTransactionally(callback);
        } catch (Exception e) {
            auditController.error(Task, format("Ошибка при %s. " +
                    "Записи не прошедшие пересчет/локализацию в таблице GL_BVJRNL.STATE = '%s'", actionName, errorState.name()), null, e);
            journalRepository.executeInNewTransaction(persistence ->
                    setBackvalueJournalState(errorState, fromState));
            throw e;
        }
    }

}
