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
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.ScmVersion;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderStub;
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
 * Test the release or branch preparation SCM commit phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ScmCommitPreparationPhaseTest
    extends AbstractReleaseTestCase
{
    private static final String PREFIX = "[maven-release-manager] prepare release ";

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.class, "scm-commit-release" );
    }

    @Test
    public void testIsCorrectImplementation()
    {
        assertEquals( ScmCommitPreparationPhase.class, phase.getClass() );
    }

    @Test
    public void testResolvesCorrectBranchImplementation()
        throws Exception
    {
        assertTrue( lookup( ReleasePhase.class, "scm-commit-branch" ) instanceof ScmCommitPreparationPhase );
    }

    @Test
    public void testCommit()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects();
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm-url" );
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        builder.setWorkingDirectory( rootProject.getFile().getParentFile().getAbsolutePath() );
        builder.setScmReleaseLabel( "release-label" );

        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile(), rootProject.getFile() );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.checkIn( isA( ScmRepository.class ), argThat( new IsScmFileSetEquals( fileSet ) ),
                                       isNull( ScmVersion.class ),
                                       eq( PREFIX
                                           + "release-label" ) ) ).thenReturn( new CheckInScmResult( "...",
                                                                                                     Collections.singletonList( new ScmFile( rootProject.getFile().getPath(),
                                                                                                                                             ScmFileStatus.CHECKED_IN ) ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );

        // execute
        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        verify( scmProviderMock ).checkIn( isA( ScmRepository.class ), argThat( new IsScmFileSetEquals( fileSet ) ),
                                           isNull( ScmVersion.class ), eq( PREFIX + "release-label" ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

    @Test
    public void testCommitAlternateMessage()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects();
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm-url" );
        builder.setScmCommentPrefix("[release]");
        builder.setScmReleaseCommitComment("@{prefix} Release of @{groupId}:@{artifactId} @{releaseLabel}");
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        builder.setWorkingDirectory( rootProject.getFile().getParentFile().getAbsolutePath() );
        builder.setScmReleaseLabel( "release-label" );

        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile(), rootProject.getFile() );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.checkIn( isA( ScmRepository.class ), argThat( new IsScmFileSetEquals( fileSet ) ),
                                       isNull( ScmVersion.class ),
                                       eq( "[release] Release of groupId:artifactId release-label" ) ) ).thenReturn( new CheckInScmResult( "...",
                                                                                                     Collections.singletonList( new ScmFile( rootProject.getFile().getPath(),
                                                                                                                                             ScmFileStatus.CHECKED_IN ) ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );

        // execute
        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        verify( scmProviderMock ).checkIn( isA( ScmRepository.class ), argThat( new IsScmFileSetEquals( fileSet ) ),
                                           isNull( ScmVersion.class ), eq( "[release] Release of groupId:artifactId release-label" ) );
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
        builder.setWorkingDirectory( rootProject.getFile().getParentFile().getAbsolutePath() );
        builder.setScmReleaseLabel( "release-label" );

        List<File> poms = new ArrayList<>();
        for ( Iterator<MavenProject> i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject project = i.next();
            poms.add( project.getFile() );
        }
        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile(), poms );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.checkIn( isA( ScmRepository.class ), argThat( new IsScmFileSetEquals( fileSet ) ),
                                       isNull( ScmVersion.class ),
                                       eq( PREFIX
                                           + "release-label" ) ) ).thenReturn( new CheckInScmResult( "...",
                                                                                                     Collections.singletonList( new ScmFile( rootProject.getFile().getPath(),
                                                                                                                                             ScmFileStatus.CHECKED_IN ) ) ) );
        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );

        // execute
        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        verify( scmProviderMock ).checkIn( isA( ScmRepository.class ), argThat( new IsScmFileSetEquals( fileSet ) ),
                                           isNull( ScmVersion.class ), eq( PREFIX + "release-label" ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

    @Test
    public void testCommitDevelopment()
        throws Exception
    {
        // prepare
        phase = (ReleasePhase) lookup( ReleasePhase.class, "scm-commit-development" );

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        List<MavenProject> reactorProjects = createReactorProjects();
        builder.setScmSourceUrl( "scm-url" );
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        builder.setWorkingDirectory( rootProject.getFile().getParentFile().getAbsolutePath() );
        builder.setScmReleaseLabel( "release-label" );

        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile(), rootProject.getFile() );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.checkIn( isA( ScmRepository.class ), argThat( new IsScmFileSetEquals( fileSet ) ),
                                       isNull( ScmVersion.class ),
                                       eq( "[maven-release-manager] prepare for next development iteration" ) ) ).thenReturn( new CheckInScmResult( "...",
                                                                                                                                                    Collections.singletonList( new ScmFile( rootProject.getFile().getPath(),
                                                                                                                                                                                            ScmFileStatus.CHECKED_IN ) ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );

        // execute
        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        verify( scmProviderMock ).checkIn( isA( ScmRepository.class ), argThat( new IsScmFileSetEquals( fileSet ) ),
                                           isNull( ScmVersion.class ),
                                           eq( "[maven-release-manager] prepare for next development iteration" ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

    @Test
    public void testCommitDevelopmentAlternateMessage()
        throws Exception
    {
        // prepare
        phase = (ReleasePhase) lookup( ReleasePhase.class, "scm-commit-development" );

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        List<MavenProject> reactorProjects = createReactorProjects();
        builder.setScmSourceUrl( "scm-url" );
        builder.setScmCommentPrefix("[release]");
        builder.setScmDevelopmentCommitComment("@{prefix} Bump version of @{groupId}:@{artifactId} after @{releaseLabel}");
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        builder.setWorkingDirectory( rootProject.getFile().getParentFile().getAbsolutePath() );
        builder.setScmReleaseLabel( "release-label" );

        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile(), rootProject.getFile() );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.checkIn( isA( ScmRepository.class ), argThat( new IsScmFileSetEquals( fileSet ) ),
                                       isNull( ScmVersion.class ),
                                       eq( "[release] Bump version of groupId:artifactId after release-label" ) ) ).thenReturn( new CheckInScmResult( "...",
                                                                                                                                                    Collections.singletonList( new ScmFile( rootProject.getFile().getPath(),
                                                                                                                                                                                            ScmFileStatus.CHECKED_IN ) ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );

        // execute
        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        verify( scmProviderMock ).checkIn( isA( ScmRepository.class ), argThat( new IsScmFileSetEquals( fileSet ) ),
                                           isNull( ScmVersion.class ),
                                           eq( "[release] Bump version of groupId:artifactId after release-label" ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

    @Test
    public void testCommitNoReleaseLabel()
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
    public void testCommitGenerateReleasePoms()
        throws Exception
    {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects();
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm-url" );
        builder.setGenerateReleasePoms( true );
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        builder.setWorkingDirectory( rootProject.getFile().getParentFile().getAbsolutePath() );
        builder.setScmReleaseLabel( "release-label" );

        List<File> files = new ArrayList<>();
        files.add( rootProject.getFile() );
        files.add( ReleaseUtil.getReleasePom( rootProject ) );
        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile(), files );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.checkIn( isA( ScmRepository.class ), argThat( new IsScmFileSetEquals( fileSet ) ),
                                       isNull( ScmVersion.class ),
                                       eq( PREFIX
                                           + "release-label" ) ) ).thenReturn( new CheckInScmResult( "...",
                                                                                                     Collections.singletonList( new ScmFile( rootProject.getFile().getPath(),
                                                                                                                                             ScmFileStatus.CHECKED_IN ) ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );

        // execute
        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        verify( scmProviderMock ).checkIn( isA( ScmRepository.class ), argThat( new IsScmFileSetEquals( fileSet ) ),
                                           isNull( ScmVersion.class ), eq( PREFIX + "release-label" ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

    @Test
    public void testSimulateCommit()
        throws Exception
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        List<MavenProject> reactorProjects = createReactorProjects();
        builder.setScmSourceUrl( "scm-url" );
        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        builder.setWorkingDirectory( rootProject.getFile().getParentFile().getAbsolutePath() );
        builder.setScmReleaseLabel( "release-label" );

        ScmProvider scmProviderMock = mock( ScmProvider.class );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );

        phase.simulate( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        // never invoke scmProviderMock
        verifyNoMoreInteractions( scmProviderMock );
    }

    @Test
    public void testSimulateCommitNoReleaseLabel()
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
        when( scmProviderMock.checkIn( isA( ScmRepository.class ), isA( ScmFileSet.class ), isNull( ScmVersion.class ),
                                       isA( String.class ) ) ).thenThrow( new ScmException( "..." ) );

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
        verify( scmProviderMock ).checkIn( isA( ScmRepository.class ), isA( ScmFileSet.class ),
                                           isNull( ScmVersion.class ), isA( String.class ) );
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

        providerStub.setCheckInScmResult( new CheckInScmResult( "", "", "", false ) );

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

    @Test
    public void testSuppressCommitWithRemoteTaggingFails()
        throws Exception
    {
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();
        List<MavenProject> reactorProjects = createReactorProjects();

        builder.setRemoteTagging( true );
        builder.setPinExternals( false );
        builder.setSuppressCommitBeforeTagOrBranch( true );

        ScmProvider scmProviderMock = mock( ScmProvider.class );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );

        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

            fail( "Commit should have failed with ReleaseFailureException" );
        }
        catch ( ReleaseFailureException e )
        {
            assertNull( "check no other cause", e.getCause() );
        }

        // never invoke scmProviderMock
        verifyNoMoreInteractions( scmProviderMock );
    }

    @Test
    public void testSuppressCommitAfterBranch()
        throws Exception
    {
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();
        List<MavenProject> reactorProjects = createReactorProjects();

        builder.setBranchCreation( true );
        builder.setRemoteTagging( false );
        builder.setSuppressCommitBeforeTagOrBranch( true );

        ScmProvider scmProviderMock = mock( ScmProvider.class );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        // never invoke scmProviderMock
        verifyNoMoreInteractions( scmProviderMock );
    }

    private List<MavenProject> createReactorProjects()
        throws Exception
    {
        String dir = "scm-commit/single-pom";
        return createReactorProjects( dir, dir, null );
    }

    private static ReleaseDescriptorBuilder createReleaseDescriptorBuilder()
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm-url" );
        builder.setScmReleaseLabel( "release-label" );
        builder.setWorkingDirectory( getTestFile( "target/test/checkout" ).getAbsolutePath() );
        return builder;
    }
}