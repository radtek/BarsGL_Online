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

    public void addErrorDescription(String message) {
        errorList.add(new ErrorDescriptor(message, null));
    }

    public void addErrorDescription(String message, String code) {
        errorList.add(new ErrorDescriptor(message, code));
    }

    public void addNewErrorDescription(String message, String code) {
        if (null == findErrorMessage(message)){
            errorList.add(new ErrorDescriptor(message, code));
        }
    }

    public String getErrorMessage() {
        StringBuilder builder = new StringBuilder();
        for (ErrorDescriptor desc : errorList) {
            builder.append(desc.getMessatge()).append("\n");
        }
        return builder.toString();
    }

    public ErrorDescriptor findErrorMessage(final String message) {
        for (ErrorDescriptor desc : errorList) {
            if ( desc.getMessatge().equals(message) )
                return desc;
        }
        return null;
    }

    public String getErrorCode() {
        return errorList.isEmpty() ? "" : errorList.get(0).getCode();
    }
    public String getErrorCode(int row) {
        return errorList.isEmpty() ? "" : errorList.get(row).getCode();
    }
    public int getErrorListLen(){return errorList.size();}
    public String getErrorMessage(int row){return errorList.get(row).getMessatge();}


    public boolean isEmpty() {
        return errorList.isEmpty();
    }
}

