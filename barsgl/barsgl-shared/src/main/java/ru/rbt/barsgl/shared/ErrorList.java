package ru.rbt.barsgl.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ER18837 on 22.08.15.
 */
public class ErrorList  implements Serializable, IsSerializable {
    private List<ErrorDescriptor> errorList;

    public ErrorList() {
        this.errorList = new ArrayList<ErrorDescriptor>();
    }

    public void clear() {
        errorList.clear();
    }

    public void addErrorDescription(String enityName, String fieldName, String message, String code) {
        errorList.add(new ErrorDescriptor(enityName, fieldName, message, code));
    }

    public void addNewErrorDescription(String enityName, String fieldName, String message, String code) {
        if (null == findErrorMessage(enityName, fieldName, message)){
            errorList.add(new ErrorDescriptor(enityName, fieldName, message, code));
        }
    }

    public String getErrorMessage() {
        StringBuilder builder = new StringBuilder();
        for (ErrorDescriptor desc : errorList) {
            builder.append(desc.getMessatge()).append("\n");
        }
        return builder.toString();
    }

    public ErrorDescriptor findErrorMessage(final String enityName, final String fieldName, final String message) {
        for (ErrorDescriptor desc : errorList) {
            if (// desc.getEnityName().equals(enityName) && desc.getFieldName().equals(fieldName) &&
                    desc.getMessatge().equals(message))
                return desc;
        }
        return null;
    }

    public String getErrorCode() {
        return errorList.isEmpty() ? "" : errorList.get(0).getCode();
    }

    public boolean isEmpty() {
        return errorList.isEmpty();
    }

}

