package ru.rbt.barsgl.ejb.entity.gl;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov
 * Журнал проводок backvalue
 */
@Entity
@Table(name = "GL_BVJRNL")
public class BackvalueJournal extends BaseEntity<BackvalueJournalId>{

    public enum BackvalueJournalState {
        NEW, LOCAL, PROCESSED, ERROR_LC, ERROR_BL
    }

    public BackvalueJournal() {
    }

    public BackvalueJournal(String acid, String bsaAcid, Date postingDate) {
        final BackvalueJournalId id = new BackvalueJournalId(acid, bsaAcid, postingDate);
        this.id = id;
    }

    public BackvalueJournal(BackvalueJournalId id) {
        this.id = id;
    }

    @EmbeddedId
    private BackvalueJournalId id;

    @Column(name = "STATE")
    @Enumerated(EnumType.STRING)
    private BackvalueJournalState state;

    @Override
    public BackvalueJournalId getId() {
        return id;
    }

    @Override
    public void setId(BackvalueJournalId id) {
        this.id = id;
    }

    public BackvalueJournalState getState() {
        return state;
    }

    public void setState(BackvalueJournalState state) {
        this.state = state;
    }
}
