select t.PDID
        ,t.pcid
        ,CASE  WHEN W.ACOD IS NULL AND NVL(A.RLNTYPE,'0') <> '2' THEN A.ID ELSE NULL  END GLACID
        ,SUBSTR(T.ACID, 1, 8) CNUM
        ,T.CCY
        ,case when T.ACID !='' then CAST (SUBSTR(T.ACID, 12, 4) AS NUMERIC(4, 0)) else null end ACOD
        ,case when T.ACID !='' then CAST (SUBSTR(T.ACID, 16, 2) AS NUMERIC(2, 0)) else null end ACSQ
        ,CAST (PSTA AS NUMERIC(13, 0)) PSTA
        ,t.PSTARUR
        ,CAST (DRCR AS NUMERIC(1, 0)) DRCR
        ,CAST (SUBSTR(T.ACID, 18, 3) AS CHAR(3)) BRCA
        ,CAST (PREF AS VARCHAR(20)) PREF
        ,CAST (DLREF AS VARCHAR(20)) DLREF
        ,CAST (OTRF  AS VARCHAR(20)) OTRF
        ,PSTB PSTB
        ,PROCDATE VALB
        ,t.BSAACID
        ,t.POD
        ,t.ACID PACID
        ,t.JACID
from( select j.id PDID
            ,j.pcid
            ,D.ACID ACID
            ,D.CCY CCY
            ,ABS(D.AMNT) PSTA
            ,ABS(D.AMNTBC) PSTARUR
            ,COALESCE (
                   CASE
                     WHEN D.AMNT > 0 THEN 1
                     WHEN D.AMNT < 0 THEN 0
                   END,
                   CASE
                     WHEN D.AMNTBC > 0 THEN 1
                     WHEN D.AMNTBC < 0 THEN 0
                   END
                 ) DRCR
            ,e5.PMT_REF PREF
            ,op.SUBDEALID DLREF
            ,E.PREF OTRF
            ,j.POD PSTB
            ,Op.PROCDATE PROCDATE
            ,j.BSAACID BSAACID
            ,j.pod pod
            ,j.acid jacid
from gl_pdjchg j
      ,gl_od od
      ,gl_oper op
      ,gl_posting po
      ,pd d
      ,PDEXT E
      ,PDEXT5 E5
where (j.operday = od.curdate or j.operday = od.lwdate)
   and j.UNF != 'Y'
   and op.procdate < od.curdate
   and po.glo_ref = op.gloid
   and j.pcid = po.pcid
   and j.chseq = NVL((select min(j2.chseq) from gl_pdjchg j2 where j2.id=j.id),j.chseq)
   and d.id = j.id
   and d.id = e.id
   and d.id = e5.id
) t
left join gl_acc A ON t.BSAACID = A.BSAACID
LEFT JOIN GL_DWHPARM W ON W.ACOD = A.ACOD AND W.SQ = A.SQ
order by t.pcid, t.pdid