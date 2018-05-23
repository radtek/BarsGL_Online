package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by er22317 on 22.05.2018.
 * режим обновления остатков BALTUR
 */
public enum BalanceMode implements HasLabel {

    /**
     * В зависимости от PST.PBR остатки обновляются асинхронно или онлайн
     */
    GIBRID("Асинхронный", "BEGIN GLAQ_PKG_UTL.START_GIBRID_MODE; END;")
    ,
    /**
     * остатки обновляются онлайн
     */
    ONLINE("Онлайн", "BEGIN GLAQ_PKG_UTL.START_ONLINE_MODE; END;")
    ,
    /**
     * все триггера на PST отключены, пересчет остатков осуществл. внешней программой
     */
    ONDEMAND("По требованию","BEGIN GLAQ_PKG_UTL.START_ONDEMAND_MODE; END;")
    ,
    /**
     * режим пересчета остатков не изменяеся
     */
    NOCHANGE(""
            ,"DECLARE\n" +
            "    L_MODE VARCHAR2(128);\n" +
            "BEGIN \n" +
            "    SELECT GLAQ_PKG_UTL.GET_CURRENT_BAL_STATE INTO L_MODE FROM DUAL;\n" +
            "    PKG_SYS_UTL.LOG_AUDIT_INFO('Operday', 'Текущий режим пересчета остатков \"'||L_MODE||'\" не изменен.'); \n" +
            "END;");

    private String procedureName;

    private String label;

    BalanceMode(String label, String procedureName) {
        this.label = label;
        this.procedureName = procedureName;
    }

    public String getSwithPlsqlBlock() {
        return procedureName;
    }


    @Override
    public String getLabel() {
        return label;
    }
}
