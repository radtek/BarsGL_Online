insert into GL_STMDEL (	PCID, POSTINGDATE , VALUEDATE , DID , CID , DCBACCOUNT
    , CCBACCOUNT, HOSTSYSTEM, DEALID , PSID , DAMOUNT , CAMOUNT , DCUR , CCUR)
select pcid.pcid, dt.postingdate, dt.valuedate, dt.pid did, ct.pid cid, dt.bsaacid dcbaccount, ct.bsaacid ccbaccount, dt.hostsystem, dt.dealid, dt.pcid, dt.amount damount, ct.amount camount, dt.ccy dcur, ct.ccy ccur
  from (select distinct pcid from session.TMP_PCID_DEL) pcid
 join session.TMP_PCID_DEL dt on pcid.pcid = dt.pcid and dt.amount < 0
 join session.TMP_PCID_DEL ct on pcid.pcid = ct.pcid and ct.amount > 0
 where not exists (select 1 from GL_STMDEL s where s.pcid = pcid.pcid)