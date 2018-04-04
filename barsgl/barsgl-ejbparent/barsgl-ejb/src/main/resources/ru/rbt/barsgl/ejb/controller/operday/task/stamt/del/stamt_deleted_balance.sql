insert into TMP_GL_BALSTMD
(
 STATDATE
, STATTYPE
, HOSTSYSTEM
, FCCCUSTNUM
, FCCACCOUNT
, FCCBRANCH
, EXT_CUSTID
, ACCBRN
, CBACCOUNT
, ACCTYPE
, CURRCODE
, SQNUMBER
, CREATEDATE
, TIMECREATE
, OPENBLNCA
, CLOSEBLNCA
, DBDOCNT
, DBTURNOVRA
, CRDOCNT
, CRTURNOVRA
, OPENBLNCN
, CLOSEBLNCN
, DBTURNOVRN
, CRTURNOVRN
, RATE
, INBDATE
, OUTBDATE
, RCUSTNAME
, CUSTINN
, ECUSTNAME
, CSTADDRSW1
, CSTADDRSW2
, CSTADDRSW3
, CSTADDRSW4
, CSTADDRKL
, CSTADDRKF
, BCUSTBIC
, RBANKNAME
, EBANKNAME
, BCSTCORRAC
, BCUSTSWIFT
, BCUSTINN
, ED
, BRANCH_ID
, ALT_AC_NO
, ACCNAME
, ACODNAME
, CURRNAME
, DATEUNLOAD
, LSTOPERDAT
)
(
select statdate
       , stattype
       , hostsystem
       , fcccustnum
       , cast(ac.dealid as varchar2(35)) fccaccount
       , fccbranch
       , ext_custid
       , accbrn
       , cbaccount
       , cast(ac.acctype as varchar2(35)) acctype
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
  from (
     select c.dat statdate
           , 'F' stattype
           , 'BARS' hostsystem
           , rln.cnum fcccustnum
           , get_fcc_br(acmx.bsaacid) fccbranch
           , substr(rln.cnum,-6) ext_custid
           , substr(acmx.acid,-3) accbrn
           , acmx.bsaacid cbaccount
           , cc.glccy currcode
           , cast(null as integer) sqnumber
           , current_date createdate
           , current_timestamp timecreate
           , case
                when b.dat = c.dat then
                  cast((b.obac) / cast(power(10,cc.nbdp) as number(4)) as decimal(22,2))
                else
                  cast((b.obac + b.dtac + b.ctac) / cast(power(10,cc.nbdp) as number(4)) as decimal(22,2))
           end openblnca
           , cast((b.obac + b.dtac + b.ctac) / cast(power(10,cc.nbdp) as number(4)) as decimal(22,2)) closeblnca
           , GL_GETOPERCNT(acmx.bsaacid, c.dat, '1') dbdocnt
           , case
                when b.dat = c.dat then
                  abs(cast((b.dtac) / cast(power(10,cc.nbdp) as number(4)) as decimal(22,2)))
                else 0
           end dbturnovra
           , GL_GETOPERCNT(acmx.bsaacid, c.dat, '0') crdocnt
           , case
                when b.dat = c.dat then
                    cast((b.ctac) / cast(power(10,cc.nbdp) as number(4)) as decimal(22,2))
                else 0
           end crturnovra
           , case
                when b.dat = c.dat then
                    cast(b.obbc *0.01 as decimal(22, 2))
              else
                 cast((b.obbc + b.dtbc + b.ctbc) *0.01 as decimal(22, 2))
           end openblncn
           , cast((b.obbc + b.dtbc + b.ctbc) *0.01 as decimal(22, 2)) closeblncn
           , case
                when b.dat = c.dat then
                    abs(cast(b.dtbc *0.01 as decimal(22, 2)))
                else 0
           end dbturnovrn
           , case
                when b.dat = c.dat then
                    cast(b.ctbc *0.01 as decimal(22, 2))
                else 0
           end crturnovrn
           , cast(r.rate as decimal(13,8)) rate
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
           , substr(rln.ccode,-3) branch_id
           , b.acid alt_ac_no
           , s.bbcrnm accname
           , cast('' as varchar(255)) acodname
           , cc.glccy||' '||cc.cynm currname
           , o.curdate dateunload
           , b.dat bdat
    from cal c, accrln rln, baltur b, currency cc, imbcbcmp i, imbcbbrp rp
         , currates r, sdcustpd s, gl_od o
      , (
        select min(v1.postingdate) pd_min, v1.bsaacid, v1.curdate pd_max, acid
         from (
            select r.bsaacid, acid, r.drlnc, row_number() over (partition by r.bsaacid order by r.drlnc desc) rn, o.curdate, postingdate
              from (
                select dcbaccount bsaacid, postingdate
                 from gl_stmdel
                union all
                select ccbaccount bsaacid, postingdate
                 from gl_stmdel
            ) v, gl_od o, accrln r
           where v.bsaacid = r.bsaacid
        ) v1
        where rn = 1
        group by v1.bsaacid, v1.curdate, v1.acid
       ) acmx
     where c.dat between acmx.pd_min and acmx.pd_max and c.ccy = 'RUR' and c.hol <> 'X'
       and rln.acid = acmx.acid and rln.bsaacid = acmx.bsaacid
       and c.dat between b.dat and b.datto and b.acid = acmx.acid and b.bsaacid = acmx.bsaacid
       and cc.glccy = substr(acmx.acid,9,3)
       and cc.glccy = r.ccy and r.dat = c.dat
       and s.bbcust = rln.cnum
       and i.ccbbr = rln.ccode and rp.a8brcd = substr(acmx.acid,-3)
       and c.dat <= (case when o.phase = 'COB' then o.curdate else o.lwdate end) -- при выгрузке в текущем открытом дне баланс за текущий день не отдаем
) p0
left join gl_acc ac on ac.bsaacid = p0.cbaccount
)
