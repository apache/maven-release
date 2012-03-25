package org.apache.maven.shared.release;

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

import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;

import java.util.List;

/**
 * Release management classes.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface ReleaseManager
{
    /**
     * The Plexus role.
     */
    String ROLE = ReleaseManager.class.getName();

    /**
     * Prepare a release.
     *
     * @param releaseDescriptor the configuration to pass to the preparation steps
     * @param releaseEnvironment settings, maven-home, java-home, etc. to use during release.
     * @param reactorProjects   the reactor projects
     * @throws ReleaseExecutionException if there is a problem performing the release
     * @throws ReleaseFailureException   if there is a problem performing the release
     */
    void prepare( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                  List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Prepare a release.
     *
     * @param releaseDescriptor the configuration to pass to the preparation steps
     * @param settings          the settings.xml configuration
     * @param reactorProjects   the reactor projects
     * @throws ReleaseExecutionException if there is a problem performing the release
     * @throws ReleaseFailureException   if there is a problem performing the release
     *
     * @deprecated Use {@link ReleaseManager#prepare(ReleaseDescriptor, ReleaseEnvironment, List)} instead.
     */
    void prepare( ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Prepare a release.
     *
     * @param releaseDescriptor the configuration to pass to the preparation steps
     * @param releaseEnvironment settings, maven-home, java-home, etc. to use during release.
     * @param reactorProjects   the reactor projects
     * @param resume            resume a previous release, if the properties file exists
     * @param dryRun            do not commit any changes to the file system or SCM
     * @throws ReleaseExecutionException if there is a problem performing the release
     * @throws ReleaseFailureException   if there is a problem performing the release
     */
    void prepare( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                  List<MavenProject> reactorProjects, boolean resume, boolean dryRun )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Prepare a release.
     *
     * @param releaseDescriptor the configuration to pass to the preparation steps
     * @param settings          the settings.xml configuration
     * @param reactorProjects   the reactor projects
     * @param resume            resume a previous release, if the properties file exists
     * @param dryRun            do not commit any changes to the file system or SCM
     * @throws ReleaseExecutionException if there is a problem performing the release
     * @throws ReleaseFailureException   if there is a problem performing the release
     *
     * @deprecated Use {@link ReleaseManager#prepare(ReleaseDescriptor, ReleaseEnvironment, List, boolean, boolean)} instead.
     */
    void prepare( ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects,
                  boolean resume, boolean dryRun )
        throws ReleaseExecutionException, ReleaseFailureException;
    
    /**
     * Prepare a release.
     * 
     * @param prepareRequest             all prepare arguments
     * @throws ReleaseExecutionException if there is a problem performing the release
     * @throws ReleaseFailureException   if there is a problem performing the release
     * @since 2.3
     */
    void prepare( ReleasePrepareRequest prepareRequest ) throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Prepare a release.
     *
     * @param releaseDescriptor the configuration to pass to the preparation steps
     * @param releaseEnvironment settings, maven-home, java-home, etc. to use during release.
     * @param reactorProjects   the reactor projects
     * @param resume            resume a previous release, if the properties file exists
     * @param dryRun            do not commit any changes to the file system or SCM
     * @param listener          the listener
     * @throws ReleaseExecutionException if there is a problem performing the release
     * @throws ReleaseFailureException   if there is a problem performing the release
     */
    void prepare( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                  List<MavenProject> reactorProjects, boolean resume, boolean dryRun, ReleaseManagerListener listener )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Prepare a release.
     *
     * @param releaseDescriptor the configuration to pass to the preparation steps
     * @param settings          the settings.xml configuration
     * @param reactorProjects   the reactor projects
     * @param resume            resume a previous release, if the properties file exists
     * @param dryRun            do not commit any changes to the file system or SCM
     * @param listener          the listener
     * @throws ReleaseExecutionException if there is a problem performing the release
     * @throws ReleaseFailureException   if there is a problem performing the release
     *
     * @deprecated Use {@link ReleaseManager#prepare(ReleaseDescriptor, ReleaseEnvironment, List, boolean, boolean, ReleaseManagerListener)} instead.
     */
    void prepare( ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects,
                  boolean resume, boolean dryRun, ReleaseManagerListener listener )
        throws ReleaseExecutionException, ReleaseFailureException;

    ReleaseResult prepareWithResult( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                     List<MavenProject> reactorProjects, boolean resume, boolean dryRun,
                                     ReleaseManagerListener listener );

    /**
     * @deprecated Use {@link ReleaseManager#prepareWithResult(ReleaseDescriptor, ReleaseEnvironment, List, boolean, boolean, ReleaseManagerListener)} instead.
     */
    ReleaseResult prepareWithResult( ReleaseDescriptor releaseDescriptor, Settings settings,
                                     List<MavenProject> reactorProjects, boolean resume, boolean dryRun,
                                     ReleaseManagerListener listener );

    /**
     * Perform a release.
     *
     * @param releaseDescriptor the configuration to use for release
     * @param releaseEnvironment settings, maven-home, java-home, etc. to use during release.
     * @param reactorProjects   the reactor projects
     * @throws ReleaseExecutionException if there is a problem performing the release
     * @throws ReleaseFailureException   if there is a problem performing the release
     */
    void perform( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                  List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Perform a release.
     *
     * @param releaseDescriptor the configuration to use for release
     * @param settings          the settings.xml configuration
     * @param reactorProjects   the reactor projects
     * @throws ReleaseExecutionException if there is a problem performing the release
     * @throws ReleaseFailureException   if there is a problem performing the release
     *
     * @deprecated Use {@link ReleaseManager#perform(ReleaseDescriptor, ReleaseEnvironment, List)} instead
     */
    void perform( ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Perform a release.
     *
     * @param releaseDescriptor the configuration to use for release
     * @param releaseEnvironment settings, maven-home, java-home, etc. to use during release.
     * @param reactorProjects   the reactor projects
     * @param listener          the listener
     * @throws ReleaseExecutionException if there is a problem performing the release
     * @throws ReleaseFailureException   if there is a problem performing the release
     */
    void perform( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                  List<MavenProject> reactorProjects, ReleaseManagerListener listener )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Perform a release.
     *
     * @param releaseDescriptor the configuration to use for release
     * @param settings          the settings.xml configuration
     * @param reactorProjects   the reactor projects
     * @param listener          the listener
     * @throws ReleaseExecutionException if there is a problem performing the release
     * @throws ReleaseFailureException   if there is a problem performing the release
     *
     * @deprecated Use {@link ReleaseManager#perform(ReleaseDescriptor, ReleaseEnvironment, List, ReleaseManagerListener)} instead.
     */
    void perform( ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects,
                  ReleaseManagerListener listener )
        throws ReleaseExecutionException, ReleaseFailureException;

    ReleaseResult performWithResult( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                     List<MavenProject> reactorProjects, ReleaseManagerListener listener );

    /**
     * @deprecated Use {@link ReleaseManager#performWithResult(ReleaseDescriptor, ReleaseEnvironment, List, ReleaseManagerListener)} instead.
     */
    ReleaseResult performWithResult( ReleaseDescriptor releaseDescriptor, Settings settings,
                                     List<MavenProject> reactorProjects, ReleaseManagerListener listener );

    /**
     * Perform a release, and optionally cleanup.
     *
     * @param releaseDescriptor the configuration to use for release
     * @param releaseEnvironment settings, maven-home, java-home, etc. to use during release.
     * @param reactorProjects   the reactor projects
     * @param clean             flag to clean the release after perform
     * @throws ReleaseExecutionException if there is a problem performing the release
     * @throws ReleaseFailureException   if there is a problem performing the release
     */
    void perform( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                  List<MavenProject> reactorProjects, boolean clean )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Perform a release
     * 
     * @param performRequest   all perform arguments
     * @throws ReleaseExecutionException if there is a problem performing the release
     * @throws ReleaseFailureException   if there is a problem performing the release
     * @since 2.3
     */
    void perform( ReleasePerformRequest performRequest )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Perform a release, and optionally cleanup.
     *
     * @param releaseDescriptor the configuration to use for release
     * @param settings          the settings.xml configuration
     * @param reactorProjects   the reactor projects
     * @param clean             flag to clean the release after perform
     * @throws ReleaseExecutionException if there is a problem performing the release
     * @throws ReleaseFailureException   if there is a problem performing the release
     *
     * @deprecated Use {@link ReleaseManager#perform(ReleaseDescriptor, ReleaseEnvironment, List, boolean)} instead.
     */
    void perform( ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects,
                  boolean clean )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Clean a release.
     *
     * @param releaseDescriptor the configuration to use for release
     * @param reactorProjects   the reactor projects
     */
    void clean( ReleaseDescriptor releaseDescriptor, ReleaseManagerListener listener, List<MavenProject> reactorProjects );

    /**
     * Clean a release.
     * 
     * @param cleanRequest all clean arguments
     * @since 2.3
     */
    void clean( ReleaseCleanRequest cleanRequest );

    /**
     * Rollback changes made by the previous release
     *
     * @param releaseDescriptor the configuration to use for release
     * @param releaseEnvironment settings, maven-home, java-home, etc. to use during release.
     * @param reactorProjects   the reactor projects
     * @throws ReleaseExecutionException if there is a problem during release rollback
     * @throws ReleaseFailureException   if there is a problem during release rollback
     */
    void rollback( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                   List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Rollback changes made by the previous release
     *
     * @param releaseDescriptor the configuration to use for release
     * @param settings          the settings.xml configuration
     * @param reactorProjects   the reactor projects
     * @throws ReleaseExecutionException if there is a problem during release rollback
     * @throws ReleaseFailureException   if there is a problem during release rollback
     *
     * @deprecated Use {@link ReleaseManager#rollback(ReleaseDescriptor, ReleaseEnvironment, List)} instead.
     */
    void rollback( ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Rollback changes made by the previous release
     *
     * @param releaseDescriptor the configuration to use for release
     * @param releaseEnvironment settings, maven-home, java-home, etc. to use during release.
     * @param reactorProjects   the reactor projects
     * @param listener          the listener
     * @throws ReleaseExecutionException if there is a problem during release rollback
     * @throws ReleaseFailureException   if there is a problem during release rollback
     */
    void rollback( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                   List<MavenProject> reactorProjects, ReleaseManagerListener listener )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Rollback changes made by the previous release
     *
     * @param releaseDescriptor the configuration to use for release
     * @param settings          the settings.xml configuration
     * @param reactorProjects   the reactor projects
     * @param listener          the listener
     * @throws ReleaseExecutionException if there is a problem during release rollback
     * @throws ReleaseFailureException   if there is a problem during release rollback
     *
     * @deprecated Use {@link ReleaseManager#rollback(ReleaseDescriptor, ReleaseEnvironment, List, ReleaseManagerListener)} instead.
     */
    void rollback( ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects,
                   ReleaseManagerListener listener )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Rollback changes made by the previous release
     * 
     * @param rollbackRequest            all rollback arguments
     * @throws ReleaseExecutionException if there is a problem during release rollback
     * @throws ReleaseFailureException   if there is a problem during release rollback
     * @since 2.3
     */
    void rollback( ReleaseRollbackRequest rollbackRequest )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Branch a project
     *
     * @param releaseDescriptor the configuration to use for release
     * @param releaseEnvironment settings, maven-home, java-home, etc. to use during release.
     * @param reactorProjects   the reactor projects
     * @param dryRun            do not commit any changes to the file system or SCM
     * @throws ReleaseExecutionException if there is a problem during release branch
     * @throws ReleaseFailureException   if there is a problem during release branch
     */
    void branch( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                 List<MavenProject> reactorProjects, boolean dryRun )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Branch a project
     *
     * @param releaseDescriptor the configuration to use for release
     * @param settings          the settings.xml configuration
     * @param reactorProjects   the reactor projects
     * @param dryRun            do not commit any changes to the file system or SCM
     * @throws ReleaseExecutionException if there is a problem during release branch
     * @throws ReleaseFailureException   if there is a problem during release branch
     *
     * @deprecated Use {@link ReleaseManager#branch(ReleaseDescriptor, ReleaseEnvironment, List, boolean)} instead.
     */
    void branch( ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects,
                 boolean dryRun )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Branch a project
     *
     * @param releaseDescriptor the configuration to use for release
     * @param releaseEnvironment settings, maven-home, java-home, etc. to use during release.
     * @param reactorProjects   the reactor projects
     * @param dryRun            do not commit any changes to the file system or SCM
     * @param listener          the listener
     * @throws ReleaseExecutionException if there is a problem during release branch
     * @throws ReleaseFailureException   if there is a problem during release branch
     */
    void branch( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                 List<MavenProject> reactorProjects, boolean dryRun, ReleaseManagerListener listener )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Branch a project
     *
     * @param releaseDescriptor the configuration to use for release
     * @param settings          the settings.xml configuration
     * @param reactorProjects   the reactor projects
     * @param dryRun            do not commit any changes to the file system or SCM
     * @param listener          the listener
     * @throws ReleaseExecutionException if there is a problem during release branch
     * @throws ReleaseFailureException   if there is a problem during release branch
     *
     * @deprecated Use {@link ReleaseManager#branch(ReleaseDescriptor, ReleaseEnvironment, List, boolean, ReleaseManagerListener)} instead.
     */
    void branch( ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects,
                 boolean dryRun, ReleaseManagerListener listener )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Branch a project
     * 
     * @param branchRequest              all branch arguments
     * @throws ReleaseExecutionException if there is a problem during release branch
     * @throws ReleaseFailureException   if there is a problem during release branch
     * @since 2.3
     */
    void branch( ReleaseBranchRequest branchRequest ) throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Update version numbers for a project
     *
     * @param releaseDescriptor the configuration to use for release
     * @param releaseEnvironment settings, maven-home, java-home, etc. to use during release.
     * @param reactorProjects   the reactor projects
     * @throws ReleaseExecutionException if there is a problem during update versions
     * @throws ReleaseFailureException   if there is a problem during update versions
     */
    void updateVersions( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                         List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Update version numbers for a project
     * 
     * @param updateVersionsRequest      all update versions arguments
     * @throws ReleaseExecutionException if there is a problem during update versions
     * @throws ReleaseFailureException   if there is a problem during update versions
     * @since 2.3
     */
    void updateVersions( ReleaseUpdateVersionsRequest updateVersionsRequest )
        throws ReleaseExecutionException, ReleaseFailureException;
}
