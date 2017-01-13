package ru.rbt.barsgl.ejb.controller.excel;

import java.util.List;

/**
 * Created by ER18837 on 24.09.16.
 */
public class BatchProcessResult {

    public enum BatchProcessDate {BT_EMPTY(""), BT_NOW("в текущем опердне"), BT_PAST("с прошедшей датой");
        private String label;

        BatchProcessDate(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    };

    public enum BatchProcessStatus {
          BT_INITIAL(": обработка %s не начата")
        , BT_SIGNED(" подписан %s")
        , BT_SIGNEDDATE(" подписан с подтверждением даты проводки %s")
        , BT_WAITDATE(" передан на подтверждение даты проводки %s")
        , BT_CONFIRM(" подтверждена дата проводки %s")
        , BT_COMPLETE(" обработан %s")
        , BT_INTERRUPT(": обработка %s прервана")
        , BT_ERROR(": ошибка при обработке пакета %s")
        ;
        private String label;

        BatchProcessStatus(String label) {
            this.label = label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private long idPackage;
    private int totalCount;
    private int completeCount;
    private int errorCount;
    private int movementCount;
    private int movementErrorCount;
    private String errorMessage;

    private BatchProcessStatus status;
    private BatchProcessDate processDate;

    public BatchProcessResult(long idPackage) {
        this(idPackage, 0, BatchProcessStatus.BT_INITIAL);
    }

    public BatchProcessResult(long idPackage, int totalCount) {
        this(idPackage, totalCount, BatchProcessStatus.BT_INITIAL);
    }

    public BatchProcessResult(long idPackage, int totalCount, BatchProcessStatus status) {
        this.status = status;
        this.idPackage = idPackage;
        this.totalCount = totalCount;
        this.errorCount = 0;
        this.completeCount = 0;
        this.errorMessage = "";
        this.processDate = BatchProcessDate.BT_EMPTY;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public void incCompleteCount() {
        completeCount++;
    }

    public void incErrorCount() {
        errorCount++;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getCompleteCount() {
        return completeCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public BatchProcessStatus getStatus() {
        return status;
    }

    public void setStatus(BatchProcessStatus status) {
        this.status = status;
    }

    public BatchProcessDate getProcessDate() {
        return processDate;
    }

    public void setProcessDate(BatchProcessDate processDate) {
        this.processDate = processDate;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = "\n" + errorMessage;
    }

    public long getIdPackage() {
        return idPackage;
    }

    public int getMovementCount() {
        return movementCount;
    }

    public int getMovementErrorCount() {
        return movementErrorCount;
    }

    public void setStatistics(List<Integer> res) {
        totalCount = res.get(0);
        completeCount = res.get(1);
        errorCount = res.get(2);
        movementCount = res.get(3);
        movementErrorCount = res.get(4);
    }

    public String getMessage() {
        return String.format("Пакет ID = %d " + status.getLabel(), idPackage, processDate.getLabel());
    }

    public String getProcessMessage() {
        return String.format("%s\n%sВсего операций: %d\nОбработано успешно: %d\nОбработано с ошибкой: %d\nЗапросов движения в АБС: %d\nОшибок от АБС: %d",
                getMessage(), errorMessage, totalCount, completeCount, errorCount, movementCount, movementErrorCount);
    }

    public String getSignedMessage() {
        return String.format("%s\n%sВсего операций: %d\nЗапросов движения в АБС: %d\nОшибок от АБС: %d",
                getMessage(), errorMessage, totalCount, movementCount, movementErrorCount);
    }

}
