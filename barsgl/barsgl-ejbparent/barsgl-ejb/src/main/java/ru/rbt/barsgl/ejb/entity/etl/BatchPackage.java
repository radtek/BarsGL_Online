package ru.rbt.barsgl.ejb.entity.etl;

import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.shared.enums.BatchPackageState;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by ER18837 on 26.02.16.
 */
@Entity
@Table(name = "GL_BATPKG")
@SequenceGenerator(name = "BatchPackageIdSeq", sequenceName = "GL_BATPKG_SEQ", allocationSize = 1)
public class BatchPackage extends BaseEntity<Long> {

    @Id
    @Column(name = "ID_PKG")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "BatchPackageIdSeq")
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DT_LOAD")
    private Date dateLoad;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATE")
    private BatchPackageState packageState;

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

    @Temporal(TemporalType.DATE)
    @Column(name = "POSTDATE")
    private Date postDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "PROCDATE")
    private Date procDate;

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

    public BatchPackageState getPackageState() {
        return packageState;
    }

    public void setPackageState(BatchPackageState packageState) {
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

    public Date getPostDate() {
        return postDate;
    }

    public void setPostDate(Date postDate) {
        this.postDate = postDate;
    }

    public Date getProcDate() {
        return procDate;
    }

    public void setProcDate(Date procDate) {
        this.procDate = procDate;
    }
}
