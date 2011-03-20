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
import java.io.IOException;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
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
import org.codehaus.plexus.util.StringUtils;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.shared.release.phase.ReleasePhase" role-hint="checkout-project-from-scm"
 */
public class CheckoutProjectFromScm
    extends AbstractReleasePhase
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

        logInfo( result, "Checking out the project to perform the release ..." );

        ScmRepository repository;
        ScmProvider provider;

        if ( releaseDescriptor.isLocalCheckout() )
        {
            // in the release phase we have to change the checkout URL
            // to do a local checkout instead of going over the network.

            // the first step is a bit tricky, we need to know which provider! like e.g. "scm:jgit:http://"
            // the offset of 4 is because 'scm:' has 4 characters...
            String providerPart = releaseDescriptor.getScmSourceUrl().substring( 0,
                                                                                 releaseDescriptor.getScmSourceUrl().indexOf(
                                                                                     ':', 4 ) );
            releaseDescriptor.setScmSourceUrl( providerPart + ":file://" + releaseDescriptor.getWorkingDirectory() );
            getLogger().info( "Performing a LOCAL checkout from " + releaseDescriptor.getScmSourceUrl() );
        }

        try
        {
            repository = scmRepositoryConfigurator.getConfiguredRepository( releaseDescriptor,
                                                                            releaseEnvironment.getSettings() );

            provider = scmRepositoryConfigurator.getRepositoryProvider( repository );
        }
        catch ( ScmRepositoryException e )
        {
            result.setResultCode( ReleaseResult.ERROR );
            logError( result, e.getMessage() );

            throw new ReleaseScmRepositoryException( e.getMessage(), e.getValidationMessages() );
        }
        catch ( NoSuchScmProviderException e )
        {
            result.setResultCode( ReleaseResult.ERROR );
            logError( result, e.getMessage() );

            throw new ReleaseExecutionException( "Unable to configure SCM repository: " + e.getMessage(), e );
        }

        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        // TODO: sanity check that it is not . or .. or lower
        File checkoutDirectory;
        if ( StringUtils.isEmpty( releaseDescriptor.getCheckoutDirectory() ) )
        {
            checkoutDirectory = new File( rootProject.getFile().getParentFile(), "target/checkout" );
            releaseDescriptor.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );
        }
        else
        {
            checkoutDirectory = new File( releaseDescriptor.getCheckoutDirectory() );
        }

        if ( checkoutDirectory.exists() )
        {
            try
            {
                FileUtils.deleteDirectory( checkoutDirectory );
            }
            catch ( IOException e )
            {
                result.setResultCode( ReleaseResult.ERROR );
                logError( result, e.getMessage() );

                throw new ReleaseExecutionException( "Unable to remove old checkout directory: " + e.getMessage(), e );
            }
        }

        checkoutDirectory.mkdirs();

        CheckOutScmResult scmResult;

        try
        {
            scmResult = provider.checkOut( repository, new ScmFileSet( checkoutDirectory ),
                                           new ScmTag( releaseDescriptor.getScmReleaseLabel() ) );
        }
        catch ( ScmException e )
        {
            result.setResultCode( ReleaseResult.ERROR );
            logError( result, e.getMessage() );

            throw new ReleaseExecutionException( "An error is occurred in the checkout process: " + e.getMessage(), e );
        }

        String scmRelativePathProjectDirectory = scmResult.getRelativePathProjectDirectory();
        if ( StringUtils.isEmpty( scmRelativePathProjectDirectory ) )
        {
            String basedir;
            try
            {
                basedir = ReleaseUtil.getCommonBasedir( reactorProjects );
            }
            catch ( IOException e )
            {
            	throw new ReleaseExecutionException("Exception occurred while calculating common basedir: " + e.getMessage(), e);
            }

            String rootProjectBasedir = rootProject.getBasedir().getAbsolutePath();
            try
            {
                if ( ReleaseUtil.isSymlink( rootProject.getBasedir() ) )
                {
                    rootProjectBasedir = rootProject.getBasedir().getCanonicalPath();
                }
            }
            catch ( IOException e )
            {
                throw new ReleaseExecutionException( e.getMessage(), e );
            }
            if ( rootProjectBasedir.length() > basedir.length() )
            {
                scmRelativePathProjectDirectory = rootProjectBasedir.substring( basedir.length() + 1 );
            }
        }
        releaseDescriptor.setScmRelativePathProjectDirectory( scmRelativePathProjectDirectory );

        if ( !scmResult.isSuccess() )
        {
            result.setResultCode( ReleaseResult.ERROR );
            logError( result, scmResult.getProviderMessage() );

            throw new ReleaseScmCommandException( "Unable to checkout from SCM", scmResult );
        }

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                   List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        return simulate( releaseDescriptor, releaseEnvironment, reactorProjects );
    }
}
