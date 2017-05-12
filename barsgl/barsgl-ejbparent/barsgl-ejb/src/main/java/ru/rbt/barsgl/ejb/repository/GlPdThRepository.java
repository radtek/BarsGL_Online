package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.gl.GLBsaAccLock;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GlPdTh;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.barsgl.shared.enums.OperState;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Operation;
import static ru.rbt.barsgl.ejb.props.PropertyName.PD_CONCURENCY;
import static ru.rbt.ejbcore.util.StringUtils.*;

/**
 * Created by er23851 on 06.03.2017.
 * бин управления репозиторием проводок по техническим счетам
 */
@Stateless
@LocalBean
public class GlPdThRepository extends AbstractBaseEntityRepository<GlPdTh, Long>
{
    private static final Logger log = Logger.getLogger(PdRepository.class.getName());

    @EJB
    private AuditController auditController;

    @EJB
    private PropertiesRepository propertiesRepository;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @EJB
    private OperdayController operdayController;

    @EJB
    private GLOperationRepository operationRepository;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private GLBsaAccLockRepository bsaAccLockRepository;



    public Long getID()
    {
        return this.nextId("PD_SEQ");
    }

    @Override
    public GlPdTh save(GlPdTh pdth)
    {
        pdth = super.save(pdth);
        return pdth;
    }

    public int getNBDP(String ccy) throws SQLException {

        DataRecord res =  this.selectFirst("select NBDP from currency where glccy = ?",ccy);

        return (null != res) && !isEmpty(res.getString("NBDP"))
                ? Integer.parseInt(res.getString("NBDP")) : 0;
    }

    public Long getAccID(String accountNumber) throws SQLException {
        DataRecord res =  this.selectFirst("select id from gl_acc where bsaacid = ?",accountNumber);

        return (null != res) && !isEmpty(res.getString("id"))
                ? Long.parseLong(res.getString("id")) : 0;
    }

    private void processPdthInternal(GLOperation operation, final List<GlPdTh> pdthList, OperState targetState) throws Exception {
        final int count = propertiesRepository.executeInNewTransaction(persistence -> propertiesRepository.getNumber(PD_CONCURENCY.getName()).intValue()); // кол-во попыток обработки
        for (int i = 0; i < count; i++) {
            try {
                final int finalCounter = i;
                beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence, connection) -> {
                    try {
                        // записать полупроводки
                        GlPdTh pdTh1 = null;
                        for (GlPdTh pdth : pdthList) {
                            createOrUpdateAccountLock(pdth.getBsaAcid());
                            if ((pdTh1!=null) && (pdTh1.getOperSide()== GLOperation.OperSide.D) && (pdth.getOperSide()== GLOperation.OperSide.C))
                            {
                                pdth.setPcId(pdTh1.getPcId());
                                pdTh1 = save(pdth);
                            }
                            else{
                                pdth.setPcId(pdth.getId());
                                pdTh1 = save(pdth);
                            }
                            // пересчет/локализация только в рамках задачи мониторинга вход сообщений по журналу backvalue recalculate(pd);
                        }

                        //registerBackvalueJournal(pstList); // todo удалить потом

                        if (null != targetState) {
                            operationRepository.updateOperationStatusSuccess(operation, targetState);
                        }
                        return pdthList;
                    } catch (Exception e) {
                        final String message = format("Ошибка при сохранении п/проводок операция '%s' попытка '%s'"
                                , operation.getId(), finalCounter);
                        log.log(Level.SEVERE, message, e);
                        throw new DefaultApplicationException(message, e);
                    }
                });
                break;
            } catch (Exception e) {
                // в последний раз выбрасываем ошибку
                if (i == count - 1) {
                    final String errorMessage = format("Ошибка при сохранении полупроводок. Операция '%s': ", operation.getId())
                            + pdthList.stream()
                            .map(pd -> format("Полупроводка ID '%s', ACID '%s', BSAACID '%s', POD '%s'"
                                    , pd.getId(), pd.getBsaAcid(), pd.getBsaAcid(), dateUtils.onlyDateString(pd.getPod())))
                            .collect(joining(">,<", "[<", ">]"));
                    auditController.error(Operation, errorMessage, operation, e);
                    throw new DefaultApplicationException(errorMessage, e);
                } else {
                    // ждем после ошибки
                    Thread.sleep(2000);
                }
            }
        }
    }

    public void createOrUpdateAccountLock(String bsaAcid) throws Exception {
        bsaAccLockRepository.createOrUpdateLock(new GLBsaAccLock(bsaAcid, operdayController.getSystemDateTime()));
    }

    public void processGlPdTh(GLOperation operation, final List<GlPdTh> pdthList, OperState targetState) throws Exception {

        this.processPdthInternal(operation,pdthList,targetState);
    }

    public List<Long> getOperationPdThIdList(long operId) {
        String sql = "select ID from GL_PDTH where GLO_REF = ?";
        try {
            List<DataRecord> res = select(sql, operId);
            if (null == res)
                return null;
            List<Long> pdIdList = new ArrayList<>();
            res.forEach(dataRecord -> pdIdList.add(dataRecord.getLong(0)));
            return pdIdList;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }

    }

    public List<GlPdTh> getOperationPdThList(List<Long> pdIdList) {
        String idList = listToString(pdIdList, ",");
        List<GlPdTh> res = select(GlPdTh.class, "from GlPdTh p where p.id in (" + idList + ")");
        return res;
    }

    public String getPrefManual(String dealId, String subDealId, String paymentRef, boolean fromPaymentHub) {
        if (fromPaymentHub) {                   // источник операция - PaymentHub
            return rsubstr(paymentRef, 15);         // другое
        } else {
            return isEmpty(dealId) ? paymentRef : dealId;
        }
    }

    public String getPnarManual(String dealId, String subDealId, String paymentRef) {
        String pnar = isEmpty(dealId) ? paymentRef : (dealId + (isEmpty(subDealId) ? "" : ";" + subDealId));
        return substr(pnar, 30);
    }
}
