package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.gl.GLManualOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.barsgl.shared.enums.OperState;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * Created by ER18837 on 19.08.15.
 */
@Stateless
@LocalBean
public class ManualOperationRepository extends AbstractBaseEntityRepository<GLManualOperation, Long> {
    public void updateOperationStatusError(GLOperation operation, OperState state, String message) {
        executeUpdate("update GLManualOperation o set o.state = ?1, o.errorMessage = ?2 where o = ?3", state, message, operation);
    }

    public void updateOperationStatus(Long operationId, OperState state) {
        executeUpdate("update GLManualOperation o set o.state = ?1 where o.id = ?2", state, operationId);
    }

}
