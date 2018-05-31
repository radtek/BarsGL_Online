package ru.rbt.barsgl.shared.enums;

/**
 * Created by ER18837 on 10.03.17.
 */
public enum CobPhase {
    CobStopEtlProc(1),      // "Остановка обработки проводок"
    CobStornoProc(2),       //"Обработка сторно текущего дня"
    CobCloseBalance(3),     //"Закрытие баланса предыдущего дня"
    CobResetBuffer(4),      //"Автоматический сброс буфера"$
    CobManualProc(5),       //"Обработка необработанных запросов на операцию"
    CobFanProc(6),          //"Обработка вееров"
    CobRecalcBaltur(7),     // "Пересчет и локализация"
    CobCloseAccounts(8),    // "Закрытие счетов"
    CobStartEtlProc(9)      // "Пересчет и локализация"
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
