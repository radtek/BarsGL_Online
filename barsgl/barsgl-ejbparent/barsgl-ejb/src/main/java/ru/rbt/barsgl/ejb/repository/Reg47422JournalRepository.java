package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.gl.Reg47422Journal;
import ru.rbt.barsgl.shared.enums.Reg47422State;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.ejbcore.validation.ValidationError;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static ru.rbt.barsgl.ejb.entity.gl.Reg47422Journal.Reg47422Valid.N;
import static ru.rbt.barsgl.ejb.entity.gl.Reg47422Journal.Reg47422Valid.U;
import static ru.rbt.barsgl.ejb.entity.gl.Reg47422Journal.Reg47422Valid.Y;
import static ru.rbt.barsgl.shared.enums.Reg47422State.*;
import static ru.rbt.ejbcore.validation.ErrorCode.REG47422_ERROR;

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
                " select GL_REG47422_SEQ.nextval,p.PD_ID,p.PCID,p.INVISIBLE,p.POD,p.VALD,p.PROCDATE,p.PBR,p.ACID,p.BSAACID,p.CCY,p.AMNT,p.AMNTBC,p.DC,p.CBCC,p.RNARLNG,p.NDOG,p.PMT_REF,p.GLO_REF,p.CURDATE, " +
                " case when r.PD_ID is null then ? else ? end, ? " +
                " from V_GL_PST47422 p left join GL_REG47422 r on r.PD_ID=p.PD_ID " +
                " where p.INVISIBLE <> '1' and (r.PD_ID is null or r.VALID = ?)  " +
                " and p.POD >= ? and p.POD < p.CURDATE " +
                " and ((p.PROCDATE is null) or (p.PROCDATE < p.CURDATE) " +
                  " or (p.PROCDATE = p.CURDATE and (exists (select 1 from GL_ETLSTMD d where d.PCID = p.PCID) " +
                                              " or exists (select 1 from GL_ETLSTMA_H a where a.PCID = p.PCID))))"
                , LOAD.name(), CHANGE.name(), Y.name(), U.name(), fromDate);
    }

    public int updateChangedPstOld() {
        return executeNativeUpdate("update GL_REG47422 set valid = 'N' where valid = 'U'");
    }

    public int updateProcGL(String idList, String pcid) {
        executeNativeUpdate("update GL_REG47422 set VALID = ? where ID in (" + idList + ") ", N.name());
        executeNativeUpdate("insert into GL_REG47422 (ID,PD_ID,PCID,INVISIBLE,POD,VALD,PROCDATE,PBR,ACID,BSAACID,CCY,AMNT,AMNTBC,DC,CBCC,RNARLNG,NDOG,PMT_REF,GLO_REF,PCID_NEW,OPERDAY,STATE,VALID) " +
                " select GL_REG47422_SEQ.nextval,p.ID,p.PCID,p.INVISIBLE,p.POD,p.VALD,p.PROCDATE,p.PBR,p.ACID,p.BSAACID,p.CCY,p.AMNT,p.AMNTBC,r.DC,r.CBCC,p.RNARLNG,r.NDOG,p.PMT_REF,p.GLO_REF, " +
                " ?, (select CURDATE from GL_OD), ?, ? " +
                " from GL_REG47422 r join PST p on p.ID = r.PD_ID " +
                " where r.ID in (" + idList + ") ", pcid, PROC_GL.name(), Y.name());
        return executeNativeUpdate("update GL_REG47422 r1 set ID_REF = (select r2.ID from GL_REG47422 r2 where PCID = ? and valid = ?) where PCID_NEW = ? and valid = ?",
                pcid, Y.name(), pcid, Y.name());
    }

    public void updateState(String idList, Reg47422State state) {
        executeNativeUpdate("update GL_REG47422 set STATE = ? where ID in (" + idList + ")", state.name());
    }

    public String getAccount47416(String acc47422) throws SQLException {
        List<DataRecord> data = select("select ACC47416 from V_GL_ACC47422 where ACC47422 = ?", acc47422);
        if (null == data || data.isEmpty())
            return "";
        else if (data.size() > 1)
            throw new ValidationError(REG47422_ERROR, "Найдено > 1 счета 47416 для подмены счета " + acc47422);
        return data.get(0).getString(0);

    }
}

