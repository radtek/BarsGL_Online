package ru.rbt.barsgl.ejb.controller.operday.task;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.etl.BatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.integr.bg.BatchPackageController;
import ru.rbt.barsgl.ejb.integr.bg.ManualPostingController;
import ru.rbt.barsgl.ejb.integr.oper.MovementCreateProcessor;
import ru.rbt.barsgl.ejb.integr.struct.MovementCreateData;
import ru.rbt.barsgl.ejb.props.PropertyName;
import ru.rbt.barsgl.ejb.repository.BatchPackageRepository;
import ru.rbt.barsgl.ejb.repository.BatchPostingRepository;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.barsgl.ejbcore.repository.PropertiesRepository;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.barsgl.shared.enums.BatchPostAction;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.MovementErrorTypes;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static ru.rbt.barsgl.shared.enums.BatchPackageState.*;
import static ru.rbt.barsgl.shared.enums.BatchPostStatus.*;

/**
 * Created by ER18837 on 23.12.16.
 */
public class MovementReceiveTask implements ParamsAwareRunnable {

    private static final Logger log = Logger.getLogger(MovementReceiveTask.class);
    private static final Long SRV_TIMEOUT = 3600L;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @EJB
    private ManualPostingController postingController;

    @EJB
    private BatchPackageController packageController;

    @Inject
    private BatchPostingRepository postingRepository;

    @Inject
    private BatchPackageRepository packageRepository;

    @Inject
    private MovementCreateProcessor movementProcessor;

    @EJB
    private PropertiesRepository propertiesRepository;

    @EJB
    private OperdayController operdayController;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
            executeWork(operdayController.getOperday(), getTimoutSec());
    }

    public void executeWork(Operday operday, int sendTimeout) throws Exception {
        log.info("MC Message receiver run");
        beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
            // получить все ответы
            List<MovementCreateData> mcdList = movementProcessor.receiveResponses();
            // обработать ответы
            for (MovementCreateData mcd : mcdList) {
                postingController.receiveMovement(mcd);
                log.info(String.format("MC Message UUID '%s' processed", mcd.getMessageUUID()));
            }

            Date curdate = operday.getCurrentDate();
            // найти запросы с таймаутом, изменить статус
            updatePostingsTimeout(curdate, sendTimeout);

            // найти пакеты, получившие все ответы, изменить статус
            updatePackagesReceiveSrv(curdate);

            return null;
        }), 60 * 60);
    }

    public int getTimoutSec() {
        return (int)(long)propertiesRepository.getNumberDef(PropertyName.MC_TIMEOUT.getName(), SRV_TIMEOUT);
    }

    public int updatePostingsTimeout(Date operday, int timeout) throws Exception {
        Date sendTime = DateUtils.addSeconds(operdayController.getSystemDateTime(), -timeout);
        List<BatchPosting> postings = postingRepository.findPostingsByTimeout(sendTime, operday);
        MovementErrorTypes error = MovementErrorTypes.ERR_TIMEOUT;
        for (BatchPosting posting : postings) {
            ManualOperationWrapper wrapper = postingController.createStatusWrapper(posting);
            postingController.setOperationRqStatusReceive(wrapper, posting.getMovementId(), TIMEOUTSRV, error.getCode(), error.getMessage());
        }
        return postings.size();
    }

    public int updatePackagesReceiveSrv(Date operday) throws Exception {
        List<Long> packageIds = packageRepository.getPackagesReceiveSrv(operday);
        for (Long packageId : packageIds) {
            BatchPackage pkg = packageRepository.findById(packageId);
            if (null == postingRepository.getOnePostingByPackageWithoutStatus(pkg.getId(), ERRSRV)) { // все с ошибками
                packageController.updatePackageStateError(pkg, ERROR, ON_WAITSRV);
            } else if (null == postingRepository.getOnePostingByPackageForSign(pkg.getId())) { // нет запросов для обработки
                packageController.updatePackageState(pkg, PROCESSED, ON_WAITSRV);
            } else {
                BatchPosting posting = postingRepository.getOnePostingByPackage(pkg.getId());
                BatchPostStatus postStatus = postingController.getOperationRqStatusSigned(posting.getSignerName(), pkg.getPostDate());
                ManualOperationWrapper wrapper = postingController.createStatusWrapper(posting);
                wrapper.setAction(BatchPostAction.SIGN);
                packageController.setPackageRqStatusSigned(wrapper, posting.getSignerName(),
                        pkg, pkg.getPackageState(), postStatus, postStatus, SIGNEDVIEW, OKSRV);   // TODO userName
            }
        }
        return packageIds.size();
    }

}
