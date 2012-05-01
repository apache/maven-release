package org.apache.maven.shared.release.phase;

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
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;

import java.util.List;

/**
 * A phase in the release cycle.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface ReleasePhase
{
    /**
     * The Plexus role.
     */
    String ROLE = ReleasePhase.class.getName();

    /**
     * Execute the phase.
     *
     * @param releaseDescriptor the configuration to use
     * @param releaseEnvironment the environmental configuration, such as Maven settings, Maven home, etc.
     * @param reactorProjects   the reactor projects
     * @throws ReleaseExecutionException an exception during the execution of the phase
     * @throws ReleaseFailureException   a failure during the execution of the phase
     * @return the release result
     */
    ReleaseResult execute( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment, List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Simulate the phase, but don't make any changes to the project.
     *
     * @param releaseDescriptor the configuration to use
     * @param releaseEnvironment the environmental configuration, such as Maven settings, Maven home, etc.
     * @param reactorProjects   the reactor projects
     * @throws ReleaseExecutionException an exception during the execution of the phase
     * @throws ReleaseFailureException   a failure during the execution of the phase
     * @return the release result
     */
    ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment, List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Execute the phase.
     *
     * @param releaseDescriptor the configuration to use
     * @param settings          the settings.xml configuration
     * @param reactorProjects   the reactor projects
     * @throws ReleaseExecutionException an exception during the execution of the phase
     * @throws ReleaseFailureException   a failure during the execution of the phase
     * @return the release result
     *
     * @deprecated Use {@link ReleasePhase#execute(ReleaseDescriptor, ReleaseEnvironment, List)} instead.
     */
    ReleaseResult execute( ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Simulate the phase, but don't make any changes to the project.
     *
     * @param releaseDescriptor the configuration to use
     * @param settings          the settings.xml configuration
     * @param reactorProjects   the reactor projects
     * @throws ReleaseExecutionException an exception during the execution of the phase
     * @throws ReleaseFailureException   a failure during the execution of the phase
     * @return the release result
     *
     * @deprecated Use {@link ReleasePhase#simulate(ReleaseDescriptor, ReleaseEnvironment, List)} instead.
     */
    ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Clean up after a phase if it leaves any additional files in the checkout.
     *
     * @param reactorProjects the reactor projects
     * @return the release result
     */
    ReleaseResult clean( List<MavenProject> reactorProjects );
}
