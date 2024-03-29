insert into gl_etlstmd (
    pcid, russhnar,                                                  -- pcid проводки, описание
    statementdate, postingdate, valuedate,                           -- даты
    hostsystem, dealid, psid,                                        -- источник, ид в системе
    dclient_id, cclient_id,                                          -- клиенты
    dbranch_id, cbranch_id,                                          -- бранчи флекс
    did, cid, dcbaccount, ccbaccount,                                -- ид и счета полупроводок
    dcur, ccur, damount, camount, damount_rur, camount_rur,          -- валюта, суммы в валюте, суммы в рублях
    doc_type, doc_n,                                                 -- мемордер
    glo_ref, post_type,
    evtp, narrative
)
 select  d.pcid pcid,                                                -- pcid проводки
     coalesce(d.rnarlng, 'Требования по прочим операциям') russhnar,                       -- описание
     j.ts statementdate, d.pod postingdate, d.vald valuedate,         -- даты
     'BSG-IB' hostsystem,
     cast(null as varchar2(20)) dealid,
     d.pcid psid,                                                             -- источник, ид в системе
     trim(da.CUSTNO) dclient_id, trim(ca.CUSTNO) cclient_id,                        -- бранч майдас дебет
     get_fcc_br(d.bsaacid) dbranch_id, get_fcc_br(c.bsaacid) cbranch_id,        -- бранч майдас кредит
     d.id did, c.id cid, d.bsaacid dcbaccount, c.bsaacid ccbaccount,          -- ид и счета полупроводок
     d.ccy dcur, c.ccy ccur,                                                  -- валюта
     -(d.amnt)/cast(power(10, dc.nbdp) as number(4)) damount, (c.amnt)/cast(power(10, cc.nbdp) as number(4)) camount,     -- суммы в валюте
     -(d.amntbc)/cast(power(10,2) as number(4)) damount_rur,  (c.amntbc)/cast(power(10, 2) as number(4)) camount_rur,     -- суммы в рублях
     nvl(m.bo_ind, 0) doc_type, nvl(m.mo_no, 'BA' || nvl(get_fcc_br(d.bsaacid), '000') || substr(cast (d.pcid as varchar2(16)), -9)) doc_n, -- мемордер
     cast(null as number(10)) glo_ref,
     '1' post_type,
     cast(null as varchar2(20)) evtp,
     d.pcid nrt
 from (select j.pcid, min(j.ts) ts from gl_pdjover j
        where j.operday >= ?
          and j.chfl in ('I', 'U') and j.unf = 'N'
         group by j.pcid
      ) j
     join pst d on j.pcid = d.pcid and d.amntbc < 0
     join pst c on j.pcid = c.pcid and c.amntbc > 0
     left join pcid_mo m on d.pcid = m.pcid
     join currency dc on dc.glccy = d.ccy
     join currency cc on cc.glccy = c.ccy
     join gl_acc da on d.bsaacid = da.bsaacid
     join gl_acc ca on c.bsaacid = ca.bsaacid
     --left join pdext2 pde2 on pde2.id = d.id
 where d.invisible <> '1' and c.invisible <> '1' -- проводки актуальны
  and (GL_STMFILTER(d.bsaacid) = '1' and GL_STMFILTER(c.bsaacid) = '1')
  and not exists (select 1 from gl_etlstma a where a.pcid = d.pcid)