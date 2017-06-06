package ru.rbt.barsgl.ejbtesting.test;

import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.controller.etc.TextResourceController;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.CallableStatement;

/**
 * Created by er21006 on 06.06.2017.
 */
public class DdlSupportBeanTesting {

    @Inject
    private TextResourceController textController;

    @EJB
    private CoreRepository coreRepository;

    public void createGlOperPodViewNew() throws Exception {
        executeCallable(textController.getContent("ru/rbt/barsgl/ejbtesting/test/view_pod_new.sql"));
    }

    public void createGlOperPodViewOld() throws Exception {
        executeCallable(textController.getContent("ru/rbt/barsgl/ejbtesting/test/view_pod_old.sql"));
    }

    private void executeCallable(String sql) throws Exception {
        coreRepository.executeTransactionally( connection -> {
            try (CallableStatement statement
                         = connection.prepareCall(sql)){
                statement.executeUpdate();
                return null;
            }
        });
    }

}
