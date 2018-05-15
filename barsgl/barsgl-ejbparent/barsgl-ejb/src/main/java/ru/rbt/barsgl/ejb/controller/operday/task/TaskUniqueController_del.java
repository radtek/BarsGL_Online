package ru.rbt.barsgl.ejb.controller.operday.task;

import javax.ejb.AccessTimeout;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;

import static java.util.concurrent.TimeUnit.MINUTES;
import static ru.rbt.audit.entity.AuditRecord.LogCode.LoadBranchDict;

/**
 * Created by er22317 on 15.02.2018.
 */
@Singleton
@AccessTimeout(value = 5, unit = MINUTES)
public class TaskUniqueController_del {
    private volatile boolean[] tasks = new boolean[]{false};

    @Lock(LockType.WRITE)
    public boolean Start(TaskId taskId, boolean force) {
        if (!tasks[taskId.getVal()] || force) {
            return tasks[taskId.getVal()] = true;
        }else
            return false;
    }

    @Lock(LockType.WRITE)
    public void setFree(TaskId taskId) {
        if (tasks[taskId.getVal()]) {
            tasks[taskId.getVal()] = false;
        }
    }

    public enum TaskId {
        LoadBranchDictTask(0);

        private int val;
        int getVal(){return val;}
        TaskId(int val) {
            this.val = val;
        }
    }
}

