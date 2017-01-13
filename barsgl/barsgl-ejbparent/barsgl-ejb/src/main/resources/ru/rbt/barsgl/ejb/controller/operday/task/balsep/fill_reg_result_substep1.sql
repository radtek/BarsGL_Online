insert into GLVD_BAL (DAT, ACID, BSAACID, GLACID, OBAL, DTRN, CTRN, UNLOAD_DAT, OBALRUR, DTRNRUR, CTRNRUR)
select cdat, acid, bsaacid, id, obal, abs(dtrn), ctrn, unload_dat, obalrur, abs(dtrnrur), ctrnrur
  from session.GLVD_BAL_SUBSTEP1
