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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.versions.DefaultVersionInfo;
import org.apache.maven.shared.release.versions.VersionInfo;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Check the dependencies of all projects being released to see if there are any unreleased snapshots.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @todo plugins with no version will be resolved to RELEASE which is not a snapshot, but remains unresolved to this point. This is a potential hole in the check, and should be revisited after the release pom writing is done and resolving versions to verify whether it is.
 * @todo plugins injected by the lifecycle are not tested here. They will be injected with a RELEASE version so are covered under the above point.
 * @plexus.component role="org.apache.maven.shared.release.phase.ReleasePhase" role-hint="check-dependency-snapshots"
 */
public class CheckDependencySnapshotsPhase
    extends AbstractReleasePhase
{
    public static final String RESOLVE_SNAPSHOT_MESSAGE = "There are still some remaining snapshot dependencies.";

    public static final String RESOLVE_SNAPSHOT_PROMPT = "Do you want to resolve them now?";

    public static final String RESOLVE_SNAPSHOT_TYPE_MESSAGE = "Dependency type to resolve,";

    public static final String RESOLVE_SNAPSHOT_TYPE_PROMPT =
        "specify the selection number ( 0:All 1:Project Dependencies 2:Plugins 3:Reports 4:Extensions ):";

    public static final String RESOLVE_ALL_SNAPSHOT_MESSAGE = "Resolve All Snapshots.";

    public static final String RESOLVE_ALL_PROJECT_DEPENDENCIES_SNAPSHOT_MESSAGE =
        "Resolve Project Dependency Snapshots.";

    public static final String RESOLVE_ALL_REPORTS_SNAPSHOT_MESSAGE = "Resolve Report Dependency Snapshots.";

    public static final String RESOLVE_ALL_EXTENSIONS_SNAPSHOT_MESSAGE = "Resolve Extension Dependency Snapshots.";

    public static final String RESOLVE_ALL_PLUGIN_SNAPSHOT_MESSAGE = "Resolve Plugin Dependency Snapshots.";

    /**
     * Component used to prompt for input.
     *
     * @plexus.requirement
     */
    private Prompter prompter;

    /**
     * Component used to create artifacts
     *
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = new ReleaseResult();

        logInfo( result, "Checking dependencies and plugins for snapshots ..." );

        Map originalVersions = releaseDescriptor.getOriginalVersions( reactorProjects );

        for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();

            checkProject( project, originalVersions, releaseDescriptor );
        }

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    private void checkProject( MavenProject project, Map originalVersions, ReleaseDescriptor releaseDescriptor )
        throws ReleaseFailureException, ReleaseExecutionException
    {
        Map artifactMap = ArtifactUtils.artifactMapByVersionlessId( project.getArtifacts() );
        
        Set snapshotDependencies = new HashSet();
        Set snapshotReportDependencies = new HashSet();
        Set snapshotExtensionsDependencies = new HashSet();
        Set snapshotPluginDependencies = new HashSet();

        if ( project.getParentArtifact() != null )
        {
            if ( checkArtifact( project.getParentArtifact(), originalVersions, artifactMap ) )
            {
                snapshotDependencies.add( project.getParentArtifact() );
            }
        }

        try
        {
            Set dependencyArtifacts = project.createArtifacts( artifactFactory, null, null );

            for ( Iterator i = dependencyArtifacts.iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();

                if ( checkArtifact( artifact, originalVersions, artifactMap ) )
                {
                    snapshotDependencies.add( artifact );
                }
            }
        }
        catch ( InvalidDependencyVersionException e )
        {
            throw new ReleaseExecutionException( "Failed to create dependency artifacts", e );
        }

        for ( Iterator i = project.getPluginArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            if ( checkArtifact( artifact, originalVersions, artifactMap ) )
            {
                boolean addToFailures = true;

                if ( "org.apache.maven.plugins".equals( artifact.getGroupId() ) &&
                    "maven-release-plugin".equals( artifact.getArtifactId() ) )
                {
                    // It's a snapshot of the release plugin. Maybe just testing - ask
                    // By default, we fail as for any other plugin
                    if ( releaseDescriptor.isInteractive() )
                    {
                        try
                        {
                            String result = "no";
                            if ( !releaseDescriptor.isSnapshotReleasePluginAllowed() )
                            {
                                prompter.showMessage(
                                    "This project relies on a SNAPSHOT of the release plugin. This may be necessary during testing." );
                                result = prompter.prompt( "Do you want to continue with the release?",
                                                          Arrays.asList( new String[]{"yes", "no"} ), "no" );
                            }
                            else
                            {
                                result = "yes";
                            }

                            if ( result.toLowerCase().startsWith( "y" ) )
                            {
                                addToFailures = false;
                                releaseDescriptor.setSnapshotReleasePluginAllowed( true );
                            }
                        }
                        catch ( PrompterException e )
                        {
                            throw new ReleaseExecutionException( e.getMessage(), e );
                        }
                    }
                }

                if ( addToFailures )
                {
                    snapshotPluginDependencies.add( artifact );
                }
            }
        }

        for ( Iterator i = project.getReportArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            if ( checkArtifact( artifact, originalVersions, artifactMap ) )
            {
                //snapshotDependencies.add( artifact );
                snapshotReportDependencies.add( artifact );
            }
        }

        for ( Iterator i = project.getExtensionArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            if ( checkArtifact( artifact, originalVersions, artifactMap ) )
            {
                snapshotExtensionsDependencies.add( artifact );
            }
        }

        if ( !snapshotDependencies.isEmpty() || !snapshotReportDependencies.isEmpty() ||
            !snapshotExtensionsDependencies.isEmpty() || !snapshotPluginDependencies.isEmpty() )
        {
            if ( releaseDescriptor.isInteractive() )
            {
                resolveSnapshots( snapshotDependencies, snapshotReportDependencies, snapshotExtensionsDependencies,
                                  snapshotPluginDependencies, releaseDescriptor );
            }

            if ( !snapshotDependencies.isEmpty() || !snapshotReportDependencies.isEmpty() ||
                !snapshotExtensionsDependencies.isEmpty() || !snapshotPluginDependencies.isEmpty() )
            {
                StringBuffer message = new StringBuffer();

                printSnapshotDependencies( snapshotDependencies, message );
                printSnapshotDependencies( snapshotReportDependencies, message );
                printSnapshotDependencies( snapshotExtensionsDependencies, message );
                printSnapshotDependencies( snapshotPluginDependencies, message );
                message.append( "in project '" + project.getName() + "' (" + project.getId() + ")" );

                throw new ReleaseFailureException(
                    "Can't release project due to non released dependencies :\n" + message );
            }
        }
    }

    private static boolean checkArtifact( Artifact artifact, Map originalVersions, Map artifactMapByVersionlessId )
    {
        String versionlessId = ArtifactUtils.versionlessKey( artifact );
        Artifact checkArtifact = (Artifact) artifactMapByVersionlessId.get( versionlessId );
        
        if ( checkArtifact == null)
        {
            checkArtifact = artifact;
        }
        
        return checkArtifact( checkArtifact, originalVersions );
    }
    
    private static boolean checkArtifact( Artifact artifact, Map originalVersions )
    {
        String versionlessArtifactKey = ArtifactUtils.versionlessKey( artifact.getGroupId(), artifact.getArtifactId() );

        // We are only looking at dependencies external to the project - ignore anything found in the reactor as
        // it's version will be updated
        return artifact.isSnapshot() &&
            !artifact.getBaseVersion().equals( originalVersions.get( versionlessArtifactKey ) );
    }

    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        // It makes no modifications, so simulate is the same as execute
        return execute( releaseDescriptor, settings, reactorProjects );
    }

    public void setPrompter( Prompter prompter )
    {
        this.prompter = prompter;
    }

    private StringBuffer printSnapshotDependencies( Set snapshotsSet, StringBuffer message )
    {
        List snapshotsList = new ArrayList( snapshotsSet );

        Collections.sort( snapshotsList );

        for ( Iterator i = snapshotsList.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            message.append( "    " );

            message.append( artifact );

            message.append( "\n" );
        }

        return message;
    }

    private void resolveSnapshots( Set projectDependencies, Set reportDependencies, Set extensionDependencies,
                                   Set pluginDependencies, ReleaseDescriptor releaseDescriptor )
        throws ReleaseExecutionException
    {
        try
        {
            prompter.showMessage( RESOLVE_SNAPSHOT_MESSAGE );
            String result =
                prompter.prompt( RESOLVE_SNAPSHOT_PROMPT, Arrays.asList( new String[]{"yes", "no"} ), "no" );

            if ( result.toLowerCase().startsWith( "y" ) )
            {
                Set snapshotSet = new HashSet();
                Map resolvedSnapshots = null;
                prompter.showMessage( RESOLVE_SNAPSHOT_TYPE_MESSAGE );
                result = prompter.prompt( RESOLVE_SNAPSHOT_TYPE_PROMPT,
                                          Arrays.asList( new String[]{"0", "1", "2", "3"} ), "1" );

                switch ( Integer.parseInt( result.toLowerCase() ) )
                {
                    // all
                    case 0:
                        prompter.showMessage( RESOLVE_ALL_SNAPSHOT_MESSAGE );
                        snapshotSet.addAll( projectDependencies );
                        snapshotSet.addAll( reportDependencies );
                        snapshotSet.addAll( extensionDependencies );
                        snapshotSet.addAll( pluginDependencies );
                        resolvedSnapshots = processSnapshot( snapshotSet );
                        break;

                        // project dependencies
                    case 1:
                        prompter.showMessage( RESOLVE_ALL_PROJECT_DEPENDENCIES_SNAPSHOT_MESSAGE );
                        resolvedSnapshots = processSnapshot( projectDependencies );
                        break;

                        // plugins
                    case 2:
                        prompter.showMessage( RESOLVE_ALL_PLUGIN_SNAPSHOT_MESSAGE );
                        resolvedSnapshots = processSnapshot( pluginDependencies );
                        break;

                        // reports
                    case 3:
                        prompter.showMessage( RESOLVE_ALL_REPORTS_SNAPSHOT_MESSAGE );
                        resolvedSnapshots = processSnapshot( reportDependencies );
                        break;

                        // extensions
                    case 4:
                        prompter.showMessage( RESOLVE_ALL_EXTENSIONS_SNAPSHOT_MESSAGE );
                        resolvedSnapshots = processSnapshot( extensionDependencies );
                        break;
                }

                releaseDescriptor.setResolvedSnapshotDependencies( resolvedSnapshots );
            }
        }
        catch ( PrompterException e )
        {
            throw new ReleaseExecutionException( e.getMessage(), e );
        }
        catch ( VersionParseException e )
        {
            throw new ReleaseExecutionException( e.getMessage(), e );
        }
    }

    private Map processSnapshot( Set snapshotSet )
        throws PrompterException, VersionParseException
    {
        Map resolvedSnapshots = new HashMap();
        Iterator iterator = snapshotSet.iterator();
        Artifact currentArtifact;
        String result;
        VersionInfo version;

        while ( iterator.hasNext() )
        {
            currentArtifact = (Artifact) iterator.next();
            version = new DefaultVersionInfo( currentArtifact.getVersion() );

            result = prompter.prompt( "'" + ArtifactUtils.versionlessKey( currentArtifact ) + "' set to release?",
                                      Arrays.asList( new String[]{"yes", "no"} ), "yes" );

            if ( result.toLowerCase().startsWith( "y" ) )
            {
                VersionInfo nextDevelopmentVersion;
                Map versionMap = new HashMap();

                iterator.remove();

                VersionInfo versionInfo = version.getNextVersion();
                String nextVersion;
                if ( versionInfo != null )
                {
                    nextVersion = versionInfo.getSnapshotVersionString();
                }
                else
                {
                    nextVersion = "1.0-SNAPSHOT";
                }
                result = prompter.prompt( "What is the next development version?",
                                          Collections.singletonList( nextVersion ), nextVersion );

                nextDevelopmentVersion = new DefaultVersionInfo( result );
                versionMap.put( ReleaseDescriptor.ORIGINAL_VERSION, version.toString() );
                versionMap.put( ReleaseDescriptor.DEVELOPMENT_KEY, nextDevelopmentVersion.getSnapshotVersionString() );
                versionMap.put( ReleaseDescriptor.RELEASE_KEY, version.getReleaseVersionString() );

                resolvedSnapshots.put( ArtifactUtils.versionlessKey( currentArtifact ), versionMap );
            }
        }

        return resolvedSnapshots;
    }
}
