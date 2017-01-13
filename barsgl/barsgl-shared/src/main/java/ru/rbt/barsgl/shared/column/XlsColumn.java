package ru.rbt.barsgl.shared.column;

import java.io.Serializable;

/**
 * Created by ER18837 on 16.02.16.
 */
public class XlsColumn implements Serializable {
    private String caption;
    private String name;
    private XlsType type;
    private String format;

    public XlsColumn() {
    }

    public XlsColumn(String name, XlsType type, String caption, String format) {
        this.name = name;
        this.type = type;
        this.caption = caption;
        if(format != null && !format.isEmpty()) {
            this.format = format;
        } else {
            switch (type) {
                case DATE:
                    this.format = "dd.MM.yyyy";
                    break;
                case DATETIME:
                    this.format = "dd.MM.yyyy hh:mm:ss";
                    break;
                case DECIMAL:
                    this.format = "#,##0.00";
                    break;
                case LONG:
                case INTEGER:
                    this.format = "##########";
                    break;
                default:
                    this.format = "";
                    break;
            }
        }

    }

    public String getCaption() {
        return this.caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getName() {
        return this.name;
    }

    public XlsType getType() {
        return this.type;
    }

    public String getFormat() {
        return this.format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

}