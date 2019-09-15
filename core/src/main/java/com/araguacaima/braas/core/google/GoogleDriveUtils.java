package com.araguacaima.braas.core.google;

import com.araguacaima.braas.core.Constants;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class GoogleDriveUtils {
    private static final String APPLICATION_NAME = "BRaaS";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final Logger log = LoggerFactory.getLogger(GoogleDriveUtils.class);

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);

    /**
     * Creates an authorized Credential object for web applications, from a service account.
     *
     * @param credentials Stream that contains a JSON or P12 object with user credentials info
     * @return An authorized Credential object.
     * @throws IOException If the credentials stream is invalid.
     */
    private static Credential getCredentialsWeb(InputStream credentials) throws Exception {
        return GoogleCredential.fromStream(credentials).createScoped(SCOPES);
    }

    /**
     * @param fileId                        Identifier of the file to retrieve
     * @param credentials                   Stream that contains a json object with user credentials info
     * @param googleDriveCredentialStrategy
     * @return A stream with the requested file
     * @throws GeneralSecurityException If there is any security issue trying tio use the provided credentials
     * @throws IOException              If the credentials stream is invalid.
     */
    public static ByteArrayOutputStream getSpreadsheet(String fileId, InputStream credentials, String googleDriveCredentialStrategy) throws Exception {
        return getFile(fileId, GoogleDocumentsMime.Types.SPREADSHEET.value(), credentials, googleDriveCredentialStrategy);
    }

    /**
     * @param fileId                        Identifier of the file to retrieve
     * @param mime                          The mime type for a Google Document, when it's not binary
     * @param credentials                   Stream that contains a json object with user credentials info
     * @param googleDriveCredentialStrategy
     * @return A stream with the requested file
     * @throws GeneralSecurityException If there is any security issue trying tio use the provided credentials
     * @throws IOException              If the credentials stream is invalid.
     */
    public static ByteArrayOutputStream getFile(String fileId, String mime, InputStream credentials, String googleDriveCredentialStrategy) throws Exception {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Credential credential;
        Constants.CREDENTIALS_ORIGIN_STRATEGIES credentialsStrategy = null;
        try {
            credentialsStrategy = Constants.CREDENTIALS_ORIGIN_STRATEGIES.valueOf(googleDriveCredentialStrategy);
        } catch (Throwable ignored) {
        }
        if (Constants.CREDENTIALS_ORIGIN_STRATEGIES.SERVER.equals(credentialsStrategy)) {
            credential = getCredentialsWeb(credentials);
        } else {
            throw new Exception("Not other method than SERVER is implemented");
            // Incompatible. Google local authentication method uses javax.servlet-api-2.5 with is not allowed in modern servers
        }
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
        try {
            service.files().export(fileId, mime).executeMediaAndDownloadTo(outputStream);
        } catch (Throwable t) {
            log.info("Error trying to retrieve file as a Google document. Trying as binary instead");
            Drive.Files.Get get = service.files().get(fileId);
            get.executeMediaAndDownloadTo(outputStream);
        }
        return outputStream;
    }
}