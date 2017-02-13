package ru.rbt.security.ejb.entity.access;

import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;
import ru.rbt.barsgl.shared.enums.PrmValueEnum;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by akichigi on 07.04.16.
 */
@Entity
@Table(name = "GL_AU_PRMVAL")
public class PrmValue  extends BaseEntity<Long> {

    @Id
    @Column(name = "ID_PRM")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ID_USER")
    private int userId;

    @Column(name = "PRM_CODE")
    @Enumerated(EnumType.STRING)
    private PrmValueEnum prmCode;

    @Column(name = "PRMVAL")
    private  String prmValue;

    @Column(name = "DT_BEGIN")
    @Temporal(TemporalType.DATE)
    private Date dateBegin;

    @Column(name = "DT_END")
    @Temporal(TemporalType.DATE)
    private Date dateEnd;

    @Column(name = "USR_AUT")
    private String userAut;

    @Column(name = "DT_AUT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateAut;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public PrmValueEnum getPrmCode() {
        return prmCode;
    }

    public void setPrmCode(PrmValueEnum prmCode) {
        this.prmCode = prmCode;
    }

    public String getPrmValue() {
        return prmValue;
    }

    public void setPrmValue(String prmValue) {
        this.prmValue = prmValue;
    }

    public Date getDateBegin() {
        return dateBegin;
    }

    public void setDateBegin(Date dateBegin) {
        this.dateBegin = dateBegin;
    }

    public Date getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(Date dateEnd) {
        this.dateEnd = dateEnd;
    }

    public String getUserAut() {
        return userAut;
    }

    public void setUserAut(String userAut) {
        this.userAut = userAut;
    }

    public Date getDateAut() {
        return dateAut;
    }

    public void setDateAut(Date dateAut) {
        this.dateAut = dateAut;
    }
}
