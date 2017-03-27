package ru.rbt.barsgl.shared.enums;

/**
 * Created by ER18837 on 10.03.17.
 */
public enum CobStep {
    CobStopEtlProc(1),  // "Остановка обработки проводок"
    CobResetBuffer(2),  //"Автоматический сброс буфера"$
    CobManualProc(3),   //"Обработка необработанных запросов на операцию"
    CobStornoProc(4),   //"Обработка сторно текущего дня"
    CobCloseBalance(5), //"Закрытие баланса предыдущего дня"
    CobFanProc(6),      //"Обработка вееров"
    CobRecalcBaltur(7)  // "Пересчет и локализация"
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

    public static void setPhaseName(String phaseName, int phaseNo) {
        CobStep step = getStep(phaseNo);
        if (null != step)
            step.phaseName = phaseName;
    }

    public static CobStep getStep(int phaseNo) {
        for (CobStep step : CobStep.values()) {
            if (step.phaseNo == phaseNo) {
                return step;
            }
        }
        return null;
    }
}
