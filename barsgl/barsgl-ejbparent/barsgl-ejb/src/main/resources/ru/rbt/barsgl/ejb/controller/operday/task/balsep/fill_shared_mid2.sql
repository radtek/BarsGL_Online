select b.acid, substr(b.acid, 9, 3) ccy
       , sum(
         case
           when b.dat = od.curdate then b.obac
           when b.dat < od.curdate then b.obac + b.dtac + b.ctac
           else null
         end
       ) obal
       , sum(
         case
           when b.dat = od.curdate then b.obbc
           when b.dat < od.curdate then b.obbc + b.dtbc + b.ctbc
           else null
         end
       ) obalrur
       , sum(
         case
           when b.dat = od.curdate then abs(b.dtac)
           else null
         end
       ) dtrn
       , sum(
         case
           when b.dat = od.curdate then abs(b.dtbc)
           else null
         end
       ) dtrnrur
       , sum(
         case
           when b.dat = od.curdate then abs(b.ctac)
           else null
         end
       ) ctrn
       , sum(
         case
           when b.dat = od.curdate then abs(b.ctbc)
           else null
         end
       ) ctrnrur
  from baltur b, accrln r, (select value(?, date('2029-01-01')) curdate from sysibm.sysdummy1) od
 where b.acid = r.acid
   and r.acid = ?
   and r.rlntype = ?
   and r.drlnc = date('2029-01-01')
   and b.datto = date('2029-01-01')
  group by b.acid