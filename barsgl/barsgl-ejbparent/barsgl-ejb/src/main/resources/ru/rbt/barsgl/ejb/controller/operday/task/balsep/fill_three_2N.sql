select c.dat, bb.acid,
   sum(case
     when bb.dat = c.dat then bb.obac
     when bb.dat < c.dat then bb.obac + bb.dtac + bb.ctac
   end) obal,
   sum(case
     when bb.dat = c.dat then bb.obbc
     when bb.dat < c.dat then bb.obbc + bb.dtbc + bb.ctbc
   end) obalrur,
   sum(case
     when bb.dat = c.dat then abs(bb.dtac)
     when bb.dat < c.dat then 0
   end) dtrn,
   sum(case
     when bb.dat = c.dat then abs(bb.dtbc)
     when bb.dat < c.dat then 0
   end) dtrnrur,
   sum(case
     when bb.dat = c.dat then abs(bb.ctac)
     when bb.dat < c.dat then 0
   end) ctrn,
   sum(case
     when bb.dat = c.dat then abs(bb.ctbc)
     when bb.dat < c.dat then 0
   end) ctrnrur
  from baltur bb, cal c, accrln r
where bb.acid = r.acid and bb.bsaacid = r.bsaacid
   and bb.acid = ?
   and c.dat between ? and  ?
   and c.dat between bb.dat and bb.datto
   and c.hol <> 'X' and c.ccy = 'RUR'
   and r.rlntype in ('0','1','2')
group by c.dat, bb.acid
