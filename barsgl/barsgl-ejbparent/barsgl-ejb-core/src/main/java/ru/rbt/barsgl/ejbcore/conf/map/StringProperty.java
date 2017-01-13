package ru.rbt.barsgl.ejbcore.conf.map;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Created by Ivan Sevastyanov on 22.01.2016.
 * содержит строку в качестве настройки
 */
@Entity
@DiscriminatorValue("STRING_TYPE")
public class StringProperty extends AbstractConfigProperty<String> {

    @Column(name = "STRING_VALUE")
    private String value;

    public StringProperty() {
    }

    public StringProperty(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    public static StringProperty nullStringProperty() {
        return new StringProperty(null);
    }

}
