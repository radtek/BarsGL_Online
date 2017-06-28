package ru.rbt.barsgl.ejb.integr.struct;

import ru.rbt.barsgl.shared.enums.MovementErrorTypes;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by ER22228 on 31.05.2016.
 */
public class MovementCreateData implements Serializable{
    String operIdC;          //ID_CR;       //	P1	ID операции
    String accountCBC;       //AC_CR;    //	P2	Счет ЦБ по дебету операции
    BigDecimal operAmountC;  //AMT_CR;  //  P3	сумма по дебету операции
    String operIdD;          //ID_DR;       //	P1	ID операции
    String accountCBD;       //AC_DR;    //	P2	Счет ЦБ по дебету операции
    BigDecimal operAmountD;  //AMT_DR;  //  P3	сумма по дебету операции
    Date pstDate;           //VDATE;         //	P4	дата проводки
    String pstSource;       //SRC_PST;     //	P5	источник сделки
    String dealId;          //DEAL_ID;     //	P6	номер сделки
    Date operCreate;        //OTS;           //	P7	дата и время создания операции
    String destinationR;    //RNRTL;       //	P8	назначение русское
    String direction;       //DIRECT;      //	P9	направление движения по счету (пока только по дебету = ‘D’)
    String deptId;          //DEPT_ID;     //	P10	код департамента
    String profitCenter;    //PRFCNTR;     //	P11	профит центр
    Boolean isCorrectionPst;//FCHNG;      //	P12	Признак исправительной проводки
    String pnar;                          //    P13

    // Ответы
    StateEnum  state;       //STATE;    //	Статус
    MovementErrorTypes errType;     //ERRTYPE;  // Тип ошибки
    String errDescr;        //ERRDESCR;     //	Описание ошибки

    // Транспортные данные
    String envelopOutcoming;
    String messageUUID;
    String blockId;

    //Информация по деталям платежа
    private PaymentDetails creditPaymentDetails;
    private PaymentDetails debitPaymentDetails;
    
    public enum StateEnum{
        SUCCESS, ERROR, WARNING, SENT
    }

    public String getOperIdD() {
        return operIdD;
    }

    public void setOperIdD(String operIdD) {
        this.operIdD = operIdD;
    }

    public String getAccountCBD() {
        return accountCBD;
    }

    public void setAccountCBD(String accountCBD) {
        this.accountCBD = accountCBD;
    }

    public BigDecimal getOperAmountD() {
        return operAmountD;
    }

    public void setOperAmountD(BigDecimal operAmountD) {
        this.operAmountD = operAmountD;
    }

    public Date getPstDate() {
        return pstDate;
    }

    public void setPstDate(Date pstDate) {
        this.pstDate = pstDate;
    }

    public String getPstSource() {
        return pstSource;
    }

    public void setPstSource(String pstSource) {
        this.pstSource = pstSource;
    }

    public String getDealId() {
        return dealId;
    }

    public void setDealId(String dealId) {
        this.dealId = dealId;
    }

    public Date getOperCreate() {
        return operCreate;
    }

    public void setOperCreate(Date operCreate) {
        this.operCreate = operCreate;
    }

    public String getDestinationR() {
        return destinationR;
    }

    public void setDestinationR(String destinationR) {
        this.destinationR = destinationR;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public String getProfitCenter() {
        return profitCenter;
    }

    public void setProfitCenter(String profitCenter) {
        this.profitCenter = profitCenter;
    }

    public Boolean getCorrectionPst() {
        return isCorrectionPst;
    }

    public void setCorrectionPst(Boolean correctionPst) {
        isCorrectionPst = correctionPst;
    }

    public StateEnum getState() {
        return state;
    }

    public void setState(StateEnum state) {
        this.state = state;
    }

    public MovementErrorTypes getErrType() {
        return errType;
    }

    public void setErrType(MovementErrorTypes errType) {
        this.errType = errType;
    }

    public String getErrDescr() {
        return errDescr;
    }

    public void setErrDescr(String errDescr) {
        this.errDescr = errDescr;
    }

    public String getPnar() {
        return pnar;
    }

    public void setPnar(String pnar) {
        this.pnar = pnar;
    }

    public String getEnvelopOutcoming() {
        return envelopOutcoming;
    }

    public void setEnvelopOutcoming(String envelopOutcoming) {
        this.envelopOutcoming = envelopOutcoming;
    }

    public String getMessageUUID() {
        return messageUUID;
    }

    public void setMessageUUID(String messageUUID) {
        this.messageUUID = messageUUID;
    }

    public String getBlockId() {
        return blockId;
    }

    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }


    public BigDecimal getOperAmountC() {
        return operAmountC;
    }

    public void setOperAmountC(BigDecimal operAmountC) {
        this.operAmountC = operAmountC;
    }

    public String getOperIdC() {
        return operIdC;
    }

    public void setOperIdC(String operIdC) {
        this.operIdC = operIdC;
    }

    public String getAccountCBC() {
        return accountCBC;
    }

    public void setAccountCBC(String accountCBC) {
        this.accountCBC = accountCBC;
    }

    public PaymentDetails getCreditPaymentDetails() {
        return creditPaymentDetails;
    }

    public void setCreditPaymentDetails(PaymentDetails creditPaymentDetails) {
        this.creditPaymentDetails = creditPaymentDetails;
    }

    public PaymentDetails getDebitPaymentDetails() {
        return debitPaymentDetails;
    }

    public void setDebitPaymentDetails(PaymentDetails debitPaymentDetails) {
        this.debitPaymentDetails = debitPaymentDetails;
    }

    @Override
    public String toString() {
        return "MovementCreateData{" +
                   "operIdC='" + operIdC + '\'' +
                   ", accountCBC='" + accountCBC + '\'' +
                   ", operAmountC=" + operAmountC +
                   ", operIdD='" + operIdD + '\'' +
                   ", accountCBD='" + accountCBD + '\'' +
                   ", operAmountD=" + operAmountD +
                   ", pstDate=" + pstDate +
                   ", pstSource='" + pstSource + '\'' +
                   ", dealId='" + dealId + '\'' +
                   ", operCreate=" + operCreate +
                   ", destinationR='" + destinationR + '\'' +
                   ", direction='" + direction + '\'' +
                   ", deptId='" + deptId + '\'' +
                   ", profitCenter='" + profitCenter + '\'' +
                   ", isCorrectionPst=" + isCorrectionPst +
                   ", pnar='" + pnar + '\'' +
                   ", state=" + state +
                   ", errType=" + errType +
                   ", errDescr='" + errDescr + '\'' +
                   ", envelopOutcoming='" + envelopOutcoming + '\'' +
                   ", messageUUID='" + messageUUID + '\'' +
                   ", blockId='" + blockId + '\'' +
                   '}';
    }
}



