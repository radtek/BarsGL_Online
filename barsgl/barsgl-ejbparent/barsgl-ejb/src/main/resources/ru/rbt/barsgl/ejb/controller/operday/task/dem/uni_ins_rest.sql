INSERT INTO gl_accrest (dat,acid,acc2,bsaacid,ccy,cbcc,outbal,outbalrur,outbmid,outbrmid,psav,acctype,cnum,deal_id,subdealid)
SELECT od.curdate, b.acid, a.acc2, b.bsaacid
       , a.ccy, a.cbcc
       , cast(cast((b.obac + b.dtac + b.ctac+nvl(s.sumamnt,0)) as DECIMAL)/cast(power(10,c.nbdp) as integer) AS DECIMAL(19,2)) outbal
       , (b.obbc + b.dtbc + b.ctbc+nvl(s.sumamntbc,0))*0.01 outbalrur
       , b.obac + b.dtac + b.ctac+nvl(s.sumamnt,0) outbmid
       , b.obbc + b.dtbc + b.ctbc+nvl(s.sumamntbc,0) outbrmid
       , a.psav, a.acctype, a.custno, a.dealid, a.subdealid
  FROM baltur b LEFT OUTER JOIN GL_BALTUR_SUMS s ON b.bsaacid=s.bsaacid,
          gl_acc a, currency c, (SELECT curdate FROM GL_TMP_CURDATE) od
 WHERE b.acid = a.acid
   AND b.bsaacid = a.bsaacid
   AND a.acctype != 0
   AND a.dto <= od.curdate
   AND nvl(a.dtc, od.curdate) >= od.curdate
   AND b.dat <= od.curdate
   AND b.datto >= od.curdate
   AND a.ccy = c.glccy