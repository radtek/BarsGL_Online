package ru.rbt.barsgl.ejb.integr.oper;

import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.entity.gl.AbstractPd;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPd;
import ru.rbt.barsgl.ejb.entity.gl.GlPdTh;
import ru.rbt.barsgl.ejb.integr.ValidationAwareHandler;
import ru.rbt.barsgl.ejb.repository.GlPdThRepository;
import ru.rbt.barsgl.ejb.security.UserContext;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.barsgl.shared.operation.ManualTechOperationWrapper;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.ejbcore.validation.ErrorCode.*;
import static ru.rbt.ejbcore.validation.ErrorCode.DATE_AFTER_OPERDAY;
import static ru.rbt.ejbcore.validation.ErrorCode.DATE_IS_HOLIDAY;
import static ru.rbt.barsgl.shared.enums.PostingChoice.PST_ALL;
import static ru.rbt.barsgl.shared.enums.PostingChoice.PST_ONE_OF;

/**
 * Created by er23851 on 20.04.2017.
 */
public class EditPdThProcessor extends ValidationAwareHandler<ManualTechOperationWrapper> {

    @EJB
    private GlPdThRepository glPdThRepository;

    @Inject
    private DateUtils dateUtils;

    @Inject
    private UserContext userContext;

    @Inject
    private BankCalendarDayRepository calendarDayRepository;


    @Override
    public void fillValidationContext(ManualTechOperationWrapper target, ValidationContext context) {
        // ============== Общее ===============
        // Value Date
        context.addValidator(() -> {
            checkDate(target, target.getValueDateStr(), "валютирования", false);
        });
        // Posting Date
        context.addValidator(() -> {
            checkDate(target, target.getPostDateStr(), "проводки", true);
            Date pdate = checkDateFormat(target.getPostDateStr(), "Дата проводки");
            Date vdate = checkDateFormat(target.getValueDateStr(), "Дата валютирования");
            if ( (null != pdate )&& (null != vdate) && vdate.after(pdate)) {
                throw new ValidationError(POSTDATE_NOT_VALID,
                        target.getPostDateStr(),
                        target.getValueDateStr());
            }
        });
        // ИД платежа
        context.addValidator(() -> {
            String fieldName = "Номер платежного документа";
            String fieldValue = target.getPaymentRefernce();
            int maxLen = 20;
            if ( null != fieldValue && maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        // Номер сделки
        context.addValidator(() -> {
            String fieldName = "Номер сделки";
            String fieldValue = target.getDealId();
            int maxLen = 20;
            if (null != fieldValue && maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        // Номер субсделки
        context.addValidator(() -> {
            String fieldName = "Номер субсделки";
            String fieldValue = target.getSubdealId();
            int maxLen = 20;
            if (null != fieldValue) {
                if (maxLen < fieldValue.length())
                    throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });

        // описания
        context.addValidator(() -> {
            String fieldName = "Основание ENG";
            String fieldValue = target.getNarrative();
            int maxLen = 300;
            if (null != fieldValue && maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        context.addValidator(() -> {
            String fieldName = "Основание RUS";
            String fieldValue = target.getRusNarrativeLong();
            int maxLen = 300;
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            } else if (maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
    }

    public Date checkDate(ManualOperationWrapper target, String checkDateStr, String fieldName, boolean checkHoliday) {
        Date currentDate = userContext.getCurrentDate();     // текущий опердень
        Date checkDate = null;
        try {
            if(null != checkDateStr)
                checkDate = new SimpleDateFormat(target.dateFormat).parse(checkDateStr);
        } catch (ParseException e) {

        }
        if ( null == checkDate ) {
            throw new ValidationError(DATE_AFTER_OPERDAY, fieldName,
                    "не задана",
                    dateUtils.onlyDateString(currentDate));
        } else if (checkDate.after(currentDate)){
            throw new ValidationError(DATE_AFTER_OPERDAY, fieldName,
                    dateUtils.onlyDateString(checkDate),
                    dateUtils.onlyDateString(currentDate));
        } else if (checkHoliday && !calendarDayRepository.isWorkday(checkDate)) {
            // TODO проверка на выходные
            throw new ValidationError(DATE_IS_HOLIDAY, fieldName,
                    dateUtils.onlyDateString(checkDate));
        }
        return checkDate;
    }

    public Date checkDateFormat(String dateStr, String name)
    {
        try {
            return dateUtils.onlyDateParse(dateStr);
        }catch (ParseException e){
            throw new ValidationError(BAD_DATE_FORMAT, name, dateStr);
        }
    }

    public List<Long> getOperationPdIdList(long parentId) {
        return glPdThRepository.getOperationPdThIdList(parentId);
    }

    public List<GlPdTh> getOperationPdList(List<Long> pdIdList) {
        return glPdThRepository.getOperationPdThList(pdIdList);
    }

    public List<GlPdTh> getOperationPdList(Long pcId) {
        return glPdThRepository.getOperationPdThListByPcId(pcId);
    }

    public void suppressAllPostings(ManualTechOperationWrapper wrapper, final List<GlPdTh> pdList)  throws Exception {

        // TODO при подавлении и изменении даты проводки (брать мин дату)
        String invisible = wrapper.isInvisible() ? "1" : "0";
        for (GlPdTh pd : pdList) {
            pd.setInvisible(invisible);
        };
    }

    public void updatePd(List<GlPdTh> pdList) {
        pdList.forEach(pd -> glPdThRepository.update(pd));
    }

    /**
     * Обновляем для всех проводок по операции: Deal / Subdeal - только для ручных, Профитцентр - для всех операций
     * @param wrapper
     * @param pdList
     */
    public void updateAllPostings(ManualTechOperationWrapper wrapper, List<GlPdTh> pdList) throws Exception {
        Date valueDate = checkDateFormat(wrapper.getValueDateStr(), "Дата валютирования");
        Date postDate = checkDateFormat(wrapper.getPostDateStr(), "Дата проводки");

        for (GlPdTh pd : pdList) {
            if (InputMethod.AE != wrapper.getInputMethod()) {       // M || F
                pd.setDealId(wrapper.getDealId());
                pd.setSubdealId(wrapper.getSubdealId());
                pd.setPaymentRef(wrapper.getDealId());
                pd.setPnar(glPdThRepository.getPnarManual(wrapper.getDealId(), wrapper.getSubdealId(), wrapper.getPaymentRefernce()));
            }

            pd.setNarrative(wrapper.getNarrative());
            pd.setRusNarrLong(wrapper.getRusNarrativeLong());
            pd.setIsCorrection(YesNo.getValue(wrapper.isCorrection()));
            pd.setVald(valueDate);
            pd.setPod(postDate);
            pd.setProfitCenter(wrapper.getProfitCenter());
        }
    }
}
