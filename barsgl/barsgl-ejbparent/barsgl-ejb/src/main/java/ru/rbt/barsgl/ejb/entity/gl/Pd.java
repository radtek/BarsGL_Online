package ru.rbt.barsgl.ejb.entity.gl;

import javax.persistence.*;

/**
 * Created by Ivan Sevastyanov on 05.02.2016.
 */
@Entity
@Table(name = "PD")
@SecondaryTables({
        @SecondaryTable(name = "PDEXT", pkJoinColumns = {@PrimaryKeyJoinColumn(name = "ID", referencedColumnName = "ID")}),
        @SecondaryTable(name = "PDEXT2", pkJoinColumns = {@PrimaryKeyJoinColumn(name = "ID", referencedColumnName = "ID")}),
        @SecondaryTable(name = "PDEXT3", pkJoinColumns = {@PrimaryKeyJoinColumn(name = "ID", referencedColumnName = "ID")}),
        @SecondaryTable(name = "PDEXT5", pkJoinColumns = {@PrimaryKeyJoinColumn(name = "ID", referencedColumnName = "ID")}),
})
public class Pd extends AbstractPd {

}
