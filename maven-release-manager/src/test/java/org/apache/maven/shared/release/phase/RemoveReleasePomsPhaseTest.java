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

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.Maven;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.command.remove.RemoveScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.manager.ScmManagerStub;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.util.IOUtil;

/**
 * Test the remove release POMs phase.
 *
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 */
public class RemoveReleasePomsPhaseTest
    extends AbstractReleaseTestCase
{
    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "remove-release-poms" );
    }

    public void testExecuteBasicPom()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom" );
        ReleaseDescriptor config = createReleaseDescriptor();
        MavenProject project = ReleaseUtil.getRootProject( reactorProjects );

        File releasePom = ReleaseUtil.getReleasePom( project );
        ScmFileSet fileSet = new ScmFileSet( new File( config.getWorkingDirectory() ), releasePom );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.remove( isA( ScmRepository.class ),
                                      argThat( new IsScmFileSetEquals( fileSet ) ),
                                      isA( String.class ) ) ).thenReturn( new RemoveScmResult( "...", Collections
                       .singletonList( new ScmFile( Maven.RELEASE_POMv4, ScmFileStatus.DELETED ) ) ) );
        
        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );

        // execute
        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        //verify
        verify( scmProviderMock ).remove( isA( ScmRepository.class ),
                                          argThat( new IsScmFileSetEquals( fileSet ) ),
                                          isA( String.class ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

    public void testExecutePomWithModules()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects( "pom-with-modules" );
        ReleaseDescriptor config = createReleaseDescriptor();

        List<File> releasePoms = new ArrayList<File>();
        for ( Iterator<MavenProject> iterator = reactorProjects.iterator(); iterator.hasNext(); )
        {
            MavenProject project = iterator.next();
            File releasePom = ReleaseUtil.getReleasePom( project );
            releasePoms.add( releasePom );
        }

        ScmFileSet fileSet = new ScmFileSet( new File( config.getWorkingDirectory() ), releasePoms );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.remove( isA( ScmRepository.class ),
                                      argThat( new IsScmFileSetEquals( fileSet ) ),
                                      isA( String.class ) ) ).thenReturn( new RemoveScmResult( "...", Collections
                       .singletonList( new ScmFile( Maven.RELEASE_POMv4, ScmFileStatus.DELETED ) ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );

        // execute
        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        verify( scmProviderMock ).remove( isA( ScmRepository.class ),
                                          argThat( new IsScmFileSetEquals( fileSet ) ),
                                          isA( String.class ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

    public void testSimulateBasicPom()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom" );
        ReleaseDescriptor config = createReleaseDescriptor();

        ScmProvider scmProviderMock = mock( ScmProvider.class );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );

        // execute
        phase.simulate( config, new DefaultReleaseEnvironment(), reactorProjects );

        // never invoke scmProviderMock
        verifyNoMoreInteractions( scmProviderMock );
    }
    
    public void testExecuteWithSuppressCommitBeforeTag() throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom" );
        ReleaseDescriptor config = createReleaseDescriptor();
        config.setSuppressCommitBeforeTagOrBranch( true );
        config.setGenerateReleasePoms( true );

        ScmProvider scmProviderMock = mock( ScmProvider.class );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );
        
        // execute
        ReleaseResult result = phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );
        
        BufferedReader reader = null;
        try 
        {
            reader = new BufferedReader( new StringReader( result.getOutput() ) );
            
            assertEquals( "[INFO] Removing release POM for 'Unnamed - groupId:artifactId:jar:1.0-SNAPSHOT'..." , reader.readLine() );
            assertEquals( "Expected EOF",  null, reader.readLine() );
        }
        finally
        {
            IOUtil.close( reader );
        }

        // never invoke scmProviderMock
        verifyNoMoreInteractions( scmProviderMock );
    }

    public void testSimulateWithSuppressCommitBeforeTag() throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom" );
        ReleaseDescriptor config = createReleaseDescriptor();
        config.setSuppressCommitBeforeTagOrBranch( true );
        config.setGenerateReleasePoms( true );

        ScmProvider scmProviderMock = mock( ScmProvider.class );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );

        // execute
        ReleaseResult result = phase.simulate( config, new DefaultReleaseEnvironment(), reactorProjects );

        BufferedReader reader = null;
        try 
        {
            reader = new BufferedReader( new StringReader( result.getOutput() ) );
            
            assertEquals( "[INFO] Removing release POM for 'Unnamed - groupId:artifactId:jar:1.0-SNAPSHOT'..." , reader.readLine() );
            assertEquals( "[INFO] Full run would be removing [" + reactorProjects.get( 0 ).getFile().getParent() + File.separator + "release-pom.xml]", reader.readLine() );
            assertEquals( "Expected EOF",  null, reader.readLine() );
        }
        finally
        {
            IOUtil.close( reader );
        }

        // never invoke scmProviderMock
        verifyNoMoreInteractions( scmProviderMock );
    }

    private List<MavenProject> createReactorProjects( String path )
        throws Exception
    {
        return createReactorProjects( "remove-release-poms/", path );
    }

    private ReleaseDescriptor createReleaseDescriptor()
    {
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        descriptor.setGenerateReleasePoms( true );
        descriptor.setWorkingDirectory( getTestFile( "target/test/checkout" ).getAbsolutePath() );
        return descriptor;
    }
}
