package ru.rbt.barsgl.ejb.entity.dict.AccType;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;

/**
 * Created by akichigi on 30.09.16.
 */

@Entity
@Table(name = "GL_ACTSRC")
public class ActSrc  extends BaseEntity<Long> {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ACCTYPE")
    private  String acctype;

    @Column(name = "DEALSRC")
    private  String dealsrc;

    public ActSrc(){}

    public ActSrc(String acctype, String dealsrc){
        this.acctype = acctype;
        this.dealsrc = dealsrc;
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getAcctype() {
        return acctype;
    }

    public String getDealsrc() {
        return dealsrc;
    }
}
