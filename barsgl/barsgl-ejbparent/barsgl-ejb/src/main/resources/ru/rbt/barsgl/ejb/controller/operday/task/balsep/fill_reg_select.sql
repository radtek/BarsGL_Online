select c.dat cdat, b.dat bdat, b.acid, b.bsaacid, d.id, b.obac obal, b.dtac dtrn, b.ctac CTRN, d.curdate unload_dat, b.obbc obalrur, b.dtbc dtrnrur, b.ctbc ctrnrur
from baltur b, cal c,
  (select min(p.pod) min_pod, od.curdate max_pod, p.acid, p.bsaacid, acc.id, od.curdate
   from gl_acc acc, gl_oper o, gl_posting ps, pd p, gl_od od
   where o.gloid = ps.glo_ref
         and ps.pcid = p.pcid
         and p.invisible <> '1'
         and o.procdate = od.curdate
         and acc.acid = p.acid and acc.bsaacid = p.bsaacid
         and (value(acc.plcode, '') = '' or (value(acc.plcode, '') <> '' AND VALUE(REV_FL, 'N') <> 'Y')
                                            and acc.bsaacid not in (
     SELECT ACC.BSAACID
     FROM GL_SHACOD , GL_ACC ACC , GL_OD OD
     WHERE
       ACC.RLNTYPE='2' AND
       GL_SHACOD.ACOD = ACC.ACOD AND
       GL_SHACOD.ACSQ = ACC.SQ AND
       GL_SHACOD.BSTYPE IN ('2', '2N') AND
       GL_SHACOD.DAT <= OD.CURDATE AND
       GL_SHACOD.DATTO > OD.CURDATE
   )
         )
         and not exists (select 1 from gl_dwhparm w where acc.acod = w.acod and acc.sq = w.sq)
   group by p.acid, p.bsaacid, acc.id, od.curdate) d
where b.acid = d.acid and b.bsaacid = d.bsaacid
      and c.dat between b.dat and b.datto
      and c.dat between d.min_pod and d.max_pod and c.ccy = 'RUR' and c.hol <> 'X'

