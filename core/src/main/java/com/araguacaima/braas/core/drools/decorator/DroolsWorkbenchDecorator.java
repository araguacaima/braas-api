package com.araguacaima.braas.core.drools.decorator;

/**
 * Created by Alejandro on 28/11/2014.
 */
public class DroolsWorkbenchDecorator {

    public static String decorate(String protocol,
                                  String server,
                                  String port,
                                  String appName,
                                  String groupid,
                                  String artifactid,
                                  String version) {
        return protocol + "://" + server + ":" + port + MavenArtifactDecorator.decorateFullPath(appName,
                groupid,
                artifactid,
                version);
    }
}
