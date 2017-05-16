insert into gl_etlpst
(id, id_pst, id_pkg, src_pst, evt_id, pmt_ref, vdate, ots, nrt, RNRTL, RNRTS, strn, ac_dr, ccy_dr, amt_dr, ac_cr, ccy_cr, amt_cr, fan, par_rf, evtp)
select gl_seq_pst.nextval id, '00'||cast (id_pst as varchar(64)) id_pst, nvl(?, 999999) id_pkg
       , 'FC6_PAY' src_pst, cast(p.evt_id as varchar(128)) evt_id, p.pmt_ref pmt_ref, p.vdate vdate, current_timestamp ots
       , p.nrt nrt, p.RNRTL, p.RNRTS, 'N' strn, p.ac_dr, p.ccy_dr, p.amt_dr, p.ac_cr, p.ccy_cr, p.amt_cr
       , 'Y' fan, p.par_rf, p.evtp
  from GL_NDSPST p
 where p.id_pst between ? and ?
   and p.processed = 'N'