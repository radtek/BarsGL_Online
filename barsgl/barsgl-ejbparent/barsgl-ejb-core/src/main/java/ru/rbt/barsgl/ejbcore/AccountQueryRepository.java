package ru.rbt.barsgl.ejbcore;

import org.apache.log4j.Logger;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.ejbcore.util.StringUtils;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by ER22228
 */
@Stateless
@LocalBean
public class AccountQueryRepository extends AbstractBaseEntityRepository {
    private static final Logger log = Logger.getLogger(AccountQueryRepository.class);

    public List<DataRecord> getCountsByAccount(Set<String> acids) throws Exception {
        try {
            String acidsStr = "'" + StringUtils.listToString(acids, "','") + "'";
            List<DataRecord> dataRecords = selectMaxRows(
                "SELECT A.*, PKG_ACC.GET_BIC(A.BSAACID) BIC FROM GL_ACC A WHERE "
                        + "A.BSAACID IN (" + acidsStr + ") "
                        + "AND A.ACCTYPE NOT IN (999999999, 361070100, 0) "
                        //+ "AND A.DTC IS NULL "
                        //+ "AND (CURRENT DATE - VALUE(A.DTC,'2029-01-01')) <= 1131 "
                        + "AND MONTHS_BETWEEN(sysdate, NVL(A.DTC, TO_DATE('2029-01-01','YYYY-DD-MM'))) < 12 "
                , Integer.MAX_VALUE, null);
            return dataRecords;            
        } catch (SQLException e) {
            throw new Exception(e);
        }
    }

    public List<DataRecord> getCountsByAcod(String customerNo, List<String> accountSpecials) throws Exception {
        try {
            accountSpecials = accountSpecials.stream().filter(accountSpecial -> accountSpecial.matches("\\d+")).collect(Collectors.toList());
            
            if(accountSpecials.isEmpty()){
              throw new Exception(format(
                      "Элемент AccountSpecials содержит некорректные данные. Ожидалось \\d+, получено '%s'"
                , accountSpecials.stream().collect(Collectors.joining(";"))));
            }

            String glacods = "'" + StringUtils.listToString(accountSpecials, "','") + "'";

            List<DataRecord> dataRecords = selectMaxRows(
                "SELECT A.*, PKG_ACC.GET_BIC(A.BSAACID) BIC FROM GL_ACC A WHERE "
                        + "A.CUSTNO=? "
                        + "AND A.ACOD IN (" + glacods + ") "
                        + "AND A.ACCTYPE NOT IN (999999999, 361070100, 0) "
                        + "AND NOT (A.OPENTYPE = 'ERR' AND A.DTO = A.DTC) "
                        + "AND MONTHS_BETWEEN(sysdate, NVL(A.DTC, TO_DATE('2029-01-01','YYYY-DD-MM'))) < 12 "
                , Integer.MAX_VALUE, new Object[]{customerNo});
              return dataRecords;
        } catch (SQLException e) {
            throw new Exception(e);
        }
    }

    public List<DataRecord> getCountsByAcctype(String customerNo, List<String> accountTypes) throws Exception {
        try {
            accountTypes = accountTypes.stream().filter(accountType -> accountType.matches("\\d+")).collect(Collectors.toList());
            
            if(accountTypes.isEmpty()){
              throw new Exception(format(
                      "Элемент AccountingType содержит некорректные данные. Ожидалось \\d{9}, получено '%s'"
                , accountTypes.stream().collect(Collectors.joining(";"))));
            }
            
            String acctypes = StringUtils.listToString(accountTypes, ",");

            List<DataRecord> dataRecords = selectMaxRows(
                "SELECT A.*, PKG_ACC.GET_BIC(A.BSAACID) BIC FROM GL_ACC A WHERE "
                        + "A.CUSTNO=? "
                        + "AND A.ACCTYPE IN (" + acctypes + ") "
                        + "AND A.ACCTYPE NOT IN (999999999, 361070100, 0) "
                        + "AND NOT (A.OPENTYPE = 'ERR' AND A.DTO = A.DTC) "
                        + "AND MONTHS_BETWEEN(sysdate, NVL(A.DTC, TO_DATE('2029-01-01','YYYY-DD-MM'))) < 12 "
                , Integer.MAX_VALUE, new Object[]{customerNo});

            return dataRecords;
        } catch (SQLException e) {
            throw new Exception(e);
        }
    }

    public List<DataRecord> getCountsByCustomerNoOnly(String customerNo) throws Exception {
        try {
            List<DataRecord> dataRecords = null;
            dataRecords = selectMaxRows(
                "SELECT A.*, PKG_ACC.GET_BIC(A.BSAACID) BIC FROM GL_ACC A WHERE "
                        + "A.CUSTNO=? "
                        + "AND A.ACCTYPE NOT IN (999999999, 361070100, 0) "
                        + "AND NOT (A.OPENTYPE = 'ERR' AND A.DTO = A.DTC) "
                        + "AND MONTHS_BETWEEN(sysdate, NVL(A.DTC, TO_DATE('2029-01-01','YYYY-DD-MM'))) < 12 "
                    ,Integer.MAX_VALUE, new Object[]{customerNo});
            return dataRecords;
        } catch (SQLException e) {
            throw new Exception(e);
        }
    }

    public List<DataRecord> getGlAccRecords(String inCondition, String customerNo) throws Exception {
        try {
            String selectExpression = "SELECT * FROM GL_ACC WHERE BSAACID IN (" + inCondition + ") and ACCTYPE <> 0";
            if(customerNo != null)
              selectExpression+=" AND CUSTNO = '" + customerNo + "'";
            return selectMaxRows(selectExpression, Integer.MAX_VALUE, null);
        } catch (SQLException e) {
            throw new Exception(e);
        }
    }

    public void loadCurrency(Map<String, String> currencyMap, Map<String, Integer> currencyNBDPMap) throws Exception {
        try {
            List<DataRecord> dataRecords = selectMaxRows("SELECT GLCCY,CBCCY,NBDP FROM CURRENCY", Integer.MAX_VALUE, null);
            for (DataRecord item : dataRecords) {
                currencyMap.put(item.getString("CBCCY"), item.getString("GLCCY"));
                currencyNBDPMap.put(item.getString("CBCCY"), item.getInteger("NBDP"));
            }
        } catch (SQLException e) {
            throw new Exception(e);
        }
    }

    public String getBranchByBsaacidorAcid(String bsaacid, String acid, Date workday) throws Exception { //todo код БРАНЧА Midas
        try {
            DataRecord record = selectFirst(
//                "SELECT BRANCH FROM DH_ACC_INF WHERE CBRF_ACC_NUMBER=? AND DAT=? AND DATTO=?", bsaacid, workday, workday);
                    "SELECT /*+ INDEX(D DH_ACC) */ BRANCH FROM DH_ACC_INF D WHERE \n" +
                            "CBRF_ACC_NUMBER=? AND DAT <= ? AND DATTO >= ?", bsaacid, workday, workday);

            if (record != null && !isEmpty(record.getString("BRANCH"))) {
                return record.getString("BRANCH");
            }
        } catch (SQLException e) {
            log.error("",e);
        }

        String vAcid = null;
        if (acid.length() == 20) {
          vAcid = acid.substring(acid.length() - 3);
        }else if (acid.length() == 3){
          vAcid = acid;
        }
        
        if(vAcid != null)
          return convertBranchToFcc(vAcid);
        
        return "";
    }

    public String getAnam(String acid, String bsaacid, Date workday) {
        try {
            DataRecord record = selectFirst(
                "SELECT ACCOUNT_NAME FROM DH_ACC_INF WHERE CBRF_ACC_NUMBER=? AND DAT=? AND DATTO=?", bsaacid, workday, workday);
            if (record != null && !isEmpty(record.getString("ACCOUNT_NAME"))) {
                return record.getString("ACCOUNT_NAME");
            }
        } catch (SQLException e) {
            log.error("",e);
        }

/*
        try {
            DataRecord record = selectFirst("SELECT ANAM FROM ACC WHERE ID=?", acid);
            if (record != null && !isEmpty(record.getString("ANAM"))) {
                return record.getString("ANAM");
            }
        } catch (SQLException e) {
            log.error("",e);
        }
*/
        return "";
    }

    /**
     * входящие и исходящие остатки
     * @param bsaacid
     * @return <code>new BigDecimal[]{record.getBigDecimal("INCO"), record.getBigDecimal("INCORUB"), record.getBigDecimal("OUTCO"), record.getBigDecimal("OUTRUB")}</code>
     */
    public BigDecimal[] getAccountBalance(String bsaacid) throws Exception {
        try {
            DataRecord record = selectFirst(
                            "select   coalesce(sum(case when b.dat < o.curdate then OBAC+CTAC+DTAC " +
                            "                                                  else OBAC end), 0) INCO\n" +
                            "       , coalesce(sum(case when b.dat < o.curdate then OBBC+CTBC+DTBC " +
                            "                                                  else OBBC end), 0) INCORUB\n" +
                            "       , coalesce(sum(OBAC+CTAC+DTAC), 0) OUTCO\n" +
                            "       , coalesce(sum(OBBC+CTBC+DTBC), 0) OUTRUB\n" +
                            "from v_gl_baltur b, gl_od o\n" +
                            "    where b.bsaacid = ?"
                , bsaacid);
            if (record != null) {
                return new BigDecimal[]{record.getBigDecimal("INCO"), record.getBigDecimal("INCORUB"), record.getBigDecimal("OUTCO"), record.getBigDecimal("OUTRUB")};
            }
        } catch (SQLException e) {
            throw new Exception(e);
        }
        return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
    }

    public Set<String> getCountsByAB(String condition) throws Exception {
        try {
//            List<DataRecord> dataRecords = selectMaxRows(
//                "SELECT BSAACID FROM ACCRLN A WHERE NVL(A.BSAACID,'')<>'' AND " + condition + " "+
//                    "AND EXISTS(SELECT * FROM GL_ACC G WHERE A.BSAACID=G.BSAACID) " +
//                    //"AND (CURRENT DATE - A.DRLNC) <= 1131"
//                    "AND MONTHS_BETWEEN(sysdate, A.DRLNC) < 12 "
//                , Integer.MAX_VALUE,null);
            List<DataRecord> dataRecords = selectMaxRows(
                    "SELECT BSAACID FROM GL_ACC A WHERE MONTHS_BETWEEN(sysdate, A.DTC) < 12 AND " + condition
                    , Integer.MAX_VALUE,null);

            Set<String> result = dataRecords.stream().map(item -> item.getString(0)).collect(Collectors.toSet());
            return result;
        } catch (SQLException e) {
            throw new Exception(e);
        }
    }

    public String convertBranchToFcc(String branch) throws Exception {
        try {
            List<DataRecord> records = select("SELECT FCC_BRANCH FROM DH_BR_MAP WHERE MIDAS_BRANCH=? ORDER BY FCC_BRANCH", branch);
            if (null == records)
                return "";
            if (records.size() > 1) {
                for (DataRecord item : records) {
                    String itemStr = item.getString("FCC_BRANCH");
                    if(!isEmpty(itemStr)) {
                        char last = itemStr.charAt(itemStr.length() - 1);
                        if (last >= '0' && last <= '9') {
                            return itemStr;
                        }
                    }
                }
                // Если все буквенные, то любой, то есть первый
                return records.get(0).getString("FCC_BRANCH");
            } else if (records.size() == 1) {
                return records.get(0).getString("FCC_BRANCH");
            }
        } catch (SQLException e) {
            throw new Exception(e);
        }
        return "";
    }
}
