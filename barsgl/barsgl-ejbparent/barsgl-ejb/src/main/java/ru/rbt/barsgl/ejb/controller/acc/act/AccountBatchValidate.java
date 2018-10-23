package ru.rbt.barsgl.ejb.controller.acc.act;

import ru.rbt.barsgl.ejb.controller.acc.AccountBatchPackageEvent;
import ru.rbt.barsgl.ejb.controller.sm.Transition;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchRequest;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.shared.enums.AccountBatchState;

import javax.ejb.EJB;
import java.util.List;

/**
 * Created by Ivan Sevastyanov on 23.10.2018.
 */
public class AccountBatchValidate extends AbstractAccountBatchAction {

    @EJB
    private CoreRepository repository;

    @Override
    public AccountBatchPackageEvent proceed(AccountBatchPackage stateObject, Transition transition) {
        List<AccountBatchRequest> requests = repository.select(AccountBatchRequest.class, "from AccountBatchRequest r where r.batchPackage = ?1"
            , stateObject);
        for (AccountBatchRequest request : requests) {
            request.setState(AccountBatchState.VALID);
            repository.update(request);
        }
        return AccountBatchPackageEvent.VALIDATE_SUCESS;
    }
}
