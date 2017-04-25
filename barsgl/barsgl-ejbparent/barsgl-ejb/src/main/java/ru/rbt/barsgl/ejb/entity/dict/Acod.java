package ru.rbt.barsgl.ejb.entity.dict;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;

/**
 * Created by akichigi on 24.10.16.
 */

@Entity
@Table(name = "GL_ACOD")
@SequenceGenerator(name = "AcodIdSeq", sequenceName = "GL_ACOD_SEQ", allocationSize = 1)
public class Acod extends BaseEntity<Long> {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "AcodIdSeq")
    private Long id;

    @Column(name = "ACOD")
    private String acod;

    @Column(name = "ACC2DSCR")
    private String acc2dscr;

    @Column(name = "TYPE")
    private String type;

    @Column(name = "SQDSCR")
    private String sqdscr;

    @Column(name = "ENAME")
    private String ename;

    @Column(name = "RNAME")
    private String rname;

    public Acod(){}

    public Acod(String acod, String acc2dscr, String type, String sqdscr, String ename, String rname) {
        this.acod = acod;
        this.acc2dscr = acc2dscr;
        this.type = type;
        this.sqdscr = sqdscr;
        this.ename = ename;
        this.rname = rname;
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getAcod() {
        return acod;
    }
    public void setAcod(String acod){
        this.acod = acod;
    }

    public String getAcc2dscr() {
        return acc2dscr;
    }

    public void setAcc2dscr(String acc2dscr){
        this.acc2dscr = acc2dscr;
    }

    public String getType() {
        return type;
    }

    public void setType(String type){
        this.type = type;
    }

    public String getSqdscr() {
        return sqdscr;
    }

    public void setSqdscr(String sqdscr){
        this.sqdscr = sqdscr;
    }

    public String getEname() {
        return ename;
    }

    public void setEname(String ename){
        this.ename = ename;
    }

    public String getRname() {
        return rname;
    }

    public void setRname(String rname){
        this.rname = rname;
    }
}
