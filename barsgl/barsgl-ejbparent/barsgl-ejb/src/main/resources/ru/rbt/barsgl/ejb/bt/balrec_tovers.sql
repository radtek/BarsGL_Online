SELECT D.ACID, D.BSAACID, D.POD
       , SUM(CASE
            WHEN D.AMNTBC > 0 THEN D.AMNTBC ELSE 0
       END) ctbc
       , SUM(CASE
            WHEN D.AMNTBC < 0 THEN D.AMNTBC ELSE 0
       END) dtbc
       , SUM(CASE
            WHEN D.AMNTBC > 0 THEN D.AMNT ELSE 0
       END) ctac
       , SUM(CASE
            WHEN D.AMNT < 0 THEN D.AMNT ELSE 0
       END) dtac
  FROM PD D
 WHERE D.POD in (SELECT DAT FROM CAL C
                  WHERE C.DAT BETWEEN ? AND (SELECT CURDATE FROM GL_OD)
                    AND C.HOL <> 'X' AND C.CCY = 'RUR')
   AND D.BSAACID = ? AND D.ACID = ? AND D.INVISIBLE <> '1'
 GROUP BY D.ACID, D.BSAACID,D.POD
 ORDER BY 3