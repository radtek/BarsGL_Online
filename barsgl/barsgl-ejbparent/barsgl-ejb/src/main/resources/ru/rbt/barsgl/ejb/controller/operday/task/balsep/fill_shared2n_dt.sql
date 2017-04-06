select min(d.pod) min_pod, od.curdate max_pod, a.id acid
  from gl_oper o, (select value(?, date('2029-01-01')) curdate from DUAL) od, gl_posting ps, pd d,  gl_shacod s, acc a
 where ps.pcid = d.pcid
   and ps.glo_ref = o.gloid
   and o.procdate = od.curdate
   and s.acod = a.acod
   and s.acsq = right('00'||a.acsq, 2)
   and d.acid = a.id
   and s.bstype = '2N'
   and s.dat <= od.curdate and s.datto > od.curdate
   and d.invisible <> '1'
   and substr(a.id, 9, 3) = 'RUR'
  group by od.curdate, a.id