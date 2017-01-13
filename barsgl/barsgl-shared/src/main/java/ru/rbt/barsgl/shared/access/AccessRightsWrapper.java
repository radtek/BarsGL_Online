package ru.rbt.barsgl.shared.access;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by akichigi on 20.04.16.
 */
public class AccessRightsWrapper implements Serializable, IsSerializable {
    private Integer userId;
    private String login;
    private String surname;
    private String firstName;
    private String patronymic;
    private String filial;
    private ArrayList<UserBranchesWrapper> branches;
    private ArrayList<UserBranchesWrapper> granted_branches;
    private ArrayList<RoleWrapper> roles;
    private ArrayList<RoleWrapper> granted_roles;
    private ArrayList<UserProductsWrapper> products;
    private ArrayList<UserProductsWrapper> granted_products;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public ArrayList<UserBranchesWrapper> getBranches() {
        return branches;
    }

    public void setBranches(ArrayList<UserBranchesWrapper> branches) {
        this.branches = branches;
    }

    public ArrayList<RoleWrapper> getRoles() {
        return roles;
    }

    public void setRoles(ArrayList<RoleWrapper> roles) {
        this.roles = roles;
    }

    public ArrayList<UserBranchesWrapper> getGranted_branches() {
        return granted_branches;
    }

    public void setGranted_branches(ArrayList<UserBranchesWrapper> granted_branches) {
        this.granted_branches = granted_branches;
    }

    public ArrayList<RoleWrapper> getGranted_roles() {
        return granted_roles;
    }

    public void setGranted_roles(ArrayList<RoleWrapper> granted_roles) {
        this.granted_roles = granted_roles;
    }

    public ArrayList<UserProductsWrapper> getProducts() {
        return products;
    }

    public void setProducts(ArrayList<UserProductsWrapper> products) {
        this.products = products;
    }

    public ArrayList<UserProductsWrapper> getGranted_products() {
        return granted_products;
    }

    public void setGranted_products(ArrayList<UserProductsWrapper> granted_products) {
        this.granted_products = granted_products;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
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

    public String getFilial() {
        return filial;
    }

    public void setFilial(String filial) {
        this.filial = filial;
    }
}
