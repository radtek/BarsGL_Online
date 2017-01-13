package ru.rbt.barsgl.ejbcore.conf.map;

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

    @Override
    public BigDecimal getValue() {
        return value;
    }

    @Override
    public void setValue(BigDecimal value) {
        this.value = value;
    }
}
