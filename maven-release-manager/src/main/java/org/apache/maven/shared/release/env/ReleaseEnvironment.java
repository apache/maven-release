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
    
    /**
     * 
     * @return the locale
     * @since 2.4
     */
    Locale getLocale();
    
    /**
     * @param locale
     * @return the locale
     * @since 2.4
     */
    ReleaseEnvironment setLocale( Locale locale );

}
