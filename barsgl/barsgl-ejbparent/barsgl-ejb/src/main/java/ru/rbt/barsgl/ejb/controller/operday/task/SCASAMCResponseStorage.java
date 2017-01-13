package ru.rbt.barsgl.ejb.controller.operday.task;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ER22228 on 24.08.2016.
 */

@Singleton
@Startup
public class SCASAMCResponseStorage extends ConcurrentHashMap<String,String[]> {
}