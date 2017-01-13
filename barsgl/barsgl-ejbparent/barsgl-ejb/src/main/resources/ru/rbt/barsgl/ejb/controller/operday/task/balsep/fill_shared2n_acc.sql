select bb.acid,
   sum(case
     when bb.dat = v.dat then bb.obac
     when bb.dat < v.dat then bb.obac + bb.dtac + bb.ctac
   end) obal,
   sum(case
     when bb.dat = v.dat then bb.obbc
     when bb.dat < v.dat then bb.obbc + bb.dtbc + bb.ctbc
   end) obalrur,
   sum(case
     when bb.dat = v.dat then abs(bb.dtac)
     when bb.dat < v.dat then 0
   end) dtrn,
   sum(case
     when bb.dat = v.dat then abs(bb.dtbc)
     when bb.dat < v.dat then 0
   end) dtrnrur,
   sum(case
     when bb.dat = v.dat then abs(bb.ctac)
     when bb.dat < v.dat then 0
   end) ctrn,
   sum(case
     when bb.dat = v.dat then abs(bb.ctbc)
     when bb.dat < v.dat then 0
   end) ctrnrur
  from baltur bb, accrln a, (select date('$1') dat from sysibm.sysdummy1) v
where bb.acid = a.acid and bb.bsaacid = a.bsaacid
  and v.dat between bb.dat and bb.datto
  and a.rlntype in ('0', '1', '2') and a.acid = ?
 group by bb.acid