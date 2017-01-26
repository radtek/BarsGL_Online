insert into session.tmp_balacc (acid,bsaacid,baldate)
select distinct acid, bsaacid, pod
  from gl_pdjover o
 where o.operday = ? and o.unf = 'N'