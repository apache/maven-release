package org.apache.maven.shared.release.exec;

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

import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.ReleaseEnvironment;

import java.io.File;

/**
 * Execute Maven. May be implemented as a forked instance, or embedded.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface MavenExecutor
{
    /**
     * Plexus Role.
     */
    String ROLE = MavenExecutor.class.getName();

    /**
     * Execute goals using Maven.
     *
     * @param workingDirectory    the directory to execute in
     * @param goals               the goals to run (space delimited)
     * @param releaseEnvironment  the environmental settings, maven-home, etc used for this release
     * @param interactive         whether to execute in interactive mode, or the default batch mode
     * @param additionalArguments additional arguments to pass to the Maven command
     * @param pomFileName         the file name of the POM to execute on
     * @param result              holds all results of the execution
     * @throws MavenExecutorException if an error occurred executing Maven
     */
    void executeGoals( File workingDirectory, String goals, ReleaseEnvironment releaseEnvironment,
                       boolean interactive, String additionalArguments, String pomFileName, ReleaseResult result )
        throws MavenExecutorException;

    /**
     * Execute goals using Maven.
     *
     * @param workingDirectory    the directory to execute in
     * @param goals               the goals to run (space delimited)
     * @param releaseEnvironment  the environmental settings, maven-home, etc used for this release
     * @param interactive         whether to execute in interactive mode, or the default batch mode
     * @param additionalArguments additional arguments to pass to the Maven command
     * @param result              holds all results of the execution
     * @throws MavenExecutorException if an error occurred executing Maven
     */
    void executeGoals( File workingDirectory, String goals, ReleaseEnvironment releaseEnvironment,
                       boolean interactive, String additionalArguments, ReleaseResult result )
        throws MavenExecutorException;

    /**
     * Execute goals using Maven.
     *
     * @param workingDirectory    the directory to execute in
     * @param goals               the goals to run (space delimited)
     * @param interactive         whether to execute in interactive mode, or the default batch mode
     * @param additionalArguments additional arguments to pass to the Maven command
     * @param pomFileName         the file name of the POM to execute on
     * @param result              holds all results of the execution
     * @throws MavenExecutorException if an error occurred executing Maven
     *
     * @deprecated Use {@link MavenExecutor#executeGoals(File, String, ReleaseEnvironment, boolean, String, String, ReleaseResult)} instead
     */
    void executeGoals( File workingDirectory, String goals, boolean interactive, String additionalArguments,
                       String pomFileName, ReleaseResult result )
        throws MavenExecutorException;

    /**
     * Execute goals using Maven.
     *
     * @param workingDirectory    the directory to execute in
     * @param goals               the goals to run (space delimited)
     * @param interactive         whether to execute in interactive mode, or the default batch mode
     * @param additionalArguments additional arguments to pass to the Maven command
     * @param result              holds all results of the execution
     * @throws MavenExecutorException if an error occurred executing Maven
     *
     * @deprecated Use {@link MavenExecutor#executeGoals(File, String, ReleaseEnvironment, boolean, String, ReleaseResult)} instead
     */
    void executeGoals( File workingDirectory, String goals, boolean interactive, String additionalArguments,
                       ReleaseResult result )
        throws MavenExecutorException;
}
