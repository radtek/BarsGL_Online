select v.acid
       , case
           when s.bstype = '0' then '0'
           when s.bstype = '2' then '2'
           when s.bstype = '2N' then '2'
         end rlntype
       , sum(case when v.drcr = 0 then v.psta else 0 end) trnv_drmid
       , sum(case when v.drcr = 1 then v.psta else 0 end) trnv_crmid
  from gl_shacod s, acc a, (select value(?, date('2029-01-01')) curdate from DUAL) od,
      (
         select right('00000000'||e.cnum, 8)||e.ccy||e.acod||right('00'||e.acsq, 2)||e.brca acid, e.*
           from M10MMDWH.EODPOPD e
          where UPPER(e.spos) NOT LIKE 'TECH%'
            and UPPER(e.spos) NOT LIKE 'FINR%'
            and e.ccy = 'RUR'
      ) v
 where s.acod = a.acod
   and s.acsq = right('0'||a.acsq, 2)
   and v.acid = a.id
   and s.dat <= od.curdate and s.datto > od.curdate
 group by v.acid, s.bstype