package com.araguacaima.braas.core.google.wrapper;

import com.araguacaima.braas.core.google.model.StoredCredential;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

/**
 * Created by Alejandro on 02/01/2015.
 */
public class CredentialsWrapper {

    private static HttpTransport transport = new NetHttpTransport();
    private static Credential.AccessMethod method = BearerToken.authorizationHeaderAccessMethod();
    private static JacksonFactory jsonFactory = new JacksonFactory();
    private static HttpRequestInitializer requestInitializer;

    public static StoredCredential toStoredCredential(Credential credential, String userId, String userSecret) {

        StoredCredential storedCredential_ = new StoredCredential();

        storedCredential_.setUserId(userId);
        storedCredential_.setUserSecret(userSecret);
        if (credential != null) {
            storedCredential_.setAccessToken(credential.getAccessToken());
            storedCredential_.setRefreshToken(credential.getRefreshToken());
        }
        return storedCredential_;
    }

    public static Credential toCredential(StoredCredential storedCredential, GoogleClientSecrets clientSecrets) {
        Credential credential_ = null;
        if (storedCredential != null) {
            credential_ = newCredential(clientSecrets.getDetails().getClientId(),
                    clientSecrets.getDetails().getClientSecret());
            credential_.setAccessToken(storedCredential.getAccessToken());
            credential_.setRefreshToken(storedCredential.getRefreshToken());
        }
        return credential_;
    }

    private static Credential newCredential(String userId, String userSecret) {
        HttpExecuteInterceptor clientAuthentication = new ClientParametersAuthentication(userId, userSecret);
        Credential.Builder builder = new Credential.Builder(method).setTransport(transport)
                .setJsonFactory(jsonFactory)
                .setTokenServerEncodedUrl(GoogleOAuthConstants.TOKEN_SERVER_URL)
                .setClientAuthentication(clientAuthentication)
                .setRequestInitializer(requestInitializer);
        return builder.build();
    }
}
