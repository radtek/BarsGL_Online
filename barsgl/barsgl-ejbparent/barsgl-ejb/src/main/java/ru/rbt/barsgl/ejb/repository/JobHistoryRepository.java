package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.entity.task.JobHistory;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by Ivan Sevastyanov on 17.02.2016.
 */
@Stateless
@LocalBean
public class JobHistoryRepository extends AbstractBaseEntityRepository<JobHistory, Long> {

    @EJB
    private OperdayController operdayController;

    public boolean isTaskOK (String taskName, Date operday) {
        return null != selectFirst(JobHistory.class
                , "from JobHistory h where h.jobName = ?1 and h.operday = ?2 and h.result = ?3"
                , taskName, operday, DwhUnloadStatus.SUCCEDED);
    }

    public boolean isAlreadyRunning(String taskName, Date operday) {
        return null != selectFirst(JobHistory.class
                , "from JobHistory h where h.jobName = ?1 and h.operday = ?2 and h.result = ?3"
                , taskName, operday, DwhUnloadStatus.STARTED);
    }

    public boolean isAlreadyRunning(String taskName, Long idHist, Date operday) {
        return null != selectFirst(JobHistory.class
                , "from JobHistory h where h.jobName = ?1 and h.id != ?2 and h.operday = ?3 and h.result = ?4"
                , taskName, idHist, operday, DwhUnloadStatus.STARTED);
    }

    public JobHistory createHeader(String jobName, Date operday) {
        JobHistory hist = new JobHistory(jobName, operday);
        hist.setStarttime(operdayController.getSystemDateTime());
        hist.setResult(DwhUnloadStatus.STARTED);
        return save(hist);
    }

    public JobHistory updateStatus(JobHistory jobHistory, DwhUnloadStatus status) {
        jobHistory.setResult(status);
        jobHistory.setEndtime(operdayController.getSystemDateTime());
        return update(jobHistory);
    }

    public JobHistory getAlreadyRunningLike(Long notId, String jobNameLikeName) {
        List<Object> paramsPre = Arrays.asList(jobNameLikeName + "%", DwhUnloadStatus.STARTED);
        List params = new ArrayList<>();
        params.addAll(paramsPre);
        String jpaQuery = "from JobHistory h where h.jobName like ?1 and h.result = ?2";
        if (notId != null && notId > 0) {
            JobHistory historyNot = findById(JobHistory.class, notId);
            jpaQuery += " and h <> ?3";
            params.add(historyNot);
        }
        return selectFirst(JobHistory.class, jpaQuery, params.toArray());
    }

    public boolean isAlreadyRunningLike(Long notId, String jobNameLikeName) {
        return null != getAlreadyRunningLike(notId, jobNameLikeName);
    }
}
