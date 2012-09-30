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
        return runLogic( releaseDescriptor, releaseEnvironment, reactorProjects, false );
    }

    private ReleaseResult runLogic( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                  List<MavenProject> reactorProjects, boolean simulate )
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
        
        String pomFileName = releaseDescriptor.getPomFileName();
        if ( pomFileName == null )
        {
            pomFileName = "pom.xml";
        }

        // ensure we don't use the release pom for the perform goals
        // ^^ paranoia? A MavenExecutor has already access to this. Probably worth refactoring. 
        if ( !StringUtils.isEmpty( additionalArguments ) )
        {
            additionalArguments = additionalArguments + " -f " + pomFileName;
        }
        else
        {
            additionalArguments = "-f " + pomFileName;
        }
        
        if ( simulate )
        {
            ReleaseResult result = new ReleaseResult();

            logDebug( result, "Additional arguments: " + additionalArguments );
            
            logInfo( result, "Executing perform goals  - since this is simulation mode these goals are skipped." );
            
            return result;
        }

        String workDir = releaseDescriptor.getWorkingDirectory();
        if ( workDir == null )
        {
            workDir = System.getProperty( "user.dir" );
        }


        File pomFile = new File( workDir, pomFileName );
        PomFinder pomFinder = new PomFinder( getLogger() );
        boolean foundPom = false;

        if ( StringUtils.isEmpty( releaseDescriptor.getScmRelativePathProjectDirectory() ) )
        {
            foundPom = pomFinder.parsePom( pomFile );
        }

        File workDirectory;
        if ( simulate )
        {
            workDirectory = new File( releaseDescriptor.getWorkingDirectory() );
        }
        else
        {
            workDirectory = new File( releaseDescriptor.getCheckoutDirectory() );
        }

        if ( foundPom )
        {
            File matchingPom = pomFinder.findMatchingPom( workDirectory );
            if ( matchingPom != null )
            {
                getLogger().info( "Invoking perform goals in directory " + matchingPom.getParent() );
                // The directory of the POM in a flat project layout is not
                // the same directory as the SCM checkout directory!
                // The same is true for a sparse checkout in e.g. GIT
                // the project to build could be in target/checkout/some/dir/
                workDirectory = matchingPom.getParentFile();
            }
        }

        return execute( releaseDescriptor, releaseEnvironment, workDirectory, additionalArguments );
    }

    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                   List<MavenProject> reactorProjects )
        throws ReleaseExecutionException
    {
        return runLogic( releaseDescriptor, releaseEnvironment, reactorProjects, true );
    }

    protected String getGoals( ReleaseDescriptor releaseDescriptor )
    {
        return releaseDescriptor.getPerformGoals();
    }
}
