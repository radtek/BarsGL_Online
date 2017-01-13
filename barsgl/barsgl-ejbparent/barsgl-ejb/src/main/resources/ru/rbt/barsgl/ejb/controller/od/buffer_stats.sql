select sum(pd_cnt) pd_cnt, sum(bal_cnt) bal_cnt
  from (
        select 0 pd_cnt, count(1) bal_cnt from gl_baltur b where b.moved <> 'Y'
        union all
        select count(1) pd_cnt, 0 bal_cnt from gl_pd where pd_id is null
) v