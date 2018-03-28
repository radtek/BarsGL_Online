package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.ejbcore.util.StringUtils;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by er22317 on 19.03.2018.
 */
@Stateless
@LocalBean
public class CloseAccountsRepository <E extends BaseEntity<String>> extends AbstractBaseEntityRepository<E, String> {

    public boolean isExistsDeals() throws SQLException {
       return null != selectFirst("select 1 FROM GL_DEALCLOSE", null);
    }

    public int delOldDeals(){
        int cnt = executeNativeUpdate("DELETE FROM GL_DEALCLOSE d where exists(select 1 from GL_DEALCLOSE_H h where d.cnum=h.cnum and d.source=h.source and d.dealid=h.dealid and coalesce(d.subdealid,' ')=coalesce(h.subdealid,' '))",null);
        return cnt;
    }

    public GLAccount getAccountByDeal(String bsaAcid, String dealId) {
        return selectFirst(GLAccount.class, "from GLAccount a where a.bsaAcid = ?1 and a.dealId = ?2", bsaAcid, dealId);
    }

    public List<GLAccount> getDealCustAccounts(GLAccount mainAccount) throws SQLException {
        return select(GLAccount.class,"from GLAccount a where a.customerNumber=?1 and a.dealId=?2 and a.bsaAcid != ?3 and a.dateClose is null",
                mainAccount.getCustomerNumber(), mainAccount.getDealId(), mainAccount.getBsaAcid());
    }

    public List<GLAccount> dealsAccounts(String cnum, String dealid, Optional<String> subdealid) throws SQLException {
//        return select("select * from GL_ACC where custno=? and dealid=? and subdealid=? and dtc is null", cnum, dealid, subdealid);
        return select(GLAccount.class,"from GLAccount a where a.customerNumber=?1 and a.dealId=?2 and coalesce(a.subDealId,'null')=?3 and a.dateClose is null", cnum, dealid, subdealid.orElse("null"));
    }

    public void moveToHistory(String cnum, String dealid, Optional<String> subdealid, String source){
        executeNativeUpdate("insert into GL_DEALCLOSE_H (DEALID,SUBDEALID,CNUM,CLOSE_DT,MATURITY_DT,STATUS,SOURCE,STREAM_ID,VALID_FROM,DEAL_TYPE,DWH_TABLE,LOADDATE) select * from GL_DEALCLOSE where cnum=? and source=? and dealid=? and coalesce(subdealid,'null')=?", cnum, source, dealid, subdealid.orElse("null"));
        executeNativeUpdate("DELETE FROM GL_DEALCLOSE where cnum=? and source=? and dealid=? and coalesce(subdealid,'null')=?", cnum, source, dealid, subdealid.orElse("null"));
    }

    public boolean moveToWaitClose(GLAccount glAccount, Date loadDate, GLAccount.CloseType closeType) throws SQLException {
        if (null != selectFirst("select 1 from gl_acwaitclose where BSAACID=? and ACID=?", glAccount.getBsaAcid(), glAccount.getAcid()))
            return false;
        executeNativeUpdate("insert into gl_acwaitclose(GLACID,BSAACID,ACID,CCY,DEALID,SUBDEALID,DEALSRC,DTO,DTR,OPENTYPE,IS_ERRACC,OPERDAY) values(?,?,?,?,?,?,?,?,?,?,?,?)",
                            glAccount.getId(),
                            glAccount.getBsaAcid(),
                            glAccount.getAcid(),
                            glAccount.getCurrency().getCurrencyCode(),
                            glAccount.getDealId(),
                            glAccount.getSubDealId(),
                            glAccount.getDealSource(),
                            glAccount.getDateOpen(),
                            glAccount.getDateRegister(),
                            glAccount.getOpenType(),
                            closeType.getFlag(),//= 0, 1, 2
                            loadDate);
        flush();
        return true;
    }

/*
    public List<GLAccount> getAccountsWaitClose() throws SQLException {
        List<DataRecord> data = select("select GLACID from V_GL_ACWAITCLOSE_BAL where EXCLDATE is null and (BAL is null or BAL = 0)");
        if (null == data || data.isEmpty())
            return Collections.emptyList();
        String ids = StringUtils.listToString(data.stream().map(r -> r.getLong(0)).collect(Collectors.toList()), ",");
        return select(GLAccount.class, "from GLAccount a where a.id in (" + ids + ")");
    }

    public int moveWaitCloseToHist(GLAccount account) {
        executeNativeUpdate("insert into GL_ACWAITCLOSE_H "

        );
    }
*/
}
