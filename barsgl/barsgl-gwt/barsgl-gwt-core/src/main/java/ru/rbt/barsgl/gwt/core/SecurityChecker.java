package ru.rbt.barsgl.gwt.core;

import ru.rbt.shared.enums.SecurityActionCode;

import java.util.List;

/**
 * Created by akichigi on 26.04.16.
 */
public class SecurityChecker {
    private static List<SecurityActionCode> codeList;

    public static void init(List<SecurityActionCode> codes){
        codeList = codes;
    }

    public static boolean checkAction(SecurityActionCode actionCode){
       if (codeList == null) return false;
           if (codeList.contains(actionCode)) return true;
        return false;
    }

    public static boolean checkActions(SecurityActionCode ... actionCodes){
        if (codeList == null) return false;
        for (SecurityActionCode actionCode :actionCodes) {
            if (codeList.contains(actionCode)) return true;
        }
        return false;
    }
}
