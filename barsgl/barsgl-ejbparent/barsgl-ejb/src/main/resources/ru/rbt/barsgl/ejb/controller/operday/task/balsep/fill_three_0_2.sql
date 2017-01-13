select m.dat, b.acid, b.bsaacid, m.glacid
       , case
           when b.dat = m.dat then b.obac
           when b.dat < m.dat then b.obac + b.dtac + b.ctac
       end obal
       , case
           when b.dat = m.dat then abs(b.dtac)
           when b.dat < m.dat then 0
       end dtrn
       , case
           when b.dat = m.dat then abs(ctac)
           when b.dat < m.dat then 0
       end ctrn
       , m.unload_dat
       , case
           when b.dat = m.dat then b.obbc
           when b.dat < m.dat then b.obbc + b.dtbc + b.ctbc
       end obalrur
       , case
           when b.dat = m.dat then abs(b.dtbc)
           when b.dat < m.dat then 0
       end dtrnrur
       , case when b.dat = m.dat then b.ctbc
           when b.dat < m.dat then 0
       end ctrnrur
 from glvd_bal2 m, baltur b
where m.acid = b.acid
  and m.bsaacid = b.bsaacid
  and m.dat between b.dat and b.datto
  and value(m.bsaacid,'') <> ''

