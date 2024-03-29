insert into TMP_PCID_DEL (
    select pcid, postingdate, valuedate, pid, bsaacid, hostsystem, dealid, psid, amount, ccy
      from (
        select distinct t.pcid, t.pod postingdate, t.vald valuedate, t.id pid, t.bsaacid, o.src_pst hostsystem
               , cast(case when o.src_pst = 'PH' then o.chnl_name else o.deal_id end as varchar(128)) dealid
               , coalesce(o.pmt_ref, o.evt_id) psid
               , cast(t.amnt/cast(power(10,c.nbdp) as number(4)) as decimal(22,2)) amount
               , c.glccy ccy, row_number() over (partition by t.id order by chseq) rn
          from gl_pdjchg t
           join gl_posting p on p.pcid = t.pcid
           join gl_oper o on  o.gloid = p.glo_ref
           join currency c on c.cbccy = substr(t.bsaacid, 6,3)
          left join pst r on r.id = t.id
         where t.operday in (?1, ?2)
           and (r.invisible ='1' or r.invisible is null)
    ) v1 where rn = 1
    union all
    select distinct t.pcid, t.pod postingdate, t.vdate valuedate, t.idpd pid, t.bsaacid, 'BSG_IB' hostsystem
           , cast(null as varchar2(20)) dealid, cast(t.pcid as varchar2(40)) psid
           , cast(t.amnt/cast(power(10,c.nbdp) as number(4)) as decimal(22,2)) amount
           , c.glccy ccy
      from gl_pdjover t
           join currency c on c.cbccy = substr(t.bsaacid, 6,3)
          left join pst r on r.id = t.idpd
         where t.operday in (?1, ?2)
           and (r.invisible ='1' or r.invisible is null)
)
