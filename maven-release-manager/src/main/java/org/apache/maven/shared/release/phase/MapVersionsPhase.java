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
import java.util.ResourceBundle;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.apache.maven.shared.release.versions.DefaultVersionInfo;
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
 * <p>
 * <table>
 *   <tr>
 *     <th>MapVersionsPhase field</th><th>map-release-versions</th><th>map-branch-versions</th><th>map-development-versions</th>
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

    void setPrompter( Prompter prompter )
    {
        this.prompter = prompter;
    }

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

            if ( convertToSnapshot )
            {
                if ( releaseDescriptor.isBranchCreation() && convertToBranch )
                {
                    releaseDescriptor.mapReleaseVersion( projectId, nextVersion );
                }
                else
                {
                    releaseDescriptor.mapDevelopmentVersion( projectId, nextVersion );
                }
            }
            else
            {
                releaseDescriptor.mapReleaseVersion( projectId, nextVersion );
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
                        releaseDescriptor.mapReleaseVersion( subProjectId, v );
                    }
                    else
                    {
                        releaseDescriptor.mapDevelopmentVersion( subProjectId, v );
                    }
                }
                else
                {
                    releaseDescriptor.mapReleaseVersion( subProjectId, nextVersion );
                }
            }
        }
        else
        {
            for ( MavenProject project : reactorProjects )
            {
                String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

                String nextVersion = resolveNextVersion( project, projectId, releaseDescriptor, result );

                if ( convertToSnapshot )
                {
                    if ( releaseDescriptor.isBranchCreation() && convertToBranch )
                    {
                        releaseDescriptor.mapReleaseVersion( projectId, nextVersion );
                    }
                    else
                    {
                        releaseDescriptor.mapDevelopmentVersion( projectId, nextVersion );
                    }
                }
                else
                {
                    releaseDescriptor.mapReleaseVersion( projectId, nextVersion );
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
                          && ( ArtifactUtils.isSnapshot( project.getVersion() ) || releaseDescriptor.isUpdateVersionsToSnapshot() ) ) )
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
                    DefaultVersionInfo versionInfo;
                    try
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
                        versionInfo = new DefaultVersionInfo( baseVersion );
                    }
                    catch ( VersionParseException e )
                    {
                        if ( releaseDescriptor.isInteractive() )
                        {
                            try
                            {
                                versionInfo = new DefaultVersionInfo( "1.0" );
                            }
                            catch ( VersionParseException e1 )
                            {
                                // if that happens we are in serious trouble!
                                throw new ReleaseExecutionException( "Version 1.0 could not be parsed!", e1 );
                            }
                        }
                        else
                        {
                            throw new ReleaseExecutionException(
                                                                 "Error parsing version, cannot determine next version: "
                                                                     + e.getMessage(), e );
                        }
                    }
                    suggestedVersion =
                        convertToSnapshot ? versionInfo.getNextVersion().getSnapshotVersionString()
                                        : versionInfo.getReleaseVersionString(); 
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
                else
                {
                    nextVersion = suggestedVersion;
                }
            }
        }
        catch ( PrompterException e )
        {
            throw new ReleaseExecutionException( "Error reading version from input handler: " + e.getMessage(), e );
        }
        return nextVersion;
    }

    private String getDevelopmentVersion( String projectId, ReleaseDescriptor releaseDescriptor )
    {
        String defaultVersion;
        defaultVersion = releaseDescriptor.getDefaultDevelopmentVersion();
        if ( StringUtils.isEmpty( defaultVersion ) )
        {
            defaultVersion = ( String ) releaseDescriptor.getDevelopmentVersions().get( projectId );
        }
        return defaultVersion;
    }

    private String getReleaseVersion( String projectId, ReleaseDescriptor releaseDescriptor )
    {
        String nextVersion = releaseDescriptor.getDefaultReleaseVersion();
        if ( StringUtils.isEmpty( nextVersion ) )
        {
            nextVersion = ( String ) releaseDescriptor.getReleaseVersions().get( projectId );
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
        else if ( convertToSnapshot )
        {
            if ( releaseDescriptor.isBranchCreation() )
            {
                messageKey = "mapversion.workingcopy.prompt";
            }
            else
            {
                messageKey = "mapversion.development.prompt";
            }
        }
        else
        {
            messageKey = "mapversion.release.prompt";
        }
        return messageKey;
    }

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
}
