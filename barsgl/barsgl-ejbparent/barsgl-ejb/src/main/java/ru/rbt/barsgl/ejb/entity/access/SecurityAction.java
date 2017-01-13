package ru.rbt.barsgl.ejb.entity.access;

import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;
import ru.rbt.barsgl.shared.enums.SecurityActionCode;

import javax.persistence.*;

/**
 * Created by ER21006 on 19.04.2016.
 */
@Entity
@Table(name = "GL_AU_ACT")
public class SecurityAction extends BaseEntity<Integer> {

    @Id
    @Column(name = "ID_ACT")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_GROUP")
    private SecurityActionGroup actionGroup;

    @Column(name = "ACT_CODE")
    @Enumerated(EnumType.STRING)
    private SecurityActionCode securityActionCode;

    @Column(name = "ACTDESCR")
    private String descr;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public SecurityActionGroup getActionGroup() {
        return actionGroup;
    }

    public void setActionGroup(SecurityActionGroup actionGroup) {
        this.actionGroup = actionGroup;
    }

    public SecurityActionCode getSecurityActionCode() {
        return securityActionCode;
    }

    public void setSecurityActionCode(SecurityActionCode securityActionCode) {
        this.securityActionCode = securityActionCode;
    }

    public String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }
}
