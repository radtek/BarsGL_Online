select *
  from gl_ndsopr o, gl_accnds r
 where o.tr_acc = r.tr_acc
   and not exists (select 1 from gl_ndspst p where o.idpd = p.evt_id)