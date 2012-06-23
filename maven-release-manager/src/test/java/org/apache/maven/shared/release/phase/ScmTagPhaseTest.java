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
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.ScmTagParameters;
import org.apache.maven.scm.command.tag.TagScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderStub;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.scm.DefaultScmRepositoryConfigurator;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.shared.release.stubs.ScmManagerStub;
import org.apache.maven.shared.release.util.ReleaseUtil;

/**
 * Test the SCM tag phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ScmTagPhaseTest
    extends AbstractReleaseTestCase
{
    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "scm-tag" );
    }

    public static String getPath(File file) 
        throws IOException
    {
        return ReleaseUtil.isSymlink( file ) ? file.getCanonicalPath() : file.getAbsolutePath();
    }
    
    public void testTag()
        throws Exception
    {
        // prepare
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createReactorProjects();
        descriptor.setScmSourceUrl( "scm-url" );
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        descriptor.setWorkingDirectory( getPath(rootProject.getFile().getParentFile() ) );
        descriptor.setScmReleaseLabel( "release-label" );
        descriptor.setScmCommentPrefix( "[my prefix]" );

        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile() );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.tag( isA( ScmRepository.class ),
                                   argThat( new IsScmFileSetEquals( fileSet ) ),
                                   eq( "release-label" ), 
                                   argThat( new IsScmTagParametersEquals( new ScmTagParameters( "[my prefix] copy for tag release-label" ) ) ) ) ).
                                   thenReturn( new TagScmResult( "...", 
                                                                 Collections.singletonList( new ScmFile( getPath (rootProject
                                                                                                                  .getFile() ), ScmFileStatus.TAGGED ) ) ) );
        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );

        // execute
        phase.execute( descriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        verify( scmProviderMock ).tag( isA( ScmRepository.class ),
                                       argThat( new IsScmFileSetEquals( fileSet ) ),
                                       eq( "release-label" ), 
                                       argThat( new IsScmTagParametersEquals( new ScmTagParameters( "[my prefix] copy for tag release-label" ) ) ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

    public void testCommitMultiModuleDeepFolders()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects( "scm-commit/", "multimodule-with-deep-subprojects" );
        String sourceUrl = "http://svn.example.com/repos/project/trunk/";
        String scmUrl = "scm:svn:" + sourceUrl;
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        descriptor.setScmSourceUrl( scmUrl );
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        descriptor.setWorkingDirectory( getPath( rootProject.getFile().getParentFile() ) );
        descriptor.setScmReleaseLabel( "release-label" );
        descriptor.setScmCommentPrefix( "[my prefix]" );
        descriptor.setScmTagBase( "http://svn.example.com/repos/project/releases/" );

        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile() );

        SvnScmProviderRepository scmProviderRepository = new SvnScmProviderRepository( sourceUrl );
        scmProviderRepository.setTagBase( "http://svn.example.com/repos/project/releases/" );
        ScmRepository repository = new ScmRepository( "svn", scmProviderRepository );
        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.tag( eq( repository ),
                                   argThat(new IsScmFileSetEquals( fileSet ) ), 
                                   eq( "release-label" ),
                                   argThat(new IsScmTagParametersEquals( new ScmTagParameters( "[my prefix] copy for tag release-label" ) ) ) ) ).
                                       thenReturn(  new TagScmResult( "...", Collections.singletonList( new ScmFile( getPath (rootProject
                                                                                                                              .getFile() ), ScmFileStatus.TAGGED ) ) ) ) ;

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );
        stub.addScmRepositoryForUrl( scmUrl, repository );

        // execute
        phase.execute( descriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        // verify
        verify( scmProviderMock ).tag( eq( repository ),
                                       argThat(new IsScmFileSetEquals( fileSet ) ), 
                                       eq( "release-label" ),
                                       argThat(new IsScmTagParametersEquals( new ScmTagParameters( "[my prefix] copy for tag release-label" ) ) ) );
        verifyNoMoreInteractions( scmProviderMock ); 
    }

    public void testCommitForFlatMultiModule()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects =
            createReactorProjects( "rewrite-for-release/pom-with-parent-flat", "/root-project" );
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        descriptor.setScmSourceUrl( rootProject.getScm().getConnection() );
        descriptor.setWorkingDirectory( getPath( rootProject.getFile().getParentFile() ) );
        descriptor.setScmReleaseLabel( "release-label" );
        descriptor.setScmCommentPrefix( "[my prefix]" );

        // one directory up from root project
        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile().getParentFile() );

        String scmUrl = "file://localhost/tmp/scm-repo/trunk";
        SvnScmProviderRepository scmProviderRepository = new SvnScmProviderRepository( scmUrl );
        ScmRepository repository = new ScmRepository( "svn", scmProviderRepository );
        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.tag( eq( repository ),
                                   argThat( new IsScmFileSetEquals( fileSet ) ),
                                   eq( "release-label" ),
                                   argThat( new IsScmTagParametersEquals( new ScmTagParameters(
                                           "[my prefix] copy for tag release-label" ) ) ) ) ).thenReturn( new TagScmResult(
                                                "...",
                                                Collections.singletonList( new ScmFile(
                                                                                        getPath( rootProject.getFile() ),
                                                                                        ScmFileStatus.TAGGED ) ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );
        stub.addScmRepositoryForUrl( "scm:svn:" + scmUrl, repository );

        // execute
        phase.execute( descriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        // verify
        verify( scmProviderMock ).tag( eq( repository ),
                                       argThat( new IsScmFileSetEquals( fileSet ) ),
                                       eq( "release-label" ),
                                       argThat( new IsScmTagParametersEquals( new ScmTagParameters( "[my prefix] copy for tag release-label" ) ) ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

    public void testCommitMultiModule()
        throws Exception
    {
        // prepare
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createReactorProjects( "scm-commit/", "multiple-poms" );
        descriptor.setScmSourceUrl( "scm-url" );
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        descriptor.setWorkingDirectory( getPath (rootProject.getFile().getParentFile() ) );
        descriptor.setScmReleaseLabel( "release-label" );
        descriptor.setScmCommentPrefix( "[my prefix]" );

        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile() );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.tag( isA( ScmRepository.class ),
                                   argThat( new IsScmFileSetEquals( fileSet ) ),
                                   eq( "release-label" ),
                                   argThat( new IsScmTagParametersEquals( new ScmTagParameters( "[my prefix] copy for tag release-label" ) ) ) ) ).thenReturn( new TagScmResult( "...", Collections.singletonList( new ScmFile( getPath( rootProject
                       .getFile() ), ScmFileStatus.TAGGED ) ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );

        // exeucte
        phase.execute( descriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        verify( scmProviderMock ).tag( isA( ScmRepository.class ),
                                       argThat( new IsScmFileSetEquals( fileSet ) ),
                                       eq( "release-label" ),
                                       argThat( new IsScmTagParametersEquals( new ScmTagParameters( "[my prefix] copy for tag release-label" ) ) ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

    public void testTagNoReleaseLabel()
        throws Exception
    {
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createReactorProjects();

        try
        {
            phase.execute( descriptor, new DefaultReleaseEnvironment(), reactorProjects );
            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSimulateTag()
        throws Exception
    {
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createReactorProjects();
        descriptor.setScmSourceUrl( "scm-url" );
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        descriptor.setWorkingDirectory( getPath ( rootProject.getFile().getParentFile() ) );
        descriptor.setScmReleaseLabel( "release-label" );

        ScmProvider scmProviderMock = mock( ScmProvider.class );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );

        // execute
        phase.simulate( descriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        // no scmProvider invocation
        verifyNoMoreInteractions( scmProviderMock );
    }

    public void testSimulateTagNoReleaseLabel()
        throws Exception
    {
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        List<MavenProject> reactorProjects = createReactorProjects();

        try
        {
            phase.simulate( descriptor, new DefaultReleaseEnvironment(), reactorProjects );
            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testNoSuchScmProviderExceptionThrown()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects();
        ReleaseDescriptor releaseDescriptor = createReleaseDescriptor();

        ScmManager scmManagerMock = mock( ScmManager.class );
        when( scmManagerMock.makeScmRepository( "scm-url" ) ).thenThrow( new NoSuchScmProviderException( "..." ) );

        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
        configurator.setScmManager( scmManagerMock );

        // execute
        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "check cause", NoSuchScmProviderException.class, e.getCause().getClass() );
        }
        
        // verify
        verify( scmManagerMock ).makeScmRepository( "scm-url" );
        verifyNoMoreInteractions( scmManagerMock );
    }

    public void testScmRepositoryExceptionThrown()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects();
        ReleaseDescriptor releaseDescriptor = createReleaseDescriptor();

        ScmManager scmManagerMock = mock( ScmManager.class );
        when( scmManagerMock.makeScmRepository( "scm-url" ) ).thenThrow( new ScmRepositoryException( "..." ) );
        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
        configurator.setScmManager( scmManagerMock );

        // execute
        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseScmRepositoryException e )
        {
            assertNull( "Check no additional cause", e.getCause() );
        }
        
        // verify
        verify( scmManagerMock ).makeScmRepository( "scm-url" );
        verifyNoMoreInteractions( scmManagerMock );
    }

    public void testScmExceptionThrown()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects();
        ReleaseDescriptor releaseDescriptor = createReleaseDescriptor();

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.tag( isA( ScmRepository.class ),
                                   isA( ScmFileSet.class ),
                                   isA( String.class ),
                                   isA( ScmTagParameters.class ) ) ).thenThrow( new ScmException( "..." ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );

        // execute
        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "check cause", ScmException.class, e.getCause().getClass() );
        }
        
        // verify
        verify( scmProviderMock ).tag( isA( ScmRepository.class ),
                                       isA( ScmFileSet.class ),
                                       isA( String.class ),
                                       isA( ScmTagParameters.class ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

    public void testScmResultFailure()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects();
        ReleaseDescriptor releaseDescriptor = createReleaseDescriptor();

        ScmManager scmManager = (ScmManager) lookup( ScmManager.ROLE );
        ScmProviderStub providerStub =
            (ScmProviderStub) scmManager.getProviderByUrl( releaseDescriptor.getScmSourceUrl() );

        providerStub.setTagScmResult( new TagScmResult( "", "", "", false ) );

        try
        {
            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Commit should have failed" );
        }
        catch ( ReleaseScmCommandException e )
        {
            assertNull( "check no other cause", e.getCause() );
        }
    }

    private List<MavenProject> createReactorProjects()
        throws Exception
    {
        return createReactorProjects( "scm-commit/", "single-pom" );
    }

    private static ReleaseDescriptor createReleaseDescriptor() 
        throws IOException
    {
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        descriptor.setScmSourceUrl( "scm-url" );
        descriptor.setScmReleaseLabel( "release-label" );
        descriptor.setWorkingDirectory( getPath(getTestFile( "target/test/checkout" ) ) );
        return descriptor;
    }
}