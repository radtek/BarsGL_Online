package ru.rbt.barsgl.ejb.etc;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.shared.enums.Repository;

import javax.ejb.EJB;
import java.sql.Statement;

import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.AS400runner;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by ER22228 on 05.10.2016.
 * <p>
 * Возможность синхронного и асинхронного вызова из BARSGL Online java на стороне AS400
 * Тест и пример запуска AS400ProcedureRunnerTest.java
 */

public class AS400ProcedureRunner {
    private static Logger log = Logger.getLogger(AS400ProcedureRunner.class);

    @EJB
    private CoreRepository repository;
    @EJB
    protected AuditController auditController;

    /**
     * синхронный вызов процедуры на AS400 - ждёт завершения работы
     *
     * @param folderName путь к jar файлу. Например, если реальный путь /Root/GCP, то указывать /GCP
     * @param jarName    имя jar файла|ов для classpath, через ":", без полного пути, если jar лежит в folderName. Например async.jar
     * @param className  полное имя класса с путем. Например ru.rb.test.Example
     * @param params     массив параметров. Например "new Object[]{"один", 2, 45.67}"
     */
    public void callSynhro(String folderName, String jarName, String className, Object[] params) {
        try {
            repository.executeInNonTransaction(connection -> {
                Statement stm = connection.createStatement();
                String cmd;
//                auditController.info(AS400runner, "Начало callSynhro");
                if (!isEmpty(folderName)) {
                    cmd = "CALL DWH.QCMDEXC2 ('cd ''" + folderName + "''')";
//                    auditController.info(AS400runner, "cd cmd "+cmd );
                    stm.execute(cmd);
                }

                cmd = "CALL DWH.QCMDEXC2 ('RUNJVA CLASS(" + className + ") " + getParamsAsStringDouble(params)
                        + " CLASSPATH(''" + jarName + "'') OUTPUT(* *CONTINUE) ') ";
//                auditController.info(AS400runner, "run cmd "+cmd );
                stm.execute(cmd);
                stm.close();
//                auditController.info(AS400runner, "Завершение callSynhro" );
                return null;
            });
        } catch (Exception ex) {
            log.error("", ex);
        }
    }

/*    public void callAsyncThread(String folderName, String jarName, String className, Object[] params) {
        Thread th = new Thread() {
            public void run() {
                callSynhro(folderName, jarName, className, params);
            }
        };

        th.start();
    }*/

    /**
     * асинхронный вызов процедуры на AS400 - не ждёт завершения работы
     *
     * @ param folderName путь к jar файлу. Например, если реальный путь /Root/GCP, то указывать /GCP
     * @param jarName    имя jar файла|ов для classpath, через ":", без полного пути, если jar лежит в folderName. Например async.jar
     * @param className  полное имя класса с путем. Например ru.rb.test.Example
     * @param params     массив параметров. Например "new Object[]{"один", 2, 45.67}"
     */
    public void callAsyncGl(String jarName, String className, Object[] params) {
        callAsync(Repository.BARSGL, jarName, className, params);
    }

    public void callAsyncRep(String jarName, String className, Object[] params) {
        callAsync(Repository.BARSREP, jarName, className, params);
    }

    public void callAsync(Repository enumRepository, String jarName, String className, Object[] params) {
        try {
            String data = "RUNJVA CLASS(" + className + ") " + getParamsAsString(params)
                    + " CLASSPATH('/GCP:/GCP/log4j-1.2.7.jar:/GCP/jt400.jar:"
                    + jarName + "') PROP((log4j.configuration 'logger.properties')) OUTPUT(* *CONTINUE)";

            repository.executeNativeUpdate(repository.getPersistence(enumRepository), "DELETE FROM EXCHDATA");
            // Формируем и записываем полный текст команды в таблицу,
            // чтобы скрипт GL_RUNSTEP эту команду считал и выполнил
            repository.executeNativeUpdate(repository.getPersistence(enumRepository), "INSERT INTO EXCHDATA(DATA) VALUES(?)",new Object[]{data});

            // Вызываемый скрипт производит переход в каталог /GCP
            Boolean result = (Boolean)repository.executeInNonTransaction(connection -> {
                try(Statement stm = connection.createStatement();) {
//                    String cmd = "CALL DWH.QCMDEXC2 ('SBMJOB CMD(CALL PGM(DWH/GL_RUNSTEP)) JOBD(QGPL/BARSJOBD)')";
                    String cmd = "CALL DWH.QCMDEXC2 ('SBMJOB CMD(CALL PGM(DWH/GL_RUNSTEP)) JOBD(DWH/BARSJOBD)')";
//                    String cmd = "CALL DWH.QCMDEXC2 ('SBMJOB CMD(CALL PGM(DWH/GL_RUNSTEP)) JOBD(DWH/BARSJOBD) JOB(BARSLOAD)')";
                    return stm.execute(cmd);
                }
            }, enumRepository);

            auditController.info(AS400runner, "В " + enumRepository.getParamDesc() +" запущено: " + data);

        } catch (Exception ex) {
            log.error("Ошибка при работе с " + enumRepository.getParamDesc(), ex);
            auditController.warning(AS400runner, "ошибка callAsync при работе с " + enumRepository.getParamDesc(), null, ex );
        }
    }

    private String getParamsAsString(Object[] params) {
        String parms = "";
        for (Object item : params) {
            if (item instanceof String) {
                parms += " '" + item + "'";
            } else {
                parms += " " + String.valueOf(item);
            }
        }

        if (parms.length() > 0) {
            parms = "PARM(" + parms.substring(1) + ")";
        }
        return parms;
    }

    private String getParamsAsStringDouble(Object[] params) {
        String parms = "";
        for (Object item : params) {
            if (item instanceof String) {
                parms += " ''" + item + "''";
            } else {
                parms += " " + String.valueOf(item);
            }
        }

        if (parms.length() > 0) {
            parms = "PARM(" + parms.substring(1) + ")";
        }
        return parms;
    }
}
