package org.apache.maven.shared.release.env;

import org.apache.maven.settings.Settings;

import java.io.File;

public interface ReleaseEnvironment
{

    String DEFAULT_MAVEN_EXECUTOR_ID = "forked-path";

    String getMavenExecutorId();

    ReleaseEnvironment setMavenExecutorId( String mavenExecutorId );

    File getLocalRepositoryDirectory();

    ReleaseEnvironment setLocalRepositoryDirectory( File localRepositoryDirectory );

    Settings getSettings();

    ReleaseEnvironment setSettings( Settings settings );

    File getMavenHome();

    ReleaseEnvironment setMavenHome( File mavenHome );

    File getJavaHome();

    ReleaseEnvironment setJavaHome( File javaHome );

}
