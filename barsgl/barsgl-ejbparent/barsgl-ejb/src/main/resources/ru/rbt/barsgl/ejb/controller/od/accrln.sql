select ac.acid
          ,ac.bsaacid BSAACID
          ,cmp.ccpcd cbcc
          ,ac.CBCCN cbccn
          ,case when ac.acid<>'' then substr(ac.acid,18,3)
                    else BRP.A8BRCD
           end BRANCH
--          ,CUR.GLCCY ccy
          ,AC.CCY
          ,case when ac.acid<>'' then ac.CUSTNO
                    else BRP.A8BICN
           end CUSTNO
          ,AC.CBCUSTTYPE CBCUSTTYPE
          ,AC.ACC2
          ,AC.PLCODE
          ,case when AC.ACID <>'' then cast(substr (AC.ACID, 16,2)  AS NUMERIC(2, 0))
                    else 1
           end sq
          ,AC.PSAV
          ,AC.dto
          ,case when AC.DTC = to_date('2029-01-01', 'yyyy-mm-dd') then null
                else AC.DTC
           end dtc
          ,AC.RLNTYPE
          ,(select GLCCY from CURRENCY where CBCCY=REV.CBCCY) REV_CCY
from gl_acc ac
join reval706n rev on ac.bsaacid = rev.bsaacid
join imbcbcmp cmp on cmp.ccbbr = ac.CBCCN
--join CURRENCY cur on cur.CBCCY=AC.CBCCY
--join CURRENCY cur on cur.GLCCY=AC.CCY
left join IMBCBBRP brp on BRP.BCBBR=AC.CBCCN AND BRP.BR_HEAD='Y'
---------------------------------------
union
select ac.acid
          ,ac.bsaacid
          ,cmp.ccpcd cbcc
          ,ac.cbccn cbccn
          ,case when ac.acid<>'' then substr(ac.acid,18,3)
                    else BRP.A8BRCD
           end BRANCH
--          ,CUR.GLCCY ccy
          ,AC.CCY
          ,case when ac.acid<>'' then ac.CUSTNO
                    else BRP.A8BICN
           end CUSTNO
          ,AC.CBCUSTTYPE CBCUSTTYPE
          ,AC.ACC2
          ,AC.PLCODE
          ,case when AC.ACID <>'' then cast(substr (AC.ACID, 16,2)  AS NUMERIC(2, 0))
                    else 1
           end sq
          ,AC.PSAV
          ,AC.DTO dto
          ,AC.DTC
          ,AC.RLNTYPE
          ,'' REV_CCY
from gl_acc ac
join imbcbcmp cmp on cmp.ccbbr = ac.CBCCN
--join CURRENCY cur on cur.CBCCY=AC.CBCCY
--join CURRENCY cur on cur.GLCCY=AC.CCY
left join IMBCBBRP brp on BRP.BCBBR=AC.CBCCN AND BRP.BR_HEAD='Y'
where ac.acc2 in ('99996','99997','99998','99999')
    and ac.acid=''
    and ac.RLNTYPE = 'T'
---------------------------------------
union
select ac.acid
          ,ac.bsaacid
          ,cmp.ccpcd cbcc
          ,ac.cbccn cbccn
          ,case when ac.acid<>'' then substr(ac.acid,18,3)
                    else BRP.A8BRCD
           end BRANCH
--          ,CUR.GLCCY ccy
          ,AC.CCY
          ,case when ac.acid<>'' then ac.CUSTNO
                    else BRP.A8BICN
           end CUSTNO
          ,AC.CBCUSTTYPE CBCUSTTYPE
          ,AC.ACC2
          ,AC.PLCODE
          ,case when AC.ACID <>'' then cast(substr (AC.ACID, 16,2)  AS NUMERIC(2, 0))
                    else 1
           end sq
          ,AC.PSAV
          ,AC.dto
          ,AC.DTC
          ,AC.RLNTYPE
          ,'' REV_CCY
from gl_acc ac
join revaliflex x on x.acid=ac.acid and ac.bsaacid = x.bsaacid
join imbcbcmp cmp on cmp.ccbbr = ac.CBCCN
--join CURRENCY cur on cur.CBCCY=AC.CBCCY
--join CURRENCY cur on cur.GLCCY=AC.CCY
left join IMBCBBRP brp on BRP.BCBBR=AC.CBCCN AND BRP.BR_HEAD='Y'
;