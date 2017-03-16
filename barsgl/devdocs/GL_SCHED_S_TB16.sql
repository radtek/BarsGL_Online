
INSERT INTO DWH.GL_SCHED_S(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('AccountQueryTaskLIRQ1', 'mq.type = queue
mq.host = vs712
mq.port = 1414
mq.queueManager = QM_MBROKER4
mq.channel = SYSTEM.DEF.SVRCONN
mq.batchSize = 10
mq.topics = LIRQ:UCBRU.ADP.BARSGL.ACLIQU.REQUEST:UCBRU.ADP.BARSGL.ACLIQU.RESPONSE
mq.user=srvwbl4mqtest
mq.password=UsATi8hU
unspents=show', 'Интеграция BARS GL с AccountListQuery и AccountBalanceListQuery LIRQ ', 'STOPPED', 'AUTO', 'INTERVAL', 'ru.rbt.barsgl.ejb.controller.operday.task.AccountQueryTaskMT', 0, 50, 'second=*/1;minute=*;hour=*');

INSERT INTO DWH.GL_SCHED_S(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('AccountQueryTaskBALIRQ1', 'mq.type = queue
mq.host = vs712
mq.port = 1414
mq.queueManager = QM_MBROKER4
mq.channel = SYSTEM.DEF.SVRCONN
mq.batchSize = 10
mq.topics = BALIRQ:UCBRU.ADP.BARSGL.ACBALIQU.REQUEST:UCBRU.ADP.BARSGL.ACBALIQU.RESPONSE
mq.user=srvwbl4mqtest
mq.password=UsATi8hU', 'Интеграция BARS GL с AccountListQuery и AccountBalanceListQuery BALIRQ', 'STOPPED', 'AUTO', 'INTERVAL', 'ru.rbt.barsgl.ejb.controller.operday.task.AccountQueryTaskMT', 0, 50, 'second=*/1;minute=*;hour=*');

INSERT INTO DWH.GL_SCHED_S(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('AccountQueryTaskMAPBRQ1', 'mq.type = queue
mq.host = vs712
mq.port = 1414
mq.queueManager = QM_MBROKER4
mq.channel = SYSTEM.DEF.SVRCONN
mq.batchSize = 10
mq.topics = MAPBRQ:UCBRU.ADP.BARSGL.MAACPOBAQU.REQUEST:UCBRU.ADP.BARSGL.MAACPOBAQU.RESPONSE
mq.user=srvwbl4mqtest
mq.password=UsATi8hU', 'Интеграция BARS GL с AccountListQuery и AccountBalanceListQuery MAPBRQ', 'STOPPED', 'AUTO', 'INTERVAL', 'ru.rbt.barsgl.ejb.controller.operday.task.AccountQueryTaskMT', 0, 50, 'second=*/1;minute=*;hour=*');

INSERT INTO DWH.GL_SCHED_S(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('AccountDetailsNotifyTask', 'mq.type = queue
mq.host = vs712
mq.port = 1414
mq.queueManager = QM_MBROKER4
mq.channel = SYSTEM.DEF.SVRCONN
mq.batchSize=-1
mq.topics=FCC:UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF.TEMP;MIDAS_OPEN:UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF
mq.user=srvwbl4mqtest
mq.password=UsATi8hU
unspents=show', 'Работа с сервисом открытия счетов SRVACC', 'STOPPED', 'AUTO', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.AccountDetailsNotifyTask', 0, 0, 'hour=*;minute=*/1');

INSERT INTO DWH.GL_SCHED_S(TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('AccountOpenServiceTask', '', 'Сервис открытия счетов', 'STOPPED', 'MANUAL', 'CALENDAR', 'ru.rbt.barsgl.ejb.controller.operday.task.AccountOpenServiceTask', 10, null, 'hour=9-23;minute=*;second=*/10');


