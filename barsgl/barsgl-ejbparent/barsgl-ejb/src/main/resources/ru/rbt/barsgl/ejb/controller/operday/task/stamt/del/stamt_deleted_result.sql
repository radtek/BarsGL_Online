insert into GL_STMDEL (	PCID, POSTINGDATE , VALUEDATE , DID , CID , DCBACCOUNT
    , CCBACCOUNT, HOSTSYSTEM, DEALID , PSID , DAMOUNT , CAMOUNT , DCUR , CCUR)
select pcid, postingdate, valuedate, did, cid, dcbaccount, ccbaccount, hostsystem, dealid, psid, damount, camount, dcur, ccur
  from (
        select pcid.pcid, dt.postingdate, dt.valuedate, dt.pid did, ct.pid cid, dt.bsaacid dcbaccount, ct.bsaacid ccbaccount, dt.hostsystem, dt.dealid, dt.pcid psid, abs(dt.amount) damount, abs(ct.amount) camount, dt.ccy dcur, ct.ccy ccur,
        row_number () over (partition by pcid.pcid order by dt.pid) rn
          from (select distinct pcid from TMP_PCID_DEL) pcid
         join TMP_PCID_DEL dt on pcid.pcid = dt.pcid and dt.amount < 0
         join TMP_PCID_DEL ct on pcid.pcid = ct.pcid and ct.amount > 0
         where not exists (select 1 from GL_STMDEL s where s.pcid = pcid.pcid)
           and (GL_STMFILTER(dt.bsaacid) = '1' or GL_STMFILTER(ct.bsaacid) = '1')
) v where rn = 1
