select ac.acid
          ,ac.bsaacid BSAACID
          ,cmp.ccpcd cbcc
          ,ac.ccode cbccn
          ,case when ac.acid<>'' then substr(ac.acid,18,3)
                    else BRP.A8BRCD
           end BRANCH
--          ,case when ac.acid<>'' then cast (substr(ac.acid,18,3) AS NUMERIC(3, 0))
--                    else cast (BRP.A8BRCD  AS NUMERIC(3, 0))
--           end BRANCH
          ,CUR.GLCCY ccy
          ,case when ac.acid<>'' then ac.CNUM
                    else BRP.A8BICN
           end CUSTNO
          ,AC.CTYPE CBCUSTTYPE
          ,AC.ACC2
          ,AC.PLCODE
          ,case when AC.ACID <>'' then cast(substr (AC.ACID, 16,2)  AS NUMERIC(2, 0))
                    else 1
           end sq
          ,AC.PSAV
          ,AC.DRLNO dto
          ,case when AC.DRLNC = '2029-01-01' then null
                else AC.DRLNC
           end dtc
          ,AC.RLNTYPE
          ,(select GLCCY from CURRENCY where CBCCY=REV.CBCCY) REV_CCY
from accrln ac
join reval706n rev on ac.bsaacid = rev.bsaacid
join imbcbcmp cmp on cmp.ccbbr = ac.ccode
join CURRENCY cur on cur.CBCCY=AC.CBCCY
left join IMBCBBRP brp on BRP.BCBBR=AC.CCODE AND BRP.BR_HEAD='Y'
---------------------------------------
union
select ac.acid
          ,ac.bsaacid
          ,cmp.ccpcd cbcc
          ,ac.ccode cbccn
--          ,case when ac.acid<>'' then cast (substr(ac.acid,18,3) AS NUMERIC(3, 0))
--                    else cast (BRP.A8BRCD  AS NUMERIC(3, 0))
--           end BRANCH
          ,case when ac.acid<>'' then substr(ac.acid,18,3)
                    else BRP.A8BRCD
           end BRANCH
          ,CUR.GLCCY ccy
          ,case when ac.acid<>'' then ac.CNUM
                    else BRP.A8BICN
           end CUSTNO
          ,AC.CTYPE CBCUSTTYPE
          ,AC.ACC2
          ,AC.PLCODE
          ,case when AC.ACID <>'' then cast(substr (AC.ACID, 16,2)  AS NUMERIC(2, 0))
                    else 1
           end sq
          ,AC.PSAV
          ,AC.DRLNO dto
          ,AC.DRLNC
          ,AC.RLNTYPE
          ,'' REV_CCY
from accrln ac
join imbcbcmp cmp on cmp.ccbbr = ac.ccode
join CURRENCY cur on cur.CBCCY=AC.CBCCY
left join IMBCBBRP brp on BRP.BCBBR=AC.CCODE AND BRP.BR_HEAD='Y'
where ac.acc2 in ('99996','99997','99998','99999')
    and ac.acid=''
    and ac.RLNTYPE = 'T'
---------------------------------------
union
select ac.acid
          ,ac.bsaacid
          ,cmp.ccpcd cbcc
          ,ac.ccode cbccn
--          ,case when ac.acid<>'' then cast (substr(ac.acid,18,3) AS NUMERIC(3, 0))
--                    else cast (BRP.A8BRCD  AS NUMERIC(3, 0))
--           end BRANCH
          ,case when ac.acid<>'' then substr(ac.acid,18,3)
                    else BRP.A8BRCD
           end BRANCH
          ,CUR.GLCCY ccy
          ,case when ac.acid<>'' then ac.CNUM
                    else BRP.A8BICN
           end CUSTNO
          ,AC.CTYPE CBCUSTTYPE
          ,AC.ACC2
          ,AC.PLCODE
          ,case when AC.ACID <>'' then cast(substr (AC.ACID, 16,2)  AS NUMERIC(2, 0))
                    else 1
           end sq
          ,AC.PSAV
          ,AC.DRLNO dto
          ,AC.DRLNC
          ,AC.RLNTYPE
          ,'' REV_CCY
from accrln ac
join revaliflex x on x.acid=ac.acid and ac.bsaacid = x.bsaacid
join imbcbcmp cmp on cmp.ccbbr = ac.ccode
join CURRENCY cur on cur.CBCCY=AC.CBCCY
left join IMBCBBRP brp on BRP.BCBBR=AC.CCODE AND BRP.BR_HEAD='Y'