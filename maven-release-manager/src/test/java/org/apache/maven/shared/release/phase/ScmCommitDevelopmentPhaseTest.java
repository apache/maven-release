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

import java.util.Collections;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.manager.ScmManagerStub;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.jmock.Mock;
import org.jmock.core.Constraint;
import org.jmock.core.constraint.IsAnything;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.constraint.IsNull;
import org.jmock.core.matcher.InvokeOnceMatcher;
import org.jmock.core.matcher.TestFailureMatcher;
import org.jmock.core.stub.ReturnStub;

/**
 * Test the SCM development commit phase.
 *
 * @author <a href="mailto:me@lcorneliussen.de">Lars Corneliussen</a>
 */
public class ScmCommitDevelopmentPhaseTest
    extends AbstractReleaseTestCase
{
    private static final String COMMIT_MESSAGE = "[maven-release-manager] prepare for next development iteration";

    private static final String ROLLBACK_PREFIX =
        "[maven-release-manager] rollback changes from release preparation of ";

    private List reactorProjects;

    private MavenProject rootProject;

    private ReleaseDescriptor descriptor;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "scm-commit-development" );

        reactorProjects = createReactorProjects();
        rootProject = ReleaseUtil.getRootProject( reactorProjects );
        descriptor = createReleaseDescriptor( rootProject );

    }

    public void testIsCorrectImplementation()
    {
        assertEquals( ScmCommitDevelopmentPhase.class, phase.getClass() );
    }

    public void testNoCommitOrRollbackRequired()
        throws Exception
    {
        ReleaseDescriptor descriptor = createReleaseDescriptor( rootProject );
        List reactorProjects = createReactorProjects();

        descriptor.setRemoteTagging( false );
        descriptor.setSuppressCommitBeforeTagOrBranch( true );
        descriptor.setUpdateWorkingCopyVersions( false );

        validateNoCheckin();

        phase.execute( descriptor, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( true );
    }

    public void testCommitsNextVersions()
        throws Exception
    {
        descriptor.setUpdateWorkingCopyVersions( true );

        validateCheckin( COMMIT_MESSAGE );

        phase.execute( descriptor, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( true );
    }

    public void testCommitsRollbackPrepare()
        throws Exception
    {
        descriptor.setUpdateWorkingCopyVersions( false );

        validateCheckin( ROLLBACK_PREFIX + descriptor.getScmReleaseLabel() );

        phase.execute( descriptor, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( true );
    }

    private void validateCheckin( String message )
        throws Exception
    {
        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile(), rootProject.getFile() );
        Mock scmProviderMock = new Mock( ScmProvider.class );
        Constraint[] arguments = new Constraint[]{new IsAnything(), new IsScmFileSetEquals( fileSet ), new IsNull(),
            new IsEqual( message )};
        scmProviderMock
            .expects( new InvokeOnceMatcher() )
            .method( "checkIn" )
            .with( arguments )
            .will( new ReturnStub( new CheckInScmResult( "...", Collections.singletonList( new ScmFile( rootProject
                       .getFile().getPath(), ScmFileStatus.CHECKED_IN ) ) ) ) );

        
        
        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( (ScmProvider) scmProviderMock.proxy() );
    }

    private void validateNoCheckin()
        throws Exception
    {
        Mock scmProviderMock = new Mock( ScmProvider.class );
        scmProviderMock.expects( new TestFailureMatcher( "Shouldn't have called checkIn" ) ).method( "checkIn" );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( (ScmProvider) scmProviderMock.proxy() );
    }

    private List createReactorProjects()
        throws Exception
    {
        return createReactorProjects( "scm-commit/", "single-pom" );
    }

    private static ReleaseDescriptor createReleaseDescriptor( MavenProject rootProject )
    {
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        descriptor.setScmSourceUrl( "scm-url" );
        descriptor.setScmReleaseLabel( "release-label" );
        descriptor.setWorkingDirectory( rootProject.getFile().getParentFile().getAbsolutePath() );
        return descriptor;
    }

}