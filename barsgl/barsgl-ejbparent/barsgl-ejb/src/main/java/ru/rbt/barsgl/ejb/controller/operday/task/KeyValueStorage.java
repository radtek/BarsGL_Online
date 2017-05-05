package ru.rbt.barsgl.ejb.controller.operday.task;

import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ER22228 on 24.08.2016.
 */

@Singleton
@Startup
public class KeyValueStorage {

    private static final Logger log = Logger.getLogger(ru.rbt.barsgl.ejb.controller.operday.task.KeyValueStorage.class);

    public enum TASKS{
        AccountListTask;
    }
    private EnumMap<TASKS, Boolean> tasksMap = new EnumMap<TASKS, Boolean>(TASKS.class);

    // Можно расширять по мере надобности
    private Map<String,String> stringMap = new HashMap<>();
    private Map<String,Boolean> booleanMap = new HashMap<>();
    private Map<String,Integer> integerMap = new HashMap<>();

    @PostConstruct
    public void init() {
        log.info("Запуск KeyValueStorage");
    }

    public boolean getTaskContinue(TASKS task){
//        log.info("\nTASKS. Get("+task+"):"+tasksMap.get(task));
        return tasksMap.get(task)!=null && tasksMap.get(task);
    }

    public void setTaskContinue(TASKS task, Boolean value){
        log.info("\nTASKS. Put("+task+","+value+")");
        tasksMap.put(task,value);
    }

    public String getString(String key){
        return stringMap.get(key);
    }

    public void setString(String key, String value){
        stringMap.put(key,value);
    }


}