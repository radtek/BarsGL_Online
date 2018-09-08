package ru.rbt.barsgl.ejb.controller.operday.task.ovp;

/**
 * Created by Ivan Sevastyanov on 10.08.2018.
 */
public enum OvpUnloadParam {

    POSTING("BARS_GL_OCP_POST", "GL_OCPPOST LOAD")
    , REST("BARS_GL_OCP_REST", "GL_OCPREST LOAD")
    , FINAL_POSTING("BARS_GL_OCP_POST", "GL_OCPPOST FINAL_LOAD")
    , FINAL_REST("BARS_GL_OCP_REST", "GL_OCPREST FINAL_LOAD");

    private String parName;
    private String parDesc;

    OvpUnloadParam(String parName, String parDesc) {
        this.parName = parName;
        this.parDesc = parDesc;
    }

    public String getParName() {
        return parName;
    }

    public String getParDesc() {
        return parDesc;
    }
}

