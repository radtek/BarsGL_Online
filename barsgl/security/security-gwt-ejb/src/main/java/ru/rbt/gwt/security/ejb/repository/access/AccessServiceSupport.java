package ru.rbt.gwt.security.ejb.repository.access;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.security.entity.AppUser;
import ru.rbt.security.entity.access.PrmValue;
import ru.rbt.security.entity.access.PrmValueHistory;
import ru.rbt.security.ejb.repository.AppUserRepository;
import ru.rbt.security.ejb.repository.access.PrmValueRepository;
import ru.rbt.security.ejb.repository.access.PrmValueHistoryRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.access.PrmValueWrapper;
import ru.rbt.shared.ctx.UserRequestHolder;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.shared.enums.PrmValueEnum;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.lang.String.format;
import java.util.ArrayList;
import java.util.List;
import static ru.rbt.audit.entity.AuditRecord.LogCode.BackValue;
import static ru.rbt.audit.entity.AuditRecord.LogCode.User;
import static ru.rbt.shared.ExceptionUtils.getErrorMessage;
import ru.rbt.shared.security.RequestContext;
import ru.rbt.shared.user.AppUserWrapper;

/**
 * Created by akichigi on 07.04.16.
 */
public class AccessServiceSupport {

    private static Logger log = Logger.getLogger(AccessServiceSupport.class);

    @EJB
    private AuditController auditController;

    @Inject
    private PrmValueRepository prmValueRepository;

    @Inject
    private BankCalendarDayRepository calendarRepository;

    @Inject
    private OperdayController operdayController;

    @Inject
    private RequestContext contextBean;

    @Inject
    private PrmValueHistoryRepository historyRepository;

    @Inject
    private AppUserRepository appUserRepository;

    @Inject
    private DateUtils dateUtils;

    public RpcRes_Base<PrmValueWrapper> getBackValue(int userId, PrmValueEnum prmCode){
        try {

            PrmValueWrapper wrapper = new PrmValueWrapper();

            AppUser user = appUserRepository.selectFirst(AppUser.class, "from AppUser u where u.id = ?1", userId);
            if (null == user) {
                return new RpcRes_Base<PrmValueWrapper>(wrapper, true, "Пользователь не существует!");
            }
            if (user.getLocked().equals("1") || user.getEnd_Date() != null){
                return new RpcRes_Base<PrmValueWrapper>(null, true, "Неактивный пользователь не может получить доступ к архиву!");
            }

            PrmValue prm = prmValueRepository.selectFirst(PrmValue.class,
                    "from PrmValue p where p.userId = ?1 and p.prmCode = ?2",
                    userId, prmCode);
            Date currentDate = operdayController.getOperday().getCurrentDate();

            if (prm == null){
                wrapper.setAction(FormAction.CREATE);
                wrapper.setId(-1L);
                wrapper.setUserId(userId);
                wrapper.setPrmCode(prmCode);
                wrapper.setDateBeginStr(currentDate == null ? null : dateUtils.onlyDateString(currentDate));
                wrapper.setDateEndStr(currentDate == null ? null : dateUtils.onlyDateString(currentDate));
                wrapper.setPrmValue("0");
                wrapper.setOperDayDateStr(currentDate == null ? null : dateUtils.onlyDateString(currentDate));
            }
            else{
                wrapper.setAction(FormAction.UPDATE);
                wrapper.setId(prm.getId());
                wrapper.setUserId(prm.getUserId());
                wrapper.setPrmCode(prm.getPrmCode());
                wrapper.setPrmValue(prm.getPrmValue());
                wrapper.setDateBeginStr(prm.getDateBegin() == null ? null : dateUtils.onlyDateString(prm.getDateBegin()));
                wrapper.setDateEndStr(prm.getPrmValue().equals("0")
                        ? (currentDate == null ? null : dateUtils.onlyDateString(currentDate))
                        : (prm.getDateEnd() == null ? null : dateUtils.onlyDateString(prm.getDateEnd())));
                wrapper.setOperDayDateStr(currentDate == null ? null : dateUtils.onlyDateString(currentDate));
            }
            return new RpcRes_Base<>(wrapper, false, "");
        }catch (Exception e){
            String errMessage = getErrorMessage(e);
            auditController.error(User, format("Ошибка при получении  параметра: '%s'", prmCode), null, e);
            return new RpcRes_Base<PrmValueWrapper>(null, true, errMessage);
        }
    }

    public RpcRes_Base<PrmValueWrapper> setBackValue(PrmValueWrapper wrapper){
        FormAction action = FormAction.OTHER;

        try {

            PrmValue prm = prmValueRepository.selectFirst(PrmValue.class,
                     "from PrmValue p where p.userId = ?1 and p.prmCode = ?2",
                     wrapper.getUserId(), wrapper.getPrmCode());

            Date sysDate = operdayController.getSystemDateTime();

            if (null != prm) {

                //write to history
                PrmValueHistory history = new PrmValueHistory();

                history.setPrmId(prm.getId());
                history.setUserId(prm.getUserId());
                history.setPrmCode(prm.getPrmCode());
                history.setPrmValue(prm.getPrmValue());
                history.setDateBegin(prm.getDateBegin());
                history.setDateEnd(prm.getDateEnd());
                history.setUserAut(prm.getUserAut());
                history.setDateAut(prm.getDateAut());
                history.setUserSys(getUserAut());
                history.setDateSys(sysDate);
                history.setChngType("U");

                historyRepository.save(history);

                //Update
                action = FormAction.UPDATE;

                prm.setUserId(wrapper.getUserId());
                prm.setPrmCode(wrapper.getPrmCode());
                prm.setPrmValue(wrapper.getPrmValue());
                prm.setDateBegin(dateUtils.onlyDateParse(wrapper.getDateBeginStr()));
                prm.setDateEnd("0".equals(wrapper.getPrmValue()) ? null : dateUtils.onlyDateParse(wrapper.getDateEndStr()));
                prm.setUserAut(getUserAut());
                prm.setDateAut(sysDate);

                prmValueRepository.update(prm);

            } else{
                //Create
                action = FormAction.CREATE;

                prm = new PrmValue();
                prm.setUserId(wrapper.getUserId());
                prm.setPrmCode(wrapper.getPrmCode());
                prm.setPrmValue(wrapper.getPrmValue());
                prm.setDateBegin(dateUtils.onlyDateParse(wrapper.getDateBeginStr()));
                prm.setDateEnd("0".equals(wrapper.getPrmValue()) ? null : dateUtils.onlyDateParse(wrapper.getDateEndStr()));
                prm.setUserAut(getUserAut());
                prm.setDateAut(sysDate);

                prmValueRepository.save(prm);
            }

            String str = action.equals(FormAction.CREATE) ? "создан": "обновлен";
            auditController.info(BackValue, format("Пользователю id=%d " + str + " параметр '%s' со значением '%s'",
                   wrapper.getUserId(), wrapper.getPrmCode(), wrapper.getPrmValue()));

            return new RpcRes_Base<>(
                    wrapper, false, format(str + " параметр: '%s', значение '%s'",
                    wrapper.getPrmCode(), wrapper.getPrmValue()));
        } catch (Exception e) {
            String str = action.equals(FormAction.CREATE) ? "создании": "обновлении";
            String errMessage = getErrorMessage(e);
            auditController.error(BackValue, format("Ошибка при " + str + " параметра '%s' значением '%s'",
                    wrapper.getPrmCode(), wrapper.getPrmValue()), null, e);
            return new RpcRes_Base<PrmValueWrapper>(wrapper, true, errMessage);
        }
    }

    public void checkUserAccessToBackValueDate(Date date, Long userId) throws Exception {
        Date operDay = operdayController.getOperday().getCurrentDate();
        //Debug for timezone bug
        //auditController.info(BackValue, format( "TimeZone debug A: OD => %s  WD => %s", operDay.toString(), date.toString()));
        //auditController.info(BackValue, format("TimeZone debug B: OD => %d  WD => %d", operDay.getTime(), date.getTime()));

        if (operDay.compareTo(date) != 1) return;
        // Производим проверки если operDay > date (т.е. в архиве)

        PrmValue prm = prmValueRepository.selectFirst(PrmValue.class,
                "from PrmValue p where p.userId = ?1 and p.prmCode = ?2",
                userId, PrmValueEnum.BackValue);
        if (prm == null || prm.getPrmValue().equals("0"))
            throw new ValidationError(ErrorCode.POSTING_BACK_NOT_ALLOWED);

        if ((operDay.compareTo(prm.getDateBegin()) == -1) ||
                (prm.getDateEnd() != null && operDay.compareTo(prm.getDateEnd()) == 1))
            throw new ValidationError(ErrorCode.POSTING_BACK_NOT_IN_DATE,
                    new SimpleDateFormat("dd.MM.yyyy").format(prm.getDateBegin()),
                    prm.getDateEnd() == null ? "" : "по " + new SimpleDateFormat("dd.MM.yyyy").format(prm.getDateEnd()));

/*
        DataRecord rec = prmValueRepository.selectFirst(format("select min(d.dat) as dat\n" +
                "from (\n" +
                "\tselect dat \n" +
                "\tfrom dwh.cal\n" +
                "\twhere ccy='RUR' and thol not in ('X', 'T') and dat< (select curdate from dwh.gl_od)\n" +
                "\torder by 1 desc\n" +
                "\tfetch first %s rows only) d", prm.getPrmValue()));
*/
        Date bvDate = calendarRepository.getWorkDateBefore(operDay, Integer.parseInt(prm.getPrmValue()), false);

        if (bvDate == null || bvDate.compareTo(date) == 1)
            throw new ValidationError(ErrorCode.POSTING_BACK_NOT_IN_DAYS, new SimpleDateFormat("dd.MM.yyyy").format(bvDate));
    }

    public AppUserWrapper getGrantedBranchesAndSourses(AppUser user){
        AppUserWrapper wrapper = new AppUserWrapper();
        ArrayList<String> grantedHeadBranches = new ArrayList<>();
        ArrayList<String> grantedSources = new ArrayList<>();

        String query = "from PrmValue p where p.userId = ?1 and p.prmCode = ?2";

        List<PrmValue> prmValues = prmValueRepository.select(PrmValue.class, query, user.getId(), PrmValueEnum.HeadBranch);
        for(PrmValue value : prmValues){
            grantedHeadBranches.add(value.getPrmValue());
        }

        prmValues = prmValueRepository.select(PrmValue.class, query, user.getId(), PrmValueEnum.Source);
        for(PrmValue value : prmValues){
            grantedSources.add(value.getPrmValue());
        }

        wrapper.setGrantedHeadBranches(grantedHeadBranches);
        wrapper.setGrantedSources(grantedSources);

        return wrapper;
    }

    private String getUserAut(){
        UserRequestHolder requestHolder = contextBean.getRequest().orElse(UserRequestHolder.empty());
        return requestHolder.getUser();
    }
}