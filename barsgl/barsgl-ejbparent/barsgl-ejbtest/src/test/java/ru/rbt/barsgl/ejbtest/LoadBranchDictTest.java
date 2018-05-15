package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.LoadBranchDictTask;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Created by er22317 on 09.02.2018.
 */
public class LoadBranchDictTest extends AbstractRemoteIT {
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

    private void initTables(String VITRINA) throws SQLException {
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

    private void testToV_GL_DWH_IMBCBBRP() {
        List<String> stms = new ArrayList<String>();
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('001','MOS','00000018','AO Prechistenskaya','N','Y','0001','Y','N','A01',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('002','SPB','00000026','AO Fontanka','N','Y','0002','Y','N','B01',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('003','MOS','00000034','AO Dmitrovka','N','Y','0001','N','N','A04',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('004','MOS','00000042','AO Leninsky','N','Y','0001','N','N','A05',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('005','MOS','00000059','AO Kosmodamianskaya','N','Y','0001','N','N','A06',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('006','MOS','00000067','AO Prospect Mira','N','Y','0001','N','N','A07',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('007','MOS','00000075','AO B.Gruzinskaya','N','Y','0001','N','N','A03',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('008','MOS','00000083','AO Kazachy','N','Y','0001','N','N','A02',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('009','PRM','00000091','Perm Branch','N','Y','0009','Y','N','E01',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('010','NVS','00000109','OO Barnaulsky','N','Y','0050','N','Y','L04',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('014','SPB','00000141','AO Kirochnaya','N','Y','0002','N','N','B02',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('015','RND','00000151','Rostov Branch','N','Y','0015','Y','N','C01',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('016','CHL','00000161','Chelyabinsk Branch','N','Y','0016','Y','N','D01',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('017','MOS','00000171','AO Khamovniky','N','Y','0001','N','N','A08',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('018','MOS','00000181','AO Zemlyanoy val','N','Y','0001','N','N','A09',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('019','MOS','00000191','AO Yuzhnaya','N','Y','0001','N','N','A18',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('020','MOS','00000201','MOS, Petrovsky blv. 23','N','Y','0001','N','N','A10',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('021','MOS','00000211','AO Zubovsky boulevard','N','Y','0001','N','N','A11',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('022','SPB','00000221','SPB, Preobrazhenskaya ploshcha','N','Y','0002','N','N','B03',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('023','KRS','00000231','Krasnodar Branch','N','Y','0023','Y','N','F01',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('024','MOS','00000241','AO Kutuzovsky prospekt','N','Y','0001','N','N','A12',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('025','MOS','00000251','AO Prospekt Vernadskogo','N','Y','0001','N','N','A13',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('026','MOS','00000261','AO Lomonosovsky prospekt','N','Y','0001','N','N','A14',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('027','MOS','00000271','MOS, Pyatnitskaya, 14','N','Y','0001','N','N','A15',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('028','MOS','00000281','AO Oktyabrskoe Pole','N','Y','0001','N','N','A16',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('029','MOS','00000291','AO Pyatnitskaya','N','Y','0001','N','N','A17',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('030','VRN','00000301','Voronezh Branch','N','Y','0030','Y','N','J01',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('031','SPB','00000311','AO Petrogradskaya storona','N','Y','0002','N','N','B05',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('032','SPB','00000321','AO Park Pobedy','N','Y','0002','N','N','B06',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('033','SAM','00000331','Samara Branch','N','Y','0033','Y','N','G01',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('034','SPB','00000341','AO Prospekt Prosveshcheniya','N','Y','0002','N','N','B04',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('035','MOS','00000351','AO Taganskaya','N','Y','0001','N','N','A19',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('036','MOS','00000361','AO Myasnitskaya','N','Y','0001','N','N','A20',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('037','MOS','00000371','AO Pokrovka','N','Y','0001','N','N','A21',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('038','SPB','00000381','AO Sennaya pl.','N','Y','0002','N','N','B07',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('039','CHL','00000391','AO Severo-Zapad','N','Y','0016','N','N','D02',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('040','EKB','00000401','Ekaterinburg Branch','N','Y','0040','Y','N','K01',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('041','MOS','00000411','MOS, Zemlyanoy Val, 25A, str. ','N','Y','0001','N','N','A22',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('042','MOS','00000421','AO Pervomayskaya','N','Y','0001','N','N','A23',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('043','MOS','00000431','AO Mayakovskaya','N','Y','0001','N','N','A24',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('044','MOS','00000441','AO Tulskaya','N','Y','0001','N','N','A25',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('045','NNV','00000451','Nizhny Novgorod Branch','N','Y','0045','Y','N','M01',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('046','CHL','00000461','AO Magnitogorsky','N','Y','0016','N','N','D03',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('047','MOS','00000471','AO Alekseevskaya','N','Y','0001','N','N','A26',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('049','RND','00000491','AO B. Sadovaya','N','Y','0015','N','N','C02',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('050','NVS','00000501','Novosibirsk Branch','N','Y','0050','Y','N','L01',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('051','MOS','00000511','MOS, CCO at the Porsche showro','N','Y','0001','N','N','A27',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('052','MOS','00000521','AO Zvenigorodsky','N','Y','0001','N','N','A28',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('053','MOS','00000531','AO Rechnoy vokzal','N','Y','0001','N','N','A29',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('054','MOS','00000541','AO Tverskaya','N','Y','0001','N','N','A31',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('055','MOS','00000551','AO Aeroport','N','Y','0001','N','N','A32',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('056','MOS','00000561','MOS, CCO Yaroslavsky','N','Y','0001','N','N','A33',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('057','MOS','00000571','AO Novoslobodskaya','N','Y','0001','N','N','A30',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('058','SPB','00000581','AO Prospekt Slavy','N','Y','0002','N','N','B08',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('059','PRM','00000591','AO Stroganovsky','N','Y','0009','N','N','E02',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('060','SPB','00000601','AO Vasilievsky ostrov','N','Y','0002','N','N','B09',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('061','KRS','00000611','AO Pokrovsky','N','Y','0023','N','N','F02',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('062','SPB','00000621','AO Komendantsky pr-t','N','Y','0002','N','N','B10',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('063','MOS','00000631','AO Odintsovo','N','Y','0001','N','N','A34',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('064','MOS','00000641','AO Khimky','N','Y','0001','N','N','A35',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('065','MOS','00000651','AO Yartsevskaya','N','Y','0001','N','N','A36',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('066','UFA','00000661','Ufa Branch','N','Y','0066','Y','N','N01',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('067','MOS','00000671','MOS, B.Nikitskaya, 15, str. 1','N','Y','0001','N','N','A37',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('068','VRN','00000681','OO Lipetsky','N','Y','0030','N','Y','J02',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('069','SPB','00000691','AO Chernaya Rechka','N','Y','0002','N','N','B11',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('070','MOS','00000701','AO Ramenki','N','Y','0001','N','N','A38',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('071','MOS','00000711','AO Rogozhskaya zastava','N','Y','0001','N','N','A39',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('072','KRS','00000721','KRS, CCO Stavropolsky','N','Y','0023','N','N','F03',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('073','NVS','00000731','OO Omsky','N','Y','0050','N','Y','L02',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('074','SPB','00000741','OO Arkhangelsky','N','Y','0002','N','Y','B12',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('075','CHL','00000751','AO Ul. Svobody','N','Y','0016','N','N','D04',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('076','MOS','00000761','AO Komsomolskaya','N','Y','0001','N','N','A40',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('077','SAM','00000771','AO Ul. Pobedy','N','Y','0033','N','N','G03',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('078','MOS','00000781','AO Ostozhenka','N','Y','0001','N','N','A41',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('079','SPB','00000791','AO Zanevskaya pl.','N','Y','0002','N','N','B13',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('080','SPB','00000801','AO Akademisheskaya','N','Y','0002','N','N','B14',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('081','SPB','00000811','SPB, Moskovsky prospekt 193','N','Y','0002','N','N','B16',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('082','SPB','00000821','AO Leninsky pr-t','N','Y','0002','N','N','B17',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('083','SPB','00000831','AO Prospekt Bolshevikov','N','Y','0002','N','N','B15',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('084','SPB','00000841','AO Moskovskaya','N','Y','0002','N','N','B18',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('085','MOS','00000851','AO Lyublino','N','Y','0001','N','N','A43',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('086','KRS','00000861','AO Novorossiysky','N','Y','0023','N','N','F04',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('087','MOS','00000871','AO Mytishchi','N','Y','0001','N','N','A42',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('088','MOS','00000881','AO Podolsk','N','Y','0001','N','Y','A46',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('089','CHL','00000891','AO Miassky','N','Y','0016','N','N','D05',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('116','PRM','00001161','UCB Perm OO Chusovoy','N','Y','0009','N','N','E05',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('090','UFA','00000901','AO Prospekt Oktyabrya','N','Y','0066','N','N','N02',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('091','PRM','00000911','AO Sibirskaya','N','Y','0009','N','N','E03',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('092','MOS','00000921','MOS, B.Nikitskaya, 15, str. 1','N','Y','0001','N','N','A44',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('093','VLG','00000931','Volgograd Branch','N','Y','0093','Y','N','O01',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('094','MOS','00000941','AO Babushkinskaya','N','Y','0001','N','N','A45',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('095','MOS','00000951','AO Altufievo','N','Y','0001','N','N','A48',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('096','RND','00000961','RND, Tekuchyova street 2','N','Y','0015','N','N','C03',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('097','RND','00000971','AO Ul. Tekusheva','N','Y','0015','N','N','C04',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('098','NVS','00000981','OO Krasnoyarsky','N','Y','0050','N','Y','L03',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('099','CHL','00000991','OO Tyumensky','N','Y','0016','N','Y','D06',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('100','KRS','00001001','AO Sochinsky','N','Y','0023','N','N','F05',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('101','STV','00001011','Stavropol Branch','N','Y','0013','Y','N','P01',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('102','MOS','00001021','MOS, Mytischi 2','N','Y','0001','N','N','A47',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('103','EKB','00001031','AO Palladium','N','Y','0040','N','N','K02',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('104','NNV','00001041','OO Saratovsky','N','Y','0045','N','Y','M02',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('105','VRN','00001051','OO Belgorodsky','N','Y','0030','N','Y','J03',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('106','MOS','00001061','MOS, Chistye Prudy','N','Y','0001','N','N','A49',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('107','MOS','00001071','OO Kaluzhsky','N','Y','0001','N','Y','A50',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('108','NVS','00001081','OO Levoberezhny','N','Y','0050','N','N','L05',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('109','NVS','00001081','AO Ul. Vatutina','N','Y','0050','N','N','L06',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('110','NNV','00001101','AO Avtozavodsky','N','Y','0045','N','N','M03',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('111','MOS','00001111','OO Tsiolkovsky','N','Y','0001','N','N','A51',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('112','PRM','00001121','OO Solikamsky','N','Y','0009','N','N','E04',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('113','SPB','00000821','OO Novodvinsky','N','Y','0002','N','N','B19',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('114','NVS','00001141','OO Kemerovsky','N','Y','0050','N','N','L07',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('115','TTT','00001151','OO Kazansky','N','Y','0095','N','N','N03',to_date('13.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('117','RND','00001171','OO Kamensk-Shakhtinskiy','N','Y','0015','N','N','C05',to_date('11.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('118','TTT','00001172','Test main branch','N','Y','0095','Y','N','H01',to_date('12.01.18','DD.MM.RR'))");
        stms.add("Insert into V_GL_DWH_IMBCBBRP (A8BRCD,A8LCCD,A8BICN,A8BRNM,BBRRI,BCORI,BCBBR,BR_HEAD,BR_OPER,FCC_CODE,VALID_FROM) values ('119','TTT','00001173','Test branch','N','Y','0095','N','N','H02',to_date('15.01.18','DD.MM.RR'))");

        clearTable("V_GL_DWH_IMBCBBRP");
        insTable(stms);
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

    private void insTable(List<String> list){
        list.forEach(item -> baseEntityRepository.executeNativeUpdate(item, new Object[]{}));
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