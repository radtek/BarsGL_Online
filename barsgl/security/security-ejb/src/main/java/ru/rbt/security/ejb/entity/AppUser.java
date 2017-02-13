package ru.rbt.security.ejb.entity;

import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;
import ru.rbt.barsgl.shared.enums.UserExternalType;
import ru.rbt.barsgl.shared.enums.UserLocked;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov
 * пароль "123" md5 "202cb962ac59075b964b07152d234b70"
 * insert into gl_user values (1, 'Admin', '202cb962ac59075b964b07152d234b70');
 */
@Entity
@Table(name = "GL_USER")
public class AppUser extends BaseEntity<Long> {

    @Id
    @Column(name = "ID_USER")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "USER_NAME")
    private String userName;

    /**
     * md5 хеш пароля
     */
    @Column(name = "USER_PWD")
    private String userPassword;

    @Column(name = "SURNAME")
    private String surname;

    @Column(name = "FIRSTNAME")
    private String firstName;

    @Column(name = "PATRONYMIC")
    private String patronymic;

    @Column(name = "FILIAL")
    private String filial;

    @Column(name = "DEPID")
    private String branch;      // Подразделение

    @Column(name = "END_DT")
    @Temporal(TemporalType.DATE)
    private Date end_Date;

    @Column(name = "LOCKED")
    @Enumerated(EnumType.ORDINAL)
    private UserLocked locked;

    @Column(name = "SEC_TYPE")
    @Enumerated(EnumType.ORDINAL)
    private UserExternalType userSecurityType;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getPatronymic() {
        return patronymic;
    }

    public void setPatronymic(String patronymic) {
        this.patronymic = patronymic;
    }

    public String getFilial() {
        return filial;
    }

    public void setFilial(String filial) {
        this.filial = filial;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Date getEnd_Date() {
        return end_Date;
    }

    public void setEnd_Date(Date end_Date) {
        this.end_Date = end_Date;
    }

    public UserLocked getLocked() {
        return locked;
    }

    public boolean isLocked() {
        return UserLocked.Y == locked;
    }

    public void setLocked(UserLocked locked) {
        this.locked = locked;
    }

    public UserExternalType getUserSecurityType() {
        return userSecurityType;
    }

    public boolean isLocal() {
        return UserExternalType.L == userSecurityType;
    }

    public void setUserSecurityType(UserExternalType userSecurityType) {
        this.userSecurityType = userSecurityType;
    }

    public boolean isExpired() {
        return (null != end_Date && end_Date.before(new Date()));
    }
}
