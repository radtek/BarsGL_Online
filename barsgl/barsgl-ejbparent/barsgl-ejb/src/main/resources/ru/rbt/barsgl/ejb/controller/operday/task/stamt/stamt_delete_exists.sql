DELETE FROM GL_BALSTMD B
 WHERE EXISTS (SELECT 1 FROM SESSION.GL_TMP_STMTBAL T
                WHERE T.CBACCOUNT = B.CBACCOUNT AND B.STATDATE = T.STATDATE)