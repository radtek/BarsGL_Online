package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLBackValueOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLManualOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.shared.enums.BackValuePostStatus;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.InvisibleType;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.ejbcore.util.StringUtils;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static ru.rbt.barsgl.shared.enums.BatchPostStatus.SIGNED;
import static ru.rbt.barsgl.shared.enums.BatchPostStatus.SIGNEDDATE;
import static ru.rbt.ejbcore.util.StringUtils.substr;

/**
 * Created by er18837 on 27.06.2017.
 */
@Stateless
@LocalBean
public class BackValueOperationRepository extends AbstractBaseEntityRepository<GLBackValueOperation, Long> {

    public void updateOperationStatusError(GLOperation operation, OperState state, String message) {
        executeUpdate("update GLOperation o set o.state = ?1, o.errorMessage = ?2 where o = ?3", state, substr(message, 4000), operation);
    }

    public int updateManualStatus(Long gloId, BackValuePostStatus status) {
//        return executeUpdate("update GLBackValueOperation o set o.operExt.manualStatus = ?1 where o.id = ?2", status , gloId);
        return executeUpdate("update GLOperationExt o set o.manualStatus = ?1 where o.id = ?2", status , gloId);
    }

    public List<GLBackValueOperation> getOperationsForProcessing (int count, Date curdate) {
        return selectMaxRows(GLBackValueOperation.class, "from GLBackValueOperation o where o.state = ?1 and o.operExt.manualStatus = ?2 order by o.id ",
                count, OperState.BLOAD, BackValuePostStatus.SIGNEDDATE);
    }

    /**
     * изменяет дату проводки, для подстраховки очищает курсы и рублевые эквиваленты
     * @param postDateNew
     * @param postDateOld
     * @param stateIn
     * @param gloidIn
     * @param sqlParams
     * @return
     */
    public int updatePostDate(Date postDateNew, Date postDateOld, String stateIn, String gloidIn, Object[] sqlParams) {
        Object[] params = new Object[sqlParams.length + 2];
        params[0] = postDateNew;
        params[1] = postDateOld;
        System.arraycopy(sqlParams, 0, params, 2, sqlParams.length);
        return executeNativeUpdate("update GL_OPER set POSTDATE = ?, RATE_DR = 0, EQV_DR = 0, RATE_CR = 0, EQV_CR = 0, EXCH_DIFF = 0" +
                " where STATE in (" + stateIn + ") and POSTDATE = ? and GLOID in (" + gloidIn + ")", params);
    }

    /**
     *  изменить GL_OPEREXT: MNL_STATUS = SIGNEDDATE, USER_AU3, OTS_AU3
     * @param gloidIn   - sql-строка для выбора операций
     * @param statusOld - текущий bv-статус операций
     * @param user      - пользователь
     * @param timestamp - время
     * @param sqlParams - массив параметров для gloidIn
     * @return
     */
    public int updateOperationsSigneddate(BackValuePostStatus statusOld, String user, Date timestamp, String gloidIn, Object[] sqlParams) {
        Object[] params = new Object[sqlParams.length + 4];
        params[0] = BackValuePostStatus.SIGNEDDATE.name();
        params[1] = user;
        params[2] = timestamp;      // TODO время надо ?
        params[3] = statusOld.name();
        System.arraycopy(sqlParams, 0, params, 4, sqlParams.length);
        return executeNativeUpdate("update GL_OPEREXT set MNL_STATUS = ?, USER_AU3 = ?, OTS_AU3 = ?" +
                " where MNL_STATUS = ? and GLOID in (" + gloidIn + ")", params);
    }

    /**
     * изменить GL_OPEREXT: MNL_STATUS = HOLD, MNL_NARRATIVE = wrapper.comment, USER_AU3, OTS_AU3
     * @param gloidIn   - sql-строка для выбора операций
     * @param description - причина
     * @param user      - пользователь
     * @param timestamp - время
     * @param sqlParams - массив параметров для gloidIn
     * @return
     */
    public int updateOperationsHold(String gloidIn, String description, String user, Date timestamp, Object[] sqlParams) {
        Object[] params = new Object[sqlParams.length + 5];
        params[0] = BackValuePostStatus.HOLD.name();
        params[1] = description;
        params[2] = user;
        params[3] = timestamp;
        params[4] = BackValuePostStatus.CONTROL.name();
        System.arraycopy(sqlParams, 0, params, 5, sqlParams.length);
        return executeNativeUpdate("update GL_OPEREXT set MNL_STATUS = ?, MNL_NRT = ?, USER_AU3 = ?, OTS_AU3 = ?" +
                " where MNL_STATUS = ? and GLOID in (" + gloidIn + ")", params);
    }

}

