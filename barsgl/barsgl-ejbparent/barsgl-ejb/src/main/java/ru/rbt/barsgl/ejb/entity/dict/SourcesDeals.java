/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.ejb.entity.dict;

import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.barsgl.shared.HasValue;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Andrew Samsonov
 */
@Entity
@Table(name = "GL_SRCPST")
public class SourcesDeals extends BaseEntity<String> {

    public enum SRCPST implements HasValue <String> {
        PH("PH"), KTP("K+TP");

        private final String value;

        SRCPST(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public void setValue(String value) {}
    }

    @Id
    @Column(name = "ID_SRC", nullable = false, length = 64)
    private String id;

    @Column(name = "SHNM", nullable = false, unique = true, length = 3)
    private String shortName;

    @Column(name = "LGNM", nullable = false, length = 255)
    private String longName;

    @Column(name = "FL_DEALID", nullable = false, length = 1)
    private String flDealId;

    public SourcesDeals() {
    }

    public SourcesDeals(String id, String shortName, String longName) {
        this.id = id;
        this.shortName = shortName;
        this.longName = longName;
    }

    public SourcesDeals(String id, String shortName, String longName,String flDealId) {
        this.id = id;
        this.shortName = shortName;
        this.longName = longName;
        this.flDealId=flDealId;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public String getFlDealId() {
        return flDealId;
    }

    public void setFlDealId(String flDealId) {
        this.flDealId = flDealId;
    }
}
