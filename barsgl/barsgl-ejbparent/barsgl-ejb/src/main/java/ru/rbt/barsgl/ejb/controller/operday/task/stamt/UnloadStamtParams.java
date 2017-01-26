package ru.rbt.barsgl.ejb.controller.operday.task.stamt;

/**
 * Created by Ivan Sevastyanov on 28.01.2016.
 */
public enum UnloadStamtParams {

    FULL_POSTING("BARS_GL_STAMT_FULL","GL_ETLSTM LOAD")
    ,DELTA_POSTING("BARS_GL_STAMT_DELTA","GL_ETLSTMD LOAD")
    ,BALANCE_FULL("BARS_GL_STAMT_BALANCE_FULL","GL_BALSTM LOAD")
    ,BALANCE_DELTA("BARS_GL_STAMT_BALANCE_DELTA","GL_BALSTMD LOAD")
    ,BALANCE_DELTA_INCR("BARS_GL_STAMT_BALANCE_DELTA","GL_BALSTMD INCR")
    ,DELTA_POSTING_INCR("BARS_GL_STAMT_DELTA","GL_ETLSTMD INCR")
    ,BALANCE_DELTA_FLEX("BARS_GL_STAMT_BALANCE_DELTA","GL_BALSTMD FLEX")
    ,BALANCE_TECHOVER("BARS_GL_STAMT_OVER","GL_BALSTMD INC")
    ,POSTING_TECHOVER("BARS_GL_STAMT_OVER","GL_ETLSTMD INC");

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
