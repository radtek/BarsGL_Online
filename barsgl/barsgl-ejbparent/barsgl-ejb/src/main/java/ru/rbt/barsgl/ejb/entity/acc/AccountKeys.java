package ru.rbt.barsgl.ejb.entity.acc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;
import static ru.rbt.ejbcore.util.StringUtils.ifEmpty;
import static ru.rbt.ejbcore.util.StringUtils.rsubstr;

/**
 * Created by ER18837 on 29.07.15.
 * Параметры открываемого счета, передаваемые в проводке
 */
public class AccountKeys implements Serializable {
    public static final String MASK_SEPARATOR = "#";
//    public static final String FIELD_SEPARATOR = "\\.";
    public static final String FIELD_SEPARATOR = ";";
            // BRANCH # CCY # CUSTNO # ATYPE # CUSTTYPE # TERM # GL_SEQ # CBCCN # ACC2 # PLCODE # ACOD # SQ # DEALSRC # DEALID # SUBDEALID
    public static final String patternAccountKeyMask =
            "\\d{3}#[A-Z]{3}#\\d{8}#\\d{9}#\\d{0,3}#\\d{0,2}#((XX.{0,8}|PL.{0,8}|GL.{0,8})|\\d{1,10})#\\d{0,4}#\\d{0,5}#\\d{0,5}#\\d{0,4}#\\d{0,2}#.{0,8}#.{0,20}#.{0,20}";

    // поля из AccKey
    private static final int I_BRANCH   = 0;
    private static final int I_CCY      = 1;
    private static final int I_CUSTNO   = 2;
    private static final int I_ACCTYPE  = 3;
    private static final int I_CUSTTYPE = 4;
    private static final int I_TERM     = 5;
    private static final int I_GLSEQ    = 6;

    // поля из AddKey
    private static final int I_CBCCN    = 7;
    private static final int I_ACC2     = 8;
    private static final int I_PLCODE   = 9;
    private static final int I_ACOD     = 10;
    private static final int I_ACSEQ    = 11;
    private static final int I_DEALSRC  = 12;
    private static final int I_DEALID   = 13;
    private static final int I_SDEALID  = 14;

    // вычисляемые поля
    private static final int I_CCYN  = 15;
    private static final int I_FILIAL  = 16;
    private static final int I_ACID  = 17;
    private static final int I_RLNTYPE  = 18;
    private static final int I_PSAV = 19;

    private static final int I_DESC = 20;
    private static final int I_EXCCY = 21;

    private static final int ACCKEY_LENGTH = 22;

    private List<String> accKeyList;

    public AccountKeys(String accKey) {
        accKeyList = new ArrayList<>();
        accKeyList.addAll(Arrays.asList(accKey.split(FIELD_SEPARATOR)));
        for (int i = accKeyList.size(); i < ACCKEY_LENGTH; i++)
            accKeyList.add("");
    }

    public AccountKeys(AccountKeys keyList) {
        accKeyList = new ArrayList<>(keyList.accKeyList);
    }

    public static Pattern getPattern() {
        return Pattern.compile( patternAccountKeyMask.replace(MASK_SEPARATOR, FIELD_SEPARATOR) );
    }

    // --------- Поля ---------

    public String getBranch() {
        return getItem(I_BRANCH);
    }

    public void setBranch(String branch) {
        accKeyList.set(I_BRANCH, branch);
    }

    public String getCurrency() {
        return getItem(I_CCY);
    }

    public void setCurrency(String ccy) {
        accKeyList.set(I_CCY, ccy);
    }

    public String getCurrencyDigital() {
        return getItem(I_CCYN);
    }

    public void setCurrencyDigital(String ccyn) {
        accKeyList.set(I_CCYN, ccyn);
    }

    public String getCustomerNumber() {
        return getItem(I_CUSTNO);
    }

    public void setCustomerNumber(String customerNumber) {
        accKeyList.set(I_CUSTNO, customerNumber);
    }

    public String getCustomerType() {
        return getItem(I_CUSTTYPE);
    }

    public void setCustomerType(String customerType) {
        accKeyList.set(I_CUSTTYPE, customerType);
    }

    public String getAccountType() {
        return getItem(I_ACCTYPE);
    }

    public void setAccountType(String accountType) {
        accKeyList.set(I_ACCTYPE, accountType);
    }

    public String getTerm() {
        return getItem(I_TERM);
    }

    public void setTerm(String term) {
        accKeyList.set(I_TERM, term);
    }

    public String getGlSequence() {
        return getItem(I_GLSEQ);
    }

    public void setGlSequence(String glSequence) {
        accKeyList.set(I_GLSEQ, glSequence);
    }

    public String getCompanyCode() {
        return getItem(I_CBCCN);
    }

    public void setCompanyCode(String companyCodeNumeric) {
        accKeyList.set(I_CBCCN, companyCodeNumeric);
    }

    public String getFilial() {
        return getItem(I_FILIAL);
    }

    public void setFilial(String filial) {
        accKeyList.set(I_FILIAL, filial);
    }

    public String getAccount2() {
        return getItem(I_ACC2);
    }

    public void setAccount2(String acc2) {
        accKeyList.set(I_ACC2, acc2);
    }

    public String getPlCode() {
        return getItem(I_PLCODE);
    }

    public void setPlCode(String plCode) {
        accKeyList.set(I_PLCODE, plCode);
    }

    public String getAccountCode() {
        return getItem(I_ACOD);
    }

    public void setAccountCode(String accountCode) {
        accKeyList.set(I_ACOD, accountCode);
    }

    public String getAccSequence() {
        return getItem(I_ACSEQ);
    }

    public void setAccSequence(String accSeguence) {
        accKeyList.set(I_ACSEQ, accSeguence);
    }

    public String getDealSource() {
        return getItem(I_DEALSRC);
    }

    public void setDealSource(String dealSource) {
        accKeyList.set(I_DEALSRC, dealSource);
    }

    public String getDealId() {
        return getItem(I_DEALID);
    }

    public void setDealId(String dealId) {
        accKeyList.set(I_DEALID, dealId);
    }

    public String getSubDealId() {
        return getItem(I_SDEALID);
    }

    public void setSubDealId(String subdealId) {
        accKeyList.set(I_SDEALID, subdealId);
    }


    public String getAccountMidas() {
        return getItem(I_ACID);
    }

    public void setAccountMidas(String accountMidas) {
        accKeyList.set(I_ACID, accountMidas);
    }

    public String getRelationType() {
        return getItem(I_RLNTYPE);
    }

    public void setRelationType(String rlntype) {
        accKeyList.set(I_RLNTYPE, rlntype);
    }

    public String getPassiveActive() {
        return getItem(I_PSAV);
    }

    public void setPassiveActive(String passiveActive) {
        accKeyList.set(I_PSAV, passiveActive);
    }

    public String getDescription() {
        return getItem(I_DESC);
    }

    public void setDescription(String description) {
        accKeyList.set(I_DESC, description);
    }

    public String getExchangeCurrency() {
        return getItem(I_EXCCY);
    }

    public void setExchangeCurrency(String exccy) {
        accKeyList.set(I_EXCCY, exccy);
    }

    // -------- Индексы ----------

    public static int getiBranch() {
        return I_BRANCH;
    }

    public static int getiCurrency() {
        return I_CCY;
    }

    public static int getiCustomerNumber() {
        return I_CUSTNO;
    }

    public static int getiAccountType() {
        return I_ACCTYPE;
    }

    public static int getiCustomerType() {
        return I_CUSTTYPE;
    }

    public static int getiTerm() {
        return I_TERM;
    }

    public static int getiGlSequence() {
        return I_GLSEQ;
    }

    public static int getiCompanyCodeN() {
        return I_CBCCN;
    }

    public static int getiAccount2() {
        return I_ACC2;
    }

    public static int getiPlCode() {
        return I_PLCODE;
    }

    public static int getiAccountCode() {
        return I_ACOD;
    }

    public static int getiAccSequence() {
        return I_ACSEQ;
    }

    public static int getiDealSource() {
        return I_DEALSRC;
    }

    public static int getiDealId() {
        return I_DEALID;
    }

    public static int getiSubDeallId() {
        return I_SDEALID;
    }

    public static int getiAccountMidas() {
        return I_ACID;
    }

    public static int getiRelationType() { return I_RLNTYPE; }

    public static int getiPassiveActive() { return I_PSAV; }

    public static int getiDesccription() { return I_DESC; }

    public static int getiExchangeCurrency() { return I_EXCCY; }

    public static String getFieldName(int i) {
        return Long.toString(i);
    }

    private String getItem(int index) {
        return null != accKeyList.get(index) ? accKeyList.get(index).trim() : null;
    }

    public String toString() {
        return accKeyList.stream()
                .map(s -> ifEmpty(s, "")).collect(joining(rsubstr(FIELD_SEPARATOR, 1)));
    }

}
