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
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.util.PomFinder;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.List;

/**
 * Run the integration tests for the project to verify that it builds before committing.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component role="org.apache.maven.shared.release.phase.ReleasePhase" role-hint="run-perform-goals"
 */
public class RunPerformGoalsPhase
    extends AbstractRunGoalsPhase
{
    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                  List<MavenProject> reactorProjects )
        throws ReleaseExecutionException
    {
        String additionalArguments = releaseDescriptor.getAdditionalArguments();

        if ( releaseDescriptor.isUseReleaseProfile() )
        {
            if ( !StringUtils.isEmpty( additionalArguments ) )
            {
                additionalArguments = additionalArguments + " -DperformRelease=true";
            }
            else
            {
                additionalArguments = "-DperformRelease=true";
            }
        }

        // ensure we don't use the release pom for the perform goals
        if ( !StringUtils.isEmpty( additionalArguments ) )
        {
            additionalArguments = additionalArguments + " -f pom.xml";
        }
        else
        {
            additionalArguments = "-f pom.xml";
        }

        String workDir = releaseDescriptor.getWorkingDirectory();
        if ( workDir == null )
        {
            workDir = System.getProperty( "user.dir" );
        }

        String pomFileName = releaseDescriptor.getPomFileName();
        if ( pomFileName == null )
        {
            pomFileName = "pom.xml";
        }

        File pomFile = new File( workDir, pomFileName );
        PomFinder pomFinder = new PomFinder( getLogger() );
        boolean foundPom = pomFinder.parsePom( pomFile );

        if ( foundPom )
        {
            File matchingPom = pomFinder.findMatchingPom( new File( releaseDescriptor.getCheckoutDirectory() ) );
            if ( matchingPom != null )
            {
                getLogger().info( "Invoking perform goals in directory " + matchingPom.getParent() );
                releaseDescriptor.setCheckoutDirectory( matchingPom.getParent() );
            }

        }

        return execute( releaseDescriptor, releaseEnvironment, new File( releaseDescriptor.getCheckoutDirectory() ),
                        additionalArguments );
    }

    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                   List<MavenProject> reactorProjects )
        throws ReleaseExecutionException
    {
        ReleaseResult result = new ReleaseResult();

        logInfo( result, "Executing perform goals" );

        execute( releaseDescriptor, releaseEnvironment, reactorProjects );

        return result;
    }

    protected String getGoals( ReleaseDescriptor releaseDescriptor )
    {
        return releaseDescriptor.getPerformGoals();
    }
}
