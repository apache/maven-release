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

import java.io.File;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.codehaus.plexus.component.annotations.Component;

import java.util.List;
import org.apache.maven.scm.CommandParameter;
import org.apache.maven.scm.CommandParameters;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.untag.UntagScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * Remove tag from SCM repository during rollback
 */
@Component( role = ReleasePhase.class, hint = "remove-scm-tag" )
public class RemoveScmTagPhase
    extends AbstractReleasePhase
{
    /**
     * Tool that gets a configured SCM repository from release configuration.
     */
    @Requirement
    private ScmRepositoryConfigurator scmRepositoryConfigurator;

    @Override
    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                  List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult releaseResult = new ReleaseResult();

        validateConfiguration( releaseDescriptor );

        logInfo( releaseResult, "Removing tag with the label " + releaseDescriptor.getScmReleaseLabel() + " ..." );

        ReleaseDescriptor basedirAlignedReleaseDescriptor =
            ReleaseUtil.createBasedirAlignedReleaseDescriptor( releaseDescriptor, reactorProjects );

        ScmRepository repository;
        ScmProvider provider;
        try
        {
            repository =
                scmRepositoryConfigurator.getConfiguredRepository( basedirAlignedReleaseDescriptor.getScmSourceUrl(),
                                                                   releaseDescriptor,
                                                                   releaseEnvironment.getSettings() );

            repository.getProviderRepository().setPushChanges( releaseDescriptor.isPushChanges() );

            repository.getProviderRepository().setWorkItem( releaseDescriptor.getWorkItem() );

            provider = scmRepositoryConfigurator.getRepositoryProvider( repository );
        }
        catch ( ScmRepositoryException e )
        {
            throw new ReleaseScmRepositoryException( e.getMessage(), e.getValidationMessages() );
        }
        catch ( NoSuchScmProviderException e )
        {
            throw new ReleaseExecutionException( "Unable to configure SCM repository: " + e.getMessage(), e );
        }

        UntagScmResult untagScmResult;
        try
        {
            ScmFileSet fileSet = new ScmFileSet( new File( basedirAlignedReleaseDescriptor.getWorkingDirectory() ) );
            String tagName = releaseDescriptor.getScmReleaseLabel();
            String message = releaseDescriptor.getScmCommentPrefix() + "remove tag " + tagName;
            CommandParameters commandParameters = new CommandParameters();
            commandParameters.setString( CommandParameter.TAG_NAME, tagName );
            commandParameters.setString( CommandParameter.MESSAGE, message );
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug(
                    "RemoveScmTagPhase :: scmUntagParameters tagName " + tagName );
                getLogger().debug(
                    "RemoveScmTagPhase :: scmUntagParameters message " + message );
                getLogger().debug(
                    "RemoveScmTagPhase :: fileSet  " + fileSet );
            }
            untagScmResult = provider.untag( repository, fileSet, commandParameters );
        }
        catch ( ScmException e )
        {
            throw new ReleaseExecutionException( "An error has occurred in the remove tag process: "
                + e.getMessage(), e );
        }

        if ( !untagScmResult.isSuccess() )
        {
            getLogger().warn( String.format( "Unable to remove tag%nProvider message: %s%nCommand output: %s",
                    untagScmResult.getProviderMessage(), untagScmResult.getCommandOutput() ) );
        }

        releaseResult.setResultCode( ReleaseResult.SUCCESS );

        return releaseResult;
    }

    @Override
    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                   List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult releaseResult = new ReleaseResult();

        validateConfiguration( releaseDescriptor );

        logInfo( releaseResult, "Full run would remove tag with label: '" + releaseDescriptor.getScmReleaseLabel()
                + "'" );

        releaseResult.setResultCode( ReleaseResult.SUCCESS );

        return releaseResult;
    }

    private void validateConfiguration( ReleaseDescriptor releaseDescriptor )
        throws ReleaseFailureException
    {
        if ( releaseDescriptor.getScmReleaseLabel() == null )
        {
            throw new ReleaseFailureException( "A release label is required for removal" );
        }
    }

}
