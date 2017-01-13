package ru.rbt.barsgl.ejb.integr.bg;

import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.integr.fan.FanOperationProcessor;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.barsgl.ejb.repository.GLPostingRepository;
import ru.rbt.barsgl.ejb.repository.MemorderRepository;
import ru.rbt.barsgl.ejb.repository.PdRepository;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.shared.enums.OperState;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static ru.rbt.barsgl.ejbcore.validation.ValidationError.initSource;

/**
 * Контроллер для обработки веерных операций НЕ СТРОРНО
 * Created by ER18837 on 18.05.15.
 */
@Stateless
@LocalBean
public class FanForwardOperationController extends FanPostingOperationController {

    @Override
    public YesNo getStorno() {
        return YesNo.N;
    }

    @Override
    public boolean isWtacEnabled() {
        return false;
    }

    @Override
    public List<GLOperation> processOperations(String parentReference) {
        return super.processOperations(parentReference);
    }
}
