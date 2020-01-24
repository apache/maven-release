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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.versions.DefaultVersionInfo;
import org.apache.maven.shared.release.versions.VersionInfo;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

/**
 * Check the dependencies of all projects being released to see if there are any unreleased snapshots.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
 // TODO plugins with no version will be resolved to RELEASE which is not a snapshot, but remains unresolved to this point. This is a potential hole in the check, and should be revisited after the release pom writing is done and resolving versions to verify whether it is.
 // TODO plugins injected by the lifecycle are not tested here. They will be injected with a RELEASE version so are covered under the above point.
@Component( role = ReleasePhase.class, hint = "check-dependency-snapshots" )
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
     */
    @Requirement
    private Prompter prompter;
    
    // Be aware of the difference between usedSnapshots and specifiedSnapshots:
    // UsedSnapshots end up on the classpath.
    // SpecifiedSnapshots are defined anywhere in the pom.
    // We'll probably need to introduce specifiedSnapshots as well.
    // @TODO MRELEASE-378: verify custom dependencies in plugins. Be aware of deprecated/removed Components in M3, such as PluginCollector
    // @TODO MRELEASE-763: verify all dependencies in inactive profiles
    
    // Don't prompt for every project in reactor, remember state of questions
    private String resolveSnapshot;

    private String resolveSnapshotType;
    
    @Override
    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                  List<MavenProject> reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = new ReleaseResult();

        if ( !releaseDescriptor.isAllowTimestampedSnapshots() )
        {
            logInfo( result, "Checking dependencies and plugins for snapshots ..." );

            for ( MavenProject project : reactorProjects )
            {
                checkProject( project, releaseDescriptor );
            }
        }
        else
        {
            logInfo( result, "Ignoring SNAPSHOT depenedencies and plugins ..." );
        }
        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    private void checkProject( MavenProject project, ReleaseDescriptor releaseDescriptor )
        throws ReleaseFailureException, ReleaseExecutionException
    {
        Map<String, Artifact> artifactMap = ArtifactUtils.artifactMapByVersionlessId( project.getArtifacts() );
        
        Set<Artifact> usedSnapshotDependencies = new HashSet<>();

        if ( project.getParentArtifact() != null )
        {
            if ( checkArtifact( project.getParentArtifact(), artifactMap, releaseDescriptor ) )
            {
                usedSnapshotDependencies.add( project.getParentArtifact() );
            }
        }

        Set<Artifact> dependencyArtifacts = project.getDependencyArtifacts();
        usedSnapshotDependencies.addAll( checkDependencies( releaseDescriptor, artifactMap, dependencyArtifacts ) );

        //@todo check dependencyManagement

        Set<Artifact> pluginArtifacts = project.getPluginArtifacts();
        Set<Artifact> usedSnapshotPlugins = checkPlugins( releaseDescriptor, artifactMap, pluginArtifacts );

        //@todo check pluginManagement

        Set<Artifact> reportArtifacts = project.getReportArtifacts();
        Set<Artifact> usedSnapshotReports = checkReports( releaseDescriptor, artifactMap, reportArtifacts );

        Set<Artifact> extensionArtifacts = project.getExtensionArtifacts();
        Set<Artifact> usedSnapshotExtensions = checkExtensions( releaseDescriptor, artifactMap, extensionArtifacts );

        //@todo check profiles

        if ( !usedSnapshotDependencies.isEmpty() || !usedSnapshotReports.isEmpty()
                        || !usedSnapshotExtensions.isEmpty() || !usedSnapshotPlugins.isEmpty() )
        {
            if ( releaseDescriptor.isInteractive() || null != releaseDescriptor.getAutoResolveSnapshots() )
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

    private Set<Artifact> checkPlugins( ReleaseDescriptor releaseDescriptor,
                               Map<String, Artifact> artifactMap, Set<Artifact> pluginArtifacts )
        throws ReleaseExecutionException
    {
        Set<Artifact> usedSnapshotPlugins = new HashSet<>();
        for ( Artifact artifact : pluginArtifacts )
        {
            if ( checkArtifact( artifact, artifactMap, releaseDescriptor ) )
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
        return usedSnapshotPlugins;
    }

    private Set<Artifact> checkDependencies( ReleaseDescriptor releaseDescriptor,
                                    Map<String, Artifact> artifactMap, 
                                    Set<Artifact> dependencyArtifacts )
    {
        Set<Artifact> usedSnapshotDependencies = new HashSet<>();
        for ( Artifact artifact : dependencyArtifacts )
        {
            if ( checkArtifact( artifact, artifactMap, releaseDescriptor ) )
            {
                usedSnapshotDependencies.add( getArtifactFromMap( artifact, artifactMap ) );
            }
        }
        return usedSnapshotDependencies;
    }

    private Set<Artifact> checkReports( ReleaseDescriptor releaseDescriptor,
                               Map<String, Artifact> artifactMap, Set<Artifact> reportArtifacts )
    {
        Set<Artifact> usedSnapshotReports = new HashSet<>();
        for ( Artifact artifact : reportArtifacts )
        {
            if ( checkArtifact( artifact, artifactMap, releaseDescriptor ) )
            {
                //snapshotDependencies.add( artifact );
                usedSnapshotReports.add( artifact );
            }
        }
        return usedSnapshotReports;
    }

    private Set<Artifact> checkExtensions( ReleaseDescriptor releaseDescriptor,
                                  Map<String, Artifact> artifactMap, Set<Artifact> extensionArtifacts )
    {
        Set<Artifact> usedSnapshotExtensions = new HashSet<>();
        for ( Artifact artifact : extensionArtifacts )
        {
            if ( checkArtifact( artifact, artifactMap, releaseDescriptor ) )
            {
                usedSnapshotExtensions.add( artifact );
            }
        }
        return usedSnapshotExtensions;
    }

    private static boolean checkArtifact( Artifact artifact,
                                          Map<String, Artifact> artifactMapByVersionlessId,
                                          ReleaseDescriptor releaseDescriptor )
    {
        Artifact checkArtifact = getArtifactFromMap( artifact, artifactMapByVersionlessId );

        return checkArtifact( checkArtifact, releaseDescriptor );
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

    private static boolean checkArtifact( Artifact artifact, ReleaseDescriptor releaseDescriptor )
    {
        String versionlessKey = ArtifactUtils.versionlessKey( artifact.getGroupId(), artifact.getArtifactId() );
        String releaseDescriptorResolvedVersion = releaseDescriptor.getDependencyReleaseVersion( versionlessKey );

        boolean releaseDescriptorResolvedVersionIsSnapshot = releaseDescriptorResolvedVersion == null
                        || releaseDescriptorResolvedVersion.contains( Artifact.SNAPSHOT_VERSION );
        
        // We are only looking at dependencies external to the project - ignore anything found in the reactor as
        // it's version will be updated
        boolean bannedVersion = artifact.isSnapshot()
                && !artifact.getBaseVersion().equals( releaseDescriptor.getProjectOriginalVersion( versionlessKey ) )
                        && releaseDescriptorResolvedVersionIsSnapshot;

        // If we have a snapshot but allowTimestampedSnapshots is true, accept the artifact if the version
        // indicates that it is a timestamped snapshot.
        if ( bannedVersion && releaseDescriptor.isAllowTimestampedSnapshots() )
        {
            bannedVersion = artifact.getVersion().indexOf( Artifact.SNAPSHOT_VERSION ) >= 0;
        }

        return bannedVersion;
    }

    @Override
    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                   List<MavenProject> reactorProjects )
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
        List<Artifact> snapshotsList = new ArrayList<>( snapshotsSet );

        Collections.sort( snapshotsList );

        for ( Artifact artifact : snapshotsList )
        {
            message.append( "    " );

            message.append( artifact );

            message.append( "\n" );
        }

        return message;
    }

    private void resolveSnapshots( Set<Artifact> projectDependencies, Set<Artifact> reportDependencies,
                                   Set<Artifact> extensionDependencies, Set<Artifact> pluginDependencies,
                                   ReleaseDescriptor releaseDescriptor )
        throws ReleaseExecutionException
    {
        try
        {
            String autoResolveSnapshots = releaseDescriptor.getAutoResolveSnapshots();
            if ( resolveSnapshot == null )
            {
                prompter.showMessage( RESOLVE_SNAPSHOT_MESSAGE );
                if ( autoResolveSnapshots != null )
                {
                    resolveSnapshot = "yes";
                    prompter.showMessage( RESOLVE_SNAPSHOT_PROMPT + " " + resolveSnapshot );
                }
                else
                {
                    resolveSnapshot = prompter.prompt( RESOLVE_SNAPSHOT_PROMPT, Arrays.asList( "yes", "no" ), "no" );
                }
            }

            if ( resolveSnapshot.toLowerCase( Locale.ENGLISH ).startsWith( "y" ) )
            {
                if ( resolveSnapshotType == null )
                {
                    prompter.showMessage( RESOLVE_SNAPSHOT_TYPE_MESSAGE );
                    int defaultAnswer = -1;
                    if ( autoResolveSnapshots != null )
                    {
                        if ( "all".equalsIgnoreCase( autoResolveSnapshots ) )
                        {
                            defaultAnswer = 0;
                        }
                        else if ( "dependencies".equalsIgnoreCase( autoResolveSnapshots ) )
                        {
                            defaultAnswer = 1;
                        }
                        else if ( "plugins".equalsIgnoreCase( autoResolveSnapshots ) )
                        {
                            defaultAnswer = 2;
                        }
                        else if ( "reports".equalsIgnoreCase( autoResolveSnapshots ) )
                        {
                            defaultAnswer = 3;
                        }
                        else if ( "extensions".equalsIgnoreCase( autoResolveSnapshots ) )
                        {
                            defaultAnswer = 4;
                        }
                        else
                        {
                            try
                            {
                                defaultAnswer = Integer.parseInt( autoResolveSnapshots );
                            }
                            catch ( NumberFormatException e )
                            {
                                throw new ReleaseExecutionException( e.getMessage(), e );
                            }
                        }
                    }
                    if ( defaultAnswer >= 0 && defaultAnswer <= 4 )
                    {
                        prompter.showMessage( RESOLVE_SNAPSHOT_TYPE_PROMPT + " " + autoResolveSnapshots );
                        resolveSnapshotType = Integer.toString( defaultAnswer );
                    }
                    else
                    {
                        resolveSnapshotType =
                            prompter.prompt( RESOLVE_SNAPSHOT_TYPE_PROMPT, Arrays.asList( "0", "1", "2", "3" ), "1" );
                    }
                }

                switch ( Integer.parseInt( resolveSnapshotType.toLowerCase( Locale.ENGLISH ) ) )
                {
                    // all
                    case 0:
                        processSnapshot( projectDependencies, releaseDescriptor, autoResolveSnapshots );
                        processSnapshot( pluginDependencies, releaseDescriptor, autoResolveSnapshots );
                        processSnapshot( reportDependencies, releaseDescriptor, autoResolveSnapshots );
                        processSnapshot( extensionDependencies, releaseDescriptor, autoResolveSnapshots );
                        break;

                        // project dependencies
                    case 1:
                        processSnapshot( projectDependencies, releaseDescriptor, autoResolveSnapshots );
                        break;

                        // plugins
                    case 2:
                        processSnapshot( pluginDependencies, releaseDescriptor, autoResolveSnapshots );
                        break;

                        // reports
                    case 3:
                        processSnapshot( reportDependencies, releaseDescriptor, autoResolveSnapshots );
                        break;

                        // extensions
                    case 4:
                        processSnapshot( extensionDependencies, releaseDescriptor, autoResolveSnapshots );
                        break;

                    default:
                }
            }
        }
        catch ( PrompterException | VersionParseException e )
        {
            throw new ReleaseExecutionException( e.getMessage(), e );
        }
    }

    private void processSnapshot( Set<Artifact> snapshotSet, ReleaseDescriptor releaseDescriptor,
                                  String autoResolveSnapshots )
        throws PrompterException, VersionParseException
    {
        Iterator<Artifact> iterator = snapshotSet.iterator();

        while ( iterator.hasNext() )
        {
            Artifact currentArtifact = iterator.next();
            String versionlessKey = ArtifactUtils.versionlessKey( currentArtifact );

            VersionInfo versionInfo = new DefaultVersionInfo( currentArtifact.getBaseVersion() );
            releaseDescriptor.addDependencyOriginalVersion( versionlessKey, versionInfo.toString() );

            prompter.showMessage(
                "Dependency '" + versionlessKey + "' is a snapshot (" + currentArtifact.getVersion() + ")\n" );
            String message = "Which release version should it be set to?";
            String result;
            if ( null != autoResolveSnapshots )
            {
                result = versionInfo.getReleaseVersionString();
                prompter.showMessage( message + " " + result );
            }
            else
            {
                result = prompter.prompt( message, versionInfo.getReleaseVersionString() );
            }
            
            releaseDescriptor.addDependencyReleaseVersion( versionlessKey, result );

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

            message = "What version should the dependency be reset to for development?";
            if ( null != autoResolveSnapshots )
            {
                result = nextVersion;
                prompter.showMessage( message + " " + result );
            }
            else
            {
                result = prompter.prompt( message, nextVersion );
            }
            
            releaseDescriptor.addDependencyDevelopmentVersion( versionlessKey, result );
        }
    }
}
