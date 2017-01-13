-- кол-во проводок backvalue
select count(1) cnt
  from gl_pd p
 where p.pod < ?