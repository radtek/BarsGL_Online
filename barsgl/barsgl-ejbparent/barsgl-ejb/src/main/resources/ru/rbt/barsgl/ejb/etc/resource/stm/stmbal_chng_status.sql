/**
 * Author:  er21775
 * Created: Jun 23, 2017
 */
update GL_PDJCHGR set UNF='Y' where pcid in (
    select pdj.pcid from GL_PDJCHGR pdj where
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
        group by pdj.pcid, pdj.pod, pdj.bsaacid 
)

