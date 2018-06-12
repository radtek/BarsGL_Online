package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.CloseAccountsForClosedDealsTask;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Optional;

/**
 * Created by er22317 on 21.03.2018.
 */
public class CloseAccountsForClosedDealsIT extends AbstractTimerJobIT{

    @Test
    public void test() throws ParseException, SQLException {
        String noaccDEALID = "106162X";
        String noaccSUBDEALID = null;
        String noaccCNUM = "00685396";
        String noaccSOURCE = "KPS";
        String accDEALID = "1061623";
        String accSUBDEALID = null;
        String accCNUM = "00685396";
        String accSOURCE = "KPS";

//        deal без счетов
        if (!isDealExsists(noaccDEALID, Optional.ofNullable(noaccSUBDEALID).orElse("null"), noaccCNUM, noaccSOURCE)){
            baseEntityRepository.executeNativeUpdate("Insert into GL_DEALCLOSE (DEALID,SUBDEALID,CNUM,CLOSE_DT,MATURITY_DT,STATUS,SOURCE,STREAM_ID,VALID_FROM,DEAL_TYPE,DWH_TABLE,LOADDATE) values (?,?,?,to_date('2017-09-12 00:00:00','YYYY-MM-DD HH24:MI:SS'),to_date('2017-09-12 00:00:00','YYYY-MM-DD HH24:MI:SS'),'C',?,'KPS_DEPOSIT',to_date('2017-09-12 00:00:00','YYYY-MM-DD HH24:MI:SS'),'KDD','DEPOSIT',to_timestamp('2015-09-18 18:30:31.630000','YYYY-MM-DD HH24:MI:SS.FF6'))",
                noaccDEALID, Optional.ofNullable(noaccSUBDEALID).orElse("null"), noaccCNUM, noaccSOURCE);
        }
        delHistory(noaccDEALID, Optional.ofNullable(noaccSUBDEALID).orElse("null"), noaccCNUM, noaccSOURCE);

//        deal со счетом
        if (!isDealExsists(accDEALID, Optional.ofNullable(noaccSUBDEALID).orElse("null"), accCNUM, accSOURCE)) {
            baseEntityRepository.executeNativeUpdate("Insert into GL_DEALCLOSE (DEALID,SUBDEALID,CNUM,CLOSE_DT,MATURITY_DT,STATUS,SOURCE,STREAM_ID,VALID_FROM,DEAL_TYPE,DWH_TABLE,LOADDATE) values (?,?,?,to_date('12.09.17','DD.MM.RR'),to_date('12.09.17','DD.MM.RR'),'C',?,'KPS_DEPOSIT',to_date('12.09.17','DD.MM.RR'),'KDD','DEPOSIT',to_timestamp('18.09.15 18:30:31,630000000','DD.MM.RR HH24:MI:SSXFF'))",
                    accDEALID, Optional.ofNullable(accSUBDEALID).orElse("null"), accCNUM, accSOURCE);
        }
        delHistory(accDEALID, Optional.ofNullable(accSUBDEALID).orElse("null"), accCNUM, accSOURCE);
        setAccountOpened(accDEALID, Optional.ofNullable(accSUBDEALID).orElse("null"), accCNUM);

//        select PKG_CHK_ACC.GET_BALANCE_TODATE( '47426810620010131033', '00611182RUR510755001', sysdate) from dual;

        remoteAccess.invoke(CloseAccountsForClosedDealsTask.class, "run", "", null);

        DataRecord rec = baseEntityRepository.selectFirst("select 1 from GL_DEALCLOSE_H where DEALID=? and nvl(SUBDEALID,'null')=? and CNUM=? and SOURCE=?",noaccDEALID, Optional.ofNullable(noaccSUBDEALID).orElse("null"), noaccCNUM, noaccSOURCE);
        Assert.assertNotNull("deal без счетов: Не перенесено в GL_DEALCLOSE_H", rec);

        rec = baseEntityRepository.selectFirst("select 1 from GL_DEALCLOSE_H where DEALID=? and nvl(SUBDEALID,'null')=? and CNUM=? and SOURCE=?", accDEALID, Optional.ofNullable(accSUBDEALID).orElse("null"), accCNUM, accSOURCE);
        Assert.assertNotNull("deal со счетами: Не перенесено в GL_DEALCLOSE_H", rec);

        Assert.assertTrue("Счет не закрыт", isAccountClosed(accDEALID, Optional.ofNullable(accSUBDEALID).orElse("null"), accCNUM));

        setAccountOpened(accDEALID, Optional.ofNullable(accSUBDEALID).orElse("null"), accCNUM);
    }

    boolean isDealExsists(String DEALID, String SUBDEALID, String CNUM, String SOURCE) throws SQLException {
        return null != baseEntityRepository.selectFirst("select 1 from GL_DEALCLOSE where DEALID=? and nvl(SUBDEALID,'null')=? and CNUM=? and SOURCE=?", DEALID, SUBDEALID, CNUM, SOURCE);
    }
    void delHistory(String DEALID, String SUBDEALID, String CNUM, String SOURCE){
        baseEntityRepository.executeNativeUpdate("delete from GL_DEALCLOSE_H where DEALID=? and nvl(SUBDEALID,'null')=? and CNUM=? and SOURCE=?", DEALID, SUBDEALID, CNUM, SOURCE);
    }
    void setAccountOpened(String DEALID, String SUBDEALID, String CNUM) {
        baseEntityRepository.executeNativeUpdate("update gl_acc set dtc = null where DEALID = ? and nvl(SUBDEALID,'null')=? and CUSTNO = ?",
                DEALID, SUBDEALID, CNUM);
    }

    boolean isAccountClosed(String DEALID, String SUBDEALID, String CNUM) throws SQLException {
        return null != baseEntityRepository.selectFirst("select dtc from gl_acc where DEALID=? and nvl(SUBDEALID,'null')=? and CUSTNO=?", DEALID, SUBDEALID, CNUM).getDate(0);
    }
}
