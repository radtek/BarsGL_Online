SELECT B.*, CHAR(DAT) || ' ' || B.BSAACID || ' ' || value(B.ACID, 'NULL') ID
  FROM BALTUR B
 WHERE B.ACID = ? AND B.BSAACID = ?
   AND (DATTO = ? OR DAT >= ?)
 ORDER BY DAT