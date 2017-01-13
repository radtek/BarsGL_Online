package ru.rbt.barsgl.ejbtest.utl;

import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.shared.Builder;

import java.util.Date;

/**
 * Created by Ivan Sevastyanov on 30.03.2016.
 */
public class GLOperationBuilder implements Builder<GLOperation> {

    private GLOperation operation = new GLOperation();

    public GLOperationBuilder() {
    }

    public GLOperationBuilder(final GLOperation operation) {
        this.operation = operation;
    }

    public static GLOperationBuilder create() {
        return new GLOperationBuilder();
    }

    public static GLOperationBuilder create(final GLOperation operation) {
        return new GLOperationBuilder(operation);
    }

    public GLOperationBuilder withValueDate(Date vdate) {
        operation.setValueDate(vdate);
        return this;
    }

    @Override
    public GLOperation build() {
        return operation;
    }
}
