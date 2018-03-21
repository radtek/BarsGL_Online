package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.acc.AcDNJournal;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.ejbcore.util.StringUtils;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.util.Date;

/**
 * Created by ER22228 30.03.2016
 */
@Stateless
@LocalBean
public class AcDNJournalRepository extends AbstractBaseEntityRepository<AcDNJournal, Long> {

    /**
     * Создает запись с ответом на запрос создания записи в журнале
     */
    public Long createJournalEntry(AcDNJournal.Sources source, String message) {
        AcDNJournal journal = new AcDNJournal();
        journal.setMessage(message);
        journal.setSource(source);
        journal.setStatusDate(new Date());
        journal.setStatus(AcDNJournal.Status.RAW);
        journal = save(journal);
        return journal.getId();
    }

    public void updateLogStatus(Long jId, AcDNJournal.Status status, String errorMessage) {
        AcDNJournal journal = findById(AcDNJournal.class, jId);
        journal.setComment(StringUtils.substr(errorMessage, 255));
        journal.setStatusDate(new Date());
        journal.setStatus(status);
        update(journal);
    }

    public void updateLogStatus(Long jId, AcDNJournal.Status status) {
        AcDNJournal journal = findById(AcDNJournal.class, jId);
        journal.setStatusDate(new Date());
        journal.setStatus(status);
        update(journal);
    }

    public void updateComment(Long jId, String errorMessage) {
        AcDNJournal journal = findById(AcDNJournal.class, jId);
        journal.setComment(StringUtils.substr(errorMessage, 255));
        journal.setStatusDate(new Date());
        update(journal);
    }

    public boolean finalizeOnException(Long jId) {
        if(jId==0L) {
            return false;
        }

        AcDNJournal journal = findById(AcDNJournal.class, jId);

        if(journal!=null) {
            if (journal.getStatus().equals(AcDNJournal.Status.ERROR) || journal.getStatus().equals(AcDNJournal.Status.PROCESSED)) {
                return false;
            }

            journal.setComment("Ошибка при обработке сообщения (см. GL_ACDENO id=" + jId + ")");
            journal.setStatusDate(new Date());
            journal.setStatus(AcDNJournal.Status.ERROR);
            update(journal);
            return true;
        }
        return false; // todo
    }
}
