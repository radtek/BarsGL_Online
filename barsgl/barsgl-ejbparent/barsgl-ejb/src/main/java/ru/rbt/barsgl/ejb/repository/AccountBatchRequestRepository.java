package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.etl.AccountBatchRequest;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * Created by er18837 on 17.10.2018.
 */
@Stateless
@LocalBean
public class AccountBatchRequestRepository extends AbstractBaseEntityRepository<AccountBatchRequest, Long> {
}
