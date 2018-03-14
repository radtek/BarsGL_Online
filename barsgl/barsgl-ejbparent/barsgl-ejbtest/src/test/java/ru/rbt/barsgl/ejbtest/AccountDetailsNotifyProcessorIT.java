package ru.rbt.barsgl.ejbtest;

import com.microsoft.schemas.office.visio.x2012.main.RelType;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.rbt.barsgl.common.xml.DomBuilder;
import ru.rbt.barsgl.ejb.controller.operday.task.AccountDetailsNotifyTask;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.XmlUtilityLocator;
import ru.rbt.barsgl.ejb.entity.acc.AcDNJournal;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.util.StringUtils;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Sources.FC12;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Sources.FCC;

/**
 * Created by er18837 on 12.03.2018.
 */
public class AccountDetailsNotifyProcessorIT extends AbstractTimerJobIT  {
    public static final Logger logger = Logger.getLogger(AccountDetailsNotifyProcessorIT.class.getName());
//    public static final String parentNode = "//soapenv:Body/acc:AccountList/acc:AccountDetails";    //"//Body/AccountList/AccountDetails";
//    public static final String cbNode = "/acc:CBAccountNo";
    public static final String parentNode = "Body/AccountList/AccountDetails";
    public static final String cbNode = "CBAccountNo";
    public static final String openNode = "OpenDate";
    public static final String closeNode = "CloseDate";
    public static final String customerNode = "CustomerNo";
    public static final String branchNode = "Branch";
    public static final String ccyNode = "Ccy";

    private DocumentBuilder docBuilder;
    private XPath xPath;

    @Before
    public void before() throws ParserConfigurationException {
        docBuilder = XmlUtilityLocator.getInstance().newDocumentBuilder();
        xPath = XmlUtilityLocator.getInstance().newXPath();
    }

    @Test
    public void testOpenFcc() throws Exception {
        String message = IOUtils.toString(this.getClass().getResourceAsStream("/AccountDetailsOpenFcc.xml"), "UTF-8");

        Document doc = getDocument(message);
        String bsaacid = getAccountParam(doc, cbNode);
        Date dateOpen = DateUtils.dbDateParse(getAccountParam(doc, openNode));
        String midasBranch = getMidasBranch(getAccountParam(doc, branchNode));
        String acid = getAccountParam(doc, customerNode) + getAccountParam(doc, ccyNode) + "000090" + midasBranch;

        baseEntityRepository.executeNativeUpdate("delete from GL_ACC where BSAACID = ?", bsaacid);

        Long journalId = getJournalId() + 1;
        remoteAccess.invoke(AccountDetailsNotifyTask.class, "processOneMessage",
                FCC, message, null);

        GLAccount account = getAccount(bsaacid);
        Assert.assertNotNull(account);
        Assert.assertEquals(acid, account.getAcid());
        Assert.assertEquals(getAccountParam(doc, customerNode), account.getCustomerNumber());
        Assert.assertEquals(midasBranch, account.getBranch());
        Assert.assertEquals(bsaacid.substring(10, 13), account.getCompanyCode().substring(1,4));
        Assert.assertEquals(dateOpen, account.getDateOpen());
        Assert.assertNull(account.getDateClose());
        Assert.assertEquals(GLAccount.RelationType.ZERO.getValue(), account.getRelationType());
        Assert.assertNull(account.getDescription());

        checkJournal(journalId, FCC, bsaacid);
    }

    @Test
    public void testCloseFcc() throws Exception {
        String message = getMessage("AccountDetailsCloseFcc.xml");
        Document doc = getDocument(message);
        String cb = getAccountParam(doc, cbNode);
        String dtClose = getAccountParam(doc, closeNode);
        Date dateClose = DateUtils.dbDateParse(dtClose);

        String cbMask = cb.substring(0, 8) + "__" + cb.substring(10, 13) + '%'; // "40817810__009%";
        String bsaacid = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), cbMask);
        correctCurrency(bsaacid, cb.substring(5, 8));
        message = changeAccountParam(message, cbNode, cb, bsaacid);

        Long journalId = getJournalId() + 1;
        remoteAccess.invoke(AccountDetailsNotifyTask.class, "processOneMessage",
                FCC, message, null);

        GLAccount account = getAccount(bsaacid);
        Assert.assertNotNull(account);
        Assert.assertEquals(dateClose, account.getDateClose());

        checkJournal(journalId, FCC, bsaacid + " закрыт");

        baseEntityRepository.executeNativeUpdate("update GL_ACC set DTC = null where BSAACID = ?", bsaacid);
    }

    @Test
    public void testCloseFc12() throws Exception {
        String message = getMessage("AccountDetailsCloseFc12.xml");
        Document doc = getDocument(message);
        String cb = getAccountParam(doc, cbNode);
        String dtClose = getAccountParam(doc, closeNode);
        Date dateClose = DateUtils.dbDateParse(dtClose);

        String cbMask = cb.substring(0, 8) + "__" + cb.substring(10, 13) + '%';
        String bsaacid = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), cbMask);
        correctCurrency(bsaacid, cb.substring(5, 8));
        message = changeAccountParam(message, cbNode, cb, bsaacid);

        Long journalId = getJournalId() + 1;
        remoteAccess.invoke(AccountDetailsNotifyTask.class, "processOneMessage",
                FC12, message, null);

        GLAccount account = getAccount(bsaacid);
        Assert.assertNotNull(account);
        Assert.assertEquals(dateClose, account.getDateClose());

        checkJournal(journalId, FC12, bsaacid + " закрыт");

        baseEntityRepository.executeNativeUpdate("update GL_ACC set DTC = null where BSAACID = ?", bsaacid);
    }

    @Test
    public void testChangeBranch() throws Exception {
        String message = getMessage("AccountDetailsBranchFc12.xml");
        Document doc = getDocument(message);
        String cb = getAccountParam(doc, cbNode);
        String midasBranch = getMidasBranch(getAccountParam(doc, branchNode));

        String cbMask = cb.substring(0, 8) + "__" + cb.substring(10, 13) + '%';
        String bsaacid = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), cbMask);
        GLAccount accountWas = getAccount(bsaacid);
        if (midasBranch.equals(accountWas.getBranch()))
            baseEntityRepository.executeNativeUpdate("update GL_ACC set BRANCH = ? where BSAACID = ?", "000", bsaacid);

        correctCurrency(bsaacid, cb.substring(5, 8));
        message = changeAccountParam(message, cbNode, cb, bsaacid);

        Long journalId = getJournalId() + 1;
        remoteAccess.invoke(AccountDetailsNotifyTask.class, "processOneMessage",
                FC12, message, null);

        GLAccount account = getAccount(bsaacid);
        Assert.assertNotNull(account);
        Assert.assertEquals(midasBranch, account.getBranch());

        checkJournal(journalId, FC12, bsaacid + " branch");

        baseEntityRepository.executeNativeUpdate("update GL_ACC set BRANCH = ? where BSAACID = ?", accountWas.getBranch(), bsaacid);
    }

    private String getMessage(String resourceName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream("/" + resourceName), "UTF-8");
    }

    private Document getDocument(String message) throws IOException, SAXException {
        return docBuilder.parse(new ByteArrayInputStream(message.getBytes("UTF-8")));
    }

    private String getAccountParam(Document doc, String paramName) throws XPathExpressionException {
        return (String) xPath.evaluate(parentNode + "/" + paramName, doc.getDocumentElement(), XPathConstants.STRING);
    }

    private String changeAccountParam(String message, String paramName, String oldValue, String newValue) {
        return message.replace(paramName + ">" + oldValue, paramName + ">" + newValue);
    }

    private GLAccount getAccount(String bsaacid) {
        return (GLAccount) baseEntityRepository.selectFirst(GLAccount.class, "from GLAccount a where a.bsaAcid = ?1", bsaacid);
    }

    private void correctCurrency(String bsaacid, String ccy) {
        baseEntityRepository.executeNativeUpdate("update GL_ACC a set CCY = (select GLCCY from CURRENCY c where c.CBCCY = a.CCY) where BSAACID = ? and CCY = ?", bsaacid, ccy);
    }

    private String getMidasBranch(String fccBranch) throws SQLException {
        DataRecord res = baseEntityRepository.selectFirst("SELECT MIDAS_BRANCH FROM DH_BR_MAP where FCC_BRANCH = ?", fccBranch);
        return res.getString(0);
    }

    private Long getJournalId() throws SQLException {
        DataRecord res = baseEntityRepository.selectFirst("select max(MESSAGE_ID) from GL_ACDENO a");
        return res.getLong(0);
    }

    private void checkJournal(Long journalId, AcDNJournal.Sources src, String like) {
        AcDNJournal journal = (AcDNJournal) baseEntityRepository.findById(AcDNJournal.class, journalId);
        Assert.assertNotNull(journal);
        Assert.assertEquals(src, journal.getSource());
        Assert.assertEquals(AcDNJournal.Status.PROCESSED, journal.getStatus());
        Assert.assertTrue(journal.getComment().contains(like));

    }

/*
    private NamespaceContext getContext() {
        return new NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                return "acc".equals(prefix) ? "urn:ucbru:gbo:v5:acc" : "http://schemas.xmlsoap.org/soap/envelope/";
            }

            public String getPrefix(String namespaceURI) {
                return "acc";
            }

            public Iterator getPrefixes(String namespaceURI) {
                List list = new ArrayList(1);
                list.add("acc");
                return list.iterator();
            }
        };
    }
*/
}
