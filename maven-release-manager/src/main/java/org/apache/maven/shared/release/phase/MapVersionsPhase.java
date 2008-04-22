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

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.apache.maven.shared.release.versions.DefaultVersionInfo;
import org.apache.maven.shared.release.versions.VersionInfo;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Map projects to their new versions after release / into the next development cycle.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class MapVersionsPhase
    extends AbstractReleasePhase
{
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

    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
        throws ReleaseExecutionException
    {
        ReleaseResult result = new ReleaseResult();

        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );

        if ( releaseDescriptor.isAutoVersionSubmodules() && ArtifactUtils.isSnapshot( rootProject.getVersion() ) )
        {
            // get the root project
            MavenProject project = rootProject;

            String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

            String nextVersion = getNextVersion( project, projectId, releaseDescriptor, result );

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

            for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
            {
                MavenProject subProject = (MavenProject) i.next();
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
            for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
            {
                MavenProject project = (MavenProject) i.next();

                String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

                String nextVersion = getNextVersion( project, projectId, releaseDescriptor, result );

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

    private String getNextVersion( MavenProject project, String projectId, ReleaseDescriptor releaseDescriptor,
                                   ReleaseResult result )
        throws ReleaseExecutionException
    {
        String nextVersion = null;

        VersionInfo currentVersionInfo = null;
        VersionInfo releaseVersionInfo = null;
        VersionInfo nextSnapshotVersionInfo = null;
        
        try
        {
            currentVersionInfo = new DefaultVersionInfo( project.getVersion() );

            // The release version defaults to currentVersionInfo.getReleaseVersionString()
            releaseVersionInfo = currentVersionInfo;
            
            // Check if the user specified a release version
            if ( releaseDescriptor.getReleaseVersions() != null )
            {
                String releaseVersion = ( String ) releaseDescriptor.getReleaseVersions().get( projectId );
                if ( releaseVersion != null )
                {
                    releaseVersionInfo = new DefaultVersionInfo( releaseVersion );
                }
            }

            if ( releaseVersionInfo != null )
            {
                nextSnapshotVersionInfo = releaseVersionInfo.getNextVersion();
            }
            
            // Check if the user specified a new snapshot version
            if ( releaseDescriptor.getDevelopmentVersions() != null )
            {
                String nextDevVersion = ( String ) releaseDescriptor.getDevelopmentVersions().get( projectId );
                if ( nextDevVersion != null )
                {
                    nextSnapshotVersionInfo = new DefaultVersionInfo( nextDevVersion );
                }
            }
            
        }
        catch ( VersionParseException e )
        {
            String msg = "Error parsing version, cannot determine next version: " + e.getMessage();
            if ( releaseDescriptor.isInteractive() )
            {
                logWarn( result, msg );
                logDebug( result, e.getMessage(), e );
            }
            else
            {
                // cannot proceed without a next value in batch mode
                throw new ReleaseExecutionException( msg, e );
            }
        }

        try
        {
            if ( convertToSnapshot )
            {
                if ( releaseDescriptor.isBranchCreation() )
                {
                    if ( convertToBranch )
                    {
                        //branch modification
                        if ( releaseDescriptor.isUpdateBranchVersions() && (
                            ArtifactUtils.isSnapshot( project.getVersion() ) ||
                                releaseDescriptor.isUpdateVersionsToSnapshot() ) )
                        {
                            if ( currentVersionInfo != null )
                            {
                                nextVersion = currentVersionInfo.getSnapshotVersionString();
                            }

                            if ( releaseDescriptor.isInteractive() )
                            {
                                nextVersion = prompter.prompt(
                                    "What is the branch version for \"" + project.getName() + "\"? (" + projectId + ")",
                                    nextVersion );
                            }
                            else
                            {
                                Map relVersions = releaseDescriptor.getDevelopmentVersions();
                                if ( relVersions.containsKey( projectId ) )
                                {
                                    nextVersion = relVersions.remove( projectId ).toString();
                                }
                            }
                        }
                        else
                        {
                            nextVersion = project.getVersion();
                        }

                    }
                    else
                    {
                        //working copy modification
                        if ( ArtifactUtils.isSnapshot( project.getVersion() ) &&
                            releaseDescriptor.isUpdateWorkingCopyVersions() )
                        {
                            if ( currentVersionInfo != null )
                            {
                                VersionInfo versionInfo = currentVersionInfo.getNextVersion();
                                if ( versionInfo != null )
                                {
                                    nextVersion = versionInfo.getSnapshotVersionString();
                                }
                                else
                                {
                                    nextVersion = "1.0-SNAPSHOT";
                                }
                            }

                            if ( releaseDescriptor.isInteractive() )
                            {
                                nextVersion = prompter.prompt( "What is the new working copy version for \"" +
                                    project.getName() + "\"? (" + projectId + ")", nextVersion );
                            }
                            else
                            {
                                Map devVersions = releaseDescriptor.getDevelopmentVersions();
                                if ( devVersions.containsKey( projectId ) )
                                {
                                    nextVersion = devVersions.remove( projectId ).toString();
                                }
                            }
                        }
                        else
                        {
                            nextVersion = project.getVersion();
                        }
                    }
                }
                else
                {
                    if ( currentVersionInfo != null )
                    {
                        if ( nextSnapshotVersionInfo != null )
                        {
                            nextVersion = nextSnapshotVersionInfo.getSnapshotVersionString();
                        }
                        else
                        {
                            nextVersion = "1.0-SNAPSHOT";
                        }
                    }

                    if ( releaseDescriptor.isInteractive() )
                    {
                        nextVersion = prompter.prompt( "What is the new development version for \"" +
                            project.getName() + "\"? (" + projectId + ")", nextVersion );
                    }
                    else
                    {
                        Map devVersions = releaseDescriptor.getDevelopmentVersions();
                        if ( devVersions.containsKey( projectId ) )
                        {
                            nextVersion = devVersions.remove( projectId ).toString();
                        }
                    }
                }
            }
            else
            {
                if ( ArtifactUtils.isSnapshot( project.getVersion() ) )
                {
                    if ( releaseVersionInfo != null )
                    {
                        nextVersion = releaseVersionInfo.getReleaseVersionString();
                    }

                    if ( releaseDescriptor.isInteractive() )
                    {
                        nextVersion = prompter.prompt(
                            "What is the release version for \"" + project.getName() + "\"? (" + projectId + ")",
                            nextVersion );
                    }
                    else
                    {
                        Map relVersions = releaseDescriptor.getReleaseVersions();
                        if ( relVersions.containsKey( projectId ) )
                        {
                            nextVersion = relVersions.remove( projectId ).toString();
                        }
                    }
                }
                else
                {
                    nextVersion = project.getVersion();
                }
            }
        }
        catch ( PrompterException e )
        {
            throw new ReleaseExecutionException( "Error reading version from input handler: " + e.getMessage(), e );
        }

        return nextVersion;
    }

    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
        throws ReleaseExecutionException
    {
        ReleaseResult result = new ReleaseResult();

        // It makes no modifications, so simulate is the same as execute
        execute( releaseDescriptor, settings, reactorProjects );

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }
}
