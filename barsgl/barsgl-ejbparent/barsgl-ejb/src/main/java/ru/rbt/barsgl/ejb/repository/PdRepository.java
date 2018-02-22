package ru.rbt.barsgl.ejb.repository;

import org.apache.commons.lang3.StringUtils;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.dict.SourcesDeals;
import ru.rbt.barsgl.ejb.entity.gl.GLBsaAccLock;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejb.repository.dict.SourcesDealsRepository;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.ejbcore.util.DateUtils;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Operation;
import static ru.rbt.barsgl.ejb.props.PropertyName.PD_CONCURENCY;
import static ru.rbt.ejbcore.util.StringUtils.*;

/**
 * Created by Ivan Sevastyanov
 */
@Stateless
@LocalBean
public class PdRepository extends AbstractBaseEntityRepository<Pd, Long> {

    private static final Logger log = Logger.getLogger(PdRepository.class.getName());

    @Inject
    private BankCurrencyRepository bankCurrencyRepository;

    @EJB
    private OperdayController operdayController;

    @Inject
    private GLPostingRepository glPostingRepository;

    @EJB
    private BackvalueJournalRepository journalRepository;

    @EJB
    private GLBsaAccLockRepository bsaAccLockRepository;

    @EJB
    private AuditController auditController;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @EJB
    private MemorderRepository memorderRepository;

    @EJB
    private GLOperationRepository operationRepository;

    @Inject
    private GLPdRepository glPdRepository;

    @EJB
    private PropertiesRepository propertiesRepository;

    @EJB
    private SourcesDealsRepository sourcesDealsRepository;

    public long getNextId() {
        return nextId("PD_SEQ");
    }

    /**
     * Создает полупроводку
     *
     * @param operation  - GL операция
     * @param id         - ID полупроводки. Если 0, берется из последовательности
     * @param pcId       - PCID проводки
     * @param pbr        - строка для записи в поле PBR полупроводок
     * @param bsaAcid    - счет ЦБ
     * @param ccy        - валюта
     * @param amount     - сумма в валюте
     * @param equivalent - рублевый эквивалент
     * @return - полупроводку
     * @throws SQLException
     */
    public Pd createPd(GLOperation operation, long id, long pcId, String pbr, String evType,
                       String bsaAcid, BankCurrency ccy, Long amount, Long equivalent) throws SQLException {
        return createPd(operation, id, pcId, pbr, evType, operation.getRusNarrativeLong(),
            bsaAcid, ccy, amount, equivalent);
    }

    ;

    public Pd createPd(GLOperation operation, long id, long pcId, String pbr, String evType, String rusNarrLong,
                       String bsaAcid, BankCurrency ccy, Long amount, Long equivalent)
        throws SQLException {

        Pd pd = new Pd();
        if (id == 0) {
            id = getNextId();
        }

        // PD
        pd.setId(id);
        pd.setPcId(pcId);
        pd.setPod(operation.getPostDate());
        pd.setVald(operation.getValueDate());
        pd.setBsaAcid(bsaAcid);
        pd.setCcy(ccy);
        pd.setPbr(pbr);
        pd.setAmount(amount);
        pd.setAmountBC(equivalent);

        DataRecord data = operationRepository.getAcidByAccRln(bsaAcid, pd.getPod());
        if (null != data) {
            pd.setAcid(data.getString("ACID"));
            pd.setCtype(getCtype(bsaAcid, data.getString("PLCODE"), data.getInteger("CTYPE")));
        }

        String pref = ifEmpty(getPref(operation), " ");
        pd.setPnar(getPnar(operation, pref));

        // PDEXT
        pd.setPref(pref);
        pd.setDepartment(substr(operation.getDeptId(), 3));

        // PDEXT2
        pd.setRusNarrLong(rusNarrLong); // operation.getRusNarrativeLong());
        pd.setRusNarrShort(operation.getRusNarrativeShort());
        pd.setOperReference("09");

        // PDEXT3
        if (operation.getInputMethod().equals("AE")) {
            pd.setOperator("System");
            pd.setOperatorDepartment("ITD");
            pd.setAuthorizer(pd.getOperator());
            pd.setAuthorizerDepartment(pd.getOperatorDepartment());
        } else {
            pd.setOperator("");
            pd.setOperatorDepartment("");
            pd.setAuthorizer("");
            pd.setAuthorizerDepartment("");
        }

        // PDEXT5
        pd.setGlOperationId(operation.getId());
        pd.setProcDate(operation.getProcDate());
        pd.setEventType(evType);
        pd.setDealId(operation.getDealId());
        pd.setSubdealId(operation.getSubdealId());
        // new
        pd.setEventId(operation.getEventId());
        pd.setPaymentRef(operation.getPaymentRefernce());
        pd.setIsCorrection(operation.getIsCorrection());
        pd.setProfitCenter(operation.getProfitCenter());
        pd.setNarrative(operation.getNarrative());
        return pd;
    }

    /**
     * Определяет значение поля PDEXT.PREF по типу операции
     *
     * @param operation
     * @return
     */
    public String getPref(GLOperation operation) {
        if (operation.fromPaymentHub()) {                   // источник операция - PaymentHub
            if (operation.hasParent()) {
                return rsubstr(operation.getParentReference(), 15);         // платеж с комиссией
            } else {
                return rsubstr(operation.getPaymentRefernce(), 15);         // другое
            }
        } else if (operation.fromKondorPlus()) {
            String dealId = operation.getDealId();
            if (isEmpty(dealId) || "0".equals(dealId)) {
                return operation.getPaymentRefernce();
            } else {
                return StringUtils.leftPad(dealId, 6, "0");
            }
        } else {
            SourcesDeals sourcesDeals = sourcesDealsRepository.findCached(operation.getSourcePosting());
            if ((null != sourcesDeals) && ifEmpty(sourcesDeals.getFlDealId(), "").equals("Y")) {
                return !isEmpty(operation.getDealId()) ? operation.getDealId() : operation.getPaymentRefernce();
            } else {
                return operation.getPref();                     // другие источники
            }
        }
    }

    private String getCtype(String bsaAcid, String plCode, Integer ctype) {
        if ((bsaAcid.startsWith("706") || bsaAcid.startsWith("707"))) {
            if (!isEmpty(plCode) && (Integer.parseInt(plCode) > 0))
                return ctype.toString();
        }
        return "";
    }

    /**
     * Определяет значение поля PD.PNAR по типу операции
     *
     * @param operation
     * @return
     */
    public String getPnar(GLOperation operation, String pref) {
        if (operation.fromPaymentHub() && operation.isChild()) {    // операция из PaymentHub, комиссия по платежу
            String pnar = "CHARGE " + pref;
            if (operation.isStorno()) {
                return "*" + pnar;
            } else {
                return pnar;
            }
        } else if (InputMethod.AE != operation.getInputMethod()) {   // для ручных операций и из файла
            return getPnarManual(operation.getDealId(), operation.getSubdealId(), operation.getPaymentRefernce());
        } else {
            return substr(operation.getNarrative(), 30);            // для AE - из NRT
        }
    }

    /**
     * для ручных операций и из файла: PREF = DealId или PaymentRef
     *
     * @param dealId
     * @param subDealId
     * @param paymentRef
     * @return
     */
    public String getPrefManual(String dealId, String subDealId, String paymentRef, boolean fromPaymentHub) {
        if (fromPaymentHub) {                   // источник операция - PaymentHub
            return rsubstr(paymentRef, 15);         // другое
        } else {
            return isEmpty(dealId) ? paymentRef : dealId;
        }
    }

    /**
     * для ручных операций и из файла: PNAR = DealId;SubDealId или PaymentRef
     *
     * @param dealId
     * @param subDealId
     * @param paymentRef
     * @return
     */
    public String getPnarManual(String dealId, String subDealId, String paymentRef) {
        String pnar = isEmpty(dealId) ? paymentRef : (dealId + (isEmpty(subDealId) ? "" : ";" + subDealId));
        return substr(pnar, 30);
    }

    /**
     * Подавляет проводки в Pd
     *
     * @param invisible true - подавить
     * @param postings  список проводок, которые надо подавить
     */
    public int updatePdInvisible(boolean invisible, List<GLPosting> postings) {

        if (postings.isEmpty()) {
            return 0;
        }
        String strInvisible = invisible ? "1" : "0";
        StringBuilder pcidIn = new StringBuilder();
        pcidIn.append("update Pd set invisible = ? where pcId in (");
        for (GLPosting posting : postings) {
            pcidIn.append(posting.getId()).append(",");
        }
        pcidIn.setCharAt(pcidIn.length() - 1, ')');
        String sql = pcidIn.toString();
        return executeNativeUpdate(sql, strInvisible);
    }

    /**
     * @param pstList GL-проводки, содержат полупроводки Майдас
     * @deprecated use ru.rbt.barsgl.ejb.repository.PdRepository#processPosting(java.util.List, ru.rbt.barsgl.shared.enums.OperState) instead <br/>
     * Обработка всех проводок сразу + создание мемордеров в той же транзакции
     */
    public void processPosting(List<GLPosting> pstList) throws Exception {
        processPostingsInternal(pstList, null);
    }

    /**
     * Обработка всех проводок сразу + создание мемордеров в той же транзакции + установка "успешного" статуса
     *
     * @param pstList     постинги
     * @param targetState статус в случае успешной обработки
     * @throws Exception что-то пошло не так
     */
    public void processPosting(List<GLPosting> pstList, OperState targetState) throws Exception {
        if (operdayController.getOperday().getPdMode() == Operday.PdMode.DIRECT) {
            processPostingsInternal(pstList, targetState);
        } else {
            glPdRepository.processPostingsInternalBuffer(pstList, targetState);
        }
    }

    public List<Pd> createPdList(List<GLPosting> pstList) {
        List<Pd> pdList = new ArrayList<>();
        // создать полный список полупроводок
        for (GLPosting glPosting : pstList) {
            pdList.addAll(glPosting.getPdList());
        }
        // сортировать по возрастанию bsaAcid
        Collections.sort(pdList);
        return pdList;
    }

    private void processPostingsInternal(final List<GLPosting> pstList, OperState targetState) throws Exception {
        final int count = propertiesRepository.executeInNewTransaction(persistence -> propertiesRepository.getNumber(PD_CONCURENCY.getName()).intValue()); // кол-во попыток обработки
        final List<Pd> pdList = createPdList(pstList);
        for (int i = 0; i < count; i++) {
            try {
                final int finalCounter = i;
                beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence, connection) -> {
                    try {
                        // записать полупроводки
                        for (Pd pd : pdList) {
                            createOrUpdateAccountLock(pd.getBsaAcid());
                            save(pd);
                            // пересчет/локализация только в рамках задачи мониторинга вход сообщений по журналу backvalue recalculate(pd);
                        }
                        // записать проводки
                        for (GLPosting pst : pstList) {
                            glPostingRepository.save(pst);
                        }
                        registerBackvalueJournal(pstList); // todo удалить потом
                        memorderRepository.createMemorders(pstList);

                        if (null != targetState) {
                            operationRepository.updateOperationStatusSuccess(pstList.get(0).getOperation(), targetState);
                        }
                        return pdList;
                    } catch (Exception e) {
                        final String message = format("Ошибка при сохранении п/проводок операция '%s' попытка '%s'"
                            , pstList.get(0).getOperation().getId(), finalCounter);
                        log.log(Level.SEVERE, message, e);
                        throw new DefaultApplicationException(message, e);
                    }
                });
                break;
            } catch (Exception e) {
                // в последний раз выбрасываем ошибку
                if (i == count - 1) {
                    final String errorMessage = format("Ошибка при сохранении полупроводок. Операция '%s': ", pstList.get(0).getOperation().getId())
                                                    + pdList.stream()
                                                          .map(pd -> format("Полупроводка ID '%s', ACID '%s', BSAACID '%s', POD '%s'"
                                                              , pd.getId(), pd.getAcid(), pd.getBsaAcid(), dateUtils.onlyDateString(pd.getPod())))
                                                          .collect(joining(">,<", "[<", ">]"));
                    auditController.error(Operation, errorMessage, pstList.get(0).getOperation(), e);
                    throw new DefaultApplicationException(errorMessage, e);
                } else {
                    // ждем после ошибки
                    Thread.sleep(2000);
                }
            }
        }
    }

    /**
     * регистрация в журнале backvalue
     *
     * @param pstList
     */
    public void registerBackvalueJournal(List<GLPosting> pstList) {
        try {
            journalRepository.executeInNewTransaction(persistence1 -> {
                journalRepository.registerBackvalueJournal(pstList);
                return null;
            });
        } catch (Exception e) {
            // при многопоточной обработке может стрельнуть ошибка в случае одновременной вставки с одним ключом
            try {
                journalRepository.executeInNewTransaction(persistence1 -> {
                    journalRepository.registerBackvalueJournal(pstList);
                    return null;
                });
            } catch (Exception e1) {
                log.log(Level.SEVERE, "Ошибка регистрации проводки BACKVALUE: " + e.getMessage(), e);
                throw new DefaultApplicationException(e1.getMessage(), e);
            }
        }
    }

    /**
     * Блокировка по номеру счета
     *
     * @param bsaAcid блокируемый счет
     */
    public void createOrUpdateAccountLock(String bsaAcid) throws Exception {
        bsaAccLockRepository.createOrUpdateLock(new GLBsaAccLock(bsaAcid, operdayController.getSystemDateTime()));
    }

    public List<Long> getOperationPdIdList(long parentId) {
        String sql = "select PD.ID from GL_OPER o" +
                         " join GL_POSTING p on o.GLOID = p.GLO_REF" +
                         " join PD on p.PCID = PD.PCID" +
                         " where NVL(PAR_GLO, GLOID) = ?";
        try {
            List<DataRecord> res = select(sql, parentId);
            if (null == res)
                return null;
            List<Long> pdIdList = new ArrayList<>();
            res.forEach(dataRecord -> pdIdList.add(dataRecord.getLong(0)));
            return pdIdList;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }

    }

    public List<Pd> getOperationPdList(List<Long> pdIdList) {
        String idList = listToString(pdIdList, ",");
        List<Pd> res = select(Pd.class, "from Pd p where p.id in (" + idList + ")");
        return res;

    }

}
