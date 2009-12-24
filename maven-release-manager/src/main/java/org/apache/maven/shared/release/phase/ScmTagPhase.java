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

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTagParameters;
import org.apache.maven.scm.command.tag.TagScmResult;
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
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * Tag the SCM repository after committing the release.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component role="org.apache.maven.shared.release.phase.ReleasePhase" role-hint="scm-tag"
 */
public class ScmTagPhase
    extends AbstractReleasePhase
{
    /**
     * Tool that gets a configured SCM repository from release configuration.
     *
     * @plexus.requirement
     */
    private ScmRepositoryConfigurator scmRepositoryConfigurator;

    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment, List reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult relResult = new ReleaseResult();

        validateConfiguration( releaseDescriptor );

        logInfo( relResult, "Tagging release with the label " + releaseDescriptor.getScmReleaseLabel() + "..." );

        ScmRepository repository;
        ScmProvider provider;

        String workingDirectory = releaseDescriptor.getWorkingDirectory();
        List modules = getModules( releaseDescriptor, workingDirectory );
        String scmSourceUrl = releaseDescriptor.getScmSourceUrl();

     // determine if project is a flat multi-module
        if ( modules != null && !modules.isEmpty() )
        {
            workingDirectory = ReleaseUtil.getBaseWorkingDirectory( workingDirectory, modules );
            releaseDescriptor.setScmSourceUrl( ReleaseUtil.getBaseScmUrl( scmSourceUrl, modules ) );
        }

        try
        {
            repository = scmRepositoryConfigurator.getConfiguredRepository( releaseDescriptor, releaseEnvironment.getSettings() );

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

        TagScmResult result;
        try
        {
            // TODO: want includes/excludes?
            ScmFileSet fileSet = new ScmFileSet( new File( workingDirectory ) );
            String tagName = releaseDescriptor.getScmReleaseLabel();
            ScmTagParameters scmTagParameters = new ScmTagParameters( releaseDescriptor.getScmCommentPrefix()
                + " copy for tag " + tagName );
            scmTagParameters.setRemoteTagging( releaseDescriptor.isRemoteTagging() );
            scmTagParameters.setScmRevision( releaseDescriptor.getScmReleasedPomRevision() );
            if (getLogger().isDebugEnabled())
            {
                getLogger().debug( "ScmTagPhase :: scmTagParameters remotingTag " + releaseDescriptor.isRemoteTagging() );
                getLogger().debug( "ScmTagPhase :: scmTagParameters scmRevision " + releaseDescriptor.getScmReleasedPomRevision() );
            }
            result = provider.tag( repository, fileSet, tagName, scmTagParameters );
        }
        catch ( ScmException e )
        {
            throw new ReleaseExecutionException( "An error is occurred in the tag process: " + e.getMessage(), e );
        }
        finally
        {
            revertToOriginalScmSourceUrl( releaseDescriptor, scmSourceUrl );
        }

        if ( !result.isSuccess() )
        {
            throw new ReleaseScmCommandException( "Unable to tag SCM", result );
        }

        relResult.setResultCode( ReleaseResult.SUCCESS );

        return relResult;
    }

    private void revertToOriginalScmSourceUrl( ReleaseDescriptor releaseDescriptor, String scmSourceUrl )
    {
        if( !scmSourceUrl.equals( releaseDescriptor.getScmSourceUrl() ) )
        {
            releaseDescriptor.setScmSourceUrl( scmSourceUrl );
        }
    }

    private List getModules( ReleaseDescriptor releaseDescriptor, String workingDirectory )
    {
        Reader in = null;
        try
        {
            String pomFile = releaseDescriptor.getPomFileName();
            if ( pomFile == null || "".equals( pomFile.trim() ) )
            {
                pomFile = ReleaseUtil.POMv4;
            }

            String pathToRootPom = workingDirectory + ReleaseUtil.FS + pomFile;

            MavenXpp3Reader reader = new MavenXpp3Reader();
            in = ReaderFactory.newXmlReader( new File( pathToRootPom ) );

            Model model = reader.read( in );

            return model.getModules();
        }
        catch ( FileNotFoundException e )
        {
            getLogger().warn( "Pom file not found : " + e.getMessage() );
            getLogger().warn( "Assuming working directory in release descriptor is the base working directory." );

        }
        catch ( IOException e )
        {
            getLogger().warn( "IO error occurred while reading pom file : " + e.getMessage() );
            getLogger().warn( "Assuming working directory in release descriptor is the base working directory." );
        }
        catch ( XmlPullParserException e )
        {
            getLogger().warn( "Error parsing pom file : " + e.getMessage() );
            getLogger().warn( "Assuming working directory in release descriptor is the base working directory." );
        }
        finally
        {
            IOUtil.close( in );
        }

        return null;
    }

    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment, List reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = new ReleaseResult();

        validateConfiguration( releaseDescriptor );

        String workingDirectory = releaseDescriptor.getWorkingDirectory();
        List modules = getModules( releaseDescriptor, workingDirectory );
        String scmSourceUrl = releaseDescriptor.getScmSourceUrl();

     // determine if project is a flat multi-module
        if ( modules != null && !modules.isEmpty() )
        {
            workingDirectory = ReleaseUtil.getBaseWorkingDirectory( workingDirectory, modules );
            releaseDescriptor.setScmSourceUrl( ReleaseUtil.getBaseScmUrl( scmSourceUrl, modules ) );
        }

        logInfo( result, "Full run would be tagging " + workingDirectory + " with label: '" +
            releaseDescriptor.getScmReleaseLabel() + "'" );

        revertToOriginalScmSourceUrl( releaseDescriptor, scmSourceUrl );

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    private static void validateConfiguration( ReleaseDescriptor releaseDescriptor )
        throws ReleaseFailureException
    {
        if ( releaseDescriptor.getScmReleaseLabel() == null )
        {
            throw new ReleaseFailureException( "A release label is required for committing" );
        }
    }
}
