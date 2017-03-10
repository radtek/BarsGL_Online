package ru.rbt.barsgl.shared.cob;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.List;

/**
 * Created by ER18837 on 10.03.17.
 */
public class CobWrapper implements Serializable, IsSerializable {
    private Long idCob;
    private CobStepItem total;
    private List<CobStepItem> stepList;
    private String errorMessage;

    public Long getIdCob() {
        return idCob;
    }

    public void setIdCob(Long idCob) {
        this.idCob = idCob;
    }

    public CobStepItem getTotal() {
        return total;
    }

    public void setTotal(CobStepItem total) {
        this.total = total;
    }

    public List<CobStepItem> getStepList() {
        return stepList;
    }

    public void setStepList(List<CobStepItem> stepList) {
        this.stepList = stepList;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
