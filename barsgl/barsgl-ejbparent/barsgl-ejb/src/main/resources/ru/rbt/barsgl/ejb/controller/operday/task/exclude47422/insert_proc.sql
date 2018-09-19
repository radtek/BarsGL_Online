insert into GL_REG47422 (ID, PD_ID, PCID, INVISIBLE, POD, VALD, PROCDATE, PBR,
    ACID, BSAACID, CCY, AMNT, AMNTBC, DC, CBCC, RNARLNG, NDOG, PMT_REF, GLO_REF, PCID_NEW, OPERDAY, STATE, VALID)
select GL_REG47422_SEQ.nextval, p.ID, p.PCID, p.INVISIBLE, p.POD, p.VALD, p.PROCDATE, p.PBR,
    p.ACID, p.BSAACID, p.CCY, p.AMNT, p.AMNTBC,r.DC,r.CBCC, p.RNARLNG,r.NDOG, p.PMT_REF, p.GLO_REF,
    $fld_pcid$, (select CURDATE from GL_OD), ?, ?
from GL_REG47422 r
join PST p on p.ID = r.PD_ID
    where r.ID in ( $id_list$ )