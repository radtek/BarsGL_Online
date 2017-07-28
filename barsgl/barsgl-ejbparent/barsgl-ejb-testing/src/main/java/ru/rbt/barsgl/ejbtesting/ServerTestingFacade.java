package ru.rbt.barsgl.ejbtesting;

import ru.rbt.barsgl.ejb.controller.operday.PreCobStepController;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountController;
import ru.rbt.barsgl.ejb.integr.bg.EtlPostingController;
import ru.rbt.barsgl.ejb.integr.bg.FanForwardOperationController;
import ru.rbt.barsgl.ejb.repository.EtlPostingRepository;
import ru.rbt.shared.Assert;

import javax.ejb.EJB;
import java.sql.SQLException;
import java.util.TimeZone;

/**
 * Created by Ivan Sevastyanov on 28.01.2016.
 * не все можно вызвать на плохом weblogic напрямую
 */
public class ServerTestingFacade {

    @EJB
    private GLAccountController accountController;

    @EJB
    private EtlPostingRepository etlPostingRepository;

    @EJB
    private EtlPostingController postingController;

    @EJB
    private FanForwardOperationController fanForwardOperationController;

    @EJB
    private PreCobStepController stepController;

    public GLAccount findGLAccountAEnoLock(AccountKeys keys) {
        return accountController.findGLAccountAE(keys, GLOperation.OperSide.C);
    }

    public GLOperation processEtlPosting(long postingId) {

        System.out.println("!!! " + TimeZone.getDefault().getID());
        EtlPosting posting = etlPostingRepository.findById(EtlPosting.class, postingId);
        Assert.notNull(posting);
        return postingController.processMessage(posting);
    }

    public void processFanPosting(String parentRef) throws SQLException {
        stepController.processFanOperation(parentRef, fanForwardOperationController, true);
    }

}
