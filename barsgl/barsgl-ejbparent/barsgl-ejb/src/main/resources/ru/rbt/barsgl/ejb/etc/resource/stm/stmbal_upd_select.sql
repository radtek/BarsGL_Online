/**
 * Author:  er21775
 * Created: Jun 23, 2017
 */
insert into session.GL_TMP_STMTBAL 
select statdate
       , stattype
       , hostsystem
       , fcccustnum
       , fccaccount
       , fccbranch
       , ext_custid
       , accbrn
       , cbaccount
       , acctype
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
 from 
(select p.pdt statdate
          , 'F' stattype
          , 'BARS' hostsystem
          , ac.custno fcccustnum
          , cast(ac.dealid as varchar(35)) fccaccount
          , get_fcc_br(ac.branch) fccbranch
          , right(ac.custno, 6) ext_custid
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
                 cast(decimal(b.obac) / integer(power(10,cc.nbdp)) as decimal(22,2))
             else
                 cast(decimal(b.obac + b.dtac + b.ctac) / integer(power(10,cc.nbdp)) as decimal(22,2))
            end openblnca
          , case
              when b.dat is null then 0
              else cast(decimal(b.obac + b.dtac + b.ctac) / integer(power(10,cc.nbdp)) as decimal(22, 2))
            end closeblnca
          , GL_GETOPERCNT(ac.bsaacid, p.pdt, '1') dbdocnt
          , case
              when b.dat is null then 0
              when b.dat = p.pdt then
                 abs(cast(decimal(b.dtac) / integer(power(10,cc.nbdp)) as decimal(22, 2)))
              else 0
            end dbturnovra
          , GL_GETOPERCNT(ac.bsaacid, p.pdt, '0') crdocnt
          , case
              when b.dat is null then 0
              when b.dat = p.pdt then
                 cast(decimal(b.ctac) / integer(power(10,cc.nbdp)) as decimal(22, 2))
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
          , value(substr(ac.description,1,255),'') acodname
          , cc.glccy||' '||cc.cynm currname
          , (select lwdate from GL_OD) dateunload
          , b.dat bdat
  from gl_acc ac 
    join 
        (select pdj.pod pdt, pdj.bsaacid  from GL_PDJCHGR pdj where
            pdj.OPERDAY = (select CURDATE from GL_OD)
            and pdj.POD < (select CURDATE from GL_OD)
            and UNF = 'N'
            and (
                    exists (select 1 from GL_STMPARM pr where pr.account = substr(pdj.bsaacid,1,5) and pr.acctype = 'B' and pr.includebln = '1')         
                    or        
                    exists (select 1 from GL_STMPARM pr where pr.account = pdj.bsaacid and pr.acctype = 'A' and pr.includebln = '1')       
                )
            and 
                not exists (select 1 from GL_STMPARM pr where pr.account = pdj.bsaacid and pr.acctype = 'A' and pr.includebln = '0')
            group by pdj.pod, pdj.bsaacid 
        ) p on ac.BSAACID = p.bsaacid
--    join (select curdate pdt from GL_OD) p on ac.dto <= p.pdt and value(ac.dtc, p.pdt) >= p.pdt
    left join baltur b on p.pdt between b.dat and b.datto and b.bsaacid = ac.bsaacid and b.acid = ac.acid
    join currency cc on ac.ccy = cc.glccy
    join currates r on ac.ccy = r.ccy and r.dat = p.pdt
    join sdcustpd s on s.bbcust = ac.custno
    join imbcbcmp i on i.ccbbr = ac.cbccn
    join imbcbbrp rp on rp.a8brcd = ac.branch
    where ac.rlntype <> '1'
) t
where not exists(
    select 1 from session.GL_TMP_STMTBAL st where st.statdate = t.statdate and st.cbaccount = t.cbaccount
)