package ru.rbt.barsgl.ejb.integr.pst;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.entity.gl.*;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.barsgl.ejb.repository.MemorderRepository;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.shared.Assert;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.entity.gl.Memorder.CancelFlag.*;
import static ru.rbt.barsgl.ejb.entity.gl.Memorder.DocType.BANK_ORDER;
import static ru.rbt.barsgl.ejb.entity.gl.Memorder.DocType.MEMORDER;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.rsubstr;

/**
 * Created by Ivan Sevastyanov
 */
@Stateless
@LocalBean
public class MemorderController {

    private static final Logger log = Logger.getLogger(MemorderController.class);

    @EJB
    private MemorderRepository memorderRepository;

    @Inject
    private GLOperationRepository operationRepository;

    public String nextMemorderNumber(Date pod, String bsaAcid, boolean isStorno) {
        Assert.notEmpty(bsaAcid, "Передан пустой счет ЦБ");
        try {
            return memorderRepository.executeInNewTransaction(persistence -> nextId(pod, bsaAcid, isStorno));
        } catch (Exception e) {
            try {
                return memorderRepository.executeInNewTransaction(persistence -> nextId(pod, bsaAcid, isStorno));
            } catch (Exception e1) {
                throw new DefaultApplicationException(e1.getMessage(), e1);
            }
        }
    }

    private String nextId(Date pod, String bsaAcid, boolean isStorno) throws SQLException {
        final Memorder.CancelFlag cancelFlag = isStorno ? Y : N;
        final BalanceChapter balanceChapter = operationRepository.getBalanceChapter(bsaAcid);
        Assert.notNull(balanceChapter, format("Не удалось определить главу баланса по счету '%s'", bsaAcid));

        final String filial = rsubstr(operationRepository.getCompanyCode(bsaAcid), 3);
        int cnt = memorderRepository.executeNativeUpdate(
                "UPDATE GL_MOCOUNT SET COUNT = COUNT + 1 WHERE POD = ?1 and BSCHAPTER = ?2 and CBCC = ?3 and STORNO = ?4"
                , pod, balanceChapter.name(), filial, cancelFlag.name());
        Long nextId;
        if (0 == cnt) {
            memorderRepository.executeNativeUpdate(
                    "INSERT into GL_MOCOUNT (POD,BSCHAPTER,CBCC,STORNO,COUNT) values (?1,?2,?3,?4,?5)"
                    , pod, operationRepository.getBSChapter(bsaAcid), filial, cancelFlag.name(), getStartNumber(cancelFlag));
            nextId = getStartNumber(cancelFlag);
        } else {
            nextId = memorderRepository.selectFirst(
                    "SELECT COUNT FROM GL_MOCOUNT WHERE POD = ? AND BSCHAPTER = ? AND CBCC = ? AND STORNO = ?"
                    , pod, operationRepository.getBSChapter(bsaAcid), filial, cancelFlag.name()).getLong(0);
        }
        return format("G%s%s%s", balanceChapter.name(), filial, StringUtils.leftPad(Long.toString(nextId), 9, "0"));
    }

    public Memorder.DocType getDocType(GLPosting posting) {
        List<Pd> debits = findPd(true, posting.getPdList());
        Assert.isTrue(debits.size() >= 1, "Не найдена проводка по дебету");
        List<Pd> credits = findPd(false, posting.getPdList());
        Assert.notNull(credits.size() >= 1, "Не найдена проводка по кредиту");
        return getDocType(posting.getOperation().isCorrection(), debits, credits, debits.get(0).getPod());
    }

    /**
     * Тип мемордера
     * @param isStorno признак сторно
     * @param debits счет по дебету
     * @param credits счет по кредиту
     * @param pod дата проводки
     * @return тип
     */
    public Memorder.DocType getDocType(boolean isStorno, List<? extends AbstractPd> debits, List<? extends AbstractPd> credits, Date pod) {
        if (isStorno) {
            return MEMORDER;
        } else {
            Assert.isTrue(debits.size() >= 1, "Не найдена проводка по дебету");
            Assert.notNull(credits.size() >= 1, "Не найдена проводка по кредиту");
            try {
                for (AbstractPd debit : debits) {
                    for (AbstractPd credit : credits) {
                        if (isMaskExists(debit.getBsaAcid(), credit.getBsaAcid(), pod)) {
                            return BANK_ORDER;
                        }
                    }
                }
                return MEMORDER;
            } catch (SQLException e) {
                throw new DefaultApplicationException(e.getMessage(), e);
            }
        }
    }

    private boolean isMaskExists(String accountDebit, String accountCredit, Date pod) throws SQLException {
        List<DataRecord> filtered = memorderRepository.select(
                "select * from F067_MASK m where dt_mask like ? and ct_mask like ? and ? between dat and datto"
                , accountDebit.substring(0, 3) + "%", accountCredit.substring(0, 3) + "%", pod);
        DataRecord record = filtered.stream().filter(dataRecord ->
                accountDebit.matches(dataRecord.getString("dt_mask").trim().replace("_", ".").replace("%", ".*"))
                        && accountCredit.matches(dataRecord.getString("ct_mask").trim().replace("_", ".").replace("%", ".*")))
                .findAny().orElse(null);
        return null != record;
    }

    public Memorder.DocType getDocTypeNotFan(boolean isStorno, String accountDebit, String accountCredit, Date pod) {
        try {
            if (isStorno) {
                return MEMORDER;
            } else {
                return isMaskExists(accountDebit, accountCredit, pod) ? BANK_ORDER : MEMORDER;
            }
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private Long getStartNumber(Memorder.CancelFlag cancelFlag) {
        return N == cancelFlag ? 1L : 900_000_000L;
    }

    private List<Pd> findPd(final boolean debit, List<Pd> pds) {
        return pds.stream().filter(pd -> debit ? pd.getAmountBC() < 0 : pd.getAmountBC() > 0).collect(Collectors.toList());
    }
}
