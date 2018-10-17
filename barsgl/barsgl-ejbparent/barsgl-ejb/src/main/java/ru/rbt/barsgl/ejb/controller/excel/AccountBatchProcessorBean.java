package ru.rbt.barsgl.ejb.controller.excel;

import org.apache.log4j.Logger;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchRequest;
import ru.rbt.barsgl.ejb.entity.etl.BatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.repository.AccountBatchPackageRepository;
import ru.rbt.barsgl.ejb.repository.AccountBatchRequestRepository;
import ru.rbt.barsgl.ejbcore.util.ExcelParser;
import ru.rbt.barsgl.shared.enums.BatchPackageState;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.shared.ctx.UserRequestHolder;
import ru.rbt.shared.security.RequestContext;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountBatch;

/**
 * Created by er18837 on 17.10.2018.
 */
public class AccountBatchProcessorBean implements BatchMessageProcessor {

    public static final Logger log = Logger.getLogger(AccountBatchProcessorBean.class);

    private static int START_ROW = 1;
    private static int COLUMN_COUNT = 11;
    private static int COLUMN_DATE = COLUMN_COUNT - 1;
    private static String LIST_DELIMITER = "#";

    private List<Object> rowHeader = null;

    @Inject
    private OperdayController operdayController;

    @Inject
    private RequestContext contextBean;

    @EJB
    private AuditController auditController;

    @EJB
    private AccountBatchRequestRepository requestRepository;

    @Inject
    private AccountBatchPackageRepository packageRepository;

    @Override
    public String processMessage(File file, Map<String, String> params) throws Exception {
        String fileName = new String(params.get("filename").getBytes("Cp1252"), "Cp1251");
        String userIdStr = params.get("userid");
        Long userId = userIdStr == null ? null : Long.valueOf(userIdStr);

        AccountBatchPackage batchPackage = null;
        try (
                InputStream is = new FileInputStream(file);
                ExcelParser parser = new ExcelParser(is);
        ) {
            Iterator<List<Object>> it = parser.parseSafe(0);
//            batchPackage = requestRepository.executeInNewTransaction(persistence ->
//                    buildPackage(it, fileName, parser.getRowCount(), userId));
        }
        if (null == batchPackage )
            return "Нет строк для загрузки!";

        String result = new StringBuffer().append(LIST_DELIMITER)
                .append("ID пакета: ").append(batchPackage.getId()).append(LIST_DELIMITER)
                .append("Загружено строк всего: ").append(batchPackage.getCntRequests()).append(LIST_DELIMITER)
                .toString();
        auditController.info(AccountBatch, "Загружен пакет счетов из файла.\n" + result, BatchPackage.class.getName(), batchPackage.getId().toString());
        return result;
    }

    public AccountBatchPackage buildPackage(Iterator<List<Object>> it, String fileName, int maxRowNum, Long userId) throws Exception {
        if(!it.hasNext() || 0 == maxRowNum) {
            return null;
        }

        AccountBatchPackage pkg = new AccountBatchPackage();

        final UserRequestHolder requestHolder = contextBean.getRequest().orElse(UserRequestHolder.empty());
        String userName = requestHolder.getUser();
        String filial = requestHolder.getUserWrapper().getFilial();
        if (null == userId)
            userId = requestHolder.getUserWrapper().getId();
        pkg.setLoadUser(userName);

        Date curdate = operdayController.getOperday().getCurrentDate();
        Date timestamp = operdayController.getSystemDateTime();

        List<AccountBatchRequest> requests = new ArrayList<>();
        List<String> errorList = new ArrayList<String>();

        rowHeader = it.next();
        if (rowHeader.isEmpty())
            return null;
        if (rowHeader.size() < COLUMN_COUNT) {
            String msg = "Неверное количество столбцов: " + rowHeader.size() + ", должно быть не менее " + COLUMN_COUNT;
            auditController.error(AccountBatch, "Ошибка при загрузке файла", null, msg);
            throw new ParamsParserException(msg);
        }

        int row = START_ROW;

        int maxRows = START_ROW + 1000; // TODO packageController.getMaxRowsExcel();
        if (maxRowNum > maxRows) {
            errorList.add(format("Нельзя загрузить файл размером больше %d строк", maxRows));
            throw new ParamsParserException(StringUtils.listToString(errorList, LIST_DELIMITER));
        }

/*
        Date postDate0 = null;
        if(it.hasNext()) {
            AccountBatchRequest request0 = createPosting(it.next(), row, source, department, errorList);
            if (null == request0)
                return null;
            postDate0 = request0.getPostDate();
            if (null != postDate0) {
                checkBackvaluePermission(postDate0, userId);
                checkFilialPermission(request0.getFilialDebit(), request0.getFilialCredit(), userId);
                requests.add(request0);
                row++;
                while(it.hasNext()) {
                    BatchPosting posting = createPosting(it.next(), row, source, department, errorList);
                    if (null != posting) {
                        checkDateEquals(postDate0, posting.getPostDate());
                        checkFilialPermission(posting.getFilialDebit(), posting.getFilialCredit(), userId);
                        requests.add(posting);
                    }
                    row++;
                    if (errorList.size() > 10)
                        break;
                }
            }
        }

        if (errorList.size() > 0) {
            throw new ParamsParserException(StringUtils.listToString(errorList, LIST_DELIMITER));
        };

        int errorCount = 0;

        pkg.setPostingCount(requests.size());
        pkg.setErrorCount(errorCount);
        pkg.setFileName(fileName);
        pkg.setDateLoad(new Date());
        pkg.setMovementOff(movementOff ? YesNo.Y : YesNo.N);
        pkg.setPostDate(postDate0);
        pkg.setProcDate(curdate);
        pkg.setPackageState(errorCount > 0 ? BatchPackageState.ERROR : BatchPackageState.LOADED);
        pkg = packageRepository.save(pkg);

        for (BatchPosting posting : requests) {
            posting.setPackageId(pkg.getId());
            requestRepository.save(posting);
        }
*/

        return pkg;
    }}
