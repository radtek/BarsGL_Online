package ru.rbt.shared.user;

import ru.rbt.shared.enums.UserExternalType;
import ru.rbt.shared.enums.UserLocked;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by ER18837 on 29.09.15.
 */
public class AppUserWrapper implements Serializable {

    private Long id;
    private String userName;
    private String userPassword;
    private UserLocked locked;
    private String surname;
    private String firstName;
    private String patronymic;
    private UserExternalType userType;
    private String filial;
    private String branch;  // TODO: deptId - Подразделение
    private String closeDateStr; //Date
    private String pwdMD5;
    private String errorListProcPermit;

    private ArrayList<String> grantedHeadBranches;
    private ArrayList<String> grantedSources;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public UserLocked getLocked() {
        return locked;
    }

    public void setLocked(UserLocked locked) {
        this.locked = locked;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getPatronymic() {
        return patronymic;
    }

    public void setPatronymic(String patronymic) {
        this.patronymic = patronymic;
    }

    public UserExternalType getUserType() {
        return userType;
    }

    public void setUserType(UserExternalType userType) {
        this.userType = userType;
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

    public String getPwdMD5() {
        return pwdMD5;
    }

    public void setPwdMD5(String pwdMD5) {
        this.pwdMD5 = pwdMD5;
    }

    public ArrayList<String> getGrantedHeadBranches() {
        return grantedHeadBranches;
    }

    public void setGrantedHeadBranches(ArrayList<String> grantedHeadBranches) {
        this.grantedHeadBranches = grantedHeadBranches;
    }

    public ArrayList<String> getGrantedSources() {
        return grantedSources;
    }

    public void setGrantedSources(ArrayList<String> grantedSources) {
        this.grantedSources = grantedSources;
    }

    public String getCloseDateStr() {
        return closeDateStr;
    }

    public void setCloseDateStr(String closeDateStr) {
        this.closeDateStr = closeDateStr;
    }

    public String getErrorListProcPermit() {
        return errorListProcPermit;
    }

    public void setErrorListProcPermit(String errorListProcPermit) {
        this.errorListProcPermit = errorListProcPermit;
    }
}
