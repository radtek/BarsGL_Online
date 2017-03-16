select * from dwh.gl_sched;


INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('EtlStructureMonitorTask', '', 'Мониторинг входящих сообщений  АЕ', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.EtlStructureMonitorTask', 10, null, 'hour=7-19,22-5;minute=*;second=*/15');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('CloseLastWorkdayBalanceTask', null, 'Закрытие баланса предыдущего операционного дня', 'STOPPED', 'MANUAL', 'SINGLE', 'ru.rbt.barsgl.ejb.controller.operday.task.CloseLastWorkdayBalanceTask', 10, null, null);
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('LoadCurratesTask', '', 'Загрузка курсов валют', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.LoadCurratesTask', 10, null, 'hour=2-7;minute=*/10');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('ReconcilationTask', null, 'Реконсиляция', 'STOPPED', 'MANUAL', 'SINGLE', 'ru.rbt.barsgl.ejb.controller.operday.task.ReconcilationTask', 10, null, null);
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('AccountOpenServiceTask', '', 'Сервис открытия счетов', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.AccountOpenServiceTask', 10, null, 'hour=9-23;minute=*;second=*/10');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('ExecutePreCOBTask', 'timeLoadBefore=23:59
#chronology=true', 'Перевод операционного дня в состояние PRE_COB', 'STOPPED', 'MANUAL', 'SINGLE', 'ru.rbt.barsgl.ejb.controller.operday.task.ExecutePreCOBTask', 10, null, 'hour=1;minute=*/10');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('DwhUnloadFull', '#checkRun=false
#operday=17.06.2016', 'Полная выгрузка счетов и проводок в DWH за день', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadFullTask', 10, null, 'hour=1-5;minute=*/10');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('AccoutPaintTask', null, 'Раскраска счетов', 'STOPPED', 'MANUAL', 'SINGLE', 'ru.rbt.barsgl.ejb.controller.operday.task.AccoutPaintTask', 10, null, null);
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('AccountBalanceSharedUnloadTask', '#checkRun=false', 'Выгрузка остатков по совместным счетам', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.balsep.AccountBalanceSharedUnloadTask', 10, null, 'hour=2-9;minute=*/10');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('AccountBalanceRegisteredUnloadTask', '#checkRun=false', 'Выгрузка остатков по GL счетам', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.balsep.AccountBalanceRegisteredUnloadTask', 10, null, 'hour=1-5;minute=*/10');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('AccountBalanceUnloadThree', '#checkRun=false
stepCheck=P14', 'Выгрузка переоцененных остатков', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.balsep.AccountBalanceUnloadThree', 10, null, 'hour=7-13;minute=*/10');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('MidasSubAccountsUpdateTask', 'operday=CURRENT', 'Выгрузка счетов доходов-расходов из BARS GL в Midas', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.MidasSubAccountsUpdateTask', 10, null, 'hour=20');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('StamtUnloadFull', '#operday=10.03.2016
#checkRun=false', 'Полная выгрузка в STAMT за день', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.stamt.StamtUnloadFullTask', 10, null, 'hour=1-5;minute=*/5');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('StamtUnloadDelta', '#checkRun=false', 'Выгрузка в STAMT проводок back-value за день', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.stamt.StamtUnloadDeltaTask', 10, null, 'hour=1-5;minute=*/5');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('UniAccountBalanceUnloadTask', 'operday=24.05.2016', 'Выгрузка остатков по запросу', 'STOPPED', 'MANUAL', 'SINGLE', 'ru.rbt.barsgl.ejb.controller.operday.task.dem.UniAccountBalanceUnloadTask', 10, null, null);
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('StamtUnloadBalanceTask', '', 'Выгрузка остатков в STAMT', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.stamt.StamtUnloadBalanceTask', 10, null, 'hour=1-5;minute=*/5');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('RecalcBS2Task', '#checkRun=true', 'Пересчет остатков по БС2', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.RecalcBS2Task', 10, null, 'hour=1-5;minute=30,59');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('ReprocessWtacOparationsTask', '#checkRun=true
stepName=MI2GL', 'Повторная обработка операций в статусе WTAC', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.ReprocessWtacOparationsTask', 10, null, 'hour=9-16;minute=*/10');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('UnloadUnspentsToDwhTask', '#minDay=2016-01-01
#maxDay=2016-06-21
#checkRun=false', 'Выгрузка остатков в DWH', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.UnloadUnspentsToDWHServiceTask', 10, null, 'hour=7-13;minute=*/30');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('PdSyncTask', null, 'Синхронизация проводок/оборотов в режиме BUFFER', 'STOPPED', 'MANUAL', 'SINGLE', 'ru.rbt.barsgl.ejb.controller.operday.task.PdSyncTask', 10, null, null);
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('EtlStructureMonitorTask2', '', 'Мониторинг входящих сообщений  АЕ 20:00-20:30', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.EtlStructureMonitorTask', 10, null, 'hour=20;minute=0-25;second=*/15');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('EtlStructureMonitorTask3', '', 'Мониторинг входящих сообщений  АЕ 21:30-22:00', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.EtlStructureMonitorTask', 10, null, 'hour=21;minute=30-59;second=*/15');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('OpenOperdayTask', 'flexStepName=IFLEX
#pdMode=BUFFER
pdMode=DIRECT', 'Открытие следующего операционного дня', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.OpenOperdayTask', 0, null, 'hour=7');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('ReplicateManualTask', null, 'Репликации после выходных', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.ReplicateManualTask', 0, null, 'hour=6');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('RemoteOpersTask', '', 'Удалённые операции для выгрузки в  DWH', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.RemoteOpersTask', 0, null, 'hour=14');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ( 'SRVACCTester', 'mq.type = queue
mq.host = vs529
mq.port = 1414
mq.queueManager = QM_MBROKER4_T5
mq.channel = SYSTEM.DEF.SVRCONN
mq.batchSize = 30
mq.topics = LIRQ:UCBRU.ADP.BARSGL.ACLIQU.REQUEST:UCBRU.ADP.BARSGL.ACLIQU.RESPONSE;BALIRQ:UCBRU.ADP.BARSGL.ACBALIQU.REQUEST:UCBRU.ADP.BARSGL.ACBALIQU.RESPONSE;MAPBRQ:UCBRU.ADP.BARSGL.MAACPOBAQU.REQUEST:UCBRU.ADP.BARSGL.MAACPOBAQU.RESPONSE
mq.user=srvwbl4mqtest
mq.password=UsATi8hU', 'SRVACCTester', 'STOPPED', 'MANUAL', 'SINGLE', 'ru.rbt.barsgl.ejb.controller.operday.task.SRVACCTester', 0, 0, '');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('AccountQueryTaskLIRQ1', 'mq.type = queue
mq.host = vs529
mq.port = 1414
mq.queueManager = QM_MBROKER4_T5
mq.channel = SYSTEM.DEF.SVRCONN
mq.batchSize = 50
mq.topics = LIRQ:UCBRU.ADP.BARSGL.ACLIQU.REQUEST:UCBRU.ADP.BARSGL.ACLIQU.RESPONSE
mq.user=srvwbl4mqtest
mq.password=UsATi8hU
unspents=show', 'Интеграция BARS GL с AccountListQuery и AccountBalanceListQuery LIRQ ', 'STARTED', 'AUTO', 'INTERVAL', 'ru.rbt.barsgl.ejb.controller.operday.task.AccountQueryTaskMT', 0, 50, 'second=*/1;minute=*;hour=*');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('AccountQueryTaskBALIRQ1', 'mq.type = queue
mq.host = vs529
mq.port = 1414
mq.queueManager = QM_MBROKER4_T5
mq.channel = SYSTEM.DEF.SVRCONN
mq.batchSize = 50
mq.topics = BALIRQ:UCBRU.ADP.BARSGL.ACBALIQU.REQUEST:UCBRU.ADP.BARSGL.ACBALIQU.RESPONSE
mq.user=srvwbl4mqtest
mq.password=UsATi8hU', 'Интеграция BARS GL с AccountListQuery и AccountBalanceListQuery BALIRQ', 'STOPPED', 'AUTO', 'INTERVAL', 'ru.rbt.barsgl.ejb.controller.operday.task.AccountQueryTaskMT', 0, 50, 'second=*/1;minute=*;hour=*');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('AccountQueryTaskMAPBRQ1', 'mq.type = queue
mq.host = vs529
mq.port = 1414
mq.queueManager = QM_MBROKER4_T5
mq.channel = SYSTEM.DEF.SVRCONN
mq.batchSize = 50
mq.topics = MAPBRQ:UCBRU.ADP.BARSGL.MAACPOBAQU.REQUEST:UCBRU.ADP.BARSGL.MAACPOBAQU.RESPONSE
mq.user=srvwbl4mqtest
mq.password=UsATi8hU', 'Интеграция BARS GL с AccountListQuery и AccountBalanceListQuery MAPBRQ', 'STARTED', 'AUTO', 'INTERVAL', 'ru.rbt.barsgl.ejb.controller.operday.task.AccountQueryTaskMT', 0, 50, 'second=*/1;minute=*;hour=*');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('PreCobBatchPostingTask', null, 'Обработка запросов на операцию с неподтвержденной датой за текущий ОД, необработанных до конца дня', 'STOPPED', 'MANUAL', 'SINGLE', 'ru.rbt.barsgl.ejb.controller.operday.task.PreCobBatchPostingTask', 0, null, null);
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('StamtUnloadBalanceFlexTask', null, 'Выгрузка оборотов по лицевым счетам после загрузки FLEX', 'ERROR', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.stamt.StamtUnloadBalanceFlexTask', 10, null, 'hour=6-16;minute=*/10');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('SyncStamtBackvalueTask', 'stepName=WT_1', 'Cинхронизация backvalue c BUFFER если день открыт в режиме BUFFER, инкрем выгрузка в STAMT', 'STOPPED', 'MANUAL', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.stamt.SyncStamtBackvalueTask', 0, null, 'hour=5-18;minute=*/5');

DELETE FROM dwh.GL_PRPRP;
INSERT INTO dwh.GL_PRPRP(ID_PRP, ID_PRN, REQUIRED, PRPTP, DESCRP, DECIMAL_VALUE, STRING_VALUE, NUMBER_VALUE) VALUES ('pd.cuncurency', 'root', 'Y', 'NUMBER_TYPE', 'Кол. одновр. потоков при обоаботке проводок', null, null, 5);
INSERT INTO dwh.GL_PRPRP(ID_PRP, ID_PRN, REQUIRED, PRPTP, DESCRP, DECIMAL_VALUE, STRING_VALUE, NUMBER_VALUE) VALUES ('etlpkg.process.count', 'root', 'Y', 'NUMBER_TYPE', 'Кол-во пакетов обрабатываемых за раз', null, null, 2);
INSERT INTO dwh.GL_PRPRP(ID_PRP, ID_PRN, REQUIRED, PRPTP, DESCRP, DECIMAL_VALUE, STRING_VALUE, NUMBER_VALUE) VALUES ('auth.ldapURI', 'auth', 'Y', 'STRING_TYPE', 'LDAP для внешних пользователей', null, 'ldap://172.17.145.105:389', null);
INSERT INTO dwh.GL_PRPRP(ID_PRP, ID_PRN, REQUIRED, PRPTP, DESCRP, DECIMAL_VALUE, STRING_VALUE, NUMBER_VALUE) VALUES ('auth.dname.regexps.1', 'auth.dname.regexps', 'Y', 'STRING_TYPE', 'regexp1->функция', null, '[eE][rR]\d*=GL_AU_DNAME_ER', null);
INSERT INTO dwh.GL_PRPRP(ID_PRP, ID_PRN, REQUIRED, PRPTP, DESCRP, DECIMAL_VALUE, STRING_VALUE, NUMBER_VALUE) VALUES ('auth.dname.regexps.2', 'auth.dname.regexps', 'Y', 'STRING_TYPE', 'regexp2->функция', null, '[mM][bB]\d*=GL_AU_DNAME_MB', null);
INSERT INTO dwh.GL_PRPRP(ID_PRP, ID_PRN, REQUIRED, PRPTP, DESCRP, DECIMAL_VALUE, STRING_VALUE, NUMBER_VALUE) VALUES ('auth.dname.regexps.3', 'auth.dname.regexps', 'Y', 'STRING_TYPE', 'regexp3->функция', null, '\\w*=GL_AU_NAME_LIT8', null);
INSERT INTO dwh.GL_PRPRP(ID_PRP, ID_PRN, REQUIRED, PRPTP, DESCRP, DECIMAL_VALUE, STRING_VALUE, NUMBER_VALUE) VALUES ('fan.concurency', 'root', 'Y', 'NUMBER_TYPE', 'Кол-во конкур. потоков для вееров', null, null, 5);
INSERT INTO dwh.GL_PRPRP(ID_PRP, ID_PRN, REQUIRED, PRPTP, DESCRP, DECIMAL_VALUE, STRING_VALUE, NUMBER_VALUE) VALUES ('fan.batchsize', 'root', 'Y', 'NUMBER_TYPE', 'Кол-во вееров обраб за раз', null, null, 100);
INSERT INTO dwh.GL_PRPRP(ID_PRP, ID_PRN, REQUIRED, PRPTP, DESCRP, DECIMAL_VALUE, STRING_VALUE, NUMBER_VALUE) VALUES (
'mc.queues.param','root','N','STRING_TYPE','Настройки очереди',null,'mq.host = vs529
mq.port = 1414
mq.queueManager = QM_MBROKER4_T5
mq.channel = SYSTEM.DEF.SVRCONN
mq.queue.inc = UCBRU.ADP.BARSGL.SCASA.MOCR.RESPONSE
mq.queue.out = UCBRU.ADP.BARSGL.SCASA.MOCR.REQUEST
mq.user=srvwbl4mqtest
mq.password=UsATi8hU',null);

