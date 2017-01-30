package ru.rbt.barsgl.ejbcore;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.barsgl.ejbcore.util.StringUtils;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static ru.rbt.barsgl.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by ER22228
 */
@Stateless
@LocalBean
public class AccountQueryRepository extends AbstractBaseEntityRepository {
    private static final Logger log = Logger.getLogger(AccountQueryRepository.class);

    public Set<String> getCountsByAcid(String customerNo, List<String> acids) {
        try {
            String acidsStr = "'" + StringUtils.listToString(acids, ",") + "'";
            List<DataRecord> dataRecords = selectMaxRows(
                "SELECT BSAACID FROM DWH.ACCRLN A WHERE VALUE(BSAACID,'')<>'' AND ACID IN (" + acidsStr + ") "+
                    "AND EXISTS(SELECT * FROM DWH.GL_ACC G WHERE A.BSAACID=G.BSAACID AND G.RLNTYPE <> 1) " +
                    "AND (CURRENT DATE - DRLNC) <= 1131 "+
                    "AND A.RLNTYPE <> 1"
                , Integer.MAX_VALUE,null);

            Set<String> result = dataRecords.stream().map(item -> item.getString(0)).collect(Collectors.toSet());
            return result;
        } catch (NoResultException e) {
            return Collections.emptySet();
        } catch (SQLException e) {
            log.error("",e);
        } catch (Exception e) {
            log.error("",e);
        }
        return Collections.emptySet();
    }

    public Set<String> getCountsByAcod(String customerNo, List<String> accountSpecials) {
        try {
            String glacods = "'" + StringUtils.listToString(accountSpecials, "','") + "'";
            List<DataRecord> dataRecords = selectMaxRows(
                "SELECT BSAACID FROM DWH.ACCRLN A WHERE A.CNUM=? AND VALUE(A.BSAACID,'')<>'' AND A.GLACOD IN (" + glacods + ") "+
                    "AND EXISTS(SELECT * FROM DWH.GL_ACC G WHERE A.BSAACID=G.BSAACID AND G.RLNTYPE <> 1) " +
                    "AND (CURRENT DATE - A.DRLNC) <= 1131 " + 
                    "AND A.RLNTYPE <> 1"                        
                , Integer.MAX_VALUE, new Object[]{customerNo});

            Set<String> result = dataRecords.stream().map(item -> item.getString(0)).collect(Collectors.toSet());
            return result;
        } catch (NoResultException e) {
            return Collections.emptySet();
        } catch (SQLException e) {
            log.error("",e);
        } catch (Exception e) {
            log.error("",e);
        }
        return Collections.emptySet();
    }

    public Set<String> getCountsByAcctype(String customerNo, List<String> accountTypes) {
        try {
            accountTypes = accountTypes.stream().filter(accountSpecial -> accountSpecial.matches("[0-9]+")).collect(Collectors.toList());
            String acctypes = StringUtils.listToString(accountTypes, ",");

            List<DataRecord> dataRecords = selectMaxRows(
                "SELECT BSAACID FROM DWH.GL_ACC A WHERE A.CUSTNO=? AND VALUE(A.BSAACID,'')<>'' AND A.ACCTYPE IN (" + acctypes + ") "+
                    "AND EXISTS(SELECT * FROM DWH.GL_ACC G WHERE A.BSAACID=G.BSAACID AND G.RLNTYPE <> 1) " +
                    "AND (CURRENT DATE - VALUE(A.DTC,'2029-01-01')) <= 1131 " + 
                    "AND A.RLNTYPE <> 1"
                , Integer.MAX_VALUE, new Object[]{customerNo});

            Set<String> result = dataRecords.stream().map(item -> item.getString(0)).collect(Collectors.toSet());
            return result;
        } catch (NoResultException e) {
            return Collections.emptySet();
        } catch (SQLException e) {
            log.error("",e);
        } catch (Exception e) {
            log.error("",e);
        }
        return Collections.emptySet();
    }

    //    public Set<String> getCountsByCustomerNoOnly(String customerNo, List<String> accountingType) {
    public Set<String> getCountsByCustomerNoOnly(String customerNo) {
        try {
            List<DataRecord> dataRecords = null;
            dataRecords = selectMaxRows(
                "SELECT BSAACID FROM DWH.ACCRLN A WHERE CNUM=? AND VALUE(BSAACID,'')<>'' " +
                    "AND EXISTS(SELECT * FROM DWH.GL_ACC G WHERE A.BSAACID=G.BSAACID AND G.RLNTYPE <> 1) " +
                    "AND (CURRENT DATE - A.DRLNC) <= 1131 "+
                    "AND A.RLNTYPE <> 1"                    
                    ,Integer.MAX_VALUE, new Object[]{customerNo});
//            }

            Set<String> result = dataRecords.stream().map(item -> item.getString(0)).collect(Collectors.toSet());
            return result;
        } catch (NoResultException e) {
            return Collections.emptySet();
        } catch (SQLException e) {
            log.error("",e);
        } catch (Exception e) {
            log.error("",e);
        }
        return Collections.emptySet();
    }


    public List<DataRecord> getAccrlnRecords(String inCondition, String customerNo) {
        try {
            String selectExpression = "SELECT * FROM DWH.ACCRLN A WHERE A.BSAACID IN (" + inCondition + ")";
            if(customerNo != null)
              selectExpression+=" AND CNUM = '"+customerNo+"'";
            return selectMaxRows(selectExpression, Integer.MAX_VALUE, null);
        } catch (NoResultException e) {
            return Collections.emptyList();
        } catch (SQLException e) {
            log.error("",e);
        } catch (Exception e) {
            log.error("",e);
        }
        return Collections.emptyList();
    }

    public List<DataRecord> getGlAccRecords(String inCondition, String customerNo) {
        try {
            String selectExpression = "SELECT * FROM DWH.GL_ACC WHERE BSAACID IN (" + inCondition + ")";
            if(customerNo != null)
              selectExpression+=" AND CUSTNO = '"+customerNo+"'";
            return selectMaxRows(selectExpression, Integer.MAX_VALUE, null);
        } catch (NoResultException e) {
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("",e);
        }
        return Collections.emptyList();
    }

    public void loadCurrency(Map<String, String> currencyMap, Map<String, Integer> currencyNBDPMap) {
        try {
            List<DataRecord> dataRecords = selectMaxRows("SELECT GLCCY,CBCCY,NBDP FROM DWH.CURRENCY", Integer.MAX_VALUE, null);
            for (DataRecord item : dataRecords) {
                currencyMap.put(item.getString("CBCCY"), item.getString("GLCCY"));
                currencyNBDPMap.put(item.getString("CBCCY"), item.getInteger("NBDP"));
            }
        } catch (SQLException e) {
            log.error("",e);
        }
    }

    public String getBranchByBsaacidorAcid(String bsaacid, String acid, Date workday) { //todo код БРАНЧА Midas
        try {
            DataRecord record = selectFirst(
                "SELECT BRANCH FROM DH_ACC_INF WHERE CBRF_ACC_NUMBER=? AND DAT=? AND DATTO=?", bsaacid, workday, workday);

            if (record != null && !isEmpty(record.getString("BRANCH"))) {
                return record.getString("BRANCH");
            }
        } catch (NoResultException e) {
            // Возможный вариант
        } catch (SQLException e) {
            log.error("",e);
        }

        if (acid.length() == 20) {
            return convertBranchToFcc(acid.substring(acid.length() - 3));
        }
        return "";
    }

    public String getAnam(String acid, String bsaacid, Date workday) {
        try {
            DataRecord record = selectFirst(
                "SELECT ACCOUNT_NAME FROM DH_ACC_INF WHERE CBRF_ACC_NUMBER=? AND DAT=? AND DATTO=?", bsaacid, workday, workday);
            if (record != null && !isEmpty(record.getString("ACCOUNT_NAME"))) {
                return record.getString("ACCOUNT_NAME");
            }
        } catch (NoResultException e) {
            // Возможный вариант
        } catch (SQLException e) {
            log.error("",e);
        }

        try {
            DataRecord record = selectFirst("SELECT ANAM FROM ACC WHERE ID=?", acid);
            if (record != null && !isEmpty(record.getString("ANAM"))) {
                return record.getString("ANAM");
            }
        } catch (NoResultException e) {
            // Возможный вариант
        } catch (SQLException e) {
            log.error("",e);
        }
        return "";
    }

    public BigDecimal[] getAccountAmountsIncoming(String bsaacid, Date workday) {
        try {
            DataRecord record = selectFirst(
                "SELECT BA.BSAACID, BA.ORDERED, BA.RESULT+VALUE(GLBA.GL_RESULT,0) AS INCO, BA.RUBRESULT+VALUE(GLBA.GL_RUBRESULT,0) AS INCORUB FROM " +
                    "  (SELECT OBAC AS RESULT,OBBC AS RUBRESULT,BSAACID, 1 AS ORDERED FROM BALTUR WHERE BSAACID=? AND DAT=? AND DATTO='2029-01-01' " +
                    "  UNION " +
                    "SELECT OBAC+CTAC+DTAC AS RESULT,OBBC+CTBC+DTBC AS RUBRESULT,BSAACID, 2 AS ORDERED FROM BALTUR WHERE BSAACID=? AND DATTO='2029-01-01') BA " +
                    "LEFT OUTER JOIN ( " +
                    "SELECT CTAC+DTAC AS GL_RESULT, CTBC+DTBC AS GL_RUBRESULT, BSAACID FROM GL_BALTUR WHERE BSAACID=? AND MOVED='N' AND DAT<?) GLBA " +
                    "ON BA.BSAACID=GLBA.BSAACID ORDER BY BA.ORDERED",
                bsaacid, workday, bsaacid, bsaacid, workday);
            if (record != null) {
                return new BigDecimal[]{record.getBigDecimal("INCO"), record.getBigDecimal("INCORUB")};
            }
        } catch (SQLException e) {
            log.error("",e);
        }
        return new BigDecimal[]{new BigDecimal(0), new BigDecimal(0)};
    }

    public BigDecimal[] getAccountAmountsCurrent(String bsaacid, Date workday) {
        try {
            DataRecord record = selectFirst(
                "SELECT BA.BSAACID, VALUE(BA.RESULT,0)+VALUE(GLBA.GL_RESULT,0) AS INCO, BA.RUBRESULT+VALUE(GLBA.GL_RUBRESULT,0) AS INCORUB FROM " +
                    "  (SELECT CTAC+DTAC AS RESULT,CTBC+DTBC AS RUBRESULT,BSAACID FROM BALTUR WHERE BSAACID=? AND DAT=? AND DATTO='2029-01-01') BA " +
                    "  FULL OUTER JOIN " +
                    "  (SELECT CTAC+DTAC AS GL_RESULT, CTBC+DTBC AS GL_RUBRESULT, BSAACID FROM GL_BALTUR WHERE BSAACID=? AND MOVED='N' AND DAT=?) GLBA " +
                    "    ON BA.BSAACID=GLBA.BSAACID",
                bsaacid, workday, bsaacid, workday);
            if (record != null) {
                return new BigDecimal[]{record.getBigDecimal("INCO"), record.getBigDecimal("INCORUB")};
            }
        } catch (SQLException e) {
            log.error("",e);
        }
        return new BigDecimal[]{new BigDecimal(0), new BigDecimal(0)};
    }

    public Set<String> getCountsByAB(String condition) {
        try {
            List<DataRecord> dataRecords = selectMaxRows(
                "SELECT BSAACID FROM DWH.ACCRLN A WHERE VALUE(A.BSAACID,'')<>'' AND " + condition + " "+
                    "AND EXISTS(SELECT * FROM DWH.GL_ACC G WHERE A.BSAACID=G.BSAACID) " +
                    "AND (CURRENT DATE - A.DRLNC) <= 1131"
                , Integer.MAX_VALUE,null);

            Set<String> result = dataRecords.stream().map(item -> item.getString(0)).collect(Collectors.toSet());
            return result;
        } catch (NoResultException e) {
            return Collections.emptySet();
        } catch (SQLException e) {
            log.error("",e);
        } catch (Exception e) {
            log.error("",e);
        }
        return Collections.emptySet();
    }

    public String convertBranchToFcc(String branch) {
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
            log.error("",e);
        }
        return "";
    }
}