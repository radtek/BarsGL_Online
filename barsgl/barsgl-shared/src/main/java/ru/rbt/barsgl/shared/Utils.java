package ru.rbt.barsgl.shared;

/**
 * Created by akichigi on 17.03.15.
 */
public class Utils {
    // format: "some {0} text {1} here {2}"
    public static String Fmt(String format, final Object... args){
        String retVal = format;
        int i = 0;
        for (final Object current : args) {
            retVal = retVal.replaceAll("\\{" + i + "\\}", current.toString());
            i++;
        }
        return retVal;
    }

    public static String fillUp(String txt, int len){
        int n = len - txt.length();
        String s = "";
        for(int i = 0; i < n; i++)
            s += "0";
        return s + txt;
    }

    public static String value(String val){
        return  val == null ? val : val.trim();
    }

    public static String toStr(String val){ return val == null ? "" : val;}
}
