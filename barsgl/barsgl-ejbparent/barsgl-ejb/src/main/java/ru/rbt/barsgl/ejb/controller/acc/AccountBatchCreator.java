package ru.rbt.barsgl.ejb.controller.acc;

import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchRequest;

import java.util.Optional;

/**
 * Created by Ivan Sevastyanov on 31.10.2018.
 */
public interface AccountBatchCreator {

    Optional<GLAccount> find(AccountBatchRequest request);

    GLAccount createAccount(AccountBatchRequest request) throws Exception;

}
