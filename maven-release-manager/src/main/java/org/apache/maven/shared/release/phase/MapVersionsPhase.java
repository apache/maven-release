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

import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.apache.maven.shared.release.versions.DefaultVersionInfo;
import org.apache.maven.shared.release.versions.VersionInfo;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

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

    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                  List<MavenProject> reactorProjects )
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

            for ( Iterator<MavenProject> i = reactorProjects.iterator(); i.hasNext(); )
            {
                MavenProject subProject = i.next();
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
            for ( Iterator<MavenProject> i = reactorProjects.iterator(); i.hasNext(); )
            {
                MavenProject project = i.next();

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
        boolean releaseVersionIsExplicit = false;

        VersionInfo nextSnapshotVersionInfo = null;
        boolean nextSnapshotVersionIsExplicit = false;

        try
        {
            currentVersionInfo = new DefaultVersionInfo( project.getVersion() );

            // The release/branch version defaults to currentVersionInfo (snapshot for branch, and release for tag)
            releaseVersionInfo = currentVersionInfo;

            // Check if the user specified a release version
            if ( releaseDescriptor.getDefaultReleaseVersion() != null )
            {
                releaseVersionInfo = new DefaultVersionInfo( releaseDescriptor.getDefaultReleaseVersion() );
                releaseVersionIsExplicit = true;
            }
            if ( releaseDescriptor.getReleaseVersions() != null )
            {
                String releaseVersion = ( String ) releaseDescriptor.getReleaseVersions().get( projectId );
                if ( releaseVersion != null )
                {
                    releaseVersionInfo = new DefaultVersionInfo( releaseVersion );
                    releaseVersionIsExplicit = true;
                }
            }

            // The next snapshot version defaults to the next version after the release version
            nextSnapshotVersionInfo = releaseVersionInfo.getNextVersion();

            // Check if the user specified a new snapshot version
            if ( releaseDescriptor.getDefaultDevelopmentVersion() != null )
            {
                nextSnapshotVersionInfo = new DefaultVersionInfo( releaseDescriptor.getDefaultDevelopmentVersion() );
                nextSnapshotVersionIsExplicit = true;
            }
            if ( releaseDescriptor.getDevelopmentVersions() != null )
            {
                String nextDevVersion = ( String ) releaseDescriptor.getDevelopmentVersions().get( projectId );
                if ( nextDevVersion != null )
                {
                    nextSnapshotVersionInfo = new DefaultVersionInfo( nextDevVersion );
                    nextSnapshotVersionIsExplicit = true;
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

                // set defaults for resume in interactive mode
                if ( releaseVersionInfo == null )
                {
                    try
                    {
                        releaseVersionInfo = new DefaultVersionInfo( "1.0" );
                    }
                    catch ( VersionParseException e1 )
                    {
                        // if that happens we are in serious trouble!
                        throw new ReleaseExecutionException( "Version 1.0 could not be parsed!", e1 );
                    }
                }

                if ( nextSnapshotVersionInfo == null )
                {
                    nextSnapshotVersionInfo = releaseVersionInfo.getNextVersion();
                }
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
                        // branch modification
                        if ( releaseDescriptor.isUpdateBranchVersions()
                            && ( ArtifactUtils.isSnapshot( project.getVersion() ) || releaseDescriptor.isUpdateVersionsToSnapshot() ) )
                        {
                            nextVersion = releaseVersionInfo.getSnapshotVersionString();
                            if ( !releaseVersionIsExplicit && releaseDescriptor.isInteractive() )
                            {
                                nextVersion = prompter.prompt(
                                    "What is the branch version for \"" + project.getName() + "\"? (" + projectId + ")",
                                    nextVersion );
                            }
                        }
                        else
                        {
                            nextVersion = project.getVersion();
                        }

                    }
                    else
                    {
                        // working copy modification
                        if ( ArtifactUtils.isSnapshot( project.getVersion() )
                            && releaseDescriptor.isUpdateWorkingCopyVersions() )
                        {
                            nextVersion = nextSnapshotVersionInfo.getSnapshotVersionString();
                            if ( releaseDescriptor.isInteractive() && !nextSnapshotVersionIsExplicit )
                            {
                                nextVersion =
                                    prompter.prompt( "What is the new working copy version for \"" + project.getName()
                                        + "\"? (" + projectId + ")", nextVersion );
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
                    nextVersion = nextSnapshotVersionInfo.getSnapshotVersionString();
                    if ( releaseDescriptor.isInteractive()  && !nextSnapshotVersionIsExplicit )
                    {
                        nextVersion =
                            prompter.prompt( "What is the new development version for \"" + project.getName() + "\"? ("
                                + projectId + ")", nextVersion );
                    }
                }
            }
            else
            {
                if ( ArtifactUtils.isSnapshot( project.getVersion() ) )
                {
                    nextVersion = releaseVersionInfo.getReleaseVersionString();

                    if ( releaseDescriptor.isInteractive() && !releaseVersionIsExplicit )
                    {
                        nextVersion = prompter.prompt(
                            "What is the release version for \"" + project.getName() + "\"? (" + projectId + ")",
                            nextVersion );
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
