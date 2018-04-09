insert into GL_ETLSTMD (
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
 select  p.pcid pcid,                                                -- pcid проводки
    substr(o.rnrtl,1,255) russhnar,                                                -- описание
    o.ots statementdate, d.POD postingdate, o.vdate valuedate,  -- даты
    o.src_pst hostsystem, -- o.deal_id dealid,
    case
        when o.src_pst ='PH'  then o.chnl_name
        else o.deal_id
    end  dealid,
    nvl(o.pmt_ref, o.evt_id) psid,                                                             -- источник, ид в системе
    trim(da.CUSTNO) dclient_id, trim(ca.CUSTNO) cclient_id,                                            -- бранч майдас дебет
    GET_FCC_BR(d.bsaacid) dbranch_id, get_fcc_br(c.bsaacid) cbranch_id,                            -- бранч майдас кредит
    d.id did, c.id cid, d.bsaacid dcbaccount, c.bsaacid ccbaccount,                              -- ид и счета полупроводок
    d.ccy dcur, c.ccy ccur,                                                                      -- валюта
    -d.amnt/power(10, dc.nbdp) damount, c.amnt/power(10, cc.nbdp) camount,     -- суммы в валюте
    -(d.amntbc)/(power(10,2)) damount_rur,  (c.amntbc)/(power(10, 2)) camount_rur,     -- суммы в рублях
    m.bo_ind doc_type, m.mo_no doc_n,                                                             -- мемордер
    p.glo_ref,
    GL_STMFANTYPE(d.bsaacid, c.bsaacid, o.ac_dr, o.ac_cr, p.post_type, o.fb_side
        , abs((d.amnt)/(power(10, dc.nbdp)))
        , abs((c.amnt)/(power(10, cc.nbdp)))
        , o.pst_scheme, o.amt_dr, o.amt_cr) POST_TYPE,
    o.evtp,
    o.nrt
 from gl_posting p
     join gl_oper o on p.glo_ref = o.gloid
     join pst d on p.pcid = d.pcid and d.amntbc < 0
     join pst c on p.pcid = c.pcid and c.amntbc > 0
     join pcid_mo m on p.pcid = m.pcid
     join currency dc on dc.glccy = d.ccy
     join currency cc on cc.glccy = c.ccy
     join gl_acc da on d.bsaacid = da.bsaacid
     join gl_acc ca on c.bsaacid = ca.bsaacid
 where o.procdate = ? and o.postdate < o.procdate
  and d.invisible <> '1' and c.invisible <> '1' -- проводки актуальны
  and
   (
       (
         exists (select 1 from GL_STMPARM pr where pr.account = substr(d.bsaacid,1,5) and pr.acctype = 'B' and pr.include = '1')
         or
         exists (select 1 from GL_STMPARM pr where pr.account = substr(c.bsaacid,1,5) and pr.acctype = 'B' and pr.include = '1')
       )
       or
       (
         exists (select 1 from GL_STMPARM pr where pr.account = d.bsaacid and pr.acctype = 'A' and pr.include = '1')
         or
         exists (select 1 from GL_STMPARM pr where pr.account = c.bsaacid and pr.acctype = 'A' and pr.include = '1')
       )
   )
  and
   (
       (
         not
         (
             exists (select 1 from GL_STMPARM pr where pr.account = substr(d.bsaacid,1,5) and pr.acctype = 'B' and pr.include = '0')
             and
             exists (select 1 from GL_STMPARM pr where pr.account = substr(c.bsaacid,1,5) and pr.acctype = 'B' and pr.include = '0')
         )
       )
       and
       (
         not
         (
             exists (select 1 from GL_STMPARM pr where pr.account = d.bsaacid and pr.acctype = 'A' and pr.include = '0')
             and
             exists (select 1 from GL_STMPARM pr where pr.account = c.bsaacid and pr.acctype = 'A' and pr.include = '0')
         )
       )
   )
  and not exists (select 1 from gl_etlstma a where a.pcid = p.pcid)