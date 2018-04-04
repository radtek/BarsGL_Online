select d.id idpd, d.bsaacid, d.amnt, d2.docn, d.pnar, d.rnarlng
  from pst d
 where d.pod = ?
   and d.pbr like '@@IF%'
   and (d.bsaacid, d.acid) in (select bsaacid, acid
                                 from gl_acc a, gl_accnds n where a.bsaacid = n.tr_acc)
   and not exists (select 1 from gl_ndsopr o where o.idpd = d.id)
   and d.amnt > 0