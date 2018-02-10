package org.apache.maven.shared.release.env;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.Locale;

import org.apache.maven.settings.Settings;

/**
 *
 */
public class DefaultReleaseEnvironment
    implements ReleaseEnvironment
{

    private File mavenHome;

    private File javaHome;

    private File localRepositoryDirectory;

    private Settings settings;

    private String mavenExecutorId = DEFAULT_MAVEN_EXECUTOR_ID;

    private Locale locale = Locale.ENGLISH;

    @Override
    public File getMavenHome()
    {
        return mavenHome;
    }

    @Override
    public Settings getSettings()
    {
        return settings;
    }

    public DefaultReleaseEnvironment setMavenHome( File mavenHome )
    {
        this.mavenHome = mavenHome;
        return this;
    }

    public DefaultReleaseEnvironment setSettings( Settings settings )
    {
        this.settings = settings;
        return this;
    }

    @Override
    public String getMavenExecutorId()
    {
        return mavenExecutorId;
    }

    public DefaultReleaseEnvironment setMavenExecutorId( String mavenExecutorId )
    {
        this.mavenExecutorId = mavenExecutorId;
        return this;
    }

    @Override
    public File getJavaHome()
    {
        return javaHome;
    }

    public DefaultReleaseEnvironment setJavaHome( File javaHome )
    {
        this.javaHome = javaHome;
        return this;
    }

    @Override
    public File getLocalRepositoryDirectory()
    {
        File localRepo = localRepositoryDirectory;

        if ( localRepo == null && settings != null && settings.getLocalRepository() != null )
        {
            localRepo = new File( settings.getLocalRepository() ).getAbsoluteFile();
        }

        return localRepo;
    }

    public DefaultReleaseEnvironment setLocalRepositoryDirectory( File localRepositoryDirectory )
    {
        this.localRepositoryDirectory = localRepositoryDirectory;
        return this;
    }

    @Override
    public Locale getLocale()
    {
        return locale;
    }

    public DefaultReleaseEnvironment setLocale( Locale locale )
    {
        this.locale = locale;
        return this;
    }
}
