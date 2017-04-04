select * from dwh.gl_sched;


INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('EtlStructureMonitorTask', '', '���������� �������� ���������  ��', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.EtlStructureMonitorTask', 10, null, 'hour=7-19,22-5;minute=*;second=*/15');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('CloseLastWorkdayBalanceTask', null, '�������� ������� ����������� ������������� ���', 'STOPPED', 'MANUAL', 'SINGLE', 'ru.rbt.barsgl.ejb.controller.operday.task.CloseLastWorkdayBalanceTask', 10, null, null);
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('LoadCurratesTask', '', '�������� ������ �����', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.LoadCurratesTask', 10, null, 'hour=2-7;minute=*/10');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('ReconcilationTask', null, '������������', 'STOPPED', 'MANUAL', 'SINGLE', 'ru.rbt.barsgl.ejb.controller.operday.task.ReconcilationTask', 10, null, null);
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('AccountOpenServiceTask', '', '������ �������� ������', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.AccountOpenServiceTask', 10, null, 'hour=9-23;minute=*;second=*/10');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('ExecutePreCOBTask', 'timeLoadBefore=23:59
#chronology=true', '������� ������������� ��� � ��������� PRE_COB', 'STOPPED', 'MANUAL', 'SINGLE', 'ru.rbt.barsgl.ejb.controller.operday.task.ExecutePreCOBTask', 10, null, 'hour=1;minute=*/10');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('DwhUnloadFull', '#checkRun=false
#operday=17.06.2016', '������ �������� ������ � �������� � DWH �� ����', 'STOPPED', 'MANUAL', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadFullTask', 10, null, 'hour=1-5;minute=*/10');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('AccoutPaintTask', null, '��������� ������', 'STOPPED', 'MANUAL', 'SINGLE', 'ru.rbt.barsgl.ejb.controller.operday.task.AccoutPaintTask', 10, null, null);
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('AccountBalanceSharedUnloadTask', '#checkRun=false', '�������� �������� �� ���������� ������', 'STOPPED', 'MANUAL', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.balsep.AccountBalanceSharedUnloadTask', 10, null, 'hour=2-9;minute=*/10');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('AccountBalanceRegisteredUnloadTask', '#checkRun=false', '�������� �������� �� GL ������', 'STOPPED', 'MANUAL', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.balsep.AccountBalanceRegisteredUnloadTask', 10, null, 'hour=1-5;minute=*/10');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('AccountBalanceUnloadThree', '#checkRun=false
stepCheck=P14', '�������� ������������� ��������', 'STOPPED', 'MANUAL', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.balsep.AccountBalanceUnloadThree', 10, null, 'hour=7-13;minute=*/10');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('MidasSubAccountsUpdateTask', 'operday=CURRENT', '�������� ������ �������-�������� �� BARS GL � Midas', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.MidasSubAccountsUpdateTask', 10, null, 'hour=20');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('StamtUnloadFull', '#operday=10.03.2016
#checkRun=false', '������ �������� � STAMT �� ����', 'STOPPED', 'MANUAL', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.stamt.StamtUnloadFullTask', 10, null, 'hour=1-5;minute=*/5');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('StamtUnloadDelta', '#checkRun=false', '�������� � STAMT �������� back-value �� ����', 'STOPPED', 'MANUAL', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.stamt.StamtUnloadDeltaTask', 10, null, 'hour=1-5;minute=*/5');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('UniAccountBalanceUnloadTask', 'operday=24.05.2016', '�������� �������� �� �������', 'STOPPED', 'MANUAL', 'SINGLE', 'ru.rbt.barsgl.ejb.controller.operday.task.dem.UniAccountBalanceUnloadTask', 10, null, null);
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('StamtUnloadBalanceTask', '', '�������� �������� � STAMT', 'STOPPED', 'MANUAL', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.stamt.StamtUnloadBalanceTask', 10, null, 'hour=1-5;minute=*/5');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('RecalcBS2Task', '#checkRun=true', '�������� �������� �� ��2', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.RecalcBS2Task', 10, null, 'hour=1-5;minute=30,59');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('ReprocessWtacOparationsTask', '#checkRun=true
stepName=MI2GL', '��������� ��������� �������� � ������� WTAC', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.ReprocessWtacOparationsTask', 10, null, 'hour=9-16;minute=*/10');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('UnloadUnspentsToDwhTask', '#minDay=2016-01-01
#maxDay=2016-06-21
#checkRun=false', '�������� �������� � DWH', 'STOPPED', 'MANUAL', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.UnloadUnspentsToDWHServiceTask', 10, null, 'hour=7-13;minute=*/30');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('PdSyncTask', null, '������������� ��������/�������� � ������ BUFFER', 'STOPPED', 'MANUAL', 'SINGLE', 'ru.rbt.barsgl.ejb.controller.operday.task.PdSyncTask', 10, null, null);
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('EtlStructureMonitorTask2', '', '���������� �������� ���������  �� 20:00-20:30', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.EtlStructureMonitorTask', 10, null, 'hour=20;minute=0-25;second=*/15');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('EtlStructureMonitorTask3', '', '���������� �������� ���������  �� 21:30-22:00', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.EtlStructureMonitorTask', 10, null, 'hour=21;minute=30-59;second=*/15');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('OpenOperdayTask', 'flexStepName=IFLEX
#pdMode=BUFFER
pdMode=DIRECT', '�������� ���������� ������������� ���', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.OpenOperdayTask', 0, null, 'hour=7');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('ReplicateManualTask', null, '���������� ����� ��������', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.ReplicateManualTask', 0, null, 'hour=6');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('RemoteOpersTask', '', '�������� �������� ��� �������� �  DWH', 'STARTED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.RemoteOpersTask', 0, null, 'hour=14');

INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ( 'SRVACCTester', 'mq.type = queue
mq.host = vs569
mq.port = 1414
mq.queueManager = QM_MBROKER4_T4
mq.channel = SYSTEM.DEF.SVRCONN
mq.batchSize = 30
mq.topics = LIRQ:UCBRU.ADP.BARSGL.ACLIQU.REQUEST:UCBRU.ADP.BARSGL.ACLIQU.RESPONSE;BALIRQ:UCBRU.ADP.BARSGL.ACBALIQU.REQUEST:UCBRU.ADP.BARSGL.ACBALIQU.RESPONSE;MAPBRQ:UCBRU.ADP.BARSGL.MAACPOBAQU.REQUEST:UCBRU.ADP.BARSGL.MAACPOBAQU.RESPONSE
mq.user=srvwbl4mqtest
mq.password=UsATi8hU', 'SRVACCTester', 'STOPPED', 'MANUAL', 'SINGLE', 'ru.rbt.barsgl.ejb.controller.operday.task.SRVACCTester', 0, 0, '');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('AccountQueryTaskLIRQ1', 'mq.type = queue
mq.host = vs569
mq.port = 1414
mq.queueManager = QM_MBROKER4_T4
mq.channel = SYSTEM.DEF.SVRCONN
mq.batchSize = 50
mq.topics = LIRQ:UCBRU.ADP.BARSGL.ACLIQU.REQUEST:UCBRU.ADP.BARSGL.ACLIQU.RESPONSE
mq.user=srvwbl4mqtest
mq.password=UsATi8hU
unspents=show', '���������� BARS GL � AccountListQuery � AccountBalanceListQuery LIRQ ', 'STARTED', 'AUTO', 'INTERVAL', 'ru.rbt.barsgl.ejb.controller.operday.task.AccountQueryTaskMT', 0, 50, 'second=*/1;minute=*;hour=*');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('AccountQueryTaskBALIRQ1', 'mq.type = queue
mq.host = vs569
mq.port = 1414
mq.queueManager = QM_MBROKER4_T4
mq.channel = SYSTEM.DEF.SVRCONN
mq.batchSize = 50
mq.topics = BALIRQ:UCBRU.ADP.BARSGL.ACBALIQU.REQUEST:UCBRU.ADP.BARSGL.ACBALIQU.RESPONSE
mq.user=srvwbl4mqtest
mq.password=UsATi8hU', '���������� BARS GL � AccountListQuery � AccountBalanceListQuery BALIRQ', 'STARTED', 'AUTO', 'INTERVAL', 'ru.rbt.barsgl.ejb.controller.operday.task.AccountQueryTaskMT', 0, 50, 'second=*/1;minute=*;hour=*');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('AccountQueryTaskMAPBRQ1', 'mq.type = queue
mq.host = vs569
mq.port = 1414
mq.queueManager = QM_MBROKER4_T4
mq.channel = SYSTEM.DEF.SVRCONN
mq.batchSize = 50
mq.topics = MAPBRQ:UCBRU.ADP.BARSGL.MAACPOBAQU.REQUEST:UCBRU.ADP.BARSGL.MAACPOBAQU.RESPONSE
mq.user=srvwbl4mqtest
mq.password=UsATi8hU', '���������� BARS GL � AccountListQuery � AccountBalanceListQuery MAPBRQ', 'STARTED', 'AUTO', 'INTERVAL', 'ru.rbt.barsgl.ejb.controller.operday.task.AccountQueryTaskMT', 0, 50, 'second=*/1;minute=*;hour=*');



INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('PreCobBatchPostingTask', null, '��������� �������� �� �������� � ���������������� ����� �� ������� ��, �������������� �� ����� ���', 'STOPPED', 'MANUAL', 'SINGLE', 'ru.rbt.barsgl.ejb.controller.operday.task.PreCobBatchPostingTask', 0, null, null);
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('StamtUnloadBalanceFlexTask', null, '�������� �������� �� ������� ������ ����� �������� FLEX', 'STOPPED', 'MANUAL', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.stamt.StamtUnloadBalanceFlexTask', 10, null, 'hour=6-16;minute=*/10');
INSERT INTO DWH.GL_SCHED(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('SyncStamtBackvalueTask', 'stepName=WT_1', 'C������������ backvalue c BUFFER ���� ���� ������ � ������ BUFFER, ������ �������� � STAMT', 'STOPPED', 'MANUAL', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.stamt.SyncStamtBackvalueTask', 0, null, 'hour=5-18;minute=*/5');

DELETE FROM dwh.GL_PRPRP;
INSERT INTO dwh.GL_PRPRP(ID_PRP, ID_PRN, REQUIRED, PRPTP, DESCRP, DECIMAL_VALUE, STRING_VALUE, NUMBER_VALUE) VALUES ('pd.cuncurency', 'root', 'Y', 'NUMBER_TYPE', '���. ������. ������� ��� ��������� ��������', null, null, 5);
INSERT INTO dwh.GL_PRPRP(ID_PRP, ID_PRN, REQUIRED, PRPTP, DESCRP, DECIMAL_VALUE, STRING_VALUE, NUMBER_VALUE) VALUES ('etlpkg.process.count', 'root', 'Y', 'NUMBER_TYPE', '���-�� ������� �������������� �� ���', null, null, 2);
INSERT INTO dwh.GL_PRPRP(ID_PRP, ID_PRN, REQUIRED, PRPTP, DESCRP, DECIMAL_VALUE, STRING_VALUE, NUMBER_VALUE) VALUES ('auth.ldapURI', 'auth', 'Y', 'STRING_TYPE', 'LDAP ��� ������� �������������', null, 'ldap://172.17.145.105:389', null);
INSERT INTO dwh.GL_PRPRP(ID_PRP, ID_PRN, REQUIRED, PRPTP, DESCRP, DECIMAL_VALUE, STRING_VALUE, NUMBER_VALUE) VALUES ('fan.concurency', 'root', 'Y', 'NUMBER_TYPE', '���-�� ������. ������� ��� ������', null, null, 5);
INSERT INTO dwh.GL_PRPRP(ID_PRP, ID_PRN, REQUIRED, PRPTP, DESCRP, DECIMAL_VALUE, STRING_VALUE, NUMBER_VALUE) VALUES ('fan.batchsize', 'root', 'Y', 'NUMBER_TYPE', '���-�� ������ ����� �� ���', null, null, 100);
INSERT INTO dwh.GL_PRPRP(ID_PRP, ID_PRN, REQUIRED, PRPTP, DESCRP, DECIMAL_VALUE, STRING_VALUE, NUMBER_VALUE) VALUES (
'mc.queues.param','root','N','STRING_TYPE','��������� �������',null, 'mq.host=vs569
mq.port=1414
mq.queueManager=QM_MBROKER4_T4
mq.channel=SYSTEM.DEF.SVRCONN
mq.queue.inc = UCBRU.ADP.BARSGL.SCASA.MOCR.RESPONSE
mq.queue.out = UCBRU.ADP.BARSGL.SCASA.MOCR.REQUEST
mq.user=srvwbl4mqtest
mq.password=UsATi8hU',null);
