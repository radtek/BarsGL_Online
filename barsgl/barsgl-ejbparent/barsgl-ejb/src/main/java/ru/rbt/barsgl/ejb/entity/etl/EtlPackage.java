package ru.rbt.barsgl.ejb.entity.etl;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov
 */
@Entity
@Table(name = "GL_ETLPKG")
public class EtlPackage extends BaseEntity<Long>{

    public enum PackageState {
        INPROGRESS, LOADED, PROCESSED, ERROR, WORKING, BUILD
    }

    @Id
    @Column(name = "ID_PKG")
    private Long id;

    @Column(name = "PKG_NAME")
    private String packageName;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DT_LOAD")
    private Date dateLoad;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATE")
    private PackageState packageState;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "ACC_CNT")
    private Integer accountCnt;

    @Column(name = "PST_CNT")
    private Integer postingCnt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DT_PRC")
    private Date processDate;

    @Column(name = "MESSAGE")
    private String message;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Date getDateLoad() {
        return dateLoad;
    }

    public void setDateLoad(Date dateLoad) {
        this.dateLoad = dateLoad;
    }

    public PackageState getPackageState() {
        return packageState;
    }

    public void setPackageState(PackageState packageState) {
        this.packageState = packageState;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getAccountCnt() {
        return accountCnt;
    }

    public void setAccountCnt(Integer accountCnt) {
        this.accountCnt = accountCnt;
    }

    public Integer getPostingCnt() {
        return postingCnt;
    }

    public void setPostingCnt(Integer postingCnt) {
        this.postingCnt = postingCnt;
    }

    public Date getProcessDate() {
        return processDate;
    }

    public void setProcessDate(Date processDate) {
        this.processDate = processDate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
