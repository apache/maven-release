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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

/**
 * Test the version mapping phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class MapVersionsPhaseTest
    extends PlexusTestCase
{
    public void testMapReleaseVersionsInteractive()
        throws Exception
    {
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-release-versions" );
        MavenProject project = createProject( "artifactId", "1.0-SNAPSHOT" );

        Prompter mockPrompter = mock( Prompter.class );
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
        verifyNoMoreInteractions( mockPrompter );
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

        Prompter mockPrompter = mock( Prompter.class );
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
        verifyNoMoreInteractions( mockPrompter );
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

        Prompter mockPrompter = mock( Prompter.class );
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
        
        // never invoke mockprompter
        verifyNoMoreInteractions( mockPrompter );
    }

    public void testMapReleaseVersionsNonInteractive()
        throws Exception
    {
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-release-versions" );

        Prompter mockPrompter = mock( Prompter.class );
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

        // never invoke mockprompter
        verifyNoMoreInteractions( mockPrompter );
    }

    public void testMapDevVersionsInteractive()
        throws Exception
    {
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );
        MavenProject project = createProject( "artifactId", "1.0" );

        Prompter mockPrompter = mock( Prompter.class );
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
        verifyNoMoreInteractions( mockPrompter );
    }

    public void testMapDevVersionsNonInteractive()
        throws Exception
    {
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );

        Prompter mockPrompter = mock( Prompter.class );
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
        
        // never invoke mockprompter
        verifyNoMoreInteractions( mockPrompter );
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

        Prompter mockPrompter = mock( Prompter.class );
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
        
        // never invoke mockprompter
        verifyNoMoreInteractions( mockPrompter );
    }

    public void testPrompterException()
        throws Exception
    {
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );

        Prompter mockPrompter = mock( Prompter.class );
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
        verifyNoMoreInteractions( mockPrompter );
    }

    public void testAdjustVersionInteractive()
        throws Exception
    {
        // prepare
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );
        MavenProject project = createProject( "artifactId", "foo" );

        Prompter mockPrompter = mock( Prompter.class );
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
        verifyNoMoreInteractions( mockPrompter );
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

    private static MavenProject createProject( String artifactId, String version )
    {
        Model model = new Model();
        model.setGroupId( "groupId" );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        return new MavenProject( model );
    }

}