package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.oper.OrdinaryPostingProcessor;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;

import static ru.rbt.ejbcore.mapping.YesNo.N;
import static ru.rbt.ejbcore.mapping.YesNo.Y;

/**
 * Created by Ivan Sevastyanov Настройка определения даты проводки
 */
public class PostingDateTest extends AbstractRemoteTest {

    public static final String NEW_VIEW
            = "CREATE OR REPLACE VIEW V_GL_OPER_POD as"
            + " select v1.gloid, v1.pod_type,\n"
            + "       case when v1.pod_type = 'HARD' then cast ('2012-12-12' as date) else cast (null as date) end pod\n"
            + "  from\n"
            + " (\n"
            + "    select gloid,\n"
            + "           case when o.fan = 'Y' and o.strn = 'N' then 'CURRENT' \n"
            + "                when o.fan = 'N' and o.strn = 'Y' then 'LAST'   \n"
            + "                when o.fan = 'Y' and o.strn = 'Y' then 'STANDARD'\n"
            + "                when o.fan = 'N' and o.strn = 'N' then 'HARD'    \n"
            + "           end pod_type\n"
            + "      from GL_OPER o\n"
            + "  ) v1";

    //public static final String AUTH = "call CHGAUT2 (CURRENT SCHEMA, 'V_GL_OPER_POD', 'FILE', 'DWHDATA')";
//    @BeforeClass
//    public static void initClass() {
//        baseEntityRepository.executeNativeUpdate("call DROPVIEW_IF_EXISTS(CURRENT SCHEMA, 'V_GL_OPER_POD')");
//    }
    @AfterClass
    public static void teardownClass() {
//        baseEntityRepository.executeNativeUpdate("call DROPVIEW_IF_EXISTS(CURRENT SCHEMA, 'V_GL_OPER_POD')");
//        baseEntityRepository.executeNativeUpdate(
//                                "CREATE VIEW V_GL_OPER_POD AS\n" +
//                                "SELECT GLOID\n" +
//                                "       , POD_TYPE\n" +
//                                "       , CASE WHEN POD_TYPE = 'HARD' THEN POSTDATE ELSE CAST (NULL AS DATE) END POD\n" +
//                                "FROM\n" +
//                                "(\n" +
//                                "    SELECT GLOID\n" +
//                                "           , CASE\n" +
//                                "                WHEN OPER_CLASS = 'MANUAL'\n" +
//                                "                    OR (OPER_CLASS = 'AUTOMATIC' AND SRC_PST IN ('ARMPRO', 'SECMOD')) THEN 'HARD'\n" +
//                                "                ELSE 'STANDARD' END POD_TYPE\n" +
//                                "           , case\n" +
//                                "                when OPER_CLASS = 'MANUAL' then POSTDATE\n" +
//                                "                when OPER_CLASS = 'AUTOMATIC' AND SRC_PST IN ('ARMPRO', 'SECMOD') then VDATE\n" +
//                                "             end POSTDATE\n" +
//                                "      FROM GL_OPER O\n" +
//                                ") V");
//
//        baseEntityRepository.executeNativeUpdate(AUTH);
        baseEntityRepository.executeNativeUpdate("CREATE OR REPLACE FORCE VIEW V_GL_OPER_POD (GLOID, POD_TYPE, POD) AS \n"
                + "  SELECT GLOID\n"
                + "       , POD_TYPE\n"
                + "       , CASE WHEN POD_TYPE = 'HARD' THEN POSTDATE ELSE CAST (NULL AS DATE) END POD\n"
                + "FROM\n"
                + "(\n"
                + "    SELECT GLOID\n"
                + "           , CASE\n"
                + "                WHEN OPER_CLASS = 'MANUAL'\n"
                + "                    OR (OPER_CLASS = 'AUTOMATIC' AND SRC_PST IN ('ARMPRO', 'SECMOD', 'IMEX', 'AXAPTA')) THEN 'HARD'\n"
                + "                ELSE 'STANDARD' END POD_TYPE\n"
                + "           , case\n"
                + "                when OPER_CLASS = 'MANUAL' then POSTDATE\n"
                + "                when OPER_CLASS = 'AUTOMATIC' and SRC_PST IN ('ARMPRO', 'SECMOD', 'IMEX', 'AXAPTA') then\n"
                + "                    case when VDATE < CC.MIN_DT then MIN_DT else VDATE end\n"
                + "             end POSTDATE\n"
                + "      FROM GL_OPER O,\n"
                + "           (SELECT MIN(DAT) MIN_DT\n"
                + "              FROM CAL C\n"
                + "             WHERE C.HOL <> 'X' AND CCY = 'RUR'\n"
                + "               AND C.DAT > (SELECT CURDATE - 14 FROM GL_OD)) CC\n"
                + ") V"
        );
    }

    /**
     * Вычисление даты проводки в соответствии с текущей настройкой
     *
     * @fsd
     * @throws ParseException
     */
    @Test
    public void test() throws ParseException {

        Operday operday = getOperday();

        baseEntityRepository.executeNativeUpdate(NEW_VIEW);
        //baseEntityRepository.executeNativeUpdate(AUTH);

        GLOperation operation = new GLOperation();
        operation.setStorno(N);
        operation.setFan(N);
        operation.setAccountCredit("123");
        operation.setAmountCredit(new BigDecimal("123.00"));
        operation.setAmountDebit(new BigDecimal("123.00"));
        operation.setAccountDebit("123");
        operation.setAePostingId(System.currentTimeMillis() + "");
        operation.setEventId(System.currentTimeMillis() + "_e");
        operation.setOperationTimestamp(new Date());
        operation.setSourcePosting("srcpst");
        operation.setValueDate(getOperday().getCurrentDate());
        operation.setCurrencyCredit(BankCurrency.AUD);
        operation.setCurrencyDebit(BankCurrency.AUD);
        operation.setDealId(System.currentTimeMillis() + "_d");
        operation.setPostDate(DateUtils.parseDate("2012-12-13", "yyyy-MM-dd"));
        operation = (GLOperation) baseEntityRepository.save(operation);

        //GLOperation operation = (GLOperation) baseEntityRepository.selectFirst(GLOperation.class, "from GLOperation o order by 1 desc");
        baseEntityRepository.executeNativeUpdate("update gl_oper o set o.fan = ?, o.STRN = ? where o.gloid = ?", Y.name(), N.name(), operation.getId());
        Date pod = remoteAccess.invoke(OrdinaryPostingProcessor.class, "calculatePostingDate", operation);
        Assert.assertEquals(operday.getCurrentDate(), pod);

        baseEntityRepository.executeNativeUpdate("update gl_oper o set o.fan = ?, o.STRN = ? where o.gloid = ?", N.name(), N.name(), operation.getId());
        pod = remoteAccess.invoke(OrdinaryPostingProcessor.class, "calculatePostingDate", operation);
        Assert.assertEquals(DateUtils.parseDate("2012-12-12", "yyyy-MM-dd"), pod);

        teardownClass();
        baseEntityRepository.executeNativeUpdate("update gl_oper o set o.oper_class = ? where o.gloid = ?", GLOperation.OperClass.MANUAL.name(), operation.getId());
        pod = remoteAccess.invoke(OrdinaryPostingProcessor.class, "calculatePostingDate", operation);
        Assert.assertEquals(DateUtils.parseDate("2012-12-13", "yyyy-MM-dd"), pod);

        baseEntityRepository.executeNativeUpdate("update gl_oper o set o.oper_class = ? where o.gloid = ?", GLOperation.OperClass.AUTOMATIC.name(), operation.getId());
        pod = remoteAccess.invoke(OrdinaryPostingProcessor.class, "calculatePostingDate", operation);
        Assert.assertEquals(getOperday().getCurrentDate(), pod);

    }

}
