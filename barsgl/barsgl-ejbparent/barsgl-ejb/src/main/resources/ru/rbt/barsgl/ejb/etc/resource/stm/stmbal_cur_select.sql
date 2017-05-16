insert into GL_BALSTM (STATDATE
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
                        , LSTOPERDAT
                        , ED
                        , BRANCH_ID
                        , ALT_AC_NO
                        , ACCNAME
                        , ACODNAME
                        , CURRNAME
                        , DATEUNLOAD)
with gl_tmp_curdate as (select cast(? as date) curdate from dual)
select *
  from (
  select p.pdt statdate
          , 'F' stattype
          , 'BARS' hostsystem
          , ac.custno fcccustnum
          , cast(ac.dealid as varchar(35)) fccaccount
          , get_fcc_br(ac.branch) fccbranch
          , substr(ac.custno, -6) ext_custid
          , ac.branch accbrn
          , ac.bsaacid cbaccount
          , cast(ac.acctype as varchar(35)) acctype
          , ac.ccy currcode
          , cast(null as integer) sqnumber
          , current_date createdate
          , current_timestamp timecreate
          , case
              when b.dat is null then 0
              when b.dat = p.pdt then
                 cast((b.obac) / cast(power(10,cc.nbdp) as number(4)) as decimal(22,2))
             else
                 cast((b.obac + b.dtac + b.ctac) / cast(power(10,cc.nbdp) as number(4)) as decimal(22,2))
            end openblnca
          , case
              when b.dat is null then 0
              else cast((b.obac + b.dtac + b.ctac) / cast(power(10,cc.nbdp) as number(4)) as decimal(22, 2))
            end closeblnca
          , GL_GETOPERCNT(ac.bsaacid, p.pdt, '1') dbdocnt
          , case
              when b.dat is null then 0
              when b.dat = p.pdt then
                 abs(cast((b.dtac) / cast(power(10,cc.nbdp) as number(4)) as decimal(22, 2)))
              else 0
            end dbturnovra
          , GL_GETOPERCNT(ac.bsaacid, p.pdt, '0') crdocnt
          , case
              when b.dat is null then 0
              when b.dat = p.pdt then
                 cast((b.ctac) / cast(power(10,cc.nbdp) as number(4)) as decimal(22, 2))
              else 0
            end crturnovra
          , case
              when b.dat is null then 0
              when b.dat = p.pdt then
                 cast(b.obbc *0.01 as decimal(22, 2))
              else
                 cast((b.obbc + b.dtbc + b.ctbc) *0.01 as decimal(22, 2))
            end openblncn
          , case
              when b.dat is null then 0
              else
                 cast((b.obbc + b.dtbc + b.ctbc) *0.01 as decimal(22, 2))
          end closeblncn
          , case
              when b.dat is null then 0
               when b.dat = p.pdt then
                 abs(cast(b.dtbc *0.01 as decimal(22, 2)))
               else 0
             end dbturnovrn
          , case
              when b.dat is null then 0
              when b.dat = p.pdt then
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
          , GL_STMDATL(ac.bsaacid, ac.acid, p.pdt) lstoperdat
          , p.pdt ed
          , ac.cbcc branch_id
          , ac.acid alt_ac_no
          , s.bbcrnm accname
          , nvl(substr(ac.description,1,255),'') acodname
          , cc.glccy||' '||cc.cynm currname
          , p.pdt dateunload
  from gl_acc ac
   join (select curdate pdt from GL_TMP_CURDATE) p on ac.dto <= p.pdt and nvl(ac.dtc, p.pdt) >= p.pdt
   left join baltur b on p.pdt between b.dat and b.datto and b.bsaacid = ac.bsaacid and b.acid = ac.acid
   join currency cc on ac.ccy = cc.glccy
   join currates r on ac.ccy = r.ccy and r.dat = p.pdt
   join sdcustpd s on s.bbcust = ac.custno
   join imbcbcmp i on i.ccbbr = ac.cbccn
   join imbcbbrp rp on rp.a8brcd = ac.branch
  where ac.rlntype <> '1'
  ) ac0
 where GL_STMFILTER(ac0.cbaccount) = '1'