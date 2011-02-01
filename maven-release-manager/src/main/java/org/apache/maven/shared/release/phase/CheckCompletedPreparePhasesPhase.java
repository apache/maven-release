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

import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.shared.release.phase.ReleasePhase" role-hint="verify-completed-prepare-phases"
 */
public class CheckCompletedPreparePhasesPhase
    extends AbstractReleasePhase
{
    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor,
                                  ReleaseEnvironment releaseEnvironment,
                                  List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = new ReleaseResult();

        // if we stopped mid-way through preparation - don't perform
        if ( releaseDescriptor.getCompletedPhase() != null
             && !"end-release".equals( releaseDescriptor.getCompletedPhase() ) )
        {
            String message = "Cannot perform release - the preparation step was stopped mid-way. Please re-run "
                             + "release:prepare to continue, or perform the release from an SCM tag.";

            result.setResultCode( ReleaseResult.ERROR );

            logError( result, message );

            throw new ReleaseFailureException( message );
        }

        if ( releaseDescriptor.getScmSourceUrl() == null )
        {
            String message = "No SCM URL was provided to perform the release from";

            result.setResultCode( ReleaseResult.ERROR );

            logError( result, message );

            throw new ReleaseFailureException( message );
        }

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor,
                                   ReleaseEnvironment releaseEnvironment,
                                   List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        return execute( releaseDescriptor, releaseEnvironment, reactorProjects );
    }
}
