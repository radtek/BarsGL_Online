package ru.rbt.barsgl.ejbcore.job;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by Ivan Sevastyanov
 */
public interface InmemoryTask extends Serializable {

    abstract void run(Map<String,Object> properties) throws Exception;

}
