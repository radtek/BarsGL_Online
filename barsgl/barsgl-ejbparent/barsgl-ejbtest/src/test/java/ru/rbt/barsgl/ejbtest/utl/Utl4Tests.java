package ru.rbt.barsgl.ejbtest.utl;

import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadParams;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPd;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.BaseEntityRepository;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Created by Ivan Sevastyanov
 */
public class Utl4Tests {

    private static final Logger logger = Logger.getLogger(Utl4Tests.class.getName());


    public static Date parseDate(String date, String format){

        SimpleDateFormat formatter = new SimpleDateFormat(format);
        try {
            return formatter.parse(date);
        } catch (ParseException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public static void deleteGlAccountWithLinks(BaseEntityRepository baseEntityRepository, String keysString) {
        Date date = parseDate("25.02.2015", "dd.MM.yyyy");

        try {
            AccountKeys keys = new AccountKeys(keysString);

            int cntAcc = baseEntityRepository.executeNativeUpdate(
                    "delete from ACC where ID in (select ACID from GL_ACC where BRANCH = ? and CUSTNO = ? and ACCTYPE = ? and CBCUSTTYPE = ? and GL_SEQ = ? and CCY = ?)" +
                            " and DACO >= ?"
                    , keys.getBranch(), keys.getCustomerNumber(), keys.getAccountType(), keys.getCustomerType(), keys.getGlSequence(), keys.getCurrency(), date);


            int cntAccRln = baseEntityRepository.executeNativeUpdate(
                    "delete from ACCRLN where BSAACID in (select BSAACID from GL_ACC where BRANCH = ? and CUSTNO = ? and ACCTYPE = ? and CBCUSTTYPE = ? and GL_SEQ = ? and CCY = ?)" +
                            " and DRLNO >= ?"
                    , keys.getBranch(), keys.getCustomerNumber(), keys.getAccountType(), keys.getCustomerType(), keys.getGlSequence(), keys.getCurrency(), date);

            // BALTUR ??

            int cntBsaAcc = baseEntityRepository.executeNativeUpdate(
                    "delete from BSAACC where ID in (select BSAACID from GL_ACC where BRANCH = ? and CUSTNO = ? and ACCTYPE = ? and CBCUSTTYPE = ? and GL_SEQ = ? and CCY = ?)" +
                            " and BSAACO >= ?"
                    , keys.getBranch(), keys.getCustomerNumber(), keys.getAccountType(), keys.getCustomerType(), keys.getGlSequence(), keys.getCurrency(), date);

            int cntGlAcc = baseEntityRepository.executeNativeUpdate(
                    "delete from GL_ACC where BRANCH = ? and CUSTNO = ? and ACCTYPE = ? and CBCUSTTYPE = ? and GL_SEQ = ? and CCY = ?"
                    , keys.getBranch(), keys.getCustomerNumber(), keys.getAccountType(), keys.getCustomerType(), keys.getGlSequence(), keys.getCurrency());

            logger.info("deleted from GL_ACC:" + cntGlAcc + "; ACC:" + cntAcc + "; ACCRLN:" + cntAccRln + "; BSAACC:" + cntBsaAcc);
        }finally {

        }
    }

    public static void deleteGlAccount(BaseEntityRepository baseEntityRepository, String keysString) {
        try {
            AccountKeys keys = new AccountKeys(keysString);
            int cnt = baseEntityRepository.executeNativeUpdate(
                    "delete from GL_ACC where BRANCH = ? and CUSTNO = ? and ACCTYPE = ? and CBCUSTTYPE = ? and GL_SEQ = ? and CCY = ?"
                    , keys.getBranch(), keys.getCustomerNumber(), keys.getAccountType(), keys.getCustomerType(), keys.getGlSequence(), keys.getCurrency());
            logger.info("deleted " + cnt);
        }finally {

        }
    }

    public static void deleteAccountByAcid(BaseEntityRepository baseEntityRepository, String acid) {
        try {
            int cntGlAcc = baseEntityRepository.executeNativeUpdate("delete from GL_ACC where BSAACID in " +
                    "(select BSAACID from accrln where acid = ? and RLNTYPE = '2')", acid);
            int cntBsaAcc = baseEntityRepository.executeNativeUpdate("delete from BSAACC where ID in " +
                    "(select BSAACID from accrln where acid = ? and RLNTYPE = '2')", acid);
            int cntAccRln = baseEntityRepository.executeNativeUpdate("delete from ACCRLN where acid = ?", acid);
            logger.info("deleted Midas from GL_ACC:" + cntGlAcc + "; ACCRLN:" + cntAccRln + "; BSAACC:" + cntBsaAcc);
        }finally {

        }
    }
    /*
            logger.info("deleted midas from BSAACC: "
                    + baseEntityRepository.executeNativeUpdate("delete from BSAACC where ID in " +
                    "(select BSAACID from accrln where acid = ? and RLNTYPE = '2')", keys1.getAccountMidas()));
            logger.info("deleted midas from ACCRLN: "
                    + baseEntityRepository.executeNativeUpdate("delete from accrln where acid = ?", keys1.getAccountMidas()));

    * */

    public static GLAccount findGlAccount(BaseEntityRepository baseEntityRepository, AccountKeys keys) {
        return (GLAccount) baseEntityRepository.selectFirst(GLAccount.class
                , "from GLAccount a where a.branch = ?1 and a.customerNumber = ?2 and a.accountType = ?3 and a.cbCustomerType = ?4 and a.term = ?5 and a.glSequence = ?6 and a.currency = ?7"
                , keys.getBranch(), keys.getCustomerNumber(), Long.parseLong(keys.getAccountType()), Short.parseShort(keys.getCustomerType()), Short.parseShort(keys.getTerm()), keys.getGlSequence(), new BankCurrency(keys.getCurrency()));
    }

    public static List<Pd> getPds(BaseEntityRepository baseEntityRepository, GLOperation operation) {
        return baseEntityRepository.select(Pd.class
                , "select p from Pd p, GLOperation o, GLPosting ps where p.pcId = ps.id and ps.operation = o and o.id = ?1"
                , operation.getId());
    }

    public static List<GLPd> getGLPds(BaseEntityRepository baseEntityRepository, GLOperation operation) {
        return baseEntityRepository.select(GLPd.class
                , "select p from GLPd p, GLOperation o where p.glOperationId = o.id and o.id = ?1"
                , operation.getId());
    }


    public static String shortTimestamp() {
        return new SimpleDateFormat("HH_mm_ss").format(new Date());
    }

    public static String toString(Date date, String format) {
        return new SimpleDateFormat(format).format(date);
    }

    public static String findBsaacid(BaseEntityRepository baseEntityRepository, Operday operday, final String like) throws SQLException {
        return Optional.ofNullable(baseEntityRepository.selectFirst(
                "select id from BSAACC B " +
                        "where B.ID like ? and (B.BSAACC is null or B.BSAACC >= ?)", like, operday.getCurrentDate()))
                .orElseThrow(() -> new RuntimeException("not found by " + like)).getString(0);
    }

    public static void cleanHeader(BaseEntityRepository baseEntityRepository, String pardesc) {
        int cnt = baseEntityRepository.executeNativeUpdate("delete from GL_ETLDWHS where PARDESC = ?"
                , pardesc);
        logger.info("deleted header = " + cnt);
    }

    public static List<DataRecord> findDwhUnloadHeaders(BaseEntityRepository baseEntityRepository
            , DwhUnloadParams params
            , Operday operday) throws SQLException {
        return findDwhUnloadHeaders(baseEntityRepository, params, operday.getCurrentDate());
    }

    public static List<DataRecord> findDwhUnloadHeaders(BaseEntityRepository baseEntityRepository
            , DwhUnloadParams params
            , Date operday) throws SQLException {
        return baseEntityRepository
                .select("select * from GL_ETLDWHS where parname = ? and OPERDAY = ? and PARDESC = ?"
                        , params.getParamName(), operday, params.getParamDesc());
    }

    public static <T extends Enum> boolean in (T value, T ... values) {
        return Arrays.asList(values).stream().filter(c -> c == value).findAny().isPresent();
    }

    public static <T extends Enum> boolean in (Supplier<T> supplier, T ... values) {
        return Arrays.asList(values).stream().filter(c -> c == supplier.get()).findAny().isPresent();
    }

    public static GLPd findDebitPd(List<GLPd> pdList) {
        return pdList.stream().filter(p -> Objects.equals(p.getPcId(), p.getId())).findFirst().orElseThrow(()-> new RuntimeException("pd debit is not found"));
    }

    public static GLPd findCreditPd(List<GLPd> pdList) {
        return pdList.stream().filter(p -> !Objects.equals(p.getId(), p.getPcId())).findFirst().orElseThrow(()->new RuntimeException("pd credit is not found"));
    }

    public static boolean createUser(long userId, BaseEntityRepository baseEntityRepository) {
        return 1 == baseEntityRepository.executeNativeUpdate("merge into gl_user u\n" +
                "using (select 2 id_user from dual) u2\n" +
                "on (u.id_user = u2.id_user)\n" +
                "when not matched then insert (id_user, user_name, user_pwd, surname, firstname, patronymic, create_dt, locked)\n" +
                "values (?, 'name2', '123', 'surname2', 'firstname2', 'patronymic2', systimestamp, '0')", userId);
    }

    public static int grantAllPerission(long userId, BaseEntityRepository baseEntityRepository) {
        return baseEntityRepository.executeNativeUpdate(
                "merge into gl_au_actrl ar\n" +
                "using ( select *\n" +
                "          from (\n" +
                "        select a.id_act, ur.id_role, row_number() over (partition by a.id_act order by ur.id_role) rn\n" +
                "          from gl_au_usrrl ur, gl_au_act a\n" +
                "         where ur.id_user = ?\n" +
                "           ) where rn = 1) s\n" +
                "  on (ar.id_act = s.id_act and ar.id_role = s.id_role)\n" +
                "when not matched then insert (ar.id_role,ar.id_act, usr_aut, dt_aut) \n" +
                "values (s.id_role, s.id_act, 'sys', systimestamp)", userId);
    }

    public static int grantAllBranches(long userId, BaseEntityRepository baseEntityRepository) {
        return baseEntityRepository.executeNativeUpdate(
                "merge into GL_AU_PRMVAL p\n" +
                "using (select ? id_user, max(id_prm) + 1 id_prm, 'HeadBranch' prm_code, '*' prmval, cast('2015-09-01' as date) dt_begin from GL_AU_PRMVAL) p0\n" +
                " on (p.id_user = p0.id_user)\n" +
                "when not matched then insert (id_prm, id_user, prm_code, prmval, dt_begin, dt_end, usr_aut, dt_aut)\n" +
                "values (p0.id_prm, p0.id_user, p0.prm_code, p0.prmval, p0.dt_begin, null, 'sys', systimestamp)", userId);
    }

}
