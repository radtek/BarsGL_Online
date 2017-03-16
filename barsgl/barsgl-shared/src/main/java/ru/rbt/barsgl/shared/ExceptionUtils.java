package ru.rbt.barsgl.shared;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.DataTruncation;
import java.sql.SQLException;

/**
 * Created by Ivan Sevastyanov
 */

public class ExceptionUtils {

    public static boolean isExistsInException(Throwable th, Class<? extends Throwable> clazz) {
        return findException(th, clazz) != null;
    }

    public static <T extends Throwable> T findException(Throwable th, Class<T> clazz) {
        if (null == th) return null;
        if (th.getClass().equals(clazz)) return (T) th;
        if (null == th.getCause()) return null;
        Throwable th2 = th.getCause();
        while (null != th2) {
            if (th2.getClass().equals(clazz)) return (T) th2;
            th2 = th2.getCause();
        }
        return null;
    }

    public static String getErrorMessage(Throwable throwable, Class<? extends Throwable> ... classes) {
        if (null != classes) {
            for (Class clazz : classes) {
                Throwable res = findException(throwable, clazz);
                if (null != res) {
                    return (res.getMessage() != null ? res.getMessage() : res.toString());
                }
            }
        }
        return throwable.getMessage();
    }

    public static String getStacktrace(Throwable throwable) {
        if (null != throwable) {
            StringWriter err = new StringWriter();
            throwable.printStackTrace(new PrintWriter(err));
            return err.toString();
        } else {
            return "<no exception>";
        }
    }

}