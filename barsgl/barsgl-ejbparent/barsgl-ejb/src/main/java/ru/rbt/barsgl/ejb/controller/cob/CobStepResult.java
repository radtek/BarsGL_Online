package ru.rbt.barsgl.ejb.controller.cob;

import ru.rbt.barsgl.shared.enums.CobStepStatus;

/**
 * Created by ER18837 on 15.03.17.
 */
public class CobStepResult {
    private CobStepStatus stepStatus;
    private String message;
    private String errorMessage;

    public CobStepResult(CobStepStatus stepStatus, String message) {
        this(stepStatus, message, "");
    }

    public CobStepResult(CobStepStatus stepStatus, String message, String errorMessage) {
        this.stepStatus = stepStatus;
        this.message = message;
        this.errorMessage = errorMessage;
    }

    public CobStepStatus getStepStatus() {
        return stepStatus;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
