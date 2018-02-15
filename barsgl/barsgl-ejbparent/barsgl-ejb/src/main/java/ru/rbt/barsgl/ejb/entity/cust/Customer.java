package ru.rbt.barsgl.ejb.entity.cust;

import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.ejbcore.mapping.YesNo;

import javax.persistence.*;

/**
 * Created by er18837 on 19.12.2017.
 */
@Entity
@Table(name = "SDCUSTPD")
public class Customer extends BaseEntity<String> {

    public enum ClientType {
        P("I"), B("B"), C("C");
        private String fcType;
        ClientType(String fcType) {
            this.fcType = fcType;
        }
    };

    public enum Resident {
        R("Y"), N("N");
        private String fcResident;
        Resident(String fcResident) {
            this.fcResident = fcResident;
        }
    };

    @Id
    @Column(name = "BBCUST")
    private String custNo;

    @Column(name = "BBBRCD")
    private String branch;

    @Column(name = "PRCD")        // P/C/B
    @Enumerated(EnumType.STRING)
    private ClientType clientType;

    @Column(name = "BXCTYP")
    private String cbType;

    @Column(name = "RECD")        // Y/N
    @Enumerated(EnumType.STRING)
    private Resident resident;

    @Column(name = "BBCNA1")
    private String nameEng;

    @Column(name = "BBCRNM")
    private String shortNameEng;

    @Column(name = "BXRUNM")
    private String nameRus;

    // ================ значения по умолчанию ================
    @Column(name = "BXBICC")
    private String bxbicc  = " ";

    @Column(name = "BXTPID")
    private String bxtpid  = " ";

    @Column(name = "BBCSSN")
    private String bbcssn  = "NEW";

    @Column(name = "BBCNA2")
    private String bbcna2  = " ";

    @Column(name = "BBCNA3")
    private String bbcna3  = " ";

    @Column(name = "BBCNA4")
    private String bbcna4  = " ";

    @Column(name = "BBCRTN")
    private String bbcrtn  = " ";

    @Column(name = "BBPAIN")
    private String bbpain  = " ";

    @Column(name = "BBCNCZ")
    private String bbcncz  = " ";

    @Column(name = "BBCOLC")
    private String bbcolc  = " ";

    @Column(name = "BBPCNB")
    private String bbpcnb  = " ";

    @Column(name = "BBBNBI")
    private String bbbnbi  = " ";

    @Column(name = "HOBICC")
    private String hobicc  = " ";

    @Column(name = "BBACOC")
    private String bbacoc  = " ";

    @Column(name = "OGRN")
    private String ogrn    = " ";

    @Column(name = "EXTCOD")
    private String extcod  = " ";

    @Column(name = "CONCOD")
    private String concod  = " ";

    @Column(name = "OWNTYP")
    private Integer owntyp = 0;

    @Column(name = "COMPNS")
    private String compns  = " ";

    @Column(name = "MRKTPL")
    private String mrktpl  = " ";

    @Column(name = "ISIBOD")
    private String isibod  = " ";

    @Column(name = "ISINSC")
    private String isinsc  = " ";

    @Column(name = "ISADV")
    private String isadv   = "N";

    @Column(name = "BXCTSG")
    private String bxctsg  = " ";

    @Override
    public String getId() {
        return custNo;
    }

    public String getCustNo() {
        return custNo;
    }

    public void setCustNo(String custNo) {
        this.custNo = custNo;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public ClientType getClientType() {
        return clientType;
    }

    public void setClientType(ClientType clientType) {
        this.clientType = clientType;
    }

    public String getCbType() {
        return cbType;
    }

    public void setCbType(String cbType) {
        this.cbType = cbType;
    }

    public Resident getResident() {
        return resident;
    }

    public void setResident(Resident resident) {
        this.resident = resident;
    }

    public String getNameEng() {
        return nameEng;
    }

    public void setNameEng(String nameEng) {
        this.nameEng = nameEng;
    }

    public String getShortNameEng() {
        return shortNameEng;
    }

    public void setShortNameEng(String shortNameEng) {
        this.shortNameEng = shortNameEng;
    }

    public String getNameRus() {
        return nameRus;
    }

    public void setNameRus(String nameRus) {
        this.nameRus = nameRus;
    }

    public Customer(String custNo) {
        this.custNo = custNo;
    }

    public Customer() {
    }
}
