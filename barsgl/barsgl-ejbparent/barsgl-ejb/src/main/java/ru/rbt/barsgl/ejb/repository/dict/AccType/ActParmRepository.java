package ru.rbt.barsgl.ejb.repository.dict.AccType;

import ru.rbt.barsgl.ejb.entity.dict.AccType.ActParm;
import ru.rbt.barsgl.ejb.entity.dict.AccType.ActParmId;
import ru.rbt.barsgl.ejb.entity.dict.AccountingType;
import ru.rbt.barsgl.ejb.entity.dict.Acod;
import ru.rbt.barsgl.ejb.repository.dict.AcodRepository;
import ru.rbt.barsgl.shared.dict.ActParmWrapper;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.ejbcore.util.DateUtils;

import javax.inject.Inject;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;


/**
 * Created by akichigi on 24.08.16.
 */
public class ActParmRepository extends AbstractBaseEntityRepository<ActParm, ActParmId> {
    @Inject
    private DateUtils dateUtils;

    @Inject
    private AcodRepository acodRepository;

    public  boolean isAccTypeExists(String accType) {
        return null != selectFirst(ActParm.class, "from ActParm T where T.id.accType = ?1", accType);
    }

    public boolean isActParmExists(ActParmWrapper wrapper) throws ParseException {
        return  null != selectFirst(ActParm.class, "from ActParm T where T.id.accType =?1 and T.id.cusType =?2 and " +
        "T.id.term =?3 and T.id.acc2 =?4 and T.id.dtb =?5", wrapper.getAccType(), wrapper.getCusTypeRightPad(),
                wrapper.getTerm(), wrapper.getAcc2(), dateUtils.onlyDateParse(wrapper.getDtb()));
    }

/*
    public boolean isActParmExists(ActParmWrapper wrapper) throws ParseException {
        return  null != selectFirst(ActParm.class, "from ActParm T where T.id.accType =?1 and T.id.cusType =?2 and " +
                        "T.id.term =?3 and T.id.acc2 =?4 and  coalesce(T.plcode, '') =?5 and T.acod =?6 and T.ac_sq =?7",  wrapper.getAccType(), wrapper.getCusType(),
                wrapper.getTerm(), wrapper.getAcc2(), wrapper.getPlcode() == null ? "" : wrapper.getPlcode(), wrapper.getAcod(), wrapper.getAc_sq());
    }
*/

    /*public boolean isParmDateClosed(ActParmWrapper wrapper) throws ParseException{
         return  null == selectFirst(ActParm.class, "from ActParm T where T.id.accType =?1 and T.id.cusType =?2 and " +
                        "T.id.term =?3 and T.id.acc2 =?4 and T.dte is null", wrapper.getAccType(), wrapper.getCusType(),
                wrapper.getTerm(), wrapper.getAcc2());

    }*/

    public boolean isParmDateClosed(ActParmWrapper wrapper) throws ParseException, SQLException{
        return 0 == select("select 1 from gl_ActParm where accType =? and cusType =? and term = ? and (dte is null or (? between dtb and dte))",
                wrapper.getAccType(), wrapper.getCusTypeRightPad(), wrapper.getTerm(), dateUtils.onlyDateParse(wrapper.getDtb())).size();
//        return  null == selectFirst(ActParm.class, "from ActParm T where T.id.accType =?1 and T.cusType =?2 and " +
//                        "T.term =?3 and T.acc2 =?4 and (T.dte is null or T.dte >= ?5) order by T.dtb desc", wrapper.getAccType(), wrapper.getCusType(),
//                wrapper.getTerm(), wrapper.getAcc2(), dateUtils.onlyDateParse(wrapper.getDtb()));
    }

    public boolean isParmDateClosedForUpdate(ActParmWrapper wrapper) throws ParseException, SQLException{
        return 0 == select("select 1 from gl_ActParm where accType =? and cusType =? and term = ? and "+
                "(dtb < ? and ? <= nvl(dte,to_date('01.01.2029','dd.mm.yyyy')) or (dtb < = ? and ? < dtb))",
                wrapper.getAccType(), wrapper.getCusTypeRightPad(), wrapper.getTerm(),
                dateUtils.onlyDateParse(wrapper.getDtb()),
                dateUtils.onlyDateParse(wrapper.getDtb()),
                dateUtils.DateParse2029(wrapper.getDte()),
                dateUtils.onlyDateParse(wrapper.getDtb())).size();
    }


    public boolean isAcc2Exists(String acc2){
        try {
            return null != selectFirst("select 1 from BSS where ACC2=?", acc2);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public boolean isAcodeExists(String acod){
        try {
            return null != selectFirst("select 1 from GL_ACOD where ACOD=?", acod);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public boolean allowUseAcodMT1000(String acod){
          Acod res = acodRepository.getNotUsedAcod(acod);
          return ((res == null) || (Long.parseLong(acod) < 1000));
    }

    public boolean isPlCodeExists(ActParmWrapper wrapper){
        try{
            String sql = "select 1 from gl_plcode where plcode=? and dat <= to_date('" + wrapper.getDtb() +
                         "', 'dd.mm.yyyy') and nvl(datto, to_date('2029-01-01', 'yyyy-mm-dd')) >= to_date('" + wrapper.getDtb() + "', 'dd.mm.yyyy')";
            return null != selectFirst(sql, wrapper.getPlcode());
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public boolean isPlCodeExists(String plCode, Date dtb){
        try{
            String sql = "select 1 from gl_plcode where plcode= ? and dat <= ? " +
                    " and nvl(datto, to_date('2029-01-01', 'yyyy-mm-dd')) >= ?";
            return null != selectFirst(sql, plCode, dtb, dtb);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public String getPsav(String acc2){
        try{
            DataRecord rec = selectFirst("select psav from BSS where acc2 =?", acc2);

            return rec == null ? null : rec.getString("psav");
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public boolean isActParmInAcc(ActParmWrapper wrapper){
       String sql = "SELECT ID\n" +
               "FROM GL_ACC \n" +
               "WHERE ACCTYPE = ? AND ACC2 = ? AND\n" +
               "((NVL(CBCUSTTYPE, 0) = CAST(? AS NUMBER(3,0))) OR (NVL(CBCUSTTYPE, 0) = 0) OR (CAST(? AS NUMBER(3,0)) = 0)) AND  \n" +
               "((NVL(TERM, 0) = CAST(? AS NUMBER(2,0))) OR ((NVL(TERM,0) = 0) OR (CAST(? AS NUMBER(2,0)) = 0)))";
       try {
            return null != selectFirst(sql, wrapper.getAccType(), wrapper.getAcc2(), wrapper.getCusType(), wrapper.getCusType(),
                    wrapper.getTerm(), wrapper.getTerm());
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * поиск технического acctype
     * @param accountingType
     * @return
     */
    public ActParm findTechnicalActparm (AccountingType accountingType) {
        List<ActParm> actParms = findNative(ActParm.class, "select * from gl_actparm p where p.acctype = ?", 10, accountingType.getId());
        if (actParms.isEmpty()) {
            return null;
        } else if (1 < actParms.size()) {
            throw new DefaultApplicationException(format("Более одной записи в gl_actparm по acctype '%s'", accountingType.getId()));
        } else {
            return actParms.get(0);
        }
    }


}
