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

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.policy.PolicyException;
import org.apache.maven.shared.release.policy.version.VersionPolicy;
import org.apache.maven.shared.release.policy.version.VersionPolicyRequest;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.util.StringUtils;

/**
 * Map projects to their new versions after release / into the next development cycle.
 *
 * The map-phases per goal are:
 * <dl>
 *  <dt>release:prepare</dt><dd>map-release-versions + map-development-versions; RD.isBranchCreation() = false</dd>
 *  <dt>release:branch</dt><dd>map-branch-versions + map-development-versions; RD.isBranchCreation() = true</dd>
 *  <dt>release:update-versions</dt><dd>map-development-versions; RD.isBranchCreation() = false</dd>
 * </dl>
 *
 * <table>
 *   <caption>MapVersionsPhase</caption>
 *   <tr>
 *     <th>MapVersionsPhase field</th><th>map-release-versions</th><th>map-branch-versions</th>
 *     <th>map-development-versions</th>
 *   </tr>
 *   <tr>
 *     <td>convertToSnapshot</td>     <td>false</td>               <td>true</td>               <td>true</td>
 *   </tr>
 *   <tr>
 *     <td>convertToBranch</td>       <td>false</td>               <td>true</td>               <td>false</td>
 *   </tr>
 * </table>
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author Robert Scholte
 */
public class MapVersionsPhase
    extends AbstractReleasePhase
{
    private ResourceBundle resourceBundle;

    /**
     * Whether to convert to a snapshot or a release.
     */
    private boolean convertToSnapshot;

    /**
     * Whether to convert to a snapshot or a release.
     */
    private boolean convertToBranch;

    /**
     * Component used to prompt for input.
     */
    private Prompter prompter;


    /**
     * Component used for custom or default version policy
     */
    private Map<String, VersionPolicy> versionPolicies;

    void setPrompter( Prompter prompter )
    {
        this.prompter = prompter;
    }

    @Override
    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                  List<MavenProject> reactorProjects )
        throws ReleaseExecutionException
    {
        ReleaseResult result = new ReleaseResult();

        resourceBundle = getResourceBundle( releaseEnvironment.getLocale() );

        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );

        if ( releaseDescriptor.isAutoVersionSubmodules() && ArtifactUtils.isSnapshot( rootProject.getVersion() ) )
        {
            // get the root project
            MavenProject project = rootProject;

            String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

            String nextVersion = resolveNextVersion( project, projectId, releaseDescriptor, result );

            if ( !convertToSnapshot )
            {
                releaseDescriptor.addReleaseVersion( projectId, nextVersion );
            }
            else if ( releaseDescriptor.isBranchCreation() && convertToBranch )
            {
                releaseDescriptor.addReleaseVersion( projectId, nextVersion );
            }
            else
            {
                releaseDescriptor.addDevelopmentVersion( projectId, nextVersion );
            }

            for ( MavenProject subProject : reactorProjects )
            {
                String subProjectId =
                    ArtifactUtils.versionlessKey( subProject.getGroupId(), subProject.getArtifactId() );

                if ( convertToSnapshot )
                {
                    String v;
                    if ( ArtifactUtils.isSnapshot( subProject.getVersion() ) )
                    {
                        v = nextVersion;
                    }
                    else
                    {
                        v = subProject.getVersion();
                    }

                    if ( releaseDescriptor.isBranchCreation() && convertToBranch )
                    {
                        releaseDescriptor.addReleaseVersion( subProjectId, v );
                    }
                    else
                    {
                        releaseDescriptor.addDevelopmentVersion( subProjectId, v );
                    }
                }
                else
                {
                    releaseDescriptor.addReleaseVersion( subProjectId, nextVersion );
                }
            }
        }
        else
        {
            for ( MavenProject project : reactorProjects )
            {
                String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

                String nextVersion = resolveNextVersion( project, projectId, releaseDescriptor, result );

                if ( !convertToSnapshot )
                {
                    releaseDescriptor.addReleaseVersion( projectId, nextVersion );
                }
                else if ( releaseDescriptor.isBranchCreation() && convertToBranch )
                {
                    releaseDescriptor.addReleaseVersion( projectId, nextVersion );
                }
                else
                {
                    releaseDescriptor.addDevelopmentVersion( projectId, nextVersion );
                }
            }
        }

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    private String resolveNextVersion( MavenProject project,
                                   String projectId,
                                   ReleaseDescriptor releaseDescriptor,
                                   ReleaseResult result )
        throws ReleaseExecutionException
    {
        String defaultVersion;
        if ( convertToBranch )
        {
            // no branch modification
            if ( !( releaseDescriptor.isUpdateBranchVersions()
                            && ( ArtifactUtils.isSnapshot( project.getVersion() )
                                            || releaseDescriptor.isUpdateVersionsToSnapshot() ) ) )
            {
                return project.getVersion();
            }

            defaultVersion = getReleaseVersion( projectId, releaseDescriptor );
        }
        else if ( !convertToSnapshot ) // map-release-version
        {
            defaultVersion = getReleaseVersion( projectId, releaseDescriptor );
        }
        else if ( releaseDescriptor.isBranchCreation() )
        {
            // no working copy modification
            if ( !( ArtifactUtils.isSnapshot( project.getVersion() )
                          && releaseDescriptor.isUpdateWorkingCopyVersions() ) )
            {
                return project.getVersion();
            }

            defaultVersion = getDevelopmentVersion( projectId, releaseDescriptor );
        }
        else
        {
            // no working copy modification
            if ( !( releaseDescriptor.isUpdateWorkingCopyVersions() ) )
            {
                return project.getVersion();
            }

            defaultVersion = getDevelopmentVersion( projectId, releaseDescriptor );
        }
        //@todo validate default version, maybe with DefaultArtifactVersion

        String suggestedVersion = null;
        String nextVersion = defaultVersion;
        String messageKey = null;
        try
        {
            while ( nextVersion == null || ArtifactUtils.isSnapshot( nextVersion ) != convertToSnapshot )
            {
                if ( suggestedVersion == null )
                {
                    String baseVersion = null;
                    if ( convertToSnapshot )
                    {
                        baseVersion = getReleaseVersion( projectId, releaseDescriptor );
                    }
                    // unspecified and unmapped version, so use project version
                    if ( baseVersion == null )
                    {
                        baseVersion = project.getVersion();
                    }

                    try
                    {
                        try
                        {
                            suggestedVersion =
                                resolveSuggestedVersion( baseVersion, releaseDescriptor.getProjectVersionPolicyId() );
                        }
                        catch ( VersionParseException e )
                        {
                            if ( releaseDescriptor.isInteractive() )
                            {
                                suggestedVersion =
                                    resolveSuggestedVersion( "1.0", releaseDescriptor.getProjectVersionPolicyId() );
                            }
                            else
                            {
                                throw new ReleaseExecutionException( "Error parsing version, cannot determine next "
                                    + "version: " + e.getMessage(), e );
                            }
                        }
                    }
                    catch ( PolicyException | VersionParseException e )
                    {
                        throw new ReleaseExecutionException( e.getMessage(), e );
                    }
                }

                if ( releaseDescriptor.isInteractive() )
                {
                    if ( messageKey == null )
                    {
                        messageKey = getMapversionPromptKey( releaseDescriptor );
                    }
                    String message =
                        MessageFormat.format( resourceBundle.getString( messageKey ), project.getName(), projectId );
                    nextVersion = prompter.prompt( message, suggestedVersion );

                  //@todo validate next version, maybe with DefaultArtifactVersion
                }
                else if ( defaultVersion == null )
                {
                    nextVersion = suggestedVersion;
                }
                else if ( convertToSnapshot )
                {
                    throw new ReleaseExecutionException( defaultVersion + " is invalid, expected a snapshot" );
                }
                else
                {
                    throw new ReleaseExecutionException( defaultVersion + " is invalid, expected a non-snapshot" );
                }
            }
        }
        catch ( PrompterException e )
        {
            throw new ReleaseExecutionException( "Error reading version from input handler: " + e.getMessage(), e );
        }
        return nextVersion;
    }

    private String resolveSuggestedVersion( String baseVersion, String policyId )
        throws PolicyException, VersionParseException
    {
        VersionPolicy policy = versionPolicies.get( policyId );
        if ( policy == null )
        {
            throw new PolicyException( "Policy '" + policyId + "' is unknown, available: " + versionPolicies.keySet() );
        }

        VersionPolicyRequest request = new VersionPolicyRequest().setVersion( baseVersion );
        return convertToSnapshot ? policy.getDevelopmentVersion( request ).getVersion()
                        : policy.getReleaseVersion( request ).getVersion();
    }

    private String getDevelopmentVersion( String projectId, ReleaseDescriptor releaseDescriptor )
    {
        String defaultVersion = releaseDescriptor.getDefaultDevelopmentVersion();
        if ( StringUtils.isEmpty( defaultVersion ) )
        {
            defaultVersion = releaseDescriptor.getProjectDevelopmentVersion( projectId );
        }
        return defaultVersion;
    }

    private String getReleaseVersion( String projectId, ReleaseDescriptor releaseDescriptor )
    {
        String nextVersion = releaseDescriptor.getDefaultReleaseVersion();
        if ( StringUtils.isEmpty( nextVersion ) )
        {
            nextVersion = releaseDescriptor.getProjectReleaseVersion( projectId );
        }
        return nextVersion;
    }


    private String getMapversionPromptKey( ReleaseDescriptor releaseDescriptor )
    {
        String messageKey;
        if ( convertToBranch )
        {
            messageKey = "mapversion.branch.prompt";
        }
        else if ( !convertToSnapshot )
        {
            messageKey = "mapversion.release.prompt";
        }
        else if ( releaseDescriptor.isBranchCreation() )
        {
            messageKey = "mapversion.workingcopy.prompt";
        }
        else
        {
            messageKey = "mapversion.development.prompt";
        }
        return messageKey;
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
    
    private ResourceBundle getResourceBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "release-messages", locale, MapVersionsPhase.class.getClassLoader() );
    }
}
