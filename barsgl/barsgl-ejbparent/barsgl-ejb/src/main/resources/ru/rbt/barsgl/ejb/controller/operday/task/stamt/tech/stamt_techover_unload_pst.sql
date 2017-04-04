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
    'Требования по прочим операциям' russhnar,                       -- описание
    j.ts statementdate, d.pod postingdate, d.vald valuedate,         -- даты
    'BSG-IB' hostsystem,
    cast(null as varchar(20)) dealid,
    d.pcid psid,                                                             -- источник, ид в системе
    da.bsaacnnum dclient_id, ca.bsaacnnum cclient_id,                        -- бранч майдас дебет
    dwh.get_fcc_br(d.bsaacid) dbranch_id, get_fcc_br(c.bsaacid) cbranch_id,        -- бранч майдас кредит
    d.id did, c.id cid, d.bsaacid dcbaccount, c.bsaacid ccbaccount,          -- ид и счета полупроводок
    d.ccy dcur, c.ccy ccur,                                                  -- валюта
    -decimal(d.amnt)/integer(power(10, dc.nbdp)) damount, decimal(c.amnt)/integer(power(10, cc.nbdp)) camount,     -- суммы в валюте
    -decimal(d.amntbc)/integer(power(10,2)) damount_rur,  decimal(c.amntbc)/integer(power(10, 2)) camount_rur,     -- суммы в рублях
    m.bo_ind doc_type, m.mo_no doc_n,                                                                              -- мемордер
    cast(null as bigint) glo_ref,
    '1' post_type,
    cast(null as varchar(20)) evtp,
    d.pcid nrt
 from (select j.pcid, min(j.ts) ts from gl_pdjover j
        where j.operday >= ?
          and j.chfl in ('I', 'U') and j.unf = 'N'
         group by j.pcid
      ) j
     join pd d on j.pcid = d.pcid and d.amntbc < 0
     join pd c on j.pcid = c.pcid and c.amntbc > 0
     join pcid_mo m on d.pcid = m.pcid
     join currency dc on dc.glccy = d.ccy
     join currency cc on cc.glccy = c.ccy
     join bsaacc da on d.bsaacid = da.id
     join bsaacc ca on c.bsaacid = ca.id
 where d.invisible <> '1' and c.invisible <> '1' -- проводки актуальны
  and (GL_STMFILTER(d.bsaacid) = '1' or GL_STMFILTER(c.bsaacid) = '1')
  and not exists (select 1 from gl_etlstma a where a.pcid = d.pcid)