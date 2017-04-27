package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.gl.BackvalueJournal;
import ru.rbt.barsgl.ejb.entity.gl.BackvalueJournalId;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static ru.rbt.barsgl.ejb.entity.gl.BackvalueJournal.BackvalueJournalState.NEW;

/**
 * Created by Ivan Sevastyanov
 */
@Stateless
@LocalBean
public class BackvalueJournalRepository extends AbstractBaseEntityRepository<BackvalueJournal, BackvalueJournalId> {

    private static final Logger logger = Logger.getLogger(BackvalueJournalRepository.class.getName());

    @EJB
    private OperdayController operdayController;

    @EJB
    private CoreRepository coreRepository;
    /**
     * Регистрация проводок backvalue в журнале для последующей обработки в PRE_COB
     * @param pstList
     * @throws Exception
     */
    public void registerBackvalueJournal(List<GLPosting> pstList) throws Exception {
        for (GLPosting glPosting : pstList) {
            for (Pd pd : glPosting.getPdList()) {
                if (pd.getPod().before(operdayController.getOperday().getCurrentDate())) {
                    registerBackvalueJournalAcc(pd.getBsaAcid(), pd.getAcid(), pd.getPod());
                }
            }
        }
    }

    public void registerBackvalueJournalAcc(String bsaAcid, String acid, Date pod) throws Exception {
        final BackvalueJournalId journalId = new BackvalueJournalId(acid, bsaAcid, pod);
        try {
            insertJournalRecordNewTransaction(journalId);
        } catch (Throwable e) {
            insertJournalRecordNewTransaction(journalId);
        }
    }

    private BackvalueJournal insertJournalRecordNewTransaction(BackvalueJournalId recordId) throws Exception {
        return (BackvalueJournal) coreRepository.executeInNewTransaction(persistence1 -> {
            BackvalueJournal journal = findById(BackvalueJournal.class, recordId);
            if (null == journal) {
                journal = new BackvalueJournal(recordId);
                journal.setState(NEW);
                return save(journal);
            } else if(!NEW.equals(journal.getState())) {
                journal.setState(NEW);
                return save(journal);
            } else {
                return journal;
            }
        });
    }

}
