package ru.rbt.barsgl.ejb.controller.excel;

import org.apache.log4j.Logger;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchRequest;
import ru.rbt.barsgl.ejb.entity.etl.BatchPackage;
import ru.rbt.barsgl.ejb.props.PropertyName;
import ru.rbt.barsgl.ejb.repository.AccountBatchPackageRepository;
import ru.rbt.barsgl.ejb.repository.AccountBatchRequestRepository;
import ru.rbt.barsgl.ejbcore.util.ExcelParser;
import ru.rbt.barsgl.shared.enums.AccountBatchPackageState;
import ru.rbt.barsgl.shared.enums.AccountBatchState;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
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
public class AccountBatchProcessorBean extends UploadProcessorBase implements BatchMessageProcessor {
    public static final Logger log = Logger.getLogger(AccountBatchProcessorBean.class);
    private final long MAX_ROWS = 1000;

    private static int START_ROW = 1;
    private static int COLUMN_COUNT = 11;
    private static int COLUMN_DATE = COLUMN_COUNT - 1;
    private static String LIST_DELIMITER = "#";

    private static final int I_Branch = 0;
    private static final int I_Ccy = 1;
    private static final int I_Custno = 2;
    private static final int I_Acctype = 3;
    private static final int I_Acc2 = 4;
    private static final int I_Ctype = 5;
    private static final int I_Term = 6;
    private static final int I_Dealsrc = 7;
    private static final int I_Dealid = 8;
    private static final int I_Subdealid = 9;
    private static final int I_Opendate = 10;

    private List<Object> rowHeader = null;

    @Inject
    private OperdayController operdayController;

    @Inject
    private RequestContext contextBean;

    @EJB
    private AuditController auditController;

    @Inject
    private AccountBatchRequestRepository requestRepository;

    @EJB
    private AccountBatchPackageRepository packageRepository;

    @EJB
    private PropertiesRepository propertiesRepository;

    @Override
    protected long getColumnCount() {
        return COLUMN_COUNT;
    }

    @Override
    protected long getStartLine() {
        return START_ROW;
    }

    @Override
    protected long getMaxLines() {
        return propertiesRepository.getNumberDef(PropertyName.BATPKG_MAXROWS.getName(), MAX_ROWS);  // TODO BATPKG_MAXROWS ??
    }

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
            batchPackage = packageRepository.executeInNewTransaction(persistence ->
                    buildPackage(it, fileName, parser.getRowCount(), userId));
        }
        if (null == batchPackage)
            return "Нет строк для загрузки!";

        String result = new StringBuffer().append(LIST_DELIMITER)
                .append("ID пакета: ").append(batchPackage.getId()).append(LIST_DELIMITER)
                .append("Загружено строк всего: ").append(batchPackage.getCntRequests()).append(LIST_DELIMITER)
                .toString();
        // TODO подробней
        auditController.info(AccountBatch, "Загружен пакет счетов из файла.\n" + result, BatchPackage.class.getName(), batchPackage.getId().toString());
        return result;
    }

    public AccountBatchPackage buildPackage(Iterator<List<Object>> it, String fileName, long maxRowNum, Long userId) throws Exception {
        if (!it.hasNext() || 0 == maxRowNum) {
            return null;
        }

        if (!checkRowHeader(it.next(), maxRowNum))
            return null;

        final UserRequestHolder requestHolder = contextBean.getRequest().orElse(UserRequestHolder.empty());
        String userName = requestHolder.getUser();
        if (null == userId)
            userId = requestHolder.getUserWrapper().getId();

        List<String> errorList = new ArrayList<>();
        List<String> allowedBranches = packageRepository.getAllowedBranches(userId);
        if (allowedBranches == null || allowedBranches.isEmpty()) {
            throw new ParamsParserException("У вас не заданы права на создание счетов в филиалах");
        }

        boolean allBranches = allowedBranches.get(0).equals("*");
        List<AccountBatchRequest> requests = new ArrayList<>();
        Date curdate = operdayController.getOperday().getCurrentDate();
        int row = START_ROW;
        if (it.hasNext()) {
            while (it.hasNext()) {
                AccountBatchRequest request = createRequest(it.next(), row, curdate, errorList);
                if (null != request) {
                    if (!allBranches && !allowedBranches.contains(request.getInBranch())) {
                        errorList.add(format("%s У вас нет прав на открытие счетов в бранче '%s'", getLocation(row, I_Branch), request.getInBranch()));
                    }
                    requests.add(request);
                }
                row++;
                if (errorList.size() > 10)
                    break;
            }
        }

        if (errorList.size() > 0) {
            throw new ParamsParserException(StringUtils.listToString(errorList, LIST_DELIMITER));
        }

        AccountBatchPackage pkg = new AccountBatchPackage();
        pkg.setLoadUser(userName);
        pkg.setFileName(fileName);
        pkg.setOperday(curdate);
        pkg.setCntRequests((long) requests.size());
        pkg.setState(AccountBatchPackageState.IS_LOAD);
        pkg = packageRepository.save(pkg);

        for (AccountBatchRequest request : requests) {
            request.setBatchPackage(pkg);
            requestRepository.save(request);
        }

        return pkg;
    }

    public AccountBatchRequest createRequest(List<Object> rowParams, int row, Date curdate, List<String> errorList) throws ParamsParserException {
        if (null == rowParams || rowParams.isEmpty())
            return null;

        AccountBatchRequest request = new AccountBatchRequest();
        // required
        request.setInBranch(getString(rowParams, row, I_Branch, true, 3, true, errorList));
        request.setInCcy(getString(rowParams, row, I_Ccy, true, 3, true, errorList));
        request.setInCustno(getNumberString(rowParams, row, I_Custno, true, 8, true, errorList));
        request.setInAcctype(getNumberString(rowParams, row, I_Acctype, true, 9, true, errorList));

        // optional
        request.setInAcc2(getNumberString(rowParams, row, I_Acc2, false, 5, true, errorList));
        request.setInCtype(getNumberString(rowParams, row, I_Ctype, false, 2, false, errorList));
        request.setInTerm(getNumberString(rowParams, row, I_Term, false, 2, false, errorList));
        request.setInDealsrc(getString(rowParams, row, I_Dealsrc, true, 8, false, errorList));     // required
        request.setInDealid(getString(rowParams, row, I_Dealid, false, 20, false, errorList));
        request.setInSubdealid(getString(rowParams, row, I_Subdealid, false, 20, false, errorList));
        request.setInOpendate(getDate(rowParams, row, I_Opendate, false, curdate, errorList));

        request.setLineNumber(getRowNumber(row));
        request.setState(AccountBatchState.LOAD);
        return request;
    }

}

