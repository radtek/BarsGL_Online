package ru.rbt.barsgl.ejb.entity.dict;

import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;
import ru.rbt.barsgl.shared.enums.BoolType;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by akichigi on 04.08.16.
 */
@Entity
@Table(name = "GL_PRFCNTR")
public class ProfitCenter extends BaseEntity<String> {

    @Id
    @Column(name = "PRFCODE")
    private String id;

    @Column(name = "PRFNAME")
    private String name;

    @Column(name = "LDATE")
    @Temporal(TemporalType.DATE)
    private Date date;

    @Override
    public String getId() {
        return id;
    }

    @Column(name = "CLOSED")
    @Enumerated(EnumType.STRING)
    private BoolType closed;

    public ProfitCenter(){}

    public ProfitCenter(String id, String name, Date date, BoolType closed){
        this.id = id;
        this.name = name;
        this.date = date;
        this.closed = closed;
    }

    @Override
    public void setId(String id){
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public BoolType getClosed() {
        return closed;
    }

    public void setClosed(BoolType closed) {
        this.closed = closed;
    }
}
