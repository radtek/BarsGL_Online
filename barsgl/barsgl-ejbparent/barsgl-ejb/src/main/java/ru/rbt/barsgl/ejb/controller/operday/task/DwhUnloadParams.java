package ru.rbt.barsgl.ejb.controller.operday.task;

/**
 * Created by Ivan Sevastyanov
 */
public enum DwhUnloadParams {

    UnloadBalanceRegistered("GLVD_BAL LOAD 1")
    , UnloadBalanceShared("GLVD_BAL LOAD 2")
    , UnloadBalanceAll("GLVD_BAL LOAD")
    , UnloadBalanceThree("GLVD_BAL LOAD 3")
    , UnloadFullPostings("GLVD_PST LOAD")

    , UnloadremoteChanges("GLVD_PST_DU")
    , UnloadremoteBal("GLVD_BAL4")

    , UnloadAccType("GLVD_ACTYPE LOAD")

    , UnloadOverValueAcc("PSTR_ACC_R LOAD")
    , UnloadOverValueChanges("GLVD_PSTR_UD")
    , UnloadOverValueStatements("GLVD_PSTR LOAD")
    , UnloadOverValueBal("GLVD_BAL_R")
    ;

    private final String paramDesc;

    DwhUnloadParams(String paramDesc) {
        this.paramDesc = paramDesc;
    }

    public String getParamName() {
        return "BARS_GL_DWH";
    }

    public String getParamDesc() {
        return paramDesc;
    }
}
