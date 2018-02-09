package ru.rbt.barsgl.ejb.entity.dict.dwh;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by er22317 on 08.02.2018.
 */
@Entity
@Table(name = "IMBCBCMP")
public class Filials  extends BaseEntity<String> {

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

    @Override
    public String getId() {
        return id;
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
}
