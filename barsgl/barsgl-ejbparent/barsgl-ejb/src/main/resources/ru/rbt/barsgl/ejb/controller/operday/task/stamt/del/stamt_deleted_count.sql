select count(distinct pcid) cnt
  from (
        select t.pcid
          from gl_pdjchg t
          left join pd r on r.id = t.id
         where t.operday in  (?1, ?2)
           and (r.invisible ='1' or r.invisible is null)
           and GL_STMFILTER(t.bsaacid) = '1'
        union all
        select t.pcid
          from gl_pdjover t
          left join pd r on r.id = t.idpd
         where t.operday in  (?1, ?2)
           and (r.invisible ='1' or r.invisible is null)
           and GL_STMFILTER(t.bsaacid) = '1'
  ) v
  where not exists (select 1 from gl_stmdel d where d.pcid = v.pcid)