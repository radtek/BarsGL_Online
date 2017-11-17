package ru.rbt.barsgl.ejb.common.controller.od;

import com.google.common.collect.ImmutableMap;
import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.common.repository.od.OperdayRepository;
import ru.rbt.barsgl.shared.enums.AccessMode;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.job.BackgroundJobService;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.shared.Assert;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;

import javax.annotation.PostConstruct;
import javax.ejb.AccessTimeout;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.Singleton;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MINUTES;
import static javax.ejb.LockType.READ;
import static javax.ejb.LockType.WRITE;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.CLOSED;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.*;
import static ru.rbt.ejbcore.util.ServerUtils.findAssignable;

/**
 * Created by Ivan Sevastyanov
 */
@Singleton
@AccessTimeout(value = 15, unit = MINUTES)
public class OperdayController {

    private static final Logger log = Logger.getLogger(OperdayController.class);

    @EJB
    private OperdayRepository repository;

    @EJB
    private BankCalendarDayRepository calendarDayRepository;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private BackgroundJobService jobService;

    @Inject
    private Instance<SystemTimeService> timeServices;

    private Operday operday;

    private SystemTimeService timeService;

    @Lock(READ)
    public Operday getOperday() {
        return operday;
    }

    @Lock(WRITE)
    public void openNextOperDay() throws Exception {
        Date operDayToOpen = calendarDayRepository.getWorkdayAfter(operday.getCurrentDate()).getId().getCalendarDate();
        log.info(format("Открываем следующий операционный день '%s'", dateUtils.onlyDateString(operDayToOpen)));
        int cnt = repository.executeUpdate("update Operday o set o.currentDate = ?1, o.phase = ?2, o.lastWorkingDay = ?3, o.lastWorkdayStatus = ?4",
                operDayToOpen, ONLINE, operday.getCurrentDate(), OPEN);
        Assert.isTrue(1 == cnt);
        init();
    }

    @Lock(WRITE)
    public void refresh() throws Exception {
        init();
    }

    @Lock(WRITE)
    public void closeLastWorkdayBalance() throws Exception {
        Assert.isTrue(OPEN == operday.getLastWorkdayStatus());
        int cnt = repository.executeUpdate("update Operday o set o.lastWorkdayStatus = ?1",
                CLOSED);
        Assert.isTrue(1 == cnt);
        init();
    }

    @Lock(WRITE)
    public void setPreCOB() throws Exception {
        Assert.isTrue(ONLINE == operday.getPhase(), "Операционный день не в статусе 'ONLINE'!");
        repository.updateOperdayPhase(PRE_COB);
        init();
    }

    @Lock(WRITE)
    public void setCOB() throws Exception {
        Assert.isTrue(PRE_COB == operday.getPhase());
        repository.updateOperdayPhase(COB);
        init();
    }

    @Lock(READ)
    public Date getSystemDateTime() {
        return timeService.getCurrentTime();
    }

    /**
     * переключение режима обработки проводок
     * @param fromMode
     * @return установленный в результате выполнения swithPdMode() режим
     */
    @Lock(WRITE)
    public Operday.PdMode swithPdMode(Operday.PdMode fromMode) throws Exception {
        Assert.isTrue(operday.getPdMode() == fromMode
                , () -> new DefaultApplicationException(format("Неактуальный режим обработки проводок: '%s'", fromMode)));
        Assert.isTrue(1 == repository.executeUpdate("update Operday o set o.pdMode = ?1 where o.pdMode = ?2"
                , Operday.PdMode.switchMode(operday.getPdMode()), fromMode)
                , ()-> new DefaultApplicationException(format("Неактуальный режим работы обработки проводок в СУБД: '%s'", fromMode)));
        init();
        return Operday.PdMode.switchMode(operday.getPdMode());
    }

    @PostConstruct
    public void init() {
        initOperday();
        initTimeService();
    }

    /**
     * флаг обработки
     * @return
     */
    public ProcessingStatus getProcessingStatus() {
        try {
            return ProcessingStatus.valueOf(repository.selectOne("select PRC from GL_OD").getString("PRC"));
        } catch (SQLException e) {
            throw new DefaultApplicationException("Не удалось получить флаг резрешения обработки", e);
        }
    }

    /**
     * разрешена ли обработка
     * @return <b>true</b> если рарзаботка разрешена, иначе <b>false</b>
     */
    @Lock(WRITE)
    public boolean isProcessingAllowed() {
        ProcessingStatus status = getProcessingStatus();
        return  status == ProcessingStatus.ALLOWED || status == ProcessingStatus.STARTED;
    }

    @Lock(WRITE)
    public void setProcessingStatus(ProcessingStatus processingStatus) throws Exception {
        Assert.isTrue(1 == repository.executeUpdate("update Operday d set d.processingStatus = ?1 where d.processingStatus = ?2"
                , processingStatus, processingStatus.getDependsOn())
                , () -> new DefaultApplicationException(String.format("Недопустимый целевой статус обработки проводок. Требуется '%s', ожидалось '%s'", processingStatus, processingStatus.getDependsOn())));
        init();
    }

    private void initOperday() {
        Map<String,String> map = ImmutableMap.<String,String>builder()
                .put("javax.persistence.cache.storeMode", "REFRESH").build();
        List<Operday> operdays = repository.selectHinted(Operday.class, "from Operday o", null, map);
        Assert.isTrue(!operdays.isEmpty() && 1 == operdays.size()
                , format("Неверно инициализирован опердень: '%s', '%s'", !operdays.isEmpty(), operdays.size()));
        Operday orig = operdays.get(0);
        operday = new Operday();
        operday.setCurrentDate(orig.getCurrentDate());
        operday.setLastWorkdayStatus(orig.getLastWorkdayStatus());
        operday.setLastWorkingDay(orig.getLastWorkingDay());
        operday.setPhase(orig.getPhase());
        operday.setPdMode(orig.getPdMode());
        operday.setProcessingStatus(orig.getProcessingStatus());
        operday.setAccessMode(orig.getAccessMode());
    }

    private void initTimeService() {
        timeService = findAssignable(DataBaseTimeService.class, timeServices);
    }

    @Lock(WRITE)
    public Boolean swithAccessMode(AccessMode accessMode) throws Exception {

        int cnt = repository.executeUpdate("update Operday o set o.accessMode = ?1",
                accessMode == AccessMode.FULL ? AccessMode.LIMIT : AccessMode.FULL);
        Assert.isTrue(1 == cnt);
        init();
        return (1 == cnt);
    }
}
