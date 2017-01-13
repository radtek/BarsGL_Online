select * from gl_etlrate r
 where r.ON_DATE = (select max(r0.ON_DATE)
                     from gl_etlrate r0 where r0.ON_DATE > ? and r0.on_date <= ?)