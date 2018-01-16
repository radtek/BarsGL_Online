package ru.rbt.barsgl.ejb.controller.operday.task.stamt;

/**
 * Created by Ivan Sevastyanov on 28.01.2016.
 */
public enum UnloadStamtParams {

    FULL_POSTING("BARS_GL_STAMT_FULL","GL_ETLSTM LOAD")
    ,DELTA_POSTING("BARS_GL_STAMT_DELTA","GL_ETLSTMD LOAD")
    ,BALANCE_FULL("BARS_GL_STAMT_BALANCE_FULL","GL_BALSTM LOAD")
    ,BALANCE_DELTA("BARS_GL_STAMT_BALANCE_DELTA","GL_BALSTMD LOAD")
    ,BALANCE_DELTA2("BARS_GL_STAMT_BALANCE_DELTA","GL_BALSTMD LOAD 2")
    ,BALANCE_DELTA_INCR("BARS_GL_STAMT_BALANCE_DELTA","GL_BALSTMD INCR")
    ,DELTA_POSTING_INCR("BARS_GL_STAMT_DELTA","GL_ETLSTMD INCR")
    ,BALANCE_DELTA_FLEX("BARS_GL_STAMT_BALANCE_DELTA","GL_BALSTMD FLEX")
    ,BALANCE_TECHOVER("BARS_GL_STAMT_BALANCE_DELTA","GL_BALSTMD TECHOV")
    ,POSTING_TECHOVER("BARS_GL_STAMT_DELTA","GL_ETLSTMD TECHOV")
    ,POSTING_DELETE("BARS_GL_STAMT_DELETE","GL_STMDEL LOAD")
    ,NEW_ACCOUNTS("BARSGL_STAMT_ACC_NEW","GL_ACCSTM LOAD");

    private final String paramName;
    private final String paramDesc;

    UnloadStamtParams(String paramName, String paramDesc) {
        this.paramName = paramName;
        this.paramDesc = paramDesc;
    }

    public String getParamName() {
        return paramName;
    }

    public String getParamDesc() {
        return paramDesc;
    }
}
