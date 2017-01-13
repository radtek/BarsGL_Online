package ru.rbt.barsgl.ejbcore.conf.map;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Created by Ivan Sevastyanov on 22.01.2016.
 * содержит целое число в качестве настройки
 */
@Entity
@DiscriminatorValue("NUMBER_TYPE")
public class NumberProperty extends AbstractConfigProperty<Long> {

    @Column(name = "NUMBER_VALUE")
    private Long value;

    public NumberProperty() {
    }

    public NumberProperty(Long value) {
        this.value = value;
    }

    @Override
    public Long getValue() {
        return value;
    }

    @Override
    public void setValue(Long value) {
        this.value = value;
    }

    public static NumberProperty nullNumberProperty() {
        return new NumberProperty(null);
    }

}
