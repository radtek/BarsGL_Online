package ru.rbt.barsgl.ejb.integr.bg;

import org.apache.commons.beanutils.BeanUtils;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.dict.AccountingType;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperSide;
import ru.rbt.audit.entity.AuditRecord.LogCode;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountService;
import ru.rbt.barsgl.ejb.repository.AccRlnRepository;
import ru.rbt.barsgl.ejb.repository.EtlPostingRepository;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.barsgl.ejb.repository.props.ConfigProperty;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.JpaAccessCallback;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.barsgl.shared.enums.OperState;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Created by Ivan Sevastyanov on 16.12.2016.
 */
@Stateless
@LocalBean
public class EtlTechnicalPostingController implements EtlMessageController<EtlPosting, GLOperation>{

    @EJB
    private EtlPostingRepository etlPostingRepository;

    @Inject
    private AccRlnRepository rlnRepository;

    @EJB
    private PropertiesRepository propertiesRepository;

    @EJB
    private GLAccountService accountService;

    @EJB
    private OperdayController operdayController;

    @EJB
    private EtlPostingController etlPostingController;

    @EJB
    private GLOperationRepository operationRepository;

    @EJB
    private GLAccountRepository accountRepository;

    @EJB
    private AuditController auditController;

    @EJB
    private AsyncProcessor asyncProcessor;

    @Override
    public GLOperation processMessage(EtlPosting posting) {
        try {
            EtlPosting etlLocal = etlPostingRepository.findById(EtlPosting.class, posting.getId());
            final OperSide clientSide = getStamtActuality(etlLocal);
            if (clientSide != OperSide.N) {
                GLOperation techOperation = etlPostingController.processMessage(createTechnicalPosting(etlLocal, clientSide));
                techOperation = operationRepository.refresh(techOperation, true);
                if (techOperation.getState() == OperState.POST) {
                    updateClientAccount(etlLocal, clientSide
                            , clientSide == OperSide.C ? accountRepository.findGLAccount(techOperation.getAccountDebit())
                                : accountRepository.findGLAccount(techOperation.getAccountCredit()));
                }
                return techOperation;
            } else {
                auditController.warning(LogCode.TechnicalPosting
                        , format("Проводка '%s' без клиентского счета  недоступна для автоматизированной переобработки", posting.getAePostingId()), null, "");
            }
        } catch (Throwable e) {
            auditController.error(LogCode.TechnicalPosting
                    , format("Ошибка при переобработке проводки id='%s', id_pst='%s'", posting.getId(), posting.getAePostingId()), posting, e);
        }
        return null;
    }

    public int reprocessPostingByPackage(EtlPackage etlPackage) throws Exception {
        List<JpaAccessCallback<GLOperation>> callbacks = etlPostingRepository
                .getFailedPostingForRepsrocessing(etlPackage).stream()
                .map(e -> (JpaAccessCallback<GLOperation>) persistence ->
                        {
                            return operationRepository.executeInNewTransaction(p2 -> {
                                processMessage(e);
                                return null;
                            });
                        }
                ).collect(Collectors.toList());
        asyncProcessor.asyncProcessPooled(callbacks, propertiesRepository.getNumber(ConfigProperty.PdConcurency.getValue()).intValue(), 1L, TimeUnit.HOURS);
        return callbacks.size();
    }

    private OperSide getStamtActuality(EtlPosting posting) throws SQLException {
        final String sql = "select GL_STMFILTER('%s') fl from dual";
        String isDt = etlPostingRepository.selectFirst(format(sql, posting.getAccountDebit())).getString("fl");
        String isCt = etlPostingRepository.selectFirst(format(sql, posting.getAccountCredit())).getString("fl");
        return isCt.equals("1") ? OperSide.C : (isDt.equals("1") ? OperSide.D : OperSide.N);
    }

    private DataRecord getBsaacidData(String bsaacid) {
        try {
            return Optional.ofNullable(rlnRepository.selectFirst(
                    "select c.glccy, r.ccode from accrln r, currency c\n" +
                    " where bsaacid = ?\n" +
                    "    and r.cbccy = c.cbccy", bsaacid))
                    .orElseThrow(() -> new DefaultApplicationException(format("Не удалось определить код валюты по счету '%s'", bsaacid)));
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private EtlPosting createTechnicalPosting(EtlPosting posting, OperSide clientSide) throws Exception {
        EtlPosting technicalPosting = new EtlPosting();
        BeanUtils.copyProperties(technicalPosting, posting);
        technicalPosting.setId(etlPostingRepository.nextId());
        technicalPosting.setFan(posting.getFan());
        technicalPosting.setStorno(posting.getStorno());
        technicalPosting.setAePostingId(posting.getAePostingId() + "*");
        technicalPosting.setErrorCode(null);
        technicalPosting.setErrorMessage(null);

        DataRecord record = getBsaacidData(clientSide == OperSide.C ? posting.getAccountCredit() : posting.getAccountDebit());
        final String glccy = record.getString("glccy");
        final String ccode = record.getString("ccode");
        final String acctype = clientSide == OperSide.C
                ? propertiesRepository.getString(ConfigProperty.TransitAccTypeCredit.getValue())
                : propertiesRepository.getString(ConfigProperty.TransitAccTypeDebit.getValue());

        GLAccount techAccount = accountService.getTechnicalAccount(new AccountingType(acctype, "")
                , glccy, ccode, operdayController.getOperday().getCurrentDate());
        if (clientSide == OperSide.C) {
            technicalPosting.setAccountDebit(techAccount.getBsaAcid());
            technicalPosting.setCurrencyDebit(posting.getCurrencyCredit());
            technicalPosting.setAmountDebit(posting.getAmountCredit());
            technicalPosting.setAmountDebitRu(posting.getAmountCreditRu());
            technicalPosting.setAccountKeyDebit(null);
        } else {
            technicalPosting.setAccountCredit(techAccount.getBsaAcid());
            technicalPosting.setCurrencyCredit(posting.getCurrencyDebit());
            technicalPosting.setAmountCredit(posting.getAmountDebit());
            technicalPosting.setAmountCreditRu(posting.getAmountDebitRu());
            technicalPosting.setAccountKeyCredit(null);
        }
        return etlPostingRepository.save(technicalPosting);
    }

    private EtlPosting updateClientAccount(EtlPosting posting, OperSide clientSide, GLAccount transitAccount) {
        if (clientSide == OperSide.C)
            posting.setAccountCredit(transitAccount.getBsaAcid());
        else
            posting.setAccountDebit(transitAccount.getBsaAcid());
        return etlPostingRepository.update(posting);
    }

}
