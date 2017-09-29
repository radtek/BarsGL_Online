package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.common.mapping.od.BankCalendarDay;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.entity.acc.BsaAcc;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.ejbcore.validation.ValidationError;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Date;
import java.util.Optional;

import static ru.rbt.ejbcore.validation.ErrorCode.ACCOUNT_BSA_INVALID;

/**
 * Created by ER18837 on 05.05.15.
 */
public class BsaAccRepository extends AbstractBaseEntityRepository<BsaAcc, String> {

    @Inject
    private BankCalendarDayRepository calendarDayRepository;

    public BsaAcc createBsaAcc(GLAccount glAccount) {
        BsaAcc bsaAcc = new BsaAcc();
        bsaAcc.setId(glAccount.getBsaAcid());
        bsaAcc.setBssAccount(glAccount.getBsaAcid().substring(0, 5));
        bsaAcc.setCurrencyD(glAccount.getCurrency().getDigitalCode());
        bsaAcc.setBsaKey(bsaAcc.getId().substring(8, 9));       // символ в позиции 9
        bsaAcc.setCompanyCode(glAccount.getCompanyCode());
        bsaAcc.setBsaCode(glAccount.getBsaAcid().substring(13, 20));  // последние 7 символов
        bsaAcc.setDateOpen(glAccount.getDateOpen());
        bsaAcc.setDateClose(glAccount.getDateCloseNotNull());
        bsaAcc.setPassiveActive(glAccount.getPassiveActive());
        bsaAcc.setBsaGroup(glAccount.getRelationType());
        bsaAcc.setDateContractOpen(glAccount.getDateOpen());
        bsaAcc.setCustomerNumber(("00000000" + glAccount.getCustomerNumber()).substring(glAccount.getCustomerNumber().length()));


        Date dateTax = glAccount.getDateOpen();
        for (int i = 0; i<3; i++) {                 // open + 3 work day
            BankCalendarDay day = calendarDayRepository.getWorkdayAfter(dateTax);
            if (day == null) throw new DefaultApplicationException("Невозможно открыть счет. Ошибка в данных календаря.");
/*        for (int i = 0; i<3; i++) {                 // open + 3 work day
            dateTax = calendarDayRepository.getWorkdayAfter(dateTax).getId().getCalendarDate();
        }
*/
        try {
            dateTax = calendarDayRepository.getWorkDateAfter(glAccount.getDateOpen(), 3, false);
        } catch (SQLException e) {
            e.printStackTrace();    // TODO !!!
        }
        bsaAcc.setDateTax(dateTax);

        bsaAcc.setDescription("");
        bsaAcc.setBsaSubtype("");

        save(bsaAcc);
        return bsaAcc;
    }

    /**
     * Обновляет дату закрытия счета
     * @param glAccount
     */
    public void setDateOpen(GLAccount glAccount) {
        int cnt = executeNativeUpdate("update BSAACC set BSAACO = ? where ID = ?", glAccount.getDateOpen(), glAccount.getBsaAcid());
        if (1 != cnt) {
            throw new ValidationError(ACCOUNT_BSA_INVALID, glAccount.getBsaAcid());
        }
    }

    /**
     * Обновляет дату закрытия счета
     * @param glAccount
     */
    public void setDateClose(GLAccount glAccount) {
        int cnt = executeNativeUpdate("update BSAACC set BSAACC = ? where ID = ?", glAccount.getDateCloseNotNull(), glAccount.getBsaAcid());
        if (1 != cnt) {
            throw new ValidationError(ACCOUNT_BSA_INVALID, glAccount.getBsaAcid());
        }
    }

    public Optional<BsaAcc> findBsaAcc (String bsaacid) {
        return Optional.ofNullable(selectFirst(BsaAcc.class, "from BsaAcc a where a.id = ?1", bsaacid));
    }
}
