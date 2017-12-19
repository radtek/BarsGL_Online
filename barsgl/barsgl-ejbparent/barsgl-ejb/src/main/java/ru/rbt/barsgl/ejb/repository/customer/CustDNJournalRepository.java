package ru.rbt.barsgl.ejb.repository.customer;

import ru.rbt.barsgl.ejb.entity.acc.AcDNJournal;
import ru.rbt.barsgl.ejb.entity.acc.AclirqJournal;
import ru.rbt.barsgl.ejb.entity.cust.CustDNJournal;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by er18837 on 12.12.2017.
 */
@Stateless
@LocalBean
public class CustDNJournalRepository  extends AbstractBaseEntityRepository<CustDNJournal, Long> {

    public Long createJournalEntry(String message) {
        CustDNJournal journal = new CustDNJournal();
        journal.setMessage(message);
        journal.setStatus(CustDNJournal.Status.RAW);
        journal = save(journal);
        return journal.getId();
    }

    public CustDNJournal updateLogStatus(Long jId, CustDNJournal.Status status) {
        return updateLogStatus(jId, status, null);
    }

    public CustDNJournal updateLogStatus(Long jId, CustDNJournal.Status status, String errorMessage) {
        CustDNJournal journal = findById(CustDNJournal.class, jId);
        journal.setComment(errorMessage);
        journal.setStatusDate(new Date());
        journal.setStatus(status);
        return update(journal);
    }


}
