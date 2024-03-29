package ru.rbt.ejb.conf.map;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.math.BigDecimal;

/**
 * Created by Ivan Sevastyanov on 22.01.2016.
 * настройка системы содержит значение с плавающей точкой
 */
@Entity
@DiscriminatorValue("DECIMAL_TYPE")
public class DecimalProperty extends AbstractConfigProperty<BigDecimal> {

    @Column(name = "DECIMAL_VALUE")
    private BigDecimal value;

    public DecimalProperty() {
    }

    public DecimalProperty(BigDecimal value) {
        this.value = value;
    }

    @Override
    public BigDecimal getValue() {
        return value;
    }

    @Override
    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public static DecimalProperty nullDecimalProperty() {
        return new DecimalProperty(null);
    }
}
