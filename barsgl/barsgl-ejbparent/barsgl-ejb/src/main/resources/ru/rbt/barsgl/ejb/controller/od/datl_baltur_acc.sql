SELECT B.*, TO_CHAR(DAT) || ' ' || B.BSAACID || ' ' || COALESCE(B.ACID, 'NULL') ID
                                         FROM BALTUR B
                                        WHERE B.ACID = ? AND B.BSAACID = ?
                                          AND (DATTO = ? OR DAT >= ?)
                                        ORDER BY DAT