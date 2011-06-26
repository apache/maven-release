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
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.edit.EditScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Edwin Punzalan
 * @plexus.component role="org.apache.maven.shared.release.phase.ReleasePhase" role-hint="restore-backup-poms"
 */
public class RestoreBackupPomsPhase
    extends AbstractBackupPomsPhase
{
    /**
     * Tool that gets a configured SCM repository from release configuration.
     *
     * @plexus.requirement
     */
    private ScmRepositoryConfigurator scmRepositoryConfigurator;

    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                  List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = new ReleaseResult();

        for ( MavenProject project : reactorProjects )
        {
            restorePomBackup( releaseDescriptor, releaseEnvironment, project );
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

    protected void restorePomBackup( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                     MavenProject project )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        File pomBackup = getPomBackup( project );

        if ( !pomBackup.exists() )
        {
            throw new ReleaseExecutionException(
                "Cannot restore from a missing backup POM: " + pomBackup.getAbsolutePath() );
        }

        try
        {
            ScmRepository scmRepository;
            ScmProvider provider;
            try
            {
                scmRepository =
                    scmRepositoryConfigurator.getConfiguredRepository( releaseDescriptor,
                                                                       releaseEnvironment.getSettings() );

                provider = scmRepositoryConfigurator.getRepositoryProvider( scmRepository );
            }
            catch ( ScmRepositoryException e )
            {
                throw new ReleaseScmRepositoryException( e.getMessage(), e.getValidationMessages() );
            }
            catch ( NoSuchScmProviderException e )
            {
                throw new ReleaseExecutionException( "Unable to configure SCM repository: " + e.getMessage(), e );
            }

            if ( releaseDescriptor.isScmUseEditMode() || provider.requiresEditMode() )
            {
                EditScmResult result = provider.edit( scmRepository, new ScmFileSet(
                    new File( releaseDescriptor.getWorkingDirectory() ), project.getFile() ) );

                if ( !result.isSuccess() )
                {
                    throw new ReleaseScmCommandException( "Unable to enable editing on the POM", result );
                }
            }
        }
        catch ( ScmException e )
        {
            throw new ReleaseExecutionException( "An error occurred enabling edit mode: " + e.getMessage(), e );
        }

        try
        {
            FileUtils.copyFile( getPomBackup( project ), ReleaseUtil.getStandardPom( project ) );
        }
        catch ( IOException e )
        {
            throw new ReleaseExecutionException( "Error restoring from backup POM: " + e.getMessage(), e );
        }
    }
}
