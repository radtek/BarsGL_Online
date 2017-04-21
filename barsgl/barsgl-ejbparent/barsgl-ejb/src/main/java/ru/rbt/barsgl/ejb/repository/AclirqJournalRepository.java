package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.acc.AclirqJournal;
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
public class AclirqJournalRepository extends AbstractBaseEntityRepository<AclirqJournal, Long> {

    /**
     * Создает запись с ответом на запрос создания записи в журнале
     */
    public Long createJournalEntry(String param, String request) {
        // param добавить в таблицу
        AclirqJournal journal = new AclirqJournal();
        journal.setRequestId("");
        journal.setRequest(request);
        journal.setStatusDate(new Timestamp(new Date().getTime()));
        journal.setStatus(AclirqJournal.Status.RAW);
        journal.setComment("");
        journal = save(journal);
        return journal.getId();
    }

    public AclirqJournal updateLogStatus(Long jId, AclirqJournal.Status status, String errorMessage) {
        AclirqJournal journal = findById(AclirqJournal.class, jId);
        journal.setComment(errorMessage);
        journal.setStatusDate(new Timestamp(new Date().getTime()));
        journal.setStatus(status);
        return update(journal);
    }

    public AclirqJournal updateLogStatus(Long jId, AclirqJournal.Status status, String errorMessage, String outMessage) {
        AclirqJournal journal = findById(AclirqJournal.class, jId);
        journal.setOutMessage(outMessage);
        journal.setComment(errorMessage);
        journal.setStatusDate(new Timestamp(new Date().getTime()));
        journal.setStatus(status);
        return update(journal);
    }
    
    public boolean finalizeOnException(Long jId) {
        if(jId==0L) {
            return false;
        }

        AclirqJournal journal = findById(AclirqJournal.class, jId);

        if(journal.getStatus().equals(AclirqJournal.Status.ERROR) || journal.getStatus().equals(AclirqJournal.Status.PROCESSED)) {
            return false;
        }

        journal.setComment("Ошибка выполнения");
        journal.setStatusDate(new Timestamp(new Date().getTime()));
        journal.setStatus(AclirqJournal.Status.ERROR);
        update(journal);
        return true;
    }
}
