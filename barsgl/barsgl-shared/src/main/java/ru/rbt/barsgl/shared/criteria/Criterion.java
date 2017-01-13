package ru.rbt.barsgl.shared.criteria;

import java.io.Serializable;

/**
 * Created by Ivan Sevastyanov
 */
public interface Criterion<T> extends Serializable {
    public T getValue();
}
