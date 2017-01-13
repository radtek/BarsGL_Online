SELECT  PDID                                                      -- PD.ID
        , CASE                                                     -- только для неисключаемых счетов
            WHEN W.ACOD IS NULL AND VALUE(A.RLNTYPE,'0') <> '2' THEN A.ID ELSE NULL
          END GLACID
--        , SUBSTRING(T.ACID, 1, 8) CNUM                            -- Номер клиента
        , VALUE( A.CUSTNO,  value(SUBSTRING(a.ACID, 1, 8),SUBSTRING(t.ACID, 1, 8) ))  CNUM
        , T.CNUM_asoc
        , T.CCY                                                   -- Валюта
--        , CAST (SUBSTRING(T.ACID, 12, 4) AS NUMERIC(4, 0)) ACOD
        , VALUE( A.ACOD, CAST (SUBSTRING(t.ACID, 12, 4) AS NUMERIC(4, 0))) ACOD
--        , CAST (SUBSTRING(T.ACID, 16, 2) AS NUMERIC(2, 0)) ACSQ
        , VALUE( A.SQ, CAST (SUBSTRING(t.ACID, 16, 2) AS NUMERIC(4, 0))) ACSQ
        , CAST (PSTA AS NUMERIC(13, 0)) PSTA                      -- Сумма
        , PSTARUR
        , CAST (DRCR AS NUMERIC(1, 0)) DRCR                       -- 0 - Дебет, 1 - Кредит
--        , CAST (SUBSTRING(T.ACID, 18, 3) AS CHARACTER(3)) BRCA    -- Бранч
        , value(a.BRANCH,substr(t.acid,18,3))  BRCA
        , CAST (PREF AS VARCHAR(20)) PREF                         -- ID платежа
        , CAST (DLREF AS VARCHAR(20)) DLREF                       -- ID сделки
        , CAST (OTRF  AS VARCHAR(20)) OTRF                        -- ID в источнике
        , PSTB PSTB                                               -- Posting date DECIMAL(5, 0)
--        , PROCDATE VALB
        , PCID
        , t.BSAACID
        , EVT_ID
        , FCHNG
        , PRFCNTR
--        , EVTP
        , PNAR
        , SPOS
        , dpmt
        , RNARLNG
        , value(a.rev_fl,'') rev_fl
        , a.BSAACID absaacid
        , rv.date_upl
        , t.pdpod
FROM (
        SELECT  D.ID PDID,                  -- PCID, ID проводки
            D.ACID ACID,    				-- счет Midas
            case when (select 1 from imbcbbrp i where i.a8bicn=SUBSTRING(D.ACID, 3, 6)) is null then SUBSTRING(D.ACID, 3, 6)
                 else  (select case when d2.acid=''or d2.acid is null then SUBSTRING(D.ACID, 3, 6)
                                    else SUBSTRING(D2.ACID, 3, 6) end
                        from pd d2 where d2.pcid=d.pcid and d2.id!=d.id)
                 end CNUM_asoc,
            D.BSAACID BSAACID,
            D.CCY CCY,          			-- валюта
            D.POD PSTB,	    			    -- дата проводки
            D.VALD VALD,    				-- дата валютирования
            ABS(D.AMNT) PSTA,  				-- сумма
            COALESCE (
                CASE
                    WHEN D.AMNT > 0 THEN 1
                    WHEN D.AMNT < 0 THEN 0
                END,
                CASE
                    WHEN D.AMNTBC > 0 THEN 1
                    WHEN D.AMNTBC < 0 THEN 0
                END
            ) DRCR,                         -- 0 - дебет, 1 - кредит
            E.DPMT DPMT,        			-- код департамента
            e5.pmt_ref PREF,     			-- ID платежа
            e5.subdealid DLREF,   			-- ID сделки
            E.PREF OTRF,        			-- ID источника
--            O.PROCDATE PROCDATE,
--            O.GLOID GLOID,
            ABS(D.AMNTBC) PSTARUR,
            d.PCID PCID,
            e5.EVT_ID,
            e5.FCHNG,
            e5.PRFCNTR,
--            o.EVTP,
            d.PNAR,
            d.PBR SPOS,
            d.pod pdpod,
            e2.RNARLNG
        FROM PD D
            left JOIN PDEXT E ON D.ID = E.ID
            left JOIN PDEXT5 E5 ON D.ID = E5.ID
            left JOIN PDEXT2 E2 ON D.ID = E2.ID
            left join gl_acc a2 on a2.bsaacid = d.bsaacid
            left join GL_SHACOD s on  S.ACOD = SUBSTRING(d.ACID, 12, 4) AND S.ACSQ = SUBSTRING(d.ACID, 16, 2)
        WHERE D.INVISIBLE <> '1'  -- Проводки актуальны
           and d.id = ?
--            and VALUE(d.ACID, '') <> ''
            and ( ( a2.bsaacid is not null and (A2.RLNTYPE <> '2' or A2.RLNTYPE is null)) or 
                      (S.ACOD is not null and (d.ACID is not null or  d.ACID<>'' or d.ACID is not null) ) )
    ) T
    LEFT JOIN GL_ACC A ON T.BSAACID = A.BSAACID
    left join gl_rvacul rv on rv.BSAACID = t.BSAACID
    LEFT JOIN GL_DWHPARM W ON W.ACOD = A.ACOD AND W.SQ = A.SQ