select d.id, d.mo_no, d.glo_ref, d.pod, d.bsaacid from gl_pd d
 where exists (select 1 from pcid_mo m where d.pod = m.pod and d.mo_no = m.mo_no)
   and d.id between ? and ?