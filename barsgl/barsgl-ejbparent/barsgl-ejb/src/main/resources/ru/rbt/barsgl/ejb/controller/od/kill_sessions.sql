DECLARE
    PRAGMA AUTONOMOUS_TRANSACTION;

    A_TABLENAME VARCHAR2(30) := ?;
    L_KILLED NUMBER(2) := 0;
BEGIN
    FOR NN IN (SELECT S.SID, S.SERIAL# FROM GV$LOCKED_OBJECT O, V$SESSION S
                    WHERE O.OBJECT_ID = (SELECT OBJECT_ID FROM DBA_OBJECTS
                                         WHERE OWNER = USER AND OBJECT_TYPE = 'TABLE' AND OBJECT_NAME = A_TABLENAME)
                      AND O.SESSION_ID = S.SID
                      AND O.SESSION_ID <> SYS_CONTEXT('USERENV','SID')) LOOP
            BEGIN
                SYSTEM.KILL_SESSION(NN.SID, NN.SERIAL#);
                PKG_SYS_UTL.LOG_AUDIT_WARN('KillSession', 'Session marked for deletion. SID:'||TO_CHAR(NN.SID)||' SERIAL:'||NN.SERIAL#, SQLERRM, DBMS_UTILITY.FORMAT_ERROR_STACK());
                L_KILLED := L_KILLED + 1;
            EXCEPTION
                WHEN OTHERS THEN
                    PKG_SYS_UTL.LOG_AUDIT_ERROR('KillSession', 'Error while deleting session SID:'||TO_CHAR(NN.SID)||' SERIAL:'||NN.SERIAL#, SQLERRM, DBMS_UTILITY.FORMAT_ERROR_STACK());
            END;
    END LOOP;
    COMMIT;
    ? := L_KILLED;
END;