insert into GLVD_BAL_R (DAT,ACID,BSAACID,GLACID,OBAL,DTRN,CTRN,UNLOAD_DAT,OBALRUR,DTRNRUR,CTRNRUR)
select b.dat
      ,acc.bsaacid
      ,acc.id glacid
      ,b.obac obal
      ,abs(b.dtac) dtrn
      ,b.ctac ctrn
      ,? unload_dat
      ,b.obbc obalrur
      ,abs(dtbc) dtrnrur
      ,b.ctbc ctrnrur
from baltur b, gl_acc acc, GL_RVACUL cul
where b.bsaacid = acc.bsaacid
 and cul.bsaacid = b.bsaacid
 and cul.DATE_UPL <= dat
 and dat <=?