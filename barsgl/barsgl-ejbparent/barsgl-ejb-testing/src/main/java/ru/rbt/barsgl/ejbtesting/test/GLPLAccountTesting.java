package ru.rbt.barsgl.ejbtesting.test;

import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountService;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;

import javax.ejb.EJB;

/**
 * Created by Ivan Sevastyanov on 31.03.2016.
 */
public class GLPLAccountTesting {

    @EJB
    private GLAccountService accountService;

    @EJB
    private GLOperationRepository operationRepository;

    public String getAccount(final GLOperation operation, GLOperation.OperSide operSide, AccountKeys keys) throws Exception {
        GLOperation operation2 = operationRepository.executeInNewTransaction(pers -> {
            return operationRepository.update(operation);
        });

        return accountService.getAccount(operation2, operSide, keys);
    }
}
