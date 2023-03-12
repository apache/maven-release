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
package org.apache.maven.shared.release.env;

import java.io.File;

import org.apache.maven.settings.Settings;

/**
 * <p>DefaultReleaseEnvironment class.</p>
 */
public class DefaultReleaseEnvironment implements ReleaseEnvironment {
    private File mavenHome;

    private File javaHome;

    private File localRepositoryDirectory;

    private Settings settings;

    private String mavenExecutorId = DEFAULT_MAVEN_EXECUTOR_ID;

    @Override
    public File getMavenHome() {
        return mavenHome;
    }

    @Override
    public Settings getSettings() {
        return settings;
    }

    /**
     * <p>Setter for the field <code>mavenHome</code>.</p>
     *
     * @param mavenHome a {@link java.io.File} object
     * @return a {@link org.apache.maven.shared.release.env.DefaultReleaseEnvironment} object
     */
    public DefaultReleaseEnvironment setMavenHome(File mavenHome) {
        this.mavenHome = mavenHome;
        return this;
    }

    /**
     * <p>Setter for the field <code>settings</code>.</p>
     *
     * @param settings a {@link org.apache.maven.settings.Settings} object
     * @return a {@link org.apache.maven.shared.release.env.DefaultReleaseEnvironment} object
     */
    public DefaultReleaseEnvironment setSettings(Settings settings) {
        this.settings = settings;
        return this;
    }

    @Override
    public String getMavenExecutorId() {
        return mavenExecutorId;
    }

    /**
     * <p>Setter for the field <code>mavenExecutorId</code>.</p>
     *
     * @param mavenExecutorId a {@link java.lang.String} object
     * @return a {@link org.apache.maven.shared.release.env.DefaultReleaseEnvironment} object
     */
    public DefaultReleaseEnvironment setMavenExecutorId(String mavenExecutorId) {
        this.mavenExecutorId = mavenExecutorId;
        return this;
    }

    @Override
    public File getJavaHome() {
        return javaHome;
    }

    /**
     * <p>Setter for the field <code>javaHome</code>.</p>
     *
     * @param javaHome a {@link java.io.File} object
     * @return a {@link org.apache.maven.shared.release.env.DefaultReleaseEnvironment} object
     */
    public DefaultReleaseEnvironment setJavaHome(File javaHome) {
        this.javaHome = javaHome;
        return this;
    }

    @Override
    public File getLocalRepositoryDirectory() {
        File localRepo = localRepositoryDirectory;

        if (localRepo == null && settings != null && settings.getLocalRepository() != null) {
            localRepo = new File(settings.getLocalRepository()).getAbsoluteFile();
        }

        return localRepo;
    }

    /**
     * <p>Setter for the field <code>localRepositoryDirectory</code>.</p>
     *
     * @param localRepositoryDirectory a {@link java.io.File} object
     * @return a {@link org.apache.maven.shared.release.env.DefaultReleaseEnvironment} object
     */
    public DefaultReleaseEnvironment setLocalRepositoryDirectory(File localRepositoryDirectory) {
        this.localRepositoryDirectory = localRepositoryDirectory;
        return this;
    }
}
