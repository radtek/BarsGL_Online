package ru.rbt.barsgl.gwt.core.utils;

import ru.rbt.barsgl.gwt.core.dialogs.ConfirmDlg;
import ru.rbt.barsgl.gwt.core.dialogs.IDlgEvents;
import ru.rbt.barsgl.gwt.core.dialogs.InfoDlg;

import java.util.Date;

/**
 * Created by Ivan Sevastyanov
 */
public class DialogUtils {
	protected static InfoDlg infoDlg = null;
    protected static ConfirmDlg confirmDlg = null;
	public final static long millsPerDay = 86400000;

    public static boolean isEmpty(String target) {
        return null == target || target.trim().isEmpty();
    }

    public static <T> String ifEmpty(T value, String defaultValue) {
    	return null == value ? defaultValue : value.toString(); }

    public static String trimStr(String target) {
        return (isEmpty(target) ? "" : target.trim());
    }

    public static Date addDays(Date dateIn, int days) {
    	long dateInMills = dateIn.getTime();
    	return new Date(dateInMills + days * millsPerDay);
    }
    
    public static <F> F check(F object, String column, String errorMessage, AppPredicate<F> predicate) {
        if (!predicate.check(object)) {
            showInfo("Ошибка", "Неверное значение в поле <b>'" + column + "'</b>: " + errorMessage);
            throw new IllegalArgumentException("column");
        }
        return object;
    }

    public static <F,T> T check(F from, String column, String errorMessage, AppPredicate<F> predicate, AppFunction<F,T> function) {
        from = check(from, column, errorMessage, predicate);
        return function.apply(from);
    }

    private static InfoDlg getInfoDlg() {
    	if (null == infoDlg) {
    		infoDlg = new InfoDlg();
    	}
    	return infoDlg;
    }
    
    public static void showInfo(String text) {
        showInfo("Сообщение", text);
    }

    public static void showInfo(String caption, String text) {
        InfoDlg msg = getInfoDlg();
        msg.setCaption(caption);
        msg.show(text);
    }

    private static ConfirmDlg getConfirmDlg() {
        if (null == confirmDlg) {
            confirmDlg = new ConfirmDlg();
        }
        return confirmDlg;
    }

    public static void showConfirm(String text, IDlgEvents dlgEvents, Object params) {
        showConfirm("Подтвердите", text, dlgEvents, params);
    }

    public static void showConfirm(String caption, String text, IDlgEvents dlgEvents, Object params) {
        ConfirmDlg msg = getConfirmDlg();
        msg.setCaption(caption);
        msg.setDlgEvents(dlgEvents);
        msg.setParams(params);
        msg.show(text);
    }

}
