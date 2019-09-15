package com.araguacaima.braas.core.drools.strategy;

import com.araguacaima.braas.core.drools.decorator.MavenArtifactDecorator;

import java.io.ByteArrayOutputStream;

/**
 * Created by Alejandro on 12/01/2015.
 */
public class AbsolutePathDrlResourceStrategy implements ResourceStrategy {

    String rulesPath;
    String artifactName;

    public AbsolutePathDrlResourceStrategy(String rulesPath, String artifactName) {

        this.rulesPath = rulesPath;
        this.artifactName = artifactName;
    }

    @Override
    public String buildUrl() {
        return MavenArtifactDecorator.decorateAbsolutePath(rulesPath, artifactName);
    }

    @Override
    public ByteArrayOutputStream getStream() {
        return null;
    }
}
