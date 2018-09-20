with glued as
  (select
        ndog, acid, sum(amnt) flag,
        sum(case when amnt<0 then amnt else 0 end) sum_dr,
        sum(case when amnt>0 then amnt else 0 end) sum_cr,
        sum(case when amnt<0 then 1 else 0 end) cnt_dr,
        sum(case when amnt>0 then 1 else 0 end) cnt_cr,
        listagg (id,',') within group (order by pd_id) id_reg,
        listagg (case when amnt<0 then pbr else null end,',') within group (order by pd_id) pbr_dr,
        listagg (case when amnt>0 then pbr else null end,',') within group (order by pd_id) pbr_cr,
        listagg (case when amnt<0 then pcid else null end,',') within group (order by pd_id) pcid_dr,
        listagg (case when amnt>0 then pcid else null end,',') within group (order by pd_id) pcid_cr,
        listagg (case when amnt<0 then pd_id else null end,',') within group (order by pd_id) pdid_dr,
        listagg (case when amnt>0 then pd_id else null end,',') within group (order by pd_id) pdid_cr,
        listagg (case when amnt<0 then pmt_ref else null end,',') within group (order by pmt_ref) pmt_dr,
        listagg (case when amnt>0 then pmt_ref else null end,',') within group (order by pmt_ref) pmt_cr,
        listagg (case when amnt<0 then glo_ref else null end,',') within group (order by glo_ref) glo_dr,
        listagg (case when amnt>0 then glo_ref else null end,',') within group (order by glo_ref) glo_cr,
        listagg (case when amnt<0 then pod else null end,',') within group (order by pd_id) pod_dr,
        listagg (case when amnt>0 then pod else null end,',') within group (order by pd_id) pod_cr
        $fld_pod$
  from GL_REG47422
    where  valid = 'Y'
        and state in ($state_list$)
        and pod > ?
  group by acid, ndog $fld_pod$ $fld_sum$
  )
select * from glued
where flag=0 and (cnt_cr=1 or cnt_dr=1)
order by cnt_cr, cnt_dr $fld_pod$, ndog