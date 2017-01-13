select  min(u.date_upl) date_upl,
        ac.acid
from gl_acc_u2 u
join accrln ac on ac.bsaacid=u.bsaacid
                               and ac.rlntype in ('0','1','2')
                               and ac.drlnc='2029-01-01'
join gl_shacod s on s.acod = substr(ac.acid,12,4)
                                    and s.acsq=substr(ac.acid,16,2)
                                    and s.datto='2029-01-01'
where u.unf='N'
    and not exists (select 1 from gl_acc g where g.bsaacid=u.bsaacid)
group by ac.acid