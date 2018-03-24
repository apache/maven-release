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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.LinkOption;
import java.util.Collections;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmBranchParameters;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.command.branch.BranchScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderStub;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.shared.release.stubs.ScmManagerStub;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.junit.Test;

/**
 * Test the SCM branch phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ScmBranchPhaseTest
    extends AbstractReleaseTestCase
{
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        phase = lookup( ReleasePhase.class, "scm-branch" );
    }

    public static String getPath( File file )
        throws IOException
    {
        return file.toPath().toRealPath( LinkOption.NOFOLLOW_LINKS ).toString();
    }

    @Test
    public void testBranch()
        throws Exception
    {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        List<MavenProject> reactorProjects = createReactorProjects();
        builder.setScmSourceUrl( "scm-url" );
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        builder.setWorkingDirectory( getPath( rootProject.getFile().getParentFile() ) );
        builder.setPomFileName( "pom.xml" );
        builder.setScmReleaseLabel( "release-label" );
        builder.setScmCommentPrefix( "[my prefix] " );

        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile() );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.branch( isA( ScmRepository.class ), argThat( new IsScmFileSetEquals( fileSet ) ),
                                      eq( "release-label" ),
                                      argThat( new IsScmBranchParametersEquals( new ScmBranchParameters( "[my prefix] copy for branch release-label" ) ) ) ) ).thenReturn( new BranchScmResult( "...",
                                                                                                                                                                                                Collections.singletonList( new ScmFile( getPath( rootProject.getFile() ),
                                                                                                                                                                                                                                        ScmFileStatus.TAGGED ) ) ) );
        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );

        // execute
        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        verify( scmProviderMock ).branch( isA( ScmRepository.class ), argThat( new IsScmFileSetEquals( fileSet ) ),
                                          eq( "release-label" ),
                                          argThat( new IsScmBranchParametersEquals( new ScmBranchParameters( "[my prefix] copy for branch release-label" ) ) ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

    @Test
    public void testCommitMultiModuleDeepFolders()
        throws Exception
    {
        // prepare
        String dir = "scm-commit/multimodule-with-deep-subprojects";
        List<MavenProject> reactorProjects =
            createReactorProjects( dir, dir, null );
        String sourceUrl = "http://svn.example.com/repos/project/trunk/";
        String scmUrl = "scm:svn:" + sourceUrl;
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( scmUrl );
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        builder.setWorkingDirectory( getPath( rootProject.getFile().getParentFile() ) );
        builder.setPomFileName( "pom.xml" );
        builder.setScmReleaseLabel( "release-label" );
        builder.setScmCommentPrefix( "[my prefix] " );
        builder.setScmBranchBase( "http://svn.example.com/repos/project/branches/" );

        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile() );

        SvnScmProviderRepository scmProviderRepository = new SvnScmProviderRepository( sourceUrl );
        scmProviderRepository.setBranchBase( "http://svn.example.com/repos/project/branches/" );
        ScmRepository repository = new ScmRepository( "svn", scmProviderRepository );
        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.branch( eq( repository ), argThat( new IsScmFileSetEquals( fileSet ) ),
                                      eq( "release-label" ),
                                      argThat( new IsScmBranchParametersEquals( new ScmBranchParameters( "[my prefix] copy for branch release-label" ) ) ) ) ).thenReturn( new BranchScmResult( "...",
                                                                                                                                                                                                Collections.singletonList( new ScmFile( getPath( rootProject.getFile() ),
                                                                                                                                                                                                                                        ScmFileStatus.TAGGED ) ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );
        stub.addScmRepositoryForUrl( scmUrl, repository );

        // execute
        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        verify( scmProviderMock ).branch( eq( repository ), argThat( new IsScmFileSetEquals( fileSet ) ),
                                          eq( "release-label" ),
                                          argThat( new IsScmBranchParametersEquals( new ScmBranchParameters( "[my prefix] copy for branch release-label" ) ) ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

    @Test
    public void testCommitForFlatMultiModule()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects =
            createReactorProjects( "rewrite-for-release/pom-with-parent-flat", "root-project" );
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( rootProject.getScm().getConnection() );
        builder.setWorkingDirectory( getWorkingDirectory( "rewrite-for-release/pom-with-parent-flat" ).toRealPath( LinkOption.NOFOLLOW_LINKS ).toString() );
        builder.setPomFileName( "root-project/pom.xml" );
        builder.setScmReleaseLabel( "release-label" );
        builder.setScmCommentPrefix( "[my prefix] " );

        // one directory up from root project
        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile().getParentFile() );

        String scmUrl = "file://localhost/tmp/scm-repo/trunk";
        SvnScmProviderRepository scmProviderRepository = new SvnScmProviderRepository( scmUrl );
        ScmRepository repository = new ScmRepository( "svn", scmProviderRepository );
        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.branch( eq( repository ), argThat( new IsScmFileSetEquals( fileSet ) ),
                                      eq( "release-label" ),
                                      argThat( new IsScmBranchParametersEquals( new ScmBranchParameters( "[my prefix] copy for branch release-label" ) ) ) ) ).thenReturn( new BranchScmResult( "...",
                                                                                                                                                                                                Collections.singletonList( new ScmFile( getPath( rootProject.getFile() ),
                                                                                                                                                                                                                                        ScmFileStatus.TAGGED ) ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );
        stub.addScmRepositoryForUrl( "scm:svn:" + scmUrl, repository );

        // execute
        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        verify( scmProviderMock ).branch( eq( repository ), argThat( new IsScmFileSetEquals( fileSet ) ),
                                          eq( "release-label" ),
                                          argThat( new IsScmBranchParametersEquals( new ScmBranchParameters( "[my prefix] copy for branch release-label" ) ) ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

    @Test
    public void testCommitMultiModule()
        throws Exception
    {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        String dir = "scm-commit/multiple-poms";
        List<MavenProject> reactorProjects = createReactorProjects( dir, dir, null );
        builder.setScmSourceUrl( "scm-url" );
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        builder.setWorkingDirectory( getPath( rootProject.getFile().getParentFile() ) );
        builder.setPomFileName( "pom.xml" );
        builder.setScmReleaseLabel( "release-label" );
        builder.setScmCommentPrefix( "[my prefix] " );

        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile() );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.branch( isA( ScmRepository.class ), argThat( new IsScmFileSetEquals( fileSet ) ),
                                      eq( "release-label" ),
                                      argThat( new IsScmBranchParametersEquals( new ScmBranchParameters( "[my prefix] copy for branch release-label" ) ) ) ) ).thenReturn( new BranchScmResult( "...",
                                                                                                                                                                                                Collections.singletonList( new ScmFile( getPath( rootProject.getFile() ),
                                                                                                                                                                                                                                        ScmFileStatus.TAGGED ) ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );

        // exeucte
        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        verify( scmProviderMock ).branch( isA( ScmRepository.class ), argThat( new IsScmFileSetEquals( fileSet ) ),
                                          eq( "release-label" ),
                                          argThat( new IsScmBranchParametersEquals( new ScmBranchParameters( "[my prefix] copy for branch release-label" ) ) ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

    @Test
    public void testBranchNoReleaseLabel()
        throws Exception
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        List<MavenProject> reactorProjects = createReactorProjects();

        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );
            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    @Test
    public void testSimulateBranch()
        throws Exception
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        List<MavenProject> reactorProjects = createReactorProjects();
        builder.setScmSourceUrl( "scm-url" );
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        builder.setWorkingDirectory( getPath( rootProject.getFile().getParentFile() ) );
        builder.setPomFileName( "pom.xml" );
        builder.setScmReleaseLabel( "release-label" );

        ScmProvider scmProviderMock = mock( ScmProvider.class );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );

        // execute
        phase.simulate( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        // no scmProvider invocation
        verifyNoMoreInteractions( scmProviderMock );
    }

    @Test
    public void testSimulateBranchNoReleaseLabel()
        throws Exception
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        List<MavenProject> reactorProjects = createReactorProjects();

        try
        {
            phase.simulate( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );
            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    @Test
    public void testNoSuchScmProviderExceptionThrown()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects();
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();

        ScmManagerStub scmManagerStub = (ScmManagerStub) lookup( ScmManager.class );
        scmManagerStub.setException( new NoSuchScmProviderException( "..." )  );

        // execute
        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseExecutionException e )
        {
            // verify
            assertEquals( "check cause", NoSuchScmProviderException.class, e.getCause().getClass() );
        }
    }

    @Test
    public void testScmRepositoryExceptionThrown()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects();
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();

        ScmManagerStub scmManagerStub = (ScmManagerStub) lookup( ScmManager.class );
        scmManagerStub.setException( new ScmRepositoryException( "..." )  );

        // execute
        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseScmRepositoryException e )
        {
            // verify
            assertNull( "Check no additional cause", e.getCause() );
        }

    }

    @Test
    public void testScmExceptionThrown()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects();
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.branch( isA( ScmRepository.class ), isA( ScmFileSet.class ), isA( String.class ),
                                      isA( ScmBranchParameters.class ) ) ).thenThrow( new ScmException( "..." ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );

        // execute
        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "check cause", ScmException.class, e.getCause().getClass() );
        }

        // verify
        verify( scmProviderMock ).branch( isA( ScmRepository.class ), isA( ScmFileSet.class ), isA( String.class ),
                                          isA( ScmBranchParameters.class ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

    @Test
    public void testScmResultFailure()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects();
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();

        ScmManager scmManager = (ScmManager) lookup( ScmManager.class );
        ScmProviderStub providerStub =
            (ScmProviderStub) scmManager.getProviderByUrl( "scm-url" );

        providerStub.setBranchScmResult( new BranchScmResult( "", "", "", false ) );

        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

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
        String dir = "scm-commit/single-pom";
        return createReactorProjects( dir, dir, null );
    }

    private static ReleaseDescriptorBuilder createReleaseDescriptorBuilder()
        throws IOException
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm-url" );
        builder.setScmReleaseLabel( "release-label" );
        
        File workingDir = getTestFile( "target/test/checkout" );
        if ( !workingDir.exists() )
        {
            assertTrue( "Failed to create the directory, along with all necessary parent directories",
                        workingDir.mkdirs() );
        }
        
        builder.setWorkingDirectory( getPath( workingDir ) );
        builder.setPomFileName( "pom.xml" );
        return builder;
    }
}
