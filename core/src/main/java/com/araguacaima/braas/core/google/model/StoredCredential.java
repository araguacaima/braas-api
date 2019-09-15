package com.araguacaima.braas.core.google.model;


import java.io.Serializable;

public class StoredCredential implements Serializable {

    public static final String FIND_BY_USER_ID = "Credential.findByUserId";
    public static final String FIND_BY_ACCOUNT = "Credential.findByAccount";
    public static final String GET_CREDENTIALS_COUNT = "Credential.getCredentialCount";
    public static final String PARAM_USER_ID = "userId";
    public static final String PARAM_ACCOUNT_EMAIL = "email";
    public static final String GET_ALL_CREDENTIALS = "Credential.getAllCredentials";
    private static final long serialVersionUID = 4205048334639722704L;

    private String id;

    /**
     * Id of the current granted user
     */

    private String userId;

    /**
     * Access token issued by the authorization server.
     */
    private String accessToken;

    /**
     * Refresh token which can be used to obtain new access tokens using the same authorization grant
     * or {@code null} for none.
     */

    private String refreshToken;


    private Account account;

    /**
     * secret of the current granted user
     */

    private String userSecret;

    public StoredCredential() {
    }


    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getUserSecret() {
        return userSecret;
    }

    public void setUserSecret(String userSecret) {
        this.userSecret = userSecret;
    }
}



