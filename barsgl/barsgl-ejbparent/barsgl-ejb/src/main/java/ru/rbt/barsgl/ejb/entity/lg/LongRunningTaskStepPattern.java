package ru.rbt.barsgl.ejb.entity.lg;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;

/**
 * Created by Ivan Sevastyanov on 18.10.2016.
 */
@Entity
@Table(name = "GL_LONGTSKSTEPPTRN")
public class LongRunningTaskStepPattern extends BaseEntity<Long> {

    @Id
    @Column(name = "ID_STEP")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PATTERN")
    private LongRunningTaskPattern pattern;

    @Column(name = "STEPNAME")
    private String stepName;

    @Column(name = "FRACTION")
    private Integer fraction;

    @Override
    public Long getId() {
        return id;
    }

    public LongRunningTaskPattern getPattern() {
        return pattern;
    }

    public String getStepName() {
        return stepName;
    }

    public Integer getFraction() {
        return fraction;
    }
}
