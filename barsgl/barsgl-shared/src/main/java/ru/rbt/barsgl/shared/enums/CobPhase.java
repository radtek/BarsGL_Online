package ru.rbt.barsgl.shared.enums;

/**
 * Created by ER18837 on 10.03.17.
 */
public enum CobPhase {
    CobStopEtlProc(1),  // "Остановка обработки проводок"
    CobResetBuffer(2),  //"Автоматический сброс буфера"$
    CobManualProc(3),   //"Обработка необработанных запросов на операцию"
    CobStornoProc(4),   //"Обработка сторно текущего дня"
    CobCloseBalance(5), //"Закрытие баланса предыдущего дня"
    CobFanProc(6),      //"Обработка вееров"
    CobRecalcBaltur(7)  // "Пересчет и локализация"
    ;

    private int phaseNo;

    CobPhase(int phaseNo) {
        this.phaseNo = phaseNo;
    }

    public int getPhaseNo() {
        return phaseNo;
    }

    public static CobPhase getPhase(int phaseNo) {
        for (CobPhase phase : CobPhase.values()) {
            if (phase.phaseNo == phaseNo) {
                return phase;
            }
        }
        return null;
    }
}
