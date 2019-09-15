package com.araguacaima.braas.core.drools.strategy;

import com.araguacaima.braas.core.drools.decorator.MavenArtifactDecorator;

import java.io.ByteArrayOutputStream;

/**
 * Created by Alejandro on 12/01/2015.
 */
public class MavenRepositoryDrlResourceStrategy implements ResourceStrategy {
    private String mavenLocalRepositoryPath;
    private String groupid;
    private String artifactid;
    private String version;

    public MavenRepositoryDrlResourceStrategy(String mavenLocalRepositoryPath,
                                              String groupid,
                                              String artifactid,
                                              String version) {

        this.mavenLocalRepositoryPath = mavenLocalRepositoryPath;
        this.groupid = groupid;
        this.artifactid = artifactid;
        this.version = version;
    }

    @Override
    public String buildUrl() {
        return MavenArtifactDecorator.decorateLocalRepositoryFullPath(mavenLocalRepositoryPath,
                groupid,
                artifactid,
                version);
    }

    @Override
    public ByteArrayOutputStream getStream() {
        return null;
    }
}
