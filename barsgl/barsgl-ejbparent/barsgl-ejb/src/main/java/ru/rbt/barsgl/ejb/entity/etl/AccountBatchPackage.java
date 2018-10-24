package ru.rbt.barsgl.ejb.entity.etl;

import ru.rbt.barsgl.ejb.controller.sm.StatefullObject;
import ru.rbt.barsgl.shared.enums.AccountBatchPackageState;
import ru.rbt.ejbcore.mapping.YesNo;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov on 16.10.2018.
 * Пакет для открытия счетов
 */
@Entity
@Table(name = "GL_ACBATPKG")
@SequenceGenerator(name = "AccountBatchPackageIdSeq", sequenceName = "SEQ_GL_ACBATPKG", allocationSize = 1)
public class AccountBatchPackage extends StatefullObject<AccountBatchPackageState, Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "AccountBatchPackageIdSeq")
    @Column(name = "ID_PKG")
    private Long id;

    @Column(name = "OD_LOAD")
    @Temporal(TemporalType.DATE)
    private Date operday;

    @Column(name = "TS_LOAD", insertable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date loadedDate;

    @Column(name = "TS_STARTV")
    @Temporal(TemporalType.TIMESTAMP)
    private Date validateStartDate;

    @Column(name = "TS_ENDV")
    @Temporal(TemporalType.TIMESTAMP)
    private Date validateEndDate;

    @Column(name = "TS_STARTP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date openStartDate;

    @Column(name = "TS_ENDP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date openEndDate;

    @Column(name = "STATE")
    @Enumerated(EnumType.STRING)
    private AccountBatchPackageState state;

    @Column(name = "USER_LOAD")
    private String loadUser;

    @Column(name = "USER_PROC")
    private String procUser;

    @Column(name = "FILE_NAME")
    private String fileName;

    @Column(name = "FILE_BODY", updatable = false)
    private byte[] fileBody;

    @Column(name = "CNT_REQ")
    private Long cntRequests;

    @Column(name = "CNT_ERR")
    private Long cntErrors;

    @Column(name = "CNT_FOUND")
    private Long cntFound;

    @Column(name = "INVISIBLE", insertable = false)
    @Enumerated(EnumType.STRING)
    private YesNo invisible;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Date getOperday() {
        return operday;
    }

    public void setOperday(Date operday) {
        this.operday = operday;
    }

    public Date getLoadedDate() {
        return loadedDate;
    }

    public void setLoadedDate(Date loadedDate) {
        this.loadedDate = loadedDate;
    }

    public Date getValidateStartDate() {
        return validateStartDate;
    }

    public void setValidateStartDate(Date validateStartDate) {
        this.validateStartDate = validateStartDate;
    }

    public Date getValidateEndDate() {
        return validateEndDate;
    }

    public void setValidateEndDate(Date validateEndDate) {
        this.validateEndDate = validateEndDate;
    }

    public Date getOpenStartDate() {
        return openStartDate;
    }

    public void setOpenStartDate(Date openStartDate) {
        this.openStartDate = openStartDate;
    }

    public Date getOpenEndDate() {
        return openEndDate;
    }

    public void setOpenEndDate(Date openEndDate) {
        this.openEndDate = openEndDate;
    }

    public AccountBatchPackageState getState() {
        return state;
    }

    public void setState(AccountBatchPackageState state) {
        this.state = state;
    }

    public String getLoadUser() {
        return loadUser;
    }

    public void setLoadUser(String loadUser) {
        this.loadUser = loadUser;
    }

    public String getProcUser() {
        return procUser;
    }

    public void setProcUser(String procUser) {
        this.procUser = procUser;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getFileBody() {
        return fileBody;
    }

    public void setFileBody(byte[] fileBody) {
        this.fileBody = fileBody;
    }

    public Long getCntRequests() {
        return cntRequests;
    }

    public void setCntRequests(Long cntRequests) {
        this.cntRequests = cntRequests;
    }

    public Long getCntErrors() {
        return cntErrors;
    }

    public void setCntErrors(Long cntErrors) {
        this.cntErrors = cntErrors;
    }

    public Long getCntFound() {
        return cntFound;
    }

    public void setCntFound(Long cntFound) {
        this.cntFound = cntFound;
    }

    public YesNo getInvisible() {
        return invisible;
    }

    public void setInvisible(YesNo invisible) {
        this.invisible = invisible;
    }


}
