package ru.rbt.barsgl.shared.operday;

import ru.rbt.barsgl.shared.enums.OperDayButtons;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by akichigi on 23.03.15.
 */
public class OperDayWrapper implements Serializable {
    private String currentOD;
    private String phaseCurrentOD;
    private String previousOD;
    private String previousODBalanceStatus;
    private String pdMode;
    private String processingStatus;
    private Date currentODDate;
    private Date previosODDate;

    private OperDayButtons enabledButton;
    private COB_OKWrapper cobOkWrapper;
    private Boolean isCOBRunning;

    public OperDayButtons getEnabledButton() {
        return enabledButton;
    }

    public void setEnabledButton(OperDayButtons enabledButton) {
        this.enabledButton = enabledButton;
    }

    public String getCurrentOD() {
        return currentOD;
    }

    public void setCurrentOD(String currentOD) {
        this.currentOD = currentOD;
    }

    public String getPhaseCurrentOD() {
        return phaseCurrentOD;
    }

    public void setPhaseCurrentOD(String phaseCurrentOD) {
        this.phaseCurrentOD = phaseCurrentOD;
    }

    public String getPreviousOD() {
        return previousOD;
    }

    public void setPreviousOD(String previousOD) {
        this.previousOD = previousOD;
    }

    public String getPreviousODBalanceStatus() {
        return previousODBalanceStatus;
    }

    public void setPreviousODBalanceStatus(String previousODBalanceStatus) {
        this.previousODBalanceStatus = previousODBalanceStatus;
    }

    public String getPdMode() {
        return pdMode;
    }

    public void setPdMode(String pdMode) {
        this.pdMode = pdMode;
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(String processingStatus) {
        this.processingStatus = processingStatus;
    }

    public Date getCurrentODDate() {
        return currentODDate;
    }

    public void setCurrentODDate(Date currentODDate) {
        this.currentODDate = currentODDate;
    }

    public Date getPreviosODDate() {
        return previosODDate;
    }

    public void setPreviosODDate(Date previosODDate) {
        this.previosODDate = previosODDate;
    }

    public COB_OKWrapper getCobOkWrapper() {
        return cobOkWrapper;
    }

    public void setCobOkWrapper(COB_OKWrapper cobOkWrapper) {
        this.cobOkWrapper = cobOkWrapper;
    }

    public Boolean getIsCOBRunning() {
        return isCOBRunning;
    }

    public void setIsCOBRunning(Boolean isCOBRunning) {
        this.isCOBRunning = isCOBRunning;
    }
}
