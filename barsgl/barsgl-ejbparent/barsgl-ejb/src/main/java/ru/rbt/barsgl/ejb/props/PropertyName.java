package ru.rbt.barsgl.ejb.props;

/**
 * Created by ER21006 on 11.04.2016.
 */
public enum PropertyName {

    PD_CONCURENCY("pd.cuncurency")
    , ETLPKG_PROCESS_COUNT("etlpkg.process.count")
    , BATPKG_PROCESS_COUNT("batpkg.process.count")
    , MANUAL_PROCESS_COUNT("manual.process.count")
    , BATCH_PROCESS_ALLOWED("manual.process.allowed")
    , BATPKG_MAXROWS("batpkg.max.count")
    , MC_QUEUES_PARAM("mc.queues.param")
    , MC_TIMEOUT("mc.timeout.sec")
    , COB_STAT_INC("cob.stat.increase")
    , STOP_PROC_TIMEOUT("process.stop.timeout.minute");

    private String name;

    PropertyName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
