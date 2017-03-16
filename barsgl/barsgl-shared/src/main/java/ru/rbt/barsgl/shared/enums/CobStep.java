package ru.rbt.barsgl.shared.enums;

/**
 * Created by ER18837 on 10.03.17.
 */
public enum CobStep {
    CobStopEtlProc(1),  // "Остановка обработки проводок"
    CobResetBuffer(2),  //"Автоматический сброс буфера"$
    CobManualProc(3),   //"Обработка необработанных запросов на операцию"
    CobStornoProc(4),   //"Обработка сторно текущего дня"
    CobFanProc(5),      //"Обработка вееров"
    CobRecalcBaltur(6)        // "Пересчет и локализация"
    ;

    private int phaseNo;
    private String phaseName;

    CobStep(int phaseNo) {
        this.phaseNo = phaseNo;
    }

    public int getPhaseNo() {
        return phaseNo;
    }

    public String getPhaseName() {
        return phaseName;
    }

    public void setPhaseName(String phaseName) {
        this.phaseName = phaseName;
    }

    public void setPhaseName(String phaseName, int phaseNo) {
        for (CobStep step : CobStep.values()) {
            if (step.phaseNo == phaseNo) {
                step.phaseName = phaseName;
                return;
            }
        }
    }
}
