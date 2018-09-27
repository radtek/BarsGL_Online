update gl_reg47422 rg set valid = 'U'
where rg.id in (
	select r.ID from GL_REG47422 r
		join PST p on r.PD_ID=p.ID
	where r.VALID = 'Y' and r.STATE not in ('PROC_DAT','PROC_ACC') and (
		r.INVISIBLE <> nvl(trim(p.INVISIBLE), '0')
		or p.RNARLNG <> r.RNARLNG
		or p.PBR <> r.PBR or p.POD <> r.POD
		or p.ACID <> r.ACID or p.BSAACID <> r.BSAACID
		or p.AMNT <> r.AMNT or p.PCID <> r.PCID
	 ))
