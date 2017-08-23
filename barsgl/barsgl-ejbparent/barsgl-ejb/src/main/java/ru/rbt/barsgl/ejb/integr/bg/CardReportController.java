package ru.rbt.barsgl.ejb.integr.bg;

import org.apache.commons.lang3.time.DateUtils;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.entity.acc.AccCardId;
import ru.rbt.barsgl.ejb.entity.acc.GLAccCard;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.repository.AccCardRepository;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.operation.CardReportWrapper;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.shared.ExceptionUtils;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by er18837 on 18.08.2017.
 */
public class CardReportController {
    private final static SimpleDateFormat databaseDate = new SimpleDateFormat("yyyy-MM-dd");
    private final static String finalDateStr = "2029-01-01";
    private final Date finalDate = getFinalDate();

    @Inject
    private AccCardRepository cardRepository;

    @EJB
    GLAccountRepository accountRepository;

    @EJB
    AuditController auditController;

    // TODO заглушка
    public RpcRes_Base<CardReportWrapper> getCardReport(CardReportWrapper wrapper) {
        Date dat;
        try {
            dat = new SimpleDateFormat(wrapper.getDateFormat()).parse(wrapper.getPostDateStr());
        } catch (ParseException e) {
            return new RpcRes_Base<>(wrapper, true, String.format("Неверный формат даты '%s', требуется '%s'"
                    , wrapper.getPostDateStr(), wrapper.getDateFormat()));
        }
        try {
            Date datStart = accountRepository.getDateStartCardPH();
            if (null == datStart || dat.before(datStart)) {
                wrapper.setReportSql(getReportSqlOld(dat, wrapper.getFilial()));
                wrapper.setComment("Рассчет по остаткам");
            } else {
                accountRepository.executeInNewTransaction(persistence -> {
                    createCardReport(dat, wrapper.getFilial()); return null;});
                wrapper.setReportSql(getReportSqlNew(dat, wrapper.getFilial()));
                wrapper.setComment("Рассчет по оборотам");
            }
            return new RpcRes_Base<>(wrapper, false, "");
        } catch (Throwable t) {
            auditController.error(AuditRecord.LogCode.CardReport, String.format("Ошибка при создании отчета по картотеке для филиала '%s' на дату '%s'"
                    , wrapper.getFilial(), wrapper.getPostDateStr()), null, t);
            return new RpcRes_Base<>(wrapper, true, ExceptionUtils.getErrorMessage(t));
        }
    }

    private void createCardReport(Date procdate, String filial) throws SQLException {
            // TODO транзакции !!
            List<DataRecord> accList = cardRepository.getFilalTurnoversOnDate(filial, procdate);
            String bsaAcidStep = "";
            Date datToStep = finalDate;
            for(DataRecord res: accList) {
                String bsaAcid = res.getString(0);
                if (bsaAcid.equals(bsaAcidStep) && datToStep.before(finalDate))
                    continue;           // повторно счет не обрабатываем, если в пред случае была не последняя запись по счету
                bsaAcidStep = bsaAcid;
                datToStep = finalDate;

                Date pod = res.getDate(1);
                BigDecimal turnover = res.getBigDecimal(2);
                GLAccCard accCardWas = cardRepository.findById(GLAccCard.class, new AccCardId(bsaAcid, pod));
                if (null != accCardWas) {
                    // запись с такой датой есть
                    datToStep = accCardWas.getEndDate();
                    accCardWas.setTurnovers(turnover);
                    if (!accCardWas.getEndDate().before(finalDate)) {
                        // последняя запись по счету
                        cardRepository.update(accCardWas);
                    } else {
                        // не последняя запись по счету - п. 3.3
                        createAccTurnoversAfterDate(accCardWas);
                    }
                } else {
                    // найти запись с максимальной датой < POD
                    Date maxBefore = cardRepository.getMaxDateBefore(bsaAcid, pod);
                    if (null == maxBefore) {
                        // это будет первая запись по счету
                        GLAccount glAcc = accountRepository.selectFirst(GLAccount.class, "from GLAccount a where a.bsaAcid = ?1", bsaAcid);
                        if (null == glAcc)
                            throw new DefaultApplicationException(String.format("Не найден счет '%s' в таблице GL_ACC", bsaAcid));
                        GLAccCard accCardNew = cardRepository.createAccCardRecord(glAcc, pod, finalDate, turnover);
                        cardRepository.save(accCardNew);
                    } else {
                        GLAccCard accCardPrev = cardRepository.findById(GLAccCard.class,new AccCardId(bsaAcid, maxBefore));
                        Date datTo = accCardPrev.getEndDate();
                        if (datTo.before(finalDate)) {
                            Date minAfter = cardRepository.getMinDateAfter(bsaAcid, pod);
                            datTo = DateUtils.addDays(minAfter, -1);
                        }
                        datToStep = datTo;
                        Date datPrev = DateUtils.addDays(pod, -1);        // TODO проверить для первого дня месяца

                        GLAccCard accCardNew = cardRepository.createAccCardRecord(accCardPrev, pod, datTo, turnover);
                        accCardPrev.setEndDate(datPrev);
                        cardRepository.save(accCardPrev);

                        if (!datTo.before(finalDate)) {
                            // последняя запись по счету
                            cardRepository.save(accCardNew);
                        } else {
                            // не последняя запись по счету - п. 3.3
                            createAccTurnoversAfterDate(accCardNew);
                        }
                    }
                }
            }
    }

    public void createAccTurnoversAfterDate(GLAccCard cardOne) throws SQLException {
        String bsaacid = cardOne.getId().getBsaAcid();
        Date dateOne = cardOne.getId().getStartDate();
        List<DataRecord> accList = cardRepository.getAccTurnoversAfterDate(bsaacid, dateOne);
        cardRepository.deleteAccBalanceAfterDate(bsaacid, dateOne);

        GLAccCard accCardPrev = cardOne;
        for (DataRecord res: accList) {
            Date pod = res.getDate(1);
            BigDecimal turnover = res.getBigDecimal(2);
            GLAccCard accCardNew = cardRepository.createAccCardRecord(accCardPrev, pod, finalDate, turnover);
            Date datPrev = DateUtils.addDays(pod, -1);        // TODO проверить для первого дня месяца
            accCardPrev.setEndDate(datPrev);
            cardRepository.save(accCardPrev);
            accCardPrev = accCardNew;
        }
        cardRepository.save(accCardPrev);

    }

    public String getReportSqlOld(Date dat, String filial) {
        String dateStr = databaseDate.format(dat);
        return String.format(
                "select a.branch" +
                "     , sum((value(b.obac,0) + value(b.dtac,0) + value(b.ctac,0) + value(c.dtac, 0) + value(c.ctac,0)) * 0.01) as sum" +
                "     , a.ccy, a.subdealid" +
                " from BALTUR b" +
                " left join GL_ACC a on b.bsaacid = a.bsaacid" +
                " left join GL_BALTUR c on c.bsaacid = b.bsaacid and c.dat <= '%s'" +
                "    where b.bsaacid in (select t.bsaacid from gl_acc t	where t.cbcc = '%s' and t.acc2 in ('90901','90902') and t.subdealid  in ('1.2','2'))" +
                "	    and b.dat <= '%s' and b.datto >= '%s'" +
                " group by a.ccy, a.branch, a.subdealid" +
                " order by a.subdealid, a.branch, a.ccy", dateStr, filial, dateStr, dateStr);

    }

    public String getReportSqlNew(Date dat, String filial) {
        String dateStr = databaseDate.format(dat);
        return String.format(
                "select a.branch" +
                "     , sum(value(a.obac,0) + value(a.dtct,0)) as sum" +
                "     , a.ccy, a.subdealid" +
                " from GL_ACCCARD a" +
                "    where a.cbcc = '%s' and a.dat <= '%s' and a.datto >= '%s'" +
                " group by a.ccy, a.branch, a.subdealid" +
                " order by a.subdealid, a.branch, a.ccy", filial, dateStr, dateStr);
    }

    private Date getFinalDate() {
        try {
            return databaseDate.parse(finalDateStr);
        } catch (ParseException e) {
            return null;
        }
    }
}
