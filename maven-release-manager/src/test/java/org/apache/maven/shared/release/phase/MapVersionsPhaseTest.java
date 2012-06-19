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

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Test the version mapping phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class MapVersionsPhaseTest
    extends PlexusTestCase
{
    @Mock
    private Prompter mockPrompter;
    
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
    }
    
    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
        verifyNoMoreInteractions( mockPrompter );
    }
    
    public void testMapReleaseVersionsInteractive()
        throws Exception
    {
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-release-versions" );
        MavenProject project = createProject( "artifactId", "1.0-SNAPSHOT" );

        when( mockPrompter.prompt( startsWith( "What is the release version for \"" + project.getName() + "\"?" ), eq( "1.0" ) ) ).thenReturn( "2.0" );
        phase.setPrompter( mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( project );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();

        // execute
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0" ),
                      releaseDescriptor.getReleaseVersions() );

        // prepare
        releaseDescriptor = new ReleaseDescriptor();

        // execute
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0" ),
                      releaseDescriptor.getReleaseVersions() );
        verify( mockPrompter, times( 2 ) ).prompt( startsWith( "What is the release version for \"" + project.getName() + "\"?" ), eq( "1.0" ) );
    }
    
    // MRELEASE-403: Release plugin ignores given version number
    public void testMapReleaseVersionsInteractiveAddZeroIncremental()
        throws Exception
    {
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-release-versions" );
        MavenProject project = createProject( "artifactId", "1.0-SNAPSHOT" );

        when(
              mockPrompter.prompt( startsWith( "What is the release version for \"" + project.getName() + "\"?" ),
                                   eq( "1.0" ) ) ).thenReturn( "1.0.0" );
        phase.setPrompter( mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( project );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();

        // execute
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "1.0.0" ),
                      releaseDescriptor.getReleaseVersions() );

        // prepare
        releaseDescriptor = new ReleaseDescriptor();

        // execute
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "1.0.0" ),
                      releaseDescriptor.getReleaseVersions() );
        verify( mockPrompter, times( 2 ) ).prompt( startsWith( "What is the release version for \"" + project.getName()
                                                       + "\"?" ), eq( "1.0" ) );
    }

    /**
     * Test to release "SNAPSHOT" version
     * MRELEASE-90
     */
    public void testMapReleaseVersionsInteractiveWithSnaphotVersion()
        throws Exception
    {
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-release-versions" );
        MavenProject project = createProject( "artifactId", "SNAPSHOT" );

        when( mockPrompter.prompt( startsWith( "What is the release version for \"" + project.getName() + "\"?" ), eq( "1.0" ) ) ).thenReturn( "2.0" );
        phase.setPrompter( mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( project );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();

        // execute
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0" ),
                      releaseDescriptor.getReleaseVersions() );

        // prepare
        releaseDescriptor = new ReleaseDescriptor();

        // execute
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0" ),
                      releaseDescriptor.getReleaseVersions() );
        
        verify( mockPrompter, times( 2 ) ).prompt( startsWith( "What is the release version for \"" + project.getName() + "\"?" ), eq( "1.0" ) );
    }

    /**
     * MRELEASE-524: ignores commandline versions in batch mode
     */
    public void testMapReleaseVersionsNonInteractiveWithExplicitVersion()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "SNAPSHOT" ) );

        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-release-versions" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.addReleaseVersion( "groupId:artifactId", "2.0" );

        phase.setPrompter( mockPrompter );

        // execute
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0" ),
                      releaseDescriptor.getReleaseVersions() );

        // prepare
        releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.addReleaseVersion( "groupId:artifactId", "2.0" );

        // execute
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0" ),
                      releaseDescriptor.getReleaseVersions() );
    }

    public void testMapReleaseVersionsNonInteractive()
        throws Exception
    {
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-release-versions" );

        phase.setPrompter( mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0-SNAPSHOT" ) );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setInteractive( false );

        // execute
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "1.0" ),
                      releaseDescriptor.getReleaseVersions() );
        // prepare
        releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setInteractive( false );

        // execute
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "1.0" ),
                      releaseDescriptor.getReleaseVersions() );
    }

    public void testMapDevVersionsInteractive()
        throws Exception
    {
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );
        MavenProject project = createProject( "artifactId", "1.0" );

        when( mockPrompter.prompt( startsWith( "What is the new development version for \"" + project.getName() + "\"?" ), eq( "1.1-SNAPSHOT" ) ) ).thenReturn( "2.0-SNAPSHOT" );
        phase.setPrompter( mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( project );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();

        // execute
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );

        // prepare
        releaseDescriptor = new ReleaseDescriptor();

        // execute
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
        
        verify( mockPrompter, times( 2 ) ).prompt( startsWith( "What is the new development version for \"" + project.getName() + "\"?" ), eq( "1.1-SNAPSHOT" ) );
    }

    /**
     * MRELEASE-760: updateWorkingCopyVersions=false still bumps up pom versions to next development version
     */
    public void testMapDevVersionsInteractiveDoNotUpdateWorkingCopy()
        throws Exception
    {
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );
        MavenProject project = createProject( "artifactId", "1.0" );

        phase.setPrompter( mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( project );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setUpdateWorkingCopyVersions( false );

        // execute
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "1.0" ),
                      releaseDescriptor.getDevelopmentVersions() );

        // prepare
        releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setUpdateWorkingCopyVersions( false );

        // execute
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "1.0" ),
                      releaseDescriptor.getDevelopmentVersions() );
    }

    public void testMapDevVersionsNonInteractive()
        throws Exception
    {
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );

        phase.setPrompter( mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setInteractive( false );

        // execute
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "1.1-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );

        // prepare
        releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setInteractive( false );

        // execute
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "1.1-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
    }

     /**
     * MRELEASE-524: ignores commandline versions in batch mode
     */
    public void testMapDevVersionsNonInteractiveWithExplicitVersion()
        throws Exception
    {
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        phase.setPrompter( mockPrompter );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.addDevelopmentVersion( "groupId:artifactId", "2-SNAPSHOT" );

        // execute
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );

        // prepare
        releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.addDevelopmentVersion( "groupId:artifactId", "2-SNAPSHOT" );

        // execute
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
    }

    public void testPrompterException()
        throws Exception
    {
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );

        when( mockPrompter.prompt( isA( String.class ),  isA( String.class ) ) ).thenThrow( new PrompterException( "..." ) );
        phase.setPrompter( mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();

        // execute
        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Expected an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "check cause", PrompterException.class, e.getCause().getClass() );
        }

        // prepare
        releaseDescriptor = new ReleaseDescriptor();

        // execute
        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Expected an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "check cause", PrompterException.class, e.getCause().getClass() );
        }
        
        //verify
        verify( mockPrompter, times( 2 ) ).prompt( isA( String.class ),  isA( String.class ) );
    }

    public void testAdjustVersionInteractive()
        throws Exception
    {
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );
        MavenProject project = createProject( "artifactId", "foo" );

        when( mockPrompter.prompt( startsWith( "What is the new development version for \"" + project.getName() + "\"?" ), eq( "1.1-SNAPSHOT" ) ) ).thenReturn( "2.0-SNAPSHOT" );
        phase.setPrompter( mockPrompter );

        List<MavenProject> reactorProjects = Collections.singletonList( project );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();

        // execute
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );

        // prepare
        releaseDescriptor = new ReleaseDescriptor();

        // execute
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
        verify( mockPrompter, times( 2 ) ).prompt( startsWith( "What is the new development version for \"" + project.getName() + "\"?" ), eq( "1.1-SNAPSHOT" ) );
    }

    public void testAdjustVersionNonInteractive()
        throws Exception
    {
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );

        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "foo" ) );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setInteractive( false );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Expected an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "check cause", VersionParseException.class, e.getCause().getClass() );
        }

        releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setInteractive( false );

        try
        {
            phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Expected an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "check cause", VersionParseException.class, e.getCause().getClass() );
        }
    }

    public void testExecuteSnapshotAutoVersionSubmodules_NotInteractive_MapDevelopment() throws Exception
    {
        //verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ), releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }
    
    public void testSimulateSnapshotAutoVersionSubmodules_NotInteractive_MapDevelopment() throws Exception
    {
        //verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ), releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }

    public void testExecuteReleaseAutoVersionSubmodules_NotInteractive_MapDevelopment() throws Exception
    {
        //verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ), releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }
    
    public void testSimulateReleaseAutoVersionSubmodules_NotInteractive_MapDevelopment() throws Exception
    {
        //verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ), releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }

    public void testExecuteSnapshotAutoVersionSubmodules_NotInteractive_MapRelease() throws Exception
    {
        //verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-release-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        assertEquals( "Check release versions",  Collections.singletonMap( "groupId:artifactId", "1.2" ), releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }
    
    public void testSimulateSnapshotAutoVersionSubmodules_NotInteractive_MapRelease() throws Exception
    {
        //verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-release-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        assertEquals( "Check release versions",  Collections.singletonMap( "groupId:artifactId", "1.2" ), releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testExecuteReleaseAutoVersionSubmodules_NotInteractive_MapRelease() throws Exception
    {
        //verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-release-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        assertEquals( "Check release versions",  Collections.singletonMap( "groupId:artifactId", "1.2" ), releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }
    
    public void testSimulateReleaseAutoVersionSubmodules_NotInteractive_MapRelease() throws Exception
    {
        //verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-release-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        assertEquals( "Check release versions",  Collections.singletonMap( "groupId:artifactId", "1.2" ), releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testExecuteSnapshotAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment() throws Exception
    {
        //prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setBranchCreation( true );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ), releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }
    
    public void testSimulateSnapshotAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment() throws Exception
    {
        //prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setBranchCreation( true );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ), releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
   }
    
    public void testExecuteReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment() throws Exception
    {
        //prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setBranchCreation( true );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.2" ), releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }
    
    public void testSimulateReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment() throws Exception
    {
        //prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setBranchCreation( true );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.2" ), releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
   }


    public void testExecuteSnapshotAutoVersionSubmodules_NotInteractive_MapBranch() throws Exception
    {
        //prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-branch-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );

        //test
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ), releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }

    public void testSimulateSnapshotAutoVersionSubmodules_NotInteractive_MapBranch() throws Exception
    {
        //prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-branch-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ), releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }

    public void testExecuteReleaseAutoVersionSubmodules_NotInteractive_MapBranch() throws Exception
    {
        //prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-branch-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );

        //test
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ), releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }

    public void testSimulateReleaseAutoVersionSubmodules_NotInteractive_MapBranch() throws Exception
    {
        //prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-branch-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "1.3-SNAPSHOT" ), releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }

    public void testExecuteSnapshotAutoVersionSubmodules_BranchCreation_NotInteractive_MapBranch() throws Exception
    {
        //prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-branch-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setBranchCreation( true );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        assertEquals( "Check release versions",  Collections.singletonMap( "groupId:artifactId", "1.2-SNAPSHOT" ), releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testSimulateSnapshotAutoVersionSubmodules_BranchCreation_NotInteractive_MapBranch() throws Exception
    {
        //prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-branch-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2-SNAPSHOT" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setBranchCreation( true );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        assertEquals( "Check release versions",  Collections.singletonMap( "groupId:artifactId", "1.2-SNAPSHOT" ), releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }
    
    public void testExecuteReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapBranch() throws Exception
    {
        //prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-branch-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setBranchCreation( true );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        /*
         * "By default, the POM in the new branch keeps the same version as the local working copy, and the local POM is incremented to the next revision." 
         * This is true for trunk, but when branching from a tag I would expect the next SNAPSHOT version.
         * For now keep '1.2' instead of '1.3-SNAPSHOT' until further investigation.
         */
        assertEquals( "Check release versions",  Collections.singletonMap( "groupId:artifactId", "1.2" ), releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }

    public void testSimulateReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapBranch() throws Exception
    {
        //prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-branch-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setBranchCreation( true );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        /*
         * "By default, the POM in the new branch keeps the same version as the local working copy, and the local POM is incremented to the next revision." 
         * This is true for trunk, but when branching from a tag I would expect the next SNAPSHOT version.
         * For now keep '1.2' instead of '1.3-SNAPSHOT' until further investigation.
         */
        assertEquals( "Check release versions",  Collections.singletonMap( "groupId:artifactId", "1.2" ), releaseDescriptor.getReleaseVersions() );
        assertNull( "Check development versions", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }
    
    public void testExecuteMultiModuleAutoVersionSubmodules__MapDevelopment() throws Exception
    {
        //verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );
        
        List<MavenProject> reactorProjects = new ArrayList<MavenProject>();
        Collections.addAll( reactorProjects, createProject( "artifactId", "1.2-SNAPSHOT" ),  createProject( "module1", "2.0" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        Map<String, String> developmentVersions = new HashMap<String, String>();
        developmentVersions.put( "groupId:artifactId", "1.3-SNAPSHOT" );
        developmentVersions.put( "groupId:module1", "2.0" );
        assertEquals( "Check development versions", developmentVersions, releaseDescriptor.getDevelopmentVersions() );
        assertEquals( "Check release versions", 0, releaseDescriptor.getReleaseVersions().size() );
    }
    
    public void testSimulateMultiModuleAutoVersionSubmodules__MapDevelopment() throws Exception
    {
        //verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );
        
        List<MavenProject> reactorProjects = new ArrayList<MavenProject>();
        Collections.addAll( reactorProjects, createProject( "artifactId", "1.2-SNAPSHOT" ),  createProject( "module1", "2.0" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        Map<String, String> developmentVersions = new HashMap<String, String>();
        developmentVersions.put( "groupId:artifactId", "1.3-SNAPSHOT" );
        developmentVersions.put( "groupId:module1", "2.0" );
        assertEquals( "Check development versions", developmentVersions, releaseDescriptor.getDevelopmentVersions() );
        assertEquals( "Check release versions", 0, releaseDescriptor.getReleaseVersions().size() );
    }
    
    public void testExecuteDefaultReleaseVersion() throws Exception
    {
        //verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2.1-SNAPSHOT" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setDefaultReleaseVersion( "3.0" );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "3.1-SNAPSHOT" ), releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }
    
    public void testSimulateDefaultReleaseVersion() throws Exception
    {
        //verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2.1-SNAPSHOT" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setDefaultReleaseVersion( "3.0" );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "3.1-SNAPSHOT" ), releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }
    
    public void testExecuteDefaultDevelopmentVersion() throws Exception
    {
        //verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2.1-SNAPSHOT" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setDefaultDevelopmentVersion( "3.0-SNAPSHOT" );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "3.0-SNAPSHOT" ), releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }
    
    public void testSimulateDefaultDevelopmentVersion() throws Exception
    {
        //verify
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.2.1-SNAPSHOT" ) );
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setAutoVersionSubmodules( true );
        releaseDescriptor.setDefaultDevelopmentVersion( "3.0-SNAPSHOT" );
        releaseDescriptor.setInteractive( false );
        
        //test
        phase.simulate( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        //verify
        assertEquals( "Check development versions", Collections.singletonMap( "groupId:artifactId", "3.0-SNAPSHOT" ), releaseDescriptor.getDevelopmentVersions() );
        assertNull( "Check release versions", releaseDescriptor.getReleaseVersions().get( "groupId:artifactId" ) );
    }

    private static MavenProject createProject( String artifactId, String version )
    {
        Model model = new Model();
        model.setGroupId( "groupId" );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        return new MavenProject( model );
    }

}