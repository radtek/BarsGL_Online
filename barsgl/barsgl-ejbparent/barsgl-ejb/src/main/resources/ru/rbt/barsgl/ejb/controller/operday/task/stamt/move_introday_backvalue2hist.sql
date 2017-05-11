insert into gl_etlstma (pcid, trycnt)
select distinct pcid, nvl(? + 1, 1)
  from gl_etlstmd d where not exists (select 1 from gl_etlstma a where a.pcid = d.pcid)