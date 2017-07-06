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


}

