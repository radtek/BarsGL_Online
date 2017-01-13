package ru.rbt.barsgl.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by akichigi on 04.07.16.
 */
public class DatePartsContainer implements Serializable, IsSerializable {
    private int[] parts;
    private boolean isNull;
    public DatePartsContainer() {
        parts = new int[7];
    }

    public void setDate(Date d) {
        isNull = d == null;
        if (!isNull) {
            parts[0] = d.getYear();
            parts[1] = d.getMonth();
            parts[2] = d.getDate();
            parts[3] = d.getHours();
            parts[4] = d.getMinutes();
            parts[5] = d.getSeconds();
            parts[6] = (int) (d.getTime() % 1000);
        }
    }
    public Date getDate() {
        if (isNull) return null;

        Date d = new Date(0);
        d.setYear(parts[0]);
        d.setMonth(parts[1]);
        d.setDate(parts[2]);
        d.setHours(parts[3]);
        d.setMinutes(parts[4]);
        d.setSeconds(parts[5]);
        d.setTime(d.getTime() + parts[6]);
        return d;
    }
}
