package ru.rbt.barsgl.ejb.entity.lg;

import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by Ivan Sevastyanov on 18.10.2016.
 */
@Entity
@Table(name = "GL_LONGTSKPTRN")
public class LongRunningTaskPattern extends BaseEntity<Long> {

    @Id
    @Column(name = "ID_PATTERN")
    private Long id;

    @Column(name = "TSKNAME")
    private String taskName;

    @Override
    public Long getId() {
        return id;
    }

    public String getTaskName() {
        return taskName;
    }
}
