package org.apache.maven.shared.release.env;

import org.apache.maven.settings.Settings;

import java.io.File;

public class DefaultReleaseEnvironment
    implements ReleaseEnvironment
{

    private File mavenHome;

    private File javaHome;

    private File localRepositoryDirectory;

    private Settings settings;

    private String mavenExecutorId = DEFAULT_MAVEN_EXECUTOR_ID;

    public File getMavenHome()
    {
        return mavenHome;
    }

    public Settings getSettings()
    {
        return settings;
    }

    public ReleaseEnvironment setMavenHome( File mavenHome )
    {
        this.mavenHome = mavenHome;
        return this;
    }

    public ReleaseEnvironment setSettings( Settings settings )
    {
        this.settings = settings;
        return this;
    }

    public String getMavenExecutorId()
    {
        return mavenExecutorId;
    }

    public ReleaseEnvironment setMavenExecutorId( String mavenExecutorId )
    {
        this.mavenExecutorId = mavenExecutorId;
        return this;
    }

    public File getJavaHome()
    {
        return javaHome;
    }

    public ReleaseEnvironment setJavaHome( File javaHome )
    {
        this.javaHome = javaHome;
        return this;
    }

    public File getLocalRepositoryDirectory()
    {
        File localRepo = localRepositoryDirectory;

        if ( localRepo == null && settings != null && settings.getLocalRepository() != null )
        {
            localRepo = new File( settings.getLocalRepository() ).getAbsoluteFile();
        }

        return localRepo;
    }

    public ReleaseEnvironment setLocalRepositoryDirectory( File localRepositoryDirectory )
    {
        this.localRepositoryDirectory = localRepositoryDirectory;
        return this;
    }

}
