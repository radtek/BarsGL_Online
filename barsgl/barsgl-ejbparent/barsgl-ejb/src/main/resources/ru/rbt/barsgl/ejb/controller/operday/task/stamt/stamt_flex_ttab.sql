-- представление со счетами по которым были обороты в закрытом дне
declare global temporary table tmp_stmflx_ledger_bsass as (
    select *
      from (
      select distinct ac.bsaacid cbaccount, p.pdt statdate
      from gl_acc ac
       join gl_balacc bc on ac.acctype = bc.acctype
       join (select value(?, current date) pdt from sysibm.sysdummy1) p on ac.dto <= p.pdt and value(ac.dtc, p.pdt) >= p.pdt
       join pd d on d.bsaacid = ac.bsaacid and d.pod = p.pdt and d.pbr like '@@IF%'
       where not exists (select 1 from GL_BALSTMD mdm where mdm.statdate = p.pdt and mdm.cbaccount = ac.bsaacid)
      ) ac0
     where
           (
               (
                 exists (select 1 from GL_STMPARM pr where pr.account = substr(ac0.cbaccount,1,5) and pr.acctype = 'B' and pr.includebln = '1')
               )
               or
               (
                 exists (select 1 from GL_STMPARM pr where pr.account = ac0.cbaccount and pr.acctype = 'A' and pr.includebln = '1')
               )
           )
      and not exists (select 1 from GL_STMPARM pr
                       where pr.account = ac0.cbaccount and pr.acctype = 'A' and pr.includebln = '0')
) with data with replace;


