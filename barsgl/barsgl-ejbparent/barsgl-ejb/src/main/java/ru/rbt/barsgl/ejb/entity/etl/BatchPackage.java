package ru.rbt.barsgl.ejb.entity.etl;

import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by ER18837 on 26.02.16.
 */
@Entity
@Table(name = "GL_BATPKG")
public class BatchPackage extends BaseEntity<Long> {

    public enum PackageState {
        INPROGRESS, LOADED, PROCESSED, ERROR, WORKING, DELETED, ON_CONTROL, ON_WAITDATE, IS_SIGNED, IS_SIGNEDDATE, WAITPROC
    }

    @Id
    @Column(name = "ID_PKG")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DT_LOAD")
    private Date dateLoad;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATE")
    private PackageState packageState;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DT_PRC")
    private Date processDate;

    @Column(name = "USER_NAME")
    private String userName;

    @Column(name = "FILE_NAME")
    private String fileName;


    @Column(name = "CNT_PST")
    private Integer postingCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "MVMNT_OFF")
    private YesNo movementOff;               // 1

    @Transient
    private Integer errorCount;

    // TODO BLOB ?
//    @Column(name = "FILE")
//    private String file;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
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

    public Date getProcessDate() {
        return processDate;
    }

    public void setProcessDate(Date processDate) {
        this.processDate = processDate;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getPostingCount() {
        return postingCount;
    }

    public void setPostingCount(Integer postingCount) {
        this.postingCount = postingCount;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }

    public YesNo getMovementOff() {
        return movementOff;
    }

    public void setMovementOff(YesNo movementOff) {
        this.movementOff = movementOff;
    }
}
