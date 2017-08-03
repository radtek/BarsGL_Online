package ru.rbt.barsgl.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by ER18837 on 14.08.15.
 */
public class ErrorDescriptor implements Serializable, IsSerializable{
//    private String enityName;
//    private String fieldName;
    private String messatge;
    private String code;

    public ErrorDescriptor() {
    }

    public ErrorDescriptor(String messatge, String code) {
//        this.enityName = enityName;
//        this.fieldName = fieldName;
        this.messatge = messatge;
        this.code = code;
    }

//    public String getEnityName() { return enityName; }

//    public String getFieldName() { return fieldName; }

    public String getMessatge() {
        return messatge;
    }

    public String getCode() {
        return code;
    }

//    public enum ErrorCode implements HasLabel {
//        AccDeals("Не соответствие DealID/SubDealId"),
//        OK("");
//
//        private String label;
//
//        ErrorCode(String label) {
//            this.label = label;
//        }
//
//        @Override
//        public String getLabel() {
//            return label;
//        }
//    }

}
