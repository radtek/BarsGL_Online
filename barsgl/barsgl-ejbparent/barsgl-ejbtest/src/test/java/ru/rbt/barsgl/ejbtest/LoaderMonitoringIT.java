package ru.rbt.barsgl.ejbtest;

import org.junit.Test;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Created by Ivan Sevastyanov on 13.07.2018.
 */
public class LoaderMonitoringIT extends AbstractRemoteIT {

    private static final String GL_WORKPROC = "GL_WORKPROC";
    private static final String REP_WORKPROC = "REP_WORKPROC";

    private static class DBCfgString {

        private final String cfgName;
        private final String propName;
        private final String defultValue;

        private DBCfgString(String cfgName, String propName, String defultValue) {
            this.cfgName = cfgName;
            this.propName = propName;
            this.defultValue = defultValue;
        }

        public String getCfgName() {
            return cfgName;
        }

        public String getPropName() {
            return propName;
        }

        public String getDefultValue() {
            return defultValue;
        }

        public static DBCfgString valueOf(String cfgName, String value, String defultValue) {
            return new DBCfgString(cfgName, value, defultValue);
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

        String cfgName = baseEntityRepository.selectOne("select pkg_mon.get_monitor_cfgname() nm from dual").getString("nm");
        String cfgGlTableName = baseEntityRepository.selectOne("select pkg_mon.get_monitor_propname_gltab() nm from dual").getString("nm");
        String cfgRepTableName = baseEntityRepository.selectOne("select pkg_mon.get_monitor_propname_reptab() nm from dual").getString("nm");

        baseEntityRepository.executeNativeUpdate("delete from db_cfg where CFG_NAME = ? ", cfgName);

        String glTableNameWorkproc = getTableName(DBCfgString.valueOf(cfgName, cfgGlTableName, GL_WORKPROC));

        String repTableNameWorkproc = getTableName(DBCfgString.valueOf(cfgName, cfgRepTableName, REP_WORKPROC));



        //
        //
        //
        //
        //
        //
        //
        //
        //
    }

    private String getTableName (DBCfgString cfgString) throws SQLException {
        return Optional.ofNullable(getTableNameSys(cfgString)).map(r -> r.getString("str_value")).orElseGet(
                () -> {
                    try {
                        createCfg(cfgString, cfgString.getDefultValue());
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

    private void checkCreateWorkproc(DBCfgString dbCfg) {
        try {
            baseEntityRepository.selectFirst("select 1 from " + dbCfg.getDefultValue());
        } catch (SQLException e) {
            baseEntityRepository.
        }

    }


    private static final String create_tab =


}
