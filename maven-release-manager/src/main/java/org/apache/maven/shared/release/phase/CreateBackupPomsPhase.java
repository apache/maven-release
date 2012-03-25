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

import java.io.IOException;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author Edwin Punzalan
 * @plexus.component role="org.apache.maven.shared.release.phase.ReleasePhase" role-hint="create-backup-poms"
 */
public class CreateBackupPomsPhase
    extends AbstractBackupPomsPhase
{
    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                  List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = new ReleaseResult();

        // remove previous backups, if any
        clean( reactorProjects );

        for ( MavenProject project : reactorProjects )
        {
            createPomBackup( project );
        }

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    public ReleaseResult clean( List<MavenProject> reactorProjects )
    {
        ReleaseResult result = new ReleaseResult();

        for ( MavenProject project : reactorProjects )
        {
            deletePomBackup( project );
        }

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                   List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        return execute( releaseDescriptor, releaseEnvironment, reactorProjects );
    }

    private void createPomBackup( MavenProject project )
        throws ReleaseExecutionException
    {
        // delete any existing backup first
        deletePomBackup( project );

        try
        {
            FileUtils.copyFile( ReleaseUtil.getStandardPom( project ), getPomBackup( project ) );
        }
        catch ( IOException e )
        {
            throw new ReleaseExecutionException( "Error creating backup POM: " + e.getMessage(), e );
        }
    }
}
