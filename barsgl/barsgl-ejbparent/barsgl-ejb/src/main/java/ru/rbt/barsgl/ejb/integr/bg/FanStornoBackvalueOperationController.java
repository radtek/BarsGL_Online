package ru.rbt.barsgl.ejb.integr.bg;

import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.List;

/**
 * Контроллер для обработки веерных операций сторно с прошедшей датой
 * Created by ER18837 on 21.05.15.
 */
@Stateless
@LocalBean
public class FanStornoBackvalueOperationController extends FanPostingOperationController {

    @Override
    public YesNo getStorno() {
        return YesNo.Y;
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
