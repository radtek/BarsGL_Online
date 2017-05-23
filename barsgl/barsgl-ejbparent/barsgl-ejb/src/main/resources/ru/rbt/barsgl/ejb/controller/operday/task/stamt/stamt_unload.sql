insert into $tablename$ (
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
    substr(o.rnrtl, 1, 255) russhnar,                                                -- описание
    o.ots statementdate, d.pod postingdate, o.vdate valuedate,  -- даты
    o.src_pst hostsystem,
    --o.deal_id dealid,
    case
        when o.src_pst ='PH'  then o.chnl_name
        else o.deal_id
    end  dealid,
    nvl(o.pmt_ref, o.evt_id) psid,                                                             -- источник, ид в системе
    trim(da.bsaacnnum) dclient_id, trim(ca.bsaacnnum) cclient_id,                                            -- бранч майдас дебет
    get_fcc_br(d.bsaacid) dbranch_id, get_fcc_br(c.bsaacid) cbranch_id,                            -- бранч майдас кредит
    d.id did, c.id cid, d.bsaacid dcbaccount, c.bsaacid ccbaccount,                              -- ид и счета полупроводок
    d.ccy dcur, c.ccy ccur,                                                                      -- валюта
    -(d.amnt)/(power(10, dc.nbdp)) damount, (c.amnt)/(power(10, cc.nbdp)) camount,     -- суммы в валюте
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
     join pd d on p.pcid = d.pcid and d.amntbc < 0
     join pd c on p.pcid = c.pcid and c.amntbc > 0
     join pcid_mo m on p.pcid = m.pcid
     join currency dc on dc.glccy = d.ccy
     join currency cc on cc.glccy = c.ccy
     join bsaacc da on d.bsaacid = da.id
     join bsaacc ca on c.bsaacid = ca.id
 where $date_criteria$
  and d.invisible <> '1' and c.invisible <> '1' -- проводки актуальны
  and (GL_STMFILTER(d.bsaacid) = '1' or GL_STMFILTER(c.bsaacid) = '1')
  and not exists (select 1 from gl_etlstma a where a.pcid = p.pcid)