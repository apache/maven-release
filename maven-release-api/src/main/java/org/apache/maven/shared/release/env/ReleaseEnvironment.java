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
 * <p>ReleaseEnvironment interface.</p>
 *
 */
public interface ReleaseEnvironment {

    /** Constant <code>DEFAULT_MAVEN_EXECUTOR_ID="forked-path"</code> */
    String DEFAULT_MAVEN_EXECUTOR_ID = "forked-path";

    /**
     * <p>getMavenExecutorId.</p>
     *
     * @return a {@link java.lang.String} object
     */
    String getMavenExecutorId();

    /**
     * <p>getLocalRepositoryDirectory.</p>
     *
     * @return a {@link java.io.File} object
     */
    File getLocalRepositoryDirectory();

    /**
     * <p>getSettings.</p>
     *
     * @return a {@link org.apache.maven.settings.Settings} object
     */
    Settings getSettings();

    /**
     * <p>getMavenHome.</p>
     *
     * @return a {@link java.io.File} object
     */
    File getMavenHome();

    /**
     * <p>getJavaHome.</p>
     *
     * @return a {@link java.io.File} object
     */
    File getJavaHome();
}
