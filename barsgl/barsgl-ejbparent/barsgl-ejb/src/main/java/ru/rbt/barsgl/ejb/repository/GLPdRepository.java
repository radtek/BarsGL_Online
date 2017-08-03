package ru.rbt.barsgl.ejb.repository;

import org.apache.commons.lang3.StringUtils;
import ru.rbt.barsgl.ejb.entity.gl.*;
import ru.rbt.barsgl.ejb.integr.pst.MemorderController;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.shared.Assert;
import ru.rbt.barsgl.shared.enums.OperState;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Operation;
import static ru.rbt.barsgl.ejb.props.PropertyName.PD_CONCURENCY;
import static ru.rbt.ejbcore.util.StringUtils.listToString;

/**
 * Created by Ivan Sevastyanov on 10.02.2016.
 */
public class GLPdRepository extends AbstractBaseEntityRepository<GLPd, Long> {

    private static final Logger log = Logger.getLogger(GLPdRepository.class.getName());

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @EJB
    private PdRepository pdRepository;

    @EJB
    private AuditController auditController;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private MemorderController memorderController;

    @EJB
    private GLOperationRepository operationRepository;

    @Inject
    private GLBalanceExcludeRepository excludeRepository;

    @EJB
    private PropertiesRepository propertiesRepository;

    /**
     * Обработка проводок в режиме BUFFER
     * @param pstList список проводок, формируется стандартно, как в DIRECT mode
     * @param targetState целевой статус операции
     * @throws Exception
     */
    public void processPostingsInternalBuffer(final List<GLPosting> pstList, OperState targetState) throws Exception {
        Assert.isTrue(!pstList.isEmpty(), "Пустой список проводок");
        final int count = propertiesRepository.executeInNewTransaction(persistence -> propertiesRepository.getNumber(PD_CONCURENCY.getName()).intValue()); // кол-во попыток обработки
        List<Pd> pds = createPdList(pstList);
        Map<Long,Long> pcidMapping = createPcidMap(pds);
        List<GLPd> glPds = convert(pcidMapping, pds);
        for (int i = 0; i < count; i++) {
            try {
                final int finalCounter = i;
                beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence,connection) -> {
                    try {
                        updateMemorderPostingData(pstList.get(0).getOperation(), glPds);
                        updatePostingData(pcidMapping, pstList, glPds);
                        // записать полупроводки
                        for (GLPd pd: glPds) {
                            if (!excludeRepository.isExcludes(pd.getBsaAcid(), pd.getPod())) {
                                pdRepository.createOrUpdateAccountLock(pd.getBsaAcid());
                            }
                            save(pd);
                        }
                        pdRepository.registerBackvalueJournal(pstList);
                        if (null != targetState) {
                            operationRepository.updateOperationStatusSuccess(pstList.get(0).getOperation(), targetState);
                        }
                        return glPds;
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
                            + glPds.stream()
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

    private List<Pd> createPdList(final List<GLPosting> pstList) throws Exception {
        return pdRepository.executeInNewTransaction(persistence -> pdRepository.createPdList(pstList));
    }

    public List<GLPd> getGLPostings(GLOperation operation) {
        return select(GLPd.class, "from GLPd p where p.glOperationId = ?1", operation.getId());
    }

    public int updateGLPdInvisible(List<GLPd> glPds) {
        String update = "update gl_pd p set p.invisible = '1' where p.id in (" + StringUtils.leftPad("",glPds.size()*2, "?,").replaceAll(",$","") + ")";
        Long[] ids = glPds.stream().map(AbstractPd::getId).collect(Collectors.toList()).toArray(new Long[glPds.size()]);
        return executeNativeUpdate(update, ids);
    }

    /**
     * конвертируем в список буферных полупроводок
     * @param pdList
     * @return
     */
    private List<GLPd> convert(Map<Long,Long> pcidMap, List<Pd> pdList) throws Exception {
        return pdRepository.executeInNewTransaction(persistence1 ->
         pdList.stream().<GLPd>map(pd -> {
            GLPd glPd = new GLPd();
            glPd.setPcId(pcidMap.get(pd.getPcId()));
            if (Objects.equals(pd.getId(), pd.getPcId())) {
                glPd.setId(glPd.getPcId());
            } else {
                glPd.setId(getNextId());
            }
            glPd.setCcy(pd.getCcy());
            glPd.setPref(pd.getPref());
            glPd.setDealId(pd.getDealId());
            glPd.setAmount(pd.getAmount());
            glPd.setGlOperationId(pd.getGlOperationId());
            glPd.setProcDate(pd.getProcDate());
            glPd.setRusNarrLong(pd.getRusNarrLong());
            glPd.setRusNarrShort(pd.getRusNarrShort());
            glPd.setSubdealId(pd.getSubdealId());
            glPd.setOperReference(pd.getOperReference());
            glPd.setEventType(pd.getEventType());
             glPd.setEventId(pd.getEventId());
             glPd.setPaymentRef(pd.getPaymentRef());
             glPd.setIsCorrection(pd.getIsCorrection());
             glPd.setProfitCenter(pd.getProfitCenter());
             glPd.setNarrative(pd.getNarrative());
            glPd.setVald(pd.getVald());
            glPd.setPod(pd.getPod());
            glPd.setPnar(pd.getPnar());
            glPd.setPbr(pd.getPbr());
            glPd.setAmountBC(pd.getAmountBC());
            glPd.setBsaAcid(pd.getBsaAcid());
            glPd.setAuthorizerDepartment(pd.getAuthorizerDepartment());
            glPd.setOperatorDepartment(pd.getOperatorDepartment());
            glPd.setAcid(pd.getAcid());
            glPd.setAuthorizer(pd.getAuthorizer());
            glPd.setOperator(pd.getOperator());
            glPd.setInvisible(pd.getInvisible());
            glPd.setDepartment(pd.getDepartment());
            glPd.setStornoRef(pd.getStornoRef());
            glPd.setAsoc(pd.getAsoc());
            glPd.setCtype(pd.getCtype());
            return glPd;
        }).collect(Collectors.toList()));
    }

    /**
     * Информация по мемордеру
     * @param operation
     * @param glPds
     */
    private void updateMemorderPostingData(GLOperation operation, List<GLPd> glPds) {
        // выбираем только дебетовые главные п/проводки
        List<GLPd> glPdsDebit = glPds.stream().filter(p -> Objects.equals(p.getId(), p.getPcId())).collect(Collectors.toList());
        for (GLPd glPdDebit : glPdsDebit) {
            glPdDebit.setMemorderNumber(memorderController.nextMemorderNumber(glPdDebit.getPod(), glPdDebit.getBsaAcid(), operation.isCorrection()));
            glPdDebit.setCancelFlag(Memorder.CancelFlag.N);
            List<GLPd> debits = glPds.stream().filter(p -> p.getAmountBC() < 0
                        && Objects.equals(p.getPcId(), glPdDebit.getPcId())).collect(toList());
            Assert.isTrue(debits.size() >= 1, "Не найдена проводка по дебету");

            List<GLPd> credits = glPds.stream().filter(p -> p.getAmountBC() > 0
                        && Objects.equals(p.getPcId(), glPdDebit.getPcId())).collect(Collectors.toList());
            Assert.isTrue(credits.size() >= 1, "Не найдена проводка по кредиту");
            glPdDebit.setDocType(memorderController.getDocType(operation.isCorrection(), debits, credits, glPdDebit.getPod()));
        }
    }

    private void updatePostingData(Map<Long,Long> pcidMapping, List<GLPosting> pstList, List<GLPd> glPds) {
        for (GLPd pd : glPds) {
            if (Objects.equals(pd.getId(), pd.getPcId())) {
                GLPosting posting = pstList.stream().filter(p -> Objects.equals(pcidMapping.get(p.getId()), pd.getId()))
                        .findFirst().orElseThrow(()-> new DefaultApplicationException(format("Не найдена проводка для pcid '%s'", pd.getId())));
                pd.setPostType(posting.getPostType());
                pd.setStornoPcid(posting.getStornoPcid());
            }
        }
    }

    public long getNextId() {
        return nextId("SEQ_GL_PD0");
    }

    /**
     * группируем по PCID и маппим новые PCID на старые
     * @param pdList проводки в Pd
     * @return маппинг
     */
    private Map<Long,Long> createPcidMap(List<Pd> pdList) throws Exception {
        return pdRepository.executeInNewTransaction(persistence1 -> pdList
                .stream().collect(Collectors.groupingBy(Pd :: getPcId))
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> getNextId()))
        );
    }

    public List<Long> getOperationPdIdList(long parentId) {
        String sql = "select pd.ID from GL_OPER o" +
                    " join GL_PD pd on o.GLOID = pd.GLO_REF" +
                    " where VALUE(o.PAR_GLO, o.GLOID) = ?";
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

    public List<GLPd> getOperationPdList(List<Long> pdIdList) {
        String idList = listToString(pdIdList, ",");
        List<GLPd> res = select(GLPd.class, "from GLPd p where p.id in (" + idList + ")");
        return res;
    }

}
