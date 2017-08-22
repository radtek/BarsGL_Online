package ru.rbt.barsgl.ejb.integr.bg;

import org.apache.commons.lang3.time.DateUtils;
import ru.rbt.barsgl.ejb.entity.acc.AccCardId;
import ru.rbt.barsgl.ejb.entity.acc.GLAccCard;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.repository.AccCardRepository;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.operation.CardReportWrapper;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;

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

    @Inject
    private AccCardRepository cardRepository;

    @EJB
    GLAccountRepository accountRepository;

    // TODO заглушка
    public RpcRes_Base<CardReportWrapper> getCardReport(CardReportWrapper wrapper) throws Exception {
        return new RpcRes_Base<>(wrapper, false, "");
    }

    private void createCardReport(Date procdate, String filial) {
        try {
            Date finalDate = databaseDate.parse(finalDateStr);

            // TODO транзакции !!
            List<DataRecord> accList = cardRepository.getAccTurnovers(procdate, filial);

            for(DataRecord res: accList) {
                Date pod = res.getDate(0);
                String bsaAcid = res.getString(1);
                BigDecimal turnover = res.getBigDecimal(2);
                GLAccCard accCardWas = cardRepository.findById(GLAccCard.class, new AccCardId(bsaAcid, pod));
                if (null != accCardWas) {
                    // запись с такой датой есть
                    accCardWas.setTurnovers(turnover);
                    if (pod.before(finalDate)) {
                        // это не последняя запись по счету
                        // TODO 3.3
                    }
                    cardRepository.update(accCardWas);
                } else {
                    // найти запись с максимальной датой < POD
                    Date maxBefore = cardRepository.getMaxDateBefore(bsaAcid, pod);
                    if (null == maxBefore) {                        // это будет первая запись по счету
                        GLAccount glAcc = accountRepository.selectFirst(GLAccount.class, "from GLAccount a where a.bsaAcid = ?1", bsaAcid);
                        if (null == glAcc)
                            throw new DefaultApplicationException(String.format("Не найден счет '%s' в таблице GL_ACC", bsaAcid));
                        GLAccCard accCardNew = cardRepository.createAccCardRecord(bsaAcid, pod, turnover, glAcc);
                        cardRepository.save(accCardNew);
                    } else {
                        GLAccCard accCardPrev = cardRepository.findById(GLAccCard.class,new AccCardId(bsaAcid, maxBefore));
                        Date datTo = accCardPrev.getEndDate();
                        if (datTo.before(finalDate)) {
                            Date minAfter = cardRepository.getMinDateAfter(bsaAcid, pod);
                            datTo = DateUtils.addDays(minAfter, -1);
                        }
                        Date datePrev = DateUtils.addDays(pod, -1);        // TODO проверить для первого дня месяца

                        GLAccCard accCardNew = cardRepository.createAccCardRecord(bsaAcid, pod, datTo, turnover, accCardPrev);
                        accCardPrev.setEndDate(datePrev);

                        cardRepository.save(accCardPrev);
                        cardRepository.save(accCardNew);
                        if (datTo.before(finalDate)) {
                            // это не последняя запись по счету
                            // TODO 3.3
                        }
                    }
                }



            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }



}
