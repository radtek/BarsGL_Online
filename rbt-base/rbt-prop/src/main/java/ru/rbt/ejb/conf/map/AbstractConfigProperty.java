package ru.rbt.ejb.conf.map;

import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.ejbcore.mapping.YesNo;

import javax.persistence.*;

import static javax.persistence.InheritanceType.SINGLE_TABLE;
import static ru.rbt.ejbcore.mapping.YesNo.N;
import static ru.rbt.ejbcore.mapping.YesNo.Y;

/**
 * Created by Ivan Sevastyanov on 22.01.2016.
 * настройки приложения
 */
@Entity
@Table(name = "GL_PRPRP")
@Inheritance(strategy = SINGLE_TABLE)
@DiscriminatorColumn(name = "PRPTP")
public abstract class AbstractConfigProperty<T> extends BaseEntity<String> {

    /**
     * название свойства
     */
    @Id
    @Column(name = "ID_PRP")
    private String id;

    /**
     * ссылка на узел настроек
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PRN")
    private ConfigNode node;

    /**
     * обязательность
     */
    @Column(name = "REQUIRED")
    @Enumerated(EnumType.STRING)
    private YesNo required;

    /**
     * описание
     */
    @Column(name = "DESCRP")
    private String description;

    public abstract T getValue();
    public abstract void setValue(T value);

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ConfigNode getNode() {
        return node;
    }

    public void setNode(ConfigNode node) {
        this.node = node;
    }

    public YesNo getRequired() {
        return required;
    }

    public void setRequired(YesNo required) {
        this.required = required;
    }

    public void setRequired(boolean required) {
        this.required = required ? Y : N;
    }

    public boolean isRequired() {
        return null != required && Y == required;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
