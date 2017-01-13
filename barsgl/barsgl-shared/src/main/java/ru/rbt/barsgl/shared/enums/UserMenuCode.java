package ru.rbt.barsgl.shared.enums;

/**
 * Created by Ivan Sevastyanov on 21.04.2016.
 */
public enum UserMenuCode {
    SystemMenu, Accounting, Dictionary, Upload, Task, Operday, Access, User, Role
    , Backvalue, Audit, PLAccount, CBAccount, Operation, Posting, OperSemiposting
    , ErrorOper, UnloadAccountBalance, AEIncomeMsg, FileIncomeMsg, PostingSource
    , TermCode, AccountingType, PlanAccountingType, PlanAccountOfr, PropertyType, Branch
    , UnloadStamtConfig, TemplateOper, OperInpConfirm, OperInpHistory, ProfitCentr
    , DomesticPlan, AccTypeParts, LoaderControl, BufferSync, AcodMidas, PLAccountAcctype, PLAccountMidas
    , Monitoring, CheckCardsRemains

    /* Дополнительные коды меню, которые строятся всегда! */
    , Separator, SystemExit, Help, HelpAbout;

    public static UserMenuCode parse(String menuCode) {
        try {
            return valueOf(menuCode);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
