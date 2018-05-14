package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.acc.AcDNJournal;
import ru.rbt.barsgl.ejb.entity.acc.AcDNJournalData;
import ru.rbt.barsgl.ejb.repository.dict.CurrencyCacheRepository;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import java.sql.SQLException;

import static java.lang.String.format;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by ER22228 30.03.2016
 */
@Deprecated
@Stateless
@LocalBean
public class AcDNJournalDataRepository extends AbstractBaseEntityRepository<AcDNJournalData, Long> {

    @EJB
    private CurrencyCacheRepository bankCurrencyRepository;

/*
    public String selectMidasBranchByBranch(String branch) throws SQLException {
        //dataRecord = selectFirst("SELECT * FROM DWH.IMBCBBRP WHERE A8BRCD = (SELECT MIDAS_BRANCH FROM DWH.DH_BR_MAP WHERE FCC_BRANCH=?)", accountList.getAccountDetails().get(0).getBranch());
        try {
            DataRecord dataRecord = selectOne("SELECT MIDAS_BRANCH FROM DH_BR_MAP WHERE FCC_BRANCH=?", branch);
            return dataRecord.getString(0);
        } catch (NoResultException e) {
            return null;
        }
    }
*/

    public void updateJournalData(Long jId, String midasBranch, String pseudoAcid) {
        AcDNJournalData data = findById(AcDNJournalData.class, jId);
        data.setMidasBranch(midasBranch);
        data.setPseudoAcid(pseudoAcid);// todo убрать
        data.setAccountNo(pseudoAcid);
        data.setSpecial("0000"); // Для сообщений из Flex всегда 0000
        update(data);

    }

    public Short findCustomerType(Long customerNo) throws SQLException {
        DataRecord dataRecord = selectFirst("SELECT BXCTYP FROM SDCUSTPD WHERE BBCUST = ?", format("%08d", customerNo));
        if (dataRecord != null && dataRecord.getShort(0) != null) {
            return dataRecord.getShort(0);
        }
        return 0;
    }

    public Short findCustomerType(Long customerNo, String acc2) throws SQLException {
        DataRecord dataRecord = selectFirst("SELECT BXCTYP FROM SDCUSTPD WHERE BBCUST = ?", format("%08d", customerNo));
        if (dataRecord != null && dataRecord.getShort(0) != null) {
            return dataRecord.getShort(0);
        }

        dataRecord = selectFirst("SELECT CTYPE FROM GL_SVTYPNO WHERE ACC2 = ?", acc2);
        if (dataRecord != null && dataRecord.getShort(0) != null) {
            return dataRecord.getShort(0);
        }
        return 0;
    }

    public String findCCode(String branch) throws SQLException {
        DataRecord dataRecord = selectFirst("SELECT BCBBR FROM IMBCBBRP WHERE A8BRCD = ?", branch);
        if (dataRecord != null && !isEmpty(dataRecord.getString(0))) {
            return dataRecord.getString(0);
        }
        return "";
    }

    public String findPsav(String acc2) throws SQLException {
        DataRecord dataRecord = selectFirst("SELECT PSAV FROM BSS WHERE ACC2 = ?", acc2);
        if (dataRecord != null && !isEmpty(dataRecord.getString(0))) {
            return dataRecord.getString(0);
        }
        return "";
    }

//    public String existsAcid(String acid) throws SQLException {
//        try {
//            DataRecord dataRecord = selectOne("SELECT ID FROM ACC WHERE ID = ?", acid);
//            return dataRecord.getString(0);
//        } catch (NoResultException e) {
//            return null;
//        }
//    }

    public boolean validateBranch(AcDNJournal.Sources source, String branch) throws SQLException {
        DataRecord dataRecord;
        if (source.equals(AcDNJournal.Sources.MIDAS_OPEN)) {
            dataRecord = selectFirst("SELECT * FROM IMBCBBRP WHERE A8BRCD=?", branch);
        } else {
            dataRecord = selectFirst("SELECT * FROM IMBCBBRP WHERE A8BRCD = (SELECT MIDAS_BRANCH FROM DH_BR_MAP WHERE FCC_BRANCH=?)", branch);
        }

        return dataRecord != null;
    }

    public boolean validateCcy(String ccy) throws SQLException {
        return bankCurrencyRepository.findCached(ccy) != null;
    }

}

