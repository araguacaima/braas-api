package com.araguacaima.braas.core.google.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

public class Account implements Serializable {

    public static final String FIND_BY_EMAIL = "Account.findByEmail";
    public static final String FIND_BY_LOGIN = "Account.findByLogin";
    public static final String FIND_BY_EMAIL_OR_LOGIN = "Account.findByEmailOrLogin";
    public static final String GET_ACCOUNT_COUNT = "Account.getAccountCount";
    public static final String PARAM_EMAIL = "email";
    public static final String PARAM_LOGIN = "login";
    public static final String GET_ALL_ACCOUNTS = "Account.getAllAccounts";
    public static final String GET_ACCOUNTS_EMAIL_OR_LOGIN_STARTS_WITH =
            "Account.getAccountsWhoseLoginOrEmailStartsWith";
    public static final String FIND_BY_EMAIL_OR_LOGIN_NOT_FIRST_TIME = "Account.findByEmailOrLoginNotFirstTime";
    private static final long serialVersionUID = 1199840813066879040L;

    private String id;

    private String email;

    private String login;

    private String password;

    private boolean firstAccess = true;

    private Set<Role> roles;

    private boolean enabled = false;

    public Account() {
        this.id = UUID.randomUUID().toString();
    }

    public Account(String id, String login, String email, String password, Set<Role> roles) {
        this();
        this.login = login;
        this.id = id;
        this.email = email;
        this.password = password;
        this.roles = roles;
    }

    public Account(String login, String email, String password, Set<Role> roles) {
        this();
        this.login = login;
        this.email = email;
        this.password = password;
        this.roles = roles;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public boolean isFirstAccess() {
        return firstAccess;
    }

    public void setFirstAccess(boolean firstAccess) {
        this.firstAccess = firstAccess;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Account account = (Account) o;

        return new EqualsBuilder()
                .append(firstAccess, account.firstAccess)
                .append(id, account.id)
                .append(email, account.email)
                .append(login, account.login)
                .append(password, account.password)
                .append(roles, account.roles)
                .append(enabled, account.enabled)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(email)
                .append(login)
                .append(password)
                .append(firstAccess)
                .append(roles)
                .append(enabled)
                .toHashCode();
    }
}
