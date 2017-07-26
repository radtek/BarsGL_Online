select t.PDID
        ,t.pcid
        ,CASE  WHEN W.ACOD IS NULL and a.rev_fl='Y' AND VALUE(A.RLNTYPE,'0') <> '2' THEN A.ID
               ELSE NULL END GLACID
        ,nvl( A.CUSTNO,  nvl(SUBSTRING(a.ACID, 1, 8),SUBSTRING(t.ACID, 1, 8) )) CNUM
        ,T.CCY
        ,nvl( A.ACOD, CAST (SUBSTRING(t.ACID, 12, 4) AS NUMERIC(4, 0))) ACOD
        ,nvl( A.SQ, CAST (SUBSTRING(t.ACID, 16, 2) AS NUMERIC(4, 0))) ACSQ
        ,CAST (PSTA AS NUMERIC(13, 0)) PSTA
        ,t.PSTARUR
        ,CAST (DRCR AS NUMERIC(1, 0)) DRCR
        ,nvl(a.BRANCH, substr(t.acid,18,3)) BRCA
        ,CAST (OTRF  AS VARCHAR(20)) OTRF
        ,PSTB PSTB
        ,t.BSAACID
        ,t.POD
        ,t.ACID PACID
        ,t.JACID
        ,nvl(a.rev_fl,'') rev_fl
        ,a.BSAACID absaacid
        ,rv.date_upl
        ,t.pdpod
from( select j.id PDID
            ,j.pcid
            ,D.ACID ACID
            ,D.CCY CCY
            ,ABS(j.AMNT) PSTA
            ,ABS(j.AMNTBC) PSTARUR
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
            ,E.PREF OTRF
            ,j.POD PSTB
            ,j.BSAACID BSAACID
            ,j.pod pod
            ,j.acid jacid
            ,d.pod pdpod
from gl_pdjchgr j
join pd d on d.id = j.id
left join PDEXT E on d.id = e.id
left join gl_acc a2 on a2.bsaacid = d.bsaacid
left join GL_SHACOD s on  S.ACOD = SUBSTRING(d.ACID, 12, 4) AND S.ACSQ = SUBSTRING(d.ACID, 16, 2)
where j.operday = ?
   and j.UNF != 'Y'
   and j.id > ?
   and j.pod < ?
   and j.chseq = nvl((select min(j2.chseq) from gl_pdjchgr j2 where j2.id=j.id),j.chseq)
   and ( ( a2.bsaacid is not null and (A2.RLNTYPE <> '2' or A2.RLNTYPE is null)) or 
                      (S.ACOD is not null and (d.ACID is not null or  d.ACID<>'' or d.ACID is not null) ) )
) t
left join gl_acc A ON t.BSAACID = A.BSAACID
left join gl_rvacul rv on rv.BSAACID = t.BSAACID
LEFT JOIN GL_DWHPARM W ON W.ACOD = A.ACOD AND W.SQ = A.SQ
order by t.pcid, t.pdid