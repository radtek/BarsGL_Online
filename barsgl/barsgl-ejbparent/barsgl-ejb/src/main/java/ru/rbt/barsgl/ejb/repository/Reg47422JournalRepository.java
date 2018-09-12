package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.gl.Reg47422Journal;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.sql.SQLException;
import java.util.Date;

import static ru.rbt.barsgl.ejb.entity.gl.Reg47422Journal.Reg47422State.*;
import static ru.rbt.barsgl.ejb.entity.gl.Reg47422Journal.Reg47422Valid.N;
import static ru.rbt.barsgl.ejb.entity.gl.Reg47422Journal.Reg47422Valid.U;
import static ru.rbt.barsgl.ejb.entity.gl.Reg47422Journal.Reg47422Valid.Y;

/**
 * Created by er18837 on 23.08.2018.
 */
@Stateless
@LocalBean
public class Reg47422JournalRepository extends AbstractBaseEntityRepository<Reg47422Journal, Long> {

    public int findChangedPst() {
        return executeNativeUpdate("update gl_reg47422 rg set valid = ? " +
                "where rg.id in ( " +
                    "select r.ID from GL_REG47422 r " +
                        "join PST p on r.PD_ID=p.ID " +
                    "where r.VALID = ? and r.STATE not in (?,?) and ( " +
                        "r.INVISIBLE <> nvl(trim(p.INVISIBLE), '0') " +
                        "or p.RNARLNG <> r.RNARLNG " +
                        "or p.PBR <> r.PBR or p.POD <> r.POD " +
                        "or p.ACID <> r.ACID or p.BSAACID <> r.BSAACID " +
                        "or p.AMNT <> r.AMNT or p.PCID <> r.PCID " +
                    " )) ", U.name(), Y.name(), PROC_GL.name(), PROC_ACC.name()
        );
    }

    public int insertNewAndChangedPst(Date fromDate) {
        return executeNativeUpdate("insert into GL_REG47422 (ID,PD_ID,PCID,INVISIBLE,POD,VALD,PROCDATE,PBR,ACID,BSAACID,CCY,AMNT,AMNTBC,DC,CBCC,RNARLNG,NDOG,PMT_REF,GLO_REF,OPERDAY,STATE,VALID) " +
                " select GL_REG47422_SEQ.nextval,p.PD_ID,p.PCID,p.INVISIBLE,p.POD,p.VALD,p.PROCDATE,p.PBR,p.ACID,p.BSAACID,p.CCY,p.AMNT,p.AMNTBC,p.DC,p.CBCC,p.RNARLNG,p.NDOG,p.PMT_REF,p.GLO_REF,p.OPERDAY, " +
                " case when r.PD_ID is null then ? else ? end, ? " +
                " from V_GL_PST47422 p left join GL_REG47422 r on r.PD_ID=p.PD_ID " +
                " where p.INVISIBLE <> '1' and (r.PD_ID is null or r.VALID = ?)  " +
                " and p.POD >= ? and p.POD < p.OPERDAY " +
                " and (p.PROCDATE < p.OPERDAY " +
                  " or p.PROCDATE = p.OPERDAY and (exists (select 1 from GL_ETLSTMD d where d.PCID = p.PCID) " +
                                              " or exists (select 1 from GL_ETLSTMA_H a where a.PCID = p.PCID)))"
                , LOAD.name(), CHANGE.name(), Y.name(), U.name(), fromDate);
    }

    public int updateChangedPstOld() {
        return executeNativeUpdate("update GL_REG47422 set valid = 'N' where valid = 'U'");
    }

    public int updateProcGLPst(String idList, String pcid) {
        executeNativeUpdate("update GL_REG47422 set VALID = ? where ID in (" + idList + ") ", N.name());
        return executeNativeUpdate("insert into GL_REG47422 (ID,PD_ID,PCID,INVISIBLE,POD,VALD,PROCDATE,PBR,ACID,BSAACID,CCY,AMNT,AMNTBC,DC,CBCC,RNARLNG,NDOG,PMT_REF,GLO_REF,ID_REF,PCID_NEW,OPERDAY,STATE,VALID) " +
                " select GL_REG47422_SEQ.nextval,p.ID,p.PCID,p.INVISIBLE,p.POD,p.VALD,p.PROCDATE,p.PBR,p.ACID,p.BSAACID,p.CCY,p.AMNT,p.AMNTBC,r.DC,r.CBCC,p.RNARLNG,r.NDOG,p.PMT_REF,p.GLO_REF,r.ID, " +
                " ?, (select CURDATE from GL_OD), ?, ? " +
                " from GL_REG47422 r join PST p on p.ID = r.PD_ID " +
                " where r.ID in (" + idList + ") ", pcid, PROC_GL.name(), Y.name());

    }

}

