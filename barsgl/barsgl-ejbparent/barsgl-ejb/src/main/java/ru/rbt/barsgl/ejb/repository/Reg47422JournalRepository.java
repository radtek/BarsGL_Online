package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.acc.GLAccParam;
import ru.rbt.barsgl.ejb.entity.gl.Reg47422Journal;
import ru.rbt.barsgl.shared.enums.Reg47422State;
import ru.rbt.ejbcore.controller.etc.TextResourceController;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ValidationError;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static ru.rbt.barsgl.ejb.entity.gl.Reg47422Journal.Reg47422Valid.N;
import static ru.rbt.barsgl.ejb.entity.gl.Reg47422Journal.Reg47422Valid.U;
import static ru.rbt.barsgl.ejb.entity.gl.Reg47422Journal.Reg47422Valid.Y;
import static ru.rbt.barsgl.shared.enums.Reg47422State.*;
import static ru.rbt.ejbcore.validation.ErrorCode.EXCLUDE_47422_ERROR;

/**
 * Created by er18837 on 23.08.2018.
 */
@Stateless
@LocalBean
public class Reg47422JournalRepository extends AbstractBaseEntityRepository<Reg47422Journal, Long> {

    @Inject
    private TextResourceController resourceController;

    /**
     * найти и изменить VALID = 'U' для проводок из регистра, которые изменились
     * @return
     * @throws IOException
     */
    public int markChangedPst() throws IOException {
        return executeNativeUpdate(resourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/exclude47422/mark_changed_pst.sql")
        );
    }

    /**
     * вставить новые и измененные проводки в регистр
     * @param fromDate
     * @return
     * @throws IOException
     */
    public int insertNewAndChangedPst(Date fromDate) throws IOException {
        return executeNativeUpdate(resourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/exclude47422/insert_new_and_changed_pst.sql")
                , fromDate);
    }

    public int updateChangedPstOld() {
        return executeNativeUpdate("update GL_REG47422 set valid = 'N' where valid = 'U'");
    }

    /**
     * sql для выброра проводок для обработки
     * @param withSum - true = с одинаковой суммой
     * @param withPod - true = с одинаковой датой
     * @return
     */
    public List<DataRecord> getGroupedList(Date dateFrom, boolean withSum, boolean withPod, Reg47422State ... status) throws IOException, SQLException {
        String sql = resourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/exclude47422/get_groups_for_glue.sql")
                .replace("$fld_pod$", withPod ? ", pod" : "")
                .replace("$fld_sum$", withSum ? ", abs(amnt)" : "")
                .replace("$state_list$", StringUtils.arrayToString(status, ",", "'"));
        return select(sql, dateFrom);
    }

    /**
     * склеивает проводки с одинаковой датой
     */
    public void gluePostings(String idInvisible, String idVisible, String pcidNew, String pbr) {
        // подавить проводки по 47422
        executeNativeUpdate("update PST p1 set INVISIBLE = '1' where id in (" + idInvisible + ")");
        // ссылка на парную полупроводку (со старым PCID) в CPDRF
        executeNativeUpdate("update PST p1 set p1.CPDRF = (select ID from PST p2 where p2.PCID = p1.PCID and p2.INVISIBLE = '1')" +
                "  where id in (" + idVisible + ")");
        // update проводки с другой стороны: PCID, DEAL_ID, PBR, PNAR, RNARLNG, PREF, PMT_REF = PREF
        executeNativeUpdate("update PST p1 set PCID = ?, PBR = ? where id in (" + idVisible + ")", pcidNew, pbr);
    }

    public void updateOperations(String gloPar, String gloAll, String stickSide, String featherSide, String pcidNew) {
        // GL_POSTING: POST_TYPE = '5' (для ручки)
        executeNativeUpdate("update GL_POSTING set POST_TYPE = '5' where GLO_REF = " + gloPar + " and PCID = " + pcidNew);
        // GL_OPER: FAN = 'Y', PAR_RF = PMT_REF, PAR_GLO = GLOID
        //          FP_SIDE = 'D'/'C' – сторона, обратная ручке (для всех операций)
        executeNativeUpdate("update GL_OPER set FAN = 'Y', PAR_GLO = " + gloPar + ", FP_SIDE = ? where GLOID in (" + gloAll + ")",    // PAR_RF = ?,
                featherSide);                                                                                        // params.parRf,
        //          FB_SIDE = 'С'/'D' - сторона ручки (только для ручки)
        executeNativeUpdate("update GL_OPER set FB_SIDE = ? where GLOID = " + gloPar, stickSide);
    }

    public void updateState(String idList, Reg47422State state) {
        executeNativeUpdate("update GL_REG47422 set STATE = ? where ID in (" + idList + ")", state.name());
    }

    public GLAccParam getAccount47416(String acid) throws SQLException {
        List<DataRecord> data = select("select distinct ACID47416, BSAACID47416 from V_GL_ACC47422 where ACID47422 = ?", acid);
        if (null == data || data.isEmpty())
            return null;
        else if (data.size() > 1)
            throw new ValidationError(EXCLUDE_47422_ERROR, "Найдено > 1 счета 47416 для подмены счета " + acid);
        return new GLAccParam(data.get(0).getString(0), data.get(0).getString(1));
    }

    public void replace47416(String pcidList, String idInvisible, GLAccParam ac47416) throws IOException {
        // подавить проводки по 47422
        executeNativeUpdate("update PST p1 set INVISIBLE = '1' where id in (" + idInvisible + ")");
        // создать проводки по 47416  insert_pst_47416.sql
        executeNativeUpdate(resourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/exclude47422/insert_pst_47416.sql")
                .replace("$id_invisible$", idInvisible), ac47416.getAcid(), ac47416.getBsaAcid());
        // ссылка на парную полупроводку (со старым PCID) в CPDRF
        executeNativeUpdate("update PST p1 set p1.CPDRF = (select ID from PST p2 where p2.PCID = p1.PCID and p2.INVISIBLE = '1')" +
                "  where p1.PCID in (" + pcidList + ") and p1.INVISIBLE = '0' and BSAACID like '47416%'");
    }

    public int updateProcGL(String idList, String pcid) throws IOException {
        executeNativeUpdate("update GL_REG47422 set VALID = ? where ID in (" + idList + ") ", N.name());
        executeNativeUpdate(resourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/exclude47422/insert_proc.sql")
                        .replace("$fld_pcid$", pcid).replace("$id_list$", idList)
                , PROC_DAT.name());
        return executeNativeUpdate("update GL_REG47422 r1 set ID_REF = (select r2.ID from GL_REG47422 r2 where PCID = ? and valid = 'Y')" +
                        " where PCID_NEW = ? and valid = 'Y'"
                , pcid, pcid);
    }

    public int updateProcAcc(String idList, String pcidList, String pcid) throws IOException {
        executeNativeUpdate("update GL_REG47422 set VALID = ? where ID in (" + idList + ") ", N.name());
        executeNativeUpdate(resourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/exclude47422/insert_proc.sql")
                        .replace("$fld_pcid$", "p.PCID").replace("$id_list$", idList)
                , PROC_ACC.name());
        return executeNativeUpdate("update GL_REG47422 r1 set ID_REF = (select r2.ID from GL_REG47422 r2 where r2.PCID_NEW = ? and r2.VALID = 'Y')" +
                        " where r1.PCID_NEW in (" + pcidList + ") and r1.VALID = 'Y'"
                , pcid);
    }

    public int updateErrProc(String idList) throws IOException {
        return executeNativeUpdate("update GL_REG47422 set STATE = ? where ID in (" + idList + ") ", ERRPROC.name());
    }

}

