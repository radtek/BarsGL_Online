package ru.rbt.barsgl.ejb.controller.operday.task.loader;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.entity.loader.LoadManagement;
import ru.rbt.barsgl.ejb.etc.AS400ProcedureRunner;
import ru.rbt.barsgl.ejb.repository.WorkdayRepository;
import ru.rbt.barsgl.ejb.repository.WorkprocRepository;
import ru.rbt.barsgl.ejb.repository.loader.LoadManagementRepository;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.barsgl.shared.enums.LoadManagementAction;
import ru.rbt.barsgl.shared.enums.LoadManagementStatus;
import ru.rbt.shared.enums.Repository;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Created by SotnikovAV on 03.11.2016.
 * @deprecated Управление загрузкой перенесенов в другое приложение
 */
@Deprecated
public class LoaderManagementTask implements ParamsAwareRunnable {

    private static final Logger log = Logger.getLogger(LoaderManagementTask.class);
    private static final String SCHEDULED_TASK_NAME = "LoadManagement";

    @Inject
    private WorkprocRepository workprocRepository;

    @Inject
    private WorkdayRepository workdayRepository;

    @Inject
    private LoadManagementRepository loadManagementRepository;

    @Inject
    private AS400ProcedureRunner as400Runner;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        Date workday = workdayRepository.getWorkday();
        for(Repository repository:Repository.values()) {
            EntityManager persistence = loadManagementRepository.getPersistence(repository);
            DataSource dataSource = loadManagementRepository.getDataSource(repository);
            execute(workday, persistence, dataSource);
        }
    }

    private void execute(Date currentWorkday, EntityManager persistence, DataSource dataSource) throws Exception {
        List<LoadManagement> loadManagementList = loadManagementRepository.findNative(
                persistence,
                LoadManagement.class,
                "select distinct ordid, ord from load_management where status=? order by ordid, ord",
                1000,
                LoadManagementStatus.Executed
        );
        if(null == loadManagementList || 0 == loadManagementList.size()) {
            log.debug("Нет незавершенных действий управления шагами загрузки");
            return;
        }

        for(LoadManagement loadManagement:loadManagementList) {
            if (currentWorkday.equals(loadManagement.getDat())) {
                executeCurrentDateAction(persistence, dataSource, loadManagement);
            } else {
                executeLastDateAction(persistence, dataSource, loadManagement);
            }
        }
    }

    private void executeCurrentDateAction(EntityManager persistence, DataSource dataSource, LoadManagement loadManagement) throws Exception {
        LoadManagementAction action = loadManagement.getAction();
        WorkprocRepository.WorkprocState state = null;
        switch(action) {
            case Restart:
                state = WorkprocRepository.WorkprocState.W;
                break;
            case SetError:
                state = WorkprocRepository.WorkprocState.E;
                break;
            case SetOK:
                state = WorkprocRepository.WorkprocState.O;
                break;
            default:
                log.debug("Действие не определено.");
                return;
        }
        workprocRepository.updateWorkproc(persistence, loadManagement.getCode(), loadManagement.getDat(), state, "");
        loadManagement.setStartTime(new Date());
        loadManagement.setFinishTime(new Date());
        loadManagementRepository.save(persistence, loadManagement);
    }

    private void executeLastDateAction(EntityManager persistence, DataSource dataSource, LoadManagement loadManagement) throws Exception {
        LoadManagementAction action = loadManagement.getAction();
        WorkprocRepository.WorkprocState state = null;
        switch(action) {
            case Restart:
                state = WorkprocRepository.WorkprocState.W;
                break;
            case SetError:
                state = WorkprocRepository.WorkprocState.E;
                break;
            case SetOK:
                state = WorkprocRepository.WorkprocState.O;
                break;
            default:
                log.debug("Действие не определено.");
                return;
        }
        workprocRepository.updateWorkproc(persistence, loadManagement.getCode(), loadManagement.getDat(), state, "");
        loadManagement.setStartTime(new Date());
        loadManagement.setStatus(LoadManagementStatus.Execution);
        loadManagementRepository.save(persistence, loadManagement);

        DataRecord rec = loadManagementRepository.selectFirst(dataSource, "select procedure from workname where code=?", loadManagement.getCode());
        String procedure = rec.getString("procedure");

        String datStr = new SimpleDateFormat("yyyy-MM-dd").format(loadManagement.getDat());

        as400Runner.callAsyncGl("bank.jar", procedure, new Object [] {datStr});
    }

}
