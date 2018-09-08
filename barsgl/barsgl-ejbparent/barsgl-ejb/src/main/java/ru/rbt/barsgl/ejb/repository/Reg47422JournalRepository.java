package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.gl.Reg47422Journal;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.sql.SQLException;
import java.util.Date;

/**
 * Created by er18837 on 23.08.2018.
 */
@Stateless
@LocalBean
public class Reg47422JournalRepository extends AbstractBaseEntityRepository<Reg47422Journal, Long> {

    public int findChangedPst() {
        return executeNativeUpdate("update gl_reg47422 rg set valid = 'U' " +
                "where rg.id in ( " +
                    "select r.id from GL_REG47422 r " +
                        "join PST p on r.pd_id=p.id " +
                    "where r.valid = 'Y' and r.state not in ('PROC_GL','PROC_ACC') and ( " +
                        "r.invisible <> nvl(trim(p.INVISIBLE), '0') " +
                        "or p.RNARLNG <> r.RNARLNG " +
                        "or p.pbr <> r.pbr or p.pod <> r.pod  " +
                        "or p.acid <> r.acid or p.bsaacid <> r.bsaacid " +
                        "or p.amnt <> r.amnt or p.pcid <> r.pcid " +
                    " )) "
        );
    }

    public int insertNewAndChangedPst(Date fromDate) {
        return executeNativeUpdate("insert into GL_REG47422 (ID,PD_ID,PCID,INVISIBLE,POD,VALD,PROCDATE,PBR,ACID,BSAACID,CCY,AMNT,AMNTBC,DC,CBCC,RNARLNG,NDOG,PMT_REF,GLO_REF,OPERDAY,STATE,VALID) " +
                "select GL_REG47422_SEQ.nextval,p.PD_ID,p.PCID,p.INVISIBLE,p.POD,p.VALD,p.PROCDATE,p.PBR,p.ACID,p.BSAACID,p.CCY,p.AMNT,p.AMNTBC,p.DC,p.CBCC,p.RNARLNG,p.NDOG,p.PMT_REF,p.GLO_REF,p.OPERDAY, " +
                "case when r.pd_id is null then 'LOAD' else 'CHANGE' end,'Y' " +
                "from V_GL_PST47422 p left join GL_REG47422 r on r.pd_id=p.pd_id " +
                "where p.INVISIBLE <> '1' and (r.pd_id is null or r.valid = 'U')  " +
                "and p.pod >= ? and p.pod < p.operday " +
                "and (p.procdate < p.operday " +
                  "or p.procdate = p.operday and (exists (select 1 from gl_etlstmd d where d.PCID = p.PCID) " +
                                              "or exists (select 1 from gl_etlstma_h a where a.PCID = p.PCID)))\n"
                , fromDate);
    }

    public int updateChangedPstOld() {
        return executeNativeUpdate("update GL_REG47422 set valid = 'N' where valid = 'U'");
    }

}

