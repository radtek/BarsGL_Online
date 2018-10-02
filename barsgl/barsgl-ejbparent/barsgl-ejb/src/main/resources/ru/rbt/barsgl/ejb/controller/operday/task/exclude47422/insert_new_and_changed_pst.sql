insert into GL_REG47422 (ID, PD_ID, PCID,INVISIBLE, POD, VALD, PROCDATE, PBR,
    ACID, BSAACID, CCY, AMNT, AMNTBC, DC, CBCC, RNARLNG, NDOG, PMT_REF, GLO_REF, OPERDAY, STATE, VALID)
select GL_REG47422_SEQ.nextval, p.PD_ID, p.PCID, p.INVISIBLE, p.POD, p.VALD, p.PROCDATE, p.PBR,
    p.ACID, p.BSAACID, p.CCY, p.AMNT, p.AMNTBC, p.DC, p.CBCC, p.RNARLNG, p.NDOG, p.PMT_REF, p.GLO_REF, p.CURDATE,
	case when r.PD_ID is null then 'LOAD' else 'CHANGE' end, 'Y'
from V_GL_PST47422 p left join GL_REG47422 r on r.PD_ID=p.PD_ID
	where p.INVISIBLE <> '1' and (r.PD_ID is null or r.VALID = 'U')
	and p.POD >= ? and p.POD < p.CURDATE
	and ((p.PROCDATE is null) or (p.PROCDATE < p.CURDATE)
	  or (p.PROCDATE = p.CURDATE and (exists (select 1 from GL_ETLSTMD d where d.PCID = p.PCID)
								   or exists (select 1 from GL_ETLSTMA_H a where a.PCID = p.PCID))))

