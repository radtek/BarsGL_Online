package ru.rbt.barsgl.ejb.entity.dict.dwh;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;

/**
 * Created by er22317 on 08.02.2018.
 */
@Entity
@Table(name = "IMBCBCMP")
public class Filials  extends BaseEntity<String> implements Comparable<Filials>{

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

    @Transient
    private String ALT_CODE = "";

    public Filials(){}

    public Filials(String id, String ccpne, String ccpnr, String ccpri, String ccbbr){
        this.id = id;
        this.ccpne = ccpne;
        this.ccpnr = ccpnr;
        this.ccpri = ccpri;
        this.ccbbr = ccbbr;
    }

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

    public String getALT_CODE() {
        return ALT_CODE;
    }

    public void setALT_CODE(String ALT_CODE) {
        this.ALT_CODE = ALT_CODE;
    }

    public int compareTo(Filials filials){
        return this.getId().compareTo(filials.getId());
    }
}
