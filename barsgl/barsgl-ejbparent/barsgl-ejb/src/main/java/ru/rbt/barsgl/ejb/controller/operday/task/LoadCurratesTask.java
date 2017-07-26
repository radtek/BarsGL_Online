package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.BankCalendarDay;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.dict.CurrencyRate;
import ru.rbt.barsgl.ejb.entity.etl.EtlCurrencyRate;
import ru.rbt.barsgl.ejb.entity.etl.EtlCurrencyRateId;
import ru.rbt.barsgl.ejb.repository.BankCurrencyRepository;
import ru.rbt.barsgl.ejb.repository.RateRepository;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.controller.etc.TextResourceController;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.LoadRatesTask;
import static ru.rbt.barsgl.ejb.controller.operday.task.LoadCurratesTask.HardCurrency.*;
import static ru.rbt.barsgl.ejb.controller.operday.task.LoadCurratesTask.LoadCurrencyPath.*;

/**
 * Created by Ivan Sevastyanov
 */
public class LoadCurratesTask extends AbstractJobHistoryAwareTask {

    public static final Logger log = Logger.getLogger(LoadCurratesTask.class.getName());

//    @Inject
//    private OperdayController operdayController;

//    @Inject
//    private DateUtils dateUtils;

    @Inject
    private BankCalendarDayRepository calendarDayRepository;

    @Inject
    private RateRepository rateRepository;

    @Inject
    private BankCurrencyRepository currencyRepository;

//    @EJB
//    private AuditController auditController;

    @Inject
    private TextResourceController textResourceController;

    @EJB
    private CoreRepository repository;

    private enum RateChangeResult {
        INSERT, UPDATE, NOTHING
    }

    public static final String BYR_CODE = "BYR";
    public static final String BYB_CODE = "BYB";

    public enum HardCurrency {
        USD, EUR, GBP, CHF, JPY
    }

    public enum LoadCurrencyPath {
        Operday, OperdayToOpen, RatesToLoad
    }

    @Override
    protected void initExec(String jobName, Properties properties) {
        try {
            Operday currentOperday = operdayController.getOperday();
            properties.put(Operday, currentOperday);
            properties.put(OperdayToOpen
                    , calendarDayRepository.getWorkdayAfter(currentOperday.getCurrentDate()).getId().getCalendarDate());
            properties.put(RatesToLoad, calendarDayRepository.findNative(EtlCurrencyRate.class,
                    textResourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/rates_dmart_check.sql"), -1
                    , currentOperday.getCurrentDate(), properties.get(OperdayToOpen)));
        } catch (Throwable e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        Operday currentOperday = (Operday) properties.get(Operday);
        Date operDayToOpen = (Date) properties.get(OperdayToOpen);
        List<EtlCurrencyRate> etlRates = (List<EtlCurrencyRate>) properties.get(RatesToLoad);

        auditController.info(LoadRatesTask, format("Загрузка курсов валют за дату следующего операционного дня: '%s'"
                , dateUtils.onlyDateString(operDayToOpen)));

        log.info(format("Найдено '%s' строк в таблице GL_ETLRATE (on_date = '%s') на для даты открываемого ОД '%s'"
                , etlRates.size(), dateUtils.onlyDateString(etlRates.get(0).getId().getRateDt()), dateUtils.onlyDateString(operDayToOpen)));


        List<BankCalendarDay> holidays = calendarDayRepository
                .getCalendarDays(dateUtils.addDays(currentOperday.getCurrentDate(), 1), dateUtils.addDays(operDayToOpen, -1));
        if (!holidays.isEmpty()) {
            log.info(format("Выходных дней между '%s' и '%s': '%s'"
                    , dateUtils.onlyDateString(currentOperday.getCurrentDate()), dateUtils.onlyDateString(operDayToOpen), holidays.size()));
            for (BankCalendarDay day : holidays) {
                try {
                    repository.executeInNewTransaction(persistence -> loadRatesOnDate(day.getId().getCalendarDate(), etlRates));
                } catch (Throwable e) {
                    auditController.error(LoadRatesTask
                            , format("Ошибка при загрузке курсов (нерабочий день) за '%s'", dateUtils.onlyDateString(day.getId().getCalendarDate())), null, e);
                }
            }
        } else {
            log.info(format("Нет выходных дней между '%s' и '%s'"
                    , dateUtils.onlyDateString(currentOperday.getCurrentDate()), dateUtils.onlyDateString(operDayToOpen)));
        }

        try {
            repository.executeInNewTransaction(persistence -> loadRatesOnDate(operDayToOpen, etlRates));
        } catch (Throwable e) {
            auditController.error(LoadRatesTask
                    , format("Ошибка при загрузке курсов (след рабочий день) за '%s'", dateUtils.onlyDateString(operDayToOpen)), null, e);
            return false;
        }
        return true;
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        Date operdayToOpen = (Date) properties.get(OperdayToOpen);
        int cnt = rateRepository.selectFirst("select count(1) cnt from currates where dat = ? and ccy in (?,?,?,?,?)"
                , operdayToOpen, CHF.name(), USD.name(), GBP.name(), EUR.name(), JPY.name()).getInteger("cnt");
        boolean isAlready = 5 == cnt;
        boolean isEmptyMart = 0 == ((List<EtlCurrencyRate>)properties.get(RatesToLoad)).size();
        if (isAlready || isEmptyMart) {
            auditController.warning(LoadRatesTask
                    , format("Загрузка курсов валют для даты '%s' невозможна", dateUtils.onlyDateString(operdayToOpen)), null
                    , format("Не заполнена витрина данных <%s>, курсы уже были загружены ранее <%s>", isEmptyMart, isAlready));
            return false;
        } else {
            return true;
        }
    }

    private RateChangeResult createOrUpdateRate(EtlCurrencyRate etlRate
            , Date currentOperday, Date operDayToOpen) {
        CurrencyRate rate = rateRepository.findRate(new BankCurrency(etlRate.getId().getBankCurrency()), operDayToOpen);
        if (null == rate) {
            log.info(format("Курс для валюты '%s' на дату '%s' не найден. Добавляем из GL_ETLRATE"
                    , etlRate.getId().getBankCurrency(), dateUtils.onlyDateString(operDayToOpen)));
            CurrencyRate rateCurrent = rateRepository.findRate(new BankCurrency(etlRate.getId().getBankCurrency()), currentOperday);
            BigDecimal rate0 = (rateCurrent != null) ? rateCurrent.getRate() : null;
            rate = new CurrencyRate(new BankCurrency(etlRate.getId().getBankCurrency())
                    , operDayToOpen, getRate(etlRate), getAmount(etlRate), rate0);
            rate = rateRepository.save(rate);
            log.info(format("Курс '%s' валюты '%s' на дату '%s' успешно сохранен"
                    , decimalToString(rate.getRate()), rate.getId().getBankCurrency(), rate.getId().getRateDt()));
            return RateChangeResult.INSERT;
        } else {
            log.info(format("Найден курс для валюты '%s' на дату '%s'. Изменяем..."
                    , etlRate.getId().getBankCurrency(), dateUtils.onlyDateString(operDayToOpen)));
            rate.setRate(getRate(etlRate));
            rate = rateRepository.update(rate);
            log.info(format("Курс '%s' валюты '%s' на дату '%s' успешно изменен"
                    , decimalToString(rate.getRate()), rate.getId().getBankCurrency(), rate.getId().getRateDt()));
            return RateChangeResult.UPDATE;
        }
    }

    /**
     * установка курса для белорусских рублем по-старому
     */
    private void setBYBRate(EtlCurrencyRate etlRate, Date dateOfRate, Date operdayToOpen) {
        if (etlRate.getId().getBankCurrency().equals(BYR_CODE)) {
            EtlCurrencyRate rateBYB = new EtlCurrencyRate();
            rateBYB.setId(new EtlCurrencyRateId(BYB_CODE, etlRate.getId().getRateDt()));
            rateBYB.setRate(etlRate.getRate());
            rateBYB.setNominal(etlRate.getNominal());
            rateBYB.setCurrencyName(etlRate.getCurrencyName());
            rateBYB.setDigitalCode(etlRate.getDigitalCode());
            createOrUpdateRate(rateBYB, dateOfRate, operdayToOpen);
        }
    }

    private BigDecimal getRate(EtlCurrencyRate etlRate) {
        return etlRate.getRate().divide(new BigDecimal(etlRate.getNominal())).setScale(9, BigDecimal.ROUND_HALF_UP);
    }

    private BigDecimal getAmount(EtlCurrencyRate etlRate) {
        return calcTenPow(2 - currencyRepository.findById(BankCurrency.class, etlRate.getId().getBankCurrency()).getScale().intValue());
    }

    private BigDecimal calcTenPow(int pow) {
        if (pow >= 0) {
            return BigDecimal.TEN.pow(pow);
        } else {
            return BigDecimal.ONE.divide(BigDecimal.TEN.pow(pow*-1));
        }
    }

    private String decimalToString(BigDecimal decimal) {
        return new DecimalFormat("#.######").format(decimal.doubleValue());
    }

    /**
     * дата в которую ложатся курсы
     * @param date
     */
    private void loadPreviousCurrates(Date date, Date workdayBefore) {
        rateRepository.executeNativeUpdate(
                "insert into currates (" +
                        //"select date('" + dateUtils.dbDateString(date) + "'),c.CCY,c.RATE,c.AMNT,c.RATE\n" +
                        "select to_date('" + dateUtils.dbDateString(date)+ "', 'yyyy-MM-dd'),c.CCY,c.RATE,c.AMNT,c.RATE\n" +
                        "  from currates c\n" +
                        " where c.dat = ?\n" +
                        "   and not exists (select 1 from currates c2 where c2.dat = ? and c2.ccy = c.ccy))", workdayBefore, date);
    }

    private int loadRatesOnDate(Date dateOfRate, List<EtlCurrencyRate> etlRates) {
        Date prevOperday = calendarDayRepository.getWorkdayBefore(dateOfRate).getId().getCalendarDate();
        loadPreviousCurrates(dateOfRate, prevOperday);
        int inserted = 0;
        int updated = 0;
        for (EtlCurrencyRate etlRate : etlRates) {
            BankCurrency currency = currencyRepository.selectFirst(BankCurrency.class
                    , "from BankCurrency c where c.id = ?1", etlRate.getId().getBankCurrency());
            if (null != currency) {
                // старый белорусский рубль, для Майдаса
                setBYBRate(etlRate, prevOperday, dateOfRate);
                if (RateChangeResult.INSERT == createOrUpdateRate(etlRate, prevOperday, dateOfRate)) {
                    inserted++;
                } else {
                    updated++;
                }
            } else {
                log.log(Level.WARNING, format("Валюта не используется: '%s'. Пропускаем..."
                        , etlRate.getId().getBankCurrency()));
            }
        }
        auditController.info(LoadRatesTask, format("Получено из DWH '%s', загружено (ins/upd) '%s/%s' валют на дату '%s'"
                , etlRates.size(), inserted, updated, dateUtils.onlyDateString(dateOfRate)));
        return inserted;
    }
}
