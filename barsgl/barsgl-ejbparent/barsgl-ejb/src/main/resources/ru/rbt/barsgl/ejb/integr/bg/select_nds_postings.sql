select d.id idpd, d.bsaacid, d.amnt, d2.docn, d.pnar, d2.rnarlng
  from pd d, pdext2 d2
 where d.id = d2.id and d.pod = ?
   and d.pbr like '@@IF%'
   and (d.bsaacid, d.acid) in (select bsaacid, acid
                                 from accrln a, gl_accnds n where a.bsaacid = n.tr_acc)
   and not exists (select 1 from gl_ndsopr o where o.idpd = d.id)
   and d.amnt > 0