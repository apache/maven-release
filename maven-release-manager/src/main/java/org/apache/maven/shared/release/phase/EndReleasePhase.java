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
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;

import java.util.List;

/**
 * Finalise release preparation so it can be flagged complete..
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component role="org.apache.maven.shared.release.phase.ReleasePhase" role-hint="end-release"
 */
public class EndReleasePhase
    extends AbstractReleasePhase
{
    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment, List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = new ReleaseResult();

        logInfo( result, "Release preparation complete." );

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment, List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = new ReleaseResult();

        logInfo( result, "Release preparation simulation complete." );

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }
}
