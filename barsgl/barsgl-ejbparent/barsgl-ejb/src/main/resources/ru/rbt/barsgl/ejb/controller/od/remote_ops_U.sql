SELECT  PDID                                                      -- PD.ID
        , CASE                                                     -- только для неисключаемых счетов
            WHEN W.ACOD IS NULL AND VALUE(A.RLNTYPE,'0') <> '2' THEN A.ID ELSE NULL
          END GLACID
        , SUBSTRING(T.ACID, 1, 8) CNUM                            -- Номер клиента
        , T.CNUM_asoc
        , T.CCY                                                   -- Валюта
        , CAST (SUBSTRING(T.ACID, 12, 4) AS NUMERIC(4, 0)) ACOD
        , CAST (SUBSTRING(T.ACID, 16, 2) AS NUMERIC(2, 0)) ACSQ
        , CAST (PSTA AS NUMERIC(13, 0)) PSTA                      -- Сумма
        , PSTARUR
        , CAST (DRCR AS NUMERIC(1, 0)) DRCR                       -- 0 - Дебет, 1 - Кредит
        , CAST (SUBSTRING(T.ACID, 18, 3) AS CHARACTER(3)) BRCA    -- Бранч
        , CAST (PREF AS VARCHAR(20)) PREF                         -- ID платежа
        , CAST (DLREF AS VARCHAR(20)) DLREF                       -- ID сделки
        , CAST (OTRF  AS VARCHAR(20)) OTRF                        -- ID в источнике
        , PSTB PSTB                                               -- Posting date DECIMAL(5, 0)
        , PROCDATE VALB
        , PCID
        , t.BSAACID
        , EVT_ID
        , FCHNG
        , PRFCNTR
        , EVTP
        , PNAR
        , SPOS
        , dpmt
        , RNARLNG
        , mo_no
        , fan
        , fan_ccy
        , fan_amt
FROM (
        SELECT  D.ID PDID,                  -- PCID, ID проводки
            D.ACID ACID,    				-- счет Midas
--            case when (select 1 from imbcbbrp i where i.a8bicn=SUBSTRING(D.ACID, 3, 6)) is null then SUBSTRING(D.ACID, 3, 6)
--                 else  (select case when d2.acid=''or d2.acid is null then SUBSTRING(D.ACID, 3, 6)
--                                    else SUBSTRING(D2.ACID, 3, 6) end
--                        from pd d2 where d2.pcid=d.pcid and d2.id!=d.id)
--                 end CNUM_asoc,
            '000000' CNUM_asoc,
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
            O.PROCDATE PROCDATE,
            O.GLOID GLOID,
            ABS(D.AMNTBC) PSTARUR,
            P.PCID PCID,
            e5.EVT_ID,
            e5.FCHNG,
            e5.PRFCNTR,
            e5.EVTP,
            d.PNAR,
            d.PBR SPOS,
            e2.RNARLNG,
            mo.mo_no,
            o.fan,
            case when o.fan = 'Y' then
                 (select case when d.amnt >0 then
                                 case  when cast(decimal(d.amnt)/integer(power(10,cc.nbdp)) as decimal(22,2)) = o2.amt_cr then o2.ccy_dr
                                 else null end
                         else
                                 case when abs(cast(decimal(d.amnt)/integer(power(10,cc.nbdp)) as decimal(22,2)) ) = o2.amt_dr then o2.ccy_cr
                                 else null end
                         end
                  from gl_oper o2 where o2.gloid=e5.glo_ref)
                 else null end fan_ccy,
            case when o.fan = 'Y' then
                   (select case when d.amnt>0 then
                                 case  when cast(decimal(d.amnt)/integer(power(10,cc.nbdp)) as decimal(22,2)) = o3.amt_cr then o3.amt_dr
                                 else null end
                         else
                                 case when abs(cast(decimal(d.amnt)/integer(power(10,cc.nbdp)) as decimal(22,2)) ) = o3.amt_dr then o3.amt_cr
                                 else null end
                         end
                   from gl_oper o3 where o3.gloid=e5.glo_ref)
                 else null end fan_amt
        FROM GL_POSTING P
            JOIN GL_OPER O ON P.GLO_REF = O.GLOID
            JOIN PD D ON P.PCID = D.PCID
            JOIN PDEXT E ON D.ID = E.ID
            JOIN PDEXT5 E5 ON D.ID = E5.ID
            JOIN PDEXT2 E2 ON D.ID = E2.ID
            JOIN pcid_mo mo on mo.pcid = p.pcid
            left join CURRENCY CC on d.CCY=CC.GLCCY
        WHERE VALUE(ACID, '') <> '' AND D.INVISIBLE <> '1'  -- Проводки актуальны
            and d.id = ?
    ) T
    LEFT JOIN GL_ACC A ON T.BSAACID = A.BSAACID
    LEFT JOIN GL_DWHPARM W ON W.ACOD = A.ACOD AND W.SQ = A.SQ