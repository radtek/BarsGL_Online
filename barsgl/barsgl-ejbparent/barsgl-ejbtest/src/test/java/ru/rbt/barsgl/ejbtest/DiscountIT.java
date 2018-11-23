package ru.rbt.barsgl.ejbtest;

import org.junit.Test;

import static java.lang.String.format;

/**
 * Created by Ivan Sevastyanov on 22.11.2018.
 */
public class DiscountIT extends AbstractRemoteIT {

    public static final String OUT_ACCOUNT_BASKET_TAB = "OUT_ACCOUNT_BASKET";

    public static final String OUT_LOG_TAB = "OUT_LOG";

    @Test
    public void test() {
        createOutAccountTab();
        createOutLogTab();
    }

    private void createOutAccountTab() {
        executeAutonomous(
                format("create table %s (\n" +
                "                BSAACID VARCHAR2(20)\n" +
                "                , ACCTYPE VARCHAR2(9)\n" +
                "                , CCY VARCHAR2(3)\n" +
                "                , DEAL_ID NUMBER(19)\n" +
                "                , PSAV CHAR(1)\n" +
                "                , FL_TURN CHAR(1)\n" +
                "                , EXCLUDE VARCHAR2(5)\n" +
                "            )", OUT_ACCOUNT_BASKET_TAB));
    }

    private void createOutLogTab() {
        executeAutonomous(format("CREATE TABLE %s (\n" +
                "    ID_PK NUMBER(19) NOT NULL\n" +
                "    , PROCESS_NM VARCHAR2(20) \n" +
                "    , OPERDAY DATE\n" +
                "    , STATUS CHAR(1)\n" +
                "    , START_DATE TIMESTAMP \n" +
                "    , END_DATE TIMESTAMP\n" +
                ")", OUT_LOG_TAB));
    }

    private void executeAutonomous(String sql) {
        baseEntityRepository.executeNativeUpdate(format(
                "declare\n" +
                "    pragma autonomous_transaction;\n" +
                "    l_already exception;\n" +
                "    pragma exception_init(l_already, -955);\n" +
                "    l_sql varchar2(4000) :=\n" +
                "    q'[ %s]';\n" +
                "begin\n" +
                "    execute immediate l_sql;\n" +
                "exception\n" +
                "    when l_already then null;\n" +
                "end;", sql));
    }
}
