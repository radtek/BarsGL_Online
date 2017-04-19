package ru.rbt.barsgl.ejbcore.job;

import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.util.reflection.ObjectHolder;
import ru.rbt.barsgl.ejbcore.util.reflection.TypedValue;

import javax.ejb.ScheduleExpression;

import static java.lang.String.format;

/**
 * Created by Ivan Sevastyanov
 */
class ScheduleExpressionParser {
    public static ScheduleExpression parse(String scheduleExpression) {
        String[] arr = scheduleExpression.split(";");
        if (arr.length < 1) {
            throw new DefaultApplicationException(format("Неверный формат выражения: '%s'. Пример: 'second=*/30;minute=*;hour=*'", scheduleExpression));
        }

        ObjectHolder<ScheduleExpression> holder = new ObjectHolder<ScheduleExpression>(new ScheduleExpression());
        for (String setMtd : arr) {
            String[] d2 = setMtd.split("=");
            if (d2.length ==2) {
                try {
                    holder.invokeMethod(d2[0], new TypedValue[]{new TypedValue(isEmpty(d2[1]) ? "0" : d2[1], String.class)});
                } catch (Throwable e) {
                    e.printStackTrace();
                    throw new DefaultApplicationException(e.getMessage(), e);
                }
            }
        }
        return holder.getObject();
    }


    private static boolean isEmpty(String str) {
        return null == str || "".equals(str);
    }
}
