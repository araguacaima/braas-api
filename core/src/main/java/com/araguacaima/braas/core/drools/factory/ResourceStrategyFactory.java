package com.araguacaima.braas.core.drools.factory;

import com.araguacaima.braas.core.Constants;
import com.araguacaima.braas.core.drools.DroolsConfig;
import com.araguacaima.braas.core.drools.strategy.*;
import com.araguacaima.braas.core.google.GoogleDriveUtils;
import org.apache.commons.io.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

/**
 * Created by Alejandro on 12/01/2015.
 */
public class ResourceStrategyFactory {

    public static ResourceStrategy getUrlResourceStrategy(DroolsConfig droolsConfig) {
        String protocol;
        String server;
        String port;
        String appName;
        String groupid;
        String artifactid;
        String version;
        String mavenLocalRepositoryPath;
        String rulesPath;
        String decisionTablePath;
        String artifactName;

        Constants.URL_RESOURCE_STRATEGIES urlResourceStrategy = Constants.URL_RESOURCE_STRATEGIES.valueOf
                (droolsConfig.getUrlResourceStrategy());
        switch (urlResourceStrategy) {
            case WORKBENCH:
                protocol = droolsConfig.getProtocol();
                server = droolsConfig.getServer();
                port = droolsConfig.getPort();
                appName = droolsConfig.getAppName();
                groupid = droolsConfig.getGroupid();
                artifactid = droolsConfig.getArtifactid();
                version = droolsConfig.getVersion();
                return new WorkbenchRepositoryResourceStrategy(protocol,
                        server,
                        port,
                        appName,
                        groupid,
                        artifactid,
                        version);
            case MAVEN:
                groupid = droolsConfig.getGroupid();
                artifactid = droolsConfig.getArtifactid();
                version = droolsConfig.getVersion();
                mavenLocalRepositoryPath = droolsConfig.getMavenLocalRepositoryPath();
                return new MavenRepositoryDrlResourceStrategy(mavenLocalRepositoryPath,
                        groupid,
                        artifactid,
                        version);
            case ABSOLUTE_DRL_PATH:
                rulesPath = droolsConfig.getRulesPath();
                artifactName = droolsConfig.getArtifactName();
                return new AbsolutePathDrlResourceStrategy(rulesPath, artifactName);
            case ABSOLUTE_DECISION_TABLE_PATH:
                decisionTablePath = droolsConfig.getDecisionTablePath();
                String file1;
                try {
                    if (new File(decisionTablePath).exists()) {
                        file1 = decisionTablePath;
                    } else {
                        URL url = ResourceStrategyFactory.class.getResource(decisionTablePath);
                        file1 = url.getFile();
                    }
                    return new AbsolutePathDecisionTableResourceStrategy(new File(file1));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            case GOOGLE_DRIVE_DECISION_TABLE_PATH:
                rulesPath = droolsConfig.getRulesPath();
                try {
                    InputStream credentialsStream = droolsConfig.getCredentialsStream();
                    if (credentialsStream == null) {
                        String path = droolsConfig.getCredentialsPath();
                        if (path != null) {
                            credentialsStream = FileUtils.openInputStream(new File(path));
                        }
                    }
                    ByteArrayOutputStream excelStream = GoogleDriveUtils.getSpreadsheet(rulesPath, credentialsStream, droolsConfig.getCredentialStrategy());
                    return new StreamDecisionTableResourceStrategy(excelStream);
                } catch (Throwable t) {
                    t.printStackTrace();
                    return null;
                }
            default:
                throw new IllegalArgumentException();
        }
    }
}