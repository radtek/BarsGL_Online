package ru.rbt.barsgl.ejb.controller.excel;

import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.shared.enums.BatchPackageState;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;

import java.sql.SQLException;
import java.util.Date;

/**
 * Created by ER18837 on 24.09.16.
 */
public class BatchProcessResult {

    public enum BatchProcessDate {BT_EMPTY(""), BT_NOW("в текущем опердне"), BT_PAST("в прошедшем дне");
        private String label;

        BatchProcessDate(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    };

    private long id;
    private int totalCount;
    private int completeCount;
    private int errorCount;
    private int movementCount;
    private int movementOkCount;
    private int movementErrorCount;
    private String errorMessage;

    private BatchPostStatus postStatus;
    private BatchPackageState packageState;
    private BatchProcessDate processDate;

    public BatchProcessResult(long id) {
        this(id, null, null);
    }

    public BatchProcessResult(long id, BatchPostStatus postStatus) {
        this(id, postStatus, BatchPackageState.getStateByPostingStatus(postStatus));
    }

    public BatchProcessResult(long id, BatchPostStatus postStatus, BatchPackageState pkgState) {
        this.postStatus = (null == postStatus) ? BatchPostStatus.NONE : postStatus;
        this.packageState = (null == pkgState) ? BatchPackageState.UNDEFINED : pkgState;
        this.id = id;
        this.totalCount = 0;
        this.errorCount = 0;
        this.completeCount = 0;
        this.errorMessage = "";
        this.processDate = BatchProcessDate.BT_EMPTY;
    }

    public BatchPackageState getPackageState() {
        return packageState;
    }

    public void setStatus(BatchPostStatus postStatus) {
        this.postStatus = postStatus;
        this.packageState = BatchPackageState.getStateByPostingStatus(postStatus);
    }

    public void setPackageState(BatchPackageState packageState) {
        this.packageState = packageState;
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

    public void setProcessDate(BatchProcessDate processDate) {
        this.processDate = processDate;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = "\n" + errorMessage;
    }

    public void setPackageStatistics(DataRecord dataStat, boolean withState) throws SQLException {
        if (withState) {
            try {
                packageState = BatchPackageState.valueOf(dataStat.getString("STATE"));
            } catch (Exception e) {
                packageState = BatchPackageState.UNDEFINED;
            }
            Date postDate = dataStat.getDate("POSTDATE");
            Date procDate = dataStat.getDate("PROCDATE");
            processDate = !BatchPackageState.IS_SIGNEDDATE.equals(packageState) ? BatchProcessDate.BT_EMPTY :
                    (postDate.equals(procDate) ? BatchProcessDate.BT_NOW : BatchProcessDate.BT_PAST);
        }
        totalCount = dataStat.getInteger("PST_ALL");
        completeCount = dataStat.getInteger("PST_OK");
        errorCount = dataStat.getInteger("PST_ERR");
        movementCount = dataStat.getInteger("SRV_ALL");
        movementOkCount = dataStat.getInteger("SRV_OK");
        movementErrorCount = dataStat.getInteger("SRV_ERR");
    }

    private String getPackageMessage() {
        return String.format("Пакет ID = %d " + packageState.getMessageFormat(), id, processDate.getLabel());
    }

    public String getPackageProcessMessage() {
        return String.format("%s \n%sВсего операций: %d \nЗапросов в АБС: %d \nОтветов от АБС: %d \nОшибок от АБС: %d " +
                "\nОбработано успешно: %d \nВсего ошибок: %d",
                getPackageMessage(), errorMessage, totalCount, movementCount, movementOkCount, movementErrorCount, completeCount, errorCount);
    }

    public String getPackageSignedMessage() {
        return String.format("%s \n%sВсего операций: %d \nЗапросов в АБС: %d ",
                getPackageMessage(), errorMessage, totalCount, movementCount);
    }

    public String getPackageSendMessage() {
        return String.format("%s с формированием движения АБС \n%sВсего операций: %d \nЗапросов движения в АБС: %d ",
                getPackageMessage(), errorMessage, totalCount, movementCount);
    }

    private String getPostMessage() {
        return String.format("Запрос на операцию ID = %d " + postStatus.getMessageFormat(), id, processDate.getLabel());
    }

    public String getPostSignedMessage() {
        return getPostMessage();
    }

    public String getPostSendMessage() {
        return getPostMessage() +  " с формированием движения АБС";
    }


}
