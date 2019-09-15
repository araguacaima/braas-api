package com.araguacaima.braas.core.drools.decorator;

/**
 * Created by Alejandro on 28/11/2014.
 */
public class MavenArtifactDecorator {

    public static String decorateFullPath(String appName, String groupId, String artifactId, String version) {
        return "/" + appName + "/maven2" + decorateArtifactWithFullGroupPath(groupId, artifactId, version);
    }

    public static String decorateArtifactWithFullGroupPath(String groupId, String artifactId, String version) {
        return "/" + groupId.replaceAll("\\.", "/") + "/" + artifactId + "/" + version + decorateArtifact(artifactId,
                version);
    }

    public static String decorateArtifact(String artifactId, String version) {
        return "/" + artifactId + "-" + version + ".jar";
    }

    public static String decorateLocalRepositoryFullPath(String mavenLocalRepositoryPath,
                                                         String groupId,
                                                         String artifactId,
                                                         String version) {
        return "file:///" + mavenLocalRepositoryPath + decorateLocalRepositoryArtifactWithFullGroupPath(groupId,
                artifactId,
                version);
    }

    public static String decorateLocalRepositoryArtifactWithFullGroupPath(String groupId,
                                                                          String artifactId,
                                                                          String version) {
        return "/"
                + groupId.replaceAll("\\.", "/")
                + "/"
                + artifactId
                + "/"
                + version
                + decorateLocalRepositoryArtifact(artifactId, version);
    }

    public static String decorateLocalRepositoryArtifact(String artifactId, String version) {
        return "/" + artifactId + "-" + version + ".jar";
    }

    public static String decorateAbsolutePath(String rulesPath, String artifactName) {
        final String s = rulesPath.replaceAll("\\\\", "/");
        return "file://" + (s.startsWith("/") ? "" : "/") + s + "/" + artifactName;
    }

    public static String decorateFullArtifactId(String groupId,
                                                String artifactId,
                                                String version) {
        return groupId
                + "-"
                + artifactId
                + "-"
                + version;
    }

}
