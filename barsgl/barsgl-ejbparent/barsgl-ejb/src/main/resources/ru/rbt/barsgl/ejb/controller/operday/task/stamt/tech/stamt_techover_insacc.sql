insert into tmp_gl_balacc (acid,bsaacid,baldate)
select acid, v1.bsaacid, min(v1.postingdate) pd_min
 from (
    select r.bsaacid, acid, r.drlnc, row_number() over (partition by r.bsaacid order by r.drlnc desc) rn, postingdate
      from (
        select d.DCBACCOUNT bsaacid, d.POSTINGDATE
          from gl_etlstmd d
        union all
        select d.CCBACCOUNT bsaacid, d.POSTINGDATE
          from gl_etlstmd d
    ) v, gl_od o, accrln r
   where v.bsaacid = r.bsaacid
) v1
where rn = 1
group by v1.bsaacid, v1.acid
