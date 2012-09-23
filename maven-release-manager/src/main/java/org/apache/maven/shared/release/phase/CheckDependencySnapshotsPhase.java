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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.versions.DefaultVersionInfo;
import org.apache.maven.shared.release.versions.VersionInfo;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

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
    public static final String RESOLVE_SNAPSHOT_MESSAGE = "There are still some remaining snapshot dependencies.\n";

    public static final String RESOLVE_SNAPSHOT_PROMPT = "Do you want to resolve them now?";

    public static final String RESOLVE_SNAPSHOT_TYPE_MESSAGE = "Dependency type to resolve,";

    public static final String RESOLVE_SNAPSHOT_TYPE_PROMPT =
        "specify the selection number ( 0:All 1:Project Dependencies 2:Plugins 3:Reports 4:Extensions ):";

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

    // Be aware of the difference between usedSnapshots and specifiedSnapshots:
    // UsedSnapshots end up on the classpath.
    // SpecifiedSnapshots are defined anywhere in the pom.
    // We'll probably need to introduce specifiedSnapshots as well.
    // @TODO MRELEASE-378: verify custom dependencies in plugins. Be aware of deprecated/removed Components in M3, such as PluginCollector
    // @TODO MRELEASE-763: verify all dependencies in inactive profiles
    private Set<Artifact> usedSnapshotDependencies = new HashSet<Artifact>();
    private Set<Artifact> usedSnapshotReports = new HashSet<Artifact>();
    private Set<Artifact> usedSnapshotExtensions = new HashSet<Artifact>();
    private Set<Artifact> usedSnapshotPlugins = new HashSet<Artifact>();

    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment, List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = new ReleaseResult();

        if ( !releaseDescriptor.isAllowTimestampedSnapshots() )
        {
            logInfo( result, "Checking dependencies and plugins for snapshots ..." );

            Map<String, String> originalVersions = releaseDescriptor.getOriginalVersions( reactorProjects );

            for ( MavenProject project : reactorProjects )
            {
                checkProject( project, originalVersions, releaseDescriptor );
            }
        }
        else
        {
            logInfo( result, "Ignoring SNAPSHOT depenedencies and plugins ..." );
        }
        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    private void checkProject( MavenProject project, Map<String, String> originalVersions, ReleaseDescriptor releaseDescriptor )
        throws ReleaseFailureException, ReleaseExecutionException
    {
        @SuppressWarnings( "unchecked" )
        Map<String, Artifact> artifactMap = ArtifactUtils.artifactMapByVersionlessId( project.getArtifacts() );

        if ( project.getParentArtifact() != null )
        {
            if ( checkArtifact( project.getParentArtifact(), originalVersions, artifactMap, releaseDescriptor ) )
            {
                usedSnapshotDependencies.add( project.getParentArtifact() );
            }
        }
        
        try
        {
            @SuppressWarnings( "unchecked" )
            Set<Artifact> dependencyArtifacts = project.createArtifacts( artifactFactory, null, null );
            checkDependencies( originalVersions, releaseDescriptor, artifactMap, dependencyArtifacts );
        }
        catch ( InvalidDependencyVersionException e )
        {
            throw new ReleaseExecutionException( "Failed to create dependency artifacts", e );
        }
        //@todo check dependencyManagement

        @SuppressWarnings( "unchecked" )
        Set<Artifact> pluginArtifacts = project.getPluginArtifacts();
        checkPlugins( originalVersions, releaseDescriptor, artifactMap, pluginArtifacts );
        //@todo check pluginManagement

        @SuppressWarnings( "unchecked" )
        Set<Artifact> reportArtifacts = project.getReportArtifacts();
        checkReports( originalVersions, releaseDescriptor, artifactMap, reportArtifacts );

        @SuppressWarnings( "unchecked" )
        Set<Artifact> extensionArtifacts = project.getExtensionArtifacts();
        checkExtensions( originalVersions, releaseDescriptor, artifactMap, extensionArtifacts );
        
        //@todo check profiles

        if ( !usedSnapshotDependencies.isEmpty() || !usedSnapshotReports.isEmpty()
                        || !usedSnapshotExtensions.isEmpty() || !usedSnapshotPlugins.isEmpty() )
        {
            if ( releaseDescriptor.isInteractive() )
            {
                resolveSnapshots( usedSnapshotDependencies, usedSnapshotReports, usedSnapshotExtensions,
                                  usedSnapshotPlugins, releaseDescriptor );
            }

            if ( !usedSnapshotDependencies.isEmpty() || !usedSnapshotReports.isEmpty()
                            || !usedSnapshotExtensions.isEmpty() || !usedSnapshotPlugins.isEmpty() )
            {
                StringBuilder message = new StringBuilder();

                printSnapshotDependencies( usedSnapshotDependencies, message );
                printSnapshotDependencies( usedSnapshotReports, message );
                printSnapshotDependencies( usedSnapshotExtensions, message );
                printSnapshotDependencies( usedSnapshotPlugins, message );
                message.append( "in project '" + project.getName() + "' (" + project.getId() + ")" );

                throw new ReleaseFailureException(
                    "Can't release project due to non released dependencies :\n" + message );
            }
        }
    }

    private void checkPlugins( Map<String, String> originalVersions, ReleaseDescriptor releaseDescriptor,
                               Map<String, Artifact> artifactMap, Set<Artifact> pluginArtifacts )
        throws ReleaseExecutionException
    {
        for ( Artifact artifact : pluginArtifacts )
        {
            if ( checkArtifact( artifact, originalVersions, artifactMap, releaseDescriptor ) )
            {
                boolean addToFailures;

                if ( "org.apache.maven.plugins".equals( artifact.getGroupId() ) && "maven-release-plugin".equals(
                    artifact.getArtifactId() ) )
                {
                    // It's a snapshot of the release plugin. Maybe just testing - ask
                    // By default, we fail as for any other plugin
                    if ( releaseDescriptor.isSnapshotReleasePluginAllowed() )
                    {
                        addToFailures = false;
                    }
                    else if ( releaseDescriptor.isInteractive() )
                    {
                        try
                        {
                            String result;
                            if ( !releaseDescriptor.isSnapshotReleasePluginAllowed() )
                            {
                                prompter.showMessage( "This project relies on a SNAPSHOT of the release plugin. "
                                                          + "This may be necessary during testing.\n" );
                                result = prompter.prompt( "Do you want to continue with the release?",
                                                          Arrays.asList( "yes", "no" ), "no" );
                            }
                            else
                            {
                                result = "yes";
                            }

                            if ( result.toLowerCase( Locale.ENGLISH ).startsWith( "y" ) )
                            {
                                addToFailures = false;
                                releaseDescriptor.setSnapshotReleasePluginAllowed( true );
                            }
                            else
                            {
                                addToFailures = true;
                            }
                        }
                        catch ( PrompterException e )
                        {
                            throw new ReleaseExecutionException( e.getMessage(), e );
                        }
                    }
                    else
                    {
                        addToFailures = true;
                    }
                }
                else
                {
                    addToFailures = true;
                }

                if ( addToFailures )
                {
                    usedSnapshotPlugins.add( artifact );
                }
            }
        }
    }

    private void checkDependencies( Map<String, String> originalVersions, ReleaseDescriptor releaseDescriptor,
                                    Map<String, Artifact> artifactMap, Set<Artifact> dependencyArtifacts )
    {
        for ( Artifact artifact : dependencyArtifacts )
        {
            if ( checkArtifact( artifact, originalVersions, artifactMap, releaseDescriptor ) )
            {
                usedSnapshotDependencies.add( getArtifactFromMap( artifact, artifactMap ) );
            }
        }
    }

    private void checkReports( Map<String, String> originalVersions, ReleaseDescriptor releaseDescriptor,
                               Map<String, Artifact> artifactMap, Set<Artifact> reportArtifacts )
    {
        for ( Artifact artifact : reportArtifacts )
        {
            if ( checkArtifact( artifact, originalVersions, artifactMap, releaseDescriptor ) )
            {
                //snapshotDependencies.add( artifact );
                usedSnapshotReports.add( artifact );
            }
        }
    }

    private void checkExtensions( Map<String, String> originalVersions, ReleaseDescriptor releaseDescriptor,
                                  Map<String, Artifact> artifactMap, Set<Artifact> extensionArtifacts )
    {
        for ( Artifact artifact : extensionArtifacts )
        {
            if ( checkArtifact( artifact, originalVersions, artifactMap, releaseDescriptor ) )
            {
                usedSnapshotExtensions.add( artifact );
            }
        }
    }

    private static boolean checkArtifact( Artifact artifact, Map<String, String> originalVersions, Map<String, Artifact> artifactMapByVersionlessId, ReleaseDescriptor releaseDescriptor )
    {
        Artifact checkArtifact = getArtifactFromMap( artifact, artifactMapByVersionlessId );

        return checkArtifact( checkArtifact, originalVersions, releaseDescriptor );
    }

    private static Artifact getArtifactFromMap( Artifact artifact, Map<String, Artifact> artifactMapByVersionlessId )
    {
        String versionlessId = ArtifactUtils.versionlessKey( artifact );
        Artifact checkArtifact = artifactMapByVersionlessId.get( versionlessId );

        if ( checkArtifact == null )
        {
            checkArtifact = artifact;
        }
        return checkArtifact;
    }

    private static boolean checkArtifact( Artifact artifact, Map<String, String> originalVersions, ReleaseDescriptor releaseDescriptor )
    {
        String versionlessArtifactKey = ArtifactUtils.versionlessKey( artifact.getGroupId(), artifact.getArtifactId() );

        // We are only looking at dependencies external to the project - ignore anything found in the reactor as
        // it's version will be updated
        boolean result =
            artifact.isSnapshot() && !artifact.getBaseVersion().equals( originalVersions.get( versionlessArtifactKey ) );

        // If we have a snapshot but allowTimestampedSnapshots is true, accept the artifact if the version
        // indicates that it is a timestamped snapshot.
        if ( result && releaseDescriptor.isAllowTimestampedSnapshots() )
        {
            result = artifact.getVersion().indexOf( Artifact.SNAPSHOT_VERSION ) >= 0;
        }

        return result;
    }

    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment, List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        // It makes no modifications, so simulate is the same as execute
        return execute( releaseDescriptor, releaseEnvironment, reactorProjects );
    }

    public void setPrompter( Prompter prompter )
    {
        this.prompter = prompter;
    }

    private StringBuilder printSnapshotDependencies( Set<Artifact> snapshotsSet, StringBuilder message )
    {
        List<Artifact> snapshotsList = new ArrayList<Artifact>( snapshotsSet );

        Collections.sort( snapshotsList );

        for ( Artifact artifact : snapshotsList )
        {
            message.append( "    " );

            message.append( artifact );

            message.append( "\n" );
        }

        return message;
    }

    private void resolveSnapshots( Set<Artifact> projectDependencies, Set<Artifact> reportDependencies, Set<Artifact> extensionDependencies,
                                   Set<Artifact> pluginDependencies, ReleaseDescriptor releaseDescriptor )
        throws ReleaseExecutionException
    {
        try
        {
            prompter.showMessage( RESOLVE_SNAPSHOT_MESSAGE );
            String result =
                prompter.prompt( RESOLVE_SNAPSHOT_PROMPT, Arrays.asList( "yes", "no" ), "no" );

            if ( result.toLowerCase( Locale.ENGLISH ).startsWith( "y" ) )
            {
                Map<String, Map<String, String>> resolvedSnapshots = null;
                prompter.showMessage( RESOLVE_SNAPSHOT_TYPE_MESSAGE );
                result = prompter.prompt( RESOLVE_SNAPSHOT_TYPE_PROMPT,
                                          Arrays.asList( "0", "1", "2", "3" ), "1" );

                switch ( Integer.parseInt( result.toLowerCase( Locale.ENGLISH ) ) )
                {
                    // all
                    case 0:
                        resolvedSnapshots = processSnapshot( projectDependencies );
                        resolvedSnapshots.putAll( processSnapshot( pluginDependencies ) );
                        resolvedSnapshots.putAll( processSnapshot( reportDependencies ) );
                        resolvedSnapshots.putAll( processSnapshot( extensionDependencies ) );
                        break;

                        // project dependencies
                    case 1:
                        resolvedSnapshots = processSnapshot( projectDependencies );
                        break;

                        // plugins
                    case 2:
                        resolvedSnapshots = processSnapshot( pluginDependencies );
                        break;

                        // reports
                    case 3:
                        resolvedSnapshots = processSnapshot( reportDependencies );
                        break;

                        // extensions
                    case 4:
                        resolvedSnapshots = processSnapshot( extensionDependencies );
                        break;
                }

                releaseDescriptor.getResolvedSnapshotDependencies().putAll( resolvedSnapshots );
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

    private Map<String, Map<String, String>> processSnapshot( Set<Artifact> snapshotSet )
        throws PrompterException, VersionParseException
    {
        Map<String, Map<String, String>> resolvedSnapshots = new HashMap<String, Map<String, String>>();
        Iterator<Artifact> iterator = snapshotSet.iterator();

        while ( iterator.hasNext() )
        {
            Artifact currentArtifact = iterator.next();
            String versionlessKey = ArtifactUtils.versionlessKey( currentArtifact );

            Map<String, String> versionMap = new HashMap<String, String>();
            VersionInfo versionInfo = new DefaultVersionInfo( currentArtifact.getVersion() );
            versionMap.put( ReleaseDescriptor.ORIGINAL_VERSION, versionInfo.toString() );

            prompter.showMessage(
                "Dependency '" + versionlessKey + "' is a snapshot (" + currentArtifact.getVersion() + ")\n" );
            String result = prompter.prompt( "Which release version should it be set to?",
                                             versionInfo.getReleaseVersionString() );
            versionMap.put( ReleaseDescriptor.RELEASE_KEY, result );

            iterator.remove();

            // by default, keep the same version for the dependency after release, unless it was previously newer
            // the user may opt to type in something different
            VersionInfo nextVersionInfo = new DefaultVersionInfo( result );

            String nextVersion;
            if ( nextVersionInfo.compareTo( versionInfo ) > 0 )
            {
                nextVersion = nextVersionInfo.toString();
            }
            else
            {
                nextVersion = versionInfo.toString();
            }

            result = prompter.prompt( "What version should the dependency be reset to for development?", nextVersion );
            versionMap.put( ReleaseDescriptor.DEVELOPMENT_KEY, result );

            resolvedSnapshots.put( versionlessKey, versionMap );
        }

        return resolvedSnapshots;
    }
}