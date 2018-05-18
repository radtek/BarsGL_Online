package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.LoadBranchDictTask;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Created by er22317 on 09.02.2018.
 */
public class LoadBranchDictIT extends AbstractRemoteIT {
    private static final Logger log = Logger.getLogger(FanNdsPostingIT.class.getName());
    Map<String, String> tableScript = new HashMap<String, String>();

    @Test
    public void test() throws Exception {
        String PARAM = "2018-01-12";
        String FIX = "065";
        String VITRINA = "13.01.18";

        String fix = saveFix();
        try {
            initTables(VITRINA);
            setFix(FIX);

            Properties props = new Properties();
            props.put(LoadBranchDictTask.PROP_OPERDAY, PARAM);
            jobService.executeJob(SingleActionJobBuilder.create().withClass(LoadBranchDictTask.class).withProps(props).build());

            DataRecord rec = baseEntityRepository.selectFirst( "select count(*) cnt from IMBCBCMP where CCPCD = 'TTT'", new Object[]{});
            Assert.assertFalse("В IMBCBCMP не добавлена CCPCD = 'TTT'", rec.getLong("cnt") == 0l);
            rec = baseEntityRepository.selectFirst( "select count(*) cnt from IMBCBCMP where CCPCD = 'UFA' and CCBBR = '0093'", new Object[]{});
            Assert.assertFalse("В IMBCBCMP не обновлена CCPCD = 'UFA' and CCBBR = '0093'", rec.getLong("cnt") == 0l);
            rec = baseEntityRepository.selectFirst( "select count(*) cnt from IMBCBCMP where CCPCD = 'VLG' and CCBBR = '0066'", new Object[]{});
            Assert.assertFalse("В IMBCBCMP не обновлена CCPCD = 'VLG' and CCBBR = '0066'", rec.getLong("cnt") == 0l);

            rec = baseEntityRepository.selectFirst( "select count(*) cnt from DH_BR_MAP where FCC_BRANCH = 'TTT' and MIDAS_BRANCH = '118'", new Object[]{});
            Assert.assertFalse("В DH_BR_MAP не добавлена FCC_BRANCH = 'TTT'", rec.getLong("cnt") == 0l);
            rec = baseEntityRepository.selectFirst( "select count(*) cnt from DH_BR_MAP where FCC_BRANCH = 'UFA' and CBR_BRANCH = '0093' and MIDAS_BRANCH = '093'", new Object[]{});
            Assert.assertFalse("В DH_BR_MAP не обновлена CCPCD = 'UFA' and CBR_BRANCH = '0093'", rec.getLong("cnt") == 0l);
            rec = baseEntityRepository.selectFirst( "select count(*) cnt from DH_BR_MAP where FCC_BRANCH = 'VLG' and CBR_BRANCH = '0066' and MIDAS_BRANCH = '066'", new Object[]{});
            Assert.assertFalse("В DH_BR_MAP не обновлена FCC_BRANCH = 'VLG' and CBR_BRANCH = '0066'", rec.getLong("cnt") == 0l);

            rec = baseEntityRepository.selectFirst( "select count(*) cnt from IMBCBBRP where A8BRCD = '119'", new Object[]{});
            Assert.assertFalse("В IMBCBBRP не добавлена A8BRCD = '119'", rec.getLong("cnt") == 0l);
            rec = baseEntityRepository.selectFirst( "select count(*) cnt from IMBCBBRP where A8BRCD = '118'", new Object[]{});
            Assert.assertFalse("В IMBCBBRP не добавлена A8BRCD = '118'", rec.getLong("cnt") == 0l);
            rec = baseEntityRepository.selectFirst( "select count(*) cnt from IMBCBBRP where A8BRCD = '115' and BCBBR = '0095' and A8LCCD = 'TTT'", new Object[]{});
            Assert.assertFalse("В IMBCBBRP не обновлена A8BRCD = '115' and BCBBR = '0095' and A8LCCD = 'TTT'", rec.getLong("cnt") == 0l);

            rec = baseEntityRepository.selectFirst( "select count(*) cnt from DH_BR_MAP where FCC_BRANCH = 'H02'", new Object[]{});
            Assert.assertFalse("В DH_BR_MAP не добавлена FCC_BRANCH = 'H02'", rec.getLong("cnt") == 0l);
            rec = baseEntityRepository.selectFirst( "select count(*) cnt from DH_BR_MAP where FCC_BRANCH = 'H01'", new Object[]{});
            Assert.assertFalse("В DH_BR_MAP не добавлена FCC_BRANCH = 'H01'", rec.getLong("cnt") == 0l);
            rec = baseEntityRepository.selectFirst( "select count(*) cnt from DH_BR_MAP where FCC_BRANCH = 'N03' and CBR_BRANCH = '0095' and MIDAS_BRANCH = '115'", new Object[]{});
            Assert.assertFalse("В DH_BR_MAP не обновлена FCC_BRANCH = 'N03' and CBR_BRANCH = '0095'", rec.getLong("cnt") == 0l);

        }finally{
            restoreTables();
            setFix(fix);
        }
    }

    private void initTables(String VITRINA) throws SQLException, IOException {
//        String sqlTableExist = "select count(*) cnt from user_tables where table_name = ?";
        Long cnt = 0l;
        Stream.of("V_GL_DWH_IMBCBBRP", "V_GL_DWH_IMBCBCMP", "IMBCBBRP_BKP", "IMBCBCMP_BKP", "DH_BR_MAP_BKP", "V_GL_DWH_LOAD_STATUS")
                .forEach(item -> {
                    try {
                        DataRecord rec = baseEntityRepository.selectFirst( "select count(*) cnt from user_tables where table_name = ?", item);
//        Assert.assertFalse("Необходимо создать таблицу "+table, rec.getLong("cnt") == 0l);
                        if (rec.getLong("cnt") == 0l) createTable(item);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });

        if (baseEntityRepository.selectFirst( "select count(*) cnt from V_GL_DWH_LOAD_STATUS", new Object[]{}).getInteger(0) == 0){
            baseEntityRepository.executeNativeUpdate("insert into V_GL_DWH_LOAD_STATUS values(to_date('"+VITRINA+"','DD.MM.RR'))", new Object[]{});
        }

        moveIMBCBBRP("IMBCBBRP", "IMBCBBRP_BKP");
        moveIMBCBCMP("IMBCBCMP", "IMBCBCMP_BKP");
        moveDH_BR_MAP("DH_BR_MAP", "DH_BR_MAP_BKP");

        testToV_GL_DWH_IMBCBCMP();
        testToV_GL_DWH_IMBCBBRP();

    }

    private void createTable(String tableName){
        if (tableScript.isEmpty()) fillScriptMap();
        baseEntityRepository.executeNativeUpdate(tableScript.get(tableName));
    }

    private void testToV_GL_DWH_IMBCBBRP() throws IOException {
        clearTable("V_GL_DWH_IMBCBBRP");
        insTable(textResourceController.getContent("ru/rbt/barsgl/ejb/controller/od/INS_V_GL_DWH_IMBCBBRP.sql"));
    }

    private void testToV_GL_DWH_IMBCBCMP() throws IOException {
        clearTable("V_GL_DWH_IMBCBCMP");
        insTable(textResourceController.getContent("ru/rbt/barsgl/ejb/controller/od/INS_V_GL_DWH_IMBCBCMP.sql"));
    }


    private void moveIMBCBBRP(String from, String to) throws SQLException {
        List<DataRecord> table = baseEntityRepository.selectMaxRows("select A8BRCD, A8CMCD, A8LCCD,A8DFAC,A8DTAC,A8BICN,A8LCD,A8TYLC,A8BRNM,A8BRSN,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER from "+from, 1000, null);
        List<Object[]> params = new ArrayList<Object[]>();

        table.forEach(item ->params.add(new Object[]{item.getString("A8BRCD"), item.getString("A8CMCD"), item.getString("A8LCCD"), item.getString("A8DFAC"),
                     item.getString("A8DTAC"), item.getString("A8BICN"), item.getString("A8LCD"), item.getString("A8TYLC"), item.getString("A8BRNM"),
                     item.getString("A8BRSN"), item.getString("BBRRI"), item.getString("BCORI"), item.getString("BCBBR"), item.getString("BR_HEAD"),
                     item.getString("BR_OPER")}) );
        clearTable(to);
        params.forEach(item ->baseEntityRepository.executeNativeUpdate("insert into "+to+"(A8BRCD, A8CMCD, A8LCCD,A8DFAC,A8DTAC,A8BICN,A8LCD,A8TYLC,A8BRNM,A8BRSN,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", item));
    }

    private void moveIMBCBCMP(String from, String to) throws SQLException {
        List<DataRecord> table = baseEntityRepository.selectMaxRows("select CCPCD,CCPNE,CCPNR,CCPRI,CCBBR from "+from, 1000, null);
        List<Object[]> params = new ArrayList<Object[]>();
        table.forEach(item ->params.add(new Object[]{item.getString("CCPCD"), item.getString("CCPNE"), item.getString("CCPNR"), item.getString("CCPRI"), item.getString("CCBBR") }) );
        clearTable(to);
        params.forEach(item ->baseEntityRepository.executeNativeUpdate("insert into "+to+"(CCPCD,CCPNE,CCPNR,CCPRI,CCBBR) values(?,?,?,?,?)", item));
    }
    private void moveDH_BR_MAP(String from, String to) throws SQLException {
        List<DataRecord> table = baseEntityRepository.selectMaxRows("select FCC_BRANCH, MIDAS_BRANCH, CBR_BRANCH from "+from, 1000, null);
        List<Object[]> params = new ArrayList<Object[]>();
        table.forEach(item ->params.add(new Object[]{item.getString("FCC_BRANCH"), item.getString("MIDAS_BRANCH"), item.getString("CBR_BRANCH") }) );
        clearTable(to);
        params.forEach(item ->baseEntityRepository.executeNativeUpdate("insert into "+to+"(FCC_BRANCH, MIDAS_BRANCH, CBR_BRANCH) values(?,?,?)", item));
    }

    private void restoreTables() throws SQLException {
        moveIMBCBBRP("IMBCBBRP_BKP", "IMBCBBRP");
        moveIMBCBCMP("IMBCBCMP_BKP", "IMBCBCMP");
        moveDH_BR_MAP("DH_BR_MAP_BKP", "DH_BR_MAP");
    }


        private void clearTable(String table){
        baseEntityRepository.executeNativeUpdate("delete from " + table, new Object[]{});
    }

//    private void insTable(List<String> list){
//        list.forEach(item -> baseEntityRepository.executeNativeUpdate(item, new Object[]{}));
//    }
    private void insTable(String sql){
        baseEntityRepository.executeNativeUpdate(sql, new Object[]{});
    }

    private void setFix(String fix) throws SQLException {
        baseEntityRepository.executeNativeUpdate("update gl_prprp set NUMBER_VALUE=? where id_prp = 'dwh.fix.branch.code'", fix);
    }
    private String saveFix() throws SQLException {
        return(baseEntityRepository.selectFirst("select NUMBER_VALUE from gl_prprp where id_prp = 'dwh.fix.branch.code'",new Object[]{}).getString(0));
    }

    private void fillScriptMap(){
        tableScript.put("V_GL_DWH_IMBCBCMP", "DECLARE PRAGMA AUTONOMOUS_TRANSACTION; "+
                                             " BEGIN " +
                                             "EXECUTE IMMEDIATE 'CREATE TABLE V_GL_DWH_IMBCBCMP " +
                "                            (CCPCD CHAR(3) NOT NULL," +
                "                            CCPNE VARCHAR2(30) NOT NULL," +
                "                            CCPNR VARCHAR2(80) NOT NULL," +
                "                            CCPRI CHAR(1) NOT NULL," +
                "                            CCBBR CHAR(4) NOT NULL," +
                "                            ALT_CODE CHAR(3) NOT NULL," +
                "                            VALID_FROM DATE NOT NULL" +
                "                            )';  END;");
        tableScript.put("V_GL_DWH_IMBCBBRP","DECLARE PRAGMA AUTONOMOUS_TRANSACTION; " +
                                            " BEGIN " +
                                            "EXECUTE IMMEDIATE 'CREATE TABLE V_GL_DWH_IMBCBBRP"+
                                            "(	A8BRCD CHAR(3) NOT NULL,"+
                                            "A8LCCD CHAR(3) NOT NULL,"+
                                            "A8BICN CHAR(8) NOT NULL,"+
                                            "A8BRNM VARCHAR2(30) NOT NULL,"+
                                            "BBRRI CHAR(1) NOT NULL,"+
                                            "BCORI CHAR(1) NOT NULL,"+
                                            "BCBBR CHAR(4) NOT NULL,"+
                                            "BR_HEAD CHAR(1) DEFAULT ''N'' NOT NULL,"+
                                            "BR_OPER CHAR(1) DEFAULT ''N'' NOT NULL,"+
                                            "FCC_CODE CHAR(3) NOT NULL,"+
                                            "VALID_FROM DATE DEFAULT SYSDATE NOT NULL)'; END;");
        tableScript.put("IMBCBBRP_BKP", "DECLARE PRAGMA AUTONOMOUS_TRANSACTION; " +
                                        " BEGIN " +
                                        "EXECUTE IMMEDIATE 'CREATE TABLE IMBCBBRP_BKP"+
                                        "(A8BRCD CHAR(3),"+
                                        "A8CMCD CHAR(3),"+
                                        "A8LCCD CHAR(3),"+
                                        "A8DFAC CHAR(4),"+
                                        "A8DTAC CHAR(4),"+
                                        "A8BICN CHAR(8),"+
                                        "A8LCD NUMBER(10,0),"+
                                        "A8TYLC CHAR(1),"+
                                        "A8BRNM VARCHAR2(30),"+
                                        "A8BRSN CHAR(3),"+
                                        "BBRRI CHAR(1),"+
                                        "BCORI CHAR(1),"+
                                        "BCBBR CHAR(4),"+
                                        "BR_HEAD CHAR(1) DEFAULT ''N'',"+
                                        "BR_OPER CHAR(1) DEFAULT ''N'')';  END;");
        tableScript.put("IMBCBCMP_BKP", "DECLARE PRAGMA AUTONOMOUS_TRANSACTION; " +
                                        " BEGIN " +
                                        "EXECUTE IMMEDIATE 'CREATE TABLE IMBCBCMP_BKP"+
                                        "(	CCPCD CHAR(3), "+
                                        " CCPNE VARCHAR2(30),"+
                                        "CCPNR VARCHAR2(80),"+
                                        "CCPRI CHAR(1),"+
                                        "CCBBR CHAR(4))'; END;");
        tableScript.put( "V_GL_DWH_LOAD_STATUS", "DECLARE PRAGMA AUTONOMOUS_TRANSACTION; " +
                                          " BEGIN " +
                                          "EXECUTE IMMEDIATE 'CREATE TABLE V_GL_DWH_LOAD_STATUS(MAX_LOAD_DATE DATE NOT NULL)'; END;");
        tableScript.put( "DH_BR_MAP_BKP", "DECLARE PRAGMA AUTONOMOUS_TRANSACTION; " +
                                          " BEGIN " +
                                          "EXECUTE IMMEDIATE 'CREATE TABLE DH_BR_MAP_BKP " +
                                          "(FCC_BRANCH CHAR(3)," +
                                          "MIDAS_BRANCH CHAR(3)," +
                                          "CBR_BRANCH CHAR(4))'; END;");
    }
    public void test1() throws Exception {
        String ablok = "DECLARE PRAGMA AUTONOMOUS_TRANSACTION;"+
                " BEGIN " +
                "EXECUTE IMMEDIATE 'CREATE TABLE V_GL_DWH_IMBCBCMP2" +
                "                            (CCPCD CHAR(3) NOT NULL," +
                "                            CCPNE VARCHAR2(30) NOT NULL," +
                "                            CCPNR VARCHAR2(80) NOT NULL," +
                "                            CCPRI CHAR(1) NOT NULL," +
                "                            CCBBR CHAR(4) NOT NULL," +
                "                            ALT_CODE CHAR(3) NOT NULL," +
                "                            VALID_FROM DATE NOT NULL" +
                "                            )';"+
                " END;";

        baseEntityRepository.executeNativeUpdate(ablok);
    }

}