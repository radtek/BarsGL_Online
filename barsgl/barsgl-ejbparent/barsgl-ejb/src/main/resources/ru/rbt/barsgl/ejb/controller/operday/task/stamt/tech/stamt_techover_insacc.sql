insert into tmp_gl_balacc (acid,bsaacid,baldate)
select acid, bsaacid, min(pod) pod
  from gl_pdjover o
 where o.operday >= ? and o.unf = 'N'
group by acid, bsaacid