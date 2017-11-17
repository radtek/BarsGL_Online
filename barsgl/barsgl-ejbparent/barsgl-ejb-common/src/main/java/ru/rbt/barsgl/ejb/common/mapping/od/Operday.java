package ru.rbt.barsgl.ejb.common.mapping.od;

import ru.rbt.barsgl.shared.HasLabel;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;
import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov
 */
@Entity
@Table(name = "GL_OD")
public class Operday extends BaseEntity<Date> {

    /**
     * фазы операционного дня
     */
    public enum OperdayPhase implements HasLabel {
        ONLINE("Открыт"), PRE_COB("Закрывается фаза 1"), COB("Закрывается фаза 2");

        private final String name;

        OperdayPhase(String name) {
            this.name = name;
        }

        @Override
        public String getLabel() {
            return name;
        }
    }

    public enum LastWorkdayStatus implements HasLabel{

        OPEN("Баланс предыдущего рабочего дня ОТКРЫТ"), CLOSED("Баланс предыдущего рабочего дня ЗАКРЫТ");

        private final String name;

        LastWorkdayStatus(String name) {
            this.name = name;
        }

        @Override
        public String getLabel() {
            return name;
        }
    }

    /**
     * режим обработки проводок
     */
    public enum PdMode implements HasLabel {

        /**
         * PCID_MO, GL_POSTING не формируются, все пишем в GL_PD
         * такой процесс предполагает в конечном итоге синхронизацию с PD при
         * закрытии ОД или в течение ОД, но с остановом обработки проводок
         */
        BUFFER("Запись в буферную таблицу GL_PD")
        /**
         * прямая запись в PD c формированием PCID_MO, GL_POSTING
         * недопустима совместная работа с загрузчиком Майдас
         */
        , DIRECT("Запись напрямую в PD");

        private final String name;

        PdMode(String name) {
            this.name = name;
        }

        @Override
        public String getLabel() {
            return name;
        }

        public static PdMode switchMode(PdMode from) {
            return from == BUFFER ? DIRECT : BUFFER;
        }
    }

    public enum SystemAccessMode {
        FULL, LIMIT;
    }

    /**
     * текущий операционный день
     */
    @Id
    @Temporal(TemporalType.DATE)
    @Column(name = "CURDATE", nullable = false)
    private Date currentDate;

    /**
     * фаза операционного дня
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "PHASE", nullable = false)
    private OperdayPhase phase;

    /**
     * предыдущий рабочий операционный день
     */
    @Temporal(TemporalType.DATE)
    @Column(name = "LWDATE", nullable = false)
    private Date lastWorkingDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "LWD_STATUS")
    private LastWorkdayStatus lastWorkdayStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "PDMOD")
    private PdMode pdMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "PRC")
    private ProcessingStatus processingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACSMODE")
    private SystemAccessMode accessMode;

    @Override
    public Date getId() {
        return currentDate;
    }

    public Date getCurrentDate() {
        return currentDate;
    }

    /**
     * для работы с JDBC API
     * @return java.sql.Date
     */
    public java.sql.Date getCurrentSqlDate() {
        return new java.sql.Date(currentDate.getTime());
    }

    public void setCurrentDate(Date currentDate) {
        this.currentDate = currentDate;
    }

    public OperdayPhase getPhase() {
        return phase;
    }

    public void setPhase(OperdayPhase phase) {
        this.phase = phase;
    }

    public Date getLastWorkingDay() {
        return lastWorkingDay;
    }

    public void setLastWorkingDay(Date lastWorkingDay) {
        this.lastWorkingDay = lastWorkingDay;
    }

    public LastWorkdayStatus getLastWorkdayStatus() {
        return lastWorkdayStatus;
    }

    public void setLastWorkdayStatus(LastWorkdayStatus lastWorkdayStatus) {
        this.lastWorkdayStatus = lastWorkdayStatus;
    }

    public PdMode getPdMode() {
        return pdMode;
    }

    public void setPdMode(PdMode pdMode) {
        this.pdMode = pdMode;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public SystemAccessMode getAccessMode() {
        return accessMode;
    }

    public void setAccessMode(SystemAccessMode accessMode) {
        this.accessMode = accessMode;
    }
}
