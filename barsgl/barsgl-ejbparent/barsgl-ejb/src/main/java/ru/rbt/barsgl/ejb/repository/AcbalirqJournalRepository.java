package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.acc.AcbalirqJournal;
import ru.rbt.barsgl.ejb.entity.acc.AcbalirqJournal;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by ER22228 16.05.2016
 */
@Stateless
@LocalBean
public class AcbalirqJournalRepository extends AbstractBaseEntityRepository<AcbalirqJournal, Long> {

    /**
     * Создает запись с ответом на запрос создания записи в журнале
     */
    public Long createJournalEntry(String request) {
        AcbalirqJournal journal = new AcbalirqJournal();
        journal.setRequestId("");
        journal.setRequest(request);
        journal.setStatusDate(new Timestamp(new Date().getTime()));
        journal.setStatus(AcbalirqJournal.Status.RAW);
        journal = save(journal);
        return journal.getId();
    }

    public void updateLogStatus(Long jId, AcbalirqJournal.Status status, String errorMessage) {
        AcbalirqJournal journal = findById(AcbalirqJournal.class, jId);
        journal.setComment(errorMessage);
        journal.setStatusDate(new Timestamp(new Date().getTime()));
        journal.setStatus(status);
        update(journal);
    }

    public boolean finalizeOnException(Long jId) {
        if(jId==0L) {
            return false;
        }

        AcbalirqJournal journal = findById(AcbalirqJournal.class, jId);

        if(journal.getStatus().equals(AcbalirqJournal.Status.ERROR) || journal.getStatus().equals(AcbalirqJournal.Status.PROCESSED)) {
            return false;
        }

        journal.setComment("Ошибка выполнения");
        journal.setStatusDate(new Timestamp(new Date().getTime()));
        journal.setStatus(AcbalirqJournal.Status.ERROR);
        update(journal);
        return true;
    }
}
