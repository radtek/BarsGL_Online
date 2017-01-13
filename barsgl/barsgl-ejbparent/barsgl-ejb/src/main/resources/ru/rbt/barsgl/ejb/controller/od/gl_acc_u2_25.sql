select  u.bsaacid bsaacid,
        u.id uid,
        u.date_upl,
        g.id glacid
from gl_acc_u2 u
join gl_acc g on g.bsaacid=u.bsaacid and (g.rlntype is null or g.rlntype != 2)
where u.unf='N'
    and not exists (select 1 from gl_dwhparm p where p.acod=g.acod and p.sq=g.sq)