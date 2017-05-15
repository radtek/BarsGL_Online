package ru.rbt.ejbcore.util;

import java.util.Collection;
import java.util.Iterator;

/**
 * Created by ER21006 on 13.01.2015.
 */
public class StringUtils {

    public static final char[] quotes =  "“”«»".toCharArray();

    // TODO перенести в barsgl-shared
    public static boolean isEmpty(String target) {
        return null == target || target.trim().isEmpty();
    }

    public static String substr(String target, int length) {
        return isEmpty(target) ? "" : target.substring(0, Integer.min(length,  target.length()));
    }

    public static String substr(String target, int start, int end) {
        return (isEmpty(target) || start >= target.length() || end <= start) ? ""
                : target.substring(start, Integer.min(end,  target.length()));
    }


    public static String trimstr(String target) {
        return (isEmpty(target) ? "" : target.trim());
    }

    /**
     * <code>
     * System.out.println(StringUtils.rsubstr("12345", 3)); // 345
     * </code>
     * @param target input
     * @param length size
     * @return result string
     */
    public static String rsubstr(String target, int length) {
        return isEmpty(target) ? "" : target.substring(target.length() - Integer.min(length,  target.length()), target.length());
    }

    public static String substr(String target, String start, String stop) {
        if (isEmpty(target)) return "";
        String res = target;
        int pos;
        if (!isEmpty(start)) {
            pos = res.indexOf(start);
            if (pos >= 0) {
                res = res.substring(pos + start.length());
            }
        }
        if (!isEmpty(stop)) {
            pos = res.indexOf(stop);
            if (pos > 0) {
                res = res.substring(0, pos);
            }
        }
        return res;
    }

    /**
     * убираем лидирующие и концевые пробелы
     * @param target входящая строка
     * @return результат без пробелов
     */
    public static String trim(String target) {
        return isEmpty(target) ? target : target.trim();
    }

    /**
     * Возвращает значение по умолчанию, если строка null или ""
     * @param target
     * @param defaultString
     * @return
     */
    public static String ifEmpty(String target, String defaultString) {
        return isEmpty(target) ? defaultString : target;
    }

    public static String requiredNotEmpty(String target, String claim) {
        if (!isEmpty(target)) {
            return target.trim();
        } else {
            throw new IllegalArgumentException(isEmpty(claim) ? "Target string is empty" : claim);
        }
    }

    public static String removeNewLine(String value) {
        if (null == value)
            return null;

        return value.replaceAll("[\n\r]", " ");
    }

    public static String removeCtrlChars(String value) {
        if (null == value)
            return null;

        int len = value.length();
        char buf[] = new char[len];
        value.getChars(0, len, buf, 0);

        int i = -1;
        while (++i < len) {
            if (buf[i]  < ' ' ) {
                break;
            }
        }
        if (i < len) {
            for (; i < len; i++) {
                if (buf[i] < ' ')
                    buf[i] = ' ';
            }
            return new String(buf, 0, len);
        }
        return value;
    }

    public static <E>  String listToString(Collection<E> list, String delimiter) {
        Iterator<E> it = list.iterator();
        if (! it.hasNext())
            return "";

        StringBuilder sb = new StringBuilder();
        for (;;) {
            E e = it.next();
            sb.append(e);
            if (! it.hasNext())
                return sb.toString();
            sb.append(delimiter);
        }
    }

    public static <E>  String listToString(Collection<E> list, String delimiter, String quote) {
        Iterator<E> it = list.iterator();
        if (! it.hasNext())
            return "";

        StringBuilder sb = new StringBuilder();
        for (;;) {
            E e = it.next();
            sb.append(quote).append(e).append(quote);
            if (! it.hasNext())
                return sb.toString();
            sb.append(delimiter);
        }
    }
    
    public static <E>  String arrayToString(E ar[], String delimiter, String quote) {
        if ((null == ar) || (0 == ar.length))
            return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ar.length; i++) {
            E e = ar[i];
            sb.append(quote).append(e).append(quote);
            if (i == ar.length - 1)
                return sb.toString();
            sb.append(delimiter);
        }
        return sb.toString();
    }

    public static String leftPad(String string, int size, String padString) {
        return org.apache.commons.lang3.StringUtils.leftPad(string, size, padString);
    }

    public static String rightPad(String string, int size, String padString) {
        return org.apache.commons.lang3.StringUtils.rightPad(string, size, padString);
    }

    public static String leftSpace(String string, int size) {
        return leftPad(string, size, " ");
    }
}
