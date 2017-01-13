select t.PDID
        ,t.pcid
        ,CASE  WHEN W.ACOD IS NULL AND VALUE(A.RLNTYPE,'0') <> '2' THEN A.ID ELSE NULL  END GLACID
        ,SUBSTRING(T.ACID, 1, 8) CNUM
        ,T.CCY
        ,CAST (SUBSTRING(T.ACID, 12, 4) AS NUMERIC(4, 0)) ACOD
        ,CAST (SUBSTRING(T.ACID, 16, 2) AS NUMERIC(2, 0)) ACSQ
        ,CAST (PSTA AS NUMERIC(13, 0)) PSTA
        ,t.PSTARUR
        ,CAST (DRCR AS NUMERIC(1, 0)) DRCR
        ,CAST (SUBSTRING(T.ACID, 18, 3) AS CHARACTER(3)) BRCA
        ,CAST (PREF AS VARCHAR(20)) PREF
        ,CAST (DLREF AS VARCHAR(20)) DLREF
        ,CAST (OTRF  AS VARCHAR(20)) OTRF
        ,PSTB PSTB
        ,PROCDATE VALB
        ,t.BSAACID aBSAACID
        ,t.POD
        ,t.ACID pacid
        ,'' jacid
        ,rv.date_upl
        ,value(a.rev_fl,'') rev_fl
FROM (
        SELECT  D.ID PDID,
            D.ACID ACID,
            D.BSAACID BSAACID,
            D.CCY CCY,
            D.VALD VALD,
            D.PNAR PNAR,
            ABS(D.AMNT) PSTA,
            COALESCE (
                CASE
                    WHEN D.AMNT > 0 THEN 1
                    WHEN D.AMNT < 0 THEN 0
                END,
                CASE
                    WHEN D.AMNTBC > 0 THEN 1
                    WHEN D.AMNTBC < 0 THEN 0
                END
            ) DRCR,
            D.ASOC ASOC,
            D.PBR SPOS,
            E.DPMT DPMT,
            e.PREF PREF,
            '' DLREF,
            E.PREF OTRF,
            D.POD PSTB,
            D.POD PROCDATE,
            ABS(D.AMNTBC) PSTARUR,
            d.PCID PCID,
            d.POD
        FROM PD D
        LEFT JOIN PDEXT E on D.ID = E.ID
       WHERE d.PCID = ?
            and d.id != ?
    ) T
    LEFT JOIN GL_ACC A ON T.BSAACID = A.BSAACID
    LEFT JOIN GL_DWHPARM W ON W.ACOD = A.ACOD AND W.SQ = A.SQ
    left join gl_rvacul rv on rv.BSAACID = t.BSAACID