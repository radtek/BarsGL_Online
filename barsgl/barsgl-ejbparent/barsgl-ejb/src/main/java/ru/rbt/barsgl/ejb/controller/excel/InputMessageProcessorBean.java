package ru.rbt.barsgl.ejb.controller.excel;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.repository.BankCurrencyRepository;
import ru.rbt.barsgl.ejb.repository.EtlPackageRepository;
import ru.rbt.barsgl.ejb.repository.EtlPostingRepository;
import ru.rbt.barsgl.ejbcore.job.InmemoryJobservice;
import ru.rbt.ejbcore.mapping.YesNo;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.*;

import static java.lang.String.format;

/**
 * Created by Ivan Sevastyanov
 */
@Deprecated
@Stateless
public class InputMessageProcessorBean implements InputMessageProcessor <EtlPackage> {

    public static final Logger log = Logger.getLogger(InputMessageProcessorBean.class);

    @Inject
    private EtlPackageRepository etlPackageRepository;

    @EJB
    private EtlPostingRepository etlPostingRepository;

    @Inject
    private BankCurrencyRepository bankCurrencyRepository;

    @EJB
    private EtlPostingRepository postingRepository;

    @EJB
    private InmemoryJobservice inmemoryJobservice;

    @Override
    public void processMessage(List<List<Object>> params) throws Exception {
        EtlPackage etlPackage = etlPostingRepository.executeInNewTransaction(persistence -> buildPackage(params));
        Map<String, Object> map = new HashMap<>();
        map.put(ProcessExcelPackageTask.ETL_PACKAGE_ID_PATH, etlPackage.getId());
        inmemoryJobservice.createSingleActionJob(0, ProcessExcelPackageTask.class, map);
    }

    @Override
    public EtlPackage buildPackage(List<List<Object>> params) throws ParamsParserException {
        final long stamp = System.currentTimeMillis();
        EtlPackage pkg = new EtlPackage();
        pkg.setAccountCnt(0);
        pkg.setDateLoad(new Date());
        pkg.setDescription("Пакет загрузки из файла Excel");
        pkg.setMessage(format("Загрузка из Excel: '%s'", new Date()));
        pkg.setPackageName("pkg" + stamp);
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setPostingCnt(params.size());
        pkg.setId(etlPackageRepository.nextId("GL_SEQ_PKG"));

        List<EtlPosting> postings = new ArrayList<>();

        for (int row = 0; row < params.size(); row++) {
            if (!params.get(row).get(0).toString().isEmpty()) {
                EtlPosting posting = new EtlPosting();
                posting.setId(etlPostingRepository.nextId("GL_SEQ_PST"));
                posting.setAePostingId(getValue(params, String.class, row, 0));
                posting.setSourcePosting(getValue(params, String.class, row, 1));
                posting.setEventId(getValue(params, String.class, row, 2));
                posting.setDealId(getValue(params, String.class, row, 3));
                posting.setPaymentRefernce(getValue(params, String.class, row, 4));
                posting.setDeptId(getValue(params, String.class, row, 5));
                posting.setValueDate(getValue(params, java.util.Date.class, row, 7));
                posting.setNarrative(getValue(params, String.class, row, 8));
                posting.setRusNarrativeLong(getValue(params, String.class, row, 9));
                posting.setRusNarrativeShort(getValue(params, String.class, row, 10));
                posting.setAccountDebit(getValue(params, String.class, row, 11));
                posting.setCurrencyDebit(bankCurrencyRepository.findById(BankCurrency.class, getValue(params, String.class, row, 12)));
                posting.setAmountDebit(new BigDecimal(getValue(params, Double.class, row, 13)).setScale(2, BigDecimal.ROUND_HALF_UP));
                posting.setAccountCredit(getValue(params, String.class, row, 14));
                posting.setCurrencyCredit(bankCurrencyRepository.findById(BankCurrency.class, getValue(params, String.class, row, 15)));
                posting.setAmountCredit(new BigDecimal(getValue(params, Double.class, row, 16)).setScale(2, BigDecimal.ROUND_HALF_UP));
                posting.setFan(YesNo.valueOf(getValue(params, String.class, row, 17)));
                posting.setParentReference(getValue(params, String.class, row, 18));
                posting.setStorno(YesNo.valueOf(getValue(params, String.class, row, 19)));
                posting.setStornoReference(getValue(params, String.class, row, 20));
                posting.setOperationTimestamp(new Date());
                postings.add(posting);
            }
        }

        pkg = etlPackageRepository.save(pkg);
        for (EtlPosting posting : postings) {
            posting.setEtlPackage(pkg);
            postingRepository.save(posting);
        }
        return pkg;
    }

    private <T> T getValue(List<List<Object>> params, Class<T> clazz, int row, int index) throws ParamsParserException {
        try {
            if (null == params.get(row).get(index)) {
                return null;
            } else {
                if (clazz.isAssignableFrom(params.get(row).get(index).getClass())) {
                    return (T) params.get(row).get(index);
                } else {
                    throw new ParamsParserException(format(
                            "Invalid type of element: row '%s' column '%s', value '%s'" +
                            ", expected '%s', but found '%s'", row, index, params.get(row).get(index), clazz, params.get(row).get(index).getClass()));
                }
            }
        } catch (ParamsParserException e) {
            throw e;
        } catch (Exception e){
            final String message = format("Error on getting value of row '%s' on column '%s': '%s'", row, index, e.getMessage());
            log.error(message, e);
            throw new ParamsParserException(message);
        }
    }

}
