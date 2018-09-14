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
    , BATPKG_MAXSIZE("batpkg.max.size")
    , MC_QUEUES_PARAM("mc.queues.param")
    , MC_TIMEOUT("mc.timeout.sec")
    , COB_STAT_INC("cob.stat.increase")
    , STOP_PROC_TIMEOUT("process.stop.timeout.minute")
    , MQ_TIMEOUT("mq.process.timeout")
    , MQ_TIME_UNIT("mq.process.timeout.unit")

    , CUST_LOAD_ONLINE("customer.load.online")
    , ACC_WAIT_CLOSE("account.wait.close")
//    , REG47422_DEPTH("reg47422.def.depth")

    , BARSGL_LOADER_TYPE("barsgl.loader.type")
    , BARSGL_LOCALIZATION_TYPE("barsgl.localize.type")

    , BARSGL_LOADER_SSH_HOST("barsgl.loader.ssh.host")
    , BARSGL_LOADER_SSH_PORT("barsgl.loader.ssh.port")
    , BARSGL_LOADER_SSH_USER("barsgl.loader.ssh.user")
    , BARSGL_LOADER_SSH_PSWD("barsgl.loader.ssh.pswd")
    , BARSGL_LOADER_SSH_RUN_CMD("barsgl.loader.ssh.run.cmd")

    , BARSGL_LOCALIZATION_SSH_HOST("barsgl.localize.ssh.host")
    , BARSGL_LOCALIZATION_SSH_PORT("barsgl.localize.ssh.port")
    , BARSGL_LOCALIZATION_SSH_USER("barsgl.localize.ssh.user")
    , BARSGL_LOCALIZATION_SSH_PSWD("barsgl.localize.ssh.pswd")
    , BARSGL_LOCALIZATION_SSH_RUN_CMD("barsgl.localize.ssh.run.cmd")

    , BARSREP_LOADER_TYPE("barsrep.loader.type")
    , BARSREP_LOADER_SSH_HOST("barsrep.loader.ssh.host")
    , BARSREP_LOADER_SSH_PORT("barsrep.loader.ssh.port")
    , BARSREP_LOADER_SSH_USER("barsrep.loader.ssh.user")
    , BARSREP_LOADER_SSH_PSWD("barsrep.loader.ssh.pswd")
    , BARSREP_LOADER_SSH_RUN_CMD("barsrep.loader.ssh.run.cmd")

    , BARSREP_REPL_TYPE("barsrep.repl.type")
    , BARSREP_REPL_SSH_HOST("barsrep.repl.ssh.host")
    , BARSREP_REPL_SSH_PORT("barsrep.repl.ssh.port")
    , BARSREP_REPL_SSH_USER("barsrep.repl.ssh.user")
    , BARSREP_REPL_SSH_PSWD("barsrep.repl.ssh.pswd")
    , BARSREP_REPL_SSH_RUN_CMD("barsrep.repl.ssh.run.cmd")

    , AQBALANCE_CHECK_MSG_CNT("aqbalance.queue.check.msgcnt")
    , AQBALANCE_CHECK_EMSG_CNT("aqbalance.queue.check.emsgcnt");

    private String name;

    PropertyName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
