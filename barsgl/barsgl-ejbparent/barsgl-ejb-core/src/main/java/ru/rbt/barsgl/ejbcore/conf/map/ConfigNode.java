package ru.rbt.barsgl.ejbcore.conf.map;

import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;

import javax.persistence.*;

/**
 * Created by Ivan Sevastyanov on 22.01.2016.
 * Узел настроек приложения
 */
@Entity
@Table(name = "GL_PRND")
public class ConfigNode extends BaseEntity<String> {

    /**
     * Название узла, идентификатор
     */
    @Id
    @Column(name = "ID_PRN")
    private String nodeName;

    /**
     * описание
     */
    @Column(name = "DESCRP")
    private String description;

    /**
     * ссылка на узел верхнего уровня
     */
    @ManyToOne
    @JoinColumn(name = "PRN_ID")
    private ConfigNode parentNode;

    @Override
    public String getId() {
        return nodeName;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public ConfigNode getParentNode() {
        return parentNode;
    }

    public void setParentNode(ConfigNode parentNode) {
        this.parentNode = parentNode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
