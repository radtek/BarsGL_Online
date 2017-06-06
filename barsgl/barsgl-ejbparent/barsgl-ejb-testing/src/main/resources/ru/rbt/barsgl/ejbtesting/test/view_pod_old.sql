declare
    PRAGMA AUTONOMOUS_TRANSACTION;
begin
    execute immediate 'CREATE OR REPLACE FORCE VIEW V_GL_OPER_POD (GLOID, POD_TYPE, POD) AS
                         SELECT GLOID
                              , POD_TYPE
                              , CASE WHEN POD_TYPE = ''HARD'' THEN POSTDATE ELSE CAST (NULL AS DATE) END POD
                       FROM (
                           SELECT GLOID
                                  , CASE
                                       WHEN OPER_CLASS = ''MANUAL''
                                           OR (OPER_CLASS = ''AUTOMATIC'' AND SRC_PST IN (''ARMPRO'', ''SECMOD'', ''IMEX'', ''AXAPTA'')) THEN ''HARD''
                                       ELSE ''STANDARD'' END POD_TYPE
                                  , case
                                       when OPER_CLASS = ''MANUAL'' then POSTDATE
                                       when OPER_CLASS = ''AUTOMATIC'' and SRC_PST IN (''ARMPRO'', ''SECMOD'', ''IMEX'', ''AXAPTA'') then
                                           case when VDATE < CC.MIN_DT then MIN_DT else VDATE end
                                    end POSTDATE
                             FROM GL_OPER O,
                                  (SELECT MIN(DAT) MIN_DT
                                     FROM CAL C
                                    WHERE C.HOL <> ''X'' AND CCY = ''RUR''
                                      AND C.DAT > (SELECT CURDATE - 14 FROM GL_OD)) CC
                       ) V';
    commit;
end;


