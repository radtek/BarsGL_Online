select min(v.pod) min_pod, v.max_pod, v.acid
  from (
    select d.pod, od.curdate max_pod, a.id acid, d.bsaacid,
           case
                when s.bstype = '0' then 1
                when substr(a.id, 9, 3) = 'RUR' and s.bstype = '2' then 1
                else 0
           end cur_flag
      from gl_oper o, (select value(?, date('2029-01-01')) curdate from DUAL) od, gl_posting ps, pd d,  gl_shacod s, acc a
     where ps.pcid = d.pcid
       and ps.glo_ref = o.gloid
       and o.procdate = od.curdate
       and s.acod = a.acod
       and s.acsq = right('0'||a.acsq, 2)
       and d.acid = a.id
       and s.bstype in ('0', '2')
       and s.dat <= od.curdate and s.datto > od.curdate
       and d.invisible <> '1'
) v
group by v.max_pod, v.acid