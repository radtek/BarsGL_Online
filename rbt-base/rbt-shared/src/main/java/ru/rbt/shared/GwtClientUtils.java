package ru.rbt.shared;

/**
 * Created by er21006 on 13.07.2017.
 */
public class GwtClientUtils {

    /**
     * без использования IO (for GWT happy)
     * @param throwable
     * @return
     */
    public static String getStacktrace2(Throwable throwable) {
        if (throwable.getStackTrace().length > 0) {
            StringBuilder b = new StringBuilder(throwable.getClass().getName()).append(": ").append("'").append(throwable.getMessage()).append("'").append("\n");
            for (StackTraceElement el : throwable.getStackTrace()) {
                b.append("at ").append(el.getClassName()).append(".").append(el.getMethodName()).append("(").append(el.getClassName()).append(".java:").append(el.getLineNumber()).append(")\n");
            }
            return b.toString();
        } else {
            return "";
        }
    }

}
