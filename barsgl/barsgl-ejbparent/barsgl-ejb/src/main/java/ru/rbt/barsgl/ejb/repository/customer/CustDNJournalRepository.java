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

    public int updateLogStatus(Long jId, CustDNJournal.Status status) {
        return updateLogStatus(jId, status, null);
    }

    public int updateLogStatus(Long jId, CustDNJournal.Status status, String errorMessage) {
        return executeUpdate("update CustDNJournal j set j.comment = ?1, j.statusDate = ?2, j.status = ?3 where j.id = ?4" ,
                errorMessage, new Date(), status, jId);
    }


}
