package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.entity.cust.CustDNInput;
import ru.rbt.barsgl.ejb.entity.cust.CustDNJournal;
import ru.rbt.barsgl.ejb.entity.cust.CustDNMapped;
import ru.rbt.barsgl.ejb.entity.cust.Customer;
import ru.rbt.barsgl.ejb.props.PropertyName;
import ru.rbt.barsgl.ejb.repository.AcDNJournalDataRepository;
import ru.rbt.barsgl.ejb.repository.customer.CustDNInputRepository;
import ru.rbt.barsgl.ejb.repository.customer.CustDNJournalRepository;
import ru.rbt.barsgl.ejb.repository.customer.CustDNMappedRepository;
import ru.rbt.barsgl.ejb.repository.customer.CustomerRepository;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.mapping.YesNo;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static ru.rbt.audit.entity.AuditRecord.LogCode.CustomerDetailsNotify;
import static ru.rbt.barsgl.ejb.entity.cust.CustDNJournal.Status.*;
import static ru.rbt.barsgl.ejb.entity.cust.CustDNMapped.CustResult.INSERT;
import static ru.rbt.barsgl.ejb.entity.cust.CustDNMapped.CustResult.NOCHANGE;
import static ru.rbt.barsgl.ejb.entity.cust.CustDNMapped.CustResult.UPDATE;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.ejbcore.util.StringUtils.substr;

/**
 * Created by er18837 on 15.12.2017.
 */
@Stateless
@LocalBean
public class CustomerNotifyProcessor extends CommonNotifyProcessor implements Serializable {
    private static final Logger log = Logger.getLogger(CustomerNotifyProcessor.class);

    public static final String journalName = "GL_CUDENO1";
    public static final String charsetName = "IBM866";
    private static final String parentNodeName = "Customer";
    private static final XmlParam[] paramNamesCust = {
             new XmlParam("CUST_NUM",   "CustomerNum",      false, 8)
            ,new XmlParam("BRANCHCODE", "BranchCode",       false, 3)
            ,new XmlParam("FCTYPE",     "Type",             false, 1)
            ,new XmlParam("CBTYPE",     "CbType",           false, 3)
            ,new XmlParam("RESIDENT",   "Resident",         false, 1)
            ,new XmlParam("NAME_ENG",   "CorporateDetails/ShortNameEng",        false, 90)
            ,new XmlParam("NAME_RUS",   "CorporateDetails/ShortNameRus",        false, 80)
            ,new XmlParam("LEGAL_FORM", "CorporateDetails/ShortLegalFormRus",   true, 20)
    };

    @EJB
    CustDNJournalRepository journalRepository;

    @Inject
    CustDNInputRepository inputRepository;

    @Inject
    CustDNMappedRepository mappedRepository;

    @Inject
    CustomerRepository customerRepository;

    @Inject
    AcDNJournalDataRepository acdnRepository;

    @EJB
    private PropertiesRepository propertiesRepository;

    @EJB
    AuditController auditController;

    public void process(String fullTopic, final Long journalId) throws Exception {
        if (fullTopic == null || !fullTopic.contains(parentNodeName)) {
            setErrorStatus(journalId, ERR_VAL, "Ошибка в содержании сообщения", "");
            return;
        }

        Map<String, String> xmlData = readFromXML(fullTopic, charsetName, "/" + parentNodeName, paramNamesCust, journalId);
        if (xmlData == null) {
            // Запись в аудит, в таблицу аудита, в лог и возврат
            setErrorStatus(journalId, ERR_VAL, "Ошибка во время распознования XML", "");
            return;
        }

        String clientType = xmlData.get("FCTYPE");
        if ("I".equals(clientType)) {       // пропускаем физиков
            journalRepository.updateLogStatus(journalId, SKIPPED);
            return;
        }

        // TODO validate
        String err = validateXmlParams(xmlData, paramNamesCust);
        if (!isEmpty(err)) {
            setErrorStatus(journalId, ERR_VAL, "Ошибка в формате XML", err);
            return;
        }

        CustDNInput inputParams = createInputParams(journalId, xmlData);

        CustDNMapped mappedParams = createMappedParams(journalId, inputParams);
        if (null == mappedParams)
            return;

        CustDNMapped.CustResult result;
        Customer customer = customerRepository.findById(Customer.class, inputParams.getCustNo());
        if (null == customer) {
            result = createCustomer(journalId, mappedParams);
        } else {
            result = updateCustomer(journalId, mappedParams, customer);
        }

        log.info(String.format("Обработка сообщения по клиенту '%s' завершена, результат: '%s'", inputParams.getCustNo(), result.name()));

    }

    private CustDNInput createInputParams(Long journalId, Map<String, String> xmlData) throws Exception {
        CustDNInput inputParam = journalRepository.executeInNewTransaction( persistence -> {
            CustDNInput input = inputRepository.createInputParams(journalId, xmlData);
            journalRepository.updateLogStatus(journalId, VALIDATED);
            return input;
        });
        return inputParam;
    }

    private CustDNMapped createMappedParams(Long journalId, CustDNInput inputData) throws Exception {
        StringBuilder err = new StringBuilder();
        CustDNMapped mappedParam = new CustDNMapped(journalId);
        mappedParam.setCustNo(inputData.getCustNo());
        mappedParam.setNameEng(substr(inputData.getNameEng(), 35));
        mappedParam.setNameEngShort(substr(inputData.getNameEng(), 20));
        mappedParam.setNameRus(substr(inputData.getLegalForm() + " " + inputData.getNameRus(), 80));
        try {
            String res = inputData.getResident().replace('Y', 'R');
            mappedParam.setResident(Customer.Resident.valueOf(res));
        } catch (IllegalArgumentException e) {
            err.append(String.format("Неверное значение поля 'RESIDENT': '%s'; ", inputData.getResident()));
        }

        // TODO
        String branch = acdnRepository.selectMidasBranchByBranch(inputData.getBranch());
        if (isEmpty(branch)) {
            err.append(String.format("Не найден бранч Midas для 'BRANCHCODE' = '%s'; ", inputData.getBranch()));
        } else {
            mappedParam.setBranch(branch);
        }

        String custType = mappedRepository.getCustTypeByFccType(inputData.getFcCbType());
        if (isEmpty(custType)) {
            err.append(String.format("Не найден тип собственности клиента для 'FCTYPE' = '%s'; ", inputData.getFcCbType()));
        } else {
            mappedParam.setCbType(custType);
        }

        try {
            mappedParam.setClientType(Customer.ClientType.valueOf(inputData.getFcCustType()));
        } catch (IllegalArgumentException e) {
            err.append(String.format("Неверный тип клиента 'FCTYPE' = '%s'; ", inputData.getFcCustType()));
        }

        if (0 != err.length()) {
            setErrorStatus(journalId, ERR_MAP, "Ошибка преобразования данных", err.toString());
            return null;
        }

        return journalRepository.executeInNewTransaction(persistence -> {
                CustDNMapped mapped = mappedRepository.save(mappedParam);
                journalRepository.updateLogStatus(journalId, MAPPED);
                return mapped;
        });
    }

    private CustDNMapped.CustResult createCustomer(Long journalId, CustDNMapped mappedParams) throws Exception {
        CustDNMapped.CustResult result = INSERT;
        return journalRepository.executeInNewTransaction(persistence -> {
            mappedRepository.updateResult(journalId, result);
            if (doOnline()) {
                customerRepository.createCustomer(mappedParams);
                journalRepository.updateLogStatus(journalId, PROCESSED, result.name());
                return result;
            } else {
                journalRepository.updateLogStatus(journalId, EMULATED, result.name());
                return result;
            }
        });
    }

    private CustDNMapped.CustResult updateCustomer(Long journalId, CustDNMapped mappedParams, Customer customer) throws Exception {
        return journalRepository.executeInNewTransaction(persistence -> {
            boolean noChange =  customer.getBranch().equals(mappedParams.getBranch())
                             && customer.getCbType().equals(mappedParams.getCbType())
                             && customer.getResident().equals(mappedParams.getResident());

            CustDNMapped.CustResult result = noChange ? NOCHANGE : UPDATE;
            mappedRepository.updateOldFields(journalId, customer, result);

            if (doOnline()) {
                if (!noChange) {
                    customerRepository.updateCustomer(customer, mappedParams);
                }
                journalRepository.updateLogStatus(journalId, PROCESSED, result.name());
                return result;
            } else {
                journalRepository.updateLogStatus(journalId, EMULATED, result.name());
                return result;
            }
        });
    }

    private void setErrorStatus(Long journalId, CustDNJournal.Status status, String msg, String errorMsg) {
        journalRepository.updateLogStatus(journalId, status, substr(msg + ": " + errorMsg, 255));
        auditController.warning(CustomerDetailsNotify, msg, journalName, journalId.toString(), errorMsg);
    }

    private boolean doOnline() {
        String prop = propertiesRepository.getStringDef(PropertyName.CUST_LOAD_ONLINE.getName(), "Yes");
        return !isEmpty(prop) && "Y".equals(prop.toUpperCase().substring(0, 1));
    }

    @Override
    protected void updateLogStatusError(Long jourbnalId, String message) {
        journalRepository.updateLogStatus(jourbnalId, ERR_VAL, message);
    }
}
