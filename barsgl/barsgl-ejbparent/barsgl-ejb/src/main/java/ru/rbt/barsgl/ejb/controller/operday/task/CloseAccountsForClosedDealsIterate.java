package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.repository.CloseAccountsRepository;
import ru.rbt.ejbcore.datarec.DataRecord;

import javax.ejb.EJB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by er22317 on 19.03.2018.
 */
public class CloseAccountsForClosedDealsIterate {
    PreparedStatement query;
    ResultSet rec;
    private String cnum, dealid, source;
    private Optional<String> subdealid;
    private List<GLAccount> accounts = new ArrayList<GLAccount>();

    @EJB
    private CloseAccountsRepository closeAccountsRepository;

    public CloseAccountsForClosedDealsIterate(){}

//    public CloseAccountsForClosedDealsIterate(Connection connection) throws SQLException {
//        query = connection.prepareStatement("select * from GL_DEALCLOSE");
//        rec = query.executeQuery();
//    }

    public void init(Connection connection) throws SQLException {
        query = connection.prepareStatement("select * from GL_DEALCLOSE");
        rec = query.executeQuery();
    }

    boolean next() throws SQLException {
        if (!rec.next()){
            return false;
        }else {
            cnum = rec.getString("cnum");
            dealid = rec.getString("dealid");
            subdealid = Optional.ofNullable(rec.getString("subdealid"));
            source = rec.getString("source");
            accounts = closeAccountsRepository.dealsAccounts( cnum, dealid, subdealid);
        }
        return true;
    }

    public void close() throws Exception {
        if (query != null){ query.close();}
        if (rec != null) { rec.close();}
    }

    public List<GLAccount> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<GLAccount> accounts) {
        this.accounts = accounts;
    }
    public String getCnum() {
        return cnum;
    }

    public void setCnum(String cnum) {
        this.cnum = cnum;
    }

    public String getDealid() {
        return dealid;
    }

    public void setDealid(String dealid) {
        this.dealid = dealid;
    }

    public Optional<String> getSubdealid() {
        return subdealid;
    }

    public void setSubdealid(Optional<String> subdealid) {
        this.subdealid = subdealid;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }


}
