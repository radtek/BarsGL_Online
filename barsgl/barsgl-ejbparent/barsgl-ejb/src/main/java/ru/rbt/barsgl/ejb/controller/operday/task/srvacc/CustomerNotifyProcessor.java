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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
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
public class CustomerNotifyProcessor implements Serializable {
    private static final Logger log = Logger.getLogger(CustomerNotifyProcessor.class);

    private final static String parentNodeName = "/Customer";
    private final static XmlParam[] paramNamesCust = {
             new XmlParam("CUST_NUM",   "CustomerNum",      false, 8)
            ,new XmlParam("BRANCHCODE", "BranchCode",       false, 3)
            ,new XmlParam("FCTYPE",     "Type",             false, 1)
            ,new XmlParam("CBTYPE",     "CbType",           false, 3)
            ,new XmlParam("RESIDENT",   "Resident",         false, 1)
            ,new XmlParam("NAME_ENG",   "CorporateDetails/ShortNameRus",        false, 50)
            ,new XmlParam("NAME_RUS",   "CorporateDetails/ShortNameEng",        false, 50)
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
        if (fullTopic == null || !fullTopic.contains("Customer")) {
            setErrorStatus(journalId, ERR_VAL, "Ошибка в содержании сообщения", "");
            return;
        }

        Map<String, String> xmlData = readFromXML(fullTopic, journalId, parentNodeName, paramNamesCust);
        if (xmlData == null) {
            // Запись в аудит, в таблицу аудита, в лог и возврат
            setErrorStatus(journalId, ERR_VAL, "Ошибка во время распознования XML", "");
            return;
        }

        // TODO validate
        String err = validateXmlParams(xmlData);
        if (!isEmpty(err)) {
            setErrorStatus(journalId, ERR_VAL, "Ошибка в формате XML", err);
            return;
        }

        CustDNInput inputParams = createInputParams(journalId, xmlData);

        CustDNMapped mappedParams = createMappedParams(journalId, inputParams);

        CustDNMapped.CustResult result = NOCHANGE;
        Customer customer = customerRepository.findById(Customer.class, mappedParams.getCustNo());
        if (null == customer) {
            result = createCustomer(journalId, mappedParams);
        } else {
            result = updateCustomer(journalId, mappedParams, customer);
        }

        log.info(String.format("Обработка сообщения по клиенту '%s' завершена, результат: '%s'", inputParams.getCustNo(), result.name()));

    }

    private Map<String, String> readFromXML(String bodyXML, Long jId, String parentName, XmlParam[] paramNames) throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        org.w3c.dom.Document doc = null;
        if (!bodyXML.startsWith("<?xml")) {
            bodyXML = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + bodyXML;
        }

        try {
            DocumentBuilder b = XmlUtilityLocator.getInstance().newDocumentBuilder();
            doc = b.parse(new ByteArrayInputStream(bodyXML.getBytes("UTF-8")));
            if (doc == null) {
                //Ошибка XML
                journalRepository.updateLogStatus(jId, ERR_VAL, "Ошибка при преобразовании входящего XML");
                return null;
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            journalRepository.updateLogStatus(jId, ERR_VAL, "Ошибка при преобразовании входящего XML\n" + e.getMessage());
            throw e;
        }

        NodeList nodes;
        XPath xPath = XmlUtilityLocator.getInstance().newXPath();
        try {
            Element element = doc.getDocumentElement();
            nodes = (NodeList) xPath.evaluate(parentName, element, XPathConstants.NODESET);
            if (nodes == null || nodes.getLength() != 1) {
                nodes = (NodeList) xPath.evaluate("Body" + parentName, element, XPathConstants.NODESET);
                if (nodes == null || nodes.getLength() != 1) {
                    //Ошибка XML
                    journalRepository.updateLogStatus(jId, ERR_VAL, "Отсутствуют неоходимые данные " + parentName);
                    return null;
                }
            }
        } catch (XPathExpressionException e) {
            journalRepository.updateLogStatus(jId, ERR_VAL, "Ошибка при чтении входящего XML\n" + e.getMessage());
            throw e;
        }

        Node parentNode = nodes.item(0);

        Map<String, String> params = new HashMap<>();

        for (XmlParam item : paramNames) {
            params.put(item.fieldName, (String) xPath.evaluate("./" + item.xmlName, parentNode, XPathConstants.STRING));
        }

        return params;
    }

    private String validateXmlParams(Map<String, String> xmlData) {
        StringBuilder builder = new StringBuilder();
        for (XmlParam item: paramNamesCust) {
            String value = xmlData.get(item.fieldName);
            if (isEmpty(value)) {
                if (!item.nullable)
                    builder.append(String.format("Не задано поле '%s'; ", item.xmlName));
            } else if (value.length() > item.length)
                builder.append(String.format("Длина поля '%s' > %d; ", item.xmlName, item.length));
        }
        return builder.toString();
    }

    private CustDNInput createInputParams(Long id, Map<String, String> xmlData) throws Exception {
        CustDNInput inputParam = journalRepository.executeInNewTransaction( persistence -> {
            CustDNInput input = inputRepository.createInputParams(id, xmlData);
            journalRepository.updateLogStatus(id, VALIDATED);
            return input;
        });
        return inputParam;
    }

    private CustDNMapped createMappedParams(Long id, CustDNInput inputData) throws Exception {
        StringBuilder err = new StringBuilder();
        CustDNMapped mappedParam = new CustDNMapped(id);
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
            setErrorStatus(id, ERR_MAP, "Ошибка преобразования данных", err.toString());
            return null;
        }

        return journalRepository.executeInNewTransaction(persistence -> {
                CustDNMapped mapped = mappedRepository.save(mappedParam);
                journalRepository.updateLogStatus(id, MAPPED);
                return mapped;
        });
    }

    private CustDNMapped.CustResult createCustomer(Long journalId, CustDNMapped mappedParams) throws Exception {
        CustDNMapped.CustResult result = INSERT;
        return journalRepository.executeInNewTransaction(persistence -> {
            mappedRepository.updateResult(journalId, result);
            if (doOnline()) {
                customerRepository.createCustomer(mappedParams);
                journalRepository.updateLogStatus(journalId, PROCESSED);
                return result;
            } else {
                journalRepository.updateLogStatus(journalId, EMULATED);
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
                customerRepository.updateCustomer(customer, mappedParams);
                journalRepository.updateLogStatus(journalId, PROCESSED);
                return result;
            } else {
                journalRepository.updateLogStatus(journalId, EMULATED);
                return result;
            }
        });
    }

    private void setErrorStatus(Long journalId, CustDNJournal.Status status, String msg, String errorMsg) {
        CustDNJournal journal = journalRepository.updateLogStatus(journalId, status, substr(msg + ": " + errorMsg, 255));
        auditController.warning(CustomerDetailsNotify, msg, journal, errorMsg);
    }

    private boolean doOnline() {
        String prop = propertiesRepository.getStringDef(PropertyName.CUST_LOAD_ONLINE.getName(), "Yes");
        return !isEmpty(prop) && "Y".equals(prop.toUpperCase().substring(0, 1));
    }

    private static class XmlParam {
        String fieldName;
        String xmlName;
        int length;
        boolean nullable;

        public XmlParam(String fieldName, String xmlName, boolean nullable, int length) {
            this.fieldName = fieldName;
            this.xmlName = xmlName;
            this.length = length;
            this.nullable = nullable;
        }
    }

}
