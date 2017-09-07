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
    , STOP_PROC_TIMEOUT("process.stop.timeout.minute")
    , BARSGL_LOADER_TYPE("barsgl.loader.type")

    , BARSGL_LOADER_SSH_HOST("barsgl.loader.ssh.host")
    , BARSGL_LOADER_SSH_PORT("barsgl.loader.ssh.port")
    , BARSGL_LOADER_SSH_USER("barsgl.loader.ssh.user")
    , BARSGL_LOADER_SSH_PSWD("barsgl.loader.ssh.pswd")
    , BARSGL_LOADER_SSH_RUN_CMD("barsgl.loader.ssh.run.cmd")

    , BARSREP_LOADER_TYPE("barsrep.loader.type")
    , BARSREP_LOADER_SSH_HOST("barsrep.loader.ssh.host")
    , BARSREP_LOADER_SSH_PORT("barsrep.loader.ssh.port")
    , BARSREP_LOADER_SSH_USER("barsrep.loader.ssh.user")
    , BARSREP_LOADER_SSH_PSWD("barsrep.loader.ssh.pswd")
    , BARSREP_LOADER_SSH_RUN_CMD("barsrep.loader.ssh.run.cmd");

    private String name;

    PropertyName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
