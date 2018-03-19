package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * Created by er22317 on 19.03.2018.
 */
@Stateless
@LocalBean
public class CloseAccountsRepository <E extends BaseEntity<String>> extends AbstractBaseEntityRepository<E, String> {

    public long countDeals() throws SQLException {
       return selectOne("select count(*) FROM GL_DEALCLOSE", null).getLong(0);
    }

    public void delOldDeals(){
        executeNativeUpdate("DELETE FROM GL_DEALCLOSE d where exists(select 1 from GL_DEALCLOSE_H h where d.cnum=h.cnum and d.source=h.source and d.dealid=h.dealid and d.subdealid=h.subdealid)",null);
        flush();
    }

    public List<GLAccount> dealsAccounts(String cnum, String dealid, String subdealid) throws SQLException {
//        return select("select * from GL_ACC where custno=? and dealid=? and subdealid=? and dtc is null", cnum, dealid, subdealid);
        return selectMaxRows(GLAccount.class,"select a from GLAccount a where a.custno=?1 and a.dealid=?2 and a.subdealid=?3 and a.dtc is null", 100, cnum, dealid, subdealid);
    }

    public void moveToHistory(String cnum, String dealid, String subdealid, String source){
        executeNativeUpdate("insert into GL_DEALCLOSE_H (DEALID,SUBDEALID,CNUM,CLOSE_DT,MATURITY_DT,STATUS,SOURCE,STREAM_ID,VALID_FROM,DEAL_TYPE,DWH_TABLE,LOADDATE) select * from GL_DEALCLOSE");
        executeNativeUpdate("DELETE FROM GL_DEALCLOSE where cnum=? and source=? and dealid=? and subdealid=?", cnum, source, dealid, subdealid);
        flush();
    }

    public void moveToWaitClose(GLAccount glAccount, Date loadDate, String closeType) throws SQLException {
        if (0 < selectOne("select count(*) from gl_acwaitclose where BSAACID=? and ACID=?", glAccount.getBsaAcid(), glAccount.getAcid()).getInteger(0))
            return;
        executeNativeUpdate("insert into gl_acwaitclose(BSAACID,ACID,CCY,DEALID,SUBDEALID,DEALSRC,DTO,DTR,OPENTYPE,IS_ERRACC,OPERDAY) values(?,?,?,?,?,?,?,?,?,?,?,?)",
                            glAccount.getBsaAcid(),
                            glAccount.getAcid(),
                            glAccount.getCurrency().getCurrencyCode(),
                            glAccount.getDealId(),
                            glAccount.getSubDealId(),
                            glAccount.getDateOpen(),
                            glAccount.getDateRegister(),
                            glAccount.getOpenType(),
                            closeType,
                            loadDate);
        flush();
    }
}
