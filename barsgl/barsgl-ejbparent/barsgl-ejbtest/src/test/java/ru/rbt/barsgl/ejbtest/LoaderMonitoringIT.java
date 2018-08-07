package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.ejbcore.datarec.DBParam;
import ru.rbt.ejbcore.datarec.DBParams;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static ru.rbt.ejbcore.datarec.DBParam.DBParamDirectionType.IN;
import static ru.rbt.ejbcore.datarec.DBParam.DBParamDirectionType.OUT;
import static ru.rbt.ejbcore.datarec.DBParam.DbParamType.*;

/**
 * Created by Ivan Sevastyanov on 13.07.2018.
 */
public class LoaderMonitoringIT extends AbstractRemoteIT {

    private final static Logger log = Logger.getLogger(LoaderMonitoringIT.class.getName());

    private static final String GL_WORKPROC = "GL_STAT_GLWORKPROC";
    private static final String REP_WORKPROC = "GL_STAT_REPWORKPROC";
    private static final String GL_WORKPROC_ESCAL_HIST = "WORK_ESCALATE_HIST";
    private static final String BARSREP_WORKPROC_ESCAL_HIST = "BARSREP_WORK_ESCALATE_HIST";
    private static final String STEP_NAME = "STEP1";
    private enum Source {
        GL, BARSREP
    }

    private static class DBCfgString {

        private final String cfgName;
        private final String propName;
        private final String defaultValue;

        private DBCfgString(String cfgName, String propName, String defaultValue) {
            this.cfgName = cfgName;
            this.propName = propName;
            this.defaultValue = defaultValue;
        }

        public String getCfgName() {
            return cfgName;
        }

        public String getPropName() {
            return propName;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public static DBCfgString valueOf(String cfgName, String value, String defaultValue) {
            return new DBCfgString(cfgName, value, defaultValue);
        }

        @Override
        public String toString() {
            return "DBCfgString{" +
                    "cfgName='" + cfgName + '\'' +
                    ", propName='" + propName + '\'' +
                    '}';
        }
    }


    @Test
    public void test() throws SQLException {

        // загружаем данные для анализа и прогнозирования за настраиваемый период в днях (DB_CFG)
        // каждая таблица (GL,BARSREP) должна быть зарегистрирована в настройках DB_CFG
        // загрузка GL через представление на workproc

        createStepProps();

        String cfgName = baseEntityRepository.selectOne("select pkg_workproc_mon.get_monitor_cfgname() nm from dual").getString("nm");
        String cfgGlTableName = baseEntityRepository.selectOne("select pkg_workproc_mon.get_monitor_propname_gltab() nm from dual").getString("nm");
        String cfgRepTableName = baseEntityRepository.selectOne("select pkg_workproc_mon.get_monitor_propname_reptab() nm from dual").getString("nm");

        baseEntityRepository.executeNativeUpdate("delete from db_cfg where CFG_NAME = ? ", cfgName);

        String glTableNameWorkproc = getTableName(DBCfgString.valueOf(cfgName, cfgGlTableName, GL_WORKPROC));

        String repTableNameWorkproc = getTableName(DBCfgString.valueOf(cfgName, cfgRepTableName, REP_WORKPROC));

        baseEntityRepository.executeNativeUpdate("delete from " + GL_WORKPROC_ESCAL_HIST);

        final Date lwDate = getOperday().getLastWorkingDay();
        final Date startDate =DateUtils.addDays(lwDate, -30);

        baseEntityRepository.executeNativeUpdate("update workday set workday = ?", lwDate);

        fillWorkproc(GL_WORKPROC, startDate);
        fillWorkproc(REP_WORKPROC, startDate);

        // выгрузка во временную таблицу для анализа
        Assert.assertTrue(0 < load4Analyze(GL_WORKPROC, startDate, Source.GL));
        Assert.assertTrue(0 < load4Analyze(REP_WORKPROC, startDate, Source.BARSREP));

        // заполнение таблицы сеанса расчета с вычислением прогнозируемых продолжительностей шагов - предварительный расчет
        baseEntityRepository.executeNativeUpdate("BEGIN PKG_WORKPROC_MON.ESTIMATE_STEP; END;");

        List<DataRecord> ests = baseEntityRepository.select("select * from GL_STAT_EST_WORKPROC");
        Assert.assertEquals(2, ests.size());
        Assert.assertTrue(ests.stream().anyMatch(r -> "39".equals(r.getString("Y"))));
        Assert.assertTrue(ests.stream().anyMatch(r -> "39".equals(r.getString("EST"))));
        DataRecord glest = ests.stream().filter(r -> r.getString("src").equals(Source.GL.name())).findAny()
                .orElseThrow(() -> new RuntimeException("gl is not found"));
        Assert.assertTrue(glest.getString("PARM"), glest.getLong("PARM") == GL_PARAM);
        DataRecord repest = ests.stream().filter(r -> r.getString("src").equals(Source.BARSREP.name())).findAny()
                .orElseThrow(() -> new RuntimeException("rep not found"));
        Assert.assertTrue(repest.getString("PARM"), repest.getLong("PARM") == REP_PARAM);

        // на данном шаге в т.ч. происходит исключение с настраиваего процента точек с максимальной пограшностью и перерасчет прогнозируемых значений
        // должно удалиться 2 записи, т.е. в GL_STAT_TMP_WORKPROC остается 28 записей по каждому источнику
        baseEntityRepository.executeNativeUpdate("BEGIN PKG_WORKPROC_MON.CLEAR_STAT_TAB; END;");

        DataRecord cnt = baseEntityRepository.selectFirst("select count(1) cnt from GL_STAT_TMP_WORKPROC");
        Assert.assertTrue(cnt.getString("cnt"), 28L * 2 == cnt.getLong("cnt"));

        // производим пересчет после исключения точек с максимальной погрешностью
        baseEntityRepository.executeNativeUpdate("BEGIN PKG_WORKPROC_MON.ESTIMATE_STEP; END;");
        // средние значения не поменяются
        ests = baseEntityRepository.select("select * from GL_STAT_EST_WORKPROC");
        Assert.assertEquals(2, ests.size());
        Assert.assertTrue(ests.stream().anyMatch(r -> "39".equals(r.getString("Y"))));
        Assert.assertTrue(ests.stream().anyMatch(r -> "39".equals(r.getString("EST"))));

        // вычисление макс/миним длительности шагов
        baseEntityRepository.executeNativeUpdate("BEGIN PKG_WORKPROC_MON.UPDATE_MINMAX; END;");
        ests = baseEntityRepository.select("select * from GL_STAT_EST_WORKPROC");
        Assert.assertTrue(ests.stream().anyMatch(r -> !"0".equals(r.getString("minest"))));
        Assert.assertTrue(ests.stream().anyMatch(r -> !"0".equals(r.getString("maxest"))));

        // применить значения в историю
        baseEntityRepository.executeNativeUpdate("BEGIN PKG_WORKPROC_MON.ACCEPT_HIST(?, ?); END;"
            , Source.GL.name(), GL_WORKPROC_ESCAL_HIST);

        List<DataRecord> eschist = baseEntityRepository.select("select * from " + GL_WORKPROC_ESCAL_HIST);
        Assert.assertEquals(1, eschist.size());

        // запрет пересчета - нет запрета
        final long minEscalate = 100L;
        final long minEscalateCalc = 23L; // расчетное значение
        Assert.assertEquals(1, baseEntityRepository.executeNativeUpdate("update " + GL_WORKPROC_ESCAL_HIST + " set MINESCALATE = ?, fixed = ? where id = ?"
            , minEscalate, "0", STEP_NAME));
        eschist = baseEntityRepository.select("select * from " + GL_WORKPROC_ESCAL_HIST);
        Assert.assertEquals(1, eschist.size());
        Assert.assertTrue(eschist.get(0).getString("MINESCALATE"), minEscalate == eschist.get(0).getLong("MINESCALATE"));

        baseEntityRepository.executeNativeUpdate("BEGIN PKG_WORKPROC_MON.ACCEPT_HIST(?, ?); END;"
                , Source.GL.name(), GL_WORKPROC_ESCAL_HIST);
        eschist = baseEntityRepository.select("select * from " + GL_WORKPROC_ESCAL_HIST);
        Assert.assertEquals(1, eschist.size());
        Assert.assertTrue(eschist.get(0).getString("MINESCALATE"), minEscalateCalc == eschist.get(0).getLong("MINESCALATE"));

        // запрет пересчета - включаем запрет
        Assert.assertEquals(1, baseEntityRepository.executeNativeUpdate("update " + GL_WORKPROC_ESCAL_HIST + " set MINESCALATE = ?, fixed = ? where id = ?"
                , minEscalate, "1", STEP_NAME));
        baseEntityRepository.executeNativeUpdate("BEGIN PKG_WORKPROC_MON.ACCEPT_HIST(?, ?); END;"
                , Source.GL.name(), GL_WORKPROC_ESCAL_HIST);
        eschist = baseEntityRepository.select("select * from " + GL_WORKPROC_ESCAL_HIST);
        Assert.assertEquals(1, eschist.size());
        Assert.assertTrue(eschist.get(0).getString("MINESCALATE"), minEscalate == eschist.get(0).getLong("MINESCALATE"));

        // еще таблица
        baseEntityRepository.executeNativeUpdate(CREATE_BARSREP_ESCALATE_HIST, BARSREP_WORKPROC_ESCAL_HIST);
        baseEntityRepository.executeNativeUpdate("BEGIN PKG_WORKPROC_MON.ACCEPT_HIST(?, ?); END;"
                , Source.BARSREP.name(), BARSREP_WORKPROC_ESCAL_HIST);
        eschist = baseEntityRepository.select("select * from " + BARSREP_WORKPROC_ESCAL_HIST);
        Assert.assertEquals(1, eschist.size());
        Assert.assertTrue(eschist.get(0).getString("MINESCALATE"), minEscalateCalc == eschist.get(0).getLong("MINESCALATE"));
    }

    private String getTableName (DBCfgString cfgString) throws SQLException {
        return Optional.ofNullable(getTableNameSys(cfgString)).map(r -> r.getString("str_value")).orElseGet(
                () -> {
                    try {
                        createCfg(cfgString, cfgString.getDefaultValue());
                        return getTableNameSys(cfgString).getString("str_value");
                    } catch (SQLException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }
        );
    }

    private DataRecord getTableNameSys(DBCfgString cfgString) throws SQLException {
        return baseEntityRepository.selectFirst("select str_value from db_cfg where CFG_NAME = ? and PROP_NAME = ?",
                cfgString.cfgName, cfgString.getPropName());
    }

    private void createCfg(DBCfgString cfgString, String value) {
        baseEntityRepository.executeNativeUpdate("insert into db_cfg (DATE_FROM, DATE_TO, CFG_NAME, PROP_NAME, STR_VALUE) " +
                " values ('2001-01-01', '2029-01-01', ?, ?, ?)",
                cfgString.getCfgName(), cfgString.getPropName(), value);
    }

    private Long load4Analyze(String fromTable, Date startDate, Source source) throws SQLException {
        DBParams result = baseEntityRepository.executeCallable(LOAD_FOR_ANALYZE
                , DBParams.createParams().addParam(VARCHAR, IN, fromTable).addParam(DATE, IN, startDate).addParam(VARCHAR, IN, source.name()).addParam(LONG,OUT,null));
        log.info("Loaded for analyze " + result.getParams().get(3).getValue() + ", tab: " + fromTable);
        return (Long) result.getParams().get(3).getValue();
    }

    private void fillWorkproc(String workprocTab, Date startDate) throws SQLException {
        baseEntityRepository.executeCallable(CREATE_WORKPROC_TAB_STRING,
                DBParams.createParams(new DBParam(VARCHAR, IN, workprocTab)));
        baseEntityRepository.executeCallable(FILL_WORKPROC_CONST
                , DBParams.createParams(new DBParam(VARCHAR, IN, workprocTab), new DBParam(VARCHAR, IN, STEP_NAME), new DBParam(DATE, IN, startDate)));
        Assert.assertNotNull(baseEntityRepository.selectFirst("select * from " + workprocTab));

    }

    private static void createStepProps() {
        baseEntityRepository.executeNativeUpdate(CREATE_CONST_FUNC);
        baseEntityRepository.executeNativeUpdate("delete from WORK_ESCALATE_STEP");
        baseEntityRepository.executeNativeUpdate("insert into WORK_ESCALATE_STEP (ID_STEP, DEFMIN, FUNCNAME,SRC) values (?,?,?,?)"
            , STEP_NAME, DEFMIN, CONST_FUNC_NAME, Source.GL.name());
        baseEntityRepository.executeNativeUpdate("insert into WORK_ESCALATE_STEP (ID_STEP, DEFMIN, FUNCNAME,SRC) values (?,?,?,?)"
            , STEP_NAME, DEFMIN, CONST_FUNC_NAME, Source.BARSREP.name());
    }

    private static final String CREATE_WORKPROC_TAB_STRING =
                    "declare\n" +
                    "    pragma autonomous_transaction;\n" +
                    "    l_table_name varchar2(30) := ?;\n" +
                    "    \n" +
                    "    e_tabnotfound exception;\n" +
                    "    pragma exception_init(e_tabnotfound, -942);\n" +
                    "begin\n" +
                    "    begin\n" +
                    "        execute immediate 'drop table '||l_table_name;\n" +
                    "    exception \n" +
                    "        when e_tabnotfound then null;\n" +
                    "    end;\n" +
                    "    \n" +
                    "    execute immediate 'create table '||l_table_name||' as select * from workproc where rownum < 1';\n" +
                    "    commit;\n" +
                    "end;\n";

    private static final String FILL_WORKPROC_CONST =
            "declare\n" +
            "    l_tabname varchar2(30) := ?;\n" +
            "    l_stepname char(10) := ?;\n" +
            "    l_startdat date := ?;\n" +
            "begin\n" +
            "    execute immediate 'delete from '||l_tabname;\n" +
            "    for nn in (select rownum rn from dual connect by rownum <= 30) loop\n" +
            "        execute immediate 'insert into '||l_tabname||' (DAT, ID, STARTTIME, ENDTIME, RESULT, COUNT) values (:1,:2,:3,:4,:5,:6)'\n" +
            "            using l_startdat + nn.rn - 1, l_stepname,  l_startdat + nn.rn - 1 + 3/24 - mod(nn.rn, 10) * 1/24/60, l_startdat + nn.rn - 1 + 3/24 + 1/48 + mod(nn.rn, 10) * 1/24/60, 'O', 1; \n" +
            "    end loop;\n" +
            "end;";

    private static final String LOAD_FOR_ANALYZE =
            "declare\n" +
            "    l_count number;\n" +
            "begin\n" +
            "    PKG_WORKPROC_MON.load_history(?, ?, ?, l_count);\n" +
            "    \n" +
            "    ? := l_count;\n" +
            "end;";

    private final static String CREATE_BARSREP_ESCALATE_HIST =
            "declare\n" +
            "    pragma autonomous_transaction;\n" +
            "    l_esc_tab varchar2(30) := ?;\n" +
            "begin\n" +
            "    DB_CONF.DROP_TABLE_IF_EXISTS(user, l_esc_tab);\n" +
            "    execute immediate 'create table '||l_esc_tab||' as select * from WORK_ESCALATE_HIST where rownum < 1';\n" +
            "    commit;\n" +
            "end;";

    private static final String CONST_FUNC_NAME = "MON_STEP1";


    private static final int GL_PARAM = 111;
    private static final int REP_PARAM = 112;
    private static final int DEFMIN = 2323;

    private static final String CREATE_CONST_FUNC =
            "declare\n" +
            "   pragma autonomous_transaction;\n" +
            "begin\n" +
            "    execute immediate q'[\n" +
            "    create or replace function "+CONST_FUNC_NAME+"(a_step varchar2, a_dat date, a_src varchar2) return number is\n" +
            "    begin\n" +
            "        if (a_src = PKG_WORKPROC_MON.C_SRC_GL) then\n" +
            "            return "+ GL_PARAM+";\n" +
            "        else\n" +
            "            return "+REP_PARAM+";\n" +
            "        end if;\n" +
            "    end "+CONST_FUNC_NAME+";\n" +
            "    ]';\n" +
            "   commit;\n" +
            "end;";

}
