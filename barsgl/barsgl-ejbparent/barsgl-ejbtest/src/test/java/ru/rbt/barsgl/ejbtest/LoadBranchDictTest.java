package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.LoadBranchDictTask;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by er22317 on 09.02.2018.
 */
public class LoadBranchDictTest extends AbstractRemoteIT {
    private static final Logger log = Logger.getLogger(FanNdsPostingIT.class.getName());

    @Test
    public void test() throws Exception {
        String fix = saveFix();
        try {
            initTables();
            setFix("065");

            Properties props = new Properties();
            props.put(LoadBranchDictTask.propOperDay, "2018-01-12");
            jobService.executeJob(SingleActionJobBuilder.create().withClass(LoadBranchDictTask.class).withProps(props).build());

            DataRecord rec = baseEntityRepository.selectFirst( "select count(*) cnt from IMBCBCMP where CCPCD = 'TTT'", new Object[]{});
            Assert.assertFalse("В IMBCBCMP не добавлена CCPCD = 'TTT'", rec.getLong("cnt") == 0l);
            rec = baseEntityRepository.selectFirst( "select count(*) cnt from IMBCBCMP where CCPCD = 'UFA' and CCBBR = '0093'", new Object[]{});
            Assert.assertFalse("В IMBCBCMP не обновлена CCPCD = 'UFA' and CCBBR = '0093'", rec.getLong("cnt") == 0l);
            rec = baseEntityRepository.selectFirst( "select count(*) cnt from IMBCBCMP where CCPCD = 'VLG' and CCBBR = '0066'", new Object[]{});
            Assert.assertFalse("В IMBCBCMP не обновлена CCPCD = 'VLG' and CCBBR = '0066'", rec.getLong("cnt") == 0l);


            rec = baseEntityRepository.selectFirst( "select count(*) cnt from DH_BR_MAP where FCC_BRANCH = 'TTT'", new Object[]{});
            Assert.assertFalse("В DH_BR_MAP не добавлена FCC_BRANCH = 'TTT'", rec.getLong("cnt") == 0l);
            rec = baseEntityRepository.selectFirst( "select count(*) cnt from DH_BR_MAP where FCC_BRANCH = 'UFA' and CBR_BRANCH = '0093' and MIDAS_BRANCH = '093'", new Object[]{});
            Assert.assertFalse("В DH_BR_MAP не обновлена CCPCD = 'UFA' and CBR_BRANCH = '0093'", rec.getLong("cnt") == 0l);
            rec = baseEntityRepository.selectFirst( "select count(*) cnt from DH_BR_MAP where FCC_BRANCH = 'VLG' and CBR_BRANCH = '0066' and MIDAS_BRANCH = '066'", new Object[]{});
            Assert.assertFalse("В DH_BR_MAP не обновлена FCC_BRANCH = 'VLG' and CBR_BRANCH = '0066'", rec.getLong("cnt") == 0l);

        }finally{
            restoreTables();
            setFix(fix);
        }
    }

    private void initTables() throws SQLException {
        String sqlTableExist = "select count(*) cnt from user_tables where table_name = ?";
        String createCMP = "CREATE TABLE V_GL_DWH_IMBCBCMP" +
                "                            (CCPCD CHAR(3) NOT NULL," +
                "                            CCPNE VARCHAR2(30) NOT NULL," +
                "                            CCPNR VARCHAR2(80) NOT NULL," +
                "                            CCPRI CHAR(1) NOT NULL," +
                "                            CCBBR CHAR(4) NOT NULL," +
                "                            ALT_CODE CHAR(3) NOT NULL," +
                "                            VALID_FROM DATE NOT NULL" +
                "                            )";
        String createCMP_BKP = "CREATE TABLE IMBCBCMP_BKP"+
                               "(	CCPCD CHAR(3), "+
                               " CCPNE VARCHAR2(30),"+
                               "CCPNR VARCHAR2(80),"+
                               "CCPRI CHAR(1),"+
                               "CCBBR CHAR(4))";
        String createLOAD_STATUS = "CREATE TABLE V_GL_DWH_LOAD_STATUS(MAX_LOAD_DATE DATE NOT NULL)";
        String DH_BR_MAP_BKP = "  CREATE TABLE DH_BR_MAP_BKP \n" +
                "   (\tFCC_BRANCH CHAR(3), \n" +
                "\t    MIDAS_BRANCH CHAR(3), \n" +
                "\t    CBR_BRANCH CHAR(4))";
        Long cnt = 0l;

        DataRecord rec = baseEntityRepository.selectFirst( sqlTableExist, new Object[]{"V_GL_DWH_IMBCBCMP"});
        Assert.assertFalse("Необходимо создать таблицу V_GL_DWH_IMBCBCMP", rec.getLong("cnt") == 0l);
        rec = baseEntityRepository.selectFirst( sqlTableExist, new Object[]{"IMBCBCMP_BKP"});
        Assert.assertFalse("Необходимо создать таблицу IMBCBCMP_BKP", rec.getLong("cnt") == 0l);
        rec = baseEntityRepository.selectFirst( sqlTableExist, new Object[]{"DH_BR_MAP_BKP"});
        Assert.assertFalse("Необходимо создать таблицу DH_BR_MAP_BKP", rec.getLong("cnt") == 0l);
        rec = baseEntityRepository.selectFirst( sqlTableExist, new Object[]{"V_GL_DWH_LOAD_STATUS"});
        Assert.assertFalse("Необходимо создать таблицу V_GL_DWH_LOAD_STATUS", rec.getLong("cnt") == 0l);
        rec = baseEntityRepository.selectFirst( "select count(*) cnt from V_GL_DWH_LOAD_STATUS", new Object[]{});
        if (rec.getInteger(0) == 0){
            baseEntityRepository.executeNativeUpdate("insert into V_GL_DWH_LOAD_STATUS values(to_date('13.01.18','DD.MM.RR'))", new Object[]{});
        }

        moveIMBCBCMP("IMBCBCMP", "IMBCBCMP_BKP");
        moveDH_BR_MAP("DH_BR_MAP", "DH_BR_MAP_BKP");

        testToV_GL_DWH_IMBCBCMP();

    }

    private void testToV_GL_DWH_IMBCBCMP(){
        List<String> stms = new ArrayList<String>();
        stms.add("Insert into V_GL_DWH_IMBCBCMP (CCPCD,CCPNE,CCPNR,CCPRI,CCBBR,ALT_CODE,VALID_FROM) values ('MOS','UCB, Moscow','АО ЮНИКРЕДИТ БАНК Г МОСКВА','Y','0001','001',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBCMP (CCPCD,CCPNE,CCPNR,CCPRI,CCBBR,ALT_CODE,VALID_FROM) values ('SPB','UCB, Petersburg Branch','Петербургский филиал','Y','0002','002',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBCMP (CCPCD,CCPNE,CCPNR,CCPRI,CCBBR,ALT_CODE,VALID_FROM) values ('PRM','UCB, Perm Branch','Пермский филиал','Y','0009','009',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBCMP (CCPCD,CCPNE,CCPNR,CCPRI,CCBBR,ALT_CODE,VALID_FROM) values ('RND','UCB, Rostov Branch','Ростовский филиал','Y','0015','015',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBCMP (CCPCD,CCPNE,CCPNR,CCPRI,CCBBR,ALT_CODE,VALID_FROM) values ('CHL','UCB, Chelyabinsk Branch','Челябинский филиал','Y','0016','016',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBCMP (CCPCD,CCPNE,CCPNR,CCPRI,CCBBR,ALT_CODE,VALID_FROM) values ('KRS','UCB, Krasnodar Branch','Краснодарский филиал','Y','0023','023',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBCMP (CCPCD,CCPNE,CCPNR,CCPRI,CCBBR,ALT_CODE,VALID_FROM) values ('VRN','UCB, Voronezh Branch','Воронежский филиал','Y','0030','030',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBCMP (CCPCD,CCPNE,CCPNR,CCPRI,CCBBR,ALT_CODE,VALID_FROM) values ('SAM','UCB, Samara Branch','Самарский филиал','Y','0033','033',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBCMP (CCPCD,CCPNE,CCPNR,CCPRI,CCBBR,ALT_CODE,VALID_FROM) values ('EKB','UCB, Ekaterinburg Branch','Екатеринбургский филиал','Y','0040','040',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBCMP (CCPCD,CCPNE,CCPNR,CCPRI,CCBBR,ALT_CODE,VALID_FROM) values ('NNV','UCB, Nizhny Novgorod Branch','Нижегородский филиал','Y','0045','045',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBCMP (CCPCD,CCPNE,CCPNR,CCPRI,CCBBR,ALT_CODE,VALID_FROM) values ('NVS','UCB, Novosibirsk Branch','Новосибирский филиал','Y','0050','050',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBCMP (CCPCD,CCPNE,CCPNR,CCPRI,CCBBR,ALT_CODE,VALID_FROM) values ('UFA','UCB, Ufa Branch','Башкирский филиал','Y','0093','093',to_date('13.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBCMP (CCPCD,CCPNE,CCPNR,CCPRI,CCBBR,ALT_CODE,VALID_FROM) values ('VLG','UCB, Volgograd Branch','Волгоградский филиал','Y','0066','066',to_date('13.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBCMP (CCPCD,CCPNE,CCPNR,CCPRI,CCBBR,ALT_CODE,VALID_FROM) values ('STV','UCB, Stavropol Branch','Ставропольский филиал','Y','0013','101',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBCMP (CCPCD,CCPNE,CCPNR,CCPRI,CCBBR,ALT_CODE,VALID_FROM) values ('TTT','UCB, Test new filial','Тест нового филиала','Y','0095','118',to_date('13.01.18','DD.MM.RR'))");

        clearTable("V_GL_DWH_IMBCBCMP");
        insTable(stms);
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
        moveIMBCBCMP("IMBCBCMP_BKP", "IMBCBCMP");
        moveDH_BR_MAP("DH_BR_MAP_BKP", "DH_BR_MAP");
    }


        private void clearTable(String table){
        baseEntityRepository.executeNativeUpdate("delete from " + table, new Object[]{});
    }

    private void insTable(List<String> list){
        list.forEach(item -> baseEntityRepository.executeNativeUpdate(item, new Object[]{}));
    }

    private void setFix(String fix) throws SQLException {
        baseEntityRepository.executeNativeUpdate("update gl_prprp set NUMBER_VALUE=? where id_prp = 'dwh.fix.branch.code'", fix);
    }
    private String saveFix() throws SQLException {
        return(baseEntityRepository.selectFirst("select NUMBER_VALUE from gl_prprp where id_prp = 'dwh.fix.branch.code'",new Object[]{}).getString(0));
    }
}