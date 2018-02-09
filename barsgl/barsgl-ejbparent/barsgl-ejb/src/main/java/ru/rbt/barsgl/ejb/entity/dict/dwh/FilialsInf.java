package ru.rbt.barsgl.ejb.entity.dict.dwh;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by er22317 on 08.02.2018.
 */
@Entity
@Table(name = "DWH_IMBCBCMP_INF")
public class FilialsInf extends BaseEntity<String> {

    @Id
    @Column(name = "CCPCD")
    private String id;

    @Column(name = "CCPNE")
    private String ccpne;

    @Column(name = "CCPNR")
    private String ccpnr;

    @Column(name = "CCPRI")
    private String ccpri;

    @Column(name = "CCBBR")
    private String ccbbr;

    @Column(name = "ALT_CODE")
    private String altCode;

    @Temporal(TemporalType.DATE)
    @Column(name = "VALID_FROM")
    private Date validFrom;

    public String getId() {
        return null;
    }

    public String getCcpne() {
        return ccpne;
    }

    public void setCcpne(String ccpne) {
        this.ccpne = ccpne;
    }

    public String getCcpnr() {
        return ccpnr;
    }

    public void setCcpnr(String ccpnr) {
        this.ccpnr = ccpnr;
    }

    public String getCcpri() {
        return ccpri;
    }

    public void setCcpri(String ccpri) {
        this.ccpri = ccpri;
    }

    public String getCcbbr() {
        return ccbbr;
    }

    public void setCcbbr(String ccbbr) {
        this.ccbbr = ccbbr;
    }

    public String getAltCode() {
        return altCode;
    }

    public void setAltCode(String altCode) {
        this.altCode = altCode;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

}
