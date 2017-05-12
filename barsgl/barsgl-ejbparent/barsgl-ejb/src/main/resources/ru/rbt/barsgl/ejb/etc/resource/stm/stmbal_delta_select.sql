with p as (select cast ('2017-02-10' as date) pdt from dual),
od as (select cast ('2017-02-10' as date) odt from dual)
select statdate
       , stattype
       , hostsystem
       , fcccustnum
       , cast(ac.dealid as varchar(35)) fccaccount
       , fccbranch
       , ext_custid
       , accbrn
       , cbaccount
       , cast(ac.acctype as varchar(35)) acctype
       , currcode
       , sqnumber
       , createdate
       , timecreate
       , openblnca
       , closeblnca
       , dbdocnt
       , dbturnovra
       , crdocnt
       , crturnovra
       , openblncn
       , closeblncn
       , dbturnovrn
       , crturnovrn
       , rate
       , inbdate
       , outbdate
       , rcustname
       , custinn
       , ecustname
       , cstaddrsw1
       , cstaddrsw2
       , cstaddrsw3
       , cstaddrsw4
       , cstaddrkl
       , cstaddrkf
       , bcustbic
       , rbankname
       , ebankname
       , bcstcorrac
       , bcustswift
       , bcustinn
       , ed
       , branch_id
       , alt_ac_no
       , accname
       , acodname
       , currname
       , dateunload
       , GL_STMDATL(cbaccount, alt_ac_no, statdate) lstoperdat
       , bdat
  from (
     select c.dat statdate
           , 'F' stattype
           , 'BARS' hostsystem
           , rln.cnum fcccustnum
           , get_fcc_br(acmx.bsaacid) fccbranch
           , substr(rln.cnum, -6, 6) ext_custid
           , substr(acmx.acid, -3, 3) accbrn
           , acmx.bsaacid cbaccount
           , cc.glccy currcode
           , cast(null as number(2)) sqnumber
           , current_date createdate
           , current_timestamp timecreate
           , cast(b.obac / power(10,cc.nbdp) as number(22,2)) openblnca
           , cast(b.obac + b.dtac + b.ctac / power(10,cc.nbdp) as number(22, 2)) closeblnca
           , GL_GETOPERCNT(acmx.bsaacid, c.dat, '1') dbdocnt
           , abs(cast(b.dtac / power(10,cc.nbdp) as number(22, 2))) dbturnovra
           , GL_GETOPERCNT(acmx.bsaacid, c.dat, '0') crdocnt
           , cast((b.ctac) / power(10,cc.nbdp) as number(22, 2)) crturnovra
           , cast(b.obbc *0.01 as number(22, 2)) openblncn
           , cast((b.obbc + b.dtbc + b.ctbc) *0.01 as number(22, 2)) closeblncn
           , abs(cast(b.dtbc *0.01 as number(22, 2))) dbturnovrn
           , cast(b.ctbc *0.01 as number(22, 2)) crturnovrn
           , cast(r.rate as number(13,8)) rate
           , cast(null as date) inbdate
           , cast(null as date) outbdate
           , s.bxrunm rcustname
           , s.bxtpid custinn
           , trim(s.bbcna1) ecustname
           , cast(null as varchar(35)) cstaddrsw1
           , cast(null as varchar(35)) cstaddrsw2
           , cast(null as varchar(35)) cstaddrsw3
           , cast(null as varchar(35)) cstaddrsw4
           , s.bxaddr cstaddrkl
           , s.bxpsad cstaddrkf
           , s.bxbicc bcustbic
           , i.ccpnr rbankname
           , rp.a8brnm ebankname
           , cast(null as varchar(35)) bcstcorrac
           , cast(null as varchar(12)) bcustswift
           , cast(null as varchar(14)) bcustinn
           , c.dat ed
           , substr(rln.ccode, -3, 3) branch_id
           , b.acid alt_ac_no
           , s.bbcrnm accname
           , cast('' as varchar(255)) acodname
           , cc.glccy||' '||cc.cynm currname
           , p.pdt dateunload
           , b.dat bdat
    from cal c, accrln rln, baltur b, currency cc, imbcbcmp i, imbcbbrp rp, p
         --, (select curdate pdt from session.gl_tmp_curdate) p
         , currates r, sdcustpd s,
      (
        select d.bsaacid, d.acid, min(d.pod) pd_min, od.odt pd_max
          from gl_oper o, gl_posting ps, pd d, od, p
               --, (select curdate pdt from session.gl_tmp_curdate) p
--               , (select curdate odt from session.gl_tmp_od) od
         where o.procdate = p.pdt and o.state = 'POST' and ps.glo_ref = o.gloid
           and ps.pcid = d.pcid and d.invisible <> '1' and d.pod < p.pdt
           and (
                  (
                    exists (select 1 from GL_STMPARM pr where pr.account = substr(d.bsaacid,1,5) and pr.acctype = 'B' and pr.includebln = '1')
                  )
                  or
                  (
                    exists (select 1 from GL_STMPARM pr where pr.account = d.bsaacid and pr.acctype = 'A' and pr.includebln = '1')
                  )
              )
           and not exists (select 1 from GL_STMPARM pr
                          where pr.account = d.bsaacid and pr.acctype = 'A' and pr.includebln = '0')
         group by d.bsaacid, d.acid, od.odt
      ) acmx
     where c.dat between acmx.pd_min and acmx.pd_max and c.ccy = 'RUR' and c.hol <> 'X'
       and rln.acid = acmx.acid and rln.bsaacid = acmx.bsaacid
       and c.dat between b.dat and b.datto and b.acid = acmx.acid and b.bsaacid = acmx.bsaacid
       and cc.glccy = substr(acmx.acid,9,3)
       and cc.glccy = r.ccy and r.dat = c.dat
       and s.bbcust = rln.cnum
       and i.ccbbr = rln.ccode and rp.a8brcd = substr(acmx.acid, -3, 3)
       and c.dat <= (select case when phase = 'COB' then curdate else lwdate end caldate from gl_od) -- при выгрузке в текущем открытом дне баланс за текущий день не отдаем
) p0
left join gl_acc ac on ac.bsaacid = p0.cbaccount