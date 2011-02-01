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

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Test the dependency snapshot check phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class CheckDependencySnapshotsPhaseTest
    extends AbstractReleaseTestCase
{
    private static final String NO = "no";

    private static final String YES = "yes";

    private static final List<String> YES_NO_ARRAY = Arrays.asList( YES, NO );

    private static final String DEFAULT_CHOICE = "1";

    private static final List<String> CHOICE_ARRAY = Arrays.asList( "0", DEFAULT_CHOICE, "2", "3" );

    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );
    }

    public void testNoSnapshotDependencies()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "no-snapshot-dependencies" );

        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testNoSnapshotRangeDependencies()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "no-snapshot-range-dependencies" );

        phase.setPrompter( createMockPrompter( YES, DEFAULT_CHOICE, new VersionPair( "1.1", "1.2-SNAPSHOT" ) ) );

        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        phase.setPrompter( createMockPrompter( YES, DEFAULT_CHOICE, new VersionPair( "1.1", "1.2-SNAPSHOT" ) ) );

        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }
    
    public void testSnapshotDependenciesInProjectOnly()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "internal-snapshot-dependencies" );

        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotReleasePluginNonInteractive()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "snapshot-release-plugin" );
        releaseDescriptor.setInteractive( false );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotReleasePluginInteractiveDeclined()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "snapshot-release-plugin" );

        phase.setPrompter( createMockPrompterWithSnapshotReleasePlugin( NO, NO ) );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        phase.setPrompter( createMockPrompterWithSnapshotReleasePlugin( NO, NO ) );

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotReleasePluginInteractiveAcceptedForExecution()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "snapshot-release-plugin" );

        phase.setPrompter( createYesMockPrompter() );

        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( true );
    }

    public void testSnapshotReleasePluginInteractiveAcceptedForSimulation()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "snapshot-release-plugin" );

        phase.setPrompter( createYesMockPrompter() );

        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( true );
    }

    public void testSnapshotReleasePluginInteractiveInvalid()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "snapshot-release-plugin" );

        phase.setPrompter( createMockPrompterWithSnapshotReleasePlugin( "donkey", NO ) );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        phase.setPrompter( createMockPrompterWithSnapshotReleasePlugin( "donkey", NO ) );

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotReleasePluginInteractiveException()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "snapshot-release-plugin" );

        Prompter mockPrompter = mock( Prompter.class );
        when( mockPrompter.prompt( anyString(), eq( YES_NO_ARRAY ), eq( NO ) ) ).thenThrow( new PrompterException(
            "..." ) );
        phase.setPrompter( mockPrompter );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "Check cause", PrompterException.class, e.getCause().getClass() );
        }

        mockPrompter = mock( Prompter.class );
        when( mockPrompter.prompt( anyString(), eq( YES_NO_ARRAY ), eq( NO ) ) ).thenThrow( new PrompterException(
            "..." ) );

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "Check cause", PrompterException.class, e.getCause().getClass() );
        }
    }

    public void testSnapshotDependenciesInProjectOnlyMismatchedVersion()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "internal-differing-snapshot-dependencies" );

        phase.setPrompter( createNoMockPrompter() );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        phase.setPrompter( createNoMockPrompter() );

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotManagedDependenciesInProjectOnly()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "internal-managed-snapshot-dependency" );

        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotUnusedInternalManagedDependency()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "unused-internal-managed-snapshot-dependency" );

        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotUnusedExternalManagedDependency()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "unused-external-managed-snapshot-dependency" );

        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotExternalManagedDependency()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "external-managed-snapshot-dependency" );

        releaseDescriptor.setInteractive( false );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotDependenciesOutsideProjectOnlyNonInteractive()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "external-snapshot-dependencies" );

        releaseDescriptor.setInteractive( false );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testRangeSnapshotDependenciesOutsideProjectOnlyNonInteractive()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "external-range-snapshot-dependencies" );

        releaseDescriptor.setInteractive( false );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotDependenciesOutsideProjectOnlyInteractiveWithSnapshotsResolved()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "external-snapshot-dependencies" );

        phase.setPrompter( createMockPrompter( YES, DEFAULT_CHOICE, new VersionPair( "1.0", "1.1-SNAPSHOT" ),
                                               new VersionPair( "1.0", "1.0" ) ) );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        }
        catch ( ReleaseFailureException e )
        {
            fail( e.getMessage() );
        }

        // validate
        @SuppressWarnings("rawtypes")
        Map versionsMap = (Map) releaseDescriptor.getResolvedSnapshotDependencies().get( "external:artifactId" );

        assertNotNull( versionsMap );
        assertEquals( "1.1-SNAPSHOT", versionsMap.get( ReleaseDescriptor.DEVELOPMENT_KEY ) );
        assertEquals( "1.0", versionsMap.get( ReleaseDescriptor.RELEASE_KEY ) );

        releaseDescriptor = new ReleaseDescriptor();

        phase.setPrompter( createMockPrompter( YES, DEFAULT_CHOICE, new VersionPair( "1.0", "1.1-SNAPSHOT" ) ) );

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        }
        catch ( ReleaseFailureException e )
        {
            fail( e.getMessage() );
        }
    }

    public void testSnapshotDependenciesSelectOlderRelease()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "external-snapshot-dependencies" );

        phase.setPrompter( createMockPrompter( YES, DEFAULT_CHOICE, new VersionPair( "0.9", "1.0-SNAPSHOT" ),
                                               new VersionPair( "1.0", "1.0-SNAPSHOT" ) ) );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        }
        catch ( ReleaseFailureException e )
        {
            fail( e.getMessage() );
        }

        // validate
        @SuppressWarnings("rawtypes")
        Map versionsMap = (Map) releaseDescriptor.getResolvedSnapshotDependencies().get( "external:artifactId" );

        assertNotNull( versionsMap );
        assertEquals( "1.0-SNAPSHOT", versionsMap.get( ReleaseDescriptor.DEVELOPMENT_KEY ) );
        assertEquals( "0.9", versionsMap.get( ReleaseDescriptor.RELEASE_KEY ) );
    }

    public void testSnapshotDependenciesSelectDefaults()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "external-snapshot-dependencies" );

        phase.setPrompter( createMockPrompter( YES, DEFAULT_CHOICE, new VersionPair( "1.0", "1.0" ) ) );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        }
        catch ( ReleaseFailureException e )
        {
            fail( e.getMessage() );
        }

        // validate
        @SuppressWarnings("rawtypes")
        Map versionsMap = (Map) releaseDescriptor.getResolvedSnapshotDependencies().get( "external:artifactId" );

        assertNotNull( versionsMap );
        assertEquals( "1.0", versionsMap.get( ReleaseDescriptor.DEVELOPMENT_KEY ) );
        assertEquals( "1.0", versionsMap.get( ReleaseDescriptor.RELEASE_KEY ) );
    }

    public void testSnapshotDependenciesUpdateAllOnlyDependenciesNeeded()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "external-snapshot-dependencies" );

        phase.setPrompter( createMockPrompter( YES, "0", new VersionPair( "1.0", "1.0" ) ) );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        }
        catch ( ReleaseFailureException e )
        {
            fail( e.getMessage() );
        }

        // validate
        @SuppressWarnings("rawtypes")
        Map versionsMap = (Map) releaseDescriptor.getResolvedSnapshotDependencies().get( "external:artifactId" );

        assertNotNull( versionsMap );
        assertEquals( "1.0", versionsMap.get( ReleaseDescriptor.DEVELOPMENT_KEY ) );
        assertEquals( "1.0", versionsMap.get( ReleaseDescriptor.RELEASE_KEY ) );
    }


    public void testSnapshotDependenciesUpdateAll()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "external-snapshot-all" );

        Prompter mockPrompter = createMockPrompter( YES, "0", Arrays.asList( new VersionPair( "1.0", "1.0" ),
                                                                             new VersionPair( "1.1", "1.1" ),
                                                                             new VersionPair( "1.2", "1.2" ),
                                                                             new VersionPair( "1.3", "1.3" ) ) );
        phase.setPrompter( mockPrompter );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        }
        catch ( ReleaseFailureException e )
        {
            fail( e.getMessage() );
        }

        // validate
        @SuppressWarnings("rawtypes")
        Map versionsMap = (Map) releaseDescriptor.getResolvedSnapshotDependencies().get( "external:artifactId" );

        assertNotNull( versionsMap );
        assertEquals( "1.0", versionsMap.get( ReleaseDescriptor.DEVELOPMENT_KEY ) );
        assertEquals( "1.0", versionsMap.get( ReleaseDescriptor.RELEASE_KEY ) );
    }

    // MRELEASE-589
    public void testSnapshotDependenciesOutsideMultimoduleProjectOnlyInteractiveWithSnapshotsResolved()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "multimodule-external-snapshot-dependencies" );

        VersionPair pair = new VersionPair( "1.0", "1.1-SNAPSHOT" );
        VersionPair defaultPair = new VersionPair( "1.0", "1.0" );
        Prompter mockPrompter = createMockPrompter( "yes", "1", Arrays.asList( pair, pair ), Arrays.asList( defaultPair,
                                                                                                            defaultPair ) );
        phase.setPrompter( mockPrompter );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        }
        catch ( ReleaseFailureException e )
        {
            fail( e.getMessage() );
        }

        @SuppressWarnings("rawtypes")
        Map resolvedDependencies = releaseDescriptor.getResolvedSnapshotDependencies();

        assertNotNull( resolvedDependencies );
        assertEquals( 2, resolvedDependencies.size() );

        assertTrue( resolvedDependencies.containsKey( "external:artifactId" ) );
        assertTrue( resolvedDependencies.containsKey( "external:artifactId2") );

        @SuppressWarnings("rawtypes")
        Map versionsMap = (Map) releaseDescriptor.getResolvedSnapshotDependencies().get( "external:artifactId" );

        assertNotNull( versionsMap );
        assertEquals( "1.1-SNAPSHOT", versionsMap.get( ReleaseDescriptor.DEVELOPMENT_KEY ) );
        assertEquals( "1.0", versionsMap.get( ReleaseDescriptor.RELEASE_KEY ) );

        versionsMap = (Map) releaseDescriptor.getResolvedSnapshotDependencies().get( "external:artifactId2" );

        assertNotNull( versionsMap );
        assertEquals( "1.1-SNAPSHOT", versionsMap.get( ReleaseDescriptor.DEVELOPMENT_KEY ) );
        assertEquals( "1.0", versionsMap.get( ReleaseDescriptor.RELEASE_KEY ) );
    }

    public void testSnapshotDependenciesInsideAndOutsideProject()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "internal-and-external-snapshot-dependencies" );

        phase.setPrompter( createNoMockPrompter() );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        phase.setPrompter( createNoMockPrompter() );

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testNoSnapshotReportPlugins()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "no-snapshot-report-plugins" );

        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotReportPluginsInProjectOnly()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "internal-snapshot-report-plugins" );

        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotReportPluginsOutsideProjectOnly()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "external-snapshot-report-plugins" );

        phase.setPrompter( createNoMockPrompter() );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        phase.setPrompter( createNoMockPrompter() );

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotReportPluginsInsideAndOutsideProject()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "internal-and-external-snapshot-report-plugins" );

        phase.setPrompter( createNoMockPrompter() );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        phase.setPrompter( createNoMockPrompter() );

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testNoSnapshotPlugins()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "no-snapshot-plugins" );

        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotPluginsInProjectOnly()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "internal-snapshot-plugins" );

        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotManagedPluginInProjectOnly()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "internal-managed-snapshot-plugin" );

        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotUnusedInternalManagedPlugin()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "unused-internal-managed-snapshot-plugin" );

        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotUnusedExternalManagedPlugin()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "unused-external-managed-snapshot-plugin" );

        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotExternalManagedPlugin()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "external-managed-snapshot-plugin" );

        phase.setPrompter( createNoMockPrompter() );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        phase.setPrompter( createNoMockPrompter() );

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotPluginsOutsideProjectOnly()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "external-snapshot-plugins" );

        phase.setPrompter( createNoMockPrompter() );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        phase.setPrompter( createNoMockPrompter() );

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotPluginsInsideAndOutsideProject()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "internal-and-external-snapshot-plugins" );

        phase.setPrompter( createNoMockPrompter() );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        phase.setPrompter( createNoMockPrompter() );

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotExternalParent()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "external-snapshot-parent/child" );

        phase.setPrompter( createNoMockPrompter() );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        phase.setPrompter( createNoMockPrompter() );

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotExternalParentAdjusted()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "external-snapshot-parent/child" );

        Prompter mockPrompter = createMockPrompter( YES, DEFAULT_CHOICE, new VersionPair( "1.0-test", "1.0-test" ),
                                                    new VersionPair( "1.0", "1.0-test" ) );
        phase.setPrompter( mockPrompter );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        }
        catch ( ReleaseFailureException e )
        {
            fail( e.getMessage() );
        }

        // validate
        @SuppressWarnings("rawtypes")
        Map versionsMap = (Map) releaseDescriptor.getResolvedSnapshotDependencies().get( "groupId:parent-external" );

        assertNotNull( versionsMap );
        assertEquals( "1.0-test", versionsMap.get( ReleaseDescriptor.DEVELOPMENT_KEY ) );
        assertEquals( "1.0-test", versionsMap.get( ReleaseDescriptor.RELEASE_KEY ) );
    }

    public void testReleaseExternalParent()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "external-parent/child" );

        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotExternalExtension()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "external-snapshot-extension" );

        phase.setPrompter( createNoMockPrompter() );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        phase.setPrompter( createNoMockPrompter() );

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotInternalExtension()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "internal-snapshot-extension" );

        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testReleaseExternalExtension()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "external-extension" );

        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testAllowTimestampedSnapshots()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createDescriptorFromProjects( "external-timestamped-snapshot-dependencies" );

        releaseDescriptor.setInteractive( false );

        // confirm POM fails without allowTimestampedSnapshots
        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        // check whether flag allows
        releaseDescriptor.setAllowTimestampedSnapshots(true);

        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    private List<MavenProject> createDescriptorFromProjects( String path )
        throws Exception
    {
        return createReactorProjects( "check-dependencies/", path );
    }

    private Prompter createNoMockPrompter()
        throws PrompterException
    {
        return createYesNoMockPrompter( false );
    }

    private Prompter createYesMockPrompter()
        throws PrompterException
    {
        return createYesNoMockPrompter( true );
    }

    private Prompter createYesNoMockPrompter( boolean yes )
        throws PrompterException
    {
        Prompter mockPrompter = mock( Prompter.class );

        when( mockPrompter.prompt( anyString(), eq( YES_NO_ARRAY ), eq( NO ) ) ).thenReturn( yes ? YES : NO );

        return mockPrompter;
    }

    private Prompter createMockPrompterWithSnapshotReleasePlugin( String useSnapshotReleasePlugin,
                                                                  String resolveSnapshots )
        throws PrompterException
    {
        Prompter mockPrompter = mock( Prompter.class );

        when( mockPrompter.prompt( anyString(), eq( YES_NO_ARRAY ), eq( NO ) ) ).thenReturn( useSnapshotReleasePlugin );
        when( mockPrompter.prompt( anyString(), eq( YES_NO_ARRAY ), eq( NO ) ) ).thenReturn( resolveSnapshots );

        return mockPrompter;
    }

    private Prompter createMockPrompter( String resolveSnapshots, String resolutionType, VersionPair resolvedVersions )
        throws PrompterException
    {
        return createMockPrompter( resolveSnapshots, resolutionType, resolvedVersions, resolvedVersions );
    }

    private Prompter createMockPrompter( String resolveSnapshots, String resolutionType, VersionPair resolvedVersions,
                                         VersionPair defaultVersions )
        throws PrompterException
    {
        return createMockPrompter( resolveSnapshots, resolutionType, Collections.singletonList( resolvedVersions ),
                                   Collections.singletonList( defaultVersions ) );
    }

    private Prompter createMockPrompter( String resolveSnapshots, String resolutionType,
                                         List<VersionPair> resolvedVersions )
        throws PrompterException
    {
        return createMockPrompter( resolveSnapshots, resolutionType, resolvedVersions, resolvedVersions );
    }

    private Prompter createMockPrompter( String resolveSnapshots, String resolutionType,
                                         List<VersionPair> resolvedVersions, List<VersionPair> defaultVersions )
        throws PrompterException
    {
        Prompter mockPrompter = mock( Prompter.class );

        when( mockPrompter.prompt( anyString(), eq( YES_NO_ARRAY ), eq( NO ) ) ).thenReturn( resolveSnapshots );
        when( mockPrompter.prompt( anyString(), eq( CHOICE_ARRAY ), eq( DEFAULT_CHOICE ) ) ).thenReturn(
            resolutionType );

        for ( int i = 0; i < resolvedVersions.size(); i++ )
        {
            when( mockPrompter.prompt( "Which release version should it be set to?", defaultVersions.get(
                i ).releaseVersion ) ).thenReturn( resolvedVersions.get( i ).releaseVersion );
            when( mockPrompter.prompt( "What version should the dependency be reset to for development?",
                                       defaultVersions.get( i ).developmentVersion ) ).thenReturn( resolvedVersions.get(
                i ).developmentVersion );
        }
        return mockPrompter;
    }

    private static class VersionPair
    {
        String releaseVersion;

        String developmentVersion;

        public VersionPair( String releaseVersion, String developmentVersion )
        {
            this.releaseVersion = releaseVersion;
            this.developmentVersion = developmentVersion;
        }
    }
}
