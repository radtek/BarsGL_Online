package ru.rbt.barsgl.ejb.controller.acc;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchRequest;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.validation.DataValidator;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.RUB;
import static ru.rbt.ejbcore.mapping.YesNo.N;
import static ru.rbt.ejbcore.mapping.YesNo.Y;
import static ru.rbt.ejbcore.util.StringUtils.*;
import static ru.rbt.ejbcore.validation.ErrorCode.ACC_BATCH_OPEN;

/**
 * Created by Ivan Sevastyanov on 26.10.2018.
 */
@Stateless
@LocalBean
public class AccountValidationSupportBean {

    @Inject
    private DateUtils dateUtils;

    @EJB
    private CoreRepository repository;

    @EJB
    private OperdayController operdayController;

    @SuppressWarnings("All")
    public List<ValidationError> validateUpdateOneRequest(AccountBatchPackage batchPackage, AccountBatchRequest request) {
        try {
            return (List<ValidationError>) repository.executeInNewTransaction(persistence -> {
                ValidationContext context = new ValidationContext();
                context.addValidator(() -> {
                    try {
                        DataRecord record = repository.selectFirst("select * from IMBCBBRP where A8BRCD = ?", request.getInBranch());
                        Assert.isTrue(null != record
                                , () -> new ValidationError(ACC_BATCH_OPEN, format("Код отделения '%s' не найден в таблице IMBCBBRP", request.getInBranch())));
                        request.setCalcCbcc(record.getString("A8CMCD"));
                    } catch (SQLException e) {
                        throw new DefaultApplicationException(e.getMessage(), e);
                    }
                });
                context.addValidator(() -> {
                    try {
                        Assert.isTrue(null != repository.selectFirst("select * from CURRENCY where glccy = ?", request.getInCcy())
                                , () -> new ValidationError(ACC_BATCH_OPEN, format("Код валюты '%s' не найден в таблице CURRENCY", request.getInCcy())));
                    } catch (SQLException e) {
                        throw new DefaultApplicationException(e.getMessage(), e);
                    }
                });
                context.addValidator(() -> {
                    try {
                        DataRecord record = repository.selectFirst("select BXCTYP from SDCUSTPD where bbcust = ?", request.getInCustno());
                        Assert.isTrue(null != record
                                , () -> new ValidationError(ACC_BATCH_OPEN, format("Код клиента '%s' не найден в таблице SDCUSTPD", request.getInCustno())));
                        request.setCalcCtype(record.getString("BXCTYP"));
                    } catch (SQLException e) {
                        throw new DefaultApplicationException(e.getMessage(), e);
                    }
                });
                context.addValidator(() -> {
                    try {
                        DataRecord record = repository.selectFirst("select * from GL_ACTNAME  where ACCTYPE = ?", request.getInAcctype());
                        Assert.isTrue(null != record
                                , () -> new ValidationError(ACC_BATCH_OPEN, format("Код AccType '%s' не найден в таблице GL_ACTNAME", request.getInAcctype().trim())));
                        if (null != record.getString("FL_CTRL")) {
                            Assert.isTrue(!"Y".equals(record.getString("FL_CTRL"))
                                    , () -> new ValidationError(ACC_BATCH_OPEN, format("Код AccType '%s' соответствует счетам, остаток по которым контролируются во внешней системе. Пакетное открытие таких счетов запрещено"
                                            , request.getInAcctype())));
                        }
                        if (null != record.getString("TECH_ACT")) {
                            Assert.isTrue(!"Y".equals(record.getString("TECH_ACT"))
                                    , () -> new ValidationError(ACC_BATCH_OPEN, format("Код AccType '%s' соответствует техниническим счетам", request.getInAcctype())));
                        }
                    } catch (SQLException e) {
                        throw new DefaultApplicationException(e.getMessage(), e);
                    }
                });
                context.addValidator(() -> {
                    try {
                        if (!isEmpty(request.getInCtype())
                                && !"00".equals(leftPad(request.getInCtype(),2,"0"))) {
                            Assert.isTrue(null != repository.selectFirst("select 1 from CBCTP where CTYPE = ?", request.getInCtype().trim())
                                    , () -> new ValidationError(ACC_BATCH_OPEN, format("Тип собственности '%s' не найден в таблице CBCTP", request.getInCtype())));
                        }
                    } catch (SQLException e) {
                        throw new DefaultApplicationException(e.getMessage(), e);
                    }
                });
                context.addValidator(() -> {
                    try {
                        if (!isEmpty(request.getInTerm())
                                && !"00".equals(leftPad(request.getInTerm(),2,"0"))) {
                            Assert.isTrue(null != repository.selectFirst("select 1 from GL_DICTERM where TERM = ?", request.getInTerm().trim())
                                    , () -> new ValidationError(ACC_BATCH_OPEN, format("Код срока '%s' не найден в таблице GL_DICTERM", request.getInTerm())));
                        }
                    } catch (SQLException e) {
                        throw new DefaultApplicationException(e.getMessage(), e);
                    }
                });
                context.addValidator(() -> {
                    try {
                        if (!isEmpty(request.getInAcc2())) {
                            Assert.isTrue(null != repository.selectFirst("select 1 from BSS where ACC2 = ?", request.getInAcc2())
                                    , () -> new ValidationError(ACC_BATCH_OPEN, format("Балансовый счет 2 порядка '%s' не найден в таблице BSS", request.getInAcc2())));
                        }
                    } catch (SQLException e) {
                        throw new DefaultApplicationException(e.getMessage(), e);
                    }
                });
                context.addValidator(() -> {
                    try {
                        if (!isEmpty(request.getInDealsrc())) {
                            Assert.isTrue(null != repository.selectFirst("select 1 from GL_SRCPST where ID_SRC = ?", request.getInDealsrc())
                                    , () -> new ValidationError(ACC_BATCH_OPEN, format("Код источника сделки '%s' не найден в таблице GL_SRCPST", request.getInDealsrc())));
                        }
                    } catch (SQLException e) {
                        throw new DefaultApplicationException(e.getMessage(), e);
                    }
                });
                context.addValidator(() -> {
                    if (isEmpty(request.getInDealid())
                            && !isEmpty(request.getInSubdealid())) {
                        throw new ValidationError(ACC_BATCH_OPEN, "Поле ИД субсделки не может быть заполнено, если ИД сделки не заполнен");
                    }
                });
                context.addValidator(() -> {
                    Date maxDate = addMonths(operdayController.getOperday().getCurrentDate(), 1);
                    if (null != request.getInOpendate() && request.getInOpendate().after(maxDate)) {
                        throw new ValidationError(ACC_BATCH_OPEN, format("Дата открытия '%s' больше максимально допустимой '%s'"
                                , dateUtils.onlyDateString(request.getInOpendate()), dateUtils.onlyDateString(maxDate)));
                    }
                });
                context.addValidator(() -> {
                    try {
                        DataRecord record = repository.selectFirst(
                                "select * from GL_AU_PRMVAL p, gl_user u\n" +
                                        " where p.ID_USER = u.ID_USER\n" +
                                        "   and u.USER_NAME = ? \n" +
                                        "   and p.PRM_CODE = 'HeadBranch' and p.PRMVAL in ('*', ?)", batchPackage.getProcUser(), request.getCalcCbcc());
                        if (null == record) {
                            throw new ValidationError(ACC_BATCH_OPEN, format("У пользователя '%s' нет прав доступа на открытие счетов в филиале '%s'"
                                    , batchPackage.getProcUser(), request.getCalcCbcc()));
                        }
                    } catch (SQLException e) {
                        throw new DefaultApplicationException(e.getMessage(), e);
                    }
                });
                context.addValidator(() -> {
                    final Integer ctypeInt = parseIntSafe(request.getCalcCtype());
                    try {
                        if (ctypeInt < 4) {
                            DataRecord imb = repository.selectFirst("Select A8BRCD from IMBCBBRP where A8BICN = ?", request.getInCustno());
                            if (null != imb) {
                                Assert.isTrue(imb.getString("A8BRCD").equals(request.getInBranch())
                                        , () -> new ValidationError(ACC_BATCH_OPEN, format("Отделение '%s' не соответствует клиенту '%s'. Клиенту '%s' соответствует отделение '%s'"
                                                , request.getInBranch(), request.getInCustno(),request.getInCustno(), imb.getString("A8BRCD"))));
                            }
                        }
                    } catch (SQLException e) {
                        throw new DefaultApplicationException(e.getMessage(), e);
                    }
                });
                context.addValidator(()-> {
                    final Integer ctypeInt = parseIntSafe(request.getCalcCtype());
                    if ("00".equals(leftPad(ifEmpty(request.getInCtype(), "0"),2,"0"))) { // null or 0 or 00
                        if (ctypeInt > 3) {
                            request.setCalcCtypeAcc(ctypeInt.toString());
                        } else if (ctypeInt >= 0 && ctypeInt <=3) {
                            request.setCalcCtypeAcc("0");
                        }
                    } else {
                        if (request.getInCtype().equals(ctypeInt.toString())) {
                            request.setCalcCtypeAcc(request.getInCtype());
                        } else if (!request.getInCtype().equals(ctypeInt.toString())) {
                            if (parseIntSafe(request.getInCtype()) > 3 && ctypeInt >=0 && ctypeInt <= 3) {
                                request.setCalcCtypeAcc(request.getInCtype());
                            } else {
                                throw new ValidationError(ACC_BATCH_OPEN
                                        , format("Тип собственности клиента CTYPE_IN '%s' не соответствует значению CTYPE_CUS '%s' из справочника клиентов SDCUSTPD для клиента CUSTNO_IN '%s'"
                                        , request.getInCtype(), ctypeInt, request.getInCustno()));
                            }
                        }
                    }
                });

                context.addValidator(new DataValidator() {
                    @Override
                    public void validate() throws ValidationError {
                        try {
                            final Integer ctypeInt = parseIntSafe(request.getCalcCtype());
                            final YesNo plAct = calcPlAct(request);
                            String custype = calcCtypeAcc(request.getCalcCtypeAcc());
                            String term = leftPad(ifEmpty(request.getInTerm(),"0"), 2, "0");
                            DataRecord record1 = findActparm(request.getInAcctype(), rightPad(custype, 2, " "), term);
                            if (N == plAct && null == record1) {
                                if (parseIntSafe(request.getInTerm(), 0) >= 0) {
                                    custype = calcCtypeAcc(request.getCalcCtypeAcc()); term = "00";
                                    record1 = findActparm(request.getInAcctype(), rightPad(custype, 2, " "), term);
                                    if (null == record1) {
                                        if (parseIntSafe(request.getCalcCtypeAcc()) >= 0) {
                                            if (parseIntSafe(request.getCalcCtypeAcc()) > 3 && ctypeInt >= 0 && ctypeInt <= 3) {
                                                throwValidationError("1", custype, term);
                                            } else {
                                                custype = "00"; term = leftPad(ifEmpty(request.getInTerm(),"0"), 2, "0");
                                                record1 = findActparm(request.getInAcctype(), custype, term);
                                                if (parseIntSafe(request.getInTerm(), 0) >= 0 && parseIntSafe(request.getCalcCtypeAcc()) >= 0) {
                                                    custype = "00"; term = "00";
                                                    record1 = findActparm(request.getInAcctype(), custype, term);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (null == record1) {
                                throwValidationError("2", custype, term);
                            } else {
                                DataRecord finalRecord = record1;
                                Assert.isTrue(!isEmpty(record1.getString("ACOD")) && !isEmpty(record1.getString("AC_SQ")),
                                        () -> new ValidationError(ACC_BATCH_OPEN
                                                , format("Найдена некорректная запись GL_ACTPARM: ACCTYPE='%s', CUSTYPE='%s', TERM='%s', ACC2='%s', PLCODE='%s', ACOD='%s', AC_SQ='%s', DTB='%s', DTE='%s' "
                                                    , finalRecord.getString("ACCTYPE"), finalRecord.getString("CUSTYPE"), finalRecord.getString("TERM"), finalRecord.getString("ACC2"), finalRecord.getString("PLCODE"), finalRecord.getString("ACOD"), finalRecord.getString("AC_SQ"), finalRecord.getString("DTB"), finalRecord.getString("DTE"))));
                                // все хорошо, но вдруг ...
                                request.setCalcCtypeParm(record1.getString("CUSTYPE").trim());
                                request.setCalcTermParm(record1.getString("TERM"));
                                request.setCalcAcc2Parm(record1.getString("ACC2"));
                                request.setCalcAcodParm(record1.getString("ACOD"));
                                request.setCalcAcsqParm(record1.getString("AC_SQ"));
                                request.setCalcPlcodeParm(record1.getString("PLCODE"));
                                if (!isEmpty(request.getInAcc2()) && !request.getInAcc2().trim().equals(request.getCalcAcc2Parm().trim())) {
                                    throw new ValidationError(ACC_BATCH_OPEN
                                            , format("Балансовый счет 2 порядка ACC2_IN '%s' не соответствует значению ACC2_PARM '%s', соответствующему настройкам GL_ACTPARM"
                                            , request.getInAcc2(), request.getCalcAcc2Parm()));
                                }
                            }
                        } catch (SQLException e) {
                            throw new DefaultApplicationException(e.getMessage(), e);
                        }
                    }

                    private void throwValidationError(String label, String custype, String term) {
                        throw new ValidationError(ACC_BATCH_OPEN, format("Не найдена запись GL_ACTPARM по  acctype= '%s' and term = '%s' and custype = '%s' (место проверки '%s')"
                                , request.getInAcctype(), term, custype, label));
                    }
                });
                context.addValidator(() -> {
                    try {
                        if (!isEmpty(request.getCalcPlcodeParm())) {
                            DataRecord record = repository.selectFirst("select A8BICN from IMBCBBRP where A8BRCD=?", request.getInBranch());
                            if (request.getCalcPlcodeParm().trim().equals(record.getString("A8BICN"))) {
                                throw new ValidationError(ACC_BATCH_OPEN, format("Код клиента %s CUST_IN не соответствует коду отделения %s BRANCH_IN. Для счетов ОФР должен быть клиент этого отделения %s IMBCBBR.A8BICN."
                                        , request.getInCtype(), request.getInBranch(), record.getString("A8BICN")));
                            }
                            if (!isEmpty(request.getInDealid()) || !isEmpty(request.getInSubdealid())) {
                                throw new ValidationError(ACC_BATCH_OPEN, format("Входные параметры DEALID='%s' и SUBDEALID='%s' не могут заполняться для счетов доходов и расходов, PLCODE='%s'"
                                        , ifEmpty(request.getInDealid(),"<empty>"), ifEmpty(request.getInSubdealid(), "<empty>"), request.getCalcPlcodeParm()));
                            }
                        }
                    } catch (SQLException e) {
                        throw new DefaultApplicationException(e.getMessage(), e);
                    }
                });
                context.addValidator(() -> {
                    if (!isEmpty(request.getCalcPlcodeParm())
                            && !RUB.getCurrencyCode().equals(request.getInCcy())) {
                        throw new ValidationError(ACC_BATCH_OPEN, format("Некорректная валюта '%s' для счета ОФР. Валюта должна быть 'RUR'", request.getInCcy()));
                    }
                });
                context.validateAll();
                repository.update(request);
                return context.getErrors();
            });
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private Integer parseIntSafe(String integer) {
        return parseIntSafe(integer, -100);
    }

    private Integer parseIntSafe(String integer, Integer ifNull) {
        try {
            return StringUtils.isEmpty(integer) ? ifNull: Integer.parseInt(integer.trim());
        } catch (NumberFormatException e) {
            return ifNull;
        }
    }

    private YesNo calcPlAct (AccountBatchRequest request) throws SQLException {
        return Optional.ofNullable(repository.selectFirst(
                "select * from gl_actparm where acctype = ? and custype = ? and term = ? and trim(plcode) is not null"
                , request.getInAcctype(), leftPad(ifEmpty(request.getCalcCtypeAcc(),"0"),2, "0"), leftPad(ifEmpty(request.getInTerm(),"0"), 2, "0")))
                .map(r -> Y).orElse(N);
    }

    private DataRecord findActparm(String acctype, String custype, String term) throws SQLException {
        return repository.selectFirst("select * from gl_actparm where acctype = ? and custype = ? and term = ?"
                , acctype, custype, term);
    }

    private Date addMonths(Date from, int months) {
        return org.apache.commons.lang3.time.DateUtils.addMonths(from, months);
    }

    private String calcCtypeAcc(String source) {
        return "0".equals(source) || isEmpty(source) ? "00" : trim(source);
    }

}
