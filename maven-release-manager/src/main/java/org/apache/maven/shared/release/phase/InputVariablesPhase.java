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
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.policy.PolicyException;
import org.apache.maven.shared.release.policy.naming.NamingPolicy;
import org.apache.maven.shared.release.policy.naming.NamingPolicyRequest;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.PrefixAwareRecursionInterceptor;
import org.codehaus.plexus.interpolation.PrefixedPropertiesValueSource;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.util.StringUtils;

/**
 * Input any variables that were not yet configured.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class InputVariablesPhase
    extends AbstractReleasePhase
{
    /**
     * Component used to prompt for input.
     */
    @Requirement
    private Prompter prompter;

    /**
     * Whether this is a branch or a tag operation.
     */
    private boolean branchOperation;

    /**
     * Tool that gets a configured SCM repository from release configuration.
     */
    @Requirement
    private ScmRepositoryConfigurator scmRepositoryConfigurator;

    /**
     * Component used for custom or default naming policy
     */
    @Requirement
    private Map<String, NamingPolicy> namingPolicies;

    /**
     * The default naming policy to apply, if any
     */
    private String defaultNamingPolicy;

    void setPrompter( Prompter prompter )
    {
        this.prompter = prompter;
    }

    boolean isBranchOperation()
    {
        return branchOperation;
    }

    protected ScmProvider getScmProvider( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment )
        throws ReleaseScmRepositoryException, ReleaseExecutionException
    {
        try
        {
            ScmRepository repository =
                scmRepositoryConfigurator.getConfiguredRepository( releaseDescriptor,
                                                                   releaseEnvironment.getSettings() );

            return scmRepositoryConfigurator.getRepositoryProvider( repository );
        }
        catch ( ScmRepositoryException e )
        {
            throw new ReleaseScmRepositoryException(
                e.getMessage() + " for URL: " + releaseDescriptor.getScmSourceUrl(), e.getValidationMessages() );
        }
        catch ( NoSuchScmProviderException e )
        {
            throw new ReleaseExecutionException( "Unable to configure SCM repository: " + e.getMessage(), e );
        }
    }

    @Override
    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                  List<MavenProject> reactorProjects )
        throws ReleaseExecutionException
    {
        ReleaseResult result = new ReleaseResult();

        // get the root project
        MavenProject project = ReleaseUtil.getRootProject( reactorProjects );

        String tag = releaseDescriptor.getScmReleaseLabel();

        if ( tag == null )
        {
            // Must get default version from mapped versions, as the project will be the incorrect snapshot
            String key = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );
            String releaseVersion = releaseDescriptor.getProjectReleaseVersion( key );
            if ( releaseVersion == null )
            {
                throw new ReleaseExecutionException( "Project tag cannot be selected if version is not yet mapped" );
            }

            String suggestedName;
            String scmTagNameFormat = releaseDescriptor.getScmTagNameFormat();
            if ( releaseDescriptor.getProjectNamingPolicyId() != null )
            {
                try
                {
                    suggestedName =
                        resolveSuggestedName( releaseDescriptor.getProjectNamingPolicyId(), releaseVersion, project );
                }
                catch ( PolicyException e )
                {
                    throw new ReleaseExecutionException( e.getMessage(), e );
                }
            }
            else if ( scmTagNameFormat != null )
            {
                Interpolator interpolator = new StringSearchInterpolator( "@{", "}" );
                List<String> possiblePrefixes = java.util.Arrays.asList( "project", "pom" );
                Properties values = new Properties();
                values.setProperty( "artifactId", project.getArtifactId() );
                values.setProperty( "groupId", project.getGroupId() );
                values.setProperty( "version", releaseVersion );
                interpolator.addValueSource( new PrefixedPropertiesValueSource( possiblePrefixes, values, true ) );
                RecursionInterceptor recursionInterceptor = new PrefixAwareRecursionInterceptor( possiblePrefixes );
                try
                {
                    suggestedName = interpolator.interpolate( scmTagNameFormat, recursionInterceptor );
                }
                catch ( InterpolationException e )
                {
                    throw new ReleaseExecutionException(
                        "Could not interpolate specified tag name format: " + scmTagNameFormat, e );
                }
            }
            else
            {
                try
                {
                    suggestedName = resolveSuggestedName( defaultNamingPolicy, releaseVersion, project );
                }
                catch ( PolicyException e )
                {
                    throw new ReleaseExecutionException( e.getMessage(), e );
                }
            }

            ScmProvider provider = null;
            try
            {
                provider = getScmProvider( releaseDescriptor, releaseEnvironment );
            }
            catch ( ReleaseScmRepositoryException e )
            {
                throw new ReleaseExecutionException(
                    "No scm provider can be found for url: " + releaseDescriptor.getScmSourceUrl(), e );
            }

            suggestedName = provider.sanitizeTagName( suggestedName );

            if ( releaseDescriptor.isInteractive() )
            {
                try
                {
                    if ( branchOperation )
                    {
                        tag = prompter.prompt( "What is the branch name for \"" + project.getName() + "\"? ("
                            + project.getGroupId() + ":" + project.getArtifactId() + ")" );
                        if ( StringUtils.isEmpty( tag ) )
                        {
                            throw new ReleaseExecutionException( "No branch name was given." );
                        }
                    }
                    else
                    {
                        tag = prompter.prompt( "What is the SCM release tag or label for \"" + project.getName()
                            + "\"? (" + project.getGroupId() + ":" + project.getArtifactId() + ")", suggestedName );
                    }
                }
                catch ( PrompterException e )
                {
                    throw new ReleaseExecutionException( "Error reading version from input handler: " + e.getMessage(),
                                                         e );
                }
            }
            else if ( suggestedName == null )
            {
                if ( isBranchOperation() )
                {
                    throw new ReleaseExecutionException( "No branch name was given." );
                }
                else
                {
                    throw new ReleaseExecutionException( "No tag name was given." );
                }
            }
            else
            {
                tag = suggestedName;
            }
            releaseDescriptor.setScmReleaseLabel( tag );
        }

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    @Override
    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                   List<MavenProject> reactorProjects )
        throws ReleaseExecutionException
    {
        ReleaseResult result = new ReleaseResult();

        // It makes no modifications, so simulate is the same as execute
        execute( releaseDescriptor, releaseEnvironment, reactorProjects );

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    private String resolveSuggestedName( String policyId, String version, MavenProject project )
        throws PolicyException
    {
        if ( policyId == null )
        {
            return null;
        }

        NamingPolicy policy = namingPolicies.get( policyId );
        if ( policy == null )
        {
            throw new PolicyException( "Policy '" + policyId + "' is unknown, available: "
                + namingPolicies.keySet() );
        }

        NamingPolicyRequest request = new NamingPolicyRequest()
                        .setGroupId( project.getGroupId() )
                        .setArtifactId( project.getArtifactId() )
                        .setVersion( version );
        return policy.getName( request ).getName();
    }
}
