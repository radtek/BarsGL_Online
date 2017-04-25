package ru.rbt.barsgl.ejbcore.mapping.job;

import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.barsgl.shared.HasLabel;
import ru.rbt.barsgl.shared.enums.JobSchedulingType;
import ru.rbt.barsgl.shared.enums.JobStartupType;

import javax.persistence.*;

import static javax.persistence.InheritanceType.SINGLE_TABLE;

/**
 * Created by Ivan Sevastyanov
 */
@Entity
@Table(name = "GL_SCHED")
@Inheritance(strategy = SINGLE_TABLE)
@DiscriminatorColumn(name = "SCH_TYPE")
@SequenceGenerator(name = "TimerJobIdSeq", sequenceName = "GL_SCHED_SEQ", allocationSize = 1)
public abstract class TimerJob extends BaseEntity<Long> {

    public enum JobState implements HasLabel {
        /**
         * запущено
         */
        STARTED("Запущено"),
        /**
         * остановлено
         */
        STOPPED("Остановлено"),
        /**
         * ошибка создания
         */
        ERROR("Ошибка запуска");

        private final String label;

        private JobState(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    @Id
    @Column(name = "ID_TASK", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "TimerJobIdSeq")
    private Long id;

    @Column(name = "TSKNM", nullable = false)
    private String name;

    @Column(name = "DESCR")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATE", nullable = false)
    private JobState state;

    @Enumerated(EnumType.STRING)
    @Column(name = "STR_TYPE", nullable = false)
    private JobStartupType startupType;

    @Enumerated(EnumType.STRING)
    @Column(name = "SCH_TYPE", nullable = false, insertable=false, updatable=false)
    private JobSchedulingType schedulingType;

    @Column(name = "RUN_CLS", nullable = false)
    private String runnableClass;

    /**
     * Отсрочка старта задачи в миллисекундах
     */
    @Column(name = "DELAY", nullable = true)
    private Long delay;

    @Column(name = "PROPS")
    private String properties;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JobState getState() {
        return state;
    }

    public void setState(JobState state) {
        this.state = state;
    }

    public JobStartupType getStartupType() {
        return startupType;
    }

    public void setStartupType(JobStartupType startupType) {
        this.startupType = startupType;
    }

    public JobSchedulingType getSchedulingType() {
        return schedulingType;
    }

    public String getRunnableClass() {
        return runnableClass;
    }

    public void setRunnableClass(String runnableClass) {
        this.runnableClass = runnableClass;
    }

    public Long getDelay() {
        return delay;
    }

    public void setDelay(Long delay) {
        this.delay = delay;
    }

    public void setProperties(String properties) {
        this.properties = properties;
    }

    public String getProperties() {
        return properties;
    }

    public void setSchedulingType(JobSchedulingType schedulingType) {
        this.schedulingType = schedulingType;
    }

    public Long getInterval() {
        return null;
    }

}
