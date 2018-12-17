package ru.rbt.barsgl.ejb.integr.bg;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.flx.FanNdsPosting;
import ru.rbt.barsgl.ejb.entity.flx.NdsPosting;
import ru.rbt.ejbcore.controller.etc.TextResourceController;
import ru.rbt.barsgl.ejb.repository.BankCurrencyRepository;
import ru.rbt.barsgl.ejb.repository.EtlPackageRepository;
import ru.rbt.barsgl.ejb.repository.EtlPostingRepository;
import ru.rbt.barsgl.ejb.repository.flx.FanNdsPostingRepository;
import ru.rbt.barsgl.ejb.repository.flx.NdsPostingRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.util.StringUtils;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

import static ru.rbt.audit.entity.AuditRecord.LogCode.FlexNdsFan;

/**
 * Created by Ivan Sevastyanov on 25.11.2016.
 * формирование веерных проводок по FLEX 6
 */
public class FanNdsPostingController {

    @EJB
    private NdsPostingRepository ndsPostingRepository;

    @Inject
    private FanNdsPostingRepository fanNdsPostingRepository;

    @Inject
    private TextResourceController textResourceController;

    @EJB
    private AuditController auditController;

    @Inject
    private DateUtils dateUtils;

    @Inject
    private BankCurrencyRepository bankCurrencyRepository;

    @Inject
    private EtlPackageRepository packageRepository;

    @EJB
    private OperdayController operdayController;

    @EJB
    private EtlPostingRepository postingRepository;

    public void processTransitPostings(Date workday) throws Exception {
        final Long pkgCount = 500L;
        auditController.info(FlexNdsFan, String.format("Конвертация постингов для формирования вееров за дату '%s'"
                , dateUtils.onlyDateString(workday)));

        cleanOld();

        auditController.info(FlexNdsFan, String.format("Создано '%s' постингов (GL_NDSOPR) для формирования вееров", convertPd(workday)));
        auditController.info(FlexNdsFan, String.format("Создано '%s' постингов (GL_NDSPST) для формирования вееров", createDrafts(workday)));
        auditController.info(FlexNdsFan, "Создание AE постингов (GL_ETLPST)");
        createAePostings(pkgCount);
        auditController.info(FlexNdsFan, "Создание AE постингов (GL_ETLPST) выполнено успешно");
    }

    private void cleanOld() throws Exception {
        try {
            postingRepository.executeInNewTransaction(persistence -> {
                auditController.info(FlexNdsFan, String.format("Удалено '%s' старых операций флекс", postingRepository.executeNativeUpdate("delete from GL_NDSOPR")));
                auditController.info(FlexNdsFan, String.format("Удалено '%s' старых проводок вееров флекс", postingRepository.executeNativeUpdate("delete from GL_NDSPST")));
                return null;
            });
        } catch (Exception e) {
            auditController.error(FlexNdsFan, "Ошибка удаления старных данных", null, e);
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private int convertPd(Date workday) {
        try {
            return ndsPostingRepository.executeInNewTransaction(persistence -> {
                return ndsPostingRepository.executeTransactionally(connection -> {
                    final String query = textResourceController
                            .getContent("ru/rbt/barsgl/ejb/integr/bg/select_nds_postings.sql");
                    int cnt = 0;
                    try (PreparedStatement statement = connection.prepareStatement(query)){
                        statement.setDate(1, new java.sql.Date(workday.getTime()));
                        try (ResultSet rs = statement.executeQuery()){
                            while (rs.next()) {
                                NdsPosting ndsPosting = new NdsPosting();
                                ndsPosting.setId(rs.getLong("IDPD"));
                                ndsPosting.setTransitAccount(rs.getString("BSAACID"));
                                ndsPosting.setAmount(rs.getLong("AMNT"));
                                ndsPosting.setDocNumber(rs.getString("DOCN"));
                                ndsPosting.setNarrative(rs.getString("PNAR"));
                                ndsPosting.setNarrativeRU(rs.getString("RNARLNG"));
                                ndsPosting = ndsPostingRepository.save(ndsPosting);
                                cnt++;
                            }
                        }
                    }
                    return cnt;
                });
            });
        } catch (Exception e) {
            auditController.error(FlexNdsFan, "Ошибка при конвертации постингов (PD)", null, e);
            throw new DefaultApplicationException(e);
        }
    }

    private int createDrafts(Date workday) {
        try {
            return ndsPostingRepository.executeInNewTransaction(persistence -> {
                BigDecimal rateNds = new BigDecimal(20);
                final String query = textResourceController
                        .getContent("ru/rbt/barsgl/ejb/integr/bg/select_nds_opers.sql");
                return ndsPostingRepository.executeTransactionally(connection -> {
                    int cnt = 0;
                    try (PreparedStatement statement = connection.prepareStatement(query)
                         ; ResultSet rs = statement.executeQuery()){
                        while (rs.next()) {
                            // комиссия
                            FanNdsPosting fanNdsComission = new FanNdsPosting();
                            fanNdsComission.setId(fanNdsPostingRepository.nextId());

                            fanNdsComission.setEvtId(rs.getLong("IDPD"));
                            String ref = fanNdsComission.getId() + "";

                            fanNdsComission.setPaymentReference(ref);
                            fanNdsComission.setValueDate(workday);
                            fanNdsComission.setFanReferences(ref);
                            fanNdsComission.setNarrativeEn(rs.getString("NRT"));
                            fanNdsComission.setNarrativeRuLong(rs.getString("RNRTL"));
                            fanNdsComission.setNarrativeRuShort(StringUtils.substr(rs.getString("RNRTL"), 100));

                            fanNdsComission.setAccountDebit(rs.getString("TR_ACC"));
                            fanNdsComission.setCurrencyDebit(bankCurrencyRepository.refreshCurrency(BankCurrency.RUB));
                            BigDecimal nds = new BigDecimal(rs.getLong("AMOUNT"))
                                    .movePointLeft(2).multiply(rateNds)
                                    .divide(rateNds.add(new BigDecimal(100)), BigDecimal.ROUND_HALF_UP)
                                    .setScale(3, BigDecimal.ROUND_HALF_UP);

                            fanNdsComission.setAmountDebit(new BigDecimal(rs.getLong("AMOUNT")).movePointLeft(2).subtract(nds));

                            fanNdsComission.setAccountCredit(rs.getString("COM_ACC"));
                            fanNdsComission.setCurrencyCredit(bankCurrencyRepository.refreshCurrency(BankCurrency.RUB));
                            fanNdsComission.setAmountCredit(fanNdsComission.getAmountDebit());
                            fanNdsComission.setEventType(rs.getString("EVTP"));
                            fanNdsComission.setProcessed(YesNo.N);
                            fanNdsComission = fanNdsPostingRepository.save(fanNdsComission);

                            // НДС
                            FanNdsPosting fanNds = new FanNdsPosting();
                            fanNds.setId(fanNdsPostingRepository.nextId());
                            fanNds.setEvtId(rs.getLong("IDPD"));
                            fanNds.setPaymentReference(rs.getLong("IDPD") + "");
                            fanNds.setValueDate(workday);
                            fanNds.setFanReferences(ref);
                            fanNds.setNarrativeEn(rs.getString("NRT"));
                            fanNds.setNarrativeRuLong(rs.getString("RNRTL"));
                            fanNds.setNarrativeRuShort(StringUtils.substr(rs.getString("RNRTL"), 100));

                            fanNds.setAccountDebit(rs.getString("TR_ACC"));
                            fanNds.setCurrencyDebit(bankCurrencyRepository.refreshCurrency(BankCurrency.RUB));
                            fanNds.setAmountDebit(nds);

                            fanNds.setAccountCredit(rs.getString("NDS_ACC"));
                            fanNds.setCurrencyCredit(bankCurrencyRepository.refreshCurrency(BankCurrency.RUB));
                            fanNds.setAmountCredit(nds);
                            fanNds.setEventType(rs.getString("EVTP"));
                            fanNds.setProcessed(YesNo.N);
                            fanNds = fanNdsPostingRepository.save(fanNds);
                            cnt++;
                        }
                        return cnt;
                    }
                });
            });
        } catch (Exception e) {
            auditController.error(FlexNdsFan, "Ошибка при формировании постингов (GL_NDSPST)", null, e);
            throw new DefaultApplicationException(e);
        }
    }

    private void createAePostings(final Long pkgCount) {
        try {
            ndsPostingRepository.executeInNewTransaction(pers-> {
                ndsPostingRepository.executeTransactionally(connection -> {
                    DataRecord stat = fanNdsPostingRepository
                            .selectFirst("select min(id_pst) mn, max(id_pst) mx, count(1) cnt from GL_NDSPST where processed = 'N'");
                    if (0L == stat.getLong("cnt")) {
                        auditController.warning(FlexNdsFan, "Нет данных для формирования пакетов НДС", null, "");
                        return null;
                    }
                    Long start = stat.getLong("mn");
                    Long end = start + pkgCount - 1;
                    for (;;) {
                        EtlPackage etlPackage = new EtlPackage();
                        etlPackage.setId(packageRepository.nextId());
                        etlPackage.setPackageName("" + etlPackage.getId());
                        etlPackage.setDateLoad(operdayController.getSystemDateTime());
                        etlPackage.setPackageState(EtlPackage.PackageState.BUILD);
                        etlPackage.setDescription("BARS_NDS");
                        etlPackage.setAccountCnt(0);
                        etlPackage.setPostingCnt(0);
                        etlPackage.setProcessDate(etlPackage.getProcessDate());

                        etlPackage = packageRepository.save(etlPackage);

                        try (PreparedStatement statement = connection
                                .prepareStatement(textResourceController.getContent("ru/rbt/barsgl/ejb/integr/bg/insert_nds_drafts.sql"))){
                            statement.setLong(1, etlPackage.getId());
                            statement.setLong(2, start);
                            statement.setLong(3, end);
                            int insCnt = statement.executeUpdate();
                            etlPackage.setPostingCnt(insCnt);
                            etlPackage.setPackageState(EtlPackage.PackageState.LOADED);
                            packageRepository.update(etlPackage);
                            try (PreparedStatement updProc = connection.prepareStatement(textResourceController
                                    .getContent("ru/rbt/barsgl/ejb/integr/bg/update_drafts_processed.sql"))){
                                updProc.setLong(1, start);
                                updProc.setLong(2, end);
                                updProc.executeUpdate();
                            }
                        }
                        start = end + 1; end = start + pkgCount - 1;
                        if (start > stat.getLong("mx")) break;
                    }
                    return null;
                });
                return null;
            });
        } catch (Exception e) {
            auditController.error(FlexNdsFan, "Ошибка при формировании AE постингов (GL_ETLPST)", null, e);
            throw new DefaultApplicationException(e);
        }
    }
}
