package ru.rbt.barsgl.ejb.cur_exchng;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.repository.BankCurrencyRepository;
import ru.rbt.barsgl.ejb.repository.RateRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.operation.CurExchangeWrapper;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Date;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.CurrencyExchange;
import static ru.rbt.shared.ExceptionUtils.getErrorMessage;

/**
 * Created by akichigi on 23.01.17.
 */
public class CurrencyExchangeSupport {
    private static Logger log = Logger.getLogger(CurrencyExchangeSupport.class);

    @EJB
    private AuditController auditController;

    @Inject
    private RateRepository rateRepository;

    @Inject
    private BankCurrencyRepository bankCurrencyRepository;

    @Inject
    private DateUtils dateUtils;

    public RpcRes_Base<CurExchangeWrapper> exchange(CurExchangeWrapper wrapper){
        try{
            BigDecimal rate;
            Date date = dateUtils.onlyDateParse(wrapper.getDate());
            BigDecimal sum;
            BankCurrency ccy_target;
            BankCurrency ccy_source;
            ccy_source = bankCurrencyRepository.refreshCurrency(wrapper.getSourceCurrency());
            ccy_target = bankCurrencyRepository.refreshCurrency(wrapper.getTargetCurrency());

            if (wrapper.getSourceCurrency().equalsIgnoreCase("RUR")){
                //convert from RUR
                rate = rateRepository.getRate(ccy_target, date);
                sum = rateRepository.getValEquivalent(ccy_target, rate, wrapper.getSourceSum());
            }else{
                //convert to RUR
                rate = rateRepository.getRate(ccy_source, date);
                sum = rateRepository.getEquivalent(rate, wrapper.getSourceSum());
            }
            wrapper.setTargetSum(sum);
            String info = format("На дату %s \n%s %s сконвертированы в %s %s \nпо курсу ЦБ %s", wrapper.getDate(), wrapper.getSourceSum().toPlainString(),
                    wrapper.getSourceCurrency(), wrapper.getTargetSum().toPlainString(), wrapper.getTargetCurrency(),
                    rate.setScale(4, BigDecimal.ROUND_HALF_UP).toPlainString());
            auditController.info(CurrencyExchange, info);

            return new RpcRes_Base<>(wrapper, false, info);
        } catch (Throwable e){
            auditController.error(CurrencyExchange, format("Ошибка конвертации валюты из %s в %s на дату %s", wrapper.getSourceCurrency(),
                    wrapper.getTargetCurrency(), wrapper.getDate()), null, e);
            return new RpcRes_Base<CurExchangeWrapper>(wrapper, true,  getErrorMessage(e));
        }
    }
}
