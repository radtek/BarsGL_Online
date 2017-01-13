package ru.rbt.barsgl.ejb.controller.operday.task;

import org.apache.commons.lang3.time.DateUtils;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.repository.od.OperdayRepository;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.MidasPLReplication;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by ER19371 on 05.11.15.
 */
public class MidasSubAccountsUpdateTask implements ParamsAwareRunnable {

    public static final String DWH_UNLOAD_FULL_DATE_KEY = "operday";

    //Использовать текущий или предыдущий опердень
    private enum CurrentOperday {
        CURRENT, PREVIOUS
    }

    public static final String NAME = "MidasSubAccountsUpdate";

    public static final String UNLOAD_PST_PARAM_DESC = "DWH.FILL_IFXBSHCP";

    @Inject
    private OperdayController operdayController;

    @Inject
    private OperdayRepository operdayRepository;

    @EJB
    private AuditController auditController;

    @Inject
    private ru.rbt.barsgl.ejbcore.util.DateUtils dateUtils;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @Resource(mappedName="/jdbc/As400Dwh2Midas")
    private DataSource dwh2MidasDataSource;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        beanManagedProcessor.executeInNewTxWithDefaultTimeout(((persistence, connection) -> {
            try (CallableStatement st = connection.prepareCall("CALL FILL_IFXBSHCP(?,?,?)")) {

                Date operday = getExecuteDate(properties);
                auditController.info(MidasPLReplication
                        , format("Запуск процедуры выгрузки счетов доходов/расходов в Midas за '%s'", dateUtils.onlyDateString(operday)));

                st.setDate(1, new java.sql.Date(operday.getTime()));
                st.registerOutParameter(2, java.sql.Types.INTEGER);
                st.registerOutParameter(3, java.sql.Types.VARCHAR);

                //предотвращение повторного запуска программы (контроль параллельного запуска вызываемой процедуры)
                // делается в sql (select * from DWH.RUN_PROC where PROC_NAME = 'DWH.FILL_IFXBSHCP')
                st.execute();

                int errorCode = st.getInt(2);
                String errorMsg = st.getString(3);

                if (errorCode == 0) {
                    //всё хорошо
                    try (Connection direct = Optional.ofNullable(dwh2MidasDataSource)
                            .orElseThrow(() -> new DefaultApplicationException("Не удалось инициализировать соедниение для запуска репликации (DWH.IFXBSSER)"))
                            .getConnection()){
                        if (executeRPG(errorMsg, direct)) {
                            auditController.info(MidasPLReplication
                                    , format("Процедура выгрузки счетов доходов/расходов в Midas за '%s' отработала успешно", dateUtils.onlyDateString(operday)));
                        }
                    }
                } else {
                    //ошибки обработки
                    auditController.error(MidasPLReplication, "Ошибка заполнения таблицы DWH.IFXBSHCP для выгрузки счетов доходов-расходов из BARS GL в Midas", null, errorMsg);
                }
            } catch (Throwable e) {
                auditController.error(MidasPLReplication, "Ошибка при выгрузке счетов доходов/расходов", null, e);
            }
            return null;
        }));

    }

    private Date getExecuteDate(Properties properties) throws ParseException {
        String propday = null != properties ? properties.getProperty(DWH_UNLOAD_FULL_DATE_KEY) : null;
        Date operday;
        if (isEmpty(propday) || CurrentOperday.CURRENT.name().equalsIgnoreCase(propday)) {
            operday = operdayController.getOperday().getCurrentDate();
        } else if (CurrentOperday.PREVIOUS.name().equalsIgnoreCase(propday)) {
            operday = operdayController.getOperday().getCurrentDate();
        } else {
            operday = DateUtils.parseDate(propday, "dd.MM.yyyy", "yyyy-MM-dd");
        }
        return operday;
    }

    private boolean executeRPG(String errorMsg, Connection connection) {
        //всё хорошо
        auditController.info(MidasPLReplication, errorMsg);
        try (CallableStatement st2 = connection.prepareCall("CALL DWH.IFXBSSER")) {
            auditController.info(MidasPLReplication, "Запуск процедуры репликации DWH.IFXBSSER");
            st2.execute();
            auditController.info(MidasPLReplication, "Окончание работы процедуры репликации DWH.IFXBSSER");
        }catch (Exception e){
            auditController.error(MidasPLReplication, "Ошибка выгрузки счетов доходов/расходов из BARS GL в Midas [DWH.IFXBSSER]", null, e);
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "select SESTS, SEMSG from DWH.ifxbssep " +
                        " where sestrtime = (select max(sestrtime) from dwh.ifxbssep)"); ResultSet res = statement.executeQuery()){
            //Проверка результата работы IFXBSSER
            if (res.next()) {
                String SESTS = res.getString(1);
                String SEMSG =  res.getString(2);
                if  (SESTS.equals("E")) {
                    auditController.error(MidasPLReplication, "Ошибка выгрузки счетов доходов/расходов из BARS GL в Midas [DWH.IFXBSSER]", null, "SEMSG = " + SEMSG);
                } else {
                    auditController.info(MidasPLReplication, "Result proc IFXBSSER SESTS = " + SESTS + "; SEMSG = " + SEMSG);
                }
            } else {
                auditController.error(MidasPLReplication, "Не удалось получить статус выполнения процедуры репликации (SELECT FROM IFXBSSEP IS EMPTY)", null, "");
                return false;
            }
        }catch(Exception ee){
            auditController.error(MidasPLReplication, "select from ifxbssep Error ", null, ee);
            return false;
        }
        return true;
    }


}
